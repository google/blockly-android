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
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import com.google.blockly.model.Block;
import com.google.blockly.model.Field;
import com.google.blockly.model.Input;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class BlockView extends FrameLayout {
    // TODO: Replace these with dimens so they get scaled correctly
    // The minimum height of a rendered block, in dips.
    private static final float BASE_HEIGHT = 48;
    // The minimum width of a rendered block, in dips.
    private static final float BASE_WIDTH = 48;
    // The distance between fields, in dips.
    private static final float FIELD_SPACING = 4;
    // The radius for rounding corners of a block, in dips.
    private static final float CORNER_RADIUS = 4;

    private final WorkspaceHelper mHelper;
    private final Block mBlock;
    private final Paint mPaint;


    private ArrayList<ArrayList<FieldView>> mFieldViews = new ArrayList<>();

    public BlockView(Context context, AttributeSet attrs, Block block, WorkspaceHelper helper) {
        super(context, attrs);
        mBlock = block;
        mHelper = helper;
        mPaint = new Paint();

        initPaint(context, attrs);
        initViews(context, attrs);
    }

    @Override
    public void onDraw(Canvas c) {
        c.drawRect(0, 0, getWidth(), getHeight(), mPaint);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        float width = BASE_WIDTH;
        float height = BASE_HEIGHT;

        for (int i = 0; i < mFieldViews.size(); i++) {
            int maxHeight = (int) BASE_HEIGHT;
            ArrayList<FieldView> currentRow = mFieldViews.get(i);
            for (int j = 0; j < currentRow.size(); j++) {
                FieldView child = currentRow.get(j);
                width += child.getInBlockWidth() + FIELD_SPACING;
                maxHeight = Math.max(maxHeight, child.getInBlockHeight());
            }
            // TODO: handle inline inputs
            height += maxHeight;
        }
        setMeasuredDimension((int) width, (int) height);
    }

    private void initViews(Context context, AttributeSet attrs) {
        List<Input> inputs = mBlock.getInputs();
        for (int i = 0; i < inputs.size(); i++) {
            // TODO: draw appropriate input, handle inline inputs
            ArrayList<FieldView> currentRow = new ArrayList<>();
            mFieldViews.add(currentRow);
            List<Field> fields = inputs.get(i).getFields();
            for (int j = 0; j < fields.size(); j++) {
                // TODO: create the appropriate field type
                FieldLabelView view = new FieldLabelView(context, attrs, fields.get(j));
                addView(view);
                currentRow.add(view);
            }
        }
    }

    private void initPaint(Context context, AttributeSet attrs) {
        mPaint.setColor(mBlock.getColour());
    }
}
