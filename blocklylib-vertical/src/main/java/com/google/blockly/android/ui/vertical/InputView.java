/*
 *  Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.blockly.android.ui.vertical;

import android.content.Context;
import android.content.res.TypedArray;
import android.view.View;

import com.google.blockly.model.Field;
import com.google.blockly.model.Input;
import com.google.blockly.android.ui.AbstractInputView;
import com.google.blockly.android.ui.BlockView;
import com.google.blockly.android.ui.fieldview.FieldView;

import java.util.List;

/**
 * View representation of an {@link Input} to a {@link com.google.blockly.model.Block}.
 */
public class InputView extends AbstractInputView {
    // The horizontal distance between fields, in dips.
    private static final int DEFAULT_FIELD_SPACING = 10;

    private final VerticalBlocksViewFactory mFactory;
    private final PatchManager mPatchManager;

    private int mHorizontalFieldSpacing;

    // Total measured width of all fields including padding around and between them.
    private int mTotalFieldWidth;
    // Maximum height over all fields, not including padding.
    private int mMaxFieldHeight;
    // Width to use for laying out fields. This can be different from mTotalFieldWidth to allow for
    // alignment of fields across multiple inputs within a block.
    private int mFieldLayoutWidth;
    // Height of the row that this view is part of. Used for determining whether a coordinate is
    // inside the visible area of this view.
    private int mRowHeight;

    // Measured width of the child, or empty-connector width for unconnected inline value inputs.
    private int mConnectedGroupWidth;
    // Measured height of the child, or empty-connector height for unconnected inline value inputs.
    private int mConnectedGroupHeight;

    // Flag to enforce that measureFieldsAndInputs() is called before each call to measure().
    private boolean mHasMeasuredFieldsAndInput = false;

    InputView(Context context, VerticalBlocksViewFactory factory, Input input) {
        super(context, factory.getWorkspaceHelper(), input);

        mFactory = factory;
        mPatchManager = factory.getPatchManager();  // Shortcut.

        initAttrs(context, mFactory.getBlockStyle());
        initViews(context);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (!mHasMeasuredFieldsAndInput) {
            throw new IllegalStateException("InputView.measureFieldsAndInputs()" +
                    " must be called before each call to measure().");
        }
        mHasMeasuredFieldsAndInput = false;

        // Width is the width of all fields, plus padding, plus width of child.
        final int width = mFieldLayoutWidth + mPatchManager.mBlockTotalPaddingX + mConnectedGroupWidth;

        // Height is maximum of field height with padding or child height, and at least the minimum
        // height for an empty block.
        final int height = Math.max(mPatchManager.mMinBlockHeight,
                Math.max(mMaxFieldHeight + mPatchManager.mBlockTotalPaddingY, mConnectedGroupHeight));

        setMeasuredDimension(width, height);

        // By default, the current row inside this input's block should be as high as this view's
        // measured height.
        mRowHeight = height;
    }

    @Override
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        boolean rtl = mHelper.useRtl();
        int viewWidth = getMeasuredWidth();

        // Initialize horizontal layout cursor. The cursor value increases as layout proceeds, but
        // actual layout locations in RTL mode are (viewWidth - cursorX).
        int cursorX = getFirstFieldX();

        for (int i = 0; i < mFieldViews.size(); i++) {
            View view = (View) mFieldViews.get(i);

            // Use the width and height of the field's view to compute its placement
            int fieldWidth = view.getMeasuredWidth();
            int fieldHeight = view.getMeasuredHeight();

            int left = rtl ? viewWidth - (cursorX + fieldWidth) : cursorX;
            view.layout(left, mPatchManager.mBlockTopPadding, left + fieldWidth,
                    mPatchManager.mBlockTopPadding + fieldHeight);

            // Move x position left or right, depending on RTL mode.
            cursorX += fieldWidth + mHorizontalFieldSpacing;
        }

