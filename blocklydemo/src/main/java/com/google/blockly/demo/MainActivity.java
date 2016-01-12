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

package com.google.blockly.demo;

import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

import com.google.blockly.BlocklySectionsActivity;
import com.google.blockly.MockBlocksProvider;
import com.google.blockly.NavigationDrawerFragment;
import com.google.blockly.control.BlocklyController;


/**
 * Demo app with a multi-section workspace, a toolbox, and a trash can.
 */
public class MainActivity extends BlocklySectionsActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {
    public static final String WORKSPACE_FOLDER_PREFIX = "sample_sections/level_";

    @Override
    protected String getWorkspaceBlocksPath(int section) {
        // Use the same blocks for all the levels. This lets the user's block code carry over from
        // level to level. The set of blocks shown in the toolbox for each level is defined by the
        // toolbox path below.
        return "sample_sections/definitions.json";
    }

    @Override
    protected String getWorkspaceToolboxPath(int section) {
        // Expose a different set of blocks to the user at each level.
        return WORKSPACE_FOLDER_PREFIX + (section + 1)
                + "/toolbox.xml";
    }

    @Override
    protected void onSectionChanged(int oldSection, int newSection) {
        // If we just went down a level clear the workspace, otherwise keep the previous blocks.
        if (newSection < oldSection) {
            // Instead of clearing we could also load a default workspace for this level.
            getController().resetWorkspace();
        }
    }

    @Override
    protected ListAdapter onCreateSectionsAdapter() {
        // Create three sections with the labels "Level 1", "Level 2", and "Level 3" displaying them
        // as simple text items in the sections drawer.
        return new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_activated_1,
                android.R.id.text1,
                new String[]{
                        getString(R.string.level_1),
                        getString(R.string.level_2),
                        getString(R.string.level_3)
                });
    }

    @Override
    protected BlocklyController onConfigureBlockly() {
        BlocklyController controller = super.onConfigureBlockly();
        MockBlocksProvider.makeComplexModel(controller.getWorkspace());
        return controller;
    }
}
