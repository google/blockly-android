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
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

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
 * Tabbed drawer style UI to show available {@link Block}s to drag into the user interface.
 *
 * TODO(Anm): detail configuration options.
 */
public class ToolboxFragment extends BlockDrawerFragment {
    public static final String ARG_TAB_EDGE = "tabEdge";
    public static final String ARG_ROTATE_TABS = "rotateTabs";

    private static final int DEFAULT_TAB_EDGE = Gravity.TOP;
    private static final boolean DEFAULT_ROTATE_TABS = true;

    protected final Point mTempScreenPosition = new Point();
    protected final WorkspacePoint mTempWorkspacePosition = new WorkspacePoint();

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
        mBlockListView.setBackgroundColor(
                getResources().getColor(R.color.blockly_toolbox_bg, null));
        mCategoryTabs = new CategoryEdgeTabs(getContext());
        mRootView = new ToolboxRoot(getContext());

        mRootView = (FrameLayout) inflater.inflate(R.layout.fragment_toolbox, container, false);
        mBlockListView = (BlockListView) mRootView.findViewById(R.id.blockly_toolbox_blocks);
        mBlockListView.setLayoutManager(createLinearLayoutManager());
        mCategoryTabs =
                (CategoryEdgeTabs) mRootView.findViewById(R.id.blockly_toolbox_category_tabs);
        mCategoryTabs.setListener(new CategoryEdgeTabs.Listener() {
            @Override
            public void onCategorySelected(ToolboxCategory category) {
                setCurrentCategory(category);
            }
        });
        updateViews();

        return mRootView;
    }

    public void setController(BlocklyController controller) {
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
        mCategoryTabs.setTopLevelCategory(topLevelCategory);
        updateViews();

        if (mBlockListView.getVisibility() == View.VISIBLE) {
            List<ToolboxCategory> subcats = topLevelCategory.getSubcategories();
            ToolboxCategory curCategory = subcats.isEmpty() ? topLevelCategory : subcats.get(0);
            mCategoryTabs.setSelectedCategory(curCategory);
            mBlockListView.setContents(curCategory.getBlocks());
        }
    }

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
        if (!mCloseable && mCategoryTabs.getTabCount() < 1) {
            mCategoryTabs.setVisibility(View.GONE);
        } else {
            mCategoryTabs.setVisibility(View.VISIBLE);
            mCategoryTabs.setLayoutParams(buildTabsLayoutParams());
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
     * @return FrameLayout parameters to align the tabs to one edge, "justified" to either the start
     *         (if horizontal) or top (if vertical).
     */
    private FrameLayout.LayoutParams buildTabsLayoutParams() {
        int width = ViewGroup.LayoutParams.WRAP_CONTENT;
        int height = ViewGroup.LayoutParams.WRAP_CONTENT;

        int justification = isTabsHorizontal() ? GravityCompat.START : Gravity.TOP;
        int gravity = mTabEdge | justification;

        return new FrameLayout.LayoutParams(width, height, gravity);
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

    protected class ToolboxRoot extends ViewGroup {
        ToolboxRoot(Context context) {
            super(context);

            // Always add the BlockListView before the tabs
            addView(mBlockListView);
            addView(mCategoryTabs);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);

            measureChild(mCategoryTabs, widthMeasureSpec, heightMeasureSpec);
            mBlocksItemDecorator.
            measureChild(mBlockListView, widthMeasureSpec, heightMeasureSpec);
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            boolean isRtl = ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL;
            int width = right - left;
            int height = bottom - top;
        }
    }

    protected class BlocksItemDecoration extends RecyclerView.ItemDecoration {
        @Override
        public void getItemOffsets(
                Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            mBlockListView.get
            int itemPosition = parent.getChildPosition(child);
            int bottomMargin = (itemPosition == (state.getItemCount() - 1)) ? cardMargin : 0;
            outRect.set(cardMargin, cardMargin, cardMargin, bottomMargin);
        }
    }
}
