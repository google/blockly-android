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
import android.support.v4.view.MotionEventCompat;
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
import java.util.List;

/**
 * Controller for dragging blocks and groups of blocks within a workspace.
 */
public class Dragger {
    private static final String TAG = "Dragger";

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

    private final ViewPoint mDragStart = new ViewPoint();
    private final ViewPoint mDragOffset = new ViewPoint();
    private final WorkspacePoint mBlockOriginalPosition = new WorkspacePoint();

    private final ConnectionManager mConnectionManager;
    private final ArrayList<Connection> mDraggedConnections = new ArrayList<>();
    // For use in bumping neighbours; instance variable only to avoid repeated allocation.
    private final ArrayList<Connection> mNeighbouringConnections = new ArrayList<>();
    // Rect for finding the bounding box of the trash can view.
    private final Rect mTrashRect = new Rect();
    // For use in getting location on screen.
    private final int[] mTempArray = new int[2];
    private final ViewPoint mTempViewPoint = new ViewPoint();
    private final BlocklyController mController;
    // Which {@link BlockView} was touched, and possibly may be being dragged.
    private BlockView mTouchedBlockView;
    private Workspace mWorkspace;
    private WorkspaceHelper mWorkspaceHelper;
    private WorkspaceView mWorkspaceView;
    private BlockGroup mDragGroup;
    private BlockView mHighlightedBlockView;
    // The view for the trash can.
    private View mTrashView;
    //The square of the required touch slop before starting a drag, precomputed to avoid
    // square root operations at runtime.
    private float mTouchSlopSquared = 0.0f;

    // Current state of touch interaction with blocks in this workspace view.
    @TouchState
    private int mTouchState = TOUCH_STATE_NONE;

    private boolean mIsDragging = false;
    private int mDraggingPointerId = MotionEvent.INVALID_POINTER_ID;

