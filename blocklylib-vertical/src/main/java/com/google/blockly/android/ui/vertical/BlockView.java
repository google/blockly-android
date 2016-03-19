/*
* Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.blockly.android.ui.vertical;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.view.MotionEvent;

import com.google.blockly.android.control.ConnectionManager;
import com.google.blockly.android.ui.Dragger;
import com.google.blockly.android.ui.AbstractBlockView;
import com.google.blockly.android.ui.BlockTouchHandler;
import com.google.blockly.android.ui.ViewPoint;
import com.google.blockly.android.ui.WorkspaceView;
import com.google.blockly.model.Block;
import com.google.blockly.model.Connection;
import com.google.blockly.model.Input;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws a block and handles laying out all its inputs/fields.
 */
@SuppressLint("ViewConstructor")
public class BlockView extends AbstractBlockView<InputView> {
    private static final boolean DEBUG = false;
    // TODO(#86): Determine from 9-patch measurements.
    private static final int MIN_BLOCK_WIDTH = 40;

    private VerticalBlocksViewFactory mFactory;

    // Width  and height of the block "content", i.e., all its input fields. Unlike the view size,
    // this does not include extruding connectors (e.g., Output, Next) and connected input blocks.
    private int mBlockContentWidth;
    private int mBlockContentHeight;
    // Offset of the block origin inside the view's measured area.
    private int mLayoutMarginLeft;
    private int mMaxStatementFieldsWidth;
    // Vertical offset for positioning the "Next" block (if one exists).
    private int mNextBlockVerticalOffset;
    // Layout coordinates for inputs in this Block, so they don't have to be computed repeatedly.
    private final ArrayList<ViewPoint> mInputLayoutOrigins = new ArrayList<>();
    // List of widths of multi-field rows when rendering inline inputs.
    private final ArrayList<Integer> mInlineRowWidth = new ArrayList<>();

    // Objects for drawing the block.
    private final PatchManager mPatchManager;
    private final ArrayList<Drawable> mBlockPatches = new ArrayList<>();
    // Overlay patches used to draw a selection border when mHighlightBlock is true.
    private final ArrayList<Drawable> mBlockBorderPatches = new ArrayList<>();
    @Nullable private Drawable mOutputConnectorHighlightPatch = null;
    @Nullable private Drawable mPreviousConnectorHighlightPatch = null;
    @Nullable private Drawable mNextConnectionHighlightPatch = null;
    private final ArrayList<Drawable> mInputConnectionHighlightPatches = new ArrayList<>();

    private final ArrayList<Rect> mFillRects = new ArrayList<>();
    @Nullable private Rect mNextFillRect = null;
    private ColorFilter mBlockColorFilter;
    private final Paint mFillPaint = new Paint();

    // Flag is set to true if this block has at least one "Value" input.
    private boolean mHasValueInput = false;
    private int mInputCount;
    // Keeps track of if the current set of touch events had started on this block
    private boolean mHasHit = false;

    private final Rect tempRect = new Rect(); // Only use in main thread functions.

    /**
     * Create a new BlockView and associated InputViews for the given block using the
     * WorkspaceHelper's provided style.
     * <p>
     * App developers should not call this constructor directly.  Instead use
     * {@link VerticalBlocksViewFactory#buildBlockViewTree}.
     *
     * @param context The context for creating this view.
     * @param block The {@link Block} represented by this view.
     * @param factory TODO(Anm)
     * @param connectionManager The {@link ConnectionManager} to update when moving connections.
     * @param touchHandler The optional handler for forwarding touch events on this block to the
     *                     {@link Dragger}.
     */
    BlockView(Context context, Block block, VerticalBlocksViewFactory factory,
                     ConnectionManager connectionManager,
                     @Nullable BlockTouchHandler touchHandler) {
        super(context, factory.getWorkspaceHelper(), block, connectionManager, touchHandler);

        mFactory = factory;
        mPatchManager = mFactory.getPatchManager();  // Shortcut.

        mTouchHandler = touchHandler;

        setClickable(true);
        setFocusable(true);
        setWillNotDraw(false);

        // TODO(Anm): Factory should be responsible for traversing tree and adding views
        createInputViews();
        initDrawingObjects();  // TODO(Anm): Call from factory after all views are added.
    }

    /**
     * Test whether event hits visible parts of this block and notify {@link WorkspaceView} if it
     * does.
     *
     * @param event The {@link MotionEvent} to handle.
     *
     * @return False if the touch was on the view but not on a visible part of the block; otherwise
     * returns whether the {@link WorkspaceView} says that the event is being handled properly.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return hitTest(event) && mTouchHandler.onTouchBlock(this, event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return hitTest(event) && mTouchHandler.onInterceptTouchEvent(this, event);
    }

    @Override
    protected void onDraw(Canvas c) {
        for (int i = 0; i < mFillRects.size(); ++i) {
            c.drawRect(mFillRects.get(i), mFillPaint);
        }

        for (int i = 0; i < mBlockPatches.size(); ++i) {
            mBlockPatches.get(i).draw(c);
        }

        if (DEBUG) {
            drawConnectorCenters(c);  // Enable to debug connection positions.
        }
        drawHighlights(c);
    }

    /**
     * Measure all children (i.e., block inputs) and compute their sizes and relative positions
     * for use in {@link #onLayout}.
     */
    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (getBlock().getInputsInline()) {
            measureInlineInputs(widthMeasureSpec, heightMeasureSpec);
        } else {
            measureExternalInputs(widthMeasureSpec, heightMeasureSpec);
        }

        mNextBlockVerticalOffset = mBlockContentHeight;
        mBlockViewSize.y = mBlockContentHeight;
        if (mBlock.getNextConnection() != null) {
            mBlockViewSize.y += mPatchManager.mNextConnectorHeight;
        }

        if (mBlock.getOutputConnection() != null) {
            mLayoutMarginLeft = mPatchManager.mOutputConnectorWidth;
            mBlockViewSize.x += mLayoutMarginLeft;
        } else {
            mLayoutMarginLeft = 0;
        }

