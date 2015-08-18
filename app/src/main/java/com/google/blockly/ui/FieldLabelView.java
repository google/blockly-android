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

package com.google.blockly.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

import com.google.blockly.model.Field;

/**
 * Renders text as part of a BlockView.
 */
public class FieldLabelView extends TextView implements FieldView {
    private final Field mLabel; // Field.FieldLabel
    private final WorkspaceHelper mWorkspaceHelper;
    private FieldWorkspaceParams mLayoutParams;

    public FieldLabelView(Context context, AttributeSet attrs, Field label, WorkspaceHelper helper) {
        super(context, attrs);
        mLabel = label;
        mWorkspaceHelper = helper;
        setText(label.getName());
        setBackground(null);
        label.setView(this);
        mLayoutParams = new FieldWorkspaceParams(label, helper);
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
        mLayoutParams.setPosition(left, top);
    }

    @Override
    public FieldWorkspaceParams getWorkspaceParams() {
        return mLayoutParams;
    }
}
