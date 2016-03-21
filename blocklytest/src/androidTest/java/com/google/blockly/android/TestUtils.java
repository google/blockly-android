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

import com.google.blockly.android.control.ConnectionManager;
import com.google.blockly.android.ui.BlockViewFactory;
import com.google.blockly.android.ui.WorkspaceView;
import com.google.blockly.model.Block;

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
}
