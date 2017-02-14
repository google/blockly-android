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

package com.google.blockly.android.ui;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.blockly.model.BlocklyCategory;

/**
 * An interface that specifies the actions that can be taken on a category selector ui component.
 */
public interface CategorySelectorUI {

    /**
     * Set the root category for the category selector.
     *
     * @param rootCategory The top-level category in the toolbox.
     */
    void setContents(@NonNull BlocklyCategory rootCategory);

    /**
     * Sets the currently selected category. If {@code category} is null, or if the category is not
     * a member of the contents previously set, the current category should be unselected.
     *
     * @param category the category to use as currently selected.
     */
    void setCurrentCategory(@Nullable BlocklyCategory category);

    /**
     * @return the current category.
     */
    BlocklyCategory getCurrentCategory();

    /**
     * @param categoryCallback the callback for when the user clicks on a category.
     */
    void setCategoryCallback(@Nullable CategorySelectorUI.Callback categoryCallback);

    /**
     * Callback for when the user clicks on a category.
     */
    abstract class Callback {
        /**
         * Notifies the callback that the user has clicked on view representing a category.
         * Callback code is responsible for updating the toolbox BlockListUI (by default implemented
         * in {@link com.google.blockly.android.control.FlyoutController}).
         * @param category The clicked category.
         */
        public abstract void onCategoryClicked(@NonNull BlocklyCategory category);
    }
}
