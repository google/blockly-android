/*
 * Copyright  2015 Google Inc. All Rights Reserved.
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

package com.google.blockly.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.google.blockly.model.WorkspacePoint;

/**
 * This wraps a set of sequential {@link BlockView} instances.
 */
public class BlockGroup extends ViewGroup {
    private final WorkspaceHelper mWorkspaceHelper;

    private int mNextBlockVerticalOffset;

    public BlockGroup(Context context, WorkspaceHelper helper) {
        super(context);
        mWorkspaceHelper = helper;
    }

    @Override
    public void onMeasure(int widthSpec, int heightSpec) {
        mNextBlockVerticalOffset = 0;

        int childCount = getChildCount();
        int width = 0;
        int height = 0;

        for (int i = 0; i < childCount; i++) {
            BlockView child = (BlockView) getChildAt(i);
            child.measure(widthSpec, heightSpec);
            width = Math.max(child.getMeasuredWidth(), width);

            // Only for last child, add the entire measured height. For all other children, add
            // offset to next block. This takes into account that blocks are rendered with
            // overlaps due to extruding "Next" connectors.
            if (i == childCount - 1) {
                height += child.getMeasuredHeight();
            } else {
                height += child.getNextBlockVerticalOffset();
            }
            mNextBlockVerticalOffset += child.getNextBlockVerticalOffset();
        }
        setMeasuredDimension(width, height);
    }

    @Override
    public void addView(View child, int index, LayoutParams params) {
        if (!(child instanceof BlockView)) {
            // There's no guarantees only BlockViews can be added, but this should catch most cases
            // and serve as a strong warning not to do this.
            throw new IllegalArgumentException("BlockGroups may only contain BlockViews");
        }
        super.addView(child, index, params);
    }

    /**
     * @return The workspace position of the top block in this group, or {@code null} if this group
     * is empty.
     */
    public WorkspacePoint getTopBlockPosition() {
        if (getChildCount() > 0) {
            return ((BlockView) getChildAt(0)).getBlock().getPosition();
        }
        return null;
    }

    /**
     * @return The vertical offset from the top of this view to the position of the next block
     * <em>below</em> this group.
     */
    int getNextBlockVerticalOffset() {
        return mNextBlockVerticalOffset;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int childCount = getChildCount();
        boolean rtl = mWorkspaceHelper.useRtL();
        int x = rtl ? getMeasuredWidth() : 0;
        int y = 0;

        for (int i = 0; i < childCount; i++) {
            BlockView child = (BlockView) getChildAt(i);
            int h = child.getMeasuredHeight();
            int w = child.getMeasuredWidth();
            int cl = rtl ? x - w : x;
            int cr = rtl ? x : x + w;
            int ct = y;
            int cb = y + h;

            child.layout(cl, ct, cr, cb);
            y += child.getNextBlockVerticalOffset();
        }
    }
}
