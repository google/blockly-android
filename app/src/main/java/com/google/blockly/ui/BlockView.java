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
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import com.google.blockly.R;
import com.google.blockly.model.Block;
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
 * Draws a block and handles laying out all its inputs/fields.
 */
public class BlockView extends FrameLayout {
    private static final String TAG = "BlockView";
    private static final boolean DEBUG = false;

    // TODO: Replace these with dimens so they get scaled correctly
    // The minimum height of a rendered block, in dips.
    private static final int BASE_HEIGHT = 80;
    // Padding inside the block, i.e., minimum distance between field bounds and block outline.
    private static final int PADDING = 12;
    // The minimum width of a rendered block, in dips.
    private static final int BASE_WIDTH = 48;
    // The distance between fields, in dips.
    private static final int FIELD_SPACING = 10;
    // Line width of block outline, in dips.
    private static final int OUTLINE_WIDTH = 10;
    // Color of block outline.
    private static final int OUTLINE_COLOR = Color.BLACK;
    // The radius for rounding corners of a block, in dips.
    private static final int CORNER_RADIUS = 8;
    // The offset between a connector and the closest corner, in dips.
    private static final int CONNECTOR_OFFSET = 20;
    // The "width" of a connector (parallel to the block boundary), in dips.
    private static final int CONNECTOR_WIDTH = 40;
    // The "height" of a connector (perpendicular to the block boundary), in dips.
    private static final int CONNECTOR_HEIGHT = 20;

    private final WorkspaceHelper mHelper;
    private final Block mBlock;

    // Objects for drawing the block.
    private final Path mDrawPath = new Path();
    private final Paint mPaintArea = new Paint();
    private final Paint mPaintBorder = new Paint();

    // Style resources for child fields
    private int mHorizontalFieldSpacing;
    private int mVerticalFieldSpacing;

    /**
     * Layout parameters for a block input row. These are computed in {@link #onMeasure} and used
     * in {@link #onLayout}.
     */
    private class InputLayoutParams {
        /** Vertical coordinate of the top of this input inside the block. */
        int mTop;
        /** Height of this input inside the block. */
        int mHeight;
        /** Width of this input inside the block. */
        int mWidth;

        InputLayoutParams(int top, int height, int width) {
            mTop = top;
            mHeight = height;
            mWidth = width;
        }
    }

    private ArrayList<InputLayoutParams> mInputLayoutParams = new ArrayList<>();

    private BlockWorkspaceParams mWorkspaceParams;
    private ArrayList<ArrayList<FieldView>> mFieldViews = new ArrayList<>();
    private ViewPoint mBlockViewSize = new ViewPoint();

    /**
     * Create a new BlockView for the given block using the workspace's style.
     *
     * @param context The context for creating this view.
     * @param block The block represented by this view.
     * @param helper The helper for loading workspace configs and doing calculations.
     */
    public BlockView(Context context, Block block, WorkspaceHelper helper) {
        this(context, 0 /* default style */, block, helper);
    }

    /**
     * Create a new BlockView for the given block using the specified style. The style must extend
     * {@link R.style#DefaultBlockStyle}.
     *
     * @param context The context for creating this view.
     * @param blockStyle The resource id for the style to use on this view.
     * @param block The block represented by this view.
     * @param helper The helper for loading workspace configs and doing calculations.
     */
    public BlockView(Context context, int blockStyle, Block block, WorkspaceHelper helper) {
        super(context, null, 0);

        mBlock = block;
        mHelper = helper;
        mWorkspaceParams = new BlockWorkspaceParams(mBlock, mHelper);

        setWillNotDraw(false);

        initAttrs(context, blockStyle);
        initViews(context);
        initDrawingObjects(context);
    }

    @Override
    public void onDraw(Canvas c) {
        c.drawPath(mDrawPath, mPaintArea);
        c.drawPath(mDrawPath, mPaintBorder);
    }

    /**
     * Measure all children (i.e., block inputs) and compute their sizes and relative positions
     * for use in {@link #onLayout}.
     */
    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // A block's width is at least the base width plus the "height" of an extruding "Output"
        // connector.
        int blockWidth = BASE_WIDTH + CONNECTOR_HEIGHT;

        mInputLayoutParams.clear();
        mInputLayoutParams.ensureCapacity(mFieldViews.size());

