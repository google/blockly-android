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

package com.google.blockly;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.google.blockly.control.BlocklyController;
import com.google.blockly.model.Block;
import com.google.blockly.model.ToolboxCategory;
import com.google.blockly.model.WorkspacePoint;
import com.google.blockly.ui.BlockListView;
import com.google.blockly.ui.BlockTouchHandler;
import com.google.blockly.ui.BlockView;
import com.google.blockly.ui.CategoryEdgeTabs;
import com.google.blockly.ui.Rotation;
import com.google.blockly.ui.BlockDrawerFragment;
import com.google.blockly.ui.WorkspaceHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * A tabbed drawer UI to show the available {@link Block}s one can drag into the workspace. The
 * available blocks are divided into drawers by {@link ToolboxCategory}s. Assign the categories
 * using {@link #setContents(ToolboxCategory)}. Each subcategory is represented as a tab, possibly
 * followed by another tab for the top level category. If the only category is the top level
 * category, and the drawer is not closeable, no tab will be shown.
 * <p/>
 * The look of the {@code ToolboxFragment} is highly configurable. It inherits from
 * {@link BlockDrawerFragment}, including the {@code closeable} and {@code scrollOrientation}
 * attributes.  Additionally, it supports configuration for which edge the tab
 * is bound to, and whether to rotate the tab labels when attached to vertical edges.
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
 * When {@code blockly:closeable} is true, the drawer of blocks will hide.  The tabs will always be
 * visible when the fragment is visible, providing the user a way to open the drawers.
 * <p/>
 * {@code blockly:scrollOrientation} can be either {@code horizontal} or {@code vertical}, and
 * affects only the block list. Not the tabs.
 * <p/>
 * {@code blockly:rotateTabs} is a boolean.  If true, the tab labels (text and background) will
 * rotate to counter-clockwise on the left edge, and clockwise on the right edge.  Top and bottom
 * labels will never rotate.
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
 *
 * @attr ref com.google.blockly.R.styleable#BlockDrawerFragment_closeable
 * @attr ref com.google.blockly.R.styleable#BlockDrawerFragment_scrollOrientation
 * @attr ref com.google.blockly.R.styleable#ToolboxFragment_tabEdge
 * @attr ref com.google.blockly.R.styleable#ToolboxFragment_rotateTabs
 */
// TODO(#9): Attribute and arguments to set the tab background.
public class ToolboxFragment extends BlockDrawerFragment {
    private static final String TAG = "ToolboxFragment";

    public static final String ARG_TAB_EDGE = "tabEdge";
    public static final String ARG_ROTATE_TABS = "rotateTabs";

    private static final int DEFAULT_TAB_EDGE = Gravity.TOP;
    private static final boolean DEFAULT_ROTATE_TABS = true;

    protected final Point mTempScreenPosition = new Point();
    protected final WorkspacePoint mTempWorkspacePosition = new WorkspacePoint();
    protected final Rect mScrollablePadding = new Rect();

    protected ToolboxRoot mRootView;
    protected CategoryEdgeTabs mCategoryTabs;

    protected BlocklyController mController;
    protected WorkspaceHelper mHelper;

    private int mTabEdge = DEFAULT_TAB_EDGE;
    private boolean mRotateTabs = DEFAULT_ROTATE_TABS;

    @Override
    public void onInflate(Context context, AttributeSet attrs, Bundle savedInstanceState) {
        super.onInflate(context, attrs, savedInstanceState);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.ToolboxFragment,
                0, 0);
        try {
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
                getResources().getColor(R.color.blockly_toolbox_bg, null));  // Replace with attrib
        mBlockListView.setVisibility(View.GONE);  // Start closed.
        mCategoryTabs = new CategoryEdgeTabs(getContext());
        mRootView = new ToolboxRoot(getContext());

        mCategoryTabs.setListener(new CategoryEdgeTabs.Listener() {
            @Override
            public void onCategorySelected(ToolboxCategory category) {
                setCurrentCategory(category);
            }
        });
        updateViews();

        return mRootView;
    }

    /**
     * Connects the {@link ToolboxFragment} to the application's {@link BlocklyController}.  It is
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
            mHelper = null;
            mBlockListView.setContents(new ArrayList<Block>(0));
            mBlockListView.init(null, null);
            return;
        }

        mHelper = mController.getWorkspaceHelper();
        mBlockListView.init(mHelper, new BlockTouchHandler() {
            @Override
            public boolean onTouchBlock(BlockView blockView, MotionEvent motionEvent) {
                if (motionEvent.getAction() != MotionEvent.ACTION_DOWN) {
                    return false;
                }

                Block copiedModel = blockView.getBlock().getRootBlock().deepCopy();

                // Make the pointer be in the same relative position on the block as it was in the
                // toolbox.
                mTempScreenPosition.set((int) motionEvent.getRawX() - (int) motionEvent.getX(),
                        (int) motionEvent.getRawY() - (int) motionEvent.getY());
                mHelper.screenToWorkspaceCoordinates(mTempScreenPosition, mTempWorkspacePosition);
                copiedModel.setPosition(mTempWorkspacePosition.x, mTempWorkspacePosition.y);
                mController.addBlockFromToolbox(copiedModel, motionEvent);

                if (mCloseable) {
                    closeBlocksDrawer();
                }
                return true;
            }

            @Override
            public boolean onInterceptTouchEvent(BlockView blockView, MotionEvent motionEvent) {
                return false;
            }
        });
    }

    /**
     * Sets the contents that should be displayed in the toolbox.
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
        }
    }

    /**
     * Sets the Toolbox's current {@link ToolboxCategory}, including opening or closing the drawer.
     * In closeable toolboxes, {@code null} {@code category} is equivalent to closing the drawer.
     * Otherwise, the drawer will be rendered empty.
     *
     * @param category The {@link ToolboxCategory} with blocks to display.
     */
    // TODO(#384): Add mBlockList animation hooks for subclasses.
    public void setCurrentCategory(@Nullable ToolboxCategory category) {
        if (category == null) {
            closeBlocksDrawer();
            return;
        }

        mCategoryTabs.setSelectedCategory(category);
        mBlockListView.setVisibility(View.VISIBLE);
        mBlockListView.setContents(category.getBlocks());
    }

    /**
     * Attempts to close the blocks drawer.
     *
     * @return True an action was taken (the drawer is closeable and was previously open).
     */
    // TODO(#384): Add mBlockList animation hooks for subclasses.
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

    @Override
    protected void readArgumentsFromBundle(Bundle bundle) {
        super.readArgumentsFromBundle(bundle);
        if (bundle != null) {
            mTabEdge = bundle.getInt(ARG_TAB_EDGE, mTabEdge);
            mRotateTabs = bundle.getBoolean(ARG_ROTATE_TABS, mRotateTabs);
        }
    }

    /**
     * Update the fragment's views based on the current values of {@link #mCloseable},
     * {@link #mTabEdge}, and {@link #mRotateTabs}.
     */
    protected void updateViews() {
        // If there is only one the drawer is not closeable, we don't need the tab.
        if (!mCloseable && mCategoryTabs.getTabCount() <= 1) {
            mCategoryTabs.setVisibility(View.GONE);
        } else {
            mCategoryTabs.setVisibility(View.VISIBLE);
            mCategoryTabs.setOrientation(isTabsHorizontal() ? CategoryEdgeTabs.HORIZONTAL
                    : CategoryEdgeTabs.VERTICAL);
            mCategoryTabs.setLabelRotation(getLabelRotation());
            mCategoryTabs.setTapSelectedDeselects(mCloseable);
        }

        if (!mCloseable) {
            mBlockListView.setVisibility(View.VISIBLE);
        }  // Otherwise leave it in the current state.
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
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);

            // Measure the tabs before the the block list.
            measureChild(mCategoryTabs, widthMeasureSpec, heightMeasureSpec);
            updateScrollablePadding();
            measureChild(mBlockListView, widthMeasureSpec, heightMeasureSpec);
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            int width = right - left;
            int height = bottom - top;

            mBlockListView.layout(0, 0, width, height);

            int tabMeasuredwidth = mCategoryTabs.getMeasuredWidth();
            int tabMeasuredHeight = mCategoryTabs.getMeasuredHeight();
            int layoutDir = ViewCompat.getLayoutDirection(mRootView);
            switch (GravityCompat.getAbsoluteGravity(mTabEdge, layoutDir)) {
                case Gravity.LEFT:
                case Gravity.TOP:
                    mCategoryTabs.layout(0, 0,
                            Math.min(width, tabMeasuredwidth), Math.min(height, tabMeasuredHeight));
                    break;

                case Gravity.RIGHT:
                    mCategoryTabs.layout(Math.max(0, width - tabMeasuredwidth), 0, width,
                            Math.min(height, tabMeasuredHeight));
                    break;

                case Gravity.BOTTOM:
                    mCategoryTabs.layout(0, Math.max(0, height - tabMeasuredHeight),
                            Math.min(width, tabMeasuredwidth), bottom);
                    break;
            }
        }
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