        setMeasuredDimension(mBlockViewSize.x, mBlockViewSize.y);
    }

    /**
     * @return Vertical offset for positioning the "Next" block (if one exists). This is relative to
     * the top of this view's area.
     */
    @Override
    public int getNextBlockVerticalOffset() {
        return mNextBlockVerticalOffset;
    }

    /**
     * @return Layout margin on the left-hand side of the block (for optional Output connector).
     */
    @Override
    public int getLayoutMarginLeft() {
        return mLayoutMarginLeft;
    }

    @Override
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        // Note that layout must be done regardless of the value of the "changed" parameter.
        boolean rtl = mHelper.useRtl();
        int rtlSign = rtl ? -1 : +1;

        int xFrom = mLayoutMarginLeft;
        if (rtl) {
            xFrom = mBlockViewSize.x - xFrom;
        }

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

        layoutPatchesAndConnectors();
        updateConnectorLocations();
    }

    /**
     * Select a connection for highlighted drawing.
     *
     * @param connection The connection whose port to highlight. This must be a connection
     * associated with the {@link Block} represented by this {@link BlockView}
     * instance.  Disables all connection highlights if connection is null.
     */
    public void setHighlightedConnection(@Nullable Connection connection) {
        mHighlightedConnection = connection;
        invalidate();
    }

    /**
     * @return The block for this view.
     */
    public Block getBlock() {
        return mBlock;
    }

    /**
     * @return The {@link ColorFilter} that applies the block's color to grayscale resources.
     */
    public ColorFilter getColorFilter() {
        return mBlockColorFilter;
    }

    /**
     * Recursively disconnects the view from the model, and removes all views.
     */
    public void unlinkModelAndSubViews() {
        mFactory.unlinkView(this);
        super.unlinkModelAndSubViews();

        removeAllViews();
    }

    /**
     * Check if border highlight is rendered.
     */
    protected boolean isEntireBlockHighlighted() {
        return isPressed() || isFocused() || isSelected();
    }

    /**
     * @return The number of {@link InputView} instances inside this view.
     */
    @VisibleForTesting
    int getInputViewCount() {
        return mInputViews.size();
    }

    /**
     * @return The {@link InputView} for the {@link Input} at the given index.
     */
    public InputView getInputView(int index) {
        return mInputViews.get(index);
    }

    /**
     * Test whether a {@link MotionEvent} event is (approximately) hitting a visible part of this
     * view.
     * <p/>
     * This is used to determine whether the event should be handled by this view, e.g., to activate
     * dragging or to open a context menu. Since the actual block interactions are implemented at
     * the {@link WorkspaceView} level, there is no need to store the event data in this class.
     *
     * @param event The {@link MotionEvent} to check.
     *
     * @return True if the coordinate of the motion event is on the visible, non-transparent part of
     * this view; false otherwise.
     */
    @Override
    protected boolean hitTest(MotionEvent event) {
        int action = event.getAction();
        if (mHasHit && action == MotionEvent.ACTION_MOVE) {
            // Events that started in this block continue to count as being in this block
            return true;
        }
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            boolean wasHit = mHasHit;
            mHasHit = false;
            return wasHit;
        }
        final int eventX = (int) event.getX();
        final int eventY = (int) event.getY();

        // Do the exact same thing for RTL and LTR, with reversed left and right block bounds. Note
        // that the bounds of each InputView include any connected child blocks, so in RTL mode,
        // the left-hand side of the input fields must be obtained from the right-hand side of the
        // input and the field layout width.
        if (mHelper.useRtl()) {
            // First check whether event is in the general horizontal range of the block outline
            // (minus children) and exit if it is not.
            final int blockEnd = mBlockViewSize.x - mLayoutMarginLeft;
            final int blockBegin = blockEnd - mBlockContentWidth;
            if (eventX < blockBegin || eventX > blockEnd) {
                return false;
            }

            // In the ballpark - now check whether event is on a field of any of this block's
            // inputs. If it is, then the event belongs to this BlockView, otherwise it does not.
            for (int i = 0; i < mInputViews.size(); ++i) {
                final InputView inputView = mInputViews.get(i);
                if (inputView.isOnFields(
                        eventX - (inputView.getRight() - inputView.getFieldLayoutWidth()),
                        eventY - inputView.getTop())) {
                    mHasHit = true;
                    return true;
                }
            }
        } else {
            final int blockBegin = mLayoutMarginLeft;
            final int blockEnd = mBlockContentWidth;
            if (eventX < blockBegin || eventX > blockEnd) {
                return false;
            }

            for (int i = 0; i < mInputViews.size(); ++i) {
                final InputView inputView = mInputViews.get(i);
                if (inputView.isOnFields(
                        eventX - inputView.getLeft(), eventY - inputView.getTop())) {
                    mHasHit = true;
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Instantiates new InputViews for this Block, using the block style from mHelper.
     */
    // TODO(Anm): Move block tree traversal and view creation to factory class.
    private void createInputViews() {
        mInputViews.clear();

        List<Input> inputs = mBlock.getInputs();
        for (int i = 0; i < inputs.size(); ++i) {
            Input in = inputs.get(i);
            InputView inputView = mFactory.buildInputView(in);
            addView(inputView);
            if (in.getType() == Input.TYPE_VALUE) {
                mHasValueInput = true;
            }
            mInputViews.add(inputView);
        }

        mInputCount = mInputViews.size();
        resizeList(mInputConnectorOffsets);
        resizeList(mInputLayoutOrigins);
    }

    /**
     * Draw highlights of block-level connections, or the entire block, if necessary.
     *
     * @param c The canvas to draw on.
     */
    private void drawHighlights(Canvas c) {
        if (isEntireBlockHighlighted()) {
            // Draw entire block highlighted..
            for (int i = 0; i < mBlockBorderPatches.size(); ++i) {
                mBlockBorderPatches.get(i).draw(c);
            }
        } else if (mHighlightedConnection != null) {
            if (mHighlightedConnection == mBlock.getOutputConnection()) {
                assert mOutputConnectorHighlightPatch != null; // Never null with output.
                mOutputConnectorHighlightPatch.draw(c);
            } else if (mHighlightedConnection == mBlock.getPreviousConnection()) {
                assert mPreviousConnectorHighlightPatch != null;  // Never null with previous.
                mPreviousConnectorHighlightPatch.draw(c);
            } else if (mHighlightedConnection == mBlock.getNextConnection()) {
                assert (mNextConnectionHighlightPatch != null);  // Never null with next.
                mNextConnectionHighlightPatch.draw(c);
            } else {
                // If the connection to highlight is not one of the three block-level connectors,
                // then it must be one of the inputs (either a "Next" connector for a Statement or
                // "Input" connector for a Value input). Figure out which input the connection
                // belongs to.
                final Input input = mHighlightedConnection.getInput();
                for (int i = 0; i < mInputViews.size(); ++i) {
                    if (mInputViews.get(i).getInput() == input) {
                        Drawable connectionHighlight = mInputConnectionHighlightPatches.get(i);
                        if (connectionHighlight != null) {
                            connectionHighlight.draw(c);
                        }
                        break;
                    }
                }
            }
        }
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
    private void measureInlineInputs(int widthMeasureSpec, int heightMeasureSpec) {
        int inputViewsSize = mInputViews.size();

        // First pass - measure all fields and inputs; compute maximum width of fields and children
        // over all Statement inputs.
        mMaxStatementFieldsWidth = 0;
        int maxStatementChildWidth = 0;
        for (int i = 0; i < inputViewsSize; i++) {
            InputView inputView = mInputViews.get(i);
            inputView.measureFieldsAndInputs(widthMeasureSpec, heightMeasureSpec);
            if (inputView.getInput().getType() == Input.TYPE_STATEMENT) {
                mMaxStatementFieldsWidth =
                        Math.max(mMaxStatementFieldsWidth, inputView.getTotalFieldWidth());
                maxStatementChildWidth =
                        Math.max(maxStatementChildWidth, inputView.getTotalChildWidth());
            }
        }

        // Second pass - compute layout positions and sizes of all inputs.
        int rowLeft = 0;
        int rowTop = 0;

        int rowHeight = 0;
        int maxRowWidth = 0;

        mInlineRowWidth.clear();
        for (int i = 0; i < inputViewsSize; i++) {
            InputView inputView = mInputViews.get(i);

            // If this is a Statement input, force its field width to be the maximum over all
            // Statements, and begin a new layout row.
            if (inputView.getInput().getType() == Input.TYPE_STATEMENT) {
                // If the first input is a Statement, add vertical space above to draw top of
                // connector just below block top boundary.
                if (i == 0) {
                    rowTop += mPatchManager.mBlockTopPadding;
                }

                // Force all Statement inputs to have the same field width.
                inputView.setFieldLayoutWidth(mMaxStatementFieldsWidth);

                // New row BEFORE each Statement input.
                mInlineRowWidth.add(Math.max(rowLeft,
                        mMaxStatementFieldsWidth + mPatchManager.mStatementInputIndent));

                rowTop += rowHeight;
                rowHeight = 0;
                rowLeft = 0;
            }

            mInputLayoutOrigins.get(i).set(rowLeft, rowTop);

            // Measure input view and update row height and width accordingly.
            inputView.measure(widthMeasureSpec, heightMeasureSpec);
            rowHeight = Math.max(rowHeight, inputView.getMeasuredHeight());

            // Set row height for the current input view as maximum over all views in this row so
            // far. A separate, reverse loop below propagates the maximum to earlier inputs in the
            // same row.
            inputView.setRowHeight(rowHeight);

            if (inputView.getInput().getType() == Input.TYPE_STATEMENT) {
                // The block width is that of the widest row.
                maxRowWidth = Math.max(maxRowWidth, inputView.getMeasuredWidth());

                // If the last input is a Statement, add vertical space below to draw bottom of
                // connector just above block top boundary.
                if (i == mInputCount - 1) {
                    rowTop += mPatchManager.mBlockBottomPadding;
                }

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

        // Add height of final row. This is non-zero with inline inputs if the final input in the
        // block is not a Statement input.
        rowTop += rowHeight;

        // Third pass - propagate row height maximums backwards. Reset height whenever a Statement
        // input is encoutered.
        int maxRowHeight = 0;
        for (int i = inputViewsSize; i > 0; --i) {
            InputView inputView = mInputViews.get(i - 1);
            if (inputView.getInput().getType() == Input.TYPE_STATEMENT) {
                maxRowHeight = 0;
            } else {
                maxRowHeight = Math.max(maxRowHeight, inputView.getRowHeight());
                inputView.setRowHeight(maxRowHeight);
            }
        }

        // If there was at least one Statement input, make sure block is wide enough to fit at least
        // an empty Statement connector. If there were non-empty Statement connectors, they were
        // already taken care of in the loop above.
        if (mMaxStatementFieldsWidth > 0) {
            maxRowWidth = Math.max(maxRowWidth,
                    mMaxStatementFieldsWidth + mPatchManager.mStatementInputIndent);
        }

        // Push width of last input row.
        mInlineRowWidth.add(Math.max(rowLeft,
                mMaxStatementFieldsWidth + mPatchManager.mStatementInputIndent));

        // Block width is the computed width of the widest input row, and at least MIN_BLOCK_WIDTH.
        mBlockContentWidth = Math.max(MIN_BLOCK_WIDTH, maxRowWidth);
        mBlockViewSize.x = mBlockContentWidth;

        // View width is the computed width of the widest statement input, including child blocks
        // and padding, and at least the width of the widest input row.
        mBlockViewSize.x = Math.max(mBlockContentWidth,
                mMaxStatementFieldsWidth + maxStatementChildWidth +
                        mPatchManager.mBlockStartPadding + mPatchManager.mStatementInputPadding);

        // Height is vertical position of next (non-existent) inputs row, and at least MIN_HEIGHT.
        mBlockContentHeight = Math.max(mPatchManager.mMinBlockHeight, rowTop);
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
    private void measureExternalInputs(int widthMeasureSpec, int heightMeasureSpec) {
        int maxInputFieldsWidth = MIN_BLOCK_WIDTH;
        // Initialize max Statement width as zero so presence of Statement inputs can be determined
        // later; apply minimum size after that.
        mMaxStatementFieldsWidth = 0;

        int maxInputChildWidth = 0;
        int maxStatementChildWidth = 0;

        // First pass - measure fields and children of all inputs.
        for (int i = 0; i < mInputViews.size(); i++) {
            InputView inputView = mInputViews.get(i);
            inputView.measureFieldsAndInputs(widthMeasureSpec, heightMeasureSpec);

            switch (inputView.getInput().getType()) {
                case Input.TYPE_VALUE: {
                    maxInputChildWidth =
                            Math.max(maxInputChildWidth, inputView.getTotalChildWidth());
                    // fall through
                }
                default:
                case Input.TYPE_DUMMY: {
                    maxInputFieldsWidth =
                            Math.max(maxInputFieldsWidth, inputView.getTotalFieldWidth());
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
            mMaxStatementFieldsWidth = Math.max(mMaxStatementFieldsWidth, MIN_BLOCK_WIDTH);
            maxInputFieldsWidth = Math.max(maxInputFieldsWidth,
                    mMaxStatementFieldsWidth + mPatchManager.mStatementInputIndent);
        }

        // Second pass - force all inputs to render fields with the same width and compute positions
        // for all inputs.
        int rowTop = 0;
        for (int i = 0; i < mInputViews.size(); i++) {
            InputView inputView = mInputViews.get(i);
            final int inputType = inputView.getInput().getType();
            if (inputType == Input.TYPE_STATEMENT) {
                // If the first input is a Statement, add vertical space above to draw top of
                // connector just below block top boundary.
                if (i == 0) {
                    rowTop += mPatchManager.mBlockTopPadding;
                }

                // Force all Statement inputs to have the same field width.
                inputView.setFieldLayoutWidth(mMaxStatementFieldsWidth);
            } else {
                // Force all Dummy and Value inputs to have the same field width.
                inputView.setFieldLayoutWidth(maxInputFieldsWidth);
            }
            inputView.measure(widthMeasureSpec, heightMeasureSpec);

            mInputLayoutOrigins.get(i).set(0, rowTop);

            // If the last input is a Statement, add vertical space below to draw bottom of
            // connector just above block top boundary.
            if ((inputType == Input.TYPE_STATEMENT) && (i == mInputCount - 1)) {
                rowTop += mPatchManager.mBlockBottomPadding;
            }

            // The block height is the sum of all the row heights.
            rowTop += inputView.getMeasuredHeight();
        }

        // Block content width is the width of the longest row.
        mBlockContentWidth = Math.max(maxInputFieldsWidth, mMaxStatementFieldsWidth);
        mBlockContentHeight = Math.max(mPatchManager.mMinBlockHeight, rowTop);

        // Add space for connector if there is at least one Value input.
        mBlockContentWidth += mPatchManager.mBlockTotalPaddingX;
        if (mHasValueInput) {
            mBlockContentWidth += mPatchManager.mValueInputWidth;
        }

        // Maximum total width of all value inputs is the sum of maximum field and child widths,
        // plus space for field padding and Value input connector, minus overlap of input and output
        // connectors.
        final int maxValueInputTotalWidth = maxInputFieldsWidth + maxInputChildWidth +
                mPatchManager.mBlockTotalPaddingX + mPatchManager.mValueInputWidth -
                mPatchManager.mOutputConnectorWidth;

        // Maximum total width of all Statement inputs is the sum of maximum field and and widths,
        // plus field padding on the left and C-connector padding in the middle.
        final int maxStatementInputTotalWidth = mMaxStatementFieldsWidth + maxStatementChildWidth +
                mPatchManager.mBlockStartPadding +
                mPatchManager.mStatementInputPadding;

        // View width is maximum of content width and the Value input and Statement input total
        // widths.
        mBlockViewSize.x = Math.max(mBlockContentWidth,
                Math.max(maxValueInputTotalWidth, maxStatementInputTotalWidth));
    }

//    /**
//     * Instantiates new InputViews for this Block, using the block style from mHelper.
//     */
//    private void createInputViews() {
//        mInputViews.clear();
//
//        List<Input> inputs = mBlock.getInputs();
//        for (int i = 0; i < inputs.size(); ++i) {
//            Input in = inputs.get(i);
//            InputView inputView = new InputView(getContext(), mFactory.getBlockStyle(), in, mHelper);
//            addView(inputView);
//            if (in.getType() == Input.TYPE_VALUE) {
//                mHasValueInput = true;
//            }
//            mInputViews.add(inputView);
//        }
//
//        mInputCount = mInputViews.size();
//        resizeList(mInputConnectorOffsets);
//        resizeList(mInputLayoutOrigins);
//    }

    private void initDrawingObjects() {
        final int blockColor = mBlock.getColour();

        // Highlight color channels are added to each color-multiplied color channel, and since the
        // patches are 50% gray, the addition should be 50% of the base value.
        final int highlight = Color.argb(255, Color.red(blockColor) / 2,
                Color.green(blockColor) / 2, Color.blue(blockColor) / 2);
        mBlockColorFilter = new LightingColorFilter(blockColor, highlight);

        mFillPaint.setColor(mBlock.getColour());
        mFillPaint.setStyle(Paint.Style.FILL);
    }

    /**
     * Adjust size of an {@link ArrayList} of {@link ViewPoint} objects to match the size of
     * {@link #mInputViews}.
     */
    private void resizeList(ArrayList<ViewPoint> list) {
        if (list.size() != mInputCount) {
            list.ensureCapacity(mInputCount);
            if (list.size() < mInputCount) {
                for (int i = list.size(); i < mInputCount; i++) {
                    list.add(new ViewPoint());
                }
            } else {
                while (list.size() > mInputCount) {
                    list.remove(list.size() - 1);
                }
            }
        }
    }

    /**
     * Position patches for block rendering and connectors.
     */
    private void layoutPatchesAndConnectors() {
        mBlockPatches.clear();
        mBlockBorderPatches.clear();
        mFillRects.clear();

        // Leave room on the left for margin (accomodates optional output connector) and block
        // padding (accomodates block boundary).
        int xFrom = mLayoutMarginLeft + mPatchManager.mBlockStartPadding;

        // For inline inputs, the upper horizontal coordinate of the block boundary varies by
        // section and changes after each Statement input. For external inputs, it is constant as
        // computed in measureExternalInputs.
        int xTo = mLayoutMarginLeft;
        int inlineRowIdx = 0;
        if (mBlock.getInputsInline()) {
            xTo += mInlineRowWidth.get(inlineRowIdx);
        } else {
            xTo += mBlockContentWidth;
        }

        // Position top-left corner drawable. Retain drawable object so we can position bottom-left
        // drawable correctly.
        int yTop = 0;
        final NinePatchDrawable topStartDrawable = addTopLeftPatch(xTo, yTop);

        // Position inputs and connectors.
        mInputConnectionHighlightPatches.clear();
        mInputConnectionHighlightPatches.ensureCapacity(mInputCount);
        for (int i = 0; i < mInputCount; ++i) {
            final InputView inputView = mInputViews.get(i);
            final ViewPoint inputLayoutOrigin = mInputLayoutOrigins.get(i);

            // Placeholder for connection patch.  Even if input does not have a connection, makes
            // sure later patches use indices matching their inputs.
            mInputConnectionHighlightPatches.add(null);

            // Start filling background of the input based on its origin and measured size.
            fillRectBySize(xFrom + inputLayoutOrigin.x, inputLayoutOrigin.y,
                    inputView.getFieldLayoutWidth(), inputView.getRowHeight());

            switch (inputView.getInput().getType()) {
                default:
                case Input.TYPE_DUMMY: {
                    boolean isLastInput = (i + 1 == mInputCount);
                    boolean nextIsStatement = !isLastInput
                            && mInputViews.get(i+1).getInput().getType() == Input.TYPE_STATEMENT;
                    boolean isEndOfLine = !mBlock.getInputsInline() || isLastInput
                            || nextIsStatement;
                    if (isEndOfLine) {
                        addDummyBoundaryPatch(xTo, inputView, inputLayoutOrigin);
                    }
                    break;
                }
                case Input.TYPE_VALUE: {
                    if (mBlock.getInputsInline()) {
                        addInlineValueInputPatch(
                                i, inlineRowIdx, xFrom, inputView, inputLayoutOrigin);

                    } else {
                        addExternalValueInputPatch(i, xTo, inputView, inputLayoutOrigin);
                    }
                    break;
                }
                case Input.TYPE_STATEMENT: {
                    // For external inputs, the horizontal end coordinate of the connector bottom is
                    // the same as the one on top. For inline inputs, however, it is the next entry
                    // in the width-by-row table.
                    int xToBottom = xTo;
                    if (mBlock.getInputsInline()) {
                        ++inlineRowIdx;
                        xToBottom = xFrom + mInlineRowWidth.get(inlineRowIdx) -
                                mPatchManager.mBlockStartPadding;
                    }

                    // Place the connector patches.
                    addStatementInputPatches(
                            i, xFrom, xTo, xToBottom, inputView, inputLayoutOrigin);

                    // Set new horizontal end coordinate for subsequent inputs.
                    xTo = xToBottom;
                    break;
                }
            }
        }

        // Select and position correct patch for bottom and left-hand side of the block, including
        // bottom-left corner.
        int bottomStartResourceId = R.drawable.bottom_start_default;
        int bottomStartBorderResourceId = R.drawable.bottom_start_default_border;
        if (mBlock.getNextConnection() != null) {
            setPointMaybeFlip(mNextConnectorOffset, mLayoutMarginLeft, mNextBlockVerticalOffset);
            bottomStartResourceId = R.drawable.bottom_start_next;
            bottomStartBorderResourceId = R.drawable.bottom_start_next_border;
        }
        final NinePatchDrawable bottomStartDrawable =
                getColoredPatchDrawable(bottomStartResourceId);
        final NinePatchDrawable bottomStartBorderDrawable =
                mPatchManager.getPatchDrawable(bottomStartBorderResourceId);

        calculateRtlAwareBounds(tempRect,
                mLayoutMarginLeft, topStartDrawable.getIntrinsicHeight(), xTo, mBlockViewSize.y);
        bottomStartDrawable.setBounds(tempRect);
        bottomStartBorderDrawable.setBounds(tempRect);

        if (mBlock.getNextConnection() != null) {
            mNextConnectionHighlightPatch =
                    mPatchManager.getPatchDrawable(R.drawable.bottom_start_next_connection);
            mNextConnectionHighlightPatch.setBounds(tempRect);
        }

        mBlockPatches.add(bottomStartDrawable);
        mBlockBorderPatches.add(bottomStartBorderDrawable);

        // Finish the final rect, if there is one.
        finishFillRect();
    }

    /**
     * Add the top-left corner drawable.
     *
     * @param xTo Horizontal end position for the drawable.
     * @param yTop Vertical position for the drawable.
     *
     * @return The added drawable. This can be used to position other drawables, e.g., the
     * bottom-left drawable, relative to it.
     */
    @NonNull
    private NinePatchDrawable addTopLeftPatch(int xTo, int yTop) {
        // Select and position the correct patch for the top and left block sides including the
        // top-left corner.
        NinePatchDrawable topStartDrawable;
        NinePatchDrawable topStartBorderDrawable;
        if (mBlock.getPreviousConnection() != null) {
            setPointMaybeFlip(mPreviousConnectorOffset, mLayoutMarginLeft, yTop);
            topStartDrawable = getColoredPatchDrawable(R.drawable.top_start_previous);
            topStartBorderDrawable =
                    mPatchManager.getPatchDrawable(R.drawable.top_start_previous_border);
            mPreviousConnectorHighlightPatch =
                    mPatchManager.getPatchDrawable(R.drawable.top_start_previous_connection);
        } else if (mBlock.getOutputConnection() != null) {
            setPointMaybeFlip(mOutputConnectorOffset, mLayoutMarginLeft, yTop);
            topStartDrawable = getColoredPatchDrawable(R.drawable.top_start_output);
            topStartBorderDrawable =
                    mPatchManager.getPatchDrawable(R.drawable.top_start_output_border);
            mOutputConnectorHighlightPatch =
                    mPatchManager.getPatchDrawable(R.drawable.top_start_output_connection);
        } else {
            topStartDrawable = getColoredPatchDrawable(R.drawable.top_start_default);
            topStartBorderDrawable =
                    mPatchManager.getPatchDrawable(R.drawable.top_start_default_border);
        }
        calculateRtlAwareBounds(tempRect, 0, 0, xTo, topStartDrawable.getIntrinsicHeight());
        topStartDrawable.setBounds(tempRect);
        topStartBorderDrawable.setBounds(tempRect);
        if (mPreviousConnectorHighlightPatch != null) {
            mPreviousConnectorHighlightPatch.setBounds(tempRect);
        }
        if (mOutputConnectorHighlightPatch != null) {
            mOutputConnectorHighlightPatch.setBounds(tempRect);
        }

        mBlockPatches.add(topStartDrawable);
        mBlockBorderPatches.add(topStartBorderDrawable);
        return topStartDrawable;
    }

    /**
     * Add boundary patch for external Dummy input.
     *
     * @param xTo Horizontal coordinate to which the patch should extend. The starting coordinate
     * is determined from this by subtracting patch width.
     * @param inputView The {@link InputView} for the current input. This is used to determine patch
     * height.
     * @param inputLayoutOrigin The layout origin for the current input. This is used to determine
     * the vertical position for the patch.
     */
    private void addDummyBoundaryPatch(int xTo, InputView inputView, ViewPoint inputLayoutOrigin) {
        // For external dummy inputs, put a patch for the block boundary.
        final NinePatchDrawable inputDrawable =
                getColoredPatchDrawable(R.drawable.dummy_input);
        final NinePatchDrawable inputBorderDrawable =
                mPatchManager.getPatchDrawable(R.drawable.dummy_input_border);
        int width = inputDrawable.getIntrinsicWidth();
        if (mHasValueInput) {
            // Stretch the patch horizontally if this block has at least one value
            // input, so that the dummy input block boundary is as thick as the
            // boundary with value input connector.
            width += mPatchManager.mValueInputWidth;
        }

        boolean inTopRow = (inputLayoutOrigin.y == 0);
        calculateRtlAwareBounds(tempRect,
                /* ltrStart */ xTo - width,
                /* top */ inputLayoutOrigin.y + (inTopRow ? mPatchManager.mBlockTopPadding : 0),
                /* ltrEnd */ xTo,
                /* bottom */ inputLayoutOrigin.y + inputView.getRowHeight());
        inputDrawable.setBounds(tempRect);
        inputBorderDrawable.setBounds(tempRect);
        mBlockPatches.add(inputDrawable);
        mBlockBorderPatches.add(inputBorderDrawable);
    }

    /**
     * Add connector patch for external Value inputs.
     *
     * @param i The index of the current input. This is used to determine whether to position patch
     * vertically below the field top boundary to account for the block's top boundary.
     * @param xTo Horizontal coordinate to which the patch should extend. The starting coordinate
     * is determined from this by subtracting patch width.
     * @param inputView The {@link InputView} for the current input. This is used to determine patch
     * height.
     * @param inputLayoutOrigin The layout origin for the current input. This is used to determine
     * the vertical position for the patch.
     */
    private void addExternalValueInputPatch(int i, int xTo,
                                            InputView inputView, ViewPoint inputLayoutOrigin) {
        // Position patch and connector for external value input.
        setPointMaybeFlip(mInputConnectorOffsets.get(i), xTo, inputLayoutOrigin.y);

        final NinePatchDrawable inputDrawable =
                getColoredPatchDrawable(R.drawable.value_input_external);
        final NinePatchDrawable inputBorderDrawable =
                mPatchManager.getPatchDrawable(R.drawable.value_input_external_border);
        final NinePatchDrawable connectionHighlightDrawable =
                mPatchManager.getPatchDrawable(R.drawable.value_input_external_connection);

        int patchLeft = xTo - inputDrawable.getIntrinsicWidth();
        int patchRight = xTo;
        int connectorTop = inputLayoutOrigin.y + mPatchManager.mBlockTopPadding;
        int connectorBottom = inputLayoutOrigin.y + inputView.getMeasuredHeight();

        calculateRtlAwareBounds(tempRect, patchLeft, connectorTop, patchRight, connectorBottom);
        inputDrawable.setBounds(tempRect);
        inputBorderDrawable.setBounds(tempRect);
        connectionHighlightDrawable.setBounds(tempRect);

        mBlockPatches.add(inputDrawable);
        mBlockBorderPatches.add(inputBorderDrawable);
        mInputConnectionHighlightPatches.set(i, connectionHighlightDrawable);

        if (i > 0) {
            // If this is not the first input in the block, then a gap above the
            // input connector patch must be closed by a non-input boundary patch.
            // The gap is the result of the need to not draw over the top block
            // boundary.
            final NinePatchDrawable boundaryGapDrawable =
                    getColoredPatchDrawable(R.drawable.dummy_input);
            final NinePatchDrawable boundaryGapBorderDrawable =
                    mPatchManager.getPatchDrawable(R.drawable.dummy_input_border);
            calculateRtlAwareBounds(tempRect,
                    patchLeft, inputLayoutOrigin.y, patchRight, connectorTop);
            boundaryGapDrawable.setBounds(tempRect);
            boundaryGapBorderDrawable.setBounds(tempRect);
            mBlockPatches.add(boundaryGapDrawable);
            mBlockBorderPatches.add(boundaryGapBorderDrawable);
        }
    }

    /**
     * Add cutout patch for inline Value inputs.
     * <p/>
     * The inline input nine patch includes the entirety of the cutout shape, including the
     * connector, and stretches to fit all child blocks.
     * <p/>
     * An inline input is usually drawn with an input 9-patch and three rects for padding between
     * inputs: one above, one below, and one after (i.e., to  the right in LTR and to the left in
     * RTL).
     *
     * @param i The index of the input in the block.
     * @param inlineRowIdx The (horizontal) index of the input in the current input row.
     * @param blockFromX The horizontal start position input views in the block.
     * @param inputView The input view.
     * @param inputLayoutOrigin Layout origin for the current input view.
     */
    private void addInlineValueInputPatch(int i, int inlineRowIdx, int blockFromX,
                                          InputView inputView, ViewPoint inputLayoutOrigin) {
        // Determine position for inline connector cutout.
        final int cutoutX = blockFromX + inputLayoutOrigin.x + inputView.getInlineInputX();
        final int cutoutY = inputLayoutOrigin.y + mPatchManager.mBlockTopPadding;

        // Set connector position - shift w.r.t. patch location to where the corner of connected
        // blocks will be positioned.
        setPointMaybeFlip(mInputConnectorOffsets.get(i),
                cutoutX + mPatchManager.mInlineInputStartPadding +
                        mPatchManager.mOutputConnectorWidth,
                cutoutY + mPatchManager.mInlineInputTopPadding);

        // Fill above inline input connector, unless first row, where connector top
        // is aligned with block boundary patch.
        if (inlineRowIdx > 0) {
            fillRectBySize(cutoutX, inputLayoutOrigin.y,
                    inputView.getTotalChildWidth(), mPatchManager.mBlockTopPadding);
            finishFillRect();  // Prevent filling through the inline connector.
        }

        // Position a properly-sized input cutout patch.
        final NinePatchDrawable inputDrawable =
                getColoredPatchDrawable(R.drawable.value_input_inline);
        final NinePatchDrawable connectionHighlightDrawable =
                mPatchManager.getPatchDrawable(R.drawable.value_input_inline_connection);
        calculateRtlAwareBounds(tempRect,
                cutoutX, cutoutY,
                cutoutX + inputView.getTotalChildWidth(),
                cutoutY + inputView.getTotalChildHeight());
        inputDrawable.setBounds(tempRect);
        connectionHighlightDrawable.setBounds(tempRect);
        mBlockPatches.add(inputDrawable);
        mInputConnectionHighlightPatches.set(i, connectionHighlightDrawable);

        // Fill below inline input cutout.
        final int cutoutEndX = cutoutX + inputView.getTotalChildWidth();
        final int cutoutEndY = inputLayoutOrigin.y + inputView.getRowHeight();
        fillRect(cutoutX, cutoutY + inputView.getTotalChildHeight(), cutoutEndX, cutoutEndY);

        // Fill after inline input cutout.
        fillRect(cutoutEndX, inputLayoutOrigin.y,
                inputLayoutOrigin.x + inputView.getMeasuredWidth(), cutoutEndY);

        // If this is either the last input in the block, or the next input is a Statement, then
        // this is the final input in the current row. In this case, put a boundary patch.
        final int nextI = i + 1;
        if ((nextI == mInputCount) ||
                (mInputViews.get(nextI).getInput().getType() == Input.TYPE_STATEMENT)) {
            // Horizontal patch position is the position of inputs in the block, plus offset of the
            // current input in its row, plus padding before and after the input fields.
            final int patchX = blockFromX + mInlineRowWidth.get(inlineRowIdx) -
                    mPatchManager.mBlockTotalPaddingX;

            // Vertical patch position is the input layout origin, plus room for block boundary if
            // this is the first input row.
            final int patchY = inputLayoutOrigin.y +
                    (inlineRowIdx > 0 ? 0 : mPatchManager.mBlockTopPadding);
            final int patchRight = patchX + mPatchManager.mBlockEndPadding;

            final NinePatchDrawable blockFillDrawable =
                    getColoredPatchDrawable(R.drawable.dummy_input);
            final NinePatchDrawable blockFillBorderDrawable =
                    mPatchManager.getPatchDrawable(R.drawable.dummy_input_border);

            calculateRtlAwareBounds(tempRect, patchX, patchY, patchRight, cutoutEndY);
            blockFillDrawable.setBounds(tempRect);
            blockFillBorderDrawable.setBounds(tempRect);

            mBlockPatches.add(blockFillDrawable);
            mBlockBorderPatches.add(blockFillBorderDrawable);

            // Also at the end of the current input row, fill background up to
            // block boundary.
            fillRect(inputLayoutOrigin.x + mPatchManager.mBlockStartPadding +
                    inputView.getMeasuredWidth(), patchY, patchX, cutoutEndY);
        }
    }

    /**
     * Add patches (top and bottom) for a Statement input connector.
     *
     * @param i Index of the input.
     * @param xFrom Horizontal offset of block content.
     * @param xToAbove Horizontal end coordinate for connector above input.
     * @param xToBelow Horizontal end coordinate for connector below input.
     * @param inputView The view for this input.
     * @param inputLayoutOrigin Layout origin for this input.
     */
    private void addStatementInputPatches(int i, int xFrom, int xToAbove, int xToBelow,
                                          InputView inputView, ViewPoint inputLayoutOrigin) {
        // Position connector. Shift by horizontal and vertical patch thickness to line up with
        // "Previous" connector on child block.
        int xOffset = xFrom + inputView.getFieldLayoutWidth();
        setPointMaybeFlip(mInputConnectorOffsets.get(i),
                xOffset + mPatchManager.mStatementInputPadding,
                inputLayoutOrigin.y + mPatchManager.mStatementTopThickness);

        // Position patch for the top part of the Statement connector. This patch is
        // stretched only horizontally to extend to the block boundary.
        final NinePatchDrawable statementTopDrawable =
                getColoredPatchDrawable(R.drawable.statementinput_top);
        final NinePatchDrawable statementTopBorderDrawable =
                mPatchManager.getPatchDrawable(R.drawable.statementinput_top_border);
        final NinePatchDrawable statementConnectionHighlight =
                mPatchManager.getPatchDrawable(R.drawable.statementinput_top_connection);

        calculateRtlAwareBounds(tempRect,
                /* ltrStart */ xOffset,
                /* top */ inputLayoutOrigin.y,
                /* ltrEnd */ xToAbove,
                /* bottom */ inputLayoutOrigin.y + statementTopDrawable.getIntrinsicHeight());
        statementTopDrawable.setBounds(tempRect);
        statementTopBorderDrawable.setBounds(tempRect);
        statementConnectionHighlight.setBounds(tempRect);

        mBlockPatches.add(statementTopDrawable);
        mBlockBorderPatches.add(statementTopBorderDrawable);
        mInputConnectionHighlightPatches.set(i, statementConnectionHighlight);

        // Position patch for the bottom part of the Statement connector. The bottom
        // patch is stretched horizontally, like the top patch, but also vertically to
        // accomodate height of the input fields as well as the size of any connected
        // blocks.
        final NinePatchDrawable statementBottomDrawable =
                getColoredPatchDrawable(R.drawable.statementinput_bottom);
        final NinePatchDrawable statementBottomBorderDrawable =
                mPatchManager.getPatchDrawable(R.drawable.statementinput_bottom_border);

        final int connectorHeight =
                Math.max(inputView.getTotalChildHeight(),
                        inputView.getMeasuredHeight());

        calculateRtlAwareBounds(tempRect,
                /* ltrStart */ xOffset,
                /* top */ inputLayoutOrigin.y + statementTopDrawable.getIntrinsicHeight(),
                /* ltrEnd */ xToBelow,
                /* bottom */ inputLayoutOrigin.y + connectorHeight);
        statementBottomDrawable.setBounds(tempRect);
        statementBottomBorderDrawable.setBounds(tempRect);

        mBlockPatches.add(statementBottomDrawable);
        mBlockBorderPatches.add(statementBottomBorderDrawable);
    }

    /**
     * Draw dots at the model's location of all connections on this block, for debugging.
     *
     * @param c The canvas to draw on.
     */
    private void drawConnectorCenters(Canvas c) {
        List<Connection> connections = mBlock.getAllConnections();
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < connections.size(); i++) {
            Connection conn = connections.get(i);
            if (conn.inDragMode()) {
                if (conn.isConnected()) {
                    paint.setColor(Color.RED);
                } else {
                    paint.setColor(Color.MAGENTA);
                }
            } else {
                if (conn.isConnected()) {
                    paint.setColor(Color.GREEN);
                } else {
                    paint.setColor(Color.CYAN);
                }
            }

            // Compute connector position relative to this view from its offset to block origin in
            // Workspace coordinates.
            mTempWorkspacePoint.set(
                    conn.getPosition().x - mBlock.getPosition().x,
                    conn.getPosition().y - mBlock.getPosition().y);
            mHelper.workspaceToVirtualViewDelta(mTempWorkspacePoint, mTempConnectionPosition);
            if (mHelper.useRtl()) {
                // In RTL mode, add block view size to x coordinate. This is counter-intuitive, but
                // equivalent to "x = size - (-x)", with the inner negation "-x" undoing the
                // side-effect of workspaceToVirtualViewDelta reversing the x coordinate. This is,
                // the addition mirrors the re-negated in-block x coordinate w.r.t. the right-hand
                // side of the block view, which is the origin of the block in RTL mode.
                mTempConnectionPosition.x += mBlockViewSize.x;
            }
            c.drawCircle(mTempConnectionPosition.x, mTempConnectionPosition.y, 10, paint);
        }
    }

    /**
     * Add a rectangular area for filling either by creating a new rectangle or extending an
     * existing one.
     * <p/>
     * If the rectangle defined by the method arguments is aligned with the in-progress rectangle
     * {@link #mNextFillRect} either horizontally or vertically, then the two are joined. Otherwise,
     * {@link #mNextFillRect} is finished and committed to the list of rectangles to draw and a new
     * rectangle is begun with the given method arguments.
     * <p/>
     * Note that rectangles are joined even if there is a gap between them. This fills padding areas
     * between inline inputs in the same row without any additional code. However, this assumes that
     * whenever there is an intended gap between aligned rectangles, then there is at last one
     * rectangle of different size (or with unaligned position) between them. If this assumption is
     * violated, call {@link #finishFillRect()} prior to the next call to this method.
     *
     * @param left Left coordinate of the new rectangle in LTR mode. In RTL mode, coordinates are
     * automatically flipped when the rectangle is committed by calling {@link #finishFillRect()}.
     * @param top Top coordinate of the new rectangle.
     * @param right Right coordinate of the new rectangle in LTR mode. In RTL mode, coordinates are
     * automatically flipped when the rectangle is committed by calling {@link #finishFillRect()}.
     * @param bottom Bottom coordinate of the new rectangle.
     */
    private void fillRect(int left, int top, int right, int bottom) {
        if (mNextFillRect != null) {
            if ((mNextFillRect.left == left) && (mNextFillRect.right == right)) {
                assert mNextFillRect.top <= top;  // New rectangle must not start above current.
                mNextFillRect.bottom = bottom;
                return;
            } else if ((mNextFillRect.top == top) && (mNextFillRect.bottom == bottom)) {
                assert mNextFillRect.left <= left;  // New rectangle must not start left of current.
                mNextFillRect.right = right;
                return;
            } else {
                finishFillRect();
            }
        }

        mNextFillRect = new Rect(left, top, right, bottom);
    }

    /**
     * Convenience wrapper for {@link #fillRect(int, int, int, int)} taking rectangle size
     * rather than upper bounds.
     * <p/>
     * This wrapper converts width and height of the given rectangle to right and bottom
     * coordinates, respectively. That makes client code more readable in places where the rectangle
     * is naturally defined by its origin and size.
     *
     * @param left Left coordinate of the new rectangle in LTR mode. In RTL mode, coordinates are
     * automatically flipped when the rectangle is committed by calling {@link #finishFillRect()}.
     * @param top Top coordinate of the new rectangle.
     * @param width Width of the new rectangle.
     * @param height Height of the new rectangle.
     */
    private void fillRectBySize(int left, int top, int width, int height) {
        fillRect(left, top, left + width, top + height);
    }

    /**
     * Finish the current fill rectangle.
     * <p/>
     * The current rectangle is cropped against the vertical block boundaries with padding
     * considered, and afterwards committed to the {@link #mFillRects} list.
     * <p/>
     * Note that horizontal block padding is assumed to have been considered prior to calling
     * {@link #fillRect(int, int, int, int)}. This is because horizontal block size can
     * vary across rows in a block with inline inputs.
     * <p/>
     * In Right-to-Left mode, the horizontal rectangle boundaries are mirrored w.r.t. the right-hand
     * side of the view.
     */
    private void finishFillRect() {
        if (mNextFillRect != null) {
            mNextFillRect.top = Math.max(mNextFillRect.top, mPatchManager.mBlockTopPadding);
            mNextFillRect.bottom = Math.min(mNextFillRect.bottom,
                    mBlockContentHeight - mPatchManager.mBlockBottomPadding);

            // In RTL mode, mirror Rect w.r.t. right-hand side of the block area.
            if (mHelper.useRtl()) {
                final int left = mNextFillRect.left;
                mNextFillRect.left = mBlockViewSize.x - mNextFillRect.right;
                mNextFillRect.right = mBlockViewSize.x - left;
            }

            mFillRects.add(mNextFillRect);
            mNextFillRect = null;
        }
    }

    private NinePatchDrawable getColoredPatchDrawable(int id) {
        NinePatchDrawable drawable = mPatchManager.getPatchDrawable(id);
        drawable.setColorFilter(mBlockColorFilter);
        return drawable;
    }
}
