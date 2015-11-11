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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.blockly.R;
import com.google.blockly.model.Block;
import com.google.blockly.model.ToolboxCategory;

/**
 * Adapter for displaying blocks and categories in the toolbox.
 */
public class ToolboxAdapter extends RecyclerView.Adapter<ToolboxAdapter.ViewHolder> {
    private static final int BLOCK_GROUP_VIEW_TYPE = 0;
    private static final int CATEGORY_VIEW_TYPE = 1;
    private final ToolboxCategory mTopLevelCategory;
    private final WorkspaceHelper mWorkspaceHelper;
    private final Context mContext;

    /**
     * Create an adapter to show block groups in the toolbox.
     *
     * @param topLevelCategory All of the blocks/groups of blocks that should be displayed in the
     * toolbox.
     * @param workspaceHelper A {@link WorkspaceHelper} for obtaining {@link BlockView}s.
     * @param context The context of the fragment.
     */
    public ToolboxAdapter(ToolboxCategory topLevelCategory, WorkspaceHelper workspaceHelper,
                          Context context) {
        mTopLevelCategory = topLevelCategory;
        mWorkspaceHelper = workspaceHelper;
        mContext = context;
    }

    @Override
    public ToolboxAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
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
        return getItemViewType(mTopLevelCategory, position);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        if (getItemViewType(position) == BLOCK_GROUP_VIEW_TYPE) {
            BlockViewHolder bvHolder = (BlockViewHolder) holder;
            bvHolder.mBlockGroup.removeAllViews();
            mWorkspaceHelper.obtainBlockView(mContext,
                    getBlockForPosition(mTopLevelCategory, position), bvHolder.mBlockGroup, null);
        } else if (getItemViewType(position) == CATEGORY_VIEW_TYPE) {
            final ToolboxCategory cat = getCategoryForPosition(mTopLevelCategory, position);
            CategoryViewHolder cvHolder = (CategoryViewHolder) holder;
            cvHolder.setCategory(cat);

            cvHolder.mTextView.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            cat.setExpanded(!cat.isExpanded());
                            ToolboxAdapter.this.notifyDataSetChanged();
                        }
                    });
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        if (mTopLevelCategory != null) {
            return mTopLevelCategory.getCurrentSize();
        }
        return 0;
    }

    private int getItemViewType(ToolboxCategory currentCategory, int position) {
        int categoryNumber = 0;
        int elementNumber = 0;
        while (elementNumber <= position) {
            if (categoryNumber < currentCategory.getSubcategories().size()) {
                ToolboxCategory subcategory =
                        currentCategory.getSubcategories().get(categoryNumber);
                if (elementNumber == position) {
                    return CATEGORY_VIEW_TYPE;
                }
                if (subcategory.isExpanded()) {
                    elementNumber++; // skip past the header for this category
                    if (position - elementNumber < subcategory.getCurrentSize()) {
                        return getItemViewType(subcategory, position - elementNumber);
                    } else {
                        elementNumber += subcategory.getCurrentSize();
                    }
                } else {
                    elementNumber++;
                }
                categoryNumber++;
            } else if (position - elementNumber >= 0) {
                // Wasn't in subcategories; must be in blocks
                return BLOCK_GROUP_VIEW_TYPE;
            }
        }
        // Wasn't in subcategories or blocks
        return -1;
    }

    private Block getBlockForPosition(ToolboxCategory currentCategory, int position) {
        int categoryNumber = 0;

        int elementNumber = 0;
        while (elementNumber <= position) {
            if (categoryNumber < currentCategory.getSubcategories().size()) {
                ToolboxCategory subcategory =
                        currentCategory.getSubcategories().get(categoryNumber);
                if (subcategory.isExpanded()) {
                    elementNumber++; // skip past the header for this category
                    if (position - elementNumber < subcategory.getCurrentSize()) {
                        return getBlockForPosition(subcategory, position - elementNumber);
                    } else {
                        elementNumber += subcategory.getCurrentSize();
                    }
                } else {
                    elementNumber++;
                }
                categoryNumber++;
            } else if (position - elementNumber >= 0) {
                // Wasn't in subcategories; must be in blocks
                return currentCategory.getBlocks().get(position - elementNumber);
            }
        }
        // Wasn't in subcategories or blocks
        return null;
    }

    private ToolboxCategory getCategoryForPosition(ToolboxCategory currentCategory, int position) {
        int categoryNumber = 0;

        int elementNumber = 0;
        while (elementNumber <= position) {
            if (categoryNumber < currentCategory.getSubcategories().size()) {
                ToolboxCategory subcategory =
                        currentCategory.getSubcategories().get(categoryNumber);
                if (elementNumber == position) {
                    return subcategory;
                } else if (subcategory.isExpanded()) {
                    elementNumber += subcategory.getCurrentSize();
                }
                elementNumber++;    // The header
                categoryNumber++;
            } else if (position - elementNumber >= 0) {
                // Wasn't in subcategories; must be in blocks
                return null;
            }
        }
        // Wasn't in subcategories or blocks
        return null;
    }

    public abstract class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View v) {
            super(v);
        }
    }

    public class BlockViewHolder extends ToolboxAdapter.ViewHolder {
        public final BlockGroup mBlockGroup;

        public BlockViewHolder(BlockGroup v) {
            super(v);
            mBlockGroup = v;
        }
    }

    public class CategoryViewHolder extends ToolboxAdapter.ViewHolder {
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
