/*
 *  Copyright  2015 Google Inc. All Rights Reserved.
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

package com.google.blockly.control;

import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;

import com.google.blockly.model.Block;
import com.google.blockly.model.Connection;
import com.google.blockly.model.Input;
import com.google.blockly.model.WorkspacePoint;
import com.google.blockly.ui.BlockGroup;
import com.google.blockly.ui.BlockView;
import com.google.blockly.ui.ViewPoint;
import com.google.blockly.ui.WorkspaceHelper;
import com.google.blockly.ui.WorkspaceView;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for dragging blocks and groups of blocks within a workspace.
 */
public class Dragger {
    private static final String TAG = "Dragger";

    // Blocks "snap" toward each other at the end of drags if they have compatible connections
    // near each other.  This is the farthest they can snap.
    // Units: Pixels.  TODO: Load from resources, value in dips.
    private static final int MAX_SNAP_DISTANCE = 50;

    private final ViewPoint mDragStart = new ViewPoint();
    private final ViewPoint mDragIntermediate = new ViewPoint();
    private final ConnectionManager mConnectionManager;
    private final ArrayList<Block> mRootBlocks;
    private final ArrayList<Connection> mTempConnections = new ArrayList<>();

    private BlockView mTouchedBlockView;
    private WorkspaceHelper mWorkspaceHelper;
    private WorkspaceView mWorkspaceView;
    private BlockGroup mDragGroup;

