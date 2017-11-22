/*
 *  Copyright 2015 Google Inc. All Rights Reserved.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.google.blockly.android.ui;

import android.content.ClipData;
import android.graphics.Canvas;
import android.graphics.Point;
import android.os.Handler;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.annotation.Size;
import android.support.annotation.VisibleForTesting;
import android.support.v4.view.ViewCompat;
import android.util.Log;
import android.util.Pair;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;

import com.google.blockly.android.clipboard.BlockClipDataHelper;
import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.android.control.ConnectionManager;
import com.google.blockly.model.Block;
import com.google.blockly.model.Connection;
import com.google.blockly.model.Workspace;
import com.google.blockly.model.WorkspacePoint;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Controller for dragging blocks and groups of blocks within a workspace.
 */
// TODO(#233): Rename to BlockViewTouchManager or similar
public class Dragger {
    private static final String TAG = "Dragger";
    private static final boolean LOG_TOUCH_EVENTS = false;
    private static final boolean LOG_DRAG_EVENTS = false;

    private static final int TAP_TIMEOUT = ViewConfiguration.getTapTimeout();

    // Modes of finishDragging()
    private static final int FINISH_BEHAVIOR_DROP = 1;
    private static final int FINISH_BEHAVIOR_REVERT = 2;
    private static final int FINISH_BEHAVIOR_DELETED = 3;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({FINISH_BEHAVIOR_DROP, FINISH_BEHAVIOR_REVERT, FINISH_BEHAVIOR_DELETED})
    public @interface FinishDragBehavior {}


    /**
     * Interface for processing a drag behavior.
     */
    // TODO(#233): Include clicks. Rename. BlockTouchHandler sounds good, but...
    public interface DragHandler {
        /**
         * This method checks whether the pending drag maps to a valid draggable {@link BlockGroup}
         * on the workspace.  If it does, it should return a {@link Runnable} that will perform (at
         * a later time) the necessary {@link Block} and {@link BlockView} manipulations to
         * construct that drag group, and assign it to the {@link PendingDrag}.  Such manipulations
         * must not occur immediately, because this can result in recursive touch events.  The
         * {@link Dragger} is designed to catch these calls and forcibly crash.  Just don't do it.
         * <p/>
         * When the {@link Runnable} is called, it should proceed with the {@code Block} and
         * {@code BlockView} manipulations, and call {@link PendingDrag#startDrag} to
         * assign the draggable {@link BlockGroup}, which must contain a root block on the
         * {@link Workspace} and be added to the {@link WorkspaceView}.
         * <p/>
         * If pending drag does not map to a draggable, this method should return null.
         *
         * @param pendingDrag The pending drag state in question.
         */
        @Nullable Runnable maybeGetDragGroupCreator(PendingDrag pendingDrag);

        /**
         * Handles click events on blocks.
         *
         * @param pendingDrag The pending drag state in question.
         * @return True if click was processed and event should be captured.
         */
        boolean onBlockClicked(PendingDrag pendingDrag);

        // TODO(#202): onDragCancel(BlockGroup dragGroup) to support invalid drop locations.
        //     For instance, returning a block to the trash. Currently drops at the last move
        //     location.
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({DRAG_MODE_IMMEDIATE, DRAG_MODE_SLOPPY})
    public @interface DragMode {}
    @VisibleForTesting static final int DRAG_MODE_IMMEDIATE = 0;
    @VisibleForTesting static final int DRAG_MODE_SLOPPY = 1;

    private final ArrayList<Connection> mDraggedConnections = new ArrayList<>();
    // For use in bumping neighbours; instance variable only to avoid repeated allocation.
    private final ArrayList<Connection> mTempConnections = new ArrayList<>();
    // For use in getting location on screen.
    private final int[] mTempScreenCoord1 = new int[2];
    private final int[] mTempScreenCoord2 = new int[2];
    private final ViewPoint mTempViewPoint = new ViewPoint();
    private final WorkspacePoint mTempWorkspacePoint = new WorkspacePoint();

