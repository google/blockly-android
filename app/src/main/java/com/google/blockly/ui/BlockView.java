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
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;
import android.widget.FrameLayout;

import com.google.blockly.R;
import com.google.blockly.control.ConnectionManager;
import com.google.blockly.model.Block;
import com.google.blockly.model.Input;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws a block and handles laying out all its inputs/fields.
 */
public class BlockView extends FrameLayout {
    private static final String TAG = "BlockView";

    // TODO: Replace these with dimens so they get scaled correctly
    // Minimum height of a block should be the same as an empty field.
    private static final int MIN_HEIGHT = InputView.MIN_HEIGHT;
    // Minimum width of a block should be the same as an empty field.
    private static final int MIN_WIDTH = InputView.MIN_WIDTH;

    // Color of block outline.
    private static final int OUTLINE_COLOR = Color.BLACK;

    private BlockWorkspaceParams mWorkspaceParams;
    private final WorkspaceHelper mHelper;
    private final Block mBlock;
    private final ConnectionManager mConnectionManager;

    // Objects for drawing the block.
    private final Path mDrawPath = new Path();
    private final Paint mPaintArea = new Paint();
    private final Paint mPaintBorder = new Paint();
    private final ArrayList<InputView> mInputViews = new ArrayList<>();

    // Current measured size of this block view.
    private final ViewPoint mBlockViewSize = new ViewPoint();
    // Position of the connection currently being updated, for temporary use during updateDrawPath.
    private final ViewPoint mTempConnectionPosition = new ViewPoint();

    // Layout coordinates for inputs in this Block, so they don't have to be computed repeatedly.
    private final ArrayList<ViewPoint> mInputLayoutOrigins = new ArrayList<>();

    // List of widths of multi-field rows when rendering inline inputs.
    private final ArrayList<Integer> mInlineRowWidth = new ArrayList<>();

    // Offset of the block origin inside the view's measured area.
    private int mLayoutMarginLeft;
    private int mMaxInputFieldsWidth;
    private int mMaxStatementFieldsWidth;

    // Vertical offset for positioning the "Next" block (if one exists).
    private int mNextBlockVerticalOffset;

    // Width of the core "block", ie, rectangle box without connectors or inputs.
    private int mBlockWidth;

    /**
     * Create a new BlockView for the given block using the workspace's style.
     * This constructor is for non-interactive display blocks. If this block is part of a
     * {@link Workspace} or (TODO linkify) Toolbox
     * {@link BlockView(Context, int, Block, WorkspaceHelper, BlockGroup, View.OnTouchListener)}
     * should be used instead.
     *
     * @param context     The context for creating this view.
     * @param block       The {@link Block} represented by this view.
     * @param helper      The helper for loading workspace configs and doing calculations.
     * @param parentGroup The {@link BlockGroup} this view will live in.
     * @param connectionManager The {@link ConnectionManager} to update when moving connections.
     */
    public BlockView(Context context, Block block, WorkspaceHelper helper, BlockGroup parentGroup,
                     ConnectionManager connectionManager) {
        this(context, 0 /* default style */, block, helper, parentGroup, null, connectionManager);
    }

    /**
     * Create a new BlockView for the given block using the specified style. The style must extend
     * {@link R.style#DefaultBlockStyle}.
     *
     * @param context     The context for creating this view.
     * @param blockStyle  The resource id for the style to use on this view.
     * @param block       The {@link Block} represented by this view.
     * @param helper      The helper for loading workspace configs and doing calculations.
     * @param parentGroup The {@link BlockGroup} this view will live in.
     * @param listener   An onTouchListener to register on this view.
     * @param connectionManager The {@link ConnectionManager} to update when moving connections.
     */
    public BlockView(Context context, int blockStyle, Block block, WorkspaceHelper helper,
                     BlockGroup parentGroup, View.OnTouchListener listener,
                     ConnectionManager connectionManager) {
        super(context, null, 0);

        mBlock = block;
        mConnectionManager = connectionManager;
        mHelper = helper;
        mWorkspaceParams = new BlockWorkspaceParams(mBlock, mHelper);
        parentGroup.addView(this);
        block.setView(this);

        setWillNotDraw(false);

        initViews(context, blockStyle, parentGroup, listener);
        initDrawingObjects(context);
        setOnTouchListener(listener);
    }

