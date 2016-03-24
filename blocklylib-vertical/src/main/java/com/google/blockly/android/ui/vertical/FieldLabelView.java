package com.google.blockly.android.ui.vertical;

import android.content.Context;
import android.content.res.TypedArray;

import com.google.blockly.android.ui.fieldview.BasicFieldLabelView;
import com.google.blockly.model.Field;

/**
 * A {@link BasicFieldLabelView} that supports Blockly specific style attributes.
 *
 * @deprecated (#141) To be replaced by inflating styled layouts.
 */
public class FieldLabelView extends BasicFieldLabelView {
    /**
     * Create a view for the given field using the specified style.
     *
     * @param context         The context for creating the view and loading resources.
     * @param labelField      The label this view is rendering.
     * @param styleAttributes The style to use on this view.
     */
    public FieldLabelView(Context context, Field labelField, TypedArray styleAttributes) {
        super(context, labelField, 0);

        applyStyle(styleAttributes);
    }

    protected void applyStyle(TypedArray styleAttributes) {
        if (styleAttributes == null) {  // Simplifies testing.
            return;
        }

        int textStyle = styleAttributes.getResourceId(
                R.styleable.BlocklyFieldView_textAppearance, 0);
        if (textStyle != 0) {
            setTextAppearance(getContext(), textStyle);
        }

        int minHeight = (int) styleAttributes.getDimension(
                R.styleable.BlocklyFieldView_fieldMinHeight, 0);
        if (minHeight != 0) {
            setMinimumHeight(minHeight);
        }
    }
}
