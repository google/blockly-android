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
import android.view.MotionEvent;

import com.google.blockly.android.control.ConnectionManager;
import com.google.blockly.android.ui.Dragger;
import com.google.blockly.android.ui.AbstractBlockView;
import com.google.blockly.android.ui.BlockTouchHandler;
import com.google.blockly.android.ui.ViewPoint;
import com.google.blockly.android.ui.WorkspaceHelper;
import com.google.blockly.android.ui.WorkspaceView;
import com.google.blockly.model.Block;
import com.google.blockly.model.Input;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws a block and handles laying out all its inputs/fields.
 */
@SuppressLint("ViewConstructor")
public class BlockView extends AbstractBlockView<InputView> {
    private static final boolean DEBUG = false;

    private static final float SHADOW_SATURATION_MULTIPLIER = 0.4f;
    private static final float SHADOW_VALUE_MULTIPLIER = 1.2f;

    // TODO(#86): Determine from 9-patch measurements.
    private final int mMinBlockWidth;

    // Width  and height of the block "content", i.e., all its input fields. Unlike the view size,
    // this does not include extruding connectors (e.g., Output, Next) and connected input blocks.
    private int mBlockContentWidth;
    private int mBlockContentHeight;
    // Offset of the block origin inside the view's measured area.
    private int mOutputConnectorMargin;
    private int mMaxStatementFieldsWidth;
    // Vertical offset for positioning the "Next" block (if one exists).
    private int mNextBlockVerticalOffset;
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
    private final boolean mUseHat;
    private int mBlockTopPadding;

    // Keeps track of if the current set of touch events had started on this block
    private boolean mHasHit = false;

    private final Rect tempRect = new Rect(); // Only use in main thread functions.

    /**
     * Create a new BlockView and associated InputViews for the given block using the
     * WorkspaceHelper's provided style.
     * <p>
     * App developers should not call this constructor directly.  Instead use
     * {@link VerticalBlockViewFactory#buildBlockViewTree}.
     *
     * @param context The context for creating this view.
     * @param helper The {@link WorkspaceHelper} that manages the block sizes on in this Activity.
     * @param factory The {@link VerticalBlockViewFactory} that is building this view.
     * @param block The {@link Block} represented by this view.
     * @param inputViews The {@link InputView} contained in this view.
     * @param connectionManager The {@link ConnectionManager} to update when moving connections.
     * @param touchHandler The optional handler for forwarding touch events on this block to the
     *                     {@link Dragger}.
     */
    protected BlockView(Context context, WorkspaceHelper helper, VerticalBlockViewFactory factory,
                        Block block, List<InputView> inputViews,
                        ConnectionManager connectionManager,
                        @Nullable BlockTouchHandler touchHandler) {

        super(context, helper, factory, block, inputViews, connectionManager, touchHandler);

        mTouchHandler = touchHandler;
        mPatchManager = factory.getPatchManager();  // Shortcut.
        mMinBlockWidth = (int) context.getResources().getDimension(R.dimen.min_block_width);
        mUseHat = factory.isBlockHatsEnabled();

        setClickable(true);
        setFocusable(true);
        setWillNotDraw(false);

        initDrawingObjects();
    }

    @Override
    protected void onDraw(Canvas c) {
        for (int i = 0; i < mFillRects.size(); ++i) {
            c.drawRect(mFillRects.get(i), mFillPaint);
        }

        for (int i = 0; i < mBlockPatches.size(); ++i) {
            mBlockPatches.get(i).draw(c);
        }
    }

