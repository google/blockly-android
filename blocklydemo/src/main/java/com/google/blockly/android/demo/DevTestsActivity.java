/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

package com.google.blockly.android.demo;

import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.Toast;

import com.google.blockly.android.BlocklySectionsActivity;
import com.google.blockly.android.codegen.CodeGenerationRequest;
import com.google.blockly.android.codegen.LoggingCodeGeneratorCallback;
import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.android.ui.BlockViewFactory;
import com.google.blockly.android.ui.WorkspaceHelper;
import com.google.blockly.android.ui.vertical.VerticalBlockViewFactory;
import com.google.blockly.model.Block;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * Activity with Developer oriented tests.
 */
public class DevTestsActivity extends BlocklySectionsActivity {
    private static final String TAG = "DevTestsActivity";

    public static final String SAVED_WORKSPACE_FILENAME = "dev_tests_workspace.xml";
    private static final List<String> BLOCK_DEFINITIONS = Collections.unmodifiableList(
            Arrays.asList(new String[] {
                    "default/logic_blocks.json",
                    "default/loop_blocks.json",
                    "default/math_blocks.json",
                    "default/variable_blocks.json",
                    "default/test_blocks.json",
                    "sample_sections/mock_block_definitions.json"
            }));

    private static int CARPET_SIZE = 1000;

    public static final String WORKSPACE_FOLDER_PREFIX = "sample_sections/level_";

    protected MenuItem mLogEventsMenuItem;

    protected CodeGenerationRequest.CodeGeneratorCallback mCodeGeneratorCallback =
            new LoggingCodeGeneratorCallback(this, TAG);
    protected LogAllEventsCallback mEventsCallback = new LogAllEventsCallback("BlocklyEvents");

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean isShown = super.onCreateOptionsMenu(menu);
        if (isShown) {
            mLogEventsMenuItem = menu.findItem(R.id.log_events_menuitem);
        }
        return isShown;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.log_events_menuitem) {
            setLogEvents(!mLogEventsMenuItem.isChecked());
        } else if (id == R.id.action_airstrike) {
            airstrike();
            return true;
        } else if (id == R.id.action_carpet_bomb) {
            carpetBomb();
            return true;
        } else if (id == R.id.action_spaghetti) {
            loadSpaghetti();
            return true;
        } else if (id == R.id.action_create_variable) {
            getController().requestAddVariable("item");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onLoadWorkspace() {
        loadWorkspaceFromAppDir(SAVED_WORKSPACE_FILENAME);
    }

    @Override
    public void onSaveWorkspace() {
        saveWorkspaceToAppDir(SAVED_WORKSPACE_FILENAME);
    }

    private void setLogEvents(boolean logEvents) {
        if (logEvents) {
            mController.addCallback(mEventsCallback);
        } else {
            mController.removeListener(mEventsCallback);
        }
        mLogEventsMenuItem.setChecked(logEvents);
    }

    /**
     * Place one instance of each of the toolbox's blocks, on the workspace, all in the same place.
     */
    private void airstrike() {
        List<Block> blocks = new ArrayList<>();
        mController.getWorkspace().getToolboxContents().getAllBlocksRecursive(blocks);
        for (int i = 0; i < blocks.size(); i++) {
            Block copiedModel = blocks.get(i).deepCopy();
            copiedModel.setPosition(0, 0);
            mController.addRootBlock(copiedModel);
        }
    }

    /**
     * Place one instance of each of the toolbox's blocks, randomly across a section of the
     * workspace.
     */
    private void carpetBomb() {
        List<Block> blocks = new ArrayList<>();
        getController().getWorkspace().getToolboxContents().getAllBlocksRecursive(blocks);
        for (int i = 0; i < blocks.size(); i++) {
            Block copiedModel = blocks.get(i).deepCopy();
            copiedModel.setPosition((int) (Math.random() * CARPET_SIZE) - CARPET_SIZE / 2,
                    (int) (Math.random() * CARPET_SIZE) - CARPET_SIZE / 2);
            mController.addRootBlock(copiedModel);
        }
    }

    /**
     * Loads a workspace with heavily nested blocks.
     */
    private void loadSpaghetti() {
        try {
            getController().loadWorkspaceContents(getAssets().open(
                    "sample_sections/workspace_spaghetti.xml"));
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(),
                    R.string.toast_workspace_file_not_found,
                    Toast.LENGTH_LONG).show();
        }
    }

    @NonNull
    @Override
    protected String getToolboxContentsXmlPath() {
        // Expose a different set of blocks to the user at each level.
        return WORKSPACE_FOLDER_PREFIX + (getCurrentSectionIndex() + 1) + "/toolbox.xml";
    }

    @NonNull
    @Override
    protected List<String> getBlockDefinitionsJsonPaths() {
        return BLOCK_DEFINITIONS;
    }

    @Override
    protected String getStartingWorkspacePath() {
        return "default/demo_workspace.xml";
    }

    @NonNull
    @Override
    protected List<String> getGeneratorsJsPaths() {
        List<String> paths = new ArrayList<String>(1);
        paths.add("sample_sections/generators.js");
        return paths;
    }

    @Override
    public BlockViewFactory onCreateBlockViewFactory(WorkspaceHelper helper) {
        return new VerticalBlockViewFactory(this, helper);
    }

    @Override
    protected void onLoadInitialWorkspace() {
        try {
            getController().loadWorkspaceContents(getAssets().open(
                    "sample_sections/mock_block_initial_workspace.xml"));
        } catch (IOException e) {
            Log.d(TAG, "Couldn't load initial workspace.");
        }
        addDefaultVariables();
    }

    @Override
    protected int getActionBarMenuResId() {
        return R.menu.dev_actionbar;
    }


    @NonNull
    @Override
    protected CodeGenerationRequest.CodeGeneratorCallback getCodeGenerationCallback() {
        // Uses the same callback for every generation call.
        return mCodeGeneratorCallback;
    }

    @NonNull
    @Override
    protected ListAdapter onCreateSectionsListAdapter() {
        return new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_activated_1,
                android.R.id.text1,
                new String[]{
                        getString(R.string.title_section1),
                        getString(R.string.title_section2),
                        getString(R.string.title_section3),
                });
    }

    @Override
    protected boolean onSectionChanged(int oldSection, int newSection) {
        reloadToolbox();
        return true;
    }

    @Override
    protected void onInitBlankWorkspace() {
        addDefaultVariables();
    }

    private void addDefaultVariables() {
        // TODO: (#22) Remove this override when variables are supported properly
        BlocklyController controller = getController();
        controller.addVariable("item");
        controller.addVariable("zim");
        controller.addVariable("gir");
        controller.addVariable("tak");
    }
}
