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

package com.google.blockly.android.ui.vertical;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;

import com.google.blockly.android.ToolboxFragment;
import com.google.blockly.android.TrashFragment;
import com.google.blockly.android.WorkspaceFragment;
import com.google.blockly.android.control.ConnectionManager;
import com.google.blockly.model.Block;
import com.google.blockly.model.Field;
import com.google.blockly.model.Input;
import com.google.blockly.android.control.NameManager;
import com.google.blockly.android.ui.BlockGroup;
import com.google.blockly.android.ui.BlockTouchHandler;
import com.google.blockly.android.ui.BlockViewFactory;
import com.google.blockly.android.ui.WorkspaceHelper;
import com.google.blockly.android.ui.fieldview.FieldView;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Constructs Blockly's default, vertical stacking blocks and related views.
 */
public class VerticalBlocksViewFactory extends BlockViewFactory {
    private static final String TAG = "VerticlBlocksViewFactry";
    private static final boolean DEBUG = false;

    private final LayoutInflater mLayoutInflater;
    private final PatchManager mPatchManager;

    private int mBlockStyle;
    private int mFieldStyle;
    private int mSpinnerLayout;
    private int mSpinnerDropDownLayout;
    private int mFieldInputLayout;
    private BaseAdapter mVariableAdapter;

    private final Map<String,WeakReference<BlockView>> mBlockIdToView
            = Collections.synchronizedMap(new HashMap<String, WeakReference<BlockView>>());

    public VerticalBlocksViewFactory(Context context, WorkspaceHelper helper) {
        this(context, helper, 0);
    }
    public VerticalBlocksViewFactory(Context context, WorkspaceHelper helper, int workspaceTheme) {
        super(context, helper);

        mLayoutInflater = LayoutInflater.from(context);
        mPatchManager = new PatchManager(mContext.getResources(), helper.useRtl());

        loadingStyleData(workspaceTheme);
    }

    /**
     * Set the {@link NameManager} being used to track variables in the workspace.
     *
     * @param variableNameManager The name manager that
     */
    public void setVariableNameManager(NameManager variableNameManager) {
        mVariableAdapter = onCreateNameAdapter(variableNameManager);
    }