    /**
     * @return The {@link InputView} for the {@link Input} at the given index.
     */
    public InputView getInputView(int index) {
        return mInputViews.get(index);
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

        if (getBlock().getInputsInline()) {
            onMeasureInlineInputs(widthMeasureSpec, heightMeasureSpec);
        } else {
            onMeasureExternalInputs(widthMeasureSpec, heightMeasureSpec);
        }

        mNextBlockVerticalOffset = mBlockViewSize.y;
        if (mBlock.getNextConnection() != null) {
            mBlockViewSize.y += ConnectorHelper.SIZE_PERPENDICULAR;
        }

        if (mBlock.getOutputConnection() != null) {
            mLayoutMarginLeft = ConnectorHelper.SIZE_PERPENDICULAR;
            mBlockViewSize.x += mLayoutMarginLeft;
        } else {
            mLayoutMarginLeft = 0;
        }

        setMeasuredDimension(mBlockViewSize.x, mBlockViewSize.y);
        mWorkspaceParams.setMeasuredDimensions(mBlockViewSize);
    }

    @Override
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        // Note that layout must be done regardless of the value of the "changed" parameter.

        boolean rtl = mHelper.useRtL();
        int rtlSign = rtl ? -1 : +1;

        int xFrom = rtl ? mBlockViewSize.x - mLayoutMarginLeft : mLayoutMarginLeft;
        for (int i = 0; i < mInputViews.size(); i++) {
            int rowTop = mInputLayoutOrigins.get(i).y;

            InputView inputView = mInputViews.get(i);
            int inputViewWidth = inputView.getMeasuredWidth();
            int rowFrom = xFrom + rtlSign * mInputLayoutOrigins.get(i).x;
            if (rtl) {
                rowFrom -= inputViewWidth;
            }

            inputView.layout(rowFrom, rowTop, rowFrom + inputViewWidth,
                    rowTop + inputView.getMeasuredHeight());
        }

