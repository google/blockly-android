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

import android.view.View;
import android.view.ViewGroup;

import com.google.blockly.android.control.ConnectionManager;
import com.google.blockly.android.ui.BlockView;
import com.google.blockly.android.ui.BlockViewFactory;
import com.google.blockly.android.ui.WorkspaceView;
import com.google.blockly.android.ui.fieldview.FieldView;
import com.google.blockly.model.Block;
import com.google.blockly.model.Field;

import java.util.List;

/**
 * Utils for setting up blocks during testing.
 */
public final class TestUtils {

    private TestUtils() {}

    /**
     * Create views for the given blocks and add them to the workspace given by the combination
     * of connection manager, helper, and view.
     */
    public static void createViews(List<Block> blocks, BlockViewFactory viewFactory,
            ConnectionManager connectionManager, WorkspaceView workspaceView) {

        // Create views for all of the blocks we're interested in.
        for (int i = 0; i < blocks.size(); i++) {
            workspaceView.addView(
                    viewFactory.buildBlockGroupTree(blocks.get(i), connectionManager, null));
        }
    }

    /**
     * Traverses the view tree for the FieldView for {@code field} of block, while ignoring
     * subblocks and related subviews.
     *
     * @param blockView The view for {@code field}'s parent {@link Block}.
     * @param field The field to find a {@link FieldView}.
     * @return The {@link View} / {@link FieldView} for {@code field}, or null if not found.
     */
    public static View getFieldView(BlockView blockView, Field field) {
        return getFieldViewImpl((ViewGroup) blockView, field);
    }

    // Recursive implementation of above.
    private static View getFieldViewImpl(ViewGroup parent, Field field) {
        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; ++i) {
            View child = parent.getChildAt(i);
            if (child instanceof BlockView) {
                continue; // Skip embedded blocks
            }
            if (child instanceof FieldView) {
                if (((FieldView) child).getField() == field) {
                    return child;
                } else {
                    continue;
                }
            }
            if (child instanceof ViewGroup) {
                View childResult = getFieldViewImpl((ViewGroup) child, field);
                if (childResult != null) {
                    return childResult;
                }
            }
        }
        return null;  // Not found.
    }
}
