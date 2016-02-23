package com.google.blockly.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.blockly.R;
import com.google.blockly.model.ToolboxCategory;

// Solves https://code.google.com/p/android/issues/detail?id=74772 until the fix is released.
import org.solovyev.android.views.llm.LinearLayoutManager;

import java.util.List;

/**
 * A view of category tabs, that are assumed to aligned to one of the edges of a container.
 */
public class CategoryEdgeTabs extends RecyclerView {
    public static final String TAG = "CategoryEdgeTabs";

    public static final int HORIZONTAL = LinearLayoutManager.HORIZONTAL;
    public static final int VERTICAL = LinearLayoutManager.VERTICAL;

    public interface Listener {
        void onCategorySelected(ToolboxCategory category);
    }

    private final LinearLayoutManager mLayoutManager;
    private final CategoryAdapter mAdapter;

    protected @Rotation.Enum int mLabelRotation = Rotation.NONE;
    protected boolean mTapSelectedDeselects = false;

    protected Listener mListener;
    protected ToolboxCategory mTopLevelCategory;
    protected ToolboxCategory mCurrentCategory;

    public CategoryEdgeTabs(Context context) {
        this(context, null, 0);
    }

    public CategoryEdgeTabs(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CategoryEdgeTabs(Context context, AttributeSet attrs, int style) {
        super(context, attrs, style);

        mLayoutManager = new LinearLayoutManager(context);
        setLayoutManager(mLayoutManager);
        mAdapter = new CategoryAdapter();
        setAdapter(mAdapter);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.ToolboxFragment,
                0, 0);
        try {
            //noinspection ResourceType
            mLabelRotation = a.getInt(R.styleable.CategoryEdgeTabs_labelRotation, mLabelRotation);
        } finally {
            a.recycle();
        }
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    /**
     * @param orientation Either {@link #HORIZONTAL} or {@link #VERTICAL}.
     */
    public void setOrientation(int orientation) {
        mLayoutManager.setOrientation(orientation);
    }

    public void setLabelRotation(@Rotation.Enum int labelRotation) {
        mLabelRotation = labelRotation;
        mAdapter.notifyDataSetChanged();
    }

    public void setTapSelectedDeselects(boolean tapSelectedDeselects) {
        mTapSelectedDeselects = tapSelectedDeselects;
    }

    public void setTopLevelCategory(ToolboxCategory category) {
        mTopLevelCategory = category;
        mAdapter.notifyDataSetChanged();
    }

    public void setSelectedCategory(ToolboxCategory category) {
        if (mCurrentCategory == category) {
            return;
        }
        if (mCurrentCategory != null) {
            TabLabelHolder vh = getTabLabelHolder(mCurrentCategory);
            if (vh != null) {  // Tab might not be rendered or visible yet.
                markSelection(vh.mLabel, vh.mCategory, vh.getAdapterPosition(), false);
            }
        }
        mCurrentCategory = category;
        if (mCurrentCategory != null) {
            TabLabelHolder vh = getTabLabelHolder(mCurrentCategory);
            if (vh != null) {  // Tab might not be rendered or visible yet.
                markSelection(vh.mLabel, vh.mCategory, vh.getAdapterPosition(), true);
            }
        }
    }

    public int getTabCount() {
        if (mTopLevelCategory == null) {
            return 0;
        }

        int subcategoryCount = mTopLevelCategory.getSubcategories().size();
        boolean showTopLevelTab =
                (subcategoryCount == 0) || !mTopLevelCategory.getBlocks().isEmpty();
        return (showTopLevelTab ? subcategoryCount + 1 : subcategoryCount);
    }

    protected int getTabPosition(ToolboxCategory category) {
        if (category == null || mTopLevelCategory == null) {
            return -1;  // Not found.
        }
        List<ToolboxCategory> subcats = mTopLevelCategory.getSubcategories();
        if (!mTopLevelCategory.isEmpty() && category == mTopLevelCategory) {
            // Top Level Category is always listed last.
            return subcats.size();
        }
        return subcats.indexOf(category);
    }

    protected TabLabelHolder getTabLabelHolder(ToolboxCategory category) {
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

    /**
     * Hook for subclasses to construct or inflate a {@link TextView} to use as a category tab label.
     */
    protected TextView onCreateLabel() {
        return (TextView) LayoutInflater.from(
                getContext()).inflate(R.layout.default_toolbox_tab,null);
    }

    /**
     * Hook for subclasses to bind and style specific category labels.  Responsible for
     *
     * @param labelView The viw used as the label.
     * @param category The {@link ToolboxCategory}.
     * @param position The ordering position of the tab.
     */
    protected void bindAndStyleLabel(
            final TextView labelView, ToolboxCategory category, final int position) {

        String labelText = category.getCategoryName();
        if (TextUtils.isEmpty(labelText)) {
            labelText = getContext().getString(R.string.blockly_toolbox_default_category_name);
        }
        labelView.setText(labelText);
    }

    /**
     * Hook for subclasses to update the style of a category labels based on selected status.
     *
     * @param labelView The viw used as the label.
     * @param category The {@link ToolboxCategory}.
     * @param position The ordering position of the tab.
     * @param isSelected True if the tab represents the currently open tab.
     */
    protected void markSelection(
            TextView labelView, ToolboxCategory category, int position, boolean isSelected) {
        labelView.setSelected(isSelected);
    }

    private void onCategoryLabelClicked(
            TextView label, @Nullable ToolboxCategory category, int tabPosition) {

        if (category == mCurrentCategory) {
            if (mTapSelectedDeselects) {
                setSelectedCategory(null);
                markSelection(label, category, tabPosition, false);
                fireOnCategorySelection(null);
            }
        } else {
            setSelectedCategory(category);
            markSelection(label, category, tabPosition, true);
            fireOnCategorySelection(category);
        }
    }

    private void fireOnCategorySelection(@Nullable ToolboxCategory category) {
        if (mListener != null) {
            mListener.onCategorySelected(category);
        }
    }

    private static class TabLabelHolder extends RecyclerView.ViewHolder {
        public final RotatedViewGroup mRotator;
        public final TextView mLabel;

        public ToolboxCategory mCategory;

        TabLabelHolder(TextView label) {
            super(new RotatedViewGroup(label.getContext()));
            mRotator = (RotatedViewGroup) itemView;
            mLabel = label;
            mRotator.addView(mLabel);
        }
    }

    private class CategoryAdapter extends RecyclerView.Adapter<TabLabelHolder> {
        @Override
        public int getItemCount() {
            return getTabCount();
        }

        @Override
        public TabLabelHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new TabLabelHolder(onCreateLabel());
        }

        @Override
        public void onBindViewHolder(TabLabelHolder holder, final int tabPosition) {
            boolean isTopLevel = (tabPosition == mTopLevelCategory.getSubcategories().size());
            final ToolboxCategory category = isTopLevel ? mTopLevelCategory
                    : mTopLevelCategory.getSubcategories().get(tabPosition);
            boolean isSelected = (category == mCurrentCategory);
            bindAndStyleLabel(holder.mLabel, category, tabPosition);
            markSelection(holder.mLabel, category, tabPosition, isSelected);
            holder.mCategory = category;
            holder.mRotator.setChildRotation(mLabelRotation);
            holder.mRotator.setTag(holder);  // For getTabLabelHolder() and deselection
            holder.mLabel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View label) {
                    onCategoryLabelClicked((TextView) label, category, tabPosition);
                }
            });
        }

        @Override
        public void onViewDetachedFromWindow(TabLabelHolder holder) {
            holder.mRotator.setTag(null);  // Remove reference to holder.
            holder.mCategory = null;
            super.onViewDetachedFromWindow(holder);
        }
    }
}
