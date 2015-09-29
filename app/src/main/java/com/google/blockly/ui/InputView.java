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
import android.graphics.Path;
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
import com.google.blockly.ui.fieldview.FieldView;

import java.util.ArrayList;
import java.util.List;

/**
 * View representation of an {@link Input} to a {@link com.google.blockly.model.Block}.
 */
public class InputView extends ViewGroup {
    private static final String TAG = "InputView";

    // Horizontal padding between field bounds and content.
    static final int FIELD_PADDING_X = 30;
    // Vertical padding between field bounds and content.
    static final int FIELD_PADDING_Y = 10;

    // The minimum height of an input is what is needed for a centered input connector with padding.
    static final int MIN_HEIGHT =
            2 * ConnectorHelper.OFFSET_FROM_CORNER + ConnectorHelper.SIZE_PARALLEL;
    // The minimum width of an input, in dips.
    static final int MIN_WIDTH = 40;

    // The horizontal distance between fields, in dips.
    private static final int DEFAULT_FIELD_SPACING = 10;

    private final Input mInput;
    private final WorkspaceHelper mHelper;
    private final ArrayList<FieldView> mFieldViews = new ArrayList<>();

    private int mHorizontalFieldSpacing;

    // Total measured width of all fields including padding around and between them.
    private int mTotalFieldWidth;
    // Maximum height over all fields, not including padding.
    private int mMaxFieldHeight;
    // Width to use for laying out fields. This can be different from mTotalFieldWidth to allow for
    // alignment of fields across multiple inputs within a block.
    private int mFieldLayoutWidth;

    // Measured width of the child, or empty-connector width for unconnected inline value inputs.
    private int mChildWidth;
    // Measured height of the child, or empty-connector height for unconnected inline value inputs.
    private int mChildHeight;

    // The view of the block connected to this input.
    private View mChildView = null;

    // Flag to enforce that measureFieldsAndInputs() is called before each call to measure().
    private boolean mHasMeasuredFieldsAndInput = false;

    InputView(Context context, int blockStyle, Input input, WorkspaceHelper helper) {
        super(context);

        mInput = input;
        mInput.setView(this);
        mHelper = helper;

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
    public View getChildView() {
        return mChildView;
    }

    /**
     * Set the view of the block whose Output port is connected to this input.
     * <p/>
     * This class is agnostic to the view type of the connected child, i.e., this could be a
     * {@link BlockView}, a {@link BlockGroup}, or any other type of view.
     *
     * @param childView The {@link BlockView} or {@link BlockGroup} to attach to this input. The
     *                  {@code childView} will be added to the layout hierarchy for the current view
     *                  via a call to {@link ViewGroup#addView(View)}.
     * @throws IllegalStateException    if a child view is already set. The Blockly model requires
     *                                  disconnecting a block from an input before a new one can be connected.
     * @throws IllegalArgumentException if the method argument is {@code null}.
     */
    public void setChildView(View childView) {
        if (mChildView != null) {
            throw new IllegalStateException("Input is already connected; must disconnect first.");
        }

        if (childView == null) {
            throw new IllegalArgumentException("Cannot use setChildView with a null child. " +
                    "Use unsetChildView to remove a child view.");
        }

        mChildView = childView;
        addView(mChildView);
        requestLayout();
    }

    /**
     * Disconnect the currently-connected child view from this input.
     * <p/>
     * This method also removes the child view from the view hierarchy by calling
     * {@link ViewGroup#removeView(View)}.
     */
    public void unsetChildView() {
        if (mChildView != null) {
            removeView(mChildView);
            mChildView = null;
            requestLayout();
        }
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (!mHasMeasuredFieldsAndInput) {
            throw new IllegalStateException("InputView.measureFieldsAndInputs()" +
                    " must be called before each call to measure().");
        }
        mHasMeasuredFieldsAndInput = false;

        int width = mFieldLayoutWidth + mChildWidth;
        if (getInput().getType() == Input.TYPE_VALUE && getInput().getBlock().getInputsInline()) {
            width += FIELD_PADDING_X;
        }

        // Height is maximum of field height with padding or child height, and at least MIN_HEIGHT.
        int height = Math.max(MIN_HEIGHT,
                Math.max(mMaxFieldHeight + 2 * FIELD_PADDING_Y, mChildHeight));

        setMeasuredDimension(width, height);
    }

    @Override
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        boolean rtl = mHelper.useRtL();
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
            view.layout(left, FIELD_PADDING_Y, left + fieldWidth, FIELD_PADDING_Y + fieldHeight);

            // Move x position left or right, depending on RTL mode.
            cursorX += fieldWidth + mHorizontalFieldSpacing;
        }

