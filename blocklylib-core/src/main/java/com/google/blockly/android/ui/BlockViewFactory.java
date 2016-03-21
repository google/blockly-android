/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.blockly.android.ui;

import android.content.Context;
import android.support.annotation.Nullable;
import android.view.View;

import com.google.blockly.android.ToolboxFragment;
import com.google.blockly.android.TrashFragment;
import com.google.blockly.android.WorkspaceFragment;
import com.google.blockly.android.control.ConnectionManager;
import com.google.blockly.android.control.NameManager;
import com.google.blockly.android.ui.fieldview.FieldView;
import com.google.blockly.model.Block;
import com.google.blockly.model.Field;
import com.google.blockly.model.Input;
import com.google.blockly.model.Workspace;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base factory class responsible for creating all the {@link BlockView}s, {@link InputView}s, and
 * {@link FieldView}s for a given block representation.
 * <p/>
 * Subclasses must override {@link #buildBlockView}, {@link #buildInputView}, and
 * {@link #buildFieldView}. These are called  views are constructed during calls to
 * {@link #buildBlockViewTree} and {@link #buildBlockGroupTree}.
 */
public abstract class BlockViewFactory<BlockView extends com.google.blockly.android.ui.BlockView,
                                       InputView extends com.google.blockly.android.ui.InputView> {
    protected Context mContext;
    protected WorkspaceHelper mHelper;

    protected final Map<String,WeakReference<BlockView>> mBlockIdToView
            = Collections.synchronizedMap(new HashMap<String, WeakReference<BlockView>>());

    protected BlockViewFactory(Context context, WorkspaceHelper helper) {
        mContext = context;
        mHelper = helper;

        helper.setBlockViewFactory(this);
    }

    public WorkspaceHelper getWorkspaceHelper() {
        return mHelper;
    }

    public abstract void setVariableNameManager(NameManager variableNameManager);

    /**
     * Creates a {@link BlockGroup} for the given block and its children using the workspace's
     * default style.
     *
     * @param rootBlock The root block to generate a view for.
     * @param connectionManager The {@link ConnectionManager} to update when moving connections.
     * @param touchHandler The {@link BlockTouchHandler} to manage all touches.
     *
     * @return A view for the block.
     */
    public BlockGroup buildBlockGroupTree(Block rootBlock,
                                          ConnectionManager connectionManager,
                                          BlockTouchHandler touchHandler) {
        BlockGroup bg = new BlockGroup(mContext, mHelper);
        buildBlockViewTree(rootBlock, bg, connectionManager, touchHandler);
        return bg;
    }

    /**
     * Called to construct the complete heirarchy of views representing a {@link Block} and it
     * subcomponents.  All constructed block views must be registered into the
     * {@link #mBlockIdToView} map, for later lookup via {@link #getView(Block)}.
     *
     * @param block The root block to generate a view for.
     * @param connectionManager The {@link ConnectionManager} to update when moving connections.
     * @param touchHandler The {@link BlockTouchHandler} to manage all touches.
     *
     * @return A view for the block.
     */
    public BlockView buildBlockViewTree(Block block, BlockGroup parentGroup,
                                        ConnectionManager connectionManager,
                                        BlockTouchHandler touchHandler) {
        BlockView blockView = getView(block);
        if (blockView != null) {
            throw new IllegalStateException("BlockView already created.");
        }

        List<Input> inputs = block.getInputs();
        final int inputCount = inputs.size();
        List<InputView> inputViews = new ArrayList<>(inputCount);
        for (int i = 0; i < inputCount; i++) {
            Input input = inputs.get(i);
            List<Field> fields = input.getFields();
            List<FieldView> fieldViews = new ArrayList<>(fields.size());
            for (int  j = 0; j < fields.size(); j++) {
                fieldViews.add(buildFieldView(fields.get(j)));
            }
            InputView inputView = buildInputView(input, fieldViews);

            if (input.getType() != Input.TYPE_DUMMY) {
                Block targetBlock = input.getConnection().getTargetBlock();
                if (targetBlock != null) {
                    // Blocks connected to inputs live in their own BlockGroups.
                    BlockGroup subgroup = buildBlockGroupTree(
                            targetBlock, connectionManager, touchHandler);
                    inputView.setConnectedBlockGroup(subgroup);
                }
            }
            inputViews.add(inputView);
        }
        blockView = buildBlockView(block, inputViews, connectionManager, touchHandler);
        mBlockIdToView.put(block.getId(), new WeakReference<BlockView>(blockView));
        parentGroup.addView((View) blockView);

        Block next = block.getNextBlock();
        if (next != null) {
            // Next blocks live in the same BlockGroup.
            buildBlockViewTree(next, parentGroup, connectionManager, touchHandler);
            // Recursively calls buildBlockViewTree(..) for the rest of the sequence.
        }

        return blockView;
    }

    /**
     * This returns the view constructed to represent {@link Block}.  Each block is only allowed
     * one view instance among the view managed by this factory (including
     * {@link WorkspaceFragment}, {@link ToolboxFragment}, and {@link TrashFragment}. Views are
     * constructed in {@link #buildBlockViewTree}, either directly or via recursion.  If the block
     * view has not been constructed, this method will return null.
     * <p/>
     * Views are released by calling one of {@link BlockGroup#unlinkModelAndSubViews()},
     * {@link BlockView#unlinkModelAndSubViews()}, {@link InputView#unlinkModelAndSubViews()}, or
     * {@link FieldView#unlinkModel()}.
     *
     * @param block The Block to view.
     * @return The previously constructed and active view of {@code block}. Otherwise null.
     */
    @Nullable
    public BlockView getView(Block block) {
        WeakReference<BlockView> viewRef = mBlockIdToView.get(block.getId());
        return viewRef == null ? null : viewRef.get();
    }

    /**
     * Build and populate the {@link com.google.blockly.android.ui.BlockView} for {@code block}.
     *
     * @param block The {@link Block} to view.
     * @param inputViews The list of {@link com.google.blockly.android.ui.InputView}s in this block.
     * @param connectionManager The {@link ConnectionManager} for the {@link Workspace}.
     * @param touchHandler The {@link BlockTouchHandler} this view should start with.
     * @return The new {@link com.google.blockly.android.ui.BlockView}.
     */
    protected abstract BlockView buildBlockView(Block block, List<InputView> inputViews,
                                                ConnectionManager connectionManager,
                                                BlockTouchHandler touchHandler);

    /**
     * Build and populate the {@link com.google.blockly.android.ui.InputView} for {@code input}.
     *
     * @param input The {@link Input} to view
     * @param fieldViews The list of {@link FieldView}s in the constructed view.
     * @return The new {@link com.google.blockly.android.ui.InputView}.
     */
    protected abstract InputView buildInputView(Input input, List<FieldView> fieldViews);

    /**
     * Build and populate the {@link com.google.blockly.android.ui.InputView} for {@code field}.
     *
     * @param field The {@link Field} to view
     * @return The new {@link com.google.blockly.android.ui.fieldview.FieldView}.
     */
    protected abstract FieldView buildFieldView(Field field);
}
