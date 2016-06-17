/*
 *  Copyright 2015 Google Inc. All Rights Reserved.
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

package com.google.blockly.android;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.android.ui.BlockGroup;
import com.google.blockly.android.ui.BlockListView;
import com.google.blockly.android.ui.CategoryTabs;
import com.google.blockly.android.ui.Rotation;
import com.google.blockly.android.ui.BlockDrawerFragment;
import com.google.blockly.android.ui.WorkspaceHelper;
import com.google.blockly.model.Block;
import com.google.blockly.model.ToolboxCategory;
import com.google.blockly.model.WorkspacePoint;
import com.google.blockly.utils.ColorUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

/**
 * A tabbed drawer UI to show the available {@link Block}s one can drag into the workspace. The
 * available blocks are divided into drawers by {@link ToolboxCategory}s. Assign the categories
 * using {@link #setContents(ToolboxCategory)}. This top level category can contain either a list of
 * blocks or a list of subcategories, but not both. If it has blocks, the {@code ToolboxFragment}
 * renders as a single tab/group. If it has subcategories, it will render each subcategory with its
 * own tab. If there is only one category (top level or subcategory) and the fragment is not
 * closeable, no tab will render with the list of blocks.
 * <p/>
 * The look of the {@code ToolboxFragment} is highly configurable. It inherits from
 * {@link BlockDrawerFragment}, including the {@code closeable} and {@code scrollOrientation}
 * attributes. Additionally, it supports configuration for which edge the tab is bound to, and
 * whether to rotate the tab labels when attached to vertical edges.
 * <p/>
 * For example:
 * <blockquote><pre>
 * &lt;fragment
 *     xmlns:android="http://schemas.android.com/apk/res/android"
 *     xmlns:blockly="http://schemas.android.com/apk/res-auto"
 *     android:name="com.google.blockly.ToolboxFragment"
 *     android:id="@+id/blockly_toolbox"
 *     android:layout_width="wrap_content"
 *     android:layout_height="match_parent"
 *     <b>blockly:closeable</b>="true"
 *     <b>blockly:scrollOrientation</b>="vertical"
 *     <b>blockly:tabEdge</b>="start"
 *     <b>blockly:rotateTabs</b>="true"
 *     /&gt;
 * </pre></blockquote>
 * <p/>
 * When {@code blockly:closeable} is true, the drawer of blocks will hide in the closed state. The
 * tabs will remain visible, providing the user a way to open the drawers.
 * <p/>
 * {@code blockly:scrollOrientation} can be either {@code horizontal} or {@code vertical}, and
 * affects only the block list. The tab scroll orientation is determined by the {@code tabEdge}.
 * <p/>
 * {@code blockly:rotateTabs} is a boolean. If true, the tab labels (text and background) will
 * rotate counter-clockwise on the left edge, and clockwise on the right edge. Top and bottom edge
 * tabs will never rotate.
 * <p/>
 * {@code blockly:tabEdge} takes the following values:
 * <table>
 *     <tr><th>XML attribute {@code blockly:tabEdge}</th><th>Fragment argument {@link #ARG_TAB_EDGE}</th></tr>
 *     <tr><td>{@code top}</td><td>{@link Gravity#TOP}</td><td>The top edge, with tabs justified to the start.  The default.</td></tr>
 *     <tr><td>{@code bottom}</td><td>{@link Gravity#BOTTOM}</td><td>The bottom edge, justified to the start.</td></tr>
 *     <tr><td>{@code left}</td><td>{@link Gravity#LEFT}</td><td>Left edge, justified to the top.</td></tr>
 *     <tr><td>{@code right}</td><td>{@link Gravity#RIGHT}</td><td>Right edge, justified to the top.</td></tr>
 *     <tr><td>{@code start}</td><td>{@link GravityCompat#START}</td><td>Starting edge (left in left-to-right), justified to the top.</td></tr>
 *     <tr><td>{@code end}</td><td>{@link GravityCompat#END}</td><td>Ending edge (right in left-to-right), justified to the top.</td></tr>
 * </table>
 * If there are more tabs than space allows, the tabs will be scrollable by dragging along the edge.
 * If this behavior is required, make sure the space is not draggable by other views, such as a
 * DrawerLayout.
 * <p/>
 * Developers can further customize the tab look by overriding {@link #onCreateLabelAdapter()} and
 * providing their own {@link CategoryTabs.LabelAdapter}.
 *
 * @attr ref com.google.blockly.R.styleable#BlockDrawerFragment_closeable
 * @attr ref com.google.blockly.R.styleable#BlockDrawerFragment_scrollOrientation
 * @attr ref com.google.blockly.R.styleable#ToolboxFragment_tabEdge
 * @attr ref com.google.blockly.R.styleable#ToolboxFragment_rotateTabs
 */
