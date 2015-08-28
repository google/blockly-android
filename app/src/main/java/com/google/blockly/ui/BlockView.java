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
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import com.google.blockly.model.Block;
import com.google.blockly.model.Field;
import com.google.blockly.model.Input;
import com.google.blockly.ui.fieldview.FieldAngleView;
import com.google.blockly.ui.fieldview.FieldCheckboxView;
import com.google.blockly.ui.fieldview.FieldDateView;
import com.google.blockly.ui.fieldview.FieldDropdownView;
import com.google.blockly.ui.fieldview.FieldLabelView;
import com.google.blockly.ui.fieldview.FieldView;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws a block and handles laying out all its inputs/fields.
 */
public class BlockView extends FrameLayout {
    private static final String TAG = "BlockView";
    private static final boolean DEBUG = false;

    // TODO: Replace these with dimens so they get scaled correctly
    // The minimum height of a rendered block, in dips.
    private static final int BASE_HEIGHT = 48;
    private static final int PADDING = 12;
    // The minimum width of a rendered block, in dips.
    private static final int BASE_WIDTH = 48;
    // The distance between fields, in dips.
    private static final int FIELD_SPACING = 4;
    // The radius for rounding corners of a block, in dips.
    private static final float CORNER_RADIUS = 4;

    private final WorkspaceHelper mHelper;
    private final Block mBlock;
    private final Paint mPaint;

    private BlockWorkspaceParams mWorkspaceParams;
    private ArrayList<ArrayList<FieldView>> mFieldViews = new ArrayList<>();
    private ViewPoint mTemp = new ViewPoint();

    public BlockView(Context context, AttributeSet attrs, Block block, WorkspaceHelper helper) {
        super(context, attrs);
        mBlock = block;
        mHelper = helper;
        mPaint = new Paint();
        mWorkspaceParams = new BlockWorkspaceParams(block, helper);
        setWillNotDraw(false);

        initPaint(context, attrs);
        initViews(context, attrs);
    }

    @Override
    public void onDraw(Canvas c) {
        c.drawRect(0, 0, getWidth(), getHeight(), mPaint);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // A block's dimensions are the width of the widest row by the height of all the rows
        // combined.
        int blockHeight = PADDING; // Start with the top padding
        int blockWidth = BASE_WIDTH;

        for (int i = 0; i < mFieldViews.size(); i++) {
            int rowHeight = 0;
            int rowWidth = PADDING; // Start at the padding distance into the block
            ArrayList<FieldView> currentRow = mFieldViews.get(i);
            // Add the spacing between fields.
            // If no fields this will subtract the spacing and end up smaller than the current max.
            rowWidth += (currentRow.size() - 1) * FIELD_SPACING;
            for (int j = 0; j < currentRow.size(); j++) {
                FieldView child = currentRow.get(j);
                ((View) child).measure(widthMeasureSpec, heightMeasureSpec);
                int cw = child.getInBlockWidth();
                rowWidth += cw; // Add each field's width
                // The row height is the height of the tallest element in that row
                rowHeight = Math.max(rowHeight, child.getInBlockHeight());
            }
            // TODO: handle inline inputs
            // The block height is the sum off all the row heights + spacing
            blockHeight += rowHeight;
            if (i != 0) {
                // The first row doesn't have spacing above it, so don't add it for the first view.
                blockHeight += FIELD_SPACING;
            }
            // The block width is the width of the widest row
            blockWidth = Math.max(blockWidth, rowWidth + PADDING);
        }
        // Height must be at least the base height
        blockHeight = Math.max(blockHeight + PADDING, BASE_HEIGHT);
        // TODO: handle statement inputs and Blocks under this view.

        setMeasuredDimension(blockWidth, blockHeight);
        mTemp.x = blockWidth;
        mTemp.y = blockHeight;
        mWorkspaceParams.setMeasuredDimensions(mTemp);
        if (DEBUG) {
            Log.d(TAG, "Set dimens to " + blockWidth + ", " + blockHeight + " for " + mFieldViews.size()
                    + " rows.");
        }
    }

    @Override
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        boolean rtl = mHelper.useRtL();
        List<Input> inputs = mBlock.getInputs();
        int currY = PADDING;
        for (int i = 0; i < inputs.size(); i++) {
            // Start padding distance in from the appropriate edge
            int currX = rtl ? getWidth() - PADDING : PADDING;
            int rowHeight = 0;
            Input input = inputs.get(i);
            List<Field> fields = input.getFields();
            for (int j = 0; j < fields.size(); j++) {
                Field field = fields.get(j);
                FieldView fv = field.getView();
                if (fv == null) {
                    throw new IllegalStateException("Attempted to render a field without a view: "
                            + field.getName());
                }
                View view = (View) fv;

                // Use the width and height of the field's view to compute its placement
                int w = view.getMeasuredWidth(), h = view.getMeasuredHeight();
                int l = rtl ? currX - w : currX;
                int r = rtl ? currX : currX + w;
                int t = currY;
                int b = currY + h;
                view.layout(l, t, r, b);

                // Move our current x position
                currX = rtl ? currX - (w + FIELD_SPACING) : currX + (w + FIELD_SPACING);
                rowHeight = Math.max(h, rowHeight);
            }
            // Between each row update the y position and add spacing
            currY += rowHeight + FIELD_SPACING;
            // TODO: handle inline inputs
        }
    }

    /**
     * @return The block for this view.
     */
    public Block getBlock() {
        return mBlock;
    }

    /**
     * A block is responsible for initializing all of its fields. Sub-blocks must be added manually
     * elsewhere.
     */
    private void initViews(Context context, AttributeSet attrs) {
        List<Input> inputs = mBlock.getInputs();
        for (int i = 0; i < inputs.size(); i++) {
            // TODO: draw appropriate input, handle inline inputs
            ArrayList<FieldView> currentRow = new ArrayList<>();
            mFieldViews.add(currentRow);
            List<Field> fields = inputs.get(i).getFields();
            for (int j = 0; j < fields.size(); j++) {
                // TODO: create the appropriate field type
                FieldView view = null;
                switch (fields.get(j).getType()) {
                    case Field.TYPE_LABEL:
                        view = new FieldLabelView(context, attrs, fields.get(j), mHelper);
                        break;
                    case Field.TYPE_CHECKBOX:
                        view = new FieldCheckboxView(context, attrs, fields.get(j), mHelper);
                        break;
                    case Field.TYPE_DATE:
                        view = new FieldDateView(context, attrs, fields.get(j), mHelper);
                        break;
                    case Field.TYPE_DROPDOWN:
                        view = new FieldDropdownView(context, attrs, fields.get(j), mHelper);
                        break;
                    case Field.TYPE_ANGLE:
                        view = new FieldAngleView(context, attrs, fields.get(j), mHelper);
                        break;
                    default:
                        Log.w(TAG, "Unknown field type.");
                        break;
                }
                if (view != null) {
                    Log.d(TAG, "Added view " + view + " at " + j);
                    addView((View)view);
                    currentRow.add(view);
                } else {
                    throw new IllegalStateException("Attempted to render a field of an unknown"
                        + "type: " + fields.get(j).getType());
                }
            }
        }
    }

    private void initPaint(Context context, AttributeSet attrs) {
        mPaint.setColor(mBlock.getColour());
    }
}
