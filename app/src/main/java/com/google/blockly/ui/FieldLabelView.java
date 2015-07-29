package com.google.blockly.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import com.google.blockly.model.Field;

/**
 * Renders text as part of a BlockView
 */
public class FieldLabelView extends TextView implements FieldView {
    private final Field mLabel; // Field.FieldLabel

    public FieldLabelView(Context context, AttributeSet attrs, Field label) {
        super(context, attrs);
        mLabel = label;
        setText(label.getName());
        setBackground(null);
    }

    @Override
    public int getInBlockHeight() {
        return getWidth();
    }

    @Override
    public int getInBlockWidth() {
        return getHeight();
    }
}
