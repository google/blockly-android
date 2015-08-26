/*
 * Copyright  2015 Google Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.blockly.ui.fieldview;

import android.view.View;

import com.google.blockly.ui.FieldWorkspaceParams;

/**
 * Describes methods that all views that are representing a Field must implement. Implementations of
 * FieldViews must extend {@link View} or a subclass of View.
 */
public interface FieldView {
    /**
     * Gets the height of this view as part of a block, not including any extra panels or dropdowns
     * that are currently open. This is the height in the block.
     *
     * @return The height the view should take up in a block.
     */
    public int getInBlockHeight();

    /**
     * Gets the width of this view as part of a block, not including any extra panels or dropdowns
     * that are currently open. This is the width in the block.
     *
     * @return The width the view should take up in a block.
     */
    public int getInBlockWidth();

    /**
     * @return The workspace params for this view.
     */
    public FieldWorkspaceParams getWorkspaceParams();
}
