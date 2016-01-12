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

import com.google.blockly.control.BlocklyController;
import com.google.blockly.control.ConnectionManager;
import com.google.blockly.control.ProcedureManager;
import com.google.blockly.control.WorkspaceStats;
import com.google.blockly.utils.BlocklyXmlHelper;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;


/**
 * Controller for the workspace.  Keeps track of all the global state used in the workspace and
 * manages interaction between the different fragments.
 */
public class Workspace {
    private static final String TAG = "Workspace";
    private static final boolean DEBUG = true;

    private final BlocklyController mController;

    private final ArrayList<Block> mRootBlocks = new ArrayList<>();
    private final ProcedureManager mProcedureManager = new ProcedureManager();
    private final NameManager mVariableNameManager = new NameManager.VariableNameManager();
    private final ConnectionManager mConnectionManager = new ConnectionManager();
    private final WorkspaceStats mStats =
            new WorkspaceStats(mVariableNameManager, mProcedureManager,
                    mConnectionManager);
    private final ToolboxCategory mDeletedBlocks = new ToolboxCategory();
    private final Context mContext;
    private ToolboxCategory mToolboxCategory;
    private BlockFactory mBlockFactory;

    /**
     * Create a workspace.
     *
     * @param controller The controller for this Workspace. {@link com.google.blockly.R.style#BlocklyTheme}
     */
    public Workspace(Context context, BlocklyController controller,
            BlockFactory factory) {

        if (controller == null) {
            throw new IllegalArgumentException("BlocklyController may not be null.");
        }

        mContext = context;
        mController = controller;
        mBlockFactory = factory;
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
     * @throws BlocklyParserException if there was a parse failure.
     */
    public void loadWorkspaceContents(InputStream is)
            throws BlocklyParserException {
        mController.resetWorkspace();
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
     * @throws BlocklySerializerException if there was a failure while serializing.
     */
    public void serializeToXml(OutputStream os) throws BlocklySerializerException {
        BlocklyXmlHelper.writeToXml(mRootBlocks, os);
    }

    /**
     * Reset the workspace view when changing workspaces.  Removes old views and creates all
     * necessary new views.
     */
    public void resetWorkspace() {
        mRootBlocks.clear();
        mStats.clear();
        mDeletedBlocks.clear();
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


    public ArrayList<Block> getRootBlocks() {
        return mRootBlocks;
    }
}