    private Handler mMainHandler;
    private final BlocklyController mController;
    private final WorkspaceHelper mViewHelper;
    private final BlockClipDataHelper mClipHelper;
    private final Workspace mWorkspace;
    private final ConnectionManager mConnectionManager;

    /**
     * This flags helps check {@link #onTouchBlockImpl} is not called recursively, which can occur
     * when the view hierarchy is manipulated during event handling.
     */
    private boolean mWithinOnTouchBlockImpl = false;

    private PendingDrag mPendingDrag;
    private Runnable mLogPending = (LOG_TOUCH_EVENTS || LOG_DRAG_EVENTS) ? new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, (mPendingDrag == null ? "\tnot pending" :
                    (mPendingDrag.getDragGroup()==null ?
                            "\tpending: touched = " + mPendingDrag.getTouchedBlockView()
                            : "\tdragging: " + mPendingDrag.getDragGroup())));
        }
    } : null;

    // Which {@link BlockView} was touched, and possibly may be being dragged.
    private WorkspaceView mWorkspaceView;
    private WeakReference<BlockView> mHighlightedBlockViewRef = new WeakReference<>(null);
    //The square of the required touch slop before starting a drag, precomputed to avoid
    // square root operations at runtime.
    private float mTouchSlopSquared = 0.0f;

    private View.OnDragListener mDragEventListener = new View.OnDragListener() {
        @Override
        public boolean onDrag(View workspaceView, DragEvent event) {
            final int action = event.getAction();

            if (LOG_DRAG_EVENTS) {
                String actionName = (action == DragEvent.ACTION_DRAG_STARTED) ? "DRAG_STARTED" :
                        (action == DragEvent.ACTION_DRAG_LOCATION) ? "DRAG_LOCATION" :
                        (action == DragEvent.ACTION_DRAG_ENDED) ? "DRAG_ENDED" :
                        (action == DragEvent.ACTION_DROP) ? "DROP" :
                        "UNKNOWN ACTION #" + action;
                Log.d(TAG, "onDrag: " + actionName + ", " + event);

                mMainHandler.removeCallbacks(mLogPending);  // Only log once per event tick
                mMainHandler.post(mLogPending);
            }

            // ClipDescription is always null on DRAG_ENDED.
            if (action != DragEvent.ACTION_DRAG_ENDED &&
                    !mClipHelper.isBlockData(event.getClipDescription()))
            {
                if (LOG_DRAG_EVENTS) {
                    Log.d(TAG, "onDrag: Not a block.");
                }
                return false;
            }

            if (mPendingDrag != null && mPendingDrag.isDragging()) {
                switch (action) {
                    case DragEvent.ACTION_DRAG_STARTED:
                        // Triggered in maybeStartDrag(..).
                        // The rest of the drag data is already captured in mPendingDrag.
                        // NOTE: This event position does not respect view scale.

                        BlockView rootDraggedBlockView = mPendingDrag.getRootDraggedBlockView();
                        if (rootDraggedBlockView.getBlock().isMovable()) {
                            BlockGroup dragGroup = mPendingDrag.getDragGroup();
                            if (dragGroup.getParent() == mWorkspaceView) {
                                // Hide the view on the workspace
                                mPendingDrag.getDragGroup().setVisibility(View.INVISIBLE);
                            }

                            // TODO(#35): This might be better described as "selected".
                            ((View) rootDraggedBlockView).setPressed(true);
                            return true;    // We want to keep listening for drag events
                        } else {
                            return false;   // We don't want to keep listening for drag events
                        }
                    case DragEvent.ACTION_DRAG_LOCATION:
                        // If we're still finishing up a previous drag we may have missed the
                        // start of the drag, in which case we shouldn't do anything.
                        continueDragging(event);
                        break;
                    case DragEvent.ACTION_DRAG_ENDED:
                        // TODO(#202): Cancel pending drag?
                        if (event.getResult()) {
                            break;
                        }
                        // Otherwise fall through
                    case DragEvent.ACTION_DROP:
                        // Finalize dragging and reset dragging state flags.
                        // These state flags are still used in the initial phase of figuring out if
                        // a drag has started.
                        maybeConnectDragGroup();
                        finishDragging(FINISH_BEHAVIOR_DROP);
                        return true;    // The drop succeeded.
                    default:
                        break;
                }
            }
            return false;   // In every case that gets here, the return value won't be checked.
        }
    };

    /**
     * @param blocklyController The {@link BlocklyController} managing Blocks in this activity.
     */
    public Dragger(BlocklyController blocklyController) {
        mController = blocklyController;
        mWorkspace = blocklyController.getWorkspace();
        mViewHelper = blocklyController.getWorkspaceHelper();
        mClipHelper = blocklyController.getClipDataHelper();
        mConnectionManager = mWorkspace.getConnectionManager();

        mMainHandler = new Handler();
    }

    /**
     * @param slop The required touch slop before starting a drag.
     */
    public void setTouchSlop(float slop) {
        mTouchSlopSquared = slop * slop;
    }

    /**
     * Remove all the connections in a blocks tree from the list of connections being dragged. This
     * is used when removing shadow blocks from a block tree during a drag. If there's no drag
     * in progress this has no effects.
     *
     * @param rootBlock The start of the block tree to remove connections for.
     */
    public void removeFromDraggingConnections(Block rootBlock) {
        if (mPendingDrag == null) {
            return;
        }
        mTempConnections.clear();
        rootBlock.getAllConnectionsRecursive(mTempConnections);
        for (int i = 0; i < mTempConnections.size(); i++) {
            Connection conn = mTempConnections.get(i);
            mDraggedConnections.remove(conn);
            conn.setDragMode(false);
        }
    }

    /**
     * Creates a BlockTouchHandler that will initiate a drag only after the user has dragged
     * beyond some touch threshold.
     *
     * @param dragHandler The {@link DragHandler} to handle gestures for the constructed
     *                    {@link BlockTouchHandler}.
     * @return A newly constructed {@link BlockTouchHandler}.
     */
    public BlockTouchHandler buildSloppyBlockTouchHandler(final DragHandler dragHandler) {
        return new BlockTouchHandler() {
            @Override
            public boolean onTouchBlock(BlockView blockView, MotionEvent motionEvent) {
                return onTouchBlockImpl(DRAG_MODE_SLOPPY, dragHandler, blockView, motionEvent,
                        /* interceptMode */ false);
            }

            @Override
            public boolean onInterceptTouchEvent(BlockView blockView, MotionEvent motionEvent) {
                // Intercepted move events might still be handled but the child view, such as
                // a drop down field.
                return onTouchBlockImpl(DRAG_MODE_SLOPPY, dragHandler, blockView, motionEvent,
                        /* interceptMode */ true);
            }
        };
    };

    /**
     * Creates a BlockTouchHandler that will initiate a drag as soon as the BlockView receives a
     * {@link MotionEvent} directly (not via interception).
     *
     * @param dragHandler The {@link DragHandler} to handle gestures for the constructed
     *                    {@link BlockTouchHandler}.
     * @return A newly constructed {@link BlockTouchHandler}.
     */
    public BlockTouchHandler buildImmediateDragBlockTouchHandler(final DragHandler dragHandler) {
        return new BlockTouchHandler() {
            @Override
            public boolean onTouchBlock(BlockView blockView, MotionEvent motionEvent) {
                return onTouchBlockImpl(DRAG_MODE_IMMEDIATE, dragHandler, blockView, motionEvent,
                        /* interceptMode */ false);
            }

            @Override
            public boolean onInterceptTouchEvent(BlockView blockView, MotionEvent motionEvent) {
                return onTouchBlockImpl(DRAG_MODE_IMMEDIATE, dragHandler, blockView, motionEvent,
                        /* interceptMode */ true);
            }
        };
    };

    /**
     * Continue dragging the currently moving block.  Called during ACTION_DRAG_LOCATION.
     *
     * @param event The next drag event to handle, as received by the {@link WorkspaceView}.
     */
    private void continueDragging(DragEvent event) {
        updateBlockPosition(event);

        // highlight as we go
        BlockView highlightedBlockView = mHighlightedBlockViewRef.get();
        if (highlightedBlockView != null) {
            highlightedBlockView.setHighlightedConnection(null);
        }
        Pair<Connection, Connection> connectionCandidate =
                findBestConnection(mPendingDrag.getRootDraggedBlock());
        if (connectionCandidate != null) {
            highlightedBlockView = mViewHelper.getView(connectionCandidate.second.getBlock());
            mHighlightedBlockViewRef = new WeakReference<>(highlightedBlockView);
            highlightedBlockView.setHighlightedConnection(connectionCandidate.second);
        }

        mPendingDrag.getDragGroup().requestLayout();
    }

    /**
     * Attempts to connect a dropped drag group with nearby connections
     */
    private void maybeConnectDragGroup() {
        Block dragRoot = mPendingDrag.getRootDraggedBlock();

        // Maybe snap to connections.
        Pair<Connection, Connection> connectionCandidate = findBestConnection(dragRoot);
        if (connectionCandidate != null) {
            mController.connect(connectionCandidate.first, connectionCandidate.second);
            // .connect(..) includes bumping block within snap distance of the new location.
        } else {
            // Even if no connection is found, still bump any neighbors within snap distance of the
            // new location.
            mController.bumpNeighbors(dragRoot);
        }
    }

    /**
     * Finish a drag gesture and clear pending drag info.  Called by event handlers for ACTION_UP,
     * ACTION_CANCEL, ACTION_DROP, and ACTION_DRAG_ENDED.
     */
    // TODO(305): Revert actions when behavior == FINISH_BEHAVIOR_REVERT
    private void finishDragging(@FinishDragBehavior int behavior) {
        if (behavior == FINISH_BEHAVIOR_DROP || behavior == FINISH_BEHAVIOR_REVERT) {
            // Update the drag group so that everything that has been changed will be properly
            // invalidated. Also, update the positions of all of the connections that were impacted
            // by the move and add them back to the manager. All of the connection locations will be
            // set relative to their block views immediately after this loop.  For now we just want
            // to unset drag mode and add the connections back to the list; 0, 0 is a cheap place to
            // put them.
            // Dragged connections may be empty, especially if the
            for (int i = 0; i < mDraggedConnections.size(); i++) {
                Connection cur = mDraggedConnections.get(i);
                cur.setPosition(0, 0);
                cur.setDragMode(false);
                mConnectionManager.addConnection(cur);
            }
        }
        mDraggedConnections.clear();

        BlockView highlightedBlockView = mHighlightedBlockViewRef.get();
        if (highlightedBlockView != null) {
            highlightedBlockView.setHighlightedConnection(null);
            mHighlightedBlockViewRef = new WeakReference<>(null);
        }

        if (mPendingDrag != null) {
            BlockGroup dragGroup = mPendingDrag.getDragGroup();
            if (dragGroup != null) {
                // Restore visibility at the end of the drag.
                dragGroup.setVisibility(View.VISIBLE);
            }


            BlockView blockView = mPendingDrag.getRootDraggedBlockView();
            if (blockView != null) {
                ((View) blockView).setPressed(false);
            } // else, trashing or similar manipulation made the view disappear.
            mPendingDrag = null;
        }
    }

    public void setWorkspaceView(WorkspaceView view) {
        mWorkspaceView = view;
    }

    /**
     * Let the Dragger know that a block was touched. This will be called when the block in the
     * workspace has been touched, but a drag has not yet been started.
     *
     * This method handles both regular touch events and intercepted touch events, with the latter
     * identified with the {@code interceptMode} parameter.  The only difference is that intercepted
     * events only return true (indicating they are handled) when a drag has been initiated. This
     * allows any underlying View, such as a field to handle the MotionEvent normally.
     *
     * @param dragMode The mode (immediate or sloppy) for handling this touch event.
     * @param dragHandler The {@link DragHandler} attached to this view.
     * @param touchedView The {@link BlockView} that detected a touch event.
     * @param event The touch event.
     * @param interceptMode When true forces all {@link MotionEvent#ACTION_MOVE} events
     *                                   that match {@link #mPendingDrag} to return true / handled.
     *                                   Otherwise, it only returns true if a drag is started.
     *
     * @return True if the event was handled by this touch implementation.
     */
    @VisibleForTesting
    boolean onTouchBlockImpl(@DragMode int dragMode, DragHandler dragHandler, BlockView touchedView,
                             MotionEvent event, boolean interceptMode) {
        if (mWithinOnTouchBlockImpl) {
            throw new IllegalStateException(
                    "onTouchBlockImpl() called recursively. Make sure OnDragHandler." +
                    "maybeGetDragGroupCreator() is not manipulating the View hierarchy.");
        }
        mWithinOnTouchBlockImpl = true;

        final int action = event.getActionMasked();

        boolean matchesPending = false;
        if (mPendingDrag != null) {
            matchesPending = mPendingDrag.isMatchAndProcessed(event, touchedView);
            if (!matchesPending && !mPendingDrag.isAlive()) {
                mPendingDrag = null;  // Was a part of previous gesture. Delete.
            }
        }

        if (LOG_TOUCH_EVENTS) {
            Log.d(TAG, "onTouchBlockImpl: "
                    + (dragMode == DRAG_MODE_IMMEDIATE ? "IMMEDIATE" : "SLOPPY")
                    + (interceptMode ? " intercept" : " direct")
                    + "\n\t" + event
                    + "\n\tMatches pending? " + matchesPending);

            mMainHandler.removeCallbacks(mLogPending);  // Only call once per event 'tick'
            mMainHandler.post(mLogPending);
        }

        final boolean result;
        if (action == MotionEvent.ACTION_DOWN ) {
            if (mPendingDrag == null) {
                mPendingDrag = new PendingDrag(mController, touchedView, event);
                if (interceptMode) {
                    // Do not handle intercepted down events. Allow child views (particularly
                    // fields) to handle the touch normally.
                    result = false;
                } else {
                    // The user touched the block directly.
                    if (dragMode == DRAG_MODE_IMMEDIATE) {
                        result = maybeStartDrag(dragHandler);
                    } else {
                        result = true;
                    }
                }
            } else if (matchesPending && !interceptMode) {
                // The Pending Drag was created during intercept, but the child did not handle it
                // and the event has bubbled down to here.
                if (dragMode == DRAG_MODE_IMMEDIATE) {
                    result = maybeStartDrag(dragHandler);
                } else {
                    result = true;
                }
            } else {
                result = false; // Pending drag already started with a different view / pointer id.
            }
        } else if (matchesPending) {
            // This touch is part of the current PendingDrag.
            if (action == MotionEvent.ACTION_MOVE) {
                if (mPendingDrag.isDragging()) {
                    result = false;  // We've already cancelled or started dragging.
                } else {
                    // Mark all direct move events as handled, but only intercepted events if they
                    // initiate a new drag.
                    boolean isDragGesture =
                            (!interceptMode && dragMode == DRAG_MODE_IMMEDIATE
                                    && event.getDownTime() > TAP_TIMEOUT)
                            || isBeyondSlopThreshold(event);
                    boolean isNewDrag = isDragGesture && maybeStartDrag(dragHandler);
                    result = isNewDrag || !interceptMode;
                }
            }
            // Handle the case when the user releases before moving far enough to start a drag.
            else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                if (!mPendingDrag.isDragging()) {
                    if (!interceptMode && mPendingDrag.isClick()) {
                        dragHandler.onBlockClicked(mPendingDrag);
                    }
                    finishDragging(FINISH_BEHAVIOR_REVERT);
                }
                result = !interceptMode;
            } else {
                result = false; // Unrecognized event action
            }
        } else {
            result = false; // Doesn't match existing drag.
        }

        if (LOG_TOUCH_EVENTS) Log.d(TAG, "\treturn " + result);
        mWithinOnTouchBlockImpl = false;
        return result;
    }

    /**
     * @return The listener to use for {@link DragEvent}'s in the {@link WorkspaceView}.
     */
    public View.OnDragListener getDragEventListener() {
        return mDragEventListener;
    }

    /**
     * Checks whether {@code actionMove} is beyond the allowed slop (i.e., unintended) drag motion
     * distance.
     *
     * @param actionMove The {@link MotionEvent#ACTION_MOVE} event.
     * @return True if the motion is beyond the allowed slop threshold
     */
    private boolean isBeyondSlopThreshold(MotionEvent actionMove) {
        BlockView touchedView = mPendingDrag.getTouchedBlockView();

        // Not dragging yet - compute distance from Down event and start dragging if far enough.
        @Size(2) int[] touchDownLocation = mTempScreenCoord1;
        mPendingDrag.getTouchDownScreen(touchDownLocation);

        @Size(2) int[] curScreenLocation = mTempScreenCoord2;
        touchedView.getTouchLocationOnScreen(actionMove, curScreenLocation);

        final int deltaX = touchDownLocation[0] - curScreenLocation[0];
        final int deltaY = touchDownLocation[1] - curScreenLocation[1];

        // Dragged far enough to start a drag?
        return (deltaX * deltaX + deltaY * deltaY > mTouchSlopSquared);
    }

    /**
     * Handle motion events while starting to drag a block.  This keeps track of whether the block
     * has been dragged more than {@code mTouchSlop} and starts a drag if necessary. Once the drag
     * has been started, all following events will be handled through the {@link
     * #mDragEventListener}.
     */
    private boolean maybeStartDrag(DragHandler dragHandler) {
        // Check with the pending drag handler to select or create the dragged group.
        final PendingDrag pendingDrag = mPendingDrag;  // Stash for async callback
        final Runnable dragGroupCreator = dragHandler.maybeGetDragGroupCreator(pendingDrag);
        final boolean foundDragGroup = (dragGroupCreator != null);
        if (foundDragGroup) {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mPendingDrag != null && mPendingDrag.isDragging()) {
                        return; // Ignore.  Probably being handled by a child view.
                    }

                    dragGroupCreator.run();
                    boolean dragStarted = pendingDrag.isDragging();
                    if (dragStarted) {
                        mPendingDrag = pendingDrag;
                        final BlockGroup dragGroup = mPendingDrag.getDragGroup();
                        ViewParent parent = dragGroup.getParent();
                        // TODO(#): Allow unparented BlockGroups (new blocks from Toolbox, etc.)
                        if (parent != mWorkspaceView) {
                            throw new IllegalStateException("dragGroup is root in WorkspaceView");
                        }

                        try {
                            ClipData clipData = mClipHelper.buildDragClipData(pendingDrag);

                            Block rootBlock = dragGroup.getFirstBlock();
                            removeDraggedConnectionsFromConnectionManager(rootBlock);
                            int flags = mViewHelper.getBlockViewFactory().getDragAndDropFlags();

                            ViewCompat.startDragAndDrop(
                                    dragGroup,
                                    clipData,
                                    new DragShadowBuilder(pendingDrag, mViewHelper),
                                    pendingDrag,
                                    flags);
                        } catch (IOException e) {
                            Log.w(TAG, "Serialization failed in ClipDataHelper.");
                            mPendingDrag = null;
                        }
                    } else {
                        mPendingDrag = null;
                    }
                }
            });
        }

        return foundDragGroup;
    }

    /**
     * Removes all connections of block and its descendants from the {@link }ConnectionManager}, so
     * these connections are not considered as potential connections when looking from connections
     * during dragging.
     *
     * @param block The root block of connections that should be removed.
     */
    private void removeDraggedConnectionsFromConnectionManager(Block block) {
        mDraggedConnections.clear();
        // Don't track any of the connections that we're dragging around.
        block.getAllConnectionsRecursive(mDraggedConnections);
        for (int i = 0; i < mDraggedConnections.size(); i++) {
            Connection conn = mDraggedConnections.get(i);
            mConnectionManager.removeConnection(conn);
            conn.setDragMode(true);
        }
    }

    /**
     * Move the currently dragged block in response to a new {@link MotionEvent}.
     * <p/>
     * All of the child blocks move with the root block based on its position during layout.
     *
     * @param event The {@link MotionEvent} to react to.
     */
    private void updateBlockPosition(DragEvent event) {
        // The event is relative to the WorkspaceView. Grab the pixel offset within.
        ViewPoint curDragLocationPixels = mTempViewPoint;
        curDragLocationPixels.set((int) event.getX(), (int) event.getY());
        WorkspacePoint curDragPositionWorkspace = mTempWorkspacePoint;
        mViewHelper.virtualViewToWorkspaceCoordinates(
                curDragLocationPixels, curDragPositionWorkspace);

        WorkspacePoint touchDownWorkspace = mPendingDrag.getTouchDownWorkspaceCoordinates();
        // Subtract original drag location from current location to get delta
        float workspaceDeltaX = curDragPositionWorkspace.x - touchDownWorkspace.x;
        float workspaceDeltaY = curDragPositionWorkspace.y - touchDownWorkspace.y;

        WorkspacePoint blockOrigPosition = mPendingDrag.getOriginalBlockPosition();
        mPendingDrag.getRootDraggedBlock().setPosition(blockOrigPosition.x + workspaceDeltaX,
                                                blockOrigPosition.y + workspaceDeltaY);
        mPendingDrag.getDragGroup().requestLayout();
    }

    private Pair<Connection, Connection> findBestConnection(Block block) {
        return mConnectionManager.findBestConnection(block, mViewHelper.getMaxSnapDistance());
    }

    private static class DragShadowBuilder extends View.DragShadowBuilder {
        private PendingDrag mPendingDrag;
        private int mSizeX, mSizeY;
        private float mZoomScale;

        DragShadowBuilder(PendingDrag pendingDrag, WorkspaceHelper helper) {
            super(pendingDrag.getDragGroup());
            mPendingDrag = pendingDrag;
            mZoomScale = helper.getWorkspaceZoomScale();

            BlockGroup dragGroup = pendingDrag.getDragGroup();
            mSizeX = dragGroup.getWidth();
            if (mSizeX == 0) {
                dragGroup.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                dragGroup.layout(0, 0, dragGroup.getMeasuredWidth(), dragGroup.getMeasuredHeight());
                mSizeX = dragGroup.getWidth();
            }
            mSizeY = dragGroup.getHeight();
        }

        @Override
        public void onProvideShadowMetrics(Point shadowSize, Point shadowTouchPoint) {
            shadowSize.set(
                    (int) Math.ceil(mSizeX * mZoomScale),
                    (int) Math.ceil(mSizeY * mZoomScale));
            ViewPoint dragTouchOffset = mPendingDrag.getDragTouchOffset();
            shadowTouchPoint.set(
                    (int) Math.ceil(dragTouchOffset.x * mZoomScale),
                    (int) Math.ceil(dragTouchOffset.y * mZoomScale));
        }

        @Override
        public void onDrawShadow(Canvas canvas) {
            canvas.scale(mZoomScale, mZoomScale);
            super.onDrawShadow(canvas);
        }
    }
}
