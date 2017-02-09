/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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
import android.support.v4.util.SimpleArrayMap;

import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.android.control.ConnectionManager;
import com.google.blockly.android.control.NameManager;
import com.google.blockly.android.control.ProcedureManager;
import com.google.blockly.android.control.WorkspaceStats;
import com.google.blockly.utils.BlocklyXmlHelper;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The root class for the Blockly model.  Keeps track of all the global state used in the workspace.
 */
public class Workspace {
    private static final String TAG = "Workspace";

    private final Context mContext;
    private final BlocklyController mController;
    private BlockFactory mBlockFactory;
    private String mId;

    private final ArrayList<Block> mRootBlocks = new ArrayList<>();
    private final ProcedureManager mProcedureManager = new ProcedureManager();
    private final NameManager mVariableNameManager = new NameManager.VariableNameManager();
    private final ConnectionManager mConnectionManager = new ConnectionManager();
    private final WorkspaceStats mStats =
            new WorkspaceStats(mVariableNameManager, mProcedureManager,
                    mConnectionManager);

    private FlyoutCategory mFlyoutCategory;
    private FlyoutCategory mTrashCategory = new FlyoutCategory();

    /**
     * Create a workspace.
     *
     * @param context The context this workspace is associated with.
     * @param controller The controller for this Workspace.
     * @param factory The factory used to build blocks in this workspace.
     */
    public Workspace(Context context, BlocklyController controller,
            BlockFactory factory) {

        if (controller == null) {
            throw new IllegalArgumentException("BlocklyController may not be null.");
        }

        mContext = context;
        mController = controller;
        mBlockFactory = factory;
        mId = UUID.randomUUID().toString();
    }

    public String getId() {
        return mId;
    }

    /**
     * Adds a new block to the workspace as a root block.
     *
     * @param block The block to add to the root of the workspace.
     * @param isNewBlock Set when the block is new to the workspace (compared to moving it from some
     *                   previous connection).
     */
    public void addRootBlock(Block block, boolean isNewBlock) {
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
        if (isNewBlock) {
            mStats.collectStats(block, true);
        }
    }

    /**
     * Remove a block from the workspace.
     *
     * @param block The block block to remove, possibly with descendants attached.
     * @param cleanupStats True if this block is being deleted and its connections and references
     *                     should be removed.
     * @return True if the block was removed, false otherwise.
     */
    public boolean removeRootBlock(Block block, boolean cleanupStats) {
        boolean foundAndRemoved = mRootBlocks.remove(block);
        if (foundAndRemoved && cleanupStats) {
            mStats.cleanupStats(block);
        }
        return foundAndRemoved;
    }

    /**
     * Add a root block to the trash.
     *
     * @param block The block to put in the trash, possibly with descendants attached.
     */
    // TODO(#56): Make sure the block doesn't have a parent.
    public void addBlockToTrash(Block block) {
        mTrashCategory.addBlock(0, block);
    }

    /**
     * Moves {@code trashedBlock} out of {@link #mTrashCategory} and into {@link #mRootBlocks}.
     *
     * @param trashedBlock The {@link Block} to move.
     * @throws IllegalArgumentException When {@code trashedBlock} is not found in
     *         {@link #mTrashCategory}.
     */
    public void addBlockFromTrash(Block trashedBlock) {
        boolean foundBlock = mTrashCategory.removeBlock(trashedBlock);
        if (!foundBlock) {
            throw new IllegalArgumentException("trashedBlock not found in mTrashCategory");
        }
        mRootBlocks.add(trashedBlock);
    }

    /**
     * @return The {@link ConnectionManager} managing the connection locations in this Workspace.
     */
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
        mFlyoutCategory = BlocklyXmlHelper.loadToolboxFromXml(source, mBlockFactory);
    }

    /**
     * Set up the trash from an input stream. The trash is loaded like a toolbox and can have a
     * name, color, and set of blocks to start with. Unlike a toolbox it may not have subcategories.
     *
     * @param source The source to initialize the trash.
     */
    public void loadTrashConfiguration(InputStream source) {
        mTrashCategory = BlocklyXmlHelper.loadToolboxFromXml(source, mBlockFactory);
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
     * Set up the trash from an input stream. The trash is loaded like a toolbox and can have a
     * name, color, and set of blocks to start with. Unlike a toolbox it may not have subcategories.
     *
     * @param trashXml The xml of the flyout to configure the trash.
     */
    public void loadTrashConfiguration(String trashXml) {
        loadTrashConfiguration(new ByteArrayInputStream(trashXml.getBytes()));
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
        List<Block> newBlocks = BlocklyXmlHelper.loadFromXml(is, mBlockFactory, mStats);

        // Successfully deserialized.  Update workspace.
        // TODO: (#22) Add proper variable support.
        // For now just save and restore the list of variables.
        SimpleArrayMap<String, String> varsMap = mVariableNameManager.getUsedNames();
        String[] vars = new String[varsMap.size()];
        for (int i = 0; i < varsMap.size(); i++) {
            vars[i] = varsMap.keyAt(i);
        }
        mController.resetWorkspace();
        for (int i = 0; i < vars.length; i++) {
            mController.addVariable(vars[i]);
        }

        mRootBlocks.addAll(newBlocks);
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
     * @return The list of fields that are using the given variable.
     */
    public List<FieldVariable> getVariableRefs(String variable) {
        List<FieldVariable> refs = mStats.getVariableReference(variable);
        List<FieldVariable> copy = new ArrayList<>(refs == null ? 0 : refs.size());
        copy.addAll(refs);
        return copy;
    }

    /**
     * Return the number of times a variable is referenced in this workspace.
     *
     * @param variable The variable to get a ref count for.
     * @return The number of times that variable appears in this workspace.
     */
    public int getVariableRefCount(String variable) {
        return mStats.getVariableReference(variable).size();
    }

    /**
     * Gets all blocks that are using the specified variable.
     *
     * @param variable The variable to get blocks for.
     * @param resultList An optional list to put the results in. This object will be returned if not
     *                   null.
     * @return A list of all blocks referencing the given variable.
     */
    public List<Block> getBlocksWithVariable(String variable, List<Block> resultList) {
        List<FieldVariable> refs = mStats.getVariableReference(variable);
        if (resultList == null) {
            resultList = new ArrayList<>();
        }
        for(FieldVariable field : refs) {
            Block block = field.getBlock();
            if (!resultList.contains(block)) {
                resultList.add(block);
            }
        }
        return resultList;
    }

    /**
     * Gets the {@link NameManager.VariableNameManager} being used by this workspace. This can be
     * used to get a list of variables in the workspace.
     *
     * @return The name manager for variables in this workspace.
     */
    public NameManager getVariableNameManager() {
        return mVariableNameManager;
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
        mBlockFactory.clearPriorBlockReferences();
        mRootBlocks.clear();
        mStats.clear();
        mTrashCategory.clear();
    }

    public boolean hasDeletedBlocks() {
        return !mTrashCategory.getBlocks().isEmpty();
    }

    public FlyoutCategory getToolboxContents() {
        return mFlyoutCategory;
    }

    public FlyoutCategory getTrashCategory() {
        return mTrashCategory;
    }


    public ArrayList<Block> getRootBlocks() {
        return mRootBlocks;
    }

    public boolean isRootBlock(Block block) {
        return mRootBlocks.contains(block);
    }

    /**
     * @return if the workspace currently has any blocks.
     */
    public boolean hasBlocks() {
        return getRootBlocks().size() > 0;
    }
}