        updateDrawPath();
    }

    /**
     * @return The block for this view.
     */
    public Block getBlock() {
        return mBlock;
    }

    /**
     * Measure view and its children with inline inputs.
     * <p>
     * This function does not return a value but has the following side effects. Upon return:
     * <ol>
     * <li>The {@link InputView#measure(int, int)} method has been called for all inputs in
     * this block,</li>
     * <li>{@link #mBlockViewSize} contains the size of the total size of the block view
     * including all its inputs, and</li>
     * <li>{@link #mInputLayoutOrigins} contains the layout positions of all inputs within
     * the block.</li>
     * </ol>
     * </p>
     */
    private void onMeasureInlineInputs(int widthMeasureSpec, int heightMeasureSpec) {
        // First pass - measure all fields and inputs; compute maximum width of fields over all
        // Statement inputs.
        mMaxStatementFieldsWidth = 0;
        for (int i = 0; i < mInputViews.size(); i++) {
            InputView inputView = mInputViews.get(i);
            inputView.measureFieldsAndInputs(widthMeasureSpec, heightMeasureSpec);
            if (inputView.getInput().getType() == Input.TYPE_STATEMENT) {
                mMaxStatementFieldsWidth =
                        Math.max(mMaxStatementFieldsWidth, inputView.getTotalFieldWidth());

            }
        }

        // Second pass - compute layout positions and sizes of all inputs.
        int rowLeft = 0;
        int rowTop = 0;

        int rowHeight = 0;
        int maxRowWidth = 0;

        mInlineRowWidth.clear();
        for (int i = 0; i < mInputViews.size(); i++) {
            InputView inputView = mInputViews.get(i);

            // If this is a Statement input, force its field width to be the maximum over all
            // Statements, and begin a new layout row.
            if (inputView.getInput().getType() == Input.TYPE_STATEMENT) {
                // If the first input is a Statement, add vertical space for drawing connector top.
                if (i == 0) {
                    rowTop += ConnectorHelper.STATEMENT_INPUT_BOTTOM_HEIGHT;
                }

                // Force all Statement inputs to have the same field width.
                inputView.setFieldLayoutWidth(mMaxStatementFieldsWidth);

                // New row BEFORE each Statement input.
                mInlineRowWidth.add(Math.max(rowLeft,
                        mMaxStatementFieldsWidth + ConnectorHelper.STATEMENT_INPUT_INDENT_WIDTH));

                rowTop += rowHeight;
                rowHeight = 0;
                rowLeft = 0;
            }

            mInputLayoutOrigins.get(i).set(rowLeft, rowTop);

            // Measure input view and update row height and width accordingly.
            inputView.measure(widthMeasureSpec, heightMeasureSpec);
            rowHeight = Math.max(rowHeight, inputView.getMeasuredHeight());

            if (inputView.getInput().getType() == Input.TYPE_STATEMENT) {
                // The block width is that of the widest row.
                maxRowWidth = Math.max(maxRowWidth, inputView.getMeasuredWidth());

                // New row AFTER each Statement input.
                rowTop += rowHeight;
                rowLeft = 0;
                rowHeight = 0;
            } else {
                // For Dummy and Value inputs, row width accumulates. Update maximum width
                // accordingly.
                rowLeft += inputView.getMeasuredWidth();
                maxRowWidth = Math.max(maxRowWidth, rowLeft);
            }
        }

        // If there was at least one Statement input, make sure block is wide enough to fit at least
        // an empty Statement connector. If there were non-empty Statement connectors, they were
        // already taken care of in the loop above.
        if (mMaxStatementFieldsWidth > 0) {
            maxRowWidth = Math.max(maxRowWidth,
                    mMaxStatementFieldsWidth + ConnectorHelper.STATEMENT_INPUT_INDENT_WIDTH);
        }

        // Add height of final row. This is non-zero with inline inputs if the final input in the
        // block is not a Statement input.
        rowTop += rowHeight;

        // Push width of last input row.
        mInlineRowWidth.add(Math.max(rowLeft,
                mMaxStatementFieldsWidth + ConnectorHelper.STATEMENT_INPUT_INDENT_WIDTH));

        // Block width is the computed width of the widest input row, and at least MIN_WIDTH.
        mBlockViewSize.x = Math.max(MIN_WIDTH, maxRowWidth);
        mBlockWidth = mBlockViewSize.x;

        // Height is vertical position of next (non-existent) inputs row, and at least MIN_HEIGHT.
        mBlockViewSize.y = Math.max(MIN_HEIGHT, rowTop);
    }

    /**
     * Measure view and its children with external inputs.
     * <p>
     * This function does not return a value but has the following side effects. Upon return:
     * <ol>
     * <li>The {@link InputView#measure(int, int)} method has been called for all inputs in
     * this block,</li>
     * <li>{@link #mBlockViewSize} contains the size of the total size of the block view
     * including all its inputs, and</li>
     * <li>{@link #mInputLayoutOrigins} contains the layout positions of all inputs within
     * the block (but note that for external inputs, only the y coordinate of each
     * position is later used for positioning.)</li>
     * </ol>
     * </p>
     */
    private void onMeasureExternalInputs(int widthMeasureSpec, int heightMeasureSpec) {
        mMaxInputFieldsWidth = MIN_WIDTH;
        // Initialize max Statement width as zero so presence of Statement inputs can be determined
        // later; apply minimum size after that.
        mMaxStatementFieldsWidth = 0;

        int maxInputChildWidth = 0;
        int maxStatementChildWidth = 0;

        // First pass - measure fields and children of all inputs.
        boolean hasValueInput = false;
        for (int i = 0; i < mInputViews.size(); i++) {
            InputView inputView = mInputViews.get(i);
            inputView.measureFieldsAndInputs(widthMeasureSpec, heightMeasureSpec);

            switch (inputView.getInput().getType()) {
                case Input.TYPE_VALUE: {
                    hasValueInput = true;
                    maxInputChildWidth =
                            Math.max(maxInputChildWidth, inputView.getTotalChildWidth());
                    // fall through
                }
                default:
                case Input.TYPE_DUMMY: {
                    mMaxInputFieldsWidth =
                            Math.max(mMaxInputFieldsWidth, inputView.getTotalFieldWidth());
                    break;
                }
                case Input.TYPE_STATEMENT: {
                    mMaxStatementFieldsWidth =
                            Math.max(mMaxStatementFieldsWidth, inputView.getTotalFieldWidth());
                    maxStatementChildWidth =
                            Math.max(maxStatementChildWidth, inputView.getTotalChildWidth());
                    break;
                }
            }
        }

        // If there was a statement, force all other input fields to be at least as wide as required
        // by the Statement field plus port width.
        if (mMaxStatementFieldsWidth > 0) {
            mMaxStatementFieldsWidth = Math.max(mMaxStatementFieldsWidth, MIN_WIDTH);
            mMaxInputFieldsWidth = Math.max(mMaxInputFieldsWidth,
                    mMaxStatementFieldsWidth + ConnectorHelper.STATEMENT_INPUT_INDENT_WIDTH);
        }

        // Second pass - force all inputs to render fields with the same width and compute positions
        // for all inputs.
        int rowTop = 0;
        for (int i = 0; i < mInputViews.size(); i++) {
            InputView inputView = mInputViews.get(i);
            if (inputView.getInput().getType() == Input.TYPE_STATEMENT) {
                // If the first input is a Statement, add vertical space for drawing connector top.
                if (i == 0) {
                    rowTop += ConnectorHelper.STATEMENT_INPUT_BOTTOM_HEIGHT;
                }

                // Force all Statement inputs to have the same field width.
                inputView.setFieldLayoutWidth(mMaxStatementFieldsWidth);
            } else {
                // Force all Dummy and Value inputs to have the same field width.
                inputView.setFieldLayoutWidth(mMaxInputFieldsWidth);
            }
            inputView.measure(widthMeasureSpec, heightMeasureSpec);

            mInputLayoutOrigins.get(i).set(0, rowTop);

            // The block height is the sum of all the row heights.
            rowTop += inputView.getMeasuredHeight();
            if (inputView.getInput().getType() == Input.TYPE_STATEMENT) {
                rowTop += ConnectorHelper.STATEMENT_INPUT_BOTTOM_HEIGHT;
            }
        }

        // Block width is the width of the longest row. Add space for connector if there is at least
        // one Value input.
        mBlockWidth = Math.max(mMaxInputFieldsWidth, mMaxStatementFieldsWidth);
        if (hasValueInput) {
            mBlockWidth += ConnectorHelper.SIZE_PERPENDICULAR;
        }

        // The width of the block view is the width of the block plus the maximum width of any of
        // its children. If there are no children, make sure view is at least as wide as the Block,
        // which accounts for width of unconnected input puts.
        mBlockViewSize.x = Math.max(mBlockWidth,
                Math.max(mMaxInputFieldsWidth + maxInputChildWidth,
                        mMaxStatementFieldsWidth + maxStatementChildWidth));
        mBlockViewSize.y = Math.max(MIN_HEIGHT, rowTop);
    }

    /**
     * A block is responsible for initializing the views all of its fields and sub-blocks,
     * meaning both inputs and next blocks.
     * @param parentGroup The group the current block and all next blocks live in.
     * @param listener An onTouchListener to register on every sub-block.
     */
    private void initViews(Context context, int blockStyle, BlockGroup parentGroup,
                           View.OnTouchListener listener) {
        List<Input> inputs = mBlock.getInputs();
        for (int i = 0; i < inputs.size(); i++) {
            Input in = inputs.get(i);
            // TODO: draw appropriate input, handle inline inputs
            InputView inputView = new InputView(context, blockStyle, in, mHelper);
            mInputViews.add(inputView);
            addView(inputView);
            if (in.getType() != Input.TYPE_DUMMY && in.getConnection().getTargetBlock() != null) {
                // Blocks connected to inputs live in their own BlockGroups.
                BlockGroup bg = new BlockGroup(context, mHelper);
                mHelper.obtainBlockView(in.getConnection().getTargetBlock(), bg, listener,
                        mConnectionManager);
                inputView.setChildView(bg);
            }
        }

        if (getBlock().getNextBlock() != null) {
            // Next blocks live in the same BlockGroup.
            mHelper.obtainBlockView(getBlock().getNextBlock(), parentGroup, listener,
                    mConnectionManager);
        }
    }

    private void initDrawingObjects(Context context) {
        mPaintArea.setColor(mBlock.getColour());
        mPaintArea.setStyle(Paint.Style.FILL);
        mPaintArea.setStrokeJoin(Paint.Join.ROUND);

        mPaintBorder.setColor(OUTLINE_COLOR);
        mPaintBorder.setStyle(Paint.Style.STROKE);
        mPaintBorder.setStrokeWidth(1);
        mPaintBorder.setStrokeJoin(Paint.Join.ROUND);

        mDrawPath.setFillType(Path.FillType.EVEN_ODD);
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
     * Update path for drawing the block after view size or layout have changed.
     */
    private void updateDrawPath() {
        // TODO(rohlfingt): refactor path drawing code to be more readable. (Will likely be
        // superseded by TODO: implement pretty block rendering.)
        mDrawPath.reset();

        int xFrom = mLayoutMarginLeft;
        int xTo = mLayoutMarginLeft;

        // For inline inputs, the upper horizontal coordinate of the block boundary varies by
        // section and changes after each Statement input. For external inputs, it is constant as
        // computed in onMeasureExternalInputs.
        int inlineRowIdx = 0;
        if (getBlock().getInputsInline()) {
            xTo += mInlineRowWidth.get(inlineRowIdx);
        } else {
            xTo += mBlockWidth;
        }

        boolean rtl = mHelper.useRtL();
        int rtlSign = rtl ? -1 : +1;

        // In right-to-left mode, mirror horizontal coordinates inside the measured view boundaries.
        if (rtl) {
            xFrom = mBlockViewSize.x - xFrom;
            xTo = mBlockViewSize.x - xTo;
        }

        int yTop = 0;
        int yBottom = mNextBlockVerticalOffset;

        // Top of the block, including "Previous" connector.
        mDrawPath.moveTo(xFrom, yTop);
        if (mBlock.getPreviousConnection() != null) {
            ConnectorHelper.addPreviousConnectorToPath(mDrawPath, xFrom, yTop, rtlSign);
            ConnectorHelper.getPreviousConnectionPosition(xFrom, yTop, rtlSign,
                    mTempConnectionPosition);
            mConnectionManager.moveConnectionTo(mBlock.getPreviousConnection(),
                    mTempConnectionPosition);
        }
        mDrawPath.lineTo(xTo, yTop);

        // Right-hand side of the block, including "Input" connectors.
        // TODO(rohlfingt): draw this on the opposite side in RTL mode.
        for (int i = 0; i < mInputViews.size(); ++i) {
            InputView inputView = mInputViews.get(i);
            ViewPoint inputLayoutOrigin = mInputLayoutOrigins.get(i);
            switch (inputView.getInput().getType()) {
                default:
                case Input.TYPE_DUMMY: {
                    break;
                }
                case Input.TYPE_VALUE: {
                    if (!getBlock().getInputsInline()) {
                        ConnectorHelper.addValueInputConnectorToPath(
                                mDrawPath, xTo, inputLayoutOrigin.y, rtlSign);
                        ConnectorHelper.getValueInputConnectionPosition(xTo, inputLayoutOrigin.y,
                                rtlSign, mTempConnectionPosition);
                        mConnectionManager.moveConnectionTo(inputView.getInput().getConnection(),
                                mTempConnectionPosition);
                    }
                    break;
                }
                case Input.TYPE_STATEMENT: {
                    int xOffset = xFrom + rtlSign * inputView.getFieldLayoutWidth();
                    int connectorHeight = inputView.getTotalChildHeight();

                    // For external inputs, the horizontal end coordinate of the connector bottom is
                    // the same as the one on top. For inline inputs, however, it is the next entry
                    // in the width-by-row table.
                    int xToBottom = xTo;
                    if (getBlock().getInputsInline()) {
                        ++inlineRowIdx;
                        xToBottom = mLayoutMarginLeft + mInlineRowWidth.get(inlineRowIdx);
                    }
                    ConnectorHelper.addStatementInputConnectorToPath(mDrawPath,
                            xTo, xToBottom, inputLayoutOrigin.y, xOffset, connectorHeight, rtlSign);
                    ConnectorHelper.getStatementInputConnectionPosition(inputLayoutOrigin.y,
                            xOffset, rtlSign, mTempConnectionPosition);
                    mConnectionManager.moveConnectionTo(inputView.getInput().getConnection(),
                            mTempConnectionPosition);
                    // Set new horizontal end coordinate for subsequent inputs.
                    xTo = xToBottom;
                    break;
                }
            }
        }
        mDrawPath.lineTo(xTo, yBottom);

        // Bottom of the block, including "Next" connector.
        if (mBlock.getNextConnection() != null) {
            ConnectorHelper.addNextConnectorToPath(mDrawPath, xFrom, yBottom, rtlSign);
            ConnectorHelper.getNextConnectionPosition(xFrom, yBottom, rtlSign, mTempConnectionPosition);
            mConnectionManager.moveConnectionTo(mBlock.getNextConnection(),
                    mTempConnectionPosition);
        }
        mDrawPath.lineTo(xFrom, yBottom);

        // Left-hand side of the block, including "Output" connector.
        if (mBlock.getOutputConnection() != null) {
            ConnectorHelper.addOutputConnectorToPath(mDrawPath, xFrom, yTop, rtlSign);
            ConnectorHelper.getOutputConnectionPosition(xFrom, yTop, rtlSign, mTempConnectionPosition);
            mConnectionManager.moveConnectionTo(mBlock.getOutputConnection(),
                    mTempConnectionPosition);
        }
        mDrawPath.lineTo(xFrom, yTop);
        // Draw an additional line segment over again to get a final rounded corner.
        mDrawPath.lineTo(xFrom + ConnectorHelper.OFFSET_FROM_CORNER, yTop);

        // Add cutout paths for "holes" from open inline Value inputs.
        if (getBlock().getInputsInline()) {
            for (int i = 0; i < mInputViews.size(); ++i) {
                InputView inputView = mInputViews.get(i);
                if (inputView.getInput().getType() == Input.TYPE_VALUE) {
                    ViewPoint inputLayoutOrigin = mInputLayoutOrigins.get(i);
                    inputView.addInlineCutoutToBlockViewPath(mDrawPath,
                            xFrom + rtlSign * inputLayoutOrigin.x, inputLayoutOrigin.y, rtlSign,
                            mTempConnectionPosition);
                    mConnectionManager.moveConnectionTo(inputView.getInput().getConnection(),
                            mTempConnectionPosition);
                }
            }
        }

        mDrawPath.close();
    }

    /**
     * @return Vertical offset for positioning the "Next" block (if one exists). This is relative to
     * the top of this view's area.
     */
    int getNextBlockVerticalOffset() {
        return mNextBlockVerticalOffset;
    }

    /**
     * @return Layout margin on the left-hand side of the block (for optional Output connector).
     */
    int getLayoutMarginLeft() {
        return mLayoutMarginLeft;
    }
}