    /**
     * @return The {@link PatchManager} for drawing Blocks using 9-patches.
     */
    public PatchManager getPatchManager() {
        return mPatchManager;
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
    @Override
    public BlockGroup buildBlockGroupTree(Block rootBlock,
                                          ConnectionManager connectionManager,
                                          BlockTouchHandler touchHandler) {
        BlockGroup bg = new BlockGroup(mContext, mHelper);
        buildBlockViewTree(rootBlock, bg, connectionManager, touchHandler);
        return bg;
    }

    /**
     * Creates a {@link BlockView} for the given block and its children using the workspace's
     * default style.
     *
     * @param block The block to generate a view for.
     * @param parentGroup The group to set as the parent for this block's view.
     * @param connectionManager The {@link ConnectionManager} to update when moving connections.
     * @param touchHandler The {@link BlockTouchHandler} to manage all touches.
     *
     * @return A view for the block.
     */
    @Override
    public BlockView buildBlockViewTree(Block block, BlockGroup parentGroup,
                                        ConnectionManager connectionManager,
                                        BlockTouchHandler touchHandler) {
        BlockView blockView = getView(block);
        if (blockView != null) {
            throw new IllegalStateException("BlockView already created.");
        }

        // TODO(#88): Refactor to use a BlockViewFactory to instantiate and combine all the views.
        blockView = new BlockView(mContext, block, this, connectionManager, touchHandler);
        List<Input> inputs = block.getInputs();
        for (int i = 0; i < inputs.size(); i++) {
            Input in = inputs.get(i);
            InputView inputView = blockView.getInputView(i);
            if (in.getType() != Input.TYPE_DUMMY && in.getConnection().getTargetBlock() != null) {
                // Blocks connected to inputs live in their own BlockGroups.
                BlockGroup subgroup = buildBlockGroupTree(
                        in.getConnection().getTargetBlock(), connectionManager, touchHandler);
                inputView.setConnectedBlockGroup(subgroup);
            }
        }
        parentGroup.addView(blockView);

        Block next = block.getNextBlock();
        if (next != null) {
            // Next blocks live in the same BlockGroup.
            BlockView child = buildBlockViewTree(
                    next, parentGroup, connectionManager, touchHandler);
            // Recursively calls buildBlockViewTree(..) for the rest of the sequence.
        }

        mBlockIdToView.put(block.getId(), new WeakReference<BlockView>(blockView));
        return blockView;
    }

    /**
     * This returns the view constructed to represent {@link Block}.  Each block is only allowed
     * one view instance among the view managed by this helper (including {@link WorkspaceFragment},
     * {@link ToolboxFragment}, and {@link TrashFragment}. Views are constructed in
     * {@link #buildBlockViewTree}, either directly or via recursion.  If the block view has not
     * been constructed, this method will return null.
     *
     * @param block The Block to view.
     * @return The view that was constructed for a given Block object, if any.
     */
    @Override
    @Nullable
    public BlockView getView(Block block) {
        WeakReference<BlockView> viewRef = mBlockIdToView.get(block.getId());
        return viewRef == null ? null : viewRef.get();
    }

    /**
     * Builds the view for {@code input}.
     *
     * @param in The {@link Input} to view.
     * @return The {@link View} of {@code input}.
     */
    // TODO(#135): Construct necessary field views here.
    InputView buildInputView(Input in) {
        return new InputView(mContext, this, in);
    }

    /**
     * Builds a view for the specified field using this helper's configuration.
     *
     * @param field The field to build a view for.
     * @return A FieldView for the field or null if the field type is not known.
     */
    FieldView buildFieldView(Field field) {
        FieldView view = null;
        switch (field.getType()) {
            case Field.TYPE_LABEL: {
                TypedArray styles = obtainFieldStyledAttributes();
                try {
                    view = new FieldLabelView(mContext, field, styles);
                } finally {
                    styles.recycle();
                }
                break;
            }
            case Field.TYPE_CHECKBOX:
                view = new FieldCheckboxView(mContext, field);
                break;
            case Field.TYPE_DATE:
                view = new FieldDateView(mContext, field);
                break;
            case Field.TYPE_DROPDOWN:
                view = new FieldDropdownView(mContext, field,
                        R.layout.default_spinner_item, R.layout.default_spinner_drop_down);
                break;
            case Field.TYPE_ANGLE:
                view = new FieldAngleView(mContext, field);
                break;
            case Field.TYPE_COLOUR:
                view = new FieldColourView(mContext, field, mHelper);
                break;
            case Field.TYPE_INPUT:
                FieldInputView fiv = (FieldInputView) mLayoutInflater
                        .inflate(mFieldInputLayout, null);
                fiv.setField(field);
                view = fiv;
                break;
            case Field.TYPE_IMAGE:
                view = new FieldImageView(mContext, field);
                break;
            case Field.TYPE_VARIABLE:
                view = new FieldVariableView(mContext, field, mVariableAdapter);;
                break;
            default:
                Log.w(TAG, "Unknown field type.");
                break;
        }
        return view;
    }

    /**
     * @return The style resource id to use for drawing field labels.
     */
    int getBlockStyle() {
        return mBlockStyle;
    }

    /**
     * @return The style resource id to use for drawing field labels.
     */
    int getFieldStyle() {
        return mFieldStyle;
    }

    /**
     * Clears the view reference to this view from its block.
     *
     * @param view The BlockView to deassociate from its Block model.
     */
    void unlinkView(BlockView view) {
        String id = view.getBlock().getId();
        WeakReference<BlockView> viewRef = mBlockIdToView.get(id);
        if (viewRef != null) {
            if (viewRef.get() == view) {
                mBlockIdToView.remove(id);
            }
        }
    }

    /**
     * Creates an adapter for use by Spinners or ListViews from a {@link NameManager}.
     *
     * @param nameManager The name manager to get the list of names from.
     * @return An adapter that can be used by a Spinner or a ListView.
     */
    protected BaseAdapter onCreateNameAdapter(NameManager nameManager) {
        ArrayAdapter adapter = new FieldVariableView.VariableAdapter(nameManager,
                mContext, mSpinnerLayout);
        adapter.setDropDownViewResource(mSpinnerDropDownLayout);
        return adapter;
    }

    /**
     * Loads the style configurations using the selected style (if not 0), or from context's theme.
     */
    private void loadingStyleData(int workspaceTheme) {
        TypedArray styles;

        if (workspaceTheme != 0) {
            styles = mContext.obtainStyledAttributes(workspaceTheme, R.styleable.BlocklyWorkspaceTheme);
        } else {
            styles = mContext.obtainStyledAttributes(R.styleable.BlocklyWorkspaceTheme);
        }
        try {
            mBlockStyle = styles.getResourceId(R.styleable.BlocklyWorkspaceTheme_blockViewStyle, 0);
            mFieldStyle = styles.getResourceId(R.styleable.BlocklyWorkspaceTheme_fieldStyle, 0);
            styles.recycle();  // Done with workspace theme

            styles = obtainFieldStyledAttributes();
            mSpinnerLayout = styles.getResourceId(R.styleable.BlocklyFieldView_spinnerItem,
                    R.layout.default_spinner_item);
            mSpinnerDropDownLayout = styles.getResourceId(
                    R.styleable.BlocklyFieldView_spinnerItemDropDown,
                    R.layout.default_spinner_drop_down);
            mFieldInputLayout = styles.getResourceId(
                    R.styleable.BlocklyFieldView_fieldInputLayout,
                    R.layout.default_field_input);
            if (DEBUG) {
                Log.d(TAG, "BlockStyle=" + mBlockStyle + ", FieldStyle=" + mFieldStyle
                        + ", SpinnerLayout=" + mSpinnerLayout + ", SpinnerDropdown="
                        + mSpinnerDropDownLayout);
            }
        } finally {
            styles.recycle();
        }
    }

    private TypedArray obtainFieldStyledAttributes() {
        TypedArray styles;
        if (mFieldStyle != 0) {
            styles = mContext.obtainStyledAttributes(mFieldStyle, R.styleable.BlocklyFieldView);
        } else {
            styles = mContext.obtainStyledAttributes(R.styleable.BlocklyFieldView);
        }
        return styles;
    }
}
