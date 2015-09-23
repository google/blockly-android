/*
 * Copyright  2015 Google Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.blockly.model;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.google.blockly.control.ProcedureManager;
import com.google.blockly.control.ConnectionManager;
import com.google.blockly.control.WorkspaceStats;
import com.google.blockly.ui.BlockGroup;
import com.google.blockly.ui.BlockView;
import com.google.blockly.ui.InputView;
import com.google.blockly.ui.ViewPoint;
import com.google.blockly.ui.WorkspaceHelper;
import com.google.blockly.ui.WorkspaceView;
import com.google.blockly.utils.BlocklyXmlHelper;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Keeps track of all the global state used in the workspace. This is mostly just blocks.
 */
public class Workspace {
    private static final String TAG = "Workspace";
    private static final boolean DEBUG = true;
    private static final BlocklyXmlHelper mXmlHelper = new BlocklyXmlHelper();

    private final ArrayList<Block> mRootBlocks = new ArrayList<>();
    private final ProcedureManager mProcedureManager = new ProcedureManager();
    private final NameManager mVariableNameManager = new NameManager.VariableNameManager();
    private final ConnectionManager mConnectionManager = new ConnectionManager();
    private final WorkspaceStats stats = new WorkspaceStats(mVariableNameManager, mProcedureManager,
            mConnectionManager);
    private final ViewPoint mDragStart = new ViewPoint(0, 0);
    private final ViewPoint mDragEnd = new ViewPoint(0, 0);
    private WorkspaceHelper mWorkspaceHelper;
    private BlockView mTouchedBlockView;

    private WorkspaceView mWorkspaceView;


    public final View.OnTouchListener mOnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                mTouchedBlockView = (BlockView) v;
                mDragStart.set((int) event.getX(), (int) event.getY());
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_UP) {
                mDragEnd.set((int) event.getX(), (int) event.getY());
                moveBlock(mTouchedBlockView.getBlock(), mDragEnd.x - mDragStart.x,
                        mDragEnd.y - mDragStart.y);
                v.requestLayout();
                v.invalidate();
                return true;
            }
            return false;
        }
    };

    public Workspace() {
    }

    /**
     * Adds a new block to the workspace as a root block.
     *
     * @param block
     */
    public void addRootBlock(Block block) {
        if (block == null) {
            throw new IllegalArgumentException("Cannot add a null block as a root block");
        }
        if (block.getPreviousBlock() != null) {
            throw new IllegalArgumentException("Root blocks may not have a previous block");
        }
        if (mRootBlocks.contains(block)) {
            throw new IllegalArgumentException("Block is already a root block.");
        }
        mRootBlocks.add(block);
        stats.collectStats(block, true);
    }

    public boolean removeRootBlock(Block block) {
        return mRootBlocks.remove(block);
    }

    public void connect(Connection a, Connection b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("Cannot connect a null connection.");
        }

        if (!a.canConnect(b)) {
            throw new IllegalArgumentException("Connections may not be connected.");
        }

        if (a.getType() == Connection.CONNECTION_TYPE_PREVIOUS) {
            if (removeRootBlock(a.getBlock()) && DEBUG) {
                Log.d(TAG, "Removed root block before connecting it.");
            }
        } else if (b.getType() == Connection.CONNECTION_TYPE_PREVIOUS) {
            if (removeRootBlock(b.getBlock()) && DEBUG) {
                Log.d(TAG, "Removed root block before connecting it.");
            }
        }

        a.connect(b);
    }

    public WorkspaceHelper getWorkspaceHelper() {
        return mWorkspaceHelper;
    }

    public void setWorkspaceHelper(WorkspaceHelper helper) {
        mWorkspaceHelper = helper;
    }

    /**
     * Reads the workspace in from an XML string.
     *
     * @param is The input stream to read from.
     * @throws BlocklyParserException
     */
    public void loadFromXml(InputStream is, BlockFactory blockFactory)
            throws BlocklyParserException {
        mRootBlocks.addAll(mXmlHelper.loadFromXml(is, blockFactory, stats));
        for (int i = 0; i < mRootBlocks.size(); i++) {
            stats.collectStats(mRootBlocks.get(i), true /* recursive */);
        }
    }

    /**
     * Outputs the workspace as an XML string.
     *
     * @param os The output stream to write to.
     * @throws BlocklySerializerException
     */
    public void serialize(OutputStream os) throws BlocklySerializerException {
        mXmlHelper.writeToXml(mRootBlocks, os);
    }

    /**
     * Recursively initialize views corresponding to every block in the model.
     *
     * @param wv      The root workspace view to add to.
     * @param context The activity context.
     */
    public void createViewsFromModel(WorkspaceView wv, Context context) {
        BlockGroup bg;
        mWorkspaceView = wv;
        for (int i = 0; i < mRootBlocks.size(); i++) {
            bg = new BlockGroup(context, mWorkspaceHelper);
            mWorkspaceHelper.obtainBlockView(mRootBlocks.get(i), bg, mOnTouchListener);
            mWorkspaceView.addView(bg);
        }
    }

    /**
     * Function to call in an onTouchListener to move the given block.
     * @param block The block to move.
     * @param dx How far to move in the x direction.
     * @param dy How far to move in the y direction.
     */
    private void moveBlock(Block block, int dx, int dy) {
        BlockView bv = block.getView();
        BlockGroup bg = (BlockGroup) bv.getParent();
        WorkspacePoint realPosition = new WorkspacePoint();
        mWorkspaceHelper.getWorkspaceCoordinates(bv, realPosition);
        if (!mRootBlocks.contains(block)) {
            // Child block
            if (block.getPreviousConnection() != null
                    && block.getPreviousConnection().isConnected()) {
                Input in = block.getPreviousConnection().getTargetConnection().getInput();
                InputView inv = in.getView();
                inv.removeView(bg);
                block.getPreviousConnection().disconnect();
            } else if (block.getOutputConnection() != null
                    && block.getOutputConnection().isConnected()) {
                // TODO(fenichel): make new blockgroups if this is not the first in a BG.
                Input in = block.getOutputConnection().getTargetConnection().getInput();
                InputView inv = in.getView();
                inv.removeView(bg);
                block.getOutputConnection().disconnect();
            }

            block.setPosition(realPosition.x, realPosition.y);
            mWorkspaceView.addView(bg);
            mRootBlocks.add(block);
        }

        List<Connection> connections = block.getAllConnections();
        dx = mWorkspaceHelper.viewToWorkspaceUnits(dx);
        dy = mWorkspaceHelper.viewToWorkspaceUnits(dy);
        // TODO (fenichel): Need to do this recursively.
        for (int i = 0; i < connections.size(); i++) {
            mConnectionManager.moveConnection(connections.get(i), dx, dy);
        }

        // TODO (fenichel):  What about moving the children?
        block.setPosition(block.getPosition().x + dx, block.getPosition().y + dy);
    }
}
