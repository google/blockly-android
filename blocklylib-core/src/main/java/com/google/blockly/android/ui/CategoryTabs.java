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

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.blockly.android.R;
import com.google.blockly.model.BlocklyCategory;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@code CategoryTabs} view shows the list of available {@link BlocklyCategory}s as tabs.
 * <p/>
 * The view can be configured in either {@link #HORIZONTAL} (default) or {@link #VERTICAL}
 * orientation. If there is not enough space, the tabs will scroll in the same direction.
 * <p/>
 * Additionally, the tab labels can be rotated using the {@link Rotation} constants. All tabs will
 * be rotated in the same direction.
 */
public class CategoryTabs extends RecyclerView {
    public static final String TAG = "CategoryTabs";

    public static final int HORIZONTAL = OrientationHelper.HORIZONTAL;
    public static final int VERTICAL = OrientationHelper.VERTICAL;

    private final LinearLayoutManager mLayoutManager;
    private final CategoryAdapter mAdapter;
    protected final List<BlocklyCategory> mCategories = new ArrayList<>();

    protected @Rotation.Enum int mLabelRotation = Rotation.NONE;
    protected boolean mTapSelectedDeselects = false;

    private LabelAdapter mLabelAdapter;
    protected @Nullable CategorySelectorUI.Callback mCallback;
    protected @Nullable
    BlocklyCategory mCurrentCategory;

    public CategoryTabs(Context context) {
        this(context, null, 0);
    }

    public CategoryTabs(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CategoryTabs(Context context, AttributeSet attrs, int style) {
        super(context, attrs, style);

        mLayoutManager = new LinearLayoutManager(context);
        setLayoutManager(mLayoutManager);
        mAdapter = new CategoryAdapter();
        setAdapter(mAdapter);
        setLabelAdapter(new DefaultTabsAdapter());

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.BlocklyCategory,
                0, 0);
        try {
            //noinspection ResourceType
            mLabelRotation = a.getInt(R.styleable.BlocklyCategory_labelRotation, mLabelRotation);
            int orientation = a.getInt(R.styleable.BlocklyCategory_scrollOrientation, VERTICAL);
            mLayoutManager.setOrientation(orientation);
        } finally {
            a.recycle();
        }
    }

    /**
     * Sets the {@link Adapter} responsible for the label views.
     */
    public void setLabelAdapter(LabelAdapter labelAdapter) {
        mLabelAdapter = labelAdapter;
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Sets the {@link CategorySelectorUI.Callback} used by this instance.
     *
     * @param callback The {@link CategorySelectorUI.Callback} for event handling.
     */
    public void setCallback(@Nullable CategorySelectorUI.Callback callback) {
        mCallback = callback;
    }

    /**
     * Sets the orientation in which the tabs will accumulate, which is also the scroll direction
     * when there are more tabs than space allows.
     *
     * @param orientation Either {@link #HORIZONTAL} or {@link #VERTICAL}.
     */
    public void setOrientation(int orientation) {
        mLayoutManager.setOrientation(orientation);
    }

    /**
     * Sets the {@link Rotation} direction constant for the tab labels.
     *
     * @param labelRotation The {@link Rotation} direction constant for the tab labels.
     */
    public void setLabelRotation(@Rotation.Enum int labelRotation) {
        mLabelRotation = labelRotation;
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Sets whether the selected tab will deselect when clicked again.
     *
     * @param tapSelectedDeselects If {@code true}, selected tab will deselect when clicked again.
     */
    public void setTapSelectedDeselects(boolean tapSelectedDeselects) {
        mTapSelectedDeselects = tapSelectedDeselects;
    }

    /**
     * Sets the list of {@link BlocklyCategory}s used to populate the tab labels.
     *
     * @param categories The list of {@link BlocklyCategory}s used to populate the tab labels.
     */
    public void setCategories(List<BlocklyCategory> categories) {
        mCategories.clear();
        mCategories.addAll(categories);
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Sets the currently selected tab. If the tab is not a member of the assigned categories, no
     * tab will render selected.
     *
     * @param category
     */
    public void setSelectedCategory(@Nullable BlocklyCategory category) {
        if (mCurrentCategory == category) {
            return;
        }
        if (mCurrentCategory != null) {
            // Deselect the old tab.
            TabLabelHolder vh = getTabLabelHolder(mCurrentCategory);
            if (vh != null && mLabelAdapter != null) {  // Tab might not be rendered or visible yet.
                // Update style. Don't use notifyItemChanged(..), due to a resulting UI flash.
                mLabelAdapter.onSelectionChanged(
                        vh.mLabel, vh.mCategory, vh.getAdapterPosition(), false);
            }
        }
        mCurrentCategory = category;
        if (mCurrentCategory != null && mLabelAdapter != null) {
            // Select the new tab.
            TabLabelHolder vh = getTabLabelHolder(mCurrentCategory);
            if (vh != null) {  // Tab might not be rendered or visible yet.
                // Update style. Don't use notifyItemChanged(..), due to a resulting UI flash.
                mLabelAdapter.onSelectionChanged(
                        vh.mLabel, vh.mCategory, vh.getAdapterPosition(), true);
            }
        }
    }

    /**
     * @return The currently highlighted category or null.
     */
    public BlocklyCategory getSelectedCategory() {
        return mCurrentCategory;
    }

    public int getTabCount() {
        return mCategories.size();
    }

    private void onCategoryClicked(BlocklyCategory category) {
        if (mCallback != null) {
            mCallback.onCategoryClicked(category);
        }
    }

    private TabLabelHolder getTabLabelHolder(BlocklyCategory category) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; ++i) {
            View child = getChildAt(i);
            TabLabelHolder vh = (TabLabelHolder) child.getTag();
            if (vh != null && vh.mCategory == category) {
                return vh;
            }
        }
        return null;
    }

    private class CategoryAdapter extends RecyclerView.Adapter<TabLabelHolder> {
        @Override
        public int getItemCount() {
            return getTabCount();
        }

        @Override
        public TabLabelHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (mLabelAdapter == null) {
                throw new IllegalStateException("No LabelAdapter assigned.");
            }
            return new TabLabelHolder(mLabelAdapter.onCreateLabel());
        }

        @Override
        public void onBindViewHolder(TabLabelHolder holder, int tabPosition) {
            final BlocklyCategory category = mCategories.get(tabPosition);
            boolean isSelected = (category == mCurrentCategory);
            // These may throw a NPE, but that is an illegal state checked above.
            mLabelAdapter.onBindLabel(holder.mLabel, category, tabPosition);
            mLabelAdapter.onSelectionChanged(holder.mLabel, category, tabPosition, isSelected);
            holder.mCategory = category;
            holder.mRotator.setChildRotation(mLabelRotation);
            holder.mRotator.setTag(holder);  // For getTabLabelHolder() and deselection
            holder.mLabel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View label) {
                    onCategoryClicked(category);
                }
            });
        }

        @Override
        public void onViewRecycled(TabLabelHolder holder) {
            holder.mRotator.setTag(null);  // Remove reference to holder.
            holder.mCategory = null;
            holder.mLabel.setOnClickListener(null);
        }
    }

    /** Manages TextView labels derived from {@link R.layout#default_toolbox_tab}. */
    protected class DefaultTabsAdapter extends CategoryTabs.LabelAdapter {
        @Override
        public View onCreateLabel() {
            return (TextView) LayoutInflater.from(getContext())
                    .inflate(R.layout.default_toolbox_tab, null);
        }

        /**
         * Assigns the category name to the {@link TextView}. Tabs without labels will be assigned
         * the text {@link R.string#blockly_toolbox_default_category_name} ("Blocks" in English).
         *
         * @param labelView The view used as the label.
         * @param category The {@link BlocklyCategory}.
         * @param position The ordering position of the tab.
         */
        @Override
        public void onBindLabel(View labelView, BlocklyCategory category, int position) {
            String labelText = category.getCategoryName();
            if (TextUtils.isEmpty(labelText)) {
                labelText = getContext().getString(R.string.blockly_toolbox_default_category_name);
            }
            ((TextView) labelView).setText(labelText);
        }

    }

    public abstract static class LabelAdapter {
        /**
         * Create a label view for a tab. This view will later be assigned an
         * {@link View.OnClickListener} to handle tab selection and deselection.
         */
        public abstract View onCreateLabel();

        /**
         * Bind a {@link BlocklyCategory} to a label view, with any appropriate styling.
         *
         * @param labelView The tab's label view.
         * @param category The category to bind to.
         * @param position The position of the category in the list of tabs.
         */
        public abstract void onBindLabel(View labelView, BlocklyCategory category, int position);

        /**
         * Called when a label is bound or when clicking results in a selection change. Responsible
         * for updating the view to reflect the new state, including applying the category name.
         * <p/>
         * By default, it calls {@link View#setSelected(boolean)}. Many views and/or styles will
         * handle this appropriately.
         *
         * @param labelView The tab's label view.
         * @param category The category to bind to.
         * @param position The position of the category in the list of tabs.
         * @param isSelected the new selection state.
         */
        public void onSelectionChanged(
                View labelView, BlocklyCategory category, int position, boolean isSelected) {
            labelView.setSelected(isSelected);
        }
    }

    /**
     * ViewHolder for the display name of a category in the toolbox.
     */
    private static class TabLabelHolder extends RecyclerView.ViewHolder {
        public final RotatedViewGroup mRotator;
        public final View mLabel;

        public BlocklyCategory mCategory;

        TabLabelHolder(View label) {
            super(new RotatedViewGroup(label.getContext()));
            mRotator = (RotatedViewGroup) itemView;
            mLabel = label;
            mRotator.addView(mLabel);
        }
    }
}
