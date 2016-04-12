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
import android.graphics.drawable.NinePatchDrawable;

import com.google.blockly.android.ui.WorkspaceHelper;
import com.google.blockly.android.ui.fieldview.BasicFieldColourView;

/**
 * {@link BasicFieldColourView} with a inset emboss that matches its containing BlockView.
 */
public class FieldColourView extends BasicFieldColourView {
    private final NinePatchDrawable mInsetPatch;

    protected WorkspaceHelper mHelper;
    private BlockView mBlockView = null;

    private boolean mAttachedToWindow = false; // replaces API 19 method isAttachedToWindow()

    public FieldColourView(Context context) {
        super(context);
        mInsetPatch = (NinePatchDrawable) getResources().getDrawable(R.drawable.inset_field_border);
    }

    public void setWorkspaceHelper(WorkspaceHelper helper) {
        mHelper = helper;
        maybeAcquireParentBlockView();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        mAttachedToWindow = true;
        maybeAcquireParentBlockView();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mAttachedToWindow = false;
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

    private void maybeAcquireParentBlockView() {
        if (mHelper != null && mAttachedToWindow) {
            mBlockView = (BlockView) mHelper.getClosestAncestorBlockView(this);
        }
    }
}
