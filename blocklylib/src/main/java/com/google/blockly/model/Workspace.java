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
import android.support.annotation.VisibleForTesting;
import android.view.MotionEvent;

import com.google.blockly.ToolboxFragment;
import com.google.blockly.control.BlockCopyBuffer;
import com.google.blockly.control.BlocklyController;
import com.google.blockly.control.ConnectionManager;
import com.google.blockly.control.Dragger;
import com.google.blockly.control.ProcedureManager;
import com.google.blockly.control.WorkspaceStats;
import com.google.blockly.ui.BlockGroup;
import com.google.blockly.ui.BlockView;
import com.google.blockly.ui.ViewPoint;
import com.google.blockly.ui.WorkspaceHelper;
import com.google.blockly.ui.WorkspaceView;
import com.google.blockly.utils.BlocklyXmlHelper;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for the workspace.  Keeps track of all the global state used in the workspace and
 * manages interaction between the different fragments.
 */
public class Workspace {
    private static final String TAG = "Workspace";
    private static final boolean DEBUG = true;

    private final BlocklyController mController;
    private final WorkspaceHelper mWorkspaceHelper;

    private final ArrayList<Block> mRootBlocks = new ArrayList<>();
    private final ProcedureManager mProcedureManager = new ProcedureManager();
    private final NameManager mVariableNameManager = new NameManager.VariableNameManager();
    private final ConnectionManager mConnectionManager = new ConnectionManager();
    private final WorkspaceStats mStats = new WorkspaceStats(mVariableNameManager, mProcedureManager,
            mConnectionManager);
    private final ToolboxCategory mDeletedBlocks = new ToolboxCategory();
    private final BlockCopyBuffer mCopyBuffer = new BlockCopyBuffer();
    private final ViewPoint mTempViewPoint = new ViewPoint();
    private final Context mContext;
    private final Dragger mDragger;
    private ToolboxCategory mToolboxCategory;
    private BlockFactory mBlockFactory;

    private WorkspaceHelper.BlockTouchHandler mTouchHandler;
    private WorkspaceView mWorkspaceView;

    /**
     * Create a workspace.
     *
     * @param controller The controller for this Workspace.
     * {@link com.google.blockly.R.style#BlocklyTheme}
     */
    public Workspace(Context context, BlocklyController controller, WorkspaceHelper helper,
            BlockFactory factory) {

        if (controller == null) {
            throw new IllegalArgumentException("BlocklyController may not be null.");
        }
        if (helper == null) {
            throw new IllegalArgumentException("WorkspaceHelper may not be null.");
        }

        mContext = context;
        mController = controller;
        mWorkspaceHelper = helper;
        mBlockFactory = factory;

        mDragger = new Dragger(this, mWorkspaceHelper, mConnectionManager, mRootBlocks);
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
        mStats.collectStats(block, true);
    }

    /**
     * Remove a block from the workspace and put it in the trash.
     *
     * @param block The block block to remove, possibly with descendants attached.
     *
     * @return True if the block was removed, false otherwise.
     */
    public boolean removeRootBlock(Block block) {
        if (mRootBlocks.remove(block)) {
            mDeletedBlocks.addBlock(block);
            return true;
        }
        // else
        return false;
    }

    /**
     * The {@link WorkspaceHelper} for the workspace can be used to get config and style properties
     * and to convert between units.
     *
     * @return The {@link WorkspaceHelper} for this workspace.
     */
    public WorkspaceHelper getWorkspaceHelper() {
        return mWorkspaceHelper;
    }

    public ConnectionManager getConnectionManager() {
        return mConnectionManager;
    }

    /**
     * Set up toolbox's contents.
     *
     * @param toolboxResId The resource id of the set of blocks or block groups to show in the
     * toolbox.
     */
    public void loadToolboxContents(int toolboxResId) {
        InputStream is = mContext.getResources().openRawResource(toolboxResId);
        loadToolboxContents(is);
    }

    /**
     * Set up toolbox's contents.
     *
     * @param source The source of the set of blocks or block groups to show in the toolbox.
     */
    public void loadToolboxContents(InputStream source) {
        mToolboxCategory = BlocklyXmlHelper.loadToolboxFromXml(source, mBlockFactory);
    }

    /**
     * Set up toolbox's contents.
     *
     * @param toolboxXml The xml of the set of blocks or block groups to show in the toolbox.
     */
    public void loadToolboxContents(String toolboxXml) {
        loadToolboxContents(new ByteArrayInputStream(toolboxXml.getBytes()));
    }

