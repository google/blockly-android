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

package com.google.blockly.android.control;

import android.view.View;

import com.google.blockly.android.CategoryFragment;
import com.google.blockly.android.FlyoutFragment;
import com.google.blockly.android.ui.BlockGroup;
import com.google.blockly.android.ui.CategoryTabs;
import com.google.blockly.android.ui.FlyoutView;
import com.google.blockly.android.ui.TrashCanView;
import com.google.blockly.model.Block;
import com.google.blockly.model.FlyoutCategory;
import com.google.blockly.model.WorkspacePoint;

import java.util.List;

/**
 * Provides helper classes to the BlocklyController for defining the behavior of the toolbox
 * and trash.
 */
public class FlyoutController {
    private static final String TAG = "FlyoutController";
        // Whether we default to a closable toolbox when categories are present.
    protected boolean mToolboxPreferCloseable = true;
    protected boolean mToolboxIsCloseable = mToolboxPreferCloseable;
    protected CategoryFragment mCategoryFragment;
    protected FlyoutFragment mToolboxFlyout;
    protected FlyoutCategory mToolboxRoot;

    protected boolean mTrashIsCloseable = true;
    protected FlyoutFragment mTrashFlyout;
    protected FlyoutCategory mTrashCategory;
    protected View mTrashIcon;

    protected BlocklyController mController;

    protected FlyoutView.Callback mToolboxViewCallback = new FlyoutView.Callback() {
        @Override
        public void onActionClicked(View v, String action, FlyoutCategory category) {
            // TODO (#503): Switch to using the view's tag to determine behavior
            if (category != null && category.isVariableCategory() && mController != null) {
                mController.requestAddVariable("item");
            }
        }

        @Override
        public BlockGroup getDraggableBlockGroup(int index, Block blockInList,
                WorkspacePoint initialBlockPosition) {
            Block copy = blockInList.deepCopy();
            copy.setPosition(initialBlockPosition.x, initialBlockPosition.y);
            BlockGroup copyView = mController.addRootBlock(copy);
            if (mToolboxFlyout.isCloseable()) {
                mToolboxFlyout.closeBlocksDrawer();
                if (mCategoryFragment != null) {
                    mCategoryFragment.setCurrentCategory(null);
                }
            }
            return copyView;
        }
    };

    protected CategoryTabs.Callback mTabsCallback = new CategoryTabs.Callback() {
        @Override
        public void onCategoryClicked(FlyoutCategory category) {
            FlyoutCategory currCategory = mCategoryFragment.getCurrentCategory();
            if (category == currCategory) {
                // Clicked the open category, close it if closeable.
                if (isToolboxCloseable()) {
                    // deselect the category
                    mCategoryFragment.setCurrentCategory(null);
                    // close the drawer
                    mToolboxFlyout.closeBlocksDrawer();
                }
            } else {
                mCategoryFragment.setCurrentCategory(category);
                mToolboxFlyout.setCurrentCategory(category);
                if (mTrashIsCloseable && mTrashFlyout != null) {
                    mTrashFlyout.closeBlocksDrawer();
                }
            }

        }
    };


    protected FlyoutView.Callback mTrashFlyoutCallback = new FlyoutView.Callback() {
        @Override
        public void onActionClicked(View v, String action, FlyoutCategory category) {
            // No actions recognized by the trash
        }

        @Override
        public BlockGroup getDraggableBlockGroup(int index, Block blockInList,
                WorkspacePoint initialBlockPosition) {
            Block copy = blockInList.deepCopy();
            copy.setPosition(initialBlockPosition.x, initialBlockPosition.y);
            BlockGroup copyView = mController.addRootBlock(copy);
            mTrashCategory.removeBlock(blockInList);
            if (mTrashFlyout.isCloseable()) {
                mTrashFlyout.closeBlocksDrawer();
            }
            return copyView;
        }
    };

