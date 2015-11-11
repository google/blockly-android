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
import android.view.MotionEvent;

import com.google.blockly.ToolboxFragment;
import com.google.blockly.TrashFragment;
import com.google.blockly.control.BlockCopyBuffer;
import com.google.blockly.control.ConnectionManager;
import com.google.blockly.control.Dragger;
import com.google.blockly.control.ProcedureManager;
import com.google.blockly.control.WorkspaceStats;
import com.google.blockly.ui.BlockGroup;
import com.google.blockly.ui.ViewPoint;
import com.google.blockly.ui.WorkspaceHelper;
import com.google.blockly.ui.WorkspaceView;
import com.google.blockly.utils.BlocklyXmlHelper;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * Controller for the workspace.  Keeps track of all the global state used in the workspace.
 */
public class Workspace {
    private static final String TAG = "Workspace";
    private static final boolean DEBUG = true;

    private final ArrayList<Block> mRootBlocks = new ArrayList<>();
    private final ProcedureManager mProcedureManager = new ProcedureManager();
    private final NameManager mVariableNameManager = new NameManager.VariableNameManager();
    private final ConnectionManager mConnectionManager = new ConnectionManager();
    private final WorkspaceStats stats = new WorkspaceStats(mVariableNameManager, mProcedureManager,
            mConnectionManager);
    private final ToolboxCategory mDeletedBlocks = new ToolboxCategory();
    private final BlockCopyBuffer mCopyBuffer = new BlockCopyBuffer();
    private final ViewPoint mTempViewPoint = new ViewPoint();
    private final Context mContext;
    private ToolboxCategory mToolboxCategory;
    private BlockFactory mBlockFactory;
    // The Workspace is the controller for the toolbox and trash as well as for the contents of
    // the main workspace.
    private ToolboxFragment mToolbox;
    // The trash can is currently just another instance of a toolbox: it holds blocks that can be
    // dragged into the workspace.
    private TrashFragment mTrash;
    private WorkspaceHelper mWorkspaceHelper;
    private WorkspaceView mWorkspaceView;
    private final Dragger mDragger =
            new Dragger(mWorkspaceHelper, mWorkspaceView, mConnectionManager, mRootBlocks);

    /**
     * Create a workspace controller.
     *
     * @param context The activity context.
     */
    public Workspace(Context context) {
        mContext = context;
    }

    public void loadBlockFactory(InputStream source) {
        mBlockFactory = new BlockFactory(source);
    }

    /**
     * Adds a new block to the workspace as a root block.
     *
     * @param block The block to add to the root of the workspace.
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

    /**
     * Remove a block from the workspace and put it in the trash.
     *
     * @param block The block block to remove, possibly with descendants attached.
     *
     * @return True if the block was removed, false otherwise.
     */
    public boolean removeRootBlock(Block block) {
        mDeletedBlocks.addBlock(block);
        mTrash.getAdapter().notifyDataSetChanged();
        return mRootBlocks.remove(block);
    }

    public WorkspaceHelper getWorkspaceHelper() {
        return mWorkspaceHelper;
    }

    public void setWorkspaceHelper(WorkspaceHelper helper) {
        mWorkspaceHelper = helper;
        mDragger.setWorkspaceHelper(mWorkspaceHelper);
    }

    /**
     * Set up the trash fragment, which will show blocks that have been deleted from this
     * workspace.
     *
     * @param trash The {@link TrashFragment} to update when blocks are deleted.
     */
    public void setTrashFragment(TrashFragment trash) {
        // Invalidate the old trash.
        if (mTrash != null) {
            mTrash.setWorkspace(null);
            mTrash.setContents(null);
        }
        // Set up the new trash.
        mTrash = trash;
        if (mTrash != null) {
            mTrash.setWorkspace(this);
            mTrash.setContents(mDeletedBlocks);
        }
    }

