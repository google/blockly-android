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
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.blockly.R;
import com.google.blockly.model.Block;
import com.google.blockly.model.ToolboxCategory;

/**
 * Adapter for displaying blocks and categories in the toolbox.
 * Categories contain zero or more blocks and zero or more subcategories.
 * Subcategories are displayed with their name as a clickable title and when clicked they expand to
 * show their contents.  Blocks may be dragged into the workspace.
 * When there are both blocks and subcategories, the subcategories are displayed first.  Multiple
 * subcategories may be expanded at one time.
 */
public class ToolboxAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = "ToolboxAdapter";

    private static final int BLOCK_GROUP_VIEW_TYPE = 0;
    private static final int CATEGORY_VIEW_TYPE = 1;
    private static final int UNKNOWN_VIEW_TYPE = 2;

    private final ToolboxCategory mTopLevelCategory;
    private final WorkspaceHelper mWorkspaceHelper;
    private final WorkspaceHelper.BlockTouchHandler mTouchHandler;
    private final Context mContext;

    /**
     * Create an adapter to show block groups in the toolbox.
     *
     * @param topLevelCategory All of the blocks/groups of blocks that should be displayed in the
     * toolbox.
     * @param workspaceHelper A {@link WorkspaceHelper} for obtaining {@link BlockView} instances.
     * @param context The context of the fragment.
     * @param touchHandler The function to call when a block is touched.
     */
    public ToolboxAdapter(ToolboxCategory topLevelCategory, WorkspaceHelper workspaceHelper,
                          WorkspaceHelper.BlockTouchHandler touchHandler, Context context) {
        mTopLevelCategory = topLevelCategory;
        mWorkspaceHelper = workspaceHelper;
        mContext = context;
        mTouchHandler = touchHandler;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == BLOCK_GROUP_VIEW_TYPE) {
            return new BlockViewHolder(new BlockGroup(mContext, mWorkspaceHelper));
        } else {
            TextView itemView = (TextView) (LayoutInflater.from(mContext))
                    .inflate(R.layout.category_label_view, null, false);
            return new CategoryViewHolder(itemView);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return getItemForPosition(mTopLevelCategory, position).first;
    }

    /**
     * Called by {@link #onBindViewHolder} to figure out what information to bind in each location.
     * {@link ToolboxCategory} instances nest recursively, with each subcategory being either
     * expanded or collapsed.
     *
     * In a given category the subcategories come first, followed by blocks.  The item to display
     * at a given position is either a block or a subcategory's name.
     *
     * @param currentCategory The {@link ToolboxCategory} to search recursively.
     * @param position The position for which an element should be found.
     * @return A pair containing the type of the item and the item itself.
     */
    private Pair<Integer, Object> getItemForPosition(ToolboxCategory currentCategory, int position) {
        int elementNumber = 0;
        // Check all of the subcategories.
        for (int i = 0; i < currentCategory.getSubcategories().size(); i++) {
            ToolboxCategory subcategory = currentCategory.getSubcategories().get(i);
            // Is it the subcategory title?
            if (elementNumber == position) {
                return new Pair<>(CATEGORY_VIEW_TYPE, (Object) subcategory);
            } else { // If not skip past the header for this category
                elementNumber++;
            }
            // Is it in the subcategory?
            if (subcategory.isExpanded()) {
                if (position - elementNumber < subcategory.getCurrentSize()) {
                    return getItemForPosition(subcategory, position - elementNumber);
                } else { // If not, just skip past all of the blocks in the subcategory.
                    elementNumber += subcategory.getCurrentSize();
                }
            }
        }
        // Is it a block in this category?
        int blockPosition = position - elementNumber;
        if (blockPosition >= 0 && blockPosition < currentCategory.getBlocks().size()) {
            return new Pair<>(BLOCK_GROUP_VIEW_TYPE,
                    (Object) currentCategory.getBlocks().get(blockPosition));
        }
        // Wasn't in subcategories or blocks
        return new Pair<>(UNKNOWN_VIEW_TYPE, null);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Pair<Integer, Object> item = getItemForPosition(mTopLevelCategory, position);
        switch (item.first) {
            case BLOCK_GROUP_VIEW_TYPE:
                final BlockViewHolder bvHolder = (BlockViewHolder) holder;
                final Block block = (Block) item.second;
                bvHolder.mBlockGroup.removeAllViews();
                mWorkspaceHelper.buildBlockViewTree(
                        block, bvHolder.mBlockGroup, null, mTouchHandler);
                break;
            case CATEGORY_VIEW_TYPE:
                final CategoryViewHolder cvHolder = (CategoryViewHolder) holder;
                final ToolboxCategory cat = (ToolboxCategory) item.second;
                cvHolder.setCategory(cat);

                cvHolder.mTextView.setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                cat.setExpanded(!cat.isExpanded());
                                ToolboxAdapter.this.notifyDataSetChanged();
                            }
                        });
                break;
            default:
                // Shouldn't get here.  Now what?
        }
    }

    @Override
    public int getItemCount() {
        if (mTopLevelCategory != null) {
            return mTopLevelCategory.getCurrentSize();
        }
        return 0;
    }

    public class BlockViewHolder extends RecyclerView.ViewHolder {
        public final BlockGroup mBlockGroup;

        public BlockViewHolder(BlockGroup v) {
            super(v);
            mBlockGroup = v;
        }
    }

    public class CategoryViewHolder extends RecyclerView.ViewHolder {
        public final TextView mTextView;
        public ToolboxCategory mCategory;

        public CategoryViewHolder(TextView textView) {
            super(textView);
            mTextView = textView;
            mCategory = null;
        }

        public void setCategory(ToolboxCategory newCategory) {
            mCategory = newCategory;
            mTextView.setText(mCategory.getCategoryName());
        }
    }
}
