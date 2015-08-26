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

package com.google.blockly.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.CheckBox;

import com.google.blockly.model.Field;

/**
 * Renders a checkbox as part of a BlockView.
 */
public class FieldCheckboxView extends CheckBox implements FieldView {
    private final Field.FieldCheckbox mCheckbox;
    private final WorkspaceHelper mWorkspaceHelper;
    private FieldWorkspaceParams mLayoutParams;

    public FieldCheckboxView(Context context, AttributeSet attrs, Field checkbox,
                             WorkspaceHelper helper) {
        super(context, attrs);
        mCheckbox = (Field.FieldCheckbox) checkbox;
        mCheckbox.setView(this);
        mWorkspaceHelper = helper;
        mLayoutParams = new FieldWorkspaceParams(checkbox, helper);

        setChecked(mCheckbox.isChecked());
        setBackground(null);
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
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mLayoutParams.setPosition(new ViewPoint(left, top));
    }

    @Override
    public FieldWorkspaceParams getWorkspaceParams() {
        return mLayoutParams;
    }
}
