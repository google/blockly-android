/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import com.google.blockly.android.ui.WorkspaceHelper;
import com.google.blockly.android.ui.fieldview.BasicFieldColourView;

/**
 * {@link BasicFieldColourView} with a inset emboss that matches its containing BlockView.
 */
public class FieldColourView extends BasicFieldColourView {
    protected WorkspaceHelper mHelper;
    private BlockView mBlockView = null;
    private int mDrawWidth;
    private int mDrawHeight;

    public FieldColourView(Context context) {
        super(context);
    }

    public FieldColourView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FieldColourView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setWorkspaceHelper(WorkspaceHelper helper) {
        mHelper = helper;
        maybeAcquireParentBlockView();
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        updateDrawSize();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        maybeAcquireParentBlockView();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawRect(0, 0, mDrawWidth, mDrawHeight, mSelectedColourPaint);
        getBackground().draw(canvas);
    }

    private void updateDrawSize() {
        Drawable bg = getBackground();
        mDrawWidth = Math.max(bg.getMinimumWidth(), getWidth());
        mDrawHeight = Math.max(bg.getMinimumHeight(), getHeight());
    }

    private void maybeAcquireParentBlockView() {
        if (mHelper != null && isAttachedToWindow()) {
            mBlockView = (BlockView) mHelper.getClosestAncestorBlockView(this);
            if (mBlockView != null) {
                getBackground().setColorFilter(mBlockView.getColorFilter());
            }
        }
    }
}
