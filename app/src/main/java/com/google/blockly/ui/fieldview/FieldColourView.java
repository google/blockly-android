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
import android.view.View;

import com.google.blockly.model.Field;
import com.google.blockly.ui.FieldWorkspaceParams;
import com.google.blockly.ui.WorkspaceHelper;

/**
 * Renders a color field and picker as part of a BlockView.
 */
public class FieldColourView extends View implements FieldView {
    private static final int ALPHA_OPAQUE = 0xFF000000;
    private static final int MIN_SIZE = 75;

    private final Field.FieldColour mColourField;
    private final WorkspaceHelper mWorkspaceHelper;
    private final FieldWorkspaceParams mWorkspaceParams;

    public FieldColourView(Context context, Field colourField, WorkspaceHelper helper) {
        super(context);

        mColourField = (Field.FieldColour) colourField;
        mWorkspaceHelper = helper;
        mWorkspaceParams = new FieldWorkspaceParams(mColourField, mWorkspaceHelper);

        setBackgroundColor(ALPHA_OPAQUE + mColourField.getColour());
        mColourField.setView(this);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(Math.max(getMeasuredWidth(), MIN_SIZE),
                Math.max(getMeasuredHeight(), MIN_SIZE));
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
