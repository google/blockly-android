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
import android.graphics.Rect;
import android.support.annotation.IntDef;
import android.support.annotation.Size;
import android.support.annotation.VisibleForTesting;
import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import android.util.Pair;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;

import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.android.control.ConnectionManager;
import com.google.blockly.model.Block;
import com.google.blockly.model.Connection;
import com.google.blockly.model.Workspace;
import com.google.blockly.model.WorkspacePoint;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Controller for dragging blocks and groups of blocks within a workspace.
 */
public class Dragger {
    private static final String TAG = "Dragger";

    /**
     * Interface for processing a drag behavior.
     */
    public interface DragHandler {
        /**
         * This method checks whether the pending drag is valid.  When valid, implementations are
         * expected to call {@link PendingDrag#setDragGroup(BlockGroup)} to assign the draggable
         * {@link BlockGroup}, which must contain a root block on the {@link Workspace} and be added
         * to the {@link WorkspaceView}.
         *
         * @param pendingDrag The pending drag state in question.
         */
        void maybeAssignDragGroup(PendingDrag pendingDrag);

        // TODO(#202): onDragCancel(BlockGroup dragGroup) to support invalid drop locations.
        //     For instance, returning a block to the trash. Currently drops at the last move
        //     location.
    }

    // Consider moving these to a new small class.
    // No current touch interaction.
    private static final int TOUCH_STATE_NONE = 0;
    // Block in this view has received "Down" event; waiting for further interactions to decide
    // whether to drag, show context menu, etc.
    private static final int TOUCH_STATE_DOWN = 1;
    // Block in this view is being dragged.
    private static final int TOUCH_STATE_DRAGGING = 2;
    // Block in this view received a long press.
    private static final int TOUCH_STATE_LONGPRESS = 3;

    private final ArrayList<Connection> mDraggedConnections = new ArrayList<>();
    // For use in bumping neighbours; instance variable only to avoid repeated allocation.
    private final ArrayList<Connection> mNeighbouringConnections = new ArrayList<>();
    // Rect for finding the bounding box of the trash can view.
    private final Rect mTrashRect = new Rect();
    // For use in getting location on screen.
    private final int[] mTempScreenCoord1 = new int[2];
    private final int[] mTempScreenCoord2 = new int[2];
    private final ViewPoint mTempViewPoint = new ViewPoint();
    private final WorkspacePoint mTempWorkspacePoint = new WorkspacePoint();

    private final BlocklyController mController;
    private final WorkspaceHelper mHelper;
    private final Workspace mWorkspace;
    private final ConnectionManager mConnectionManager;

    private PendingDrag mPendingDrag;

    // Which {@link BlockView} was touched, and possibly may be being dragged.
    private WorkspaceView mWorkspaceView;
    private BlockView mHighlightedBlockView;
    // The view for the trash can.
    private View mTrashView;
    //The square of the required touch slop before starting a drag, precomputed to avoid
    // square root operations at runtime.
    private float mTouchSlopSquared = 0.0f;

    // Current state of touch interaction with blocks in this workspace view.
    @TouchState
    private int mTouchState = TOUCH_STATE_NONE;

