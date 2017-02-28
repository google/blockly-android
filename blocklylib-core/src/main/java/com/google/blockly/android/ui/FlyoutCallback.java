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

import android.view.View;

import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.model.Block;
import com.google.blockly.model.BlocklyCategory;
import com.google.blockly.model.Workspace;
import com.google.blockly.model.WorkspacePoint;

/**
 * Callback for determining the behavior of user actions in a flyout that affect the workspace. Use
 * {@link com.google.blockly.android.FlyoutFragment#init(BlocklyController, FlyoutCallback)} to
 * define this behavior. {@link BlockRecyclerViewHelper#init(BlocklyController, FlyoutCallback)} may
 * be used instead if not using a FlyoutFragment.
 */
public abstract class FlyoutCallback {
    /**
     * Called when an action button is clicked (example: when "Create variable" is clicked).
     *
     * @param v The view that was clicked.
     * @param action The callback key associated with the clicked button.
     * @param category The category that this action was in.
     */
    public abstract void onButtonClicked(View v, String action, BlocklyCategory category);

    /**
     * Handles the selection of the draggable {@link BlockGroup}, including possibly adding the
     * block to the {@link Workspace} and {@link WorkspaceView}.
     *
     * @param index The list position of the touched block group.
     * @param blockInList The root block of the touched block.
     * @param initialBlockPosition The initial workspace coordinate for
     *         {@code touchedBlockGroup}'s screen location.
     * @return The block group to drag within the workspace.
     */
    public abstract BlockGroup getDraggableBlockGroup(int index, Block blockInList,
            WorkspacePoint initialBlockPosition);
}
