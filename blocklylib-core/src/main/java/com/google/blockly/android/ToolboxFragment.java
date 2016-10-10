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
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.android.ui.BlockGroup;
import com.google.blockly.android.ui.BlockListView;
import com.google.blockly.android.ui.CategoryTabs;
import com.google.blockly.android.ui.Rotation;
import com.google.blockly.android.ui.BlockDrawerFragment;
import com.google.blockly.android.ui.ToolboxView;
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
 * Developers can further customize the toolbox by providing their own ToolboxView.
 *
 * @attr ref com.google.blockly.R.styleable#BlockDrawerFragment_closeable
 * @attr ref com.google.blockly.R.styleable#BlockDrawerFragment_scrollOrientation
 * @attr ref com.google.blockly.R.styleable#ToolboxFragment_tabEdge
 * @attr ref com.google.blockly.R.styleable#ToolboxFragment_rotateTabs
 */
// TODO(#9): Attribute and arguments to set the tab background.
public class ToolboxFragment extends Fragment {
    private static final String TAG = "ToolboxFragment";

    protected ToolboxView mToolboxView;
    protected BlocklyController mController;
    protected WorkspaceHelper mHelper;

    private BlockListView.OnDragListBlock mDragHandler = new BlockListView.OnDragListBlock() {
        @Override
        public BlockGroup getDraggableBlockGroup(int index, Block blockInList,
                                                 WorkspacePoint initialBlockPosition) {
            Block copy = blockInList.deepCopy();
            copy.setPosition(initialBlockPosition.x, initialBlockPosition.y);
            BlockGroup copyView = mController.addRootBlock(copy);
            if (mToolboxView.isCloseable()) {
                closeBlocksDrawer();
            }
            return copyView;
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // TODO replace with lookup in onFinishInflate
        mToolboxView = onCreateToolboxView(inflater, savedInstanceState);

        mToolboxView.setOnActionClickListener(new ToolboxView.OnActionClickListener() {
            @Override
            public void onActionClicked(View v, ToolboxCategory category) {
                if (category.isVariableCategory() && mController != null) {
                    mController.requestAddVariable("item");
                }
            }
        });

        mToolboxView.setCategoryTabsCallback(new CategoryTabs.Callback() {
            @Override
            public void onCategorySelected(ToolboxCategory category) {
                mToolboxView.setCurrentCategory(category);
            }
        });
        return mToolboxView;
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
            mToolboxView.reset();
        }
        mToolboxView.init(mController, mDragHandler);
        BlockListView blv = mToolboxView.getBlockListView();
        blv.init(mController, mDragHandler);
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
        mToolboxView.setContents(topLevelCategory);
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
        mToolboxView.setCurrentCategory(category);
    }

    public boolean isCloseable() {
        return mToolboxView.isCloseable();
    }

    /**
     * Attempts to close the blocks drawer.
     *
     * @return True if an action was taken (the drawer is closeable and was previously open).
     */
    // TODO(#80): Add mBlockList animation hooks for subclasses.
    public boolean closeBlocksDrawer() {
        return mToolboxView.setCurrentCategory(null);
    }

    /**
     * Create the {@link ToolboxView} to be used as the base view for this class. Typically, this
     * just needs to inflate the appropriate layout and return it, but custom configuration may
     * also be done.
     *
     * @param inflater The inflater to use for loading a layout.
     * @param savedInstanceState Any saved state for the fragment.
     * @return
     */
    protected ToolboxView onCreateToolboxView(LayoutInflater inflater, Bundle savedInstanceState) {
        mToolboxView = (ToolboxView) inflater.inflate(R.layout.default_toolbox_start, null);
        return mToolboxView;
    }
}
