/*
 *  Copyright  2015 Google Inc. All Rights Reserved.
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
import android.view.ViewGroup;

import com.google.blockly.model.Block;

import java.util.List;

/**
 * Renders BlockGroups in the toolbox.
 */
public class BlockGroupAdapter extends RecyclerView.Adapter<BlockGroupAdapter.ViewHolder> {
    private final List<Block> mRootBlocks;
    private final WorkspaceHelper mWorkspaceHelper;
    private final Context mContext;

    /**
     * Create an adapter to show block groups in the toolbox.
     *
     * @param rootBlocks All of the blocks/groups of blocks that should be displayed in the toolbox.
     * @param workspaceHelper A {@link WorkspaceHelper} for obtaining {@link BlockView}s.
     * @param context The context of the fragment.
     */
    public BlockGroupAdapter(List<Block> rootBlocks, WorkspaceHelper workspaceHelper,
                             Context context) {
        mRootBlocks = rootBlocks;
        mWorkspaceHelper = workspaceHelper;
        mContext = context;
    }

    @Override
    public BlockGroupAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(new BlockGroup(mContext, mWorkspaceHelper));
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.mBlockGroup.removeAllViews();
        mWorkspaceHelper.obtainBlockView(mContext, mRootBlocks.get(position), holder.mBlockGroup,
                null);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mRootBlocks.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final BlockGroup mBlockGroup;

        public ViewHolder(BlockGroup v) {
            super(v);
            mBlockGroup = v;
        }
    }
}