        // Top of first inputs row leaves room for padding plus intruding "Previous" connector.
        int rowTop = PADDING + CONNECTOR_HEIGHT;
        for (int i = 0; i < mFieldViews.size(); i++) {
            ArrayList<FieldView> currentRow = mFieldViews.get(i);

            // Add vertical spacing to previous block, if there is one.
            if (i > 0) {
                rowTop += mVerticalFieldSpacing;
            }

            // Row height is maximum of base height and maximum height of any child.
            int rowHeight = BASE_HEIGHT;

            // Row width is sum of widths of all children plus spacing between them plus padding
            // on both sides, plus room for connector on each side.
            int rowWidth = 2 * (PADDING + CONNECTOR_HEIGHT);
            if (currentRow.size() > 1) {
                rowWidth += (currentRow.size() - 1) * mHorizontalFieldSpacing;
            }

            for (int j = 0; j < currentRow.size(); j++) {
                FieldView child = currentRow.get(j);
                ((View) child).measure(widthMeasureSpec, heightMeasureSpec);
                rowWidth += child.getInBlockWidth(); // Add each field's width
                // The row height is the height of the tallest element in that row
                rowHeight = Math.max(rowHeight, child.getInBlockHeight());
            }
            mInputLayoutParams.add(new InputLayoutParams(rowTop, rowHeight, rowWidth));

            // TODO: handle inline inputs
            // The block height is the sum of all the row heights.
            rowTop += rowHeight;
            // The block width is that of the widest row
            blockWidth = Math.max(blockWidth, rowWidth);

        }
        // Height is vertical position of next (non-existant) inputs row plus bottom padding plus
        // room for extruding "Next" connector. Also must be at least the base height.
        int blockHeight = Math.max(rowTop + PADDING + CONNECTOR_HEIGHT, BASE_HEIGHT);

