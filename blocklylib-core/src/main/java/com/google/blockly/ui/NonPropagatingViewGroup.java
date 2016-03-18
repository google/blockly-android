package com.google.blockly.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;

/**
 * ViewGroup base class that does not propagate pressed, activated, or selected state to child
 * Views.
 */
public abstract class NonPropagatingViewGroup extends ViewGroup {
    public NonPropagatingViewGroup(Context context) {
        super(context);
    }

    public NonPropagatingViewGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NonPropagatingViewGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public NonPropagatingViewGroup(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void dispatchSetActivated(boolean activated) {
        // Do nothing.  Do not assign the activated state to children.
    }

    @Override
    public void dispatchSetPressed(boolean pressed) {
        // Do nothing.  Do not assign the pressed state to children.
    }

    @Override
    public void dispatchSetSelected(boolean selected) {
        // Do nothing.  Do not assign the selected state to children.
    }
}
