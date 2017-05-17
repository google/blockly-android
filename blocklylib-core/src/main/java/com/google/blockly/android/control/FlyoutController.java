/*
 *  Copyright 2017 Google Inc. All Rights Reserved.
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

import android.support.annotation.Nullable;
import android.view.View;

import com.google.blockly.android.ui.BlockListUI;
import com.google.blockly.android.ui.CategorySelectorUI;
import com.google.blockly.android.ui.BlockGroup;
import com.google.blockly.android.ui.FlyoutCallback;
import com.google.blockly.android.ui.OnDragToTrashListener;
import com.google.blockly.model.Block;
import com.google.blockly.model.BlocklyCategory;
import com.google.blockly.model.VariableCustomCategory;
import com.google.blockly.model.WorkspacePoint;

import java.util.List;

/**
 * Provides control logic for the toolbox and trash in a workspace. Ensures the toolbox and trash
 * block-list flyouts are populated, opened, and closed in coordination.
 */
public class FlyoutController {
    private static final String TAG = "FlyoutController";
    /** Whether the toolbox is currently closeable, depending on configuration. */
    protected boolean mToolboxIsCloseable = true;
    /** The fragment for displaying toolbox categories. */
    protected CategorySelectorUI mCategorySelectorUi;
    /** The fragment for displaying blocks in the current category. */
    protected BlockListUI mToolbox;
    /** The root of the toolbox tree, containing either blocks or subcategories (not both). */
    protected BlocklyCategory mToolboxRoot;

    /** Whether the trash is closeable, depending on configuration. */
    protected boolean mTrashIsCloseable = true;
    /** The UI for displaying blocks in the trash. */
    protected BlockListUI mTrashUi;
    /** The category backing the trash's list of blocks. */
    protected BlocklyCategory mTrashCategory;

    /** Main controller for any actions that require wider state changes. */
    protected BlocklyController mController;

    /** Callbacks for user actions on the toolbox's flyout. */
    protected FlyoutCallback mToolboxCallback = new FlyoutCallback() {
        @Override
        public void onButtonClicked(View v, String action, BlocklyCategory category) {
            if (action == VariableCustomCategory.ACTION_CREATE_VARIABLE && mController != null) {
                mController.requestAddVariable("item");
            }
        }

        @Override
        public BlockGroup getDraggableBlockGroup(int index, Block blockInList,
                WorkspacePoint initialBlockPosition) {
            Block copy = blockInList.deepCopy();
            copy.setPosition(initialBlockPosition.x, initialBlockPosition.y);
            BlockGroup copyView = mController.addRootBlock(copy);
            closeToolbox();
            return copyView;
        }
    };

    /** Callback for user category selection. */
    protected CategorySelectorUI.Callback mCategoriesCallback = new CategorySelectorUI.Callback() {
        @Override
        public void onCategoryClicked(BlocklyCategory category) {
            BlocklyCategory currCategory = mCategorySelectorUi.getCurrentCategory();
            if (category == currCategory) {
                // Clicked the open category, close it if closeable.
                closeToolbox();
            } else {
                setToolboxCategory(category);
                closeTrash();
            }

        }
    };

    /** Callbacks for user actions on the trash's flyout. */
    protected FlyoutCallback mTrashCallback = new FlyoutCallback() {
        @Override
        public void onButtonClicked(View v, String action, BlocklyCategory category) {
            // No actions recognized by the trash
        }

        @Override
        public BlockGroup getDraggableBlockGroup(int index, Block blockInList,
                WorkspacePoint initialBlockPosition) {
            Block copy = blockInList.deepCopy();
            copy.setPosition(initialBlockPosition.x, initialBlockPosition.y);
            BlockGroup copyView = mController.addRootBlock(copy);
            mTrashCategory.removeItem(index);
            closeTrash();
            return copyView;
        }
    };

