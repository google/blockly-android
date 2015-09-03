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

package com.google.blockly.ui.fieldview;

import android.content.Context;
import android.content.res.TypedArray;
import android.widget.TextView;

import com.google.blockly.R;
import com.google.blockly.model.Field;
import com.google.blockly.ui.FieldWorkspaceParams;
import com.google.blockly.ui.WorkspaceHelper;

/**
 * Renders text as part of a BlockView.
 */
public class FieldLabelView extends TextView implements FieldView {
    private final Field.FieldLabel mLabelField;
    private final WorkspaceHelper mWorkspaceHelper;
    private final FieldWorkspaceParams mWorkspaceParams;

    /**
     * Create a view for the given field using the workspace's style.
     *
     * @param context The context for creating the view and loading resources.
     * @param labelField The label this view is rendering.
     * @param helper The helper for loading workspace configs and doing calculations.
     */
    public FieldLabelView(Context context, Field labelField, WorkspaceHelper helper) {
        this(context, 0, labelField, helper);
    }

    /**
     * Create a view for the given field using the specified style. The style must be a resource id
     * for a style that extends {@link R.style#DefaultFieldLabelStyle}.
     *
     * @param context The context for creating the view and loading resources.
     * @param fieldLabelStyle The resource id for the style to use on this view.
     * @param labelField The label this view is rendering.
     * @param helper The helper for loading workspace configs and doing calculations.
     */
    public FieldLabelView(Context context, int fieldLabelStyle, Field labelField,
                          WorkspaceHelper helper) {
        super(context, null, 0);

        mLabelField = (Field.FieldLabel) labelField;
        mWorkspaceHelper = helper;
        mWorkspaceParams = new FieldWorkspaceParams(mLabelField, mWorkspaceHelper);

        configureStyle(context, fieldLabelStyle);

        setBackground(null);
        setText(mLabelField.getText());
        labelField.setView(this);
     }

    @Override
    public int getInBlockHeight() {
        return getMeasuredHeight();
    }

    @Override
    public int getInBlockWidth() {
        return getMeasuredWidth();
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mWorkspaceParams.setMeasuredDimensions(getMeasuredWidth(), getMeasuredHeight());
    }

    @Override
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            mWorkspaceParams.updateFromView(this);
        }
    }

    @Override
    public FieldWorkspaceParams getWorkspaceParams() {
        return mWorkspaceParams;
    }

    private void configureStyle(Context context, int style) {
        if (style == 0) {
            style = mWorkspaceHelper.getFieldLabelStyle();
        }
        TypedArray a = context.obtainStyledAttributes(style, R.styleable.BlocklyFieldView);
        int textStyle = a.getResourceId(R.styleable.BlocklyFieldView_textAppearance, 0);
        int minSize = (int) a.getDimension(R.styleable.BlocklyFieldView_fieldMinHeight, 0);
        if (textStyle != 0) {
            setTextAppearance(context, textStyle);
        }
    }
}