// TODO(#9): Attribute and arguments to set the tab background.
public class ToolboxFragment extends BlockDrawerFragment {
    private static final String TAG = "ToolboxFragment";

    protected static final float BLOCKS_BACKGROUND_LIGHTNESS = 0.75f;
    protected static final int DEFAULT_BLOCKS_BACKGROUND_ALPHA = 0xBB;
    protected static final int DEFAULT_BLOCKS_BACKGROUND_COLOR = Color.LTGRAY;

    public static final String ARG_TAB_EDGE = "ToolboxFragment_tabEdge";
    public static final String ARG_ROTATE_TABS = "ToolboxFragment_rotateTabs";

    public static final int DEFAULT_TAB_EDGE = Gravity.TOP;
    public static final boolean DEFAULT_ROTATE_TABS = true;

    /** Subset of Gravity to identify the edge the category tabs should be bound to. */
    @IntDef(flag=true, value={
            Gravity.TOP,
            Gravity.LEFT,
            Gravity.BOTTOM,
            Gravity.RIGHT,
            GravityCompat.START,
            GravityCompat.END
    })
    @Retention(RetentionPolicy.SOURCE)
    private @interface EdgeEnum {}

    protected final Rect mScrollablePadding = new Rect();

    protected ToolboxRoot mRootView;
    protected CategoryTabs mCategoryTabs;

    protected BlocklyController mController;
    protected WorkspaceHelper mHelper;

    private @EdgeEnum int mTabEdge = DEFAULT_TAB_EDGE;
    private boolean mRotateTabs = DEFAULT_ROTATE_TABS;

    private BlockListView.OnDragListBlock mDragHandler = new BlockListView.OnDragListBlock() {
        @Override
        public BlockGroup getDraggableBlockGroup(int index, Block blockInList,
                                                 WorkspacePoint initialBlockPosition) {
            Block copy = blockInList.deepCopy();
            copy.setPosition(initialBlockPosition.x, initialBlockPosition.y);
            BlockGroup copyView = mController.addRootBlock(copy);
            if (mCloseable) {
                closeBlocksDrawer();
            }
            return copyView;
        }
    };

    @Override
    public void onInflate(Context context, AttributeSet attrs, Bundle savedInstanceState) {
        super.onInflate(context, attrs, savedInstanceState);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.ToolboxFragment,
                0, 0);
        try {
            //noinspection ResourceType
            mTabEdge = a.getInt(R.styleable.ToolboxFragment_tabEdge, DEFAULT_TAB_EDGE);
            mRotateTabs = a.getBoolean(R.styleable.ToolboxFragment_rotateTabs, DEFAULT_ROTATE_TABS);
        } finally {
            a.recycle();
        }

        // Store values in arguments, so fragment resume works (no inflation during resume).
        Bundle args = getArguments();
        if (args == null) {
            setArguments(args = new Bundle());
        }
        args.putInt(ARG_TAB_EDGE, mTabEdge);
        args.putBoolean(ARG_ROTATE_TABS, mRotateTabs);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Read configure
        readArgumentsFromBundle(getArguments());
        readArgumentsFromBundle(savedInstanceState);  // Overwrite initial state with stored state.

        mBlockListView = new BlockListView(getContext());
        mBlockListView.setLayoutManager(createLinearLayoutManager());
        mBlockListView.addItemDecoration(new BlocksItemDecoration());
        mBlockListView.setBackgroundColor(
                getResources().getColor(R.color.blockly_toolbox_bg));  // Replace with attrib
        mCategoryTabs = new CategoryTabs(getContext());
        mCategoryTabs.setLabelAdapter(onCreateLabelAdapter());
        mCategoryTabs.setCallback(new CategoryTabs.Callback() {
            @Override
            public void onCategorySelected(ToolboxCategory category) {
                setCurrentCategory(category);
            }
        });
        mRootView = new ToolboxRoot(getContext());
        updateViews();

