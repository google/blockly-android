/*
 *  Copyright 2015 Google Inc. All Rights Reserved.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.google.blockly.ui.fieldview;

import android.content.Context;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.google.blockly.model.Field;
import com.google.blockly.ui.FieldWorkspaceParams;
import com.google.blockly.ui.WorkspaceHelper;

/**
 * Renders a checkbox as part of a BlockView.
 */
public class FieldCheckboxView extends CheckBox implements FieldView {

    private final Field.FieldCheckbox mCheckboxField;
    private final WorkspaceHelper mWorkspaceHelper;
    private final FieldWorkspaceParams mWorkspaceParams;

    public FieldCheckboxView(Context context, Field checkboxField, WorkspaceHelper helper) {
        super(context);

        mCheckboxField = (Field.FieldCheckbox) checkboxField;
        mWorkspaceHelper = helper;
        mWorkspaceParams = new FieldWorkspaceParams(mCheckboxField, mWorkspaceHelper);

        setBackground(null);
        setChecked(mCheckboxField.isChecked());
        mCheckboxField.setView(this);

        setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mCheckboxField.setChecked(isChecked);
            }
        });
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

    @Override
    public void unlinkModel() {
        mCheckboxField.setView(null);
        // TODO(#45): Remove model from view. Set mCheckboxField to null,
        //             and handle null cases above.
    }
}
