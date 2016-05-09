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
import android.graphics.drawable.Drawable;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;

import com.google.blockly.android.ui.WorkspaceHelper;
import com.google.blockly.android.ui.fieldview.BasicFieldColorView;

/**
 * {@link BasicFieldColorView} with a inset emboss that matches its containing BlockView.
 */
public class FieldColorView extends BasicFieldColorView {
    protected WorkspaceHelper mHelper;

    public FieldColorView(Context context) {
        super(context);
    }

    public FieldColorView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FieldColorView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setWorkspaceHelper(WorkspaceHelper helper) {
        mHelper = helper;
        maybeAcquireParentBlockView();
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        maybeAcquireParentBlockView();
    }

    private void maybeAcquireParentBlockView() {
        BlockView blockView = (mHelper != null && ViewCompat.isAttachedToWindow(this)) ?
            (BlockView) mHelper.getClosestAncestorBlockView(this) : null;
        if (blockView != null) {
            int foregroundId = blockView.getBlock().isShadow() ?
                    R.drawable.inset_field_border_shadow : R.drawable.inset_field_border;
            Drawable foreground = getResources().getDrawable(foregroundId);
            foreground.setColorFilter(blockView.getColorFilter());
            setForeground(foreground);
        }
    }
}
