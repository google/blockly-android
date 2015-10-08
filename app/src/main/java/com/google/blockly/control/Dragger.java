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
import com.google.blockly.ui.InputView;
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
    private static final int MAX_SNAP_DISTANCE = 25;

    private final ViewPoint mDragStart = new ViewPoint();
    private final WorkspacePoint mBlockOriginalPosition = new WorkspacePoint();

    private final ConnectionManager mConnectionManager;
    private final ArrayList<Block> mRootBlocks;
    private final ArrayList<Connection> mDraggedConnections = new ArrayList<>();

    private BlockView mTouchedBlockView;
    private WorkspaceHelper mWorkspaceHelper;
    private WorkspaceView mWorkspaceView;
    private BlockGroup mDragGroup;

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

    public boolean onTouch(View v, MotionEvent event) {
        int eventX = (int) event.getRawX();
        int eventY = (int) event.getRawY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                // TODO (fenichel): Don't start a drag until the user has passed some threshold.
                mTouchedBlockView = (BlockView) v;
                mBlockOriginalPosition.setFrom(((BlockView) v).getBlock().getPosition());
                mDragStart.set(eventX, eventY);
                setDragGroup(mTouchedBlockView.getBlock());
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                updateBlockPosition(mTouchedBlockView.getBlock(),
                        mWorkspaceHelper.viewToWorkspaceUnits(eventX - mDragStart.x),
                        mWorkspaceHelper.viewToWorkspaceUnits(eventY - mDragStart.y));
                v.requestLayout();
                return true;
            }
            case MotionEvent.ACTION_UP: {
                if (!snapToConnection(mTouchedBlockView.getBlock())) {
                    finalizeMove();
                }
                return true;
            }
            // TODO (fenichel): Handle ACTION_CANCEL.
            default:
        }

        return false;
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
        mDragGroup = bg;
        mDragGroup.bringToFront();

        mDraggedConnections.clear();
        // Don't track any of the connections that we're dragging around.
        block.getAllConnectionsRecursive(mDraggedConnections);
        for (int i = 0; i < mDraggedConnections.size(); i++) {
            mConnectionManager.removeConnection(mDraggedConnections.get(i));
            mDraggedConnections.get(i).setDragMode(true);
        }
    }

    /**
     * Function to call in an onTouchListener to move the given block relative to its original
     * position at the beginning of the drag sequence.
     * <p/>
     * All of the child blocks move with the root block based on its position during layout.
     *
     * @param block The block whose position to update.
     * @param dx Distance in the x direction from the original block position.
     * @param dy Distance in the y direction from the original block position.
     */
    private void updateBlockPosition(Block block, int dx, int dy) {
        block.setPosition(mBlockOriginalPosition.x + dx, mBlockOriginalPosition.y + dy);
        mDragGroup.requestLayout();
    }

    /**
     * Iterate over all of the connections on the block and find the one that is closest to a
     * valid connection on another block.
     *
     * @param block The {@link Block} whose connections to search.
     * @return A pair of connections, where the first is a connection on {@code block} and the
     * second is the closest compatible connection.
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
        // The block and its group lived at the root level while dragging, but may cease to be a
        // top level block after being dragged, e.g. when connecting to the "Next" connection
        // of another block in the workspace.
        switch (primary.getType()) {
            case Connection.CONNECTION_TYPE_OUTPUT:
                mRootBlocks.remove(dragRoot);
                mWorkspaceView.removeView(mDragGroup);
                connectAsChild(target, primary);
                break;
            case Connection.CONNECTION_TYPE_PREVIOUS:
                mRootBlocks.remove(dragRoot);
                mWorkspaceView.removeView(mDragGroup);
                if (target.isStatementInput()) {
                    connectToStatement(target, primary.getBlock());
                } else {
                    connectAfter(primary.getBlock(), target.getBlock());
                }
                break;
            case Connection.CONNECTION_TYPE_NEXT:
                // target might be a root block
                if (!target.isConnected()) {
                    removeFromRoot(target.getBlock());
                }
                if (primary.isStatementInput()) {
                    connectToStatement(primary, target.getBlock());
                } else {
                    connectAfter(target.getBlock(), primary.getBlock());
                }
                break;
            case Connection.CONNECTION_TYPE_INPUT:
                // target might be a root block
                if (!target.isConnected()) {
                    removeFromRoot(target.getBlock());
                }
                connectAsChild(primary, target);
                break;
            default:
                return;
        }

        // Update the drag group so that everything that has been changed will be properly
        // invalidated.
        mDragGroup = mWorkspaceHelper.getRootBlockGroup(target.getBlock());
    }


    private void connectToStatement(Connection parentStatementConnection, Block toConnect) {
        // If there was already a block connected there.
        if (parentStatementConnection.isConnected()) {
            Block remainderBlock = parentStatementConnection.getTargetBlock();
            parentStatementConnection.getInputView().unsetChildView();
            parentStatementConnection.disconnect();
            // We may be dragging multiple blocks.  Connect after the end of the group we are
            // dragging.
            Block lastBlockInGroup = mWorkspaceHelper.getNearestParentBlockGroup(toConnect)
                    .lastChildBlock();
            connectAfter(remainderBlock, lastBlockInGroup);
        }
        connectAsChild(parentStatementConnection, toConnect.getPreviousConnection());
    }

    private void connectAfter(Block inferior, Block superior) {
        // Assume that the inferior's previous connection is disconnected.
        // Assume that inferior's blockGroup doesn't currently live at the root level.
        BlockGroup superiorBlockGroup = mWorkspaceHelper.getNearestParentBlockGroup(superior);
        BlockGroup inferiorBlockGroup = mWorkspaceHelper.getNearestParentBlockGroup(inferior);

        // To splice between two blocks, just need another call to connectAfter.
        if (superior.getNextConnection().isConnected()) {
            Block remainderBlock = superior.getNextBlock();
            BlockGroup remainder = superiorBlockGroup.extractBlocksAsNewGroup(
                    remainderBlock);
            superior.getNextConnection().disconnect();
            // We may be dragging multiple blocks.  Connect after the end of the group we are
            // dragging.
            Block lastBlockInGroup = inferiorBlockGroup.lastChildBlock();
            // if lastBlockInGroup doesn't have a next connection, add back to the root level.
            connectAfter(remainderBlock, lastBlockInGroup, inferiorBlockGroup, remainder);
        }

        connectAfter(inferior, superior, superiorBlockGroup, inferiorBlockGroup);
    }

    private void connectAfter(Block inferior, Block superior, BlockGroup superiorBlockGroup,
                              BlockGroup inferiorBlockGroup) {
        // Assume that the superior's next connection is disconnected.
        superior.getNextConnection().connect(inferior.getPreviousConnection());
        superiorBlockGroup.moveBlocksFrom(inferiorBlockGroup, inferior);
    }

    private void connectAsChild(Connection parent, Connection child) {
        InputView parentInputView = parent.getInputView();
        if (parentInputView == null) {
            // What?  Return.
        }
        BlockGroup childBlockGroup = mWorkspaceHelper.getNearestParentBlockGroup(child.getBlock());

        if (parent.isConnected()) {
            Connection remainder = parent.getTargetConnection();
            parent.disconnect();
            parentInputView.unsetChildView();
            if (childBlockGroup.lastChildBlock() != null
                    && childBlockGroup.lastChildBlock().getOnlyValueInput() != null) {
                connectAsChild(childBlockGroup.lastChildBlock().getOnlyValueInput().getConnection(),
                        remainder);
            } else {
                // Add back to the root
            }
        }
        parent.connect(child);
        parentInputView.setChildView(childBlockGroup);
    }


    /**
     * Removes the given block and its view from the root view.  If it didn't live at the root level
     * do nothing.
     *
     * @param block The {@link Block} to look up and remove.
     * @return The parent {@BlockGroup}, guaranteed to not be a child of any view.
     */
    private BlockGroup removeFromRoot(Block block) {
        BlockGroup group = mWorkspaceHelper.getNearestParentBlockGroup(block);
        if (group.getParent() instanceof WorkspaceView) {
            // The block we are connecting to is a root block.
            mWorkspaceView.removeView(group);
            mRootBlocks.remove(block);
        }
        return group;
    }

    /**
     * Update the positions of all of the connections that were impacted by the move and add them
     * back to the manager.
     */
    private void finalizeMove() {
        // All of the connection locations will be set relative to their block views immediately
        // after this loop.  For now we just want to unset drag mode and add the connections back
        // to the list; 0, 0 is a cheap place to put them.
        for (int i = 0; i < mDraggedConnections.size(); i++) {
            Connection cur = mDraggedConnections.get(i);
            cur.setPosition(0, 0);
            cur.setDragMode(false);
            mConnectionManager.addConnection(cur);
        }
        mDragGroup.requestLayout();
    }
}
