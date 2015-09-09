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
import com.google.blockly.model.Input;

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
    // The vertical spacing between external inputs, in dips.
    private static final int DEFAULT_VERTICAL_SPACING = 10;
    // The horizontal spacing between internal inputs, in dips.
    private static final int DEFAULT_HORIZONTAL_SPACING = 10;
    // Line width of block outline, in dips.
    private static final int OUTLINE_WIDTH = 10;
    // Color of block outline.
    private static final int OUTLINE_COLOR = Color.BLACK;
    // The radius for rounding corners of a block, in dips.
    private static final int CORNER_RADIUS = 8;
    // The offset between a connector and the closest corner, in dips.
    private static final int CONNECTOR_OFFSET = 20;
    // The size of a connector parallel to the block boundary, in dips.
    private static final int CONNECTOR_SIZE_PARALLEL = 40;
    // The size of a connector perpendicular to the block boundary, in dips.
    private static final int CONNECTOR_SIZE_PERPENDICULAR = 20;

    private final WorkspaceHelper mHelper;
    private final Block mBlock;

    // Objects for drawing the block.
    private final Path mDrawPath = new Path();
    private final Paint mPaintArea = new Paint();
    private final Paint mPaintBorder = new Paint();

    // Style resources for child fields
    private int mHorizontalFieldSpacing;
    private int mVerticalFieldSpacing;

    private ArrayList<ViewPoint> mInputLayoutOrigins = new ArrayList<>();

    private BlockWorkspaceParams mWorkspaceParams;
    private ArrayList<InputView> mInputViews = new ArrayList<>();

    /** Offset of the block origin inside the view's measured area. */
    private final ViewPoint mBlockOriginOffset = new ViewPoint(CONNECTOR_SIZE_PERPENDICULAR, 0);

    /** Current measured size of this block view. */
    private final ViewPoint mBlockViewSize = new ViewPoint();

    /** Vertical offset for positioning the "Next" block (if one exists). */
    private int mNextBlockVerticalOffset;

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
        initViews(context, blockStyle);
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
        adjustInputLayoutOriginsListSize();

        // A block's width is at least the base width plus the "height" of an extruding "Output"
        // connector.
        int blockWidth = BASE_WIDTH + CONNECTOR_SIZE_PERPENDICULAR;

        // Top of first inputs row leaves room for padding plus intruding "Previous" connector.
        int rowTop = PADDING + CONNECTOR_SIZE_PERPENDICULAR;

        for (int i = 0; i < mInputViews.size(); i++) {
            InputView inputView = mInputViews.get(i);
            inputView.measure(widthMeasureSpec, heightMeasureSpec);

            // Add vertical spacing to previous row of fields, if there is one.
            if (i > 0) {
                rowTop += mVerticalFieldSpacing;
            }

            // TODO: handle inline inputs
            mInputLayoutOrigins.get(i).set(0, rowTop);
            // The block height is the sum of all the row heights.
            rowTop += inputView.getMeasuredHeight();
            // The block width is that of the widest row
            blockWidth = Math.max(blockWidth, inputView.getMeasuredWidth());
        }

        // Height is vertical position of next (non-existent) inputs row plus bottom padding plus
        // room for extruding "Next" connector. Also must be at least the base height.
        int blockHeight = Math.max(rowTop + PADDING + CONNECTOR_SIZE_PERPENDICULAR, BASE_HEIGHT);
        blockWidth += 2 * PADDING + 3 * CONNECTOR_SIZE_PERPENDICULAR;

        setMeasuredDimension(blockWidth, blockHeight);
        mBlockViewSize.x = blockWidth;
        mBlockViewSize.y = blockHeight;
        mWorkspaceParams.setMeasuredDimensions(mBlockViewSize);
        if (DEBUG) {
            Log.d(TAG, "Set dimens to " + blockWidth + ", " + blockHeight +
                    " for " + mInputViews.size() + " rows.");
        }

        mNextBlockVerticalOffset = blockHeight - CONNECTOR_SIZE_PERPENDICULAR;
    }

    @Override
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int xLeft = PADDING + CONNECTOR_SIZE_PERPENDICULAR;
        int xRight = mBlockViewSize.x - PADDING - 2 * CONNECTOR_SIZE_PERPENDICULAR;
        for (int i = 0; i < mInputViews.size(); i++) {
            int rowTop = mInputLayoutOrigins.get(i).y;
            InputView inputView = mInputViews.get(i);

            switch (inputView.getInput().getType()) {
                case Input.TYPE_VALUE: {
                    // Value inputs are drawn right-aligned with their input port.
                    inputView.layout(xRight - inputView.getMeasuredWidth(), rowTop,
                            xRight, rowTop + inputView.getMeasuredHeight());
                    break;
                }
                default: {
                    // Dummy and statement inputs are left-aligned with the block boundary.
                    // (Actually, statement inputs are centered, since the width of the rendered
                    // block is adjusted to match their exact width.)
                    inputView.layout(xLeft, rowTop, xLeft + inputView.getMeasuredWidth(),
                            rowTop + inputView.getMeasuredHeight());
                    break;
                }
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
     * @return Offset of the block origin inside the view's measured area. This is the lop-left
     * corner of the block outline, accounting for padding due to extruding connectors, outline
     * stroke width, etc.
     */
    ViewPoint getBlockOriginOffset() {
        return mBlockOriginOffset;
    }

    /**
     * @return Vertical offset for positioning the "Next" block (if one exists). This is relative to
     * the top of this view's area.
     */
    int getNextBlockVerticalOffset() {
        return mNextBlockVerticalOffset;
    }

    /**
     * Set up the block specific parameters defined in the attributes.
     */
    private void initAttrs(Context context, int blockStyle) {
        if (blockStyle == 0) {
            blockStyle = mHelper.getBlockStyle();
        }
        TypedArray a = context.obtainStyledAttributes(blockStyle, R.styleable.BlocklyBlockView);
        mVerticalFieldSpacing = (int) a.getDimension(
                R.styleable.BlocklyBlockView_fieldVerticalPadding, DEFAULT_VERTICAL_SPACING);
        mHorizontalFieldSpacing = (int) a.getDimension(
                R.styleable.BlocklyBlockView_fieldHorizontalPadding, DEFAULT_HORIZONTAL_SPACING);

        Log.d(TAG, "Vertical spacing=" + mVerticalFieldSpacing + " from style " + blockStyle);
    }

    /**
     * A block is responsible for initializing all of its fields. Sub-blocks must be added
     * elsewhere.
     */
    private void initViews(Context context, int blockStyle) {
        List<Input> inputs = mBlock.getInputs();
        for (int i = 0; i < inputs.size(); i++) {
            // TODO: draw appropriate input, handle inline inputs
            InputView inputView = new InputView(context, blockStyle, inputs.get(i), mHelper);
            mInputViews.add(inputView);
            addView(inputView);
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

        int xLeft = CONNECTOR_SIZE_PERPENDICULAR;
        int xRight = width - CONNECTOR_SIZE_PERPENDICULAR;

        int yTop = 0;
        int yBottom = height - CONNECTOR_SIZE_PERPENDICULAR;

        // Top of the block, including "Previous" connector.
        mDrawPath.moveTo(xLeft, yTop);
        if (mBlock.getPreviousConnection() != null) {
            addPreviousConnectorToPath(xLeft, yTop);
        }
        mDrawPath.lineTo(xRight, yTop);

        // Right-hand side of the block, including "Input" connectors.
        // TODO(rohlfingt): draw this on the opposite side in RTL mode.
        if (!getBlock().getInputsInline()) {
            for (int i = 0; i < mInputViews.size(); ++i) {
                InputView inputView = mInputViews.get(i);
                ViewPoint inputLayoutOrigin = mInputLayoutOrigins.get(i);
                switch (inputView.getInput().getType()) {
                    default:
                    case Input.TYPE_DUMMY: {
                        break;
                    }
                    case Input.TYPE_VALUE: {
                        addValueInputConnectorToPath(xRight, inputLayoutOrigin.y);
                        break;
                    }
                    case Input.TYPE_STATEMENT: {
                        int xOffset = inputView.getMeasuredWidth() +
                                2 * PADDING + CONNECTOR_SIZE_PERPENDICULAR;
                        addStatementInputConnectorToPath(xRight, inputLayoutOrigin.y, xOffset);
                        break;
                    }
                }
            }
        }
        mDrawPath.lineTo(xRight, yBottom);

        // Bottom of the block, including "Next" connector.
        if (mBlock.getNextConnection() != null) {
            addNextConnectorToPath(xLeft, yBottom);
        }
        mDrawPath.lineTo(xLeft, yBottom);

        // Left-hand side of the block, including "Output" connector.
        if (mBlock.getOutputConnection() != null) {
            addOutputConnectorToPath(xLeft, yTop);
        }
        mDrawPath.lineTo(xLeft, yTop);

        mDrawPath.close();
    }

    /**
     * Adjust size of {@link #mInputLayoutOrigins} list to match the size of {@link #mInputViews}.
     */
    private void adjustInputLayoutOriginsListSize() {
        if (mInputLayoutOrigins.size() != mInputViews.size()) {
            mInputLayoutOrigins.ensureCapacity(mInputViews.size());
            if (mInputLayoutOrigins.size() < mInputViews.size()) {
                for (int i = mInputLayoutOrigins.size(); i < mInputViews.size(); i++) {
                    mInputLayoutOrigins.add(new ViewPoint());
                }
            } else {
                while (mInputLayoutOrigins.size() > mInputViews.size()) {
                    mInputLayoutOrigins.remove(mInputLayoutOrigins.size() - 1);
                }
            }
        }
    }

    /**
     * Add a "Previous" connector to the block's draw path.
     * @param xFrom Horizontal view coordinate of the reference point for this connector.
     * @param yFrom Vertical view coordinate of the reference point for this connector.
     */
    private void addPreviousConnectorToPath(int xFrom, int yFrom) {
        mDrawPath.lineTo(xFrom + CONNECTOR_OFFSET, yFrom);
        mDrawPath.lineTo(xFrom + CONNECTOR_OFFSET, yFrom + CONNECTOR_SIZE_PERPENDICULAR);
        mDrawPath.lineTo(xFrom + CONNECTOR_OFFSET + CONNECTOR_SIZE_PARALLEL,
                yFrom + CONNECTOR_SIZE_PERPENDICULAR);
        mDrawPath.lineTo(xFrom + CONNECTOR_OFFSET + CONNECTOR_SIZE_PARALLEL, yFrom);
    }

    /**
     * Add a "Next" connector to the block's draw path.
     * @param xFrom Horizontal view coordinate of the reference point for this connector.
     * @param yFrom Vertical view coordinate of the reference point for this connector.
     */
    private void addNextConnectorToPath(int xFrom, int yFrom) {
        mDrawPath.lineTo(xFrom + CONNECTOR_OFFSET + CONNECTOR_SIZE_PARALLEL, yFrom);
        mDrawPath.lineTo(xFrom + CONNECTOR_OFFSET + CONNECTOR_SIZE_PARALLEL,
                yFrom + CONNECTOR_SIZE_PERPENDICULAR);
        mDrawPath.lineTo(xFrom + CONNECTOR_OFFSET, yFrom + CONNECTOR_SIZE_PERPENDICULAR);
        mDrawPath.lineTo(xFrom + CONNECTOR_OFFSET, yFrom);
    }

    /**
     * Add a Value input connector to the block's draw path.
     * @param xFrom Horizontal view coordinate of the reference point for this connector.
     * @param yFrom Vertical view coordinate of the reference point for this connector.
     */
    private void addValueInputConnectorToPath(int xFrom, int yFrom) {
        mDrawPath.lineTo(xFrom, yFrom + CONNECTOR_OFFSET);
        mDrawPath.lineTo(xFrom - CONNECTOR_SIZE_PERPENDICULAR, yFrom + CONNECTOR_OFFSET);
        mDrawPath.lineTo(xFrom - CONNECTOR_SIZE_PERPENDICULAR,
                yFrom + CONNECTOR_OFFSET + CONNECTOR_SIZE_PARALLEL);
        mDrawPath.lineTo(xFrom, yFrom + CONNECTOR_OFFSET + CONNECTOR_SIZE_PARALLEL);
    }

    /**
     * Add a Statement input connector to the block's draw path.
     * @param xFrom Horizontal view coordinate of the reference point for this connector.
     * @param yFrom Vertical view coordinate of the reference point for this connector.
     * @param xOffset The offset of the Statement input connector from the left (or right, in RTL
     *                mode) boundary of the block.
     */
    private void addStatementInputConnectorToPath(int xFrom, int yFrom, int xOffset) {
        mDrawPath.lineTo(xFrom, yFrom + CONNECTOR_OFFSET);
        mDrawPath.lineTo(xOffset + 2 * CONNECTOR_SIZE_PARALLEL, yFrom + CONNECTOR_OFFSET);
        mDrawPath.lineTo(xOffset + 2 * CONNECTOR_SIZE_PARALLEL,
                yFrom + CONNECTOR_OFFSET + CONNECTOR_SIZE_PERPENDICULAR);
        mDrawPath.lineTo(xOffset + CONNECTOR_SIZE_PARALLEL,
                yFrom + CONNECTOR_OFFSET + CONNECTOR_SIZE_PERPENDICULAR);
        mDrawPath.lineTo(xOffset + CONNECTOR_SIZE_PARALLEL, yFrom + CONNECTOR_OFFSET);
        mDrawPath.lineTo(xOffset, yFrom + CONNECTOR_OFFSET);
        mDrawPath.lineTo(xOffset, yFrom + CONNECTOR_OFFSET + 2 * CONNECTOR_SIZE_PERPENDICULAR);
        mDrawPath.lineTo(xFrom, yFrom + CONNECTOR_OFFSET + 2 * CONNECTOR_SIZE_PERPENDICULAR);
    }

    /**
     * Add a "Output" connector to the block's draw path.
     * @param xFrom Horizontal view coordinate of the reference point for this connector.
     * @param yFrom Vertical view coordinate of the reference point for this connector.
     */
    private void addOutputConnectorToPath(int xFrom, int yFrom) {
        mDrawPath.lineTo(xFrom, yFrom + CONNECTOR_OFFSET + CONNECTOR_SIZE_PARALLEL);
        mDrawPath.lineTo(xFrom - CONNECTOR_SIZE_PERPENDICULAR,
                yFrom + CONNECTOR_OFFSET + CONNECTOR_SIZE_PARALLEL);
        mDrawPath.lineTo(xFrom - CONNECTOR_SIZE_PERPENDICULAR, yFrom + CONNECTOR_OFFSET);
        mDrawPath.lineTo(xFrom, yFrom + CONNECTOR_OFFSET);
    }
}
