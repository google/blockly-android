/*
 *  Copyright 2015 Google Inc. All Rights Reserved.
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
import android.view.ViewGroup;

import com.google.blockly.R;
import com.google.blockly.model.Field;
import com.google.blockly.model.Input;
import com.google.blockly.ui.fieldview.FieldAngleView;
import com.google.blockly.ui.fieldview.FieldCheckboxView;
import com.google.blockly.ui.fieldview.FieldColourView;
import com.google.blockly.ui.fieldview.FieldDateView;
import com.google.blockly.ui.fieldview.FieldDropdownView;
import com.google.blockly.ui.fieldview.FieldImageView;
import com.google.blockly.ui.fieldview.FieldInputView;
import com.google.blockly.ui.fieldview.FieldLabelView;
import com.google.blockly.ui.fieldview.FieldVariableView;
import com.google.blockly.ui.fieldview.FieldView;

import java.util.ArrayList;
import java.util.List;

/**
 * View representation of an {@link Input} to a {@link com.google.blockly.model.Block}.
 */
public class InputView extends NonPropagatingViewGroup {
    private static final String TAG = "InputView";

    // The horizontal distance between fields, in dips.
    private static final int DEFAULT_FIELD_SPACING = 10;

    private final Input mInput;
    private final @Input.InputType int mInputType;

    private final WorkspaceHelper mHelper;
    private final PatchManager mPatchManager;
    private final ArrayList<FieldView> mFieldViews = new ArrayList<>();

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
    private int mChildWidth;
    // Measured height of the child, or empty-connector height for unconnected inline value inputs.
    private int mChildHeight;

    // The view of the blocks connected to this input.
    private BlockGroup mChildView = null;

    // Flag to enforce that measureFieldsAndInputs() is called before each call to measure().
    private boolean mHasMeasuredFieldsAndInput = false;

    InputView(Context context, int blockStyle, Input input, WorkspaceHelper helper) {
        super(context);

        mInput = input;
        mInputType = mInput.getType();
        mInput.setView(this);
        mHelper = helper;
        mPatchManager = mHelper.getPatchManager();  // Shortcut.

        initAttrs(context, blockStyle);
        initViews(context);
    }

    /**
     * @return The block {@link Input} wrapped by this view.
     */
    public Input getInput() {
        return mInput;
    }

    /**
     * @return The child view connected to this input port.
     */
    public BlockGroup getChildView() {
        return mChildView;
    }

    /**
     * Set the view of the blocks whose Output port is connected to this input.
     *
     * @param childView The {@link BlockGroup} to attach to this input. The {@code childView} will
     *                  be added to the layout hierarchy for the current view via a call to
     *                  {@link ViewGroup#addView(View)}.
     *
     * @throws IllegalStateException if a child view is already set. The Blockly model requires
     * disconnecting a block from an input before a new one can be connected.
     * @throws IllegalArgumentException if the method argument is {@code null}.
     */
    public void setChildView(BlockGroup childView) {
        if (mChildView != null) {
            throw new IllegalStateException("Input is already connected; must disconnect first.");
        }

        if (childView == null) {
            throw new IllegalArgumentException("Cannot use setChildView with a null child. " +
                    "Use unsetChildView to remove a child view.");
        }

        addView(childView);
        mChildView = childView;
        requestLayout();
    }

    /**
     * Disconnect the currently-connected child view from this input.
     * <p/>
     * This method also removes the child view from the view hierarchy by calling
     * {@link ViewGroup#removeView(View)}.
     *
     * @return The removed child view, if any. Otherwise, null.
     */
    public BlockGroup unsetChildView() {
        BlockGroup result = mChildView;
        if (mChildView != null) {
            removeView(mChildView);
            mChildView = null;
            requestLayout();
        }

        return result;
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (!mHasMeasuredFieldsAndInput) {
            throw new IllegalStateException("InputView.measureFieldsAndInputs()" +
                    " must be called before each call to measure().");
        }
        mHasMeasuredFieldsAndInput = false;

        // Width is the width of all fields, plus padding, plus width of child.
        final int width = mFieldLayoutWidth + mPatchManager.mBlockTotalPaddingX + mChildWidth;

        // Height is maximum of field height with padding or child height, and at least the minimum
        // height for an empty block.
        final int height = Math.max(mPatchManager.mMinBlockHeight,
                Math.max(mMaxFieldHeight + mPatchManager.mBlockTotalPaddingY, mChildHeight));

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
     * Recursively disconnects the view from the model, and removes all views.
     */
    void unlinkModelAndSubViews() {
        int max = mFieldViews.size();
        for (int i = 0; i < max; ++i) {
            FieldView fieldView = mFieldViews.get(i);
            fieldView.unlinkModel();
        }
        if (mChildView != null) {
            mChildView.unlinkModelAndSubViews();
            unsetChildView();
        }
        removeAllViews();
        mInput.setView(null);
        // TODO(#45): Remove model from view. Set mInput to null, and handle all null cases.
    }

    /**
     * If there is a child connected to this Input, then layout the child in the correct place.
     */
    private void layoutChild() {
        if (mChildView != null) {
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

            final int width = mChildView.getMeasuredWidth();
            final int height = mChildView.getMeasuredHeight();

            if (mHelper.useRtl()) {
                leftOffset = getMeasuredWidth() - leftOffset - width;
            }

            mChildView.layout(leftOffset, topOffset, leftOffset + width, topOffset + height);
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

        if (mChildView != null) {
            // There is a block group connected to this input - measure it and add its size
            // to this InputView's size.
            mChildView.measure(widthMeasureSpec, heightMeasureSpec);
            mChildWidth = mChildView.getMeasuredWidth();
            mChildHeight = mChildView.getMeasuredHeight();

            // Only add space for decorations around Statement and inline Value inputs.
            switch (mInputType) {
                case  Input.TYPE_VALUE: {
                    if (inputsInline) {
                        // Inline Value input - add space for connector that is enclosing connected
                        // block(s).
                        mChildWidth += mPatchManager.mInlineInputTotalPaddingX;
                        mChildHeight += mPatchManager.mInlineInputTotalPaddingY;
                    }
                    break;
                }
                case Input.TYPE_STATEMENT: {
                    // Statement input - add space for top and bottom of C-connector.
                    mChildHeight += mPatchManager.mStatementTopThickness +
                            mPatchManager.mStatementBottomThickness;
                    break;
                }
                default: {
                    // Nothing to do for other types of inputs.
                }
            }
        } else {
            // There's nothing connected to this input - use the size of the empty connectors.
            mChildWidth = emptyConnectorWidth(inputsInline);
            mChildHeight = emptyConnectorHeight(inputsInline);
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
    private void initViews(Context context) {
        List<Field> fields = mInput.getFields();
        for (int j = 0; j < fields.size(); j++) {
            FieldView view = mHelper.buildFieldView(fields.get(j));
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
        return mChildWidth;
    }

    /**
     * @return Total height of all children connected to this input.
     */
    int getTotalChildHeight() {
        return mChildHeight;
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
            mMaxFieldHeight = Math.max(mMaxFieldHeight, mChildHeight);
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
