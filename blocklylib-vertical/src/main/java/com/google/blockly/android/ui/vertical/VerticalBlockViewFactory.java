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
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.widget.SpinnerAdapter;

import com.google.blockly.android.control.ConnectionManager;
import com.google.blockly.android.ui.BlockGroup;
import com.google.blockly.android.ui.BlockTouchHandler;
import com.google.blockly.android.ui.BlockViewFactory;
import com.google.blockly.android.ui.WorkspaceHelper;
import com.google.blockly.android.ui.fieldview.BasicFieldVariableView;
import com.google.blockly.android.ui.fieldview.FieldView;
import com.google.blockly.model.Block;
import com.google.blockly.model.Field;
import com.google.blockly.model.Input;

import java.util.List;

/**
 * Constructs Blockly's default, vertical stacking blocks and related views.
 */
public class VerticalBlockViewFactory extends BlockViewFactory<BlockView, InputView> {
    private static final String TAG = "VertcalBlockViewFactory";  // 23 char limit

    private final PatchManager mPatchManager;

    private SparseIntArray mFieldLayouts = new SparseIntArray();
    private LayoutInflater mLayoutInflater;
    private boolean mUseHats = false;
    private BasicFieldVariableView.VariableViewAdapter mVariableAdapter;

    public VerticalBlockViewFactory(Context context, WorkspaceHelper helper) {
        this(context, helper, 0);
    }

    /**
     * @param context The application or activity's {@link Context}.
     * @param helper The {@link WorkspaceHelper} associated with the workspace.
     * @param workspaceTheme The theme resource id for the block styles.
     */
    public VerticalBlockViewFactory(Context context, WorkspaceHelper helper, int workspaceTheme) {
        super(context, helper);

        loadStyleData(workspaceTheme);

        mPatchManager = new PatchManager(mContext.getResources(), helper.useRtl(), mUseHats);
        mLayoutInflater = LayoutInflater.from(context);
    }

    /**
     * @return The {@link PatchManager} for drawing Blocks using 9-patches.
     */
    public PatchManager getPatchManager() {
        return mPatchManager;
    }

    /**
     * Creates a new {@link BlockGroup} where the render order is reversed. That is, for each
     * {@link BlockView}, any next connected {@link BlockView} below it (if any) will render first.
     * This ensures connector highlights will not be occluded.
     *
     * @return A new {@link BlockGroup}
     */
    @Override
    public BlockGroup buildBlockGroup() {
        return new BlockGroup(mContext, mHelper) {
            {
                setChildrenDrawingOrderEnabled(true);
            }

            @Override
            protected int getChildDrawingOrder(int childCount, int i) {
                return childCount - i - 1;
            }
        };
    }

    /**
     * @return Whether blocks without previous or output should have a bump.
     */
    public boolean isBlockHatsEnabled() {
        return mUseHats;
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
    protected FieldView buildFieldView(Field field) {
        @Field.FieldType int type = field.getType();
        int layoutResId = getLayoutForField(type);
        FieldView fieldView = null;
        // If we have a layout for this field type load that and return it
        if (layoutResId != 0) {
            fieldView = (FieldView) mLayoutInflater.inflate(layoutResId, null);
            fieldView.setField(field);
        }

        // Field specific configuration can be done here.
        switch (type) {
            case Field.TYPE_COLOR: {
                FieldColorView colorView = (FieldColorView) fieldView;
                colorView.setWorkspaceHelper(mHelper);
                break;
            }
            case Field.TYPE_VARIABLE: {
                BasicFieldVariableView varView = (BasicFieldVariableView) fieldView;
                varView.setAdapter(getVariableAdapter());
                varView.setVariableRequestCallback(mVariableCallback);
                break;
            }
            default:
                if (fieldView == null) {
                    fieldView = super.buildFieldView(field);
                }
                break;
        }
        return fieldView;
    }

    @Override
    protected SpinnerAdapter getVariableAdapter() {
        if (mVariableNameManager == null) {
            throw new IllegalStateException("NameManager must be set before variable field is "
                    + "instantiated.");
        }
        if (mVariableAdapter == null) {
            BasicFieldVariableView.VariableViewAdapter
                    adapter = new BasicFieldVariableView.VariableViewAdapter(mContext, mVariableNameManager,
                    R.layout.default_spinner_closed_item);
            adapter.setDropDownViewResource(R.layout.default_spinner_dropdown_item);
            mVariableAdapter = adapter;
        }
        return mVariableAdapter;
    }

    /**
     * Sets a layout to inflate for the given field type. The layout file must contain a subclass
     * of the appropriate field as its only top element. Setting the resource id to 0 will clear
     * it.
     *
     * @param fieldType The type of field this layout should be used for.
     * @param layoutResId The layout resource id to inflate when creating views for this field type.
     */
    protected final void setFieldLayout(@Field.FieldType int fieldType, int layoutResId) {
        if (layoutResId == 0) {
            mFieldLayouts.delete(fieldType);
        } else {
            mFieldLayouts.put(fieldType, layoutResId);
        }
    }

    /**
     * Gets the layout resource id for a given field type or 0 if none exist.
     *
     * @param fieldType The field type to get a layout for.
     * @return The layout resource id or 0 if not found.
     */
    protected final int getLayoutForField(@Field.FieldType int fieldType) {
        return mFieldLayouts.get(fieldType);
    }

    /**
     * Loads the style configurations using the selected style (if not 0), or from context's theme.
     */
    private void loadStyleData(int workspaceTheme) {
        TypedArray styles;

        if (workspaceTheme != 0) {
            styles = mContext.obtainStyledAttributes(
                    workspaceTheme, R.styleable.BlocklyVertical);
        } else {
            styles = mContext.obtainStyledAttributes(R.styleable.BlocklyVertical);
        }
        try {
            mUseHats = styles.getBoolean(R.styleable.BlocklyVertical_blockHat, false);

            setFieldLayout(Field.TYPE_DROPDOWN, R.layout.default_field_dropdown);
            setFieldLayout(Field.TYPE_LABEL, R.layout.default_field_label);
            setFieldLayout(Field.TYPE_CHECKBOX, R.layout.default_field_checkbox);
            setFieldLayout(Field.TYPE_DATE, R.layout.default_field_date);
            setFieldLayout(Field.TYPE_ANGLE, R.layout.default_field_angle);
            setFieldLayout(Field.TYPE_NUMBER, R.layout.default_field_number);
            setFieldLayout(Field.TYPE_COLOR, R.layout.default_field_color);
            setFieldLayout(Field.TYPE_INPUT, R.layout.default_field_input);
            setFieldLayout(Field.TYPE_VARIABLE, R.layout.default_field_variable);
        } finally {
            styles.recycle();
        }
    }
}
