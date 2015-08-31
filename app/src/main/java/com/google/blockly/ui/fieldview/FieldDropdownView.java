/*
 *  Copyright  2015 Google Inc. All Rights Reserved.
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
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.google.blockly.model.Field;
import com.google.blockly.ui.FieldWorkspaceParams;
import com.google.blockly.ui.WorkspaceHelper;

/**
 * Renders a dropdown field as part of a Block.
 */
public class FieldDropdownView extends Spinner implements FieldView {
    private final Field.FieldDropdown mDropdownField;
    private final WorkspaceHelper mWorkspaceHelper;
    private final FieldWorkspaceParams mWorkspaceParams;

    public FieldDropdownView(Context context, Field dropdownField,
                         WorkspaceHelper helper) {
        super(context);

        mWorkspaceHelper = helper;
        mDropdownField = (Field.FieldDropdown) dropdownField;
        mWorkspaceParams = new FieldWorkspaceParams(mDropdownField, mWorkspaceHelper);

        setBackground(null);
        setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_item,
                mDropdownField.getDisplayNames()));
        dropdownField.setView(this);
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

}
