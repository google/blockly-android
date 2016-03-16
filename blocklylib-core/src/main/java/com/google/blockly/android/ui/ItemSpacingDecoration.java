/*
 *  Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.blockly.android.ui;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Item decoration that adds spacing around RecyclerView items.
 */
public class ItemSpacingDecoration extends RecyclerView.ItemDecoration {
    // TODO (#46): Load from resources, adapt to dpi.
    // Minimum pixel distance between blocks in the toolbox.
    private int mBlockMargin = 10;

    private RecyclerView.Adapter mAdapter;

    public ItemSpacingDecoration(RecyclerView.Adapter adapter) {
        mAdapter = adapter;
    }

    @Override
    public void getItemOffsets(
            Rect outRect, View child, RecyclerView parent, RecyclerView.State state) {
        int itemPosition = parent.getChildPosition(child);
        int bottomMargin = (itemPosition == (mAdapter.getItemCount() - 1)) ? mBlockMargin : 0;
        outRect.set(mBlockMargin, mBlockMargin, mBlockMargin, bottomMargin);
    }
}