        return mRootView;
    }

    /**
     * Connects the {@link ToolboxFragment} to the application's {@link BlocklyController}. It is
     * called by {@link BlocklyController#setToolboxFragment(ToolboxFragment)} and should not be
     * called by the application developer.
     *
     * @param controller The application's {@link BlocklyController}.
     */
    public void setController(BlocklyController controller) {
        if (mController != null && mController.getToolboxFragment() != this) {
            throw new IllegalStateException("Call BlockController.setToolboxFragment(..) instead of"
                    + " ToolboxFragment.setController(..).");
        }

        mController = controller;
        if (mController == null) {
            mBlockListView.setContents(new ArrayList<Block>(0));
        }
        mBlockListView.init(mController, mDragHandler);
    }

    /**
     * Sets the top level category used to populate the toolbox. This top level category can contain
     * either a list of blocks or a list of subcategories, but not both. If it has blocks, the
     * {@code ToolboxFragment} renders as a single tab/group. If it has subcategories, it will
     * render each subcategory with its own tab.
     *
     * @param topLevelCategory The top-level category in the toolbox.
     */
    public void setContents(final ToolboxCategory topLevelCategory) {
        List<Block> blocks = topLevelCategory.getBlocks();
        List<ToolboxCategory> subcats = topLevelCategory.getSubcategories();

        if (!blocks.isEmpty() && !subcats.isEmpty()) {
            throw new IllegalArgumentException(
                    "Toolbox cannot have both blocks and categories in the root level.");
        }

        if (blocks.isEmpty()) {
            mCategoryTabs.setCategories(subcats);
        } else {
            List<ToolboxCategory> singleCategory = new ArrayList<>(1);
            singleCategory.add(topLevelCategory);
            mCategoryTabs.setCategories(singleCategory);
        }
        updateViews();

        if (mBlockListView.getVisibility() == View.VISIBLE) {
            ToolboxCategory curCategory = subcats.isEmpty() ? topLevelCategory : subcats.get(0);
            mCategoryTabs.setSelectedCategory(curCategory);
            mBlockListView.setContents(curCategory.getBlocks());

            updateCategoryColors(curCategory);
        }
    }

    /**
     * Sets the Toolbox's current {@link ToolboxCategory}, including opening or closing the drawer.
     * In closeable toolboxes, {@code null} {@code category} is equivalent to closing the drawer.
     * Otherwise, the drawer will be rendered empty.
     *
     * @param category The {@link ToolboxCategory} with blocks to display.
     */
    // TODO(#80): Add mBlockList animation hooks for subclasses.
    public void setCurrentCategory(@Nullable ToolboxCategory category) {
        if (category == null) {
            closeBlocksDrawer();
            return;
        }

        mCategoryTabs.setSelectedCategory(category);
        updateCategoryColors(category);
        mBlockListView.setVisibility(View.VISIBLE);
        mBlockListView.setContents(category.getBlocks());
    }

    /**
     * Attempts to close the blocks drawer.
     *
     * @return True if an action was taken (the drawer is closeable and was previously open).
     */
    // TODO(#80): Add mBlockList animation hooks for subclasses.
    public boolean closeBlocksDrawer() {
        if (mCloseable && mBlockListView.getVisibility() == View.VISIBLE) {
            mBlockListView.setVisibility(View.GONE);
            mCategoryTabs.setSelectedCategory(null);
            return true;
        }
        return false;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ARG_TAB_EDGE, mTabEdge);
        outState.putBoolean(ARG_ROTATE_TABS, mRotateTabs);
    }

    protected CategoryTabs.LabelAdapter onCreateLabelAdapter() {
        return new DefaultTabsAdapter();
    }

    @Override
    protected void readArgumentsFromBundle(Bundle bundle) {
        super.readArgumentsFromBundle(bundle);
        if (bundle != null) {
            //noinspection ResourceType
            mTabEdge = bundle.getInt(ARG_TAB_EDGE, mTabEdge);
            mRotateTabs = bundle.getBoolean(ARG_ROTATE_TABS, mRotateTabs);
        }
    }

    /**
     * Update the fragment's views based on the current values of {@link #mCloseable},
     * {@link #mTabEdge}, and {@link #mRotateTabs}.
     */
    protected void updateViews() {
        // If there is only one drawer and the drawer is not closeable, we don't need the tab.
        if (!mCloseable && mCategoryTabs.getTabCount() <= 1) {
            mCategoryTabs.setVisibility(View.GONE);
        } else {
            mCategoryTabs.setVisibility(View.VISIBLE);
            mCategoryTabs.setOrientation(isTabsHorizontal() ? CategoryTabs.HORIZONTAL
                    : CategoryTabs.VERTICAL);
            mCategoryTabs.setLabelRotation(getLabelRotation());
            mCategoryTabs.setTapSelectedDeselects(mCloseable);
        }

        if (!mCloseable) {
            mBlockListView.setVisibility(View.VISIBLE);
        }  // Otherwise leave it in the current state.
    }

    protected void updateCategoryColors(ToolboxCategory curCategory) {
        Integer maybeColor = curCategory.getColor();
        int bgColor = DEFAULT_BLOCKS_BACKGROUND_COLOR;
        if (maybeColor != null) {
            bgColor = getBackgroundColor(maybeColor);
        }
        int alphaBgColor = Color.argb(
                mCloseable ? DEFAULT_BLOCKS_BACKGROUND_ALPHA : ColorUtils.ALPHA_OPAQUE,
                Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor));
        mBlockListView.setBackgroundColor(alphaBgColor);
    }

    protected int getBackgroundColor(int categoryColor) {
        return ColorUtils.blendRGB(categoryColor, Color.WHITE, BLOCKS_BACKGROUND_LIGHTNESS);
    }

    /**
     * @return True if {@link #mTabEdge} is {@link Gravity#TOP} or {@link Gravity#BOTTOM}.
     */
    protected boolean isTabsHorizontal() {
        return (mTabEdge == Gravity.TOP || mTabEdge == Gravity.BOTTOM);
    }

    /**
     * Updates the padding used to calculate the margins of the scrollable blocks, based on the size
     * and placement of the tabs.
     */
    private void updateScrollablePadding() {
        int layoutDir = ViewCompat.getLayoutDirection(mRootView);
        switch (GravityCompat.getAbsoluteGravity(mTabEdge, layoutDir)) {
            case Gravity.LEFT:
                mScrollablePadding.set(mCategoryTabs.getMeasuredWidth(), 0, 0, 0);
                break;
            case Gravity.TOP:
                mScrollablePadding.set(0, mCategoryTabs.getMeasuredHeight(), 0, 0);
                break;
            case Gravity.RIGHT:
                mScrollablePadding.set(0, 0, mCategoryTabs.getMeasuredWidth(), 0);
                break;
            case Gravity.BOTTOM:
                mScrollablePadding.set(0, 0, 0, mCategoryTabs.getMeasuredHeight());
                break;
        }
    }

    /**
     * @return Computed {@link Rotation} constant for {@link #mRotateTabs} and {@link #mTabEdge}.
     */
    @Rotation.Enum
    private int getLabelRotation() {
        if (!mRotateTabs) {
            return Rotation.NONE;
        }
        switch (mTabEdge) {
            case Gravity.LEFT:
                return Rotation.COUNTER_CLOCKWISE;
            case Gravity.RIGHT:
                return Rotation.CLOCKWISE;
            case Gravity.TOP:
                return Rotation.NONE;
            case Gravity.BOTTOM:
                return Rotation.NONE;
            case GravityCompat.START:
                return Rotation.ADAPTIVE_COUNTER_CLOCKWISE;
            case GravityCompat.END:
                return Rotation.ADAPTIVE_CLOCKWISE;
            default:
                throw new IllegalArgumentException("Invalid tabEdge: " + mTabEdge);
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
         * @param category The {@link ToolboxCategory}.
         * @param position The ordering position of the tab.
         */
        @Override
        public void onBindLabel(View labelView, ToolboxCategory category, int position) {
            String labelText = category.getCategoryName();
            if (TextUtils.isEmpty(labelText)) {
                labelText = getContext().getString(R.string.blockly_toolbox_default_category_name);
            }
            ((TextView) labelView).setText(labelText);
        }

    }

    /**
     * A custom view to manage the measure and draw order of the tabs and blocks drawer.
     */
    private class ToolboxRoot extends ViewGroup {
        ToolboxRoot(Context context) {
            super(context);

            // Always add the BlockListView before the tabs, for draw order.
            addView(mBlockListView);
            addView(mCategoryTabs);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            // Measure the tabs before the the block list.
            measureChild(mCategoryTabs, widthMeasureSpec, heightMeasureSpec);
            updateScrollablePadding();
            measureChild(mBlockListView, widthMeasureSpec, heightMeasureSpec);

            int listWidth = mBlockListView.getVisibility() == View.GONE ?
                    0 : mBlockListView.getMeasuredWidth();
            int listHeight = mBlockListView.getVisibility() == View.GONE ?
                    0 : mBlockListView.getMeasuredHeight();
            int width = Math.max(mCategoryTabs.getMeasuredWidth(), listWidth);
            int height = Math.max(mCategoryTabs.getMeasuredHeight(), listHeight);
            width = getSizeForSpec(widthMeasureSpec, width);
            height = getSizeForSpec(heightMeasureSpec, height);

            setMeasuredDimension(width, height);
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            int width = right - left;
            int height = bottom - top;

            mBlockListView.layout(0, 0, width, height);

            int tabMeasuredWidth = mCategoryTabs.getMeasuredWidth();
            int tabMeasuredHeight = mCategoryTabs.getMeasuredHeight();
            int layoutDir = ViewCompat.getLayoutDirection(mRootView);
            switch (GravityCompat.getAbsoluteGravity(mTabEdge, layoutDir)) {
                case Gravity.LEFT:
                case Gravity.TOP:
                    mCategoryTabs.layout(0, 0,
                            Math.min(width, tabMeasuredWidth), Math.min(height, tabMeasuredHeight));
                    break;
                case Gravity.RIGHT:
                    mCategoryTabs.layout(Math.max(0, width - tabMeasuredWidth), 0, width,
                            Math.min(height, tabMeasuredHeight));
                    break;
                case Gravity.BOTTOM:
                    mCategoryTabs.layout(0, Math.max(0, height - tabMeasuredHeight),
                            Math.min(width, tabMeasuredWidth), bottom);
                    break;
            }
        }
    }

    private int getSizeForSpec(int measureSpec, int desiredSize) {
        int mode = View.MeasureSpec.getMode(measureSpec);
        int size = View.MeasureSpec.getSize(measureSpec);

        switch (mode) {
            case View.MeasureSpec.AT_MOST:
                return Math.min(size, desiredSize);
            case View.MeasureSpec.EXACTLY:
                return size;
            case View.MeasureSpec.UNSPECIFIED:
                return desiredSize;
        }
        return desiredSize;
    }

    /**
     * {@link RecyclerView.ItemDecoration} to assign padding to block items, to avoid initial
     * overlap with the tabs.
     */
    private class BlocksItemDecoration extends RecyclerView.ItemDecoration {
        @Override
        public void getItemOffsets(
                Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            boolean ltr = ViewCompat.getLayoutDirection(parent) == ViewCompat.LAYOUT_DIRECTION_LTR;
            int itemCount = state.getItemCount();
            int itemIndex = parent.getChildAdapterPosition(view);

            boolean isFirst = (itemIndex == 0);
            boolean isLast = (itemIndex == itemCount - 1);
            int scrollDirection =
                    ((LinearLayoutManager) parent.getLayoutManager()).getOrientation();

            switch (scrollDirection) {
                case LinearLayoutManager.HORIZONTAL: {
                    boolean leftmost = ltr ? isFirst : isLast;
                    boolean rightmost = ltr ? isLast : isFirst;
                    outRect.set(leftmost ? mScrollablePadding.left: 0, mScrollablePadding.top,
                            rightmost ? mScrollablePadding.right : 0, mScrollablePadding.bottom);
                    break;
                }
                case LinearLayoutManager.VERTICAL: {
                    boolean topmost = ltr ? isFirst : isLast;
                    boolean bottommost = ltr ? isLast : isFirst;
                    outRect.set(mScrollablePadding.left, topmost ? mScrollablePadding.top : 0,
                            mScrollablePadding.right, bottommost ? mScrollablePadding.bottom : 0);
                    break;
                }
            }
        }
    }
}
