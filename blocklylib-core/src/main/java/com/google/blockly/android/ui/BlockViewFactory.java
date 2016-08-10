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
import android.widget.SpinnerAdapter;

import com.google.blockly.android.ToolboxFragment;
import com.google.blockly.android.TrashFragment;
import com.google.blockly.android.WorkspaceFragment;
import com.google.blockly.android.control.ConnectionManager;
import com.google.blockly.android.control.NameManager;
import com.google.blockly.android.ui.fieldview.BasicFieldAngleView;
import com.google.blockly.android.ui.fieldview.BasicFieldCheckboxView;
import com.google.blockly.android.ui.fieldview.BasicFieldColorView;
import com.google.blockly.android.ui.fieldview.BasicFieldDateView;
import com.google.blockly.android.ui.fieldview.BasicFieldDropdownView;
import com.google.blockly.android.ui.fieldview.BasicFieldImageView;
import com.google.blockly.android.ui.fieldview.BasicFieldInputView;
import com.google.blockly.android.ui.fieldview.BasicFieldLabelView;
import com.google.blockly.android.ui.fieldview.BasicFieldNumberView;
import com.google.blockly.android.ui.fieldview.BasicFieldVariableView;
import com.google.blockly.android.ui.fieldview.FieldView;
import com.google.blockly.android.ui.fieldview.VariableRequestCallback;
import com.google.blockly.model.Block;
import com.google.blockly.model.Field;
import com.google.blockly.model.FieldAngle;
import com.google.blockly.model.FieldCheckbox;
import com.google.blockly.model.FieldColor;
import com.google.blockly.model.FieldDate;
import com.google.blockly.model.FieldDropdown;
import com.google.blockly.model.FieldImage;
import com.google.blockly.model.FieldInput;
import com.google.blockly.model.FieldLabel;
import com.google.blockly.model.FieldNumber;
import com.google.blockly.model.FieldVariable;
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
 * {@link FieldView}s for a given block representation.  Complete view trees are constructed via
 * calls to {@link #buildBlockGroupTree} or {@link #buildBlockViewTree}.
 * <p/>
 * Subclasses must override {@link #buildBlockView}, {@link #buildInputView}, and
 * {@link #buildFieldView}. They may also override {@link #buildBlockGroup()}, to provide customized
 * implementations to the container view.
 */
public abstract class BlockViewFactory<BlockView extends com.google.blockly.android.ui.BlockView,
                                       InputView extends com.google.blockly.android.ui.InputView> {
    /**
     * Context for creating or loading views.
     */
    protected Context mContext;
    /**
     * Helper for doing conversions and style lookups.
     */
    protected WorkspaceHelper mHelper;
    /**
     * Name manager for the list of variables in this instance of Blockly.
     */
    protected NameManager mVariableNameManager;
    /**
     * The callback to use for views that can request changes to the list of variables.
     */
    protected VariableRequestCallback mVariableCallback;

    private SpinnerAdapter mVariableAdapter;

    // TODO(#137): Move to ViewPool class.
    protected final Map<String,WeakReference<BlockView>> mBlockIdToView
            = Collections.synchronizedMap(new HashMap<String, WeakReference<BlockView>>());

    protected BlockViewFactory(Context context, WorkspaceHelper helper) {
        mContext = context;
        mHelper = helper;

        helper.setBlockViewFactory(this);
    }

    /**
     * Sets the callback to use for variable view events, such as the user selected delete/rename.
     *
     * @param callback The callback to set on variable field views.
     */
    public void setVariableRequestCallback(VariableRequestCallback callback) {
        mVariableCallback = callback;
    }

    public WorkspaceHelper getWorkspaceHelper() {
        return mHelper;
    }

    /**
     * Set the {@link NameManager} being used to track variables in the workspace.
     *
     * @param variableNameManager The name manager for the variables in the associated workspace.
     */
    public void setVariableNameManager(NameManager variableNameManager) {
        mVariableNameManager = variableNameManager;
    }

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
    public final BlockGroup buildBlockGroupTree(Block rootBlock,
                                          ConnectionManager connectionManager,
                                          BlockTouchHandler touchHandler) {
        BlockGroup bg = buildBlockGroup();
        buildBlockViewTree(rootBlock, bg, connectionManager, touchHandler);
        return bg;
    }

    /**
     * Called to construct the complete hierarchy of views representing a {@link Block} and its
     * subcomponents, added to {@code parentGroup}.
     *
     * @param block The root block to generate a view for.
     * @param parentGroup T
     * @param connectionManager The {@link ConnectionManager} to update when moving connections.
     * @param touchHandler The {@link BlockTouchHandler} to manage all touches.
     *
     * @return A view for the block and all its descendants.
     */
    public final BlockView buildBlockViewTree(Block block, BlockGroup parentGroup,
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

        // TODO(#137): Move to ViewPool class.
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
     * This returns the view constructed to represent a {@link Block}.  Each block is only allowed
     * one view instance among the views managed by this factory (including
     * {@link WorkspaceFragment}, {@link ToolboxFragment}, and {@link TrashFragment}). Views are
     * constructed in {@link #buildBlockViewTree}, either directly or via recursion.  If the block
     * view has not been constructed, this method will return null.
     * <p/>
     * Calling {@link BlockView#unlinkModel()} (possibly via {@link BlockGroup#unlinkModel()}) will
     * disconnect this view from its model, and will it no longer be returned from this method.
     *
     * @param block The {@link Block} to get the view for.
     * @return The previously constructed and active view of {@code block}. Otherwise null.
     */
    @Nullable
    public final BlockView getView(Block block) {
        WeakReference<BlockView> viewRef = mBlockIdToView.get(block.getId());
        return viewRef == null ? null : viewRef.get();
    }

    /**
     * @return A new, empty {@link BlockGroup} container view for a sequence of blocks.
     */
    public BlockGroup buildBlockGroup() {
        return new BlockGroup(mContext, mHelper);
    }

    /**
     * Build and populate the {@link BlockView} for {@code block}, using the provided
     * {@link InputView}s.
     * <p/>
     * This method should not recurse the model to generate more than one view.
     * {@link #buildBlockViewTree} will traverse the model and call this method for each
     * {@link Block}.
     *
     * @param block The {@link Block} to build a view for.
     * @param inputViews The list of {@link com.google.blockly.android.ui.InputView}s in this block.
     * @param connectionManager The {@link ConnectionManager} for the {@link Workspace}.
     * @param touchHandler The {@link BlockTouchHandler} this view should start with.
     * @return The new {@link com.google.blockly.android.ui.BlockView}.
     */
    protected abstract BlockView buildBlockView(Block block, List<InputView> inputViews,
                                                ConnectionManager connectionManager,
                                                BlockTouchHandler touchHandler);

    /**
     * Build and populate the {@link InputView} for {@code input}.
     * <p/>
     * This method should not recurse the model to generate more than one view.
     * {@link #buildBlockViewTree} will traverse the model and call this method for each
     * {@link Input}.
     *
     * @param input The {@link Input} to build a view for.
     * @param fieldViews The list of {@link FieldView}s in the constructed view.
     * @return The new {@link com.google.blockly.android.ui.InputView}.
     */
    protected abstract InputView buildInputView(Input input, List<FieldView> fieldViews);

    /**
     * Build and populate the {@link FieldView} for {@code field}.
     * <p>
     * Note: Variables need some extra setup when they are created by a custom
     * ViewFactory.
     * <ul>
     *     <li>If they use an adapter to display the list of variables it must be set.</li>
     *     <li>If they have delete/rename/create options they must have a
     *     {@link VariableRequestCallback} set on them. {@link #mVariableCallback} may be used for
     *     this purpose.</li>
     * </ul>
     *
     *
     * @param field The {@link Field} to build a view for.
     * @return The new {@link FieldView}.
     */
    protected FieldView buildFieldView(Field field) {
        @Field.FieldType int type = field.getType();
        switch (type) {
            case Field.TYPE_ANGLE: {
                BasicFieldAngleView fieldAngleView = new BasicFieldAngleView(mContext);
                fieldAngleView.setField((FieldAngle) field);
                return fieldAngleView;
            }
            case Field.TYPE_CHECKBOX: {
                BasicFieldCheckboxView fieldCheckboxView = new BasicFieldCheckboxView(mContext);
                fieldCheckboxView.setField((FieldCheckbox) field);
                return fieldCheckboxView;
            }
            case Field.TYPE_COLOR: {
                BasicFieldColorView fieldColorView = new BasicFieldColorView(mContext);
                fieldColorView.setField((FieldColor) field);
                return fieldColorView;
            }
            case Field.TYPE_DATE: {
                BasicFieldDateView fieldDateView = new BasicFieldDateView(mContext);
                fieldDateView.setField((FieldDate) field);
                return fieldDateView;
            }
            case Field.TYPE_DROPDOWN: {
                BasicFieldDropdownView fieldDropdownView = new BasicFieldDropdownView(mContext);
                fieldDropdownView.setField((FieldDropdown) field);
                return fieldDropdownView;
            }
            case Field.TYPE_IMAGE: {
                BasicFieldImageView fieldImageView = new BasicFieldImageView(mContext);
                fieldImageView.setField((FieldImage) field);
                return fieldImageView;
            }
            case Field.TYPE_INPUT: {
                BasicFieldInputView fieldInputView = new BasicFieldInputView(mContext);
                fieldInputView.setField((FieldInput) field);
                return fieldInputView;
            }
            case Field.TYPE_LABEL: {
                BasicFieldLabelView fieldLabelView = new BasicFieldLabelView(mContext);
                fieldLabelView.setField((FieldLabel) field);
                return fieldLabelView;
            }
            case Field.TYPE_VARIABLE: {
                BasicFieldVariableView fieldVariableView = new BasicFieldVariableView(mContext);
                fieldVariableView.setAdapter(getVariableAdapter());
                fieldVariableView.setField((FieldVariable) field);
                fieldVariableView.setVariableRequestCallback(mVariableCallback);
                return fieldVariableView;
            }
            case Field.TYPE_NUMBER: {
                BasicFieldNumberView fieldNumberView = new BasicFieldNumberView(mContext);
                fieldNumberView.setField((FieldNumber) field);
                return fieldNumberView;
            }

            case Field.TYPE_UNKNOWN:
            default:
                throw new IllegalArgumentException("Unknown Field type: " + type);
        }
    }

    protected SpinnerAdapter getVariableAdapter() {
        if (mVariableNameManager == null) {
            throw new IllegalStateException("NameManager must be set before variable field is "
                    + "instantiated.");
        }
        if (mVariableAdapter == null) {
            mVariableAdapter = new BasicFieldVariableView.VariableViewAdapter(mContext, mVariableNameManager,
                    android.R.layout.simple_spinner_item);
        }
        return mVariableAdapter;
    }

    /**
     * Removes the mapping to this view from its block.  This should only be called from
     * {@link BlockView#unlinkModel()}, which is already handled in {@link AbstractBlockView}.
     *
     *  @param blockView The BlockView to disassociate from its Block model.
     */
    // TODO(#137): Move to ViewPool class.
    protected final void unregisterView(BlockView blockView) {
        Block block = blockView.getBlock();
        mBlockIdToView.remove(block.getId());
    }
}
