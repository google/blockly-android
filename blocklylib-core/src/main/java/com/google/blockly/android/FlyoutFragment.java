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

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.android.ui.BlockGroup;
import com.google.blockly.android.ui.BlockListView;
import com.google.blockly.android.ui.CategoryTabs;
import com.google.blockly.android.ui.BlockDrawerFragment;
import com.google.blockly.android.ui.FlyoutView;
import com.google.blockly.android.ui.WorkspaceHelper;
import com.google.blockly.model.Block;
import com.google.blockly.model.ToolboxCategory;
import com.google.blockly.model.WorkspacePoint;

/**
 * A tabbed drawer UI to show the available {@link Block}s one can drag into the workspace. The
 * available blocks are divided into drawers by {@link ToolboxCategory}s. Assign the categories
 * using {@link #setContents(ToolboxCategory)}. This top level category can contain either a list of
 * blocks or a list of subcategories, but not both. If it has blocks, the {@code FlyoutFragment}
 * renders as a single tab/group. If it has subcategories, it will render each subcategory with its
 * own tab. If there is only one category (top level or subcategory) and the fragment is not
 * closeable, no tab will render with the list of blocks.
 * <p/>
 * The look of the {@code FlyoutFragment} is highly configurable. It inherits from
 * {@link BlockDrawerFragment}, including the {@code closeable} and {@code scrollOrientation}
 * attributes. Additionally, it supports configuration for which edge the tab is bound to, and
 * whether to rotate the tab labels when attached to vertical edges.
 * <p/>
 * For example:
 * <blockquote><pre>
 * &lt;fragment
 *     xmlns:android="http://schemas.android.com/apk/res/android"
 *     xmlns:blockly="http://schemas.android.com/apk/res-auto"
 *     android:name="com.google.blockly.FlyoutFragment"
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
 * If there are more tabs than space allows, the tabs will be scrollable by dragging along the edge.
 * If this behavior is required, make sure the space is not draggable by other views, such as a
 * DrawerLayout.
 * <p/>
 * Developers can further customize the toolbox by providing their own ToolboxView.
 *
 * @attr ref com.google.blockly.R.styleable#BlockDrawerFragment_closeable
 * @attr ref com.google.blockly.R.styleable#BlockDrawerFragment_scrollOrientation
 * @attr ref com.google.blockly.R.styleable#ToolboxFragment_tabEdge
 * @attr ref com.google.blockly.R.styleable#ToolboxFragment_rotateTabs
 */
// TODO(#9): Attribute and arguments to set the tab background.
public class FlyoutFragment extends Fragment {
    private static final String TAG = "FlyoutFragment";

    protected FlyoutView mFlyoutView;
    protected BlocklyController mController;
    protected WorkspaceHelper mHelper;

    private BlockListView.OnDragListBlock mDragHandler = new BlockListView.OnDragListBlock() {
        @Override
        public BlockGroup getDraggableBlockGroup(int index, Block blockInList,
                                                 WorkspacePoint initialBlockPosition) {
            Block copy = blockInList.deepCopy();
            copy.setPosition(initialBlockPosition.x, initialBlockPosition.y);
            BlockGroup copyView = mController.addRootBlock(copy);
            if (mFlyoutView.isCloseable()) {
                closeBlocksDrawer();
            }
            return copyView;
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // TODO replace with lookup in onFinishInflate
        mFlyoutView = onCreateToolboxView(inflater, savedInstanceState);

        mFlyoutView.setOnActionClickListener(new FlyoutView.OnActionClickListener() {
            @Override
            public void onActionClicked(View v, ToolboxCategory category) {
                if (category.isVariableCategory() && mController != null) {
                    mController.requestAddVariable("item");
                }
            }
        });

        mFlyoutView.setCategoryTabsCallback(new CategoryTabs.Callback() {
            @Override
            public void onCategorySelected(ToolboxCategory category) {
                mFlyoutView.setCurrentCategory(category);
            }
        });
        return mFlyoutView;
    }

    /**
     * Connects the {@link FlyoutFragment} to the application's {@link BlocklyController}. It is
     * called by {@link BlocklyController#setToolboxFragment(FlyoutFragment)} and should not be
     * called by the application developer.
     *
     * @param controller The application's {@link BlocklyController}.
     */
    public void setController(BlocklyController controller) {
        if (mController != null && mController.getToolboxFragment() != this) {
            throw new IllegalStateException("Call BlockController.setToolboxFragment(..) instead of"
                    + " FlyoutFragment.setController(..).");
        }

        mController = controller;
        if (mController == null) {
            mFlyoutView.reset();
        }
        mFlyoutView.init(mController, mDragHandler);
        BlockListView blv = mFlyoutView.getBlockListView();
        blv.init(mController, mDragHandler);
    }

    /**
     * Sets the top level category used to populate the toolbox. This top level category can contain
     * either a list of blocks or a list of subcategories, but not both. If it has blocks, the
     * {@code FlyoutFragment} renders as a single tab/group. If it has subcategories, it will
     * render each subcategory with its own tab.
     *
     * @param topLevelCategory The top-level category in the toolbox.
     */
    public void setContents(final ToolboxCategory topLevelCategory) {
        mFlyoutView.setContents(topLevelCategory);
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
        mFlyoutView.setCurrentCategory(category);
    }

    public boolean isCloseable() {
        return mFlyoutView.isCloseable();
    }

    /**
     * Attempts to close the blocks drawer.
     *
     * @return True if an action was taken (the drawer is closeable and was previously open).
     */
    // TODO(#80): Add mBlockList animation hooks for subclasses.
    public boolean closeBlocksDrawer() {
        return mFlyoutView.setCurrentCategory(null);
    }

    /**
     * Create the {@link FlyoutView} to be used as the base view for this class. Typically, this
     * just needs to inflate the appropriate layout and return it, but custom configuration may
     * also be done.
     *
     * @param inflater The inflater to use for loading a layout.
     * @param savedInstanceState Any saved state for the fragment.
     * @return
     */
    protected FlyoutView onCreateToolboxView(LayoutInflater inflater, Bundle savedInstanceState) {
        mFlyoutView = (FlyoutView) inflater.inflate(R.layout.default_toolbox_start, null);
        return mFlyoutView;
    }
}