    private View.OnDragListener mDragEventListener = new View.OnDragListener() {
        @Override
        public boolean onDrag(View v, DragEvent event) {
            final int action = event.getAction();
            switch (action) {
                case DragEvent.ACTION_DRAG_STARTED:
                    startDragInternal();
                    return true;    // We want to keep listening for drag events
                case DragEvent.ACTION_DRAG_LOCATION:
                    continueDragging(event);
                    break;
                case DragEvent.ACTION_DRAG_ENDED:
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
                        mIsDragging = false;
                        setTouchedBlock(null, MotionEvent.INVALID_POINTER_ID);
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
     * @param blocklyController
     */
    public Dragger(BlocklyController blocklyController) {
        mController = blocklyController;
        mWorkspace = blocklyController.getWorkspace();
        mWorkspaceHelper = blocklyController.getWorkspaceHelper();
        mConnectionManager = mWorkspace.getConnectionManager();
    }

    /**
     * @param slop The required touch slop before starting a drag.
     */
    public void setTouchSlop(float slop) {
        mTouchSlopSquared = slop * slop;
    }

    /**
     * Tells the drag and drop framework to start a drag, but does not actually make any changes to
     * models or views.
     */
    public void startDragging() {
        ((View) mTouchedBlockView).startDrag(
                ClipData.newPlainText(WorkspaceView.BLOCK_GROUP_CLIP_DATA_LABEL, ""),
                new View.DragShadowBuilder(), null, 0);
    }

    /**
     * Forcefully sets the start position of the next drag event.  Useful when dragging out of the
     * toolbox and doing coordinate conversions. If the start position was generated by a {@link
     * MotionEvent} it needs to be offset to the {@link WorkspaceView}'s coordinates.
     *
     * @param startX The starting x position of the drag.
     * @param startY The starting y position of the drag
     * @param offsetX The x offset from the original {@link MotionEvent} to the {@link
     * WorkspaceView}.
     * @param offsetY The y offset from the original {@link MotionEvent} to the {@link
     * WorkspaceView}.
     */
    public void setDragStartPos(int startX, int startY, int offsetX, int offsetY) {
        mDragStart.set(startX + offsetX, startY + offsetY);
        mDragOffset.set(offsetX, offsetY);
    }

    /**
     * Start dragging a block in the workspace.
     * <p/>
     * This method separates the block to drag into its own {@link BlockGroup} and sets the initial
     * dragging position. It must be called before any calls to {@link
     * #continueDragging(DragEvent)}, but may not be called immediately on receiving a "down" event
     * (e.g., to first wait for a minimum drag distance).
     */
    private void startDragInternal() {
        mIsDragging = true;
        mTouchState = TOUCH_STATE_DRAGGING;

        mBlockOriginalPosition.setFrom(mTouchedBlockView.getBlock().getPosition());
        setDragGroup(mTouchedBlockView.getBlock());
    }

    /**
     * Sets the block view currently being touched, and marks it 'pressed'.
     *
     * @param blockView The {@link BlockView} that will be dragged.
     * @param event The {@link MotionEvent} corresponding to the touch.
     */
    public void setTouchedBlock(BlockView blockView, MotionEvent event) {
        setTouchedBlock(blockView,
                MotionEventCompat.getPointerId(event, MotionEventCompat.getActionIndex(event)));
    }

    /**
     * Sets the touched block if a drag is not in progress.
     *
     * @param blockView The {@link BlockView} that has been touched and may be dragged.
     * @param pointerId The id of the pointer that touched the {@link BlockView}.
     */
    public void setTouchedBlock(BlockView blockView, int pointerId) {
        View view = (View) blockView;
        if (mIsDragging) {
            return;
        }
        if (mTouchedBlockView != null) {
            ((View) mTouchedBlockView).setPressed(false);
        }
        mTouchedBlockView = blockView;
        if (mTouchedBlockView != null) {
            // Starting a new drag.
            mTouchState = TOUCH_STATE_DOWN;
            view.setPressed(true);
        } else {
            // Just clearing old state.
            mTouchState = TOUCH_STATE_NONE;
        }
        mDraggingPointerId = pointerId;
    }

    /**
     * Continue dragging the currently moving block.
     * <p/>
     * This method must be called for each drag event that is received by the {@link WorkspaceView}
     * after {@link #startDragInternal()} (BlockView, int, int)} has previously been called.
     *
     * @param event The next drag event to handle, as received by the {@link WorkspaceView}.
     */
    public void continueDragging(DragEvent event) {
        updateBlockPosition(event);

        // highlight as we go
        if (mHighlightedBlockView != null) {
            mHighlightedBlockView.setHighlightedConnection(null);
        }
        Pair<Connection, Connection> connectionCandidate =
                findBestConnection(mTouchedBlockView.getBlock());
        if (connectionCandidate != null) {
            mHighlightedBlockView = mWorkspaceHelper.getView(connectionCandidate.second.getBlock());
            mHighlightedBlockView.setHighlightedConnection(connectionCandidate.second);
        }

        ((View) mTouchedBlockView).requestLayout();
    }

    /**
     * Finish block dragging.
     * <p/>
     * This method must be called upon receiving the "up" event that ends an ongoing drag process.
     */
    public void finishDragging() {
        if (!snapToConnection(mTouchedBlockView.getBlock())) {
            finalizeMove();
        }
        mIsDragging = false;
    }

    public void setWorkspaceView(WorkspaceView view) {
        mWorkspaceView = view;
    }

    public void setTrashView(View trashView) {
        mTrashView = trashView;
    }

    /**
     * Let the Dragger know that a block was touched. This will be called when the block has been
     * touched but a drag has not yet been started.
     *
     * @param blockView The {@link BlockView} that detected a touch event.
     * @param event The touch event.
     * @return true if the WorkspaceView has started dragging the given {@link BlockView} or
     * recorded it as draggable.
     */
    public boolean onTouchBlock(BlockView blockView, MotionEvent event) {
        final int action = MotionEventCompat.getActionMasked(event);
        // Handle the case when the user releases before moving far enough to start a drag.
        if ((action == MotionEvent.ACTION_UP && !mIsDragging)
                && mTouchedBlockView == blockView) {
            setTouchedBlock(null, MotionEvent.INVALID_POINTER_ID);
            return true;
        }

        if (action == MotionEvent.ACTION_DOWN) {
            setTouchedBlock(blockView, event);
            handleActionDown(blockView, event);
            return true;
        }
        if ((mTouchState == TOUCH_STATE_DOWN || mTouchState == TOUCH_STATE_DRAGGING)
                && action == MotionEvent.ACTION_MOVE && mTouchedBlockView == blockView) {
            maybeStartDrag(event);
            return true;
        }
        return false;
    }

    /**
     * Let this instance know that a block was touched. This will be called when an editable field
     * on the block has been touched but a drag has not yet been started.
     *
     * @param blockView The {@link BlockView} that detected a touch event.
     * @param motionEvent The touch event.
     * @return true if a drag has been started, false otherwise.
     */
    public boolean onInterceptTouchEvent(BlockView blockView, MotionEvent motionEvent) {
        final int action = MotionEventCompat.getActionMasked(motionEvent);
        if (action == MotionEvent.ACTION_DOWN) {
            setTouchedBlock(blockView, motionEvent); // regardless of the idle state
            handleActionDown(blockView, motionEvent);
            return false;
        } else if (action == MotionEvent.ACTION_MOVE && mTouchedBlockView == blockView) {
            // If there was a move that didn't start a drag, and we were already on a field,
            // we don't want to intercept the motion event yet.
            return maybeStartDrag(motionEvent);
        } else if ((action == MotionEvent.ACTION_UP && !mIsDragging)
                && mTouchedBlockView == blockView) {
            setTouchedBlock(null, MotionEvent.INVALID_POINTER_ID);
            return false;
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
    private boolean maybeStartDrag(MotionEvent event) {
        final int pointerIdx = MotionEventCompat.findPointerIndex(event, mDraggingPointerId);
        if (mTouchState != TOUCH_STATE_DRAGGING) {
            // Not dragging yet - compute distance from Down event and start dragging if
            // far enough.
            final float deltaX = mDragStart.x -
                    (MotionEventCompat.getX(event, pointerIdx) + mDragOffset.x);
            final float deltaY = mDragStart.y -
                    (MotionEventCompat.getY(event, pointerIdx) + mDragOffset.y);

            if (deltaX * deltaX + deltaY * deltaY >= mTouchSlopSquared) {
                startDragging();
                return true;
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
    private void handleActionDown(BlockView blockView, MotionEvent event) {
        final int pointerIdx = MotionEventCompat.findPointerIndex(event,
                mDraggingPointerId);
        mWorkspaceHelper.getVirtualViewCoordinates((View) blockView, mTempViewPoint);
        int startX = (int) MotionEventCompat.getX(event, pointerIdx);
        int startY = (int) MotionEventCompat.getY(event, pointerIdx);
        setDragStartPos(startX, startY, mTempViewPoint.x, mTempViewPoint.y);
    }

    /**
     * Check whether the given event occurred on top of the trash can button.  Should be called from
     * {@link WorkspaceView}.
     *
     * @param event The event whose location should be checked, with position in WorkspaceView
     * coordinates.
     * @return Whether the event was on top of the trash can button.
     */
    private boolean touchingTrashView(DragEvent event) {
        mTrashView.getLocationOnScreen(mTempArray);
        mTrashView.getHitRect(mTrashRect);

        mTrashRect.offset((mTempArray[0] - mTrashRect.left), (mTempArray[1] - mTrashRect.top));
        // offset drag event positions by the workspace view's position on screen.
        mWorkspaceView.getLocationOnScreen(mTempArray);
        return mTrashRect.contains((int) event.getX() + mTempArray[0],
                (int) event.getY() + mTempArray[1]);
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
        mController.trashRootBlock(mTouchedBlockView.getBlock());
        mTouchedBlockView = null;
        mDragGroup = null;
    }

    private void setDragGroup(Block block) {
        mController.extractBlockAsRoot(block);
        mDragGroup = mWorkspaceHelper.getRootBlockGroup(block);
        mDragGroup.bringToFront();

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
        int dx = mWorkspaceHelper.virtualViewToWorkspaceUnits((int) (event.getX()) - mDragStart.x);
        int dy = mWorkspaceHelper.virtualViewToWorkspaceUnits((int) (event.getY()) - mDragStart.y);

        if (mWorkspaceHelper.useRtl()) {
            dx *= -1;
        }

        mTouchedBlockView.getBlock().
                setPosition(mBlockOriginalPosition.x + dx, mBlockOriginalPosition.y + dy);
        mDragGroup.requestLayout();
    }

    private boolean snapToConnection(Block block) {
        Pair<Connection, Connection> connectionCandidate = findBestConnection(block);
        if (connectionCandidate == null) {
            return false;
        }

        mController.connect(block, connectionCandidate.first, connectionCandidate.second);
        // Update the drag group so that everything that has been changed will be properly
        // invalidated.
        mDragGroup = mWorkspaceHelper.getRootBlockGroup(connectionCandidate.second.getBlock());

        finalizeMove();
        return true;
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
        BlockGroup rootBlockGroup = mWorkspaceHelper.getRootBlockGroup(
                mTouchedBlockView.getBlock());
        bumpNeighbours(mTouchedBlockView.getBlock(), rootBlockGroup);
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

        rootBlockGroup.requestLayout();
    }

    /**
     * Move all neighbours of the current block and its sub-blocks so that they don't appear to be
     * connected to the current block.
     *
     * @param currentBlock The {@link Block} to bump others away from.
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
            if (mWorkspaceHelper.getRootBlockGroup(curNeighbour.getBlock()) != rootBlockGroup) {
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
            BlockGroup neighbourBlockGroup = mWorkspaceHelper.getRootBlockGroup(
                    curNeighbour.getBlock());
            if (neighbourBlockGroup != rootBlockGroup) {
                mController.bumpBlock(conn, curNeighbour);
            }
        }
    }

    private Pair<Connection, Connection> findBestConnection(Block block) {
        return mConnectionManager.findBestConnection(block, mWorkspaceHelper.getMaxSnapDistance());
    }

    private void getBumpableNeighbours(Connection conn, List<Connection> result) {
        int snapDistance = mWorkspaceHelper.getMaxSnapDistance();
        mConnectionManager.getNeighbours(conn, snapDistance, result);
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({TOUCH_STATE_NONE, TOUCH_STATE_DOWN, TOUCH_STATE_DRAGGING, TOUCH_STATE_LONGPRESS})
    public @interface TouchState {
    }
}