    /**
     * Loads the blocks that belong in the toolbox; sets up the relationships between the toolbox
     * and the workspace.  The workspace provides the list of blocks that the toolbox can provide.
     *
     * @param toolbox The {@link ToolboxFragment} that will provide blocks to be added to the
     * workspace.
     */
    public void setToolboxFragment(ToolboxFragment toolbox) {
        // Invalidate the old toolbox.
        if (mToolbox != null) {
            mToolbox.setWorkspace(null);
            mToolbox.setContents(null);
        }

        mToolbox = toolbox;
        // Set up the new toolbox.
        if (mToolbox != null) {
            mToolbox.setWorkspace(this);
            mToolbox.setContents(mToolboxCategory);
        }
    }

    /**
     * Set up toolbox's contents.
     *
     * @param blocks The resource id of the set of blocks or block groups to show in the toolbox.
     */
    public void loadToolboxContents(int blocks) {
        InputStream is = mContext.getResources().openRawResource(blocks);
        loadToolboxContents(is);
    }

    public void loadToolboxContents(InputStream source) {
        mToolboxCategory = BlocklyXmlHelper.loadToolboxFromXml(source, mBlockFactory);
    }

    /**
     * Reads the workspace in from an XML string.
     *
     * @param is The input stream to read from.
     *
     * @throws BlocklyParserException if there was a parse failure.
     */
    public void loadFromXml(InputStream is)
            throws BlocklyParserException {
        mRootBlocks.addAll(BlocklyXmlHelper.loadFromXml(is, mBlockFactory, stats));
        for (int i = 0; i < mRootBlocks.size(); i++) {
            stats.collectStats(mRootBlocks.get(i), true /* recursive */);
        }
    }

    /**
     * Outputs the workspace as an XML string.
     *
     * @param os The output stream to write to.
     *
     * @throws BlocklySerializerException if there was a failure while serializing.
     */
    public void serializeToXml(OutputStream os) throws BlocklySerializerException {
        BlocklyXmlHelper.writeToXml(mRootBlocks, os);
    }

    /**
     * Recursively initialize views corresponding to every block in the model.
     *
     * @param wv The root workspace view to add to.
     */
    public void createViewsFromModel(WorkspaceView wv) {
        BlockGroup bg;
        mWorkspaceView = wv;
        mDragger.setWorkspaceView(mWorkspaceView);
        mWorkspaceView.setDragger(mDragger);
        for (int i = 0; i < mRootBlocks.size(); i++) {
            bg = new BlockGroup(mContext, mWorkspaceHelper);
            mWorkspaceHelper.obtainBlockView(mRootBlocks.get(i), bg, mConnectionManager);
            mWorkspaceView.addView(bg);
        }
    }

    /**
     * Takes in a block model, creates corresponding views and adds it to the workspace.  Also
     * starts a drag of that block group.
     *
     * @param block The root block to be added to the workspace.
     * @param event The {@link MotionEvent} that caused the block to be added to the workspace.
     * This is used to find the correct position to start the drag event.
     */
    public void addBlockFromToolbox(Block block, MotionEvent event) {
        BlockGroup bg = new BlockGroup(mContext, mWorkspaceHelper);
        mWorkspaceHelper.obtainBlockView(mContext, block, bg, mConnectionManager);
        mWorkspaceView.addView(bg);
        addRootBlock(block);
        // let the workspace view know that this is the block we want to drag
        mWorkspaceView.setDragFocus(block.getView(), event);
        // Adjust the event's coordinates from the {@link BlockView}'s coordinate system to
        // {@link WorkspaceView} coordinates.
        int xPosition = (int) event.getX() +
                mWorkspaceHelper.workspaceToViewUnits(block.getPosition().x -
                        mWorkspaceHelper.getOffset().x);
        int yPosition = (int) event.getY() +
                mWorkspaceHelper.workspaceToViewUnits(block.getPosition().y -
                        mWorkspaceHelper.getOffset().y);
        mWorkspaceView.setDraggingStart(xPosition, yPosition);
        mWorkspaceView.startDrag();
    }
}
