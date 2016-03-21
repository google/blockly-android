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
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;

import com.google.blockly.android.control.ConnectionManager;
import com.google.blockly.model.Block;
import com.google.blockly.model.Field;
import com.google.blockly.model.Input;
import com.google.blockly.android.control.NameManager;
import com.google.blockly.android.ui.BlockTouchHandler;
import com.google.blockly.android.ui.BlockViewFactory;
import com.google.blockly.android.ui.WorkspaceHelper;
import com.google.blockly.android.ui.fieldview.FieldView;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Constructs Blockly's default, vertical stacking blocks and related views.
 */
public class VerticalBlocksViewFactory extends BlockViewFactory<BlockView, InputView> {
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

    /** Implements {@link BlockViewFactory#buildBlockView}. */
    @Override
    protected BlockView buildBlockView(Block block, List<InputView> inputViews,
                                       ConnectionManager connectionManager,
                                       BlockTouchHandler touchHandler) {
        return new BlockView(mContext, mHelper, this, block, inputViews,
                             connectionManager, touchHandler);
    }

    /** Implements {@link BlockViewFactory#buildInputView}. */
    protected InputView buildInputView(Input input, List<FieldView> fieldViews) {
        return new InputView(mContext, this, input, fieldViews);
    }

    /** Implements {@link BlockViewFactory#buildFieldView}. */
    @Override
    protected com.google.blockly.android.ui.fieldview.FieldView buildFieldView(Field field) {
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