    /**
     * Reads the workspace in from a XML stream. This will clear the workspace and replace it with
     * the contents of the xml.
     *
     * @param is The input stream to read from.
     *
     * @throws BlocklyParserException if there was a parse failure.
     */
    public void loadWorkspaceContents(InputStream is)
            throws BlocklyParserException {
        resetWorkspace();
        mRootBlocks.addAll(BlocklyXmlHelper.loadFromXml(is, mBlockFactory, mStats));
        for (int i = 0; i < mRootBlocks.size(); i++) {
            mStats.collectStats(mRootBlocks.get(i), true /* recursive */);
        }
    }

    /**
     * Reads the workspace in from a XML stream. This will clear the workspace and replace it with
     * the contents of the xml.
     *
     * @param xml The XML source string to read from.
     *
     * @throws BlocklyParserException if there was a parse failure.
     */
    public void loadWorkspaceContents(String xml) throws BlocklyParserException {
        loadWorkspaceContents(new ByteArrayInputStream(xml.getBytes()));
    }

    /**
     * Gets the {@link BlockFactory} being used by this workspace. This can be used to update or
     * replace the set of known blocks.
     *
     * @return The block factory used by this workspace.
     */
    public BlockFactory getBlockFactory() {
        return mBlockFactory;
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
     * Set up the {@link WorkspaceView} with this workspace's model. This method will perform the
     * following steps:
     * <ul>
     * <li>Set the block touch handler for the view.</li>
     * <li>Configure the dragger for the view.</li>
     * <li>Recursively initialize views for all the blocks in the model and add them to the
     * view.</li>
     * </ul>
     *
     * @param wv The root workspace view to add to.
     */
    public void initWorkspaceView(final WorkspaceView wv) {
        mWorkspaceView = wv;
        mWorkspaceView.setController(mController);

        mWorkspaceHelper.setWorkspaceView(wv);
        // Tell the workspace helper to pass onTouchBlock events straight through to the Dragger.
        mTouchHandler = new WorkspaceHelper.BlockTouchHandler() {
            @Override
            public boolean onTouchBlock(BlockView blockView, MotionEvent motionEvent) {
                return mDragger.onTouchBlock(blockView, motionEvent);
            }

            @Override
            public boolean onInterceptTouchEvent(BlockView blockView, MotionEvent motionEvent) {
                return mDragger.onInterceptTouchEvent(blockView, motionEvent);
            }
        };
        mDragger.setWorkspaceView(mWorkspaceView);
        mWorkspaceView.setDragger(mDragger);
        initBlockViews();
    }

    /**
     * Recursively initialize views for all the blocks in the model and add them to the
     * view.
     */
    public void initBlockViews() {
        BlockGroup bg;
        for (int i = 0; i < mRootBlocks.size(); i++) {
            bg = new BlockGroup(mContext, mWorkspaceHelper);
            mWorkspaceHelper.buildBlockViewTree(mRootBlocks.get(i), bg, mConnectionManager,
                    mTouchHandler);
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
     * @param fragment The {@link ToolboxFragment} where the event originated.
     */
    public void addBlockFromToolbox(Block block, MotionEvent event, ToolboxFragment fragment) {
        addBlockWithView(block);
        // let the workspace view know that this is the block we want to drag
        mDragger.setTouchedBlock(block.getView(), event);
        // Adjust the event's coordinates from the {@link BlockView}'s coordinate system to
        // {@link WorkspaceView} coordinates.
        mWorkspaceHelper.workspaceToVirtualViewCoordinates(block.getPosition(), mTempViewPoint);
        mDragger.setDragStartPos((int) event.getX() + mTempViewPoint.x,
                (int) event.getY() + mTempViewPoint.y);
        mDragger.startDragging();
        mController.maybeCloseToolboxFragment(fragment);
    }

    /**
     * Takes in a block model, creates corresponding views and adds it to the workspace.
     *
     * @param block The {@link Block} to add to the workspace.
     */
    public void addBlockWithView(Block block) {
        mWorkspaceView.addView(
                mWorkspaceHelper.buildBlockGroupTree(block, mConnectionManager, mTouchHandler));
        addRootBlock(block);
    }

    /**
     * Reset the workspace view when changing workspaces.  Removes old views and creates all
     * necessary new views.
     */
    public void resetWorkspace() {
        mRootBlocks.clear();
        mStats.clear();
        mDeletedBlocks.clear();
        if (mWorkspaceView != null) {
            mWorkspaceView.removeAllViews();
            initBlockViews();
        }
        // TODO(fenichel): notify adapters when contents change.
    }

    public boolean hasDeletedBlocks() {
        return !mDeletedBlocks.isEmpty();
    }

    public ToolboxCategory getToolboxContents() {
        return mToolboxCategory;
    }

    public ToolboxCategory getTrashContents() {
        return mDeletedBlocks;
    }

    @VisibleForTesting
    List<Block> getRootBlocks() {
        return mRootBlocks;
    }
}