    /** Opens/closes the trash in response to clicks on the trash icon. */
    protected View.OnClickListener mTrashClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // Toggle opened state.
            if (mTrashUi.isOpen()) {
                closeTrash();
            } else {
                mTrashUi.setCurrentCategory(mTrashCategory);
                closeToolbox();
            }
        }
    };

    public FlyoutController(BlocklyController controller) {
        mController = controller;
    }

    /**
     * Sets the fragments used by a toolbox. At minimum a flyout is needed. If a category fragment
     * is provided it will be used to switch between categories and open/close the flyout if it
     * is closeable.
     *
     * @param categoryFragment The fragment for displaying category tabs.
     * @param toolbox The fragment for displaying blocks in a category.
     */
    public void setToolboxUiComponents(CategorySelectorUI categoryFragment,
                                       BlockListUI toolbox) {
        mCategorySelectorUi = categoryFragment;
        mToolbox = toolbox;
        if (mToolbox == null) {
            return;
        }

        if (mCategorySelectorUi != null) {
            mCategorySelectorUi.setCategoryCallback(mCategoriesCallback);
        }
        mToolboxIsCloseable = mToolbox.isCloseable();
        if (mToolboxRoot != null) {
            setToolboxRoot(mToolboxRoot);
        }
        mToolbox.init(mController, mToolboxCallback);
        updateToolbox();
    }

    /**
     * Sets the root of the toolbox tree. This will be used to populate the category and toolbox.
     *
     * @param root The root category for the toolbox.
     */
    public void setToolboxRoot(BlocklyCategory root) {
        mToolboxRoot = root;
        if (mToolboxRoot == null) {
            if (mCategorySelectorUi != null) {
                mCategorySelectorUi.setContents(null);
            }
            if (mToolbox != null) {
                mToolbox.closeUi();
            }
            return;
        }
        updateToolbox();
    }

    /**
     * Closes the trash and toolbox if they're open and closeable.
     *
     * @return True if either flyout was closed, false otherwise.
     */
    public boolean closeFlyouts() {
        return closeTrash() || closeToolbox();
    }

    /**
     * @return True if the toolbox's flyout may be closed.
     */
    public boolean isToolboxCloseable() {
        return mToolboxIsCloseable && mCategorySelectorUi != null;
    }

    /**
     * @return True if the trash's flyout may be closed.
     */
    public boolean isTrashCloseable() {
        return mTrashIsCloseable;
    }

    /**
     * @param trashUi The trash UI to use for displaying blocks in the trash.
     */
    public void setTrashUi(BlockListUI trashUi) {
        mTrashUi = trashUi;
        if (trashUi != null) {
            mTrashIsCloseable = mTrashUi.isCloseable();
            mTrashUi.init(mController, mTrashCallback);
            closeTrash();
        }
    }

    /**
     * @param trashContents The category with the set of blocks for display in the trash.
     */
    public void setTrashContents(BlocklyCategory trashContents) {
        mTrashCategory = trashContents;
        if (mTrashUi != null && mTrashUi.isOpen()) {
            mTrashUi.setCurrentCategory(trashContents);
        }
    }

    /**
     * @param trashIcon The view for toggling the trash.
     */
    public void setTrashIcon(View trashIcon) {
        if (trashIcon == null) {
            return;
        }
        // The trash icon is always a drop target.
        trashIcon.setOnDragListener(new OnDragToTrashListener(mController));
        if (mTrashUi != null && mTrashIsCloseable) {
            // But we only need a click listener if the trash can be closed.
            trashIcon.setOnClickListener(mTrashClickListener);
        }
    }

    /**
     * Updates the contents of toolbox and ensures it's open if it's not closeable.
     */
    private void updateToolbox() {
        if (mToolboxRoot == null) {
            return;
        }
        List<BlocklyCategory> subCats = mToolboxRoot.getSubcategories();
        List<BlocklyCategory.CategoryItem> topItems = mToolboxRoot.getItems();
        if (subCats.size() > 0 && topItems.size() > 0) {
            throw new IllegalArgumentException(
                    "Toolbox root cannot have both blocks and subcategories.");
        }
        if (mToolboxRoot.getSubcategories().size() == 0) {
            BlocklyCategory newRoot = new BlocklyCategory();
            newRoot.addSubcategory(mToolboxRoot);
            mToolboxRoot = newRoot;
        }
        if (mCategorySelectorUi != null) {
            mCategorySelectorUi.setContents(mToolboxRoot);
        }
        if (!mToolboxIsCloseable) {
            setToolboxCategory(mToolboxRoot.getSubcategories().get(0));
        } else {
            closeToolbox();
        }
    }

    /**
     * Handles setting the category on the toolbox flyout and the category fragment if they exist.
     *
     * @param category The category to set.
     */
    private void setToolboxCategory(@Nullable BlocklyCategory category) {
        if (mToolbox != null) {
            if (category != null) {
                mToolbox.setCurrentCategory(category);
            } else {
                mToolbox.closeUi();
            }
        }
        if (mCategorySelectorUi != null) {
            mCategorySelectorUi.setCurrentCategory(category);
        }
    }

    /**
     * Handles checking if the toolbox is closeable and closing it if so.
     *
     * @return true if the toolbox was closed, false otherwise.
     */
    private boolean closeToolbox() {
        boolean didClose = false;
        if (isToolboxCloseable() && mToolbox != null) {
            didClose = mToolbox.closeUi();
            if (mCategorySelectorUi != null) {
                mCategorySelectorUi.setCurrentCategory(null);
            }
        }
        return didClose;
    }

    /**
     * Handles checking and closing the trash flyout.
     *
     * @return true if the trash was closed, false otherwise.
     */
    private boolean closeTrash() {
        boolean didClose = false;
        if (isTrashCloseable() && mTrashUi != null) {
            didClose = mTrashUi.closeUi();
        }
        return didClose;
    }
}
