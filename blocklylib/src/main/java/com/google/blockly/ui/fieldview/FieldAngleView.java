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
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;

import com.google.blockly.model.Field;
import com.google.blockly.ui.FieldWorkspaceParams;
import com.google.blockly.ui.WorkspaceHelper;

/**
 * Renders an angle as part of a Block.
 */
public class FieldAngleView extends TextView implements FieldView {
    private final Field.FieldAngle mAngleField;
    private final WorkspaceHelper mWorkspaceHelper;
    private final FieldWorkspaceParams mWorkspaceParams;

    public FieldAngleView(Context context, Field angleField, WorkspaceHelper helper) {
        super(context);

        mAngleField = (Field.FieldAngle) angleField;
        mWorkspaceHelper = helper;
        mWorkspaceParams = new FieldWorkspaceParams(mAngleField, mWorkspaceHelper);

        setBackground(null);
        setText(Integer.toString(mAngleField.getAngle()));
        angleField.setView(this);

        addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                mAngleField.setFromString(s.toString());
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
        mAngleField.setView(null);
        // TODO(#45): Remove model from view. Set mAngleField to null, and handle null cases above.
    }
}
