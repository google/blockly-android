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

import android.support.annotation.Nullable;

import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.model.BlocklyCategory;

/**
 * An interface that specifies the actions that can be taken on a flyout ui component.
 */
public interface BlockListUI {

    /**
     * @return True if this UI is currently visible, false otherwise.
     */
    boolean isOpen();

    /**
     * Sets the Flyout's current {@link BlocklyCategory}, including opening or closing the drawer.
     * In closeable toolboxes, {@code null} {@code category} is equivalent to closing the drawer.
     * Otherwise, the drawer will be rendered empty.
     *
     * @param category The {@link BlocklyCategory} with blocks to display.
     */
    void setCurrentCategory(@Nullable BlocklyCategory category);

    /**
     * @return True if this flyout is allowed to close, false otherwise.
     */
    boolean isCloseable();

    /**
     * Connects the {@link BlockListUI} to the application's drag and click handling. It is
     * called by
     * {@link BlocklyController#setToolboxUi(BlockListUI, CategorySelectorUI)}
     * and should not be called by the application developer.
     *
     * @param callback The callback that will handle user actions in the flyout.
     */
    void init(BlocklyController controller, FlyoutCallback callback);

    /**
     * Attempts to hide or close the blocks UI (e.g., a drawer).
     *
     * @return True if an action was taken (the drawer is closeable and was previously open).
     */
    boolean closeUi();
}
