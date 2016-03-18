/*
* Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.blockly.ui;

import android.content.ClipData;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.view.View;
import android.view.ViewParent;

import com.google.blockly.model.Block;
import com.google.blockly.model.Connection;

/**
 * Draws a block and handles laying out all its inputs/fields.
 */
public interface BlockView {
    /**
     * @return The block represented by this view.
     */
    Block getBlock();

    /** @see View#getParent() */
    ViewParent getParent();

    /**
     * @return The closest view tree ancestor that is a BlockGroup.
     */
    BlockGroup getParentBlockGroup();

    /**
     * Sets the {@link BlockTouchHandler} to use on this and all subviews.
     *
     * @param touchHandler
     */
    void setTouchHandler(BlockTouchHandler touchHandler);

    void setHighlightedConnection(@Nullable Connection connection);

    /**
     * @return Vertical offset for positioning the "Next" block (if one exists). This is relative to
     * the top of this view's area.
     */
    // TODO(Anm): Adapt for horizontal layout.
    int getNextBlockVerticalOffset();

    /**
     * @return Layout margin on the left-hand side of the block (for optional Output connector).
     */
    // TODO(Anm): Generalize for other block shapes?
    int getLayoutMarginLeft();

    /**
     * Updates the locations of the connections based on their offsets within the {@link BlockView},
     * based upon the view's position within the  {@link WorkspaceView}.  Often used when the block
     * has moved but not changed shape, such as after a drag.
     */
    void updateConnectorLocations();

    /**
     * Recursively disconnects the view from the model.
     */
    void unlinkModelAndSubViews();
}
