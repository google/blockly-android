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
import android.widget.CheckBox;

import com.google.blockly.model.Field;
import com.google.blockly.ui.FieldWorkspaceParams;
import com.google.blockly.ui.ViewPoint;
import com.google.blockly.ui.WorkspaceHelper;

/**
 * Renders a checkbox as part of a BlockView.
 */
public class FieldCheckboxView extends CheckBox implements FieldView {
    private final Field.FieldCheckbox mCheckbox;
    private final WorkspaceHelper mWorkspaceHelper;
    private final FieldWorkspaceParams mLayoutParams;

    // ViewPoint object allocated once and reused in onLayout to prevent repeatedly allocating
    // objects during drawing.
    private final ViewPoint mTempViewPoint = new ViewPoint();

    public FieldCheckboxView(Context context, Field checkbox,
                             WorkspaceHelper helper) {
        super(context);
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
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mLayoutParams.setMeasuredDimensions(getMeasuredWidth(), getMeasuredHeight());
    }

    @Override
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mTempViewPoint.x = left;
        mTempViewPoint.y = top;
        mLayoutParams.setPosition(mTempViewPoint);
    }


    @Override
    public FieldWorkspaceParams getWorkspaceParams() {
        return mLayoutParams;
    }
}