        layoutChild();
    }

    /**
     * @return True if and only if a coordinate is on the fields of this view, including the padding
     * to the left and right of the fields.  Any connected inputs should handle events themselves
     * and are thus not allowed here.
     */
    public boolean isOnFields(int eventX, int eventY) {
        return (eventX >= 0 && eventX < (mFieldLayoutWidth + mPatchManager.mBlockTotalPaddingX)) &&
                eventY >= 0 && eventY < mRowHeight;
    }

    /**
     * If there is a child connected to this Input, then layout the child in the correct place.
     */
    private void layoutChild() {
        if (mConnectedGroup != null) {
            // Compute offset of child relative to InputView. By default, align top of fields and
            // input, and shift right by left padding plus field width.
            int topOffset = 0;
            int leftOffset = mFieldLayoutWidth + mPatchManager.mBlockStartPadding;
            switch (mInputType) {
                case Input.TYPE_VALUE: {
                    if (mInput.getBlock().getInputsInline()) {
                        topOffset += mPatchManager.mBlockTopPadding +
                                mPatchManager.mInlineInputTopPadding;
                        leftOffset += mPatchManager.mInlineInputStartPadding;
                    } else {
                        // The child block overlaps the parent block slightly at the connector, by
                        // the width of the extruding output connector.
                        leftOffset +=
                                mPatchManager.mBlockEndPadding + mPatchManager.mValueInputWidth -
                                mPatchManager.mOutputConnectorWidth;
                    }
                    break;
                }
                case Input.TYPE_STATEMENT: {
                    topOffset += mPatchManager.mStatementTopThickness;
                    leftOffset += mPatchManager.mStatementInputPadding;
                    break;
                }
                default:
                    // Nothing to do.
            }

            final int width = mConnectedGroup.getMeasuredWidth();
            final int height = mConnectedGroup.getMeasuredHeight();

            if (mHelper.useRtl()) {
                leftOffset = getMeasuredWidth() - leftOffset - width;
            }

            mConnectedGroup.layout(leftOffset, topOffset, leftOffset + width, topOffset + height);
        }
    }

    /**
     * Get horizontal (x) coordinate for first field in the input.
     *
     * @return The x coordinate for the first field in this input.
     */
    private int getFirstFieldX() {
        switch (mInput.getAlign()) {
            default:
            case Input.ALIGN_LEFT: {
                return mPatchManager.mBlockStartPadding;
            }
            case Input.ALIGN_CENTER: {
                return mPatchManager.mBlockStartPadding +
                        (mFieldLayoutWidth - mTotalFieldWidth) / 2;
            }
            case Input.ALIGN_RIGHT: {
                return mPatchManager.mBlockStartPadding + mFieldLayoutWidth - mTotalFieldWidth;
            }
        }
    }

    // Measure only the fields of this input view.
    private void measureFields(int widthMeasureSpec, int heightMeasureSpec) {
        mTotalFieldWidth = 0;
        mMaxFieldHeight = 0;

        if (mFieldViews.size() > 1) {
            mTotalFieldWidth += (mFieldViews.size() - 1) * mHorizontalFieldSpacing;
        }

        for (int j = 0; j < mFieldViews.size(); j++) {
            View field = (View) mFieldViews.get(j);
            field.measure(widthMeasureSpec, heightMeasureSpec);
            mTotalFieldWidth += field.getMeasuredWidth();
            mMaxFieldHeight = Math.max(mMaxFieldHeight, field.getMeasuredHeight());
        }

        // The field layout width defaults to the total measured width of all fields, but may be
        // overridden by the block that owns this input to force all of its rows to have identical
        // widths when rendering with external inputs.
        mFieldLayoutWidth = mTotalFieldWidth;
    }

    // Measure only blocks connected to this input.
    private void measureChildView(int widthMeasureSpec, int heightMeasureSpec) {
        final boolean inputsInline = mInput.getBlock().getInputsInline();

        if (mConnectedGroup != null) {
            // There is a block group connected to this input - measure it and add its size
            // to this InputView's size.
            mConnectedGroup.measure(widthMeasureSpec, heightMeasureSpec);
            mConnectedGroupWidth = mConnectedGroup.getMeasuredWidth();
            mConnectedGroupHeight = mConnectedGroup.getMeasuredHeight();

            // Only add space for decorations around Statement and inline Value inputs.
            switch (mInputType) {
                case  Input.TYPE_VALUE: {
                    if (inputsInline) {
                        // Inline Value input - add space for connector that is enclosing connected
                        // block(s).
                        mConnectedGroupWidth += mPatchManager.mInlineInputTotalPaddingX;
                        mConnectedGroupHeight += mPatchManager.mInlineInputTotalPaddingY;
                    }
                    break;
                }
                case Input.TYPE_STATEMENT: {
                    // Statement input - add space for top and bottom of C-connector.
                    mConnectedGroupHeight += mPatchManager.mStatementTopThickness +
                            mPatchManager.mStatementBottomThickness;
                    break;
                }
                default: {
                    // Nothing to do for other types of inputs.
                }
            }
        } else {
            // There's nothing connected to this input - use the size of the empty connectors.
            mConnectedGroupWidth = emptyConnectorWidth(inputsInline);
            mConnectedGroupHeight = emptyConnectorHeight(inputsInline);
        }
    }

    private int emptyConnectorWidth(boolean inputsInline) {
        if (mInputType == Input.TYPE_STATEMENT) {
            return mPatchManager.mBlockTotalPaddingX + mPatchManager.mStatementInputPadding;
        }

        if (inputsInline && mInputType == Input.TYPE_VALUE) {
            return mPatchManager.mInlineInputMinimumWidth;
        }

        return 0;
    }

    private int emptyConnectorHeight(boolean inputsInline) {
        if (mInputType == Input.TYPE_STATEMENT) {
            return mPatchManager.mStatementMinHeight;
        }

        if (inputsInline && mInputType == Input.TYPE_VALUE) {
            return mPatchManager.mInlineInputMinimumHeight;
        }

        return 0;
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
    }

    /**
     * Initialize child views for fields in the {@link Input} wrapped by this view.
     *
     * @param context Context of this view.
     */
    // TODO(#135): Move block tree traversal and view creation to factory class.
    private void initViews(Context context) {
        List<Field> fields = mInput.getFields();
        for (int j = 0; j < fields.size(); j++) {
            FieldView view = mFactory.buildFieldView(fields.get(j));
            if (view != null) {
                addView((View) view);
                mFieldViews.add(view);
            } else {
                throw new IllegalStateException("Attempted to render a field of an unknown"
                        + "type: " + fields.get(j).getType());
            }
        }
    }

    /**
     * @return Total measured width of all fields in this input, including spacing between them.
     */
    int getTotalFieldWidth() {
        return mTotalFieldWidth;
    }

    /**
     * @return Layout width for the fields in this input.
     */
    int getFieldLayoutWidth() {
        return mFieldLayoutWidth;
    }

    /**
     * Set the width for the field layout.
     * <p/>
     * This is called by the {@link BlockView} that owns this input to force all value inputs in the
     * same section to layout their fields with the same total width.
     *
     * @param fieldLayoutWidth The total field width to use for layout, regardless of the measured
     * field width.
     */
    void setFieldLayoutWidth(int fieldLayoutWidth) {
        mFieldLayoutWidth = fieldLayoutWidth;
        requestLayout();
    }

    /**
     * @return The height of the input row that this view is part of. Defaults to measured height
     * but can be overridden.
     */
    int getRowHeight() {
        return mRowHeight;
    }

    /**
     * Set the height of the row that this input view is part of.
     */
    void setRowHeight(int rowHeight) {
        mRowHeight = rowHeight;
    }

    /**
     * @return Total width of all children connected to this input.
     */
    int getTotalChildWidth() {
        return mConnectedGroupWidth;
    }

    /**
     * @return Total height of all children connected to this input.
     */
    int getTotalChildHeight() {
        return mConnectedGroupHeight;
    }

    /**
     * Pre-measure fields and inputs.
     * <p/>
     * The results of this pre-measurement pass are used by the owning Block to determine the
     * correct layout parameters across rows of external inputs.
     */
    void measureFieldsAndInputs(int widthMeasureSpec, int heightMeasureSpec) {
        // Measure fields and connected inputs separately.
        measureFields(widthMeasureSpec, heightMeasureSpec);
        measureChildView(widthMeasureSpec, heightMeasureSpec);

        // For inline Value inputs only, treat the connected input block(s) like a field for
        // measurement of input height.
        if ((mInputType == Input.TYPE_VALUE) && mInput.getBlock().getInputsInline()) {
            mMaxFieldHeight = Math.max(mMaxFieldHeight, mConnectedGroupHeight);
        }

        mHasMeasuredFieldsAndInput = true;
    }

    /**
     * Get horizontal position for inline connector in this input view.
     */
    int getInlineInputX() {
        return getMeasuredWidth() - mPatchManager.mBlockTotalPaddingX - getTotalChildWidth();
    }
}