        setMeasuredDimension(blockWidth, blockHeight);
        mBlockViewSize.x = blockWidth;
        mBlockViewSize.y = blockHeight;
        mWorkspaceParams.setMeasuredDimensions(mBlockViewSize);
        if (DEBUG) {
            Log.d(TAG, "Set dimens to " + blockWidth + ", " + blockHeight + " for " + mFieldViews.size()
                    + " rows.");
        }
    }

    @Override
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        boolean rtl = mHelper.useRtL();
        List<Input> inputs = mBlock.getInputs();
        int currY = PADDING + CONNECTOR_HEIGHT;
        for (int i = 0; i < inputs.size(); i++) {
            // Start padding distance in from the appropriate edge
            int currX = PADDING + CONNECTOR_HEIGHT;
            if (rtl) {
                currX = getWidth() - currX;
            }

            Input input = inputs.get(i);
            List<Field> fields = input.getFields();
            for (int j = 0; j < fields.size(); j++) {
                Field field = fields.get(j);
                FieldView fv = field.getView();
                InputLayoutParams rowLayoutParams = mInputLayoutParams.get(i);

                if (fv == null) {
                    throw new IllegalStateException("Attempted to render a field without a view: "
                            + field.getName());
                }
                View view = (View) fv;

                // Use the width and height of the field's view to compute its placement
                int w = view.getMeasuredWidth();
                int h = view.getMeasuredHeight();
                int l = rtl ? currX - w : currX;
                int r = l + w;
                int t = rowLayoutParams.mTop + (rowLayoutParams.mHeight - h) / 2;
                int b = t + h;
                view.layout(l, t, r, b);

                // Move our current x position left or right, depending on RTL mode.
                currX += (rtl ? -1 : +1) * (w + mHorizontalFieldSpacing);
            }
        }
    }

    /**
     * @return The block for this view.
     */
    public Block getBlock() {
        return mBlock;
    }

    /**
     * Set up the block specific parameters defined in the attributes.
     */
    private void initAttrs(Context context, int blockStyle) {
        if (blockStyle == 0) {
            blockStyle = mHelper.getBlockStyle();
        }
        TypedArray a = context.obtainStyledAttributes(blockStyle, R.styleable.BlocklyBlockView);
        mHorizontalFieldSpacing = (int) a.getDimension(
                R.styleable.BlocklyBlockView_fieldHorizontalPadding, FIELD_SPACING);
        mVerticalFieldSpacing = (int) a.getDimension(
                R.styleable.BlocklyBlockView_fieldVerticalPadding, FIELD_SPACING);

        Log.d(TAG, "Horizontal spacing=" + mHorizontalFieldSpacing + ", Vertical spacing=" + mVerticalFieldSpacing
                + " from style " + blockStyle);
    }

    /**
     * A block is responsible for initializing all of its fields. Sub-blocks must be added
     * elsewhere.
     */
    private void initViews(Context context) {
        List<Input> inputs = mBlock.getInputs();
        for (int i = 0; i < inputs.size(); i++) {
            // TODO: draw appropriate input, handle inline inputs
            ArrayList<FieldView> currentRow = new ArrayList<>();
            mFieldViews.add(currentRow);
            List<Field> fields = inputs.get(i).getFields();
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
                    addView((View)view);
                    currentRow.add(view);
                } else {
                    throw new IllegalStateException("Attempted to render a field of an unknown"
                        + "type: " + fields.get(j).getType());
                }
            }
        }
    }

    private void initDrawingObjects(Context context) {
        mPaintArea.setColor(mBlock.getColour());
        mPaintArea.setStyle(Paint.Style.FILL);
        mPaintArea.setStrokeJoin(Paint.Join.ROUND);

        mPaintBorder.setColor(OUTLINE_COLOR);
        mPaintBorder.setStyle(Paint.Style.STROKE);
        mPaintBorder.setStrokeWidth(OUTLINE_WIDTH);
        mPaintBorder.setStrokeJoin(Paint.Join.ROUND);

        mDrawPath.setFillType(Path.FillType.EVEN_ODD);
    }

    /**
     * Update path for drawing the block after view size has changed.
     *
     * @param width The new width of the block view.
     * @param height The new height of the block view.
     * @param oldw The previous width of the block view (unused).
     * @param oldh The previous height of the block view (unused).
     */
    @Override
    protected void onSizeChanged (int width, int height, int oldw, int oldh) {
        mDrawPath.reset();

        float xLeft = CONNECTOR_HEIGHT;
        float xRight = width - CONNECTOR_HEIGHT;

        float yTop = 0;
        float yBottom = height - CONNECTOR_HEIGHT;

        // Top of the block, including "Previous" connector.
        mDrawPath.moveTo(xLeft, yTop);
        if (mBlock.getPreviousConnection() != null) {
            mDrawPath.lineTo(xLeft + CONNECTOR_OFFSET, yTop);
            mDrawPath.lineTo(xLeft + CONNECTOR_OFFSET, yTop + CONNECTOR_HEIGHT);
            mDrawPath.lineTo(xLeft + CONNECTOR_OFFSET + CONNECTOR_WIDTH, yTop + CONNECTOR_HEIGHT);
            mDrawPath.lineTo(xLeft + CONNECTOR_OFFSET + CONNECTOR_WIDTH, yTop);
        }
        mDrawPath.lineTo(xRight, yTop);

        // Right-hand side of the block, including "Input" connectors.
        // TODO(rohlfingt): draw this on the opposite side in RTL mode.
        List<Input> inputs = mBlock.getInputs();
        for (int i = 0; i < inputs.size(); ++i) {
            InputLayoutParams rowLayoutParams = mInputLayoutParams.get(i);
            int y = rowLayoutParams.mTop;
            switch (inputs.get(i).getType()) {
                default:
                case Input.TYPE_DUMMY: {
                    break;
                }
                case Input.TYPE_VALUE: {
                    mDrawPath.lineTo(xRight, y + CONNECTOR_OFFSET);
                    mDrawPath.lineTo(xRight - CONNECTOR_HEIGHT, y + CONNECTOR_OFFSET);
                    mDrawPath.lineTo(xRight - CONNECTOR_HEIGHT, y + CONNECTOR_OFFSET + CONNECTOR_WIDTH);
                    mDrawPath.lineTo(xRight, y + CONNECTOR_OFFSET + CONNECTOR_WIDTH);
                    break;
                }
                case Input.TYPE_STATEMENT: {
                    float xOffset = rowLayoutParams.mWidth;
                    mDrawPath.lineTo(xRight, y + CONNECTOR_OFFSET);
                    mDrawPath.lineTo(xOffset + 2 * CONNECTOR_WIDTH, y + CONNECTOR_OFFSET);
                    mDrawPath.lineTo(xOffset + 2 * CONNECTOR_WIDTH, y + CONNECTOR_OFFSET + CONNECTOR_HEIGHT);
                    mDrawPath.lineTo(xOffset + CONNECTOR_WIDTH, y + CONNECTOR_OFFSET + CONNECTOR_HEIGHT);
                    mDrawPath.lineTo(xOffset + CONNECTOR_WIDTH, y + CONNECTOR_OFFSET);
                    mDrawPath.lineTo(xOffset, y + CONNECTOR_OFFSET);
                    mDrawPath.lineTo(xOffset, y + CONNECTOR_OFFSET + 2 * CONNECTOR_HEIGHT);
                    mDrawPath.lineTo(xRight, y + CONNECTOR_OFFSET + 2 * CONNECTOR_HEIGHT);
                    break;
                }
            }
        }
        mDrawPath.lineTo(xRight, yBottom);

        // Bottom of the block, including "Next" connector.
        if (mBlock.getNextConnection() != null) {
            mDrawPath.lineTo(xLeft + CONNECTOR_OFFSET + CONNECTOR_WIDTH, yBottom);
            mDrawPath.lineTo(xLeft + CONNECTOR_OFFSET + CONNECTOR_WIDTH, yBottom + CONNECTOR_HEIGHT);
            mDrawPath.lineTo(xLeft + CONNECTOR_OFFSET, yBottom + CONNECTOR_HEIGHT);
            mDrawPath.lineTo(xLeft + CONNECTOR_OFFSET, yBottom);
        }
        mDrawPath.lineTo(xLeft, yBottom);

        // Left-hand side of the block, including "Output" connector.
        if (mBlock.getOutputConnection() != null) {
            mDrawPath.lineTo(xLeft, yTop + CONNECTOR_OFFSET + CONNECTOR_WIDTH);
            mDrawPath.lineTo(xLeft - CONNECTOR_HEIGHT, yTop + CONNECTOR_OFFSET + CONNECTOR_WIDTH);
            mDrawPath.lineTo(xLeft - CONNECTOR_HEIGHT, yTop + CONNECTOR_OFFSET );
            mDrawPath.lineTo(xLeft, yTop + CONNECTOR_OFFSET);
        }
        mDrawPath.lineTo(xLeft, yTop);

        mDrawPath.close();
    }
}