    /**
     * Draw highlights and connector locators (if any) over above all child views.
     * @param canvas
     */
    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);  // Normal draw call.

        if (DEBUG) {
            drawConnectorCenters(canvas);  // Enable to debug connection positions.
        }
        drawHighlights(canvas);
    }

    /**
     * Measure all children (i.e., block inputs) and compute their sizes and relative positions
     * for use in {@link #onLayout}.
     */
    // TODO(#144): Move to AbstractBlockView, using abstract methods for calls. After #133
    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mBlockTopPadding = mPatchManager.computeBlockTopPadding(mBlock);

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
            mOutputConnectorMargin = mPatchManager.mOutputConnectorWidth;
            mBlockViewSize.x += mOutputConnectorMargin;
        } else {
            mOutputConnectorMargin = 0;
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
    public int getOutputConnectorMargin() {
        return mOutputConnectorMargin;
    }

    @Override
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        // Note that layout must be done regardless of the value of the "changed" parameter.
        boolean rtl = mHelper.useRtl();
        int rtlSign = rtl ? -1 : +1;

        int xFrom = mOutputConnectorMargin;
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
     * @return The {@link ColorFilter} that applies the block's color to grayscale resources.
     */
    public ColorFilter getColorFilter() {
        return mBlockColorFilter;
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
    // TODO(#143): Move some of this to AbstractBlockView (state, subviews, etc.)
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
            final int blockEnd = mBlockViewSize.x - mOutputConnectorMargin;
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
            final int blockBegin = mOutputConnectorMargin;
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
                    rowTop += mBlockTopPadding;
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

        // Block width is the computed width of the widest input row, and at least mMinBlockWidth.
        mBlockContentWidth = Math.max(mMinBlockWidth, maxRowWidth);
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
        int maxInputFieldsWidth = mMinBlockWidth;
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
            mMaxStatementFieldsWidth = Math.max(mMaxStatementFieldsWidth, mMinBlockWidth);
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
                    rowTop += mBlockTopPadding;
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

    private void initDrawingObjects() {
        int blockColour = mBlock.getColor();
        if (mBlock.isShadow()) {
            float hsv[] = new float[3];
            Color.colorToHSV(blockColour, hsv);
            hsv[1] *= SHADOW_SATURATION_MULTIPLIER;
            hsv[2] *= SHADOW_VALUE_MULTIPLIER;
            if (hsv[2] > 1) {
                hsv[2] = 1;
            }
            blockColour = Color.HSVToColor(hsv);
        }

        // Highlight color channels are added to each color-multiplied color channel, and since the
        // patches are 50% gray, the addition should be 50% of the base value.
        final int highlight = Color.argb(255, Color.red(blockColour) / 2,
                Color.green(blockColour) / 2, Color.blue(blockColour) / 2);
        mBlockColorFilter = new LightingColorFilter(blockColour, highlight);

        mFillPaint.setColor(blockColour);
        mFillPaint.setStyle(Paint.Style.FILL);
    }

    /**
     * Position patches for block rendering and connectors.
     */
    private void layoutPatchesAndConnectors() {
        mBlockPatches.clear();
        mBlockBorderPatches.clear();
        mFillRects.clear();

        boolean isShadow = mBlock.isShadow();

        // Leave room on the left for margin (accomodates optional output connector) and block
        // padding (accomodates block boundary).
        int xFrom = mOutputConnectorMargin + mPatchManager.mBlockStartPadding;

        // For inline inputs, the upper horizontal coordinate of the block boundary varies by
        // section and changes after each Statement input. For external inputs, it is constant as
        // computed in measureExternalInputs.
        int xTo = mOutputConnectorMargin;
        int inlineRowIdx = 0;
        if (mBlock.getInputsInline()) {
            xTo += mInlineRowWidth.get(inlineRowIdx);
        } else {
            xTo += mBlockContentWidth;
        }

        // Position top-left corner drawable. Retain drawable object so we can position bottom-left
        // drawable correctly.
        int yTop = 0;
        final NinePatchDrawable topStartDrawable = addTopLeftPatch(isShadow, xTo, yTop);

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
                            && mInputViews.get(i + 1).getInput().getType() == Input.TYPE_STATEMENT;
                    boolean isEndOfLine = !mBlock.getInputsInline() || isLastInput
                            || nextIsStatement;
                    if (isEndOfLine) {
                        addDummyBoundaryPatch(isShadow, xTo, inputView, inputLayoutOrigin);
                    }
                    break;
                }
                case Input.TYPE_VALUE: {
                    if (mBlock.getInputsInline()) {
                        addInlineValueInputPatch(
                                isShadow, i, inlineRowIdx, xFrom, inputView, inputLayoutOrigin);

                    } else {
                        addExternalValueInputPatch(isShadow, i, xTo, inputView, inputLayoutOrigin);
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
                            isShadow, i, xFrom, xTo, xToBottom, inputView, inputLayoutOrigin);

                    // Set new horizontal end coordinate for subsequent inputs.
                    xTo = xToBottom;
                    break;
                }
            }
        }

        // Select and position correct patch for bottom and left-hand side of the block, including
        // bottom-left corner.
        int bottomStartResourceId = isShadow ? R.drawable.bottom_start_default_shadow
                : R.drawable.bottom_start_default;
        int bottomStartBorderResourceId = R.drawable.bottom_start_default_border;
        if (mBlock.getNextConnection() != null) {
            mHelper.setPointMaybeFlip(
                    mNextConnectorOffset, mOutputConnectorMargin, mNextBlockVerticalOffset);
            bottomStartResourceId = isShadow ? R.drawable.bottom_start_next_shadow
                    : R.drawable.bottom_start_next;
            bottomStartBorderResourceId = R.drawable.bottom_start_next_border;
        } else if (mBlock.getOutputConnection() != null) {
            bottomStartResourceId = isShadow ? R.drawable.bottom_start_default_square_shadow
                    : R.drawable.bottom_start_default_square;
            bottomStartBorderResourceId = R.drawable.bottom_start_square_border;
        }
        final NinePatchDrawable bottomStartDrawable =
                getColoredPatchDrawable(bottomStartResourceId);
        final NinePatchDrawable bottomStartBorderDrawable =
                mPatchManager.getPatchDrawable(bottomStartBorderResourceId);

        mHelper.setRtlAwareBounds(tempRect,
                /* this width */ mBlockViewSize.x,
                /* LTR start */ mOutputConnectorMargin,
                /* top */ topStartDrawable.getIntrinsicHeight(),
                /* LTR end */ xTo,
                /* bottom */ mBlockViewSize.y);
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
    private NinePatchDrawable addTopLeftPatch(boolean isShadow, int xTo, int yTop) {
        // Select and position the correct patch for the top and left block sides including the
        // top-left corner.
        NinePatchDrawable topStartDrawable;
        NinePatchDrawable topStartBorderDrawable;
        if (mBlock.getPreviousConnection() != null) {
            mHelper.setPointMaybeFlip(mPreviousConnectorOffset, mOutputConnectorMargin, yTop);
            topStartDrawable = getColoredPatchDrawable(isShadow
                    ? R.drawable.top_start_previous_shadow : R.drawable.top_start_previous);
            topStartBorderDrawable =
                    mPatchManager.getPatchDrawable(R.drawable.top_start_previous_border);
            mPreviousConnectorHighlightPatch =
                    mPatchManager.getPatchDrawable(R.drawable.top_start_previous_connection);
        } else if (mBlock.getOutputConnection() != null) {
            mHelper.setPointMaybeFlip(mOutputConnectorOffset, mOutputConnectorMargin, yTop);
            topStartDrawable = getColoredPatchDrawable(
                    isShadow ? R.drawable.top_start_output_shadow : R.drawable.top_start_output);
            topStartBorderDrawable =
                    mPatchManager.getPatchDrawable(R.drawable.top_start_output_border);
            mOutputConnectorHighlightPatch =
                    mPatchManager.getPatchDrawable(R.drawable.top_start_output_connection);
        } else if (mUseHat) {
            topStartDrawable = getColoredPatchDrawable(
                    isShadow ? R.drawable.top_start_hat_shadow : R.drawable.top_start_hat);
            topStartBorderDrawable =
                    mPatchManager.getPatchDrawable(R.drawable.top_start_hat_border);
        } else {
            topStartDrawable = getColoredPatchDrawable(
                    isShadow ? R.drawable.top_start_default_shadow : R.drawable.top_start_default);
            topStartBorderDrawable =
                    mPatchManager.getPatchDrawable(R.drawable.top_start_default_border);
        }
        mHelper.setRtlAwareBounds(tempRect,
                /* this width */ mBlockViewSize.x,
                /* LTR start */ 0,
                /* top */ 0,
                /* LTR end */ xTo,
                /* bottom */ topStartDrawable.getIntrinsicHeight());
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
    private void addDummyBoundaryPatch(boolean isShadow, int xTo, InputView inputView,
                                       ViewPoint inputLayoutOrigin) {
        // For external dummy inputs, put a patch for the block boundary.
        final NinePatchDrawable inputDrawable = getColoredPatchDrawable(
                isShadow ? R.drawable.dummy_input_shadow : R.drawable.dummy_input);
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
        mHelper.setRtlAwareBounds(tempRect,
                /* this width */  mBlockViewSize.x,
                /* LTR start */ xTo - width,
                /* top */ inputLayoutOrigin.y + (inTopRow ? mBlockTopPadding : 0),
                /* LTR end */ xTo,
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
    private void addExternalValueInputPatch(boolean isShadow, int i, int xTo,
                                            InputView inputView, ViewPoint inputLayoutOrigin) {
        // Position patch and connector for external value input.
        mHelper.setPointMaybeFlip(mInputConnectorOffsets.get(i), xTo, inputLayoutOrigin.y);

        final NinePatchDrawable inputDrawable = getColoredPatchDrawable(isShadow
                ? R.drawable.value_input_external_shadow : R.drawable.value_input_external);
        final NinePatchDrawable inputBorderDrawable =
                mPatchManager.getPatchDrawable(R.drawable.value_input_external_border);
        final NinePatchDrawable connectionHighlightDrawable =
                mPatchManager.getPatchDrawable(R.drawable.value_input_external_connection);

        int patchLeft = xTo - inputDrawable.getIntrinsicWidth();
        int patchRight = xTo;
        int connectorTop = inputLayoutOrigin.y + mBlockTopPadding;
        int connectorBottom = inputLayoutOrigin.y + inputView.getMeasuredHeight();

        mHelper.setRtlAwareBounds(tempRect,
                /* this width */  mBlockViewSize.x,
                /* LTR start */ patchLeft,
                /* top */ connectorTop,
                /* LTR end */ patchRight,
                /* bottom */ connectorBottom);
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
            mHelper.setRtlAwareBounds(tempRect,
                    /* this width */  mBlockViewSize.x,
                    /* LTR start */ patchLeft,
                    /* top */ inputLayoutOrigin.y,
                    /* LTR end */patchRight,
                    /* bottom */ connectorTop);
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
    private void addInlineValueInputPatch(boolean isShadow, int i, int inlineRowIdx, int blockFromX,
                                          InputView inputView, ViewPoint inputLayoutOrigin) {
        // Determine position for inline connector cutout.
        final int cutoutX = blockFromX + inputLayoutOrigin.x + inputView.getInlineInputX();
        final int cutoutY = inputLayoutOrigin.y + mBlockTopPadding;

        // Set connector position - shift w.r.t. patch location to where the corner of connected
        // blocks will be positioned.
        mHelper.setPointMaybeFlip(mInputConnectorOffsets.get(i),
                cutoutX + mPatchManager.mInlineInputStartPadding +
                        mPatchManager.mOutputConnectorWidth,
                cutoutY + mPatchManager.mInlineInputTopPadding);

        // Fill above inline input connector, unless first row, where connector top
        // is aligned with block boundary patch.
        if (inlineRowIdx > 0) {
            fillRectBySize(cutoutX, inputLayoutOrigin.y,
                    inputView.getTotalChildWidth(), mBlockTopPadding);
            finishFillRect();  // Prevent filling through the inline connector.
        }

        // Position a properly-sized input cutout patch.
        final NinePatchDrawable inputDrawable = getColoredPatchDrawable(
                isShadow ? R.drawable.value_input_inline_shadow : R.drawable.value_input_inline);
        final NinePatchDrawable connectionHighlightDrawable =
                mPatchManager.getPatchDrawable(R.drawable.value_input_inline_connection);
        mHelper.setRtlAwareBounds(tempRect,
                /* this width */  mBlockViewSize.x,
                /* LTR start */ cutoutX,
                /* top */ cutoutY,
                /* LTR end */ cutoutX + inputView.getTotalChildWidth(),
                /* bottom */ cutoutY + inputView.getTotalChildHeight());
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
                    (inlineRowIdx > 0 ? 0 : mBlockTopPadding);
            final int patchRight = patchX + mPatchManager.mBlockEndPadding;

            final NinePatchDrawable blockFillDrawable = getColoredPatchDrawable(
                    isShadow ? R.drawable.dummy_input_shadow : R.drawable.dummy_input);
            final NinePatchDrawable blockFillBorderDrawable =
                    mPatchManager.getPatchDrawable(R.drawable.dummy_input_border);

            mHelper.setRtlAwareBounds(tempRect,
                    /* this width */  mBlockViewSize.x,
                    /* LTR start */ patchX,
                    /* top */ patchY,
                    /* LTR end */ patchRight,
                    /* bottom */ cutoutEndY);
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
    private void addStatementInputPatches(boolean isShadow, int i,
                                          int xFrom, int xToAbove, int xToBelow,
                                          InputView inputView, ViewPoint inputLayoutOrigin) {
        // Position connector. Shift by horizontal and vertical patch thickness to line up with
        // "Previous" connector on child block.
        int xOffset = xFrom + inputView.getFieldLayoutWidth();
        mHelper.setPointMaybeFlip(mInputConnectorOffsets.get(i),
                xOffset + mPatchManager.mStatementInputPadding,
                inputLayoutOrigin.y + mPatchManager.mStatementTopThickness);

        // Position patch for the top part of the Statement connector. This patch is
        // stretched only horizontally to extend to the block boundary.
        final NinePatchDrawable statementTopDrawable = getColoredPatchDrawable(
                isShadow ? R.drawable.statementinput_top_shadow : R.drawable.statementinput_top);
        final NinePatchDrawable statementTopBorderDrawable =
                mPatchManager.getPatchDrawable(R.drawable.statementinput_top_border);
        final NinePatchDrawable statementConnectionHighlight =
                mPatchManager.getPatchDrawable(R.drawable.statementinput_top_connection);

        mHelper.setRtlAwareBounds(tempRect,
                /* this width */  mBlockViewSize.x,
                /* LTR start */ xOffset,
                /* top */ inputLayoutOrigin.y,
                /* LTR end */ xToAbove,
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
        final NinePatchDrawable statementBottomDrawable = getColoredPatchDrawable(isShadow ?
                R.drawable.statementinput_bottom_shadow : R.drawable.statementinput_bottom);
        final NinePatchDrawable statementBottomBorderDrawable =
                mPatchManager.getPatchDrawable(R.drawable.statementinput_bottom_border);

        final int connectorHeight =
                Math.max(inputView.getTotalChildHeight(),
                        inputView.getMeasuredHeight());

        mHelper.setRtlAwareBounds(tempRect,
                /* this width */  mBlockViewSize.x,
                /* LTR start */ xOffset,
                /* top */ inputLayoutOrigin.y + statementTopDrawable.getIntrinsicHeight(),
                /* LTR end */ xToBelow,
                /* bottom */ inputLayoutOrigin.y + connectorHeight);
        statementBottomDrawable.setBounds(tempRect);
        statementBottomBorderDrawable.setBounds(tempRect);

        mBlockPatches.add(statementBottomDrawable);
        mBlockBorderPatches.add(statementBottomBorderDrawable);
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
            mNextFillRect.top = Math.max(mNextFillRect.top, mBlockTopPadding);
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
        // TODO: (#161) Use flat 9-patches for shadow blocks
        NinePatchDrawable drawable = mPatchManager.getPatchDrawable(id);
        drawable.setColorFilter(mBlockColorFilter);
        return drawable;
    }
}
