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
    private static final int MAX_SNAP_DISTANCE = 50;

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
     * Splice a block or group of blocks between a "previous" and a "next" connection.
     *
     * @param previousBlock The {@link Block} whose next connection we are interested in.
     * @param nextBlock The {@link Block} whose previous connection we are interested in.
     * @param dragBlock The root {@link Block} of the {@link BlockGroup} to be spliced in.
     */
    private void insertBetweenPreviousNext(Block previousBlock, Block nextBlock, Block dragBlock) {
        previousBlock.getNextConnection().disconnect();
        previousBlock.getNextConnection().connect(dragBlock.getPreviousConnection());

        BlockGroup toBlockGroup = mWorkspaceHelper.getNearestParentBlockGroup(previousBlock);
        BlockGroup tempBlockGroup = toBlockGroup.extractBlocksAsNewGroup(nextBlock);
        toBlockGroup.moveBlocksFrom(mDragGroup, dragBlock);
        // moveBlocksFrom walks the list of next connections, so don't reconnect this until after
        // moving the blocks between groups.
        nextBlock.getPreviousConnection().connect(toBlockGroup.lastChildBlock().getNextConnection());
        // For the same reason, don't add the last group of blocks back until the connections have
        // been sewn up.
        toBlockGroup.moveBlocksFrom(tempBlockGroup, nextBlock);
    }

    /**
     * Splice a block or group of blocks between an "input" and an "output" connection.
     *
     * @param previousInputView The {@link InputView} of the relevant input on the previous block.
     * @param moving The {@link Connection} that is in mid-drag. Must be an output connection.
     * @param candidate The input {@link Connection} on the previous block that we want to attach to.
     */
    private void insertBetweenInputOutput(InputView previousInputView, Connection moving,
                                          Connection candidate) {
        // Disconnect the blocks we want to splice between, but keep references to them so we can
        // reconnect them after the splice.
        BlockGroup child = (BlockGroup) previousInputView.getChildView();
        Connection nextConnection = candidate.getTargetConnection();
        candidate.disconnect();
        previousInputView.unsetChildView();

        // Connect the moving block to the previous block and reattach views.
        candidate.connect(moving);
        previousInputView.setChildView(mDragGroup);

        // Connect the moving block/group to the next block and reattach views.
        // We may be splicing in a single block or a group of blocks, so find the last connection.
        // If the last block has no inputs or multiple inputs, bump away instead of reconnecting.
        Input dragBlockInput = mDragGroup.lastChildBlock().getOnlyValueInput();
        if (dragBlockInput != null && dragBlockInput.getConnection() != null) {
            nextConnection.connect(dragBlockInput.getConnection());
            dragBlockInput.getView().setChildView(child);
        }
        // TODO (fenichel): Otherwise bump and add to root blocks/views
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
        mRootBlocks.remove(dragRoot);
        mWorkspaceView.removeView(mDragGroup);
        boolean addToRoot = false;
        boolean connectPrimary = true;

        Block targetBlock;
        BlockGroup child;
        // TODO (fenichel): allow dragging a block between two connected blocks.
        switch (primary.getType()) {
            case Connection.CONNECTION_TYPE_OUTPUT:
                InputView previousInputView = target.getInput().getView();
                if (target.isConnected()) {
                    insertBetweenInputOutput(previousInputView, primary, target);
                    connectPrimary = false;
                } else {
                    previousInputView.setChildView(mDragGroup);
                }
                break;
            case Connection.CONNECTION_TYPE_PREVIOUS:
                if (target.getInput() != null
                        && target.getInput().getType() == Input.TYPE_STATEMENT) {
                    // Connecting to a statement input.
                    // TODO (fenichel): Handle case where target is already connected here.
                    target.getInput().getView().setChildView(mDragGroup);
                } else {
                    // Connecting to a next.
                    if (target.isConnected()) {
                        insertBetweenPreviousNext(target.getBlock(), target.getBlock().getNextBlock(),
                                dragRoot);
                        connectPrimary = false;
                    } else {
                        BlockGroup parent = mWorkspaceHelper.getNearestParentBlockGroup(target.getBlock());
                        parent.moveBlocksFrom(mDragGroup, dragRoot);
                    }
                }
                break;
            case Connection.CONNECTION_TYPE_NEXT:
                targetBlock = target.getBlock();
                child = mWorkspaceHelper.getNearestParentBlockGroup(targetBlock);
                if (!target.isConnected() && child.getParent() instanceof WorkspaceView) {
                    // The block we are connecting to is a root block.
                    mWorkspaceView.removeView(child);
                    mRootBlocks.remove(targetBlock);
                }
                // All of the blocks in a chain of previous/next connections live in the same
                // {@link BlockGroup}
                if (primary.getInput() != null
                        && primary.getInput().getType() == Input.TYPE_STATEMENT) {
                    primary.getInput().getView().setChildView(child);
                    addToRoot = true;
                } else {
                    // Connecting to a previous
                     if (target.isConnected()) {
                        insertBetweenPreviousNext(target.getBlock().getPreviousBlock(), target.getBlock(),
                                dragRoot);
                        connectPrimary = false;
                        addToRoot = false;
                    } else {
                        mDragGroup.moveBlocksFrom(child, targetBlock);
                        addToRoot = true;
                    }
                }
                break;
            case Connection.CONNECTION_TYPE_INPUT:
                targetBlock = target.getBlock();
                child = mWorkspaceHelper.getNearestParentBlockGroup(targetBlock);
                // Could may be also just check if the output is occupied
                if (child.getParent() instanceof WorkspaceView) {
                    // The block we are connecting to is a root block.
                    mWorkspaceView.removeView(child);
                    mRootBlocks.remove(targetBlock);
                }
                primary.getInput().getView().setChildView(child);
                addToRoot = true;
                break;
            default:
                return;
        }

        // If the connection on the dragged block was an input or a next it's possible that the
        // block is still a top level block.  If so, add it back to the workspace and view.
        if (addToRoot) {
            mRootBlocks.add(dragRoot);
            mWorkspaceView.addView(mDragGroup);
        }

        // Primary may have been connected during a helper method; don't connect it twice.
        if (connectPrimary) {
            primary.connect(target);
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
