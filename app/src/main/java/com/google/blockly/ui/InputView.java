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
import android.content.res.TypedArray;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import com.google.blockly.R;
import com.google.blockly.model.Field;
import com.google.blockly.model.Input;
import com.google.blockly.ui.fieldview.FieldAngleView;
import com.google.blockly.ui.fieldview.FieldCheckboxView;
import com.google.blockly.ui.fieldview.FieldColourView;
import com.google.blockly.ui.fieldview.FieldDateView;
import com.google.blockly.ui.fieldview.FieldDropdownView;
import com.google.blockly.ui.fieldview.FieldInputView;
import com.google.blockly.ui.fieldview.FieldLabelView;
import com.google.blockly.ui.fieldview.FieldView;

import java.util.ArrayList;
import java.util.List;

/**
 * View representation of an {@link Input} to a {@link com.google.blockly.model.Block}.
 */
public class InputView extends FrameLayout {
    private static final String TAG = "InputView";

    private final WorkspaceHelper mHelper;
    private final ArrayList<FieldView> mFieldViews = new ArrayList<>();

    // The horizontal distance between fields, in dips.
    private static final int DEFAULT_FIELD_SPACING = 10;
    // The minimum height of a input, in dips.
    private static final int BASE_HEIGHT = 80;

    private int mHorizontalFieldSpacing;

    InputView(Context context, int blockStyle, Input input, WorkspaceHelper helper) {
        super(context);

        mHelper = helper;

        initAttrs(context, blockStyle);
        initViews(context, input);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Height is maximum of base height and maximum height of any child.
        int height = BASE_HEIGHT;

        // Row width is sum of widths of all children plus spacing between them plus padding
        // on both sides, plus room for connector on each side.
        int width = 0;
        if (mFieldViews.size() > 1) {
            width += (mFieldViews.size() - 1) * mHorizontalFieldSpacing;
        }

        for (int j = 0; j < mFieldViews.size(); j++) {
            FieldView child = mFieldViews.get(j);
            ((View) child).measure(widthMeasureSpec, heightMeasureSpec);
            width += child.getInBlockWidth(); // Add each field's width
            // The row height is the height of the tallest element in that row
            height = Math.max(height, child.getInBlockHeight());
        }

        setMeasuredDimension(width, height);
    }

    @Override
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        boolean rtl = mHelper.useRtL();

        int currX = 0;
        if (rtl) {
            currX = getWidth() - currX;
        }

        for (int i = 0; i < mFieldViews.size(); i++) {
            FieldView fv = mFieldViews.get(i);
            View view = (View) fv;

            // Use the width and height of the field's view to compute its placement
            int w = fv.getInBlockWidth();
            int h = fv.getInBlockHeight();

            int l = rtl ? currX - w : currX;
            view.layout(l, 0, l + w, h);

            // Move x position left or right, depending on RTL mode.
            currX += (rtl ? -1 : +1) * (w + mHorizontalFieldSpacing);
        }
    }

    /**
     * Initialize style attributes.
     *
     * @param context Context of this view.
     * @param blockStyle The selected block style.
     */
    private void initAttrs(Context context, int blockStyle) {
        TypedArray a = context.obtainStyledAttributes(blockStyle, R.styleable.BlocklyBlockView);
        mHorizontalFieldSpacing = (int) a.getDimension(
                R.styleable.BlocklyBlockView_fieldHorizontalPadding, DEFAULT_FIELD_SPACING);
        Log.d(TAG, "Horizontal spacing=" + mHorizontalFieldSpacing + " from style " + blockStyle);
    }

    /**
     * Initialize child views for fields in the {@link Input} wrapped by this view.
     *
     * @param context Context of this view.
     * @param input The {@link Input} wrapped by this view.
     */
    private void initViews(Context context, Input input) {
        List<Field> fields = input.getFields();
        for (int j = 0; j < fields.size(); j++) {
            // TODO: create the appropriate field type
            // TODO: add a way to pass the field styles through
            FieldView view = null;
            switch (fields.get(j).getType()) {
                case Field.TYPE_LABEL:
                    view = new FieldLabelView(context, fields.get(j), mHelper);
                    break;
                case Field.TYPE_CHECKBOX:
                    view = new FieldCheckboxView(context, fields.get(j), mHelper);
                    break;
                case Field.TYPE_DATE:
                    view = new FieldDateView(context, fields.get(j), mHelper);
                    break;
                case Field.TYPE_DROPDOWN:
                    view = new FieldDropdownView(context, fields.get(j), mHelper);
                    break;
                case Field.TYPE_ANGLE:
                    view = new FieldAngleView(context, fields.get(j), mHelper);
                    break;
                case Field.TYPE_COLOUR:
                    view = new FieldColourView(context, fields.get(j), mHelper);
                    break;
                case Field.TYPE_INPUT:
                    view = new FieldInputView(context, fields.get(j), mHelper);
                    break;
                default:
                    Log.w(TAG, "Unknown field type.");
                    break;
            }
            if (view != null) {
                Log.d(TAG, "Added view " + view + " at " + j);
                addView((View) view);
                mFieldViews.add(view);
            } else {
                throw new IllegalStateException("Attempted to render a field of an unknown"
                        + "type: " + fields.get(j).getType());
            }
        }
    }
}