    private View.OnDragListener mDragEventListener = new View.OnDragListener() {
        @Override
        public boolean onDrag(View workspaceView, DragEvent event) {
            final int action = event.getAction();
            switch (action) {
                case DragEvent.ACTION_DRAG_STARTED:
                    // Triggered in maybeStartDrag(..).
                    mTouchState = TOUCH_STATE_DRAGGING;
                    // The rest of the drag data is already captured in mPendingDrag.
                    // NOTE: This event position does not respect view scale.
                    return true;    // We want to keep listening for drag events
                case DragEvent.ACTION_DRAG_LOCATION:
                    if (mTouchState == TOUCH_STATE_DRAGGING) {
                        // If we're still finishing up a previous drag we may have missed the
                        // start of the drag, in which case we shouldn't do anything.
                        continueDragging(event);
                    }
                    break;
                case DragEvent.ACTION_DRAG_ENDED:
                    // TODO(#202): Cancel pending drag?
                    if (event.getResult()) {
                        break;
                    }
                    // Otherwise fall through
                case DragEvent.ACTION_DROP:
                    // Finalize dragging and reset dragging state flags.
                    // These state flags are still used in the initial phase of figuring out if a
                    // drag has started.
                    if (mTouchState == TOUCH_STATE_DRAGGING) {
                        if (touchingTrashView(event)) {
                            dropInTrash();
                        } else {
                            finishDragging();
                        }
                        clearPendingDrag();
                        return true;    // The drop succeeded.
                    }
                    return false;
                default:
                    break;
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
        mHelper = blocklyController.getWorkspaceHelper();
        mConnectionManager = mWorkspace.getConnectionManager();
    }

    /**
     * @param slop The required touch slop before starting a drag.
     */
    public void setTouchSlop(float slop) {
        mTouchSlopSquared = slop * slop;
    }

    public BlockTouchHandler buildBlockTouchHandler(final DragHandler dragHandler) {
        return new BlockTouchHandler() {
            @Override
            public boolean onTouchBlock(BlockView blockView, MotionEvent motionEvent) {
                return onTouchBlockImpl(dragHandler, blockView, motionEvent,
                        /* interceptMode */ false);
            }

            @Override
            public boolean onInterceptTouchEvent(BlockView blockView, MotionEvent motionEvent) {
                // Intercepted move events might still be handled but the child view, such as
                // a drop down field.
                return onTouchBlockImpl(dragHandler, blockView, motionEvent,
                        /* interceptMode */ true);
            }
        };
    };

    private void clearPendingDrag() {
        if (mPendingDrag != null) {
            BlockView blockView = mPendingDrag.getTouchedBlockView();
            if (blockView != null) {
                ((View) blockView).setPressed(false);
            }
            mPendingDrag = null;
        }
        mTouchState = TOUCH_STATE_NONE;
    }

    /**
     * Continue dragging the currently moving block.  Called during ACTION_DRAG_LOCATION.
     *
     * @param event The next drag event to handle, as received by the {@link WorkspaceView}.
     */
    private void continueDragging(DragEvent event) {
        updateBlockPosition(event);

        // highlight as we go
        if (mHighlightedBlockView != null) {
            mHighlightedBlockView.setHighlightedConnection(null);
        }
        Pair<Connection, Connection> connectionCandidate =
                findBestConnection(mPendingDrag.getRootBlock());
        if (connectionCandidate != null) {
            mHighlightedBlockView = mHelper.getView(connectionCandidate.second.getBlock());
            mHighlightedBlockView.setHighlightedConnection(connectionCandidate.second);
        }

        mPendingDrag.getDragGroup().requestLayout();
    }

    /**
     * Finish block dragging. Called during ACTION_DRAG_ENDED and ACTION_DROP.
     * <p/>
     * This method must be called upon receiving the "up" event that ends an ongoing drag process.
     */
    @VisibleForTesting
    void finishDragging() {
        Block dragRoot = mPendingDrag.getRootBlock();

        // Maybe snap to connections
        Pair<Connection, Connection> connectionCandidate = findBestConnection(dragRoot);
        if (connectionCandidate != null) {
            mController.connect(dragRoot, connectionCandidate.first, connectionCandidate.second);
        }

        finalizeMove();
    }

    public void setWorkspaceView(WorkspaceView view) {
        mWorkspaceView = view;
    }

    // TODO(#210): Generalize this to other possible block drop targets.
    public void setTrashView(View trashView) {
        mTrashView = trashView;
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
     * @param dragHandler The {@link DragHandler} attached to this view
     *                    (via {@link #buildBlockTouchHandler}).
     * @param touchedView The {@link BlockView} that detected a touch event.
     * @param event The touch event.
     * @param interceptMode When true forces all {@link MotionEvent#ACTION_MOVE} events
     *                                   that match {@link #mPendingDrag} to return true / handled.
     *                                   Otherwise, it only returns true if a drag is started.
     *
     * @return True if the event was handled by this touch implementation.
     */
    @VisibleForTesting
    boolean onTouchBlockImpl(DragHandler dragHandler, BlockView touchedView, MotionEvent event,
                             boolean interceptMode) {
        final int action = MotionEventCompat.getActionMasked(event);
        final int pointerId = MotionEventCompat.getPointerId(
                event, MotionEventCompat.getActionIndex(event));

        boolean matchesPending = mPendingDrag != null && mPendingDrag.getPointerId() == pointerId
                && mPendingDrag.getTouchedBlockView() == touchedView;

        if (action == MotionEvent.ACTION_DOWN ) {
            if (mPendingDrag == null) {
                handleActionDown(dragHandler, touchedView, event);
            }
            // Do not handle intercepted down events, to allow child views (particularly fields) to
            // handle the touch normally.  However, the first intercept event may have created
            // the PendingDrag, and if the view receives a normal event that means the child did
            // not handle it, and we want to continue receiving events.
            return !interceptMode && matchesPending;
        }
        if (matchesPending) {
            // This touch is part of the previously started drag.
            if (action == MotionEvent.ACTION_MOVE) {
                // Mark all direct move events as handled, but only intercepted events if they
                // initiate a new drag.
                return maybeStartDrag(dragHandler, event)  || !interceptMode;
            }
            // Handle the case when the user releases before moving far enough to start a drag.
            if (action == MotionEvent.ACTION_UP) {
                clearPendingDrag();
                return !interceptMode;
            }
        }

        return false;
    }

    /**
     * @return The listener to use for {@link DragEvent}'s in the {@link WorkspaceView}.
     */
    public View.OnDragListener getDragEventListener() {
        return mDragEventListener;
    }

    /**
     * Handle motion events while starting to drag a block.  This keeps track of whether the block
     * has been dragged more than {@code mTouchSlop} and starts a drag if necessary. Once the drag
     * has been started, all following events will be handled through the {@link
     * #mDragEventListener}.
     */
    private boolean maybeStartDrag(DragHandler dragHandler, MotionEvent actionMove) {
        final int pointerIdx =
                MotionEventCompat.findPointerIndex(actionMove, mPendingDrag.getPointerId());
        if (mTouchState != TOUCH_STATE_DRAGGING) {
            BlockView touchedView = mPendingDrag.getTouchedBlockView();

            // Not dragging yet - compute distance from Down event and start dragging if far enough.
            @Size(2) int[] touchDownLocation = mTempScreenCoord1;
            mPendingDrag.getTouchDownScreen(touchDownLocation);

            @Size(2) int[] curScreenLocation = mTempScreenCoord2;
            touchedView.getTouchLocationOnScreen(actionMove, curScreenLocation);

            final int deltaX = touchDownLocation[0] - curScreenLocation[0];
            final int deltaY = touchDownLocation[1] - curScreenLocation[1];

            if (deltaX * deltaX + deltaY * deltaY >= mTouchSlopSquared) {
                // Dragged far enough to start a drag.  Check with the pending drag handler
                // to select or create the dragged group.
                dragHandler.maybeAssignDragGroup(mPendingDrag);
                boolean dragStarted = mPendingDrag.isDragging();
                if (dragStarted) {
                    final PendingDrag pendingDrag = mPendingDrag;
                    final BlockGroup dragGroup = mPendingDrag.getDragGroup();
                    if (dragGroup.getParent() != mWorkspaceView) {
                        throw new IllegalStateException("dragGroup is root in WorkspaceView");
                    }

                    if (pendingDrag == mPendingDrag) {
                        Block rootBlock = dragGroup.getFirstBlock();
                        removeDraggedConnections(rootBlock);
                        ClipData clipData = ClipData.newPlainText(
                                WorkspaceView.BLOCK_GROUP_CLIP_DATA_LABEL, "");
                        dragGroup.startDrag(clipData,
                                new View.DragShadowBuilder(), null, 0);
                    }
                    mTouchState = TOUCH_STATE_DRAGGING;
                } else {
                    mPendingDrag = null;
                }

                return dragStarted;
            }
        }
        return false;
    }

    /**
     * Save position of Down event for later use when (if) minimum dragging distance threshold has
     * been met. By not calling Dragger.startDragging() here already, we prevent unnecessary block
     * operations until we are sure that the user is dragging. Adjust the event's coordinates from
     * the {@link BlockView}'s coordinate system to {@link WorkspaceView} coordinates.
     */
    private void handleActionDown(DragHandler dragHandler, BlockView touchedView,
                                  MotionEvent actionDown) {

        if (mPendingDrag != null || mTouchState == TOUCH_STATE_DRAGGING) {
            return;  // Pending or active or drag. Possibly another finger.
        }

        mTouchState = TOUCH_STATE_DOWN;
        mPendingDrag = new PendingDrag(mController, touchedView, actionDown);
        ((View) touchedView).setPressed(true);
    }

    /**
     * Check whether the given event occurred on top of the trash can button.  Should be called from
     * {@link WorkspaceView}.
     *
     * @param event The event whose location should be checked, with position in WorkspaceView
     * coordinates.
     * @return Whether the event was on top of the trash can button.
     */
    // TODO(#210): Generalize this to other possible block drop targets.
    private boolean touchingTrashView(DragEvent event) {
        if (mTrashView == null) {
            return false;
        }

        mTrashView.getLocationOnScreen(mTempScreenCoord1);
        mTrashView.getHitRect(mTrashRect);

        mTrashRect.offset((mTempScreenCoord1[0] - mTrashRect.left), (mTempScreenCoord1[1] - mTrashRect.top));
        // offset drag event positions by the workspace view's position on screen.
        mWorkspaceView.getLocationOnScreen(mTempScreenCoord1);
        return mTrashRect.contains((int) event.getX() + mTempScreenCoord1[0],
                (int) event.getY() + mTempScreenCoord1[1]);
    }

    /**
     * Ends a drag in the trash can, clearing state and deleting blocks as needed.
     */
    private void dropInTrash() {
        if (mHighlightedBlockView != null) {
            mHighlightedBlockView.setHighlightedConnection(null);
            mHighlightedBlockView = null;
        }
        mDraggedConnections.clear();
        mController.trashRootBlock(mPendingDrag.getRootBlock());
    }

    private void removeDraggedConnections(Block block) {
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
        mHelper.virtualViewToWorkspaceCoordinates(curDragLocationPixels, curDragPositionWorkspace);

        WorkspacePoint touchDownWorkspace = mPendingDrag.getTouchDownWorkspaceCoordinates();
        // Subtract original drag location from current location to get delta
        int workspaceDeltaX = curDragPositionWorkspace.x - touchDownWorkspace.x;
        int workspaceDeltaY = curDragPositionWorkspace.y - touchDownWorkspace.y;

        WorkspacePoint blockOrigPosition = mPendingDrag.getOriginalBlockPosition();
        mPendingDrag.getRootBlock().setPosition(blockOrigPosition.x + workspaceDeltaX,
                                                blockOrigPosition.y + workspaceDeltaY);
        mPendingDrag.getDragGroup().requestLayout();
    }

    /**
     * Update the positions of all of the connections that were impacted by the move and add them
     * back to the manager.
     */
    private void finalizeMove() {
        if (mHighlightedBlockView != null) {
            mHighlightedBlockView.setHighlightedConnection(null);
            mHighlightedBlockView = null;
        }
        // Update the drag group so that everything that has been changed will be properly
        // invalidated.
        BlockGroup newRootBlockGroup =
                mHelper.getRootBlockGroup(mPendingDrag.getRootBlock());
        bumpNeighbours(mPendingDrag.getRootBlock(), newRootBlockGroup);
        // All of the connection locations will be set relative to their block views immediately
        // after this loop.  For now we just want to unset drag mode and add the connections back
        // to the list; 0, 0 is a cheap place to put them.
        for (int i = 0; i < mDraggedConnections.size(); i++) {
            Connection cur = mDraggedConnections.get(i);
            cur.setPosition(0, 0);
            cur.setDragMode(false);
            mConnectionManager.addConnection(cur);
        }
        mDraggedConnections.clear();

        newRootBlockGroup.requestLayout();
    }

    /**
     * Move all neighbours of the current block and its sub-blocks so that they don't appear to be
     * connected to the current block.
     *
     * @param currentBlock The {@link Block} to bump others away from.
     * @param rootBlockGroup The root of {@code currentBlock}.
     */
    // TODO(#82): Move to BlocklyController as part of the connect(..) method.
    private void bumpNeighbours(Block currentBlock, BlockGroup rootBlockGroup) {
        List<Connection> connectionsOnBlock = new ArrayList<>();
        rootBlockGroup.updateAllConnectorLocations();
        // Move this block before trying to bump others
        Connection prev = currentBlock.getPreviousConnection();
        if (prev != null && !prev.isConnected()) {
            bumpInferior(rootBlockGroup, prev);
        }
        Connection out = currentBlock.getOutputConnection();
        if (out != null && !out.isConnected()) {
            bumpInferior(rootBlockGroup, out);
        }

        currentBlock.getAllConnections(connectionsOnBlock);
        for (int i = 0; i < connectionsOnBlock.size(); i++) {
            Connection conn = connectionsOnBlock.get(i);
            if (conn.isHighPriority()) {
                if (conn.isConnected()) {
                    bumpNeighbours(conn.getTargetBlock(), rootBlockGroup);
                }
                bumpConnectionNeighbours(conn, rootBlockGroup);
            }
        }
    }

    /**
     * Bump the block containing {@code lowerPriority} away from the first nearby block it finds.
     *
     * @param rootBlockGroup The root block group of the block being bumped.
     * @param lowerPriority The low priority connection that is the center of the current bump
     * operation.
     */
    private void bumpInferior(BlockGroup rootBlockGroup, Connection lowerPriority) {
        getBumpableNeighbours(lowerPriority, mNeighbouringConnections);
        // Bump from the first one that isn't in the same block group.
        for (int j = 0; j < mNeighbouringConnections.size(); j++) {
            Connection curNeighbour = mNeighbouringConnections.get(j);
            if (mHelper.getRootBlockGroup(curNeighbour.getBlock()) != rootBlockGroup) {
                mController.bumpBlock(curNeighbour, lowerPriority);
                return;
            }
        }
    }

    /**
     * Find all connections near a given connection and bump their blocks away.
     *
     * @param conn The high priority connection that is at the center of the current bump
     * operation.
     * @param rootBlockGroup The root block group of the block conn belongs to.
     */
    private void bumpConnectionNeighbours(Connection conn, BlockGroup rootBlockGroup) {
        getBumpableNeighbours(conn, mNeighbouringConnections);
        for (int j = 0; j < mNeighbouringConnections.size(); j++) {
            Connection curNeighbour = mNeighbouringConnections.get(j);
            BlockGroup neighbourBlockGroup = mHelper.getRootBlockGroup(
                    curNeighbour.getBlock());
            if (neighbourBlockGroup != rootBlockGroup) {
                mController.bumpBlock(conn, curNeighbour);
            }
        }
    }

    private Pair<Connection, Connection> findBestConnection(Block block) {
        return mConnectionManager.findBestConnection(block, mHelper.getMaxSnapDistance());
    }

    private void getBumpableNeighbours(Connection conn, List<Connection> result) {
        int snapDistance = mHelper.getMaxSnapDistance();
        mConnectionManager.getNeighbours(conn, snapDistance, result);
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({TOUCH_STATE_NONE, TOUCH_STATE_DOWN, TOUCH_STATE_DRAGGING, TOUCH_STATE_LONGPRESS})
    public @interface TouchState {}
}
