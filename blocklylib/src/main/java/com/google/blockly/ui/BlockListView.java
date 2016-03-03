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

package com.google.blockly.ui;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.google.blockly.model.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link RecyclerView} for Block and their containing {@link BlockGroup} views.  It does not
 * define a {@link RecyclerView.LayoutManager}.
 */
public class BlockListView extends RecyclerView {
    private final ArrayList<Block> mBlocks = new ArrayList<>();
    private final Adapter mAdapter = new Adapter();

    private WorkspaceHelper mHelper;
    private BlockTouchHandler mTouchHandler;

    public BlockListView(Context context) {
        this(context, null, 0);
    }

    public BlockListView(Context context, AttributeSet attribs) {
        this(context, attribs, 0);
    }

    public BlockListView(Context context, AttributeSet attribs, int style) {
        super(context, attribs, style);

        setAdapter(mAdapter);
        addItemDecoration(new ItemSpacingDecoration(mAdapter));
    }

    public void init(WorkspaceHelper helper,
                     BlockTouchHandler touchHandler) {
        mHelper = helper;
        mTouchHandler = touchHandler;

        // Update all currently visible BlockGroups.
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            // ViewHolder's FrameLayout
            FrameLayout container = (FrameLayout) getChildAt(i);
            if (container.getChildCount() > 0) {
                BlockGroup bg = (BlockGroup) container.getChildAt(0);
                bg.setTouchHandler(touchHandler);
            }
        }
    }

    public void setContents(List<Block> blocks) {
        mBlocks.clear();
        mBlocks.addAll(blocks);

        mAdapter.notifyDataSetChanged();
    }

    public void addBlock(Block block) {
        addBlock(mBlocks.size(), block);
    }

    public void addBlock(int insertPostion, Block block) {
        if (insertPostion < 0 || insertPostion > mBlocks.size()) {
            throw new IllegalArgumentException("Invalid position.");
        }
        mBlocks.add(insertPostion, block);
        mAdapter.notifyItemInserted(insertPostion);
    }

    public void removeBlock(Block block) {
        int position = mBlocks.indexOf(block);
        mBlocks.remove(position);
        mAdapter.notifyItemRemoved(position);
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {
        final FrameLayout mContainer;
        BlockGroup bg = null;  // Root of the currently attach block views.

        ViewHolder(Context context) {
            super(new FrameLayout(context));
            mContainer = (FrameLayout) itemView;
        }
    }

    protected class Adapter extends RecyclerView.Adapter<BlockListView.ViewHolder> {
        @Override
        public int getItemCount() {
            return mBlocks.size();
        }

        @Override
        public BlockListView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new BlockListView.ViewHolder(getContext());
        }

        @Override
        public void onBindViewHolder(BlockListView.ViewHolder holder, int position) {
            Block block = mBlocks.get(position);
            BlockGroup bg = mHelper.getParentBlockGroup(block);
            if (bg == null) {
                bg = mHelper.buildBlockGroupTree(block, null, mTouchHandler);
            } else {
                bg.setTouchHandler(mTouchHandler);
            }
            holder.mContainer.addView(bg, new FrameLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            holder.bg = bg;
        }

        @Override
        public void onViewRecycled(BlockListView.ViewHolder holder) {
            // TODO(#77): Reuse views to save memory and allocation time.  E.g., a view pool.
            holder.bg.unlinkModelAndSubViews();
            holder.bg = null;
            holder.mContainer.removeAllViews();

            super.onViewRecycled(holder);
        }
    }
}
