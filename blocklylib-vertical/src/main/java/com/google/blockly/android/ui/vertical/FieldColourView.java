package com.google.blockly.android.ui.vertical;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.NinePatchDrawable;

import com.google.blockly.android.ui.WorkspaceHelper;
import com.google.blockly.android.ui.fieldview.BasicFieldColourView;
import com.google.blockly.model.Field;

/**
 * {@link BasicFieldColourView} with a inset emboss that matches its containing BlockView.
 */
public class FieldColourView extends BasicFieldColourView {
    private final NinePatchDrawable mInsetPatch;

    protected final WorkspaceHelper mHelper;
    private BlockView mBlockView = null;

    public FieldColourView(Context context, Field colourField, WorkspaceHelper helper) {
        super(context, colourField);

        mHelper = helper;
        mInsetPatch = (NinePatchDrawable) getResources().getDrawable(R.drawable.inset_field_border);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mBlockView = (BlockView) mHelper.getClosestAncestorBlockView(this);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int width = Math.max(getWidth(), mInsetPatch.getMinimumWidth());
        int height = Math.max(getHeight(), mInsetPatch.getMinimumHeight());

        canvas.drawRect(0, 0, width, height, mSelectedColourPaint);
        if (mBlockView != null) {
            mInsetPatch.setBounds(0, 0, width, height);
            mInsetPatch.setColorFilter(mBlockView.getColorFilter());
            mInsetPatch.draw(canvas);
        }
    }
}
