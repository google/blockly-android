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

import com.google.blockly.android.control.ConnectionManager;
import com.google.blockly.android.ui.BlockTouchHandler;
import com.google.blockly.android.ui.BlockViewFactory;
import com.google.blockly.android.ui.WorkspaceHelper;
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

    private int mBlockStyle;

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

        mPatchManager = new PatchManager(mContext.getResources(), helper.useRtl());

        loadStyleData(workspaceTheme);
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
    protected FieldView buildFieldView(Field field) {
        switch (field.getType()) {
            // TODO(Anm): Inflate custom / styled variants.
            case Field.TYPE_COLOUR: {
                FieldColourView colourView = new FieldColourView(mContext);
                colourView.setWorkspaceHelper(mHelper);
                colourView.setField((Field.FieldColour) field);
                return colourView;
            }
            default:
                return super.buildFieldView(field);
        }
    }

    /**
     * @return The style resource id to use for drawing field labels.
     */
    int getBlockStyle() {
        return mBlockStyle;
    }

    /**
     * Loads the style configurations using the selected style (if not 0), or from context's theme.
     */
    private void loadStyleData(int workspaceTheme) {
        TypedArray styles;

        if (workspaceTheme != 0) {
            styles = mContext.obtainStyledAttributes(
                    workspaceTheme, R.styleable.BlocklyWorkspaceTheme);
        } else {
            styles = mContext.obtainStyledAttributes(R.styleable.BlocklyWorkspaceTheme);
        }
        try {
            mBlockStyle = styles.getResourceId(R.styleable.BlocklyWorkspaceTheme_blockViewStyle, 0);
        } finally {
            styles.recycle();
        }
    }
}