    public boolean onTouch(View v, MotionEvent event) {
        int eventX = (int) event.getRawX();
        int eventY = (int) event.getRawY();
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            // TODO (fenichel): Don't start a drag until the user has passed some threshold.
            mTouchedBlockView = (BlockView) v;
            mDragStart.set(eventX, eventY);
            mDragIntermediate.set(mDragStart.x, mDragStart.y);
            setDragGroup(mTouchedBlockView.getBlock());
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            // TODO (fenichel): Make this not fail for slow drags.
            moveBlock(mTouchedBlockView.getBlock(), eventX - mDragIntermediate.x,
                    eventY - mDragIntermediate.y);
            mDragIntermediate.set(eventX, eventY);
            v.requestLayout();
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            if (!snapToConnection(mTouchedBlockView.getBlock())) {
                moveBlock(mTouchedBlockView.getBlock(), eventX - mDragIntermediate.x,
                        eventY - mDragIntermediate.y);
                finalizeMove();
            }
            return true;
        }
        // TODO (fenichel): Handle ACTION_CANCEL.
        return false;
    }

    /**
     * @param workspaceHelper For use in computing workspace coordinates.
     * @param workspaceView The root view to add block groups to.
     * @param connectionManager The {@link ConnectionManager} to update when moving connections.
     * @param rootBlocks The list of blocks to update when moving blocks.
     */
    public Dragger(WorkspaceHelper workspaceHelper, WorkspaceView workspaceView,
                   ConnectionManager connectionManager, ArrayList<Block> rootBlocks) {
        mWorkspaceHelper = workspaceHelper;
        mWorkspaceView = workspaceView;
        mConnectionManager = connectionManager;
        mRootBlocks = rootBlocks;
    }

    public void setWorkspaceHelper(WorkspaceHelper helper) {
        mWorkspaceHelper = helper;
    }

    public void setWorkspaceView(WorkspaceView view) {
        mWorkspaceView = view;
    }

    private void setDragGroup(Block block) {
        BlockView bv = block.getView();
        BlockGroup rootBlockGroup = mWorkspaceHelper.getRootBlockGroup(block);
        BlockGroup bg = (BlockGroup) bv.getParent();
        WorkspacePoint realPosition = new WorkspacePoint();
        mWorkspaceHelper.getWorkspaceCoordinates(bv, realPosition);
        if (!mRootBlocks.contains(block)) {
            // Child block
            if (block.getPreviousConnection() != null
                    && block.getPreviousConnection().isConnected()) {
                Input in = block.getPreviousConnection().getTargetConnection().getInput();
                if (in == null) {
                    // Next block
                    bg = bg.extractBlocksAsNewGroup(block);
                } else {
                    // Statement input
                    in.getView().unsetChildView();
                }
                block.getPreviousConnection().disconnect();
            } else if (block.getOutputConnection() != null
                    && block.getOutputConnection().isConnected()) {
                // Value input
                Input in = block.getOutputConnection().getTargetConnection().getInput();
                in.getView().unsetChildView();
                block.getOutputConnection().disconnect();
            }
            rootBlockGroup.requestLayout();
            mWorkspaceView.addView(bg);
            mRootBlocks.add(block);
        }
        block.setPosition(realPosition.x, realPosition.y);
        mDragGroup = bg;
        mDragGroup.bringToFront();

        mTempConnections.clear();
        // Don't track any of the connections that we're dragging around.
        block.getAllConnectionsRecursive(mTempConnections);
        for (int i = 0; i < mTempConnections.size(); i++) {
            mConnectionManager.removeConnection(mTempConnections.get(i));
            mTempConnections.get(i).setDragMode(true);
        }
    }

    /**
     * Function to call in an onTouchListener to move the given block.
     * <p>
     * All of the child blocks move with the root block based on its position during layout.
     *
     * @param block The block to move.
     * @param dx How far to move in the x direction.
     * @param dy How far to move in the y direction.
     */
    private void moveBlock(Block block, int dx, int dy) {
        dx = mWorkspaceHelper.viewToWorkspaceUnits(dx);
        dy = mWorkspaceHelper.viewToWorkspaceUnits(dy);
        block.setPosition(block.getPosition().x + dx, block.getPosition().y + dy);
        mDragGroup.requestLayout();
    }

    /**
     * Iterate over all of the connections on the block and find the one that is closest to a
     * valid connection on another block.
     *
     * @param block The {@link Block} whose connections to search.
     * @return A pair of connections, where the first is a connection on {@code block} and the
     *  second is the closest compatible connection.
     */
    private Pair<Connection, Connection> findBestConnection(Block block) {
        // Find the connection that is closest to any connection on the block.
        Connection draggedBlockConnection = null;
        Connection compatibleConnection = null;
        double radiusConnection = MAX_SNAP_DISTANCE;
        List<Connection> blockConnections = block.getAllConnections();
        Connection curDraggedBlockConnection;
        Connection curCompatibleConnection;

        for (int i = 0; i < blockConnections.size(); i++) {
            curDraggedBlockConnection = blockConnections.get(i);
            curCompatibleConnection =
                    mConnectionManager.closestConnection(curDraggedBlockConnection,
                            radiusConnection);
            if (curCompatibleConnection != null) {
                draggedBlockConnection = curCompatibleConnection;
                compatibleConnection = curDraggedBlockConnection;
                radiusConnection = draggedBlockConnection.distanceFrom(compatibleConnection);
            }
        }
        if (draggedBlockConnection == null) {
            return null;
        }
        return new Pair<>(compatibleConnection, draggedBlockConnection);
    }

    private boolean snapToConnection(Block block) {
        Pair<Connection, Connection> connectionCandidates = findBestConnection(block);
        if (connectionCandidates == null) {
            return false;
        }

        // TODO (fenichel): Shouldn't actually need to set the position unless it's staying as a
        // root block.  Otherwise it will be derived from the position of the blocks above during
        // layout.
        int dx = connectionCandidates.first.getPosition().x
                - connectionCandidates.second.getPosition().x;
        int dy = connectionCandidates.first.getPosition().y
                - connectionCandidates.second.getPosition().y;
        block.setPosition(mWorkspaceHelper.viewToWorkspaceUnits(block.getPosition().x + dx),
                mWorkspaceHelper.viewToWorkspaceUnits(block.getPosition().y + dy));

        reconnectViews(connectionCandidates.first, connectionCandidates.second, block);
        finalizeMove();
        return true;
    }

    /**
     * Once the closest connection has been found, call this method to remove the views that are
     * being dragged from the root workspace view and reattach them in the correct places in the
     * view hierarchy, to match the new model.
     *
     * @param primary The connection on the block being moved.
     * @param target The closest compatible connection to primary.
     * @param dragRoot The {@link Block} that is the root of the group of blocks being dragged
     * around.
     */
    private void reconnectViews(Connection primary, Connection target, Block dragRoot) {
        primary.connect(target);
        mRootBlocks.remove(dragRoot);
        mWorkspaceView.removeView(mDragGroup);

        Block targetBlock;
        BlockGroup child;
        // TODO (fenichel): allow dragging a block between two connected blocks.
        switch (primary.getType()) {
            case Connection.CONNECTION_TYPE_OUTPUT:
                target.getInput().getView().setChildView(mDragGroup);
                break;
            case Connection.CONNECTION_TYPE_PREVIOUS:
                if (target.getInput() != null
                        && target.getInput().getType() == Input.TYPE_STATEMENT) {
                    // Connecting to a statement input.
                    target.getInput().getView().setChildView(mDragGroup);
                } else {
                    // Connecting to a next.
                    BlockGroup parent = mWorkspaceHelper.getNearestParentBlockGroup(target.getBlock());
                    parent.moveBlocksFrom(mDragGroup, dragRoot);
                }
                break;
            case Connection.CONNECTION_TYPE_NEXT:
                // Connecting to a previous
                targetBlock = target.getBlock();
                child = mWorkspaceHelper.getNearestParentBlockGroup(targetBlock);
                if (child.getParent() instanceof WorkspaceView) {
                    // The block we are connecting to is a root block.
                    mWorkspaceView.removeView(child);
                    mRootBlocks.remove(targetBlock);
                }
                // All of the blocks in a chain of previous/next connections live in the same
                // {@link BlockGroup}
                mDragGroup.moveBlocksFrom(child, targetBlock);
                mRootBlocks.add(dragRoot);
                mWorkspaceView.addView(mDragGroup);
                break;
            case Connection.CONNECTION_TYPE_INPUT:
                // TODO (fenichel): Implement, with special case for statment inputs.
                break;
            default:
                return;
        }
        // Update the drag group so that everything that has been changed will be properly
        // invalidated.
        mDragGroup = mWorkspaceHelper.getRootBlockGroup(target.getBlock());
    }

    /**
     * Update the positions of all of the connections that were impacted by the move and add them
     * back to the manager.
     */
    private void finalizeMove() {
        mTempConnections.clear();
        mTouchedBlockView.getBlock().getAllConnectionsRecursive(mTempConnections);
        Connection cur;
        // All of the connection locations will be set relative to their block views immediately
        // after this loop.  For now we just want to unset drag mode and add the connections back
        // to the list; 0, 0 is a cheap place to put them.
        for (int i = 0; i < mTempConnections.size(); i++) {
            cur = mTempConnections.get(i);
            cur.setPosition(0, 0);
            cur.setDragMode(false);
            mConnectionManager.addConnection(cur);
        }
        mDragGroup.updateAllConnectorLocations();
        mDragGroup.requestLayout();
    }
}