        layoutChild();
    }

    /**
     * If there is a child connected to this Input, then layout the child in the correct place.
     */
    private void layoutChild() {
        if (mChildView != null) {
            int inputType = mInput.getType();
            if (inputType == Input.TYPE_STATEMENT || inputType == Input.TYPE_VALUE) {
                int width = mChildView.getMeasuredWidth();
                int height = mChildView.getMeasuredHeight();

                // Align top of fields and input, unless this is an inline Value input, in which
                // case field padding must be added.
                int top = 0;
                if (inputType == Input.TYPE_VALUE && getInput().getBlock().getInputsInline()) {
                    top = FIELD_PADDING_Y;
                }

                boolean rtl = mHelper.useRtL();
                int left = rtl ? getMeasuredWidth() - mFieldLayoutWidth - width : mFieldLayoutWidth;

                mChildView.layout(left, top, left + width, top + height);
            }
        }
    }

    /**
     * Get horizontal (x) coordinate for first field in the input.
     * @return The x coordinate for the first field in this input.
     */
    private int getFirstFieldX() {
        switch (mInput.getAlign()) {
            default:
            case Input.ALIGN_LEFT: {
                return FIELD_PADDING_X;
            }
            case Input.ALIGN_CENTER: {
                return FIELD_PADDING_X + (mFieldLayoutWidth - mTotalFieldWidth) / 2;
            }
            case Input.ALIGN_RIGHT: {
                return FIELD_PADDING_X + mFieldLayoutWidth - mTotalFieldWidth;
            }
        }
    }

    // Measure only the fields of this input view.
    private void measureFields(int widthMeasureSpec, int heightMeasureSpec) {
        mTotalFieldWidth = 2 * FIELD_PADDING_X;
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
    private void measureInputs(int widthMeasureSpec, int heightMeasureSpec) {
        if (mChildView != null) {
            // There is a block group connected to this input - measure it and add its size
            // to this InputView's size.
            mChildView.measure(widthMeasureSpec, heightMeasureSpec);
            mChildWidth = mChildView.getMeasuredWidth();
            mChildHeight = mChildView.getMeasuredHeight();
        } else {
            // There's nothing connected to this input - use the size of the empty connectors.
            if (getInput().getBlock().getInputsInline()) {
                switch (getInput().getType()) {
                    default:
                    case Input.TYPE_DUMMY: {
                        break;
                    }
                    case Input.TYPE_VALUE: {
                        mChildWidth = ConnectorHelper.OPEN_INLINE_CONNECTOR_WIDTH;
                        mChildHeight = ConnectorHelper.OPEN_INLINE_CONNECTOR_HEIGHT;
                        break;
                    }
                    case Input.TYPE_STATEMENT: {
                        mChildWidth = MIN_WIDTH;
                        mChildHeight = MIN_HEIGHT + ConnectorHelper.SIZE_PERPENDICULAR;
                        break;
                    }
                }
            } else {
                if (getInput().getType() == Input.TYPE_STATEMENT) {
                    mChildWidth = MIN_WIDTH;
                    mChildHeight = MIN_HEIGHT + ConnectorHelper.SIZE_PERPENDICULAR;
                } else {
                    mChildWidth = 0;
                    mChildHeight = 0;
                }
            }
        }
    }

    /**
     * Initialize style attributes.
     *
     * @param context    Context of this view.
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
     */
    private void initViews(Context context) {
        List<Field> fields = mInput.getFields();
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
                case Field.TYPE_IMAGE:
                    view = new FieldImageView(context, fields.get(j), mHelper);
                    break;
                default:
                    // TODO (fenichel): Add variable field type.
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
     *                         field width.
     */
    void setFieldLayoutWidth(int fieldLayoutWidth) {
        mFieldLayoutWidth = fieldLayoutWidth;
        requestLayout();
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
        measureInputs(widthMeasureSpec, heightMeasureSpec);

        // For inline inputs, consider the connected input block(s) like a field for  measurement.
        // Width is treated equally for inline and external inputs, since in both cases connected
        // blocks are positioned to the right (or left, in RTL mode) of the fields.
        if (getInput().getBlock().getInputsInline()) {
            mMaxFieldHeight = Math.max(mMaxFieldHeight, mChildHeight);
        }

        mHasMeasuredFieldsAndInput = true;
    }

    /**
     * Add cutout for inline value input to draw path of {@link BlockView}.
     *
     * @param path    The draw path of the {@link BlockView} as assembled so far. Commands to draw the
     *                cutout are appended to this path.
     * @param xOffset The horizontal offset for cutout path coordinates provided by the caller to
     *                position the cutout in the parent's view area. In RTL mode, this is the
     *                relative position of the right-hand side of the cutout, otherwise of its
     *                left-hand side.
     * @param yOffset The vertical offset for cutout path coordinates provided by the caller to
     *                position the cutout in the parent's view area.
     * @param rtlSign Sign of horizontal layout direction. In RTL mode, this is -1, otherwise +1.
     * @param connectorPosition A {@link ViewPoint} in which the function returns the coordinate of
     *                          the input connector in parent coordinates.
    */
    void addInlineCutoutToBlockViewPath(Path path, int xOffset, int yOffset, int rtlSign,
                                        ViewPoint connectorPosition) {
        int top = yOffset + InputView.FIELD_PADDING_Y;
        int bottom = top + getTotalChildHeight();

        int right = xOffset + rtlSign * (getMeasuredWidth() - InputView.FIELD_PADDING_X);
        int left = right + rtlSign * (ConnectorHelper.SIZE_PERPENDICULAR - getTotalChildWidth());

        path.moveTo(left, top);
        path.lineTo(right, top);
        path.lineTo(right, bottom);
        path.lineTo(left, bottom);
        ConnectorHelper.addOutputConnectorToPath(path, left, top, rtlSign);
        path.lineTo(left, top);
        // Draw an additional line segment over again to get a final rounded corner.
        path.lineTo(left + rtlSign * ConnectorHelper.OFFSET_FROM_CORNER, top);

        connectorPosition.set(left, top);
    }
}
