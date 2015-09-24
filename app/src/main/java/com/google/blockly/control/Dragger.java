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

import android.util.Log;
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

    private final ViewPoint mDragStart = new ViewPoint(0, 0);
    private final ConnectionManager mConnectionManager;
    private final ArrayList<Block> mRootBlocks;

    private BlockView mTouchedBlockView;
    private WorkspaceHelper mWorkspaceHelper;
    private WorkspaceView mWorkspaceView;
    private BlockGroup mDragGroup;

    private final View.OnTouchListener mOnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                mTouchedBlockView = (BlockView) v;
                mDragStart.set((int) event.getRawX(), (int) event.getRawY());
                setDragGroup(mTouchedBlockView.getBlock());
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_UP
                    || event.getAction() == MotionEvent.ACTION_MOVE) {
                moveBlock(mTouchedBlockView.getBlock(), (int) event.getRawX() - mDragStart.x,
                        (int) event.getRawY() - mDragStart.y);
                mDragStart.set((int) event.getRawX(), (int) event.getRawY());
                v.requestLayout();
                v.invalidate();
                return true;
            }
            return false;
        }
    };

    /**
     * @param workspaceHelper   For use in computing workspace coordinates.
     * @param workspaceView     The root view to add block groups to.
     * @param connectionManager
     * @param rootBlocks
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

    public View.OnTouchListener getOnTouchListener() {
        return mOnTouchListener;
    }

    private void setDragGroup(Block block) {
        BlockView bv = block.getView();
        BlockView parentView = bv;
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
                    BlockGroup newBlockGroup = new BlockGroup(bg.getContext(), mWorkspaceHelper);
                    Block cur = block;
                    while (cur != null) {
                        // Create a new block group and move blocks between groups.
                        bg.removeView(cur.getView());
                        newBlockGroup.addView(cur.getView());
                        cur = cur.getNextBlock();
                    }
                    bg = newBlockGroup;
                    parentView = block.getPreviousBlock().getView();
                } else {
                    // Statement input
                    in.getView().unsetChildView();
                    parentView = in.getBlock().getView();
                }
                block.getPreviousConnection().disconnect();
            } else if (block.getOutputConnection() != null
                    && block.getOutputConnection().isConnected()) {
                // Value input
                Input in = block.getOutputConnection().getTargetConnection().getInput();
                in.getView().unsetChildView();
                parentView = in.getBlock().getView();
                block.getOutputConnection().disconnect();
            }
            parentView.requestLayout();
            parentView.invalidate();
            mWorkspaceView.addView(bg);
            mRootBlocks.add(block);
        }
        block.setPosition(realPosition.x, realPosition.y);
        mDragGroup = bg;
    }

    /**
     * Function to call in an onTouchListener to move the given block.
     *
     * @param block The block to move.
     * @param dx    How far to move in the x direction.
     * @param dy    How far to move in the y direction.
     */
    private void moveBlock(Block block, int dx, int dy) {
        List<Connection> connections = block.getAllConnections();
        dx = mWorkspaceHelper.viewToWorkspaceUnits(dx);
        dy = mWorkspaceHelper.viewToWorkspaceUnits(dy);
        // TODO (fenichel): Need to do this recursively.
        for (int i = 0; i < connections.size(); i++) {
            mConnectionManager.moveConnection(connections.get(i), dx, dy);
        }

        // TODO (fenichel):  What about moving the children?
        block.setPosition(block.getPosition().x + dx, block.getPosition().y + dy);
        mDragGroup.requestLayout();
        mDragGroup.invalidate();
    }
}