    protected View.OnClickListener mTrashClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // Toggle opened state.
            if (mTrashFlyout.isOpen()) {
                mTrashFlyout.closeBlocksDrawer();
            } else {
                mTrashFlyout.setCurrentCategory(mTrashCategory);
                if (mToolboxIsCloseable && mToolboxFlyout != null) {
                    mToolboxFlyout.closeBlocksDrawer();
                }
            }
        }
    };

    public FlyoutController(BlocklyController controller) {
        mController = controller;
    }

    public void setToolboxFragments(CategoryFragment categoryFragment,
            FlyoutFragment toolboxFlyout) {
        mCategoryFragment = categoryFragment;
        mToolboxFlyout = toolboxFlyout;
        if (mToolboxFlyout == null) {
            throw new IllegalArgumentException("Must have a flyout to manage the Toolbox.");
        }

        if (mCategoryFragment != null) {
            mCategoryFragment.setCategoryCallback(mTabsCallback);
        }
        mToolboxPreferCloseable = mToolboxFlyout.isCloseable();
        mToolboxFlyout.init(mController, mToolboxViewCallback);
        if (mToolboxRoot != null) {
            setToolboxRoot(mToolboxRoot);
        }
    }

    public void setController(BlocklyController controller) {
        mController = controller;
        if (mController != null) {
            if (mToolboxFlyout != null) {
                mToolboxFlyout.init(mController, mTrashFlyoutCallback);
            }
            if (mTrashFlyout != null) {
                mTrashFlyout.init(controller, mTrashFlyoutCallback);
            }
        }
    }

    public void setToolboxRoot(FlyoutCategory root) {
        mToolboxRoot = root;
        if (mToolboxRoot == null) {
            if (mCategoryFragment != null) {
                mCategoryFragment.setContents(null);
            }
            if (mToolboxFlyout != null) {
                mToolboxFlyout.setCurrentCategory(null);
            }
            // TODO reset fragments
            return;
        }
        List<FlyoutCategory> subCats = root.getSubcategories();
        List<Block> topBlocks = root.getBlocks();
        if (subCats.size() > 0 && topBlocks.size() > 0) {
            throw new IllegalArgumentException(
                    "Toolbox root cannot have both blocks and subcategories.");
        }
        if (root.getSubcategories().size() == 0) {
            mToolboxIsCloseable = false;
            if (mCategoryFragment != null) {
                mCategoryFragment.setContents(null);
                mCategoryFragment.getView().setVisibility(View.GONE);
            }
        } else {
            mToolboxIsCloseable = mToolboxPreferCloseable;
            if (mCategoryFragment != null) {
                mCategoryFragment.setContents(root);
                mCategoryFragment.getView().setVisibility(View.VISIBLE);
            }
        }
        if (mToolboxIsCloseable && mToolboxFlyout != null) {
            mToolboxFlyout.closeBlocksDrawer();
        }
    }

    public boolean closeFlyouts() {
        boolean didClose = false;
        if (isTrashCloseable() && mTrashFlyout != null) {
            didClose = mTrashFlyout.closeBlocksDrawer();
        }
        if (isToolboxCloseable() && mToolboxFlyout != null) {
            didClose |= mToolboxFlyout.closeBlocksDrawer();
        }
        return didClose;
    }

    public boolean isToolboxCloseable() {
        return mToolboxIsCloseable && mCategoryFragment != null
                && mToolboxRoot.getSubcategories().size() > 0;
    }

    public boolean isTrashCloseable() {
        return mTrashIsCloseable;
    }

    public void setTrashFragment(FlyoutFragment trashFragment) {
        mTrashFlyout = trashFragment;
        if (trashFragment != null) {
            mTrashIsCloseable = mTrashFlyout.isCloseable();
            mTrashFlyout.init(mController, mTrashFlyoutCallback);
        }
    }

    public void setTrashContents(FlyoutCategory trashContents) {
        mTrashCategory = trashContents;
        if (mTrashFlyout != null) {
            mTrashFlyout.setCurrentCategory(mTrashFlyout.isOpen() ? trashContents : null);
        }
    }

    public void setTrashIcon(TrashCanView trashIcon) {
        mTrashIcon = trashIcon;
        if (mTrashFlyout.isCloseable()) {
            mTrashFlyout.closeBlocksDrawer();

            trashIcon.setOnClickListener(mTrashClickListener);
            trashIcon.setBlocklyController(mController);
        }
    }
}
