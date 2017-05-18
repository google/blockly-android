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
import com.google.blockly.android.ZoomBehavior;
import com.google.blockly.android.codegen.CodeGenerationRequest;
import com.google.blockly.android.codegen.LoggingCodeGeneratorCallback;
import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.model.Block;
import com.google.blockly.model.DefaultBlocks;
import com.google.blockly.utils.BlockLoadingException;

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

    public static final String SAVE_FILENAME = "dev_tests_workspace.xml";

    private static final List<String> BLOCK_DEFINITIONS = Collections.unmodifiableList(
            Arrays.asList(
                    DefaultBlocks.LIST_BLOCKS_PATH,
                    DefaultBlocks.LOGIC_BLOCKS_PATH,
                    DefaultBlocks.LOOP_BLOCKS_PATH,
                    DefaultBlocks.MATH_BLOCKS_PATH,
                    DefaultBlocks.TEXT_BLOCKS_PATH,
                    DefaultBlocks.VARIABLE_BLOCKS_PATH,
                    "default/test_blocks.json",
                    "sample_sections/mock_block_definitions.json"
            ));

    private static int CARPET_SIZE = 1000;

    public static final String WORKSPACE_FOLDER_PREFIX = "sample_sections/level_";

    protected MenuItem mScrollableMenuItem;
    protected MenuItem mPinchZoomMenuItem;
    protected MenuItem mLogEventsMenuItem;

    protected CodeGenerationRequest.CodeGeneratorCallback mCodeGeneratorCallback =
            new LoggingCodeGeneratorCallback(this, TAG);
    protected LogAllEventsCallback mEventsCallback = new LogAllEventsCallback("BlocklyEvents");

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean isShown = super.onCreateOptionsMenu(menu);
        if (isShown) {
            ZoomBehavior zb = getController().getWorkspaceHelper().getZoomBehavior();

            mScrollableMenuItem = menu.findItem(R.id.scrollable_menuitem);
            mPinchZoomMenuItem = menu.findItem(R.id.pinch_zoom_menuitem);
            mLogEventsMenuItem = menu.findItem(R.id.log_events_menuitem);

            if (mScrollableMenuItem != null) {
                mScrollableMenuItem.setEnabled(false); // TODO: Dynamic Zoom Behavior
                mScrollableMenuItem.setChecked(zb.isScrollEnabled());
            }
            if (mPinchZoomMenuItem != null) {
                mPinchZoomMenuItem.setEnabled(false); // TODO: Dynamic Zoom Behavior
                mPinchZoomMenuItem.setChecked(zb.isPinchZoomEnabled());
            }
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

    /**
     * @param logEvents Enable event logging if true. Otherwise, disable.
     */
    private void setLogEvents(boolean logEvents) {
        if (logEvents) {
            getController().addCallback(mEventsCallback);
        } else {
            getController().removeCallback(mEventsCallback);
        }
        mLogEventsMenuItem.setChecked(logEvents);
    }

    /**
     * Place one instance of each of the toolbox's blocks, on the workspace, all in the same place.
     */
    private void airstrike() {
        List<Block> blocks = new ArrayList<>();
        getController().getWorkspace().getToolboxContents().getAllBlocksRecursive(blocks);
        for (int i = 0; i < blocks.size(); i++) {
            Block copiedModel = blocks.get(i).deepCopy();
            copiedModel.setPosition(0, 0);
            getController().addRootBlock(copiedModel);
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
            getController().addRootBlock(copiedModel);
        }
    }

    /**
     * Loads a workspace with heavily nested blocks.
     */
    private boolean loadSpaghetti() {
        try {
            getController().loadWorkspaceContents(getAssets().open(
                    "sample_sections/workspace_spaghetti.xml"));
            return true;
        } catch (IOException | BlockLoadingException e) {
            Toast.makeText(getApplicationContext(),
                    R.string.toast_workspace_file_not_found,
                    Toast.LENGTH_LONG).show();
            Log.e(TAG, "Failed to load spaghetti workspace.", e);
            return false;
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

    @NonNull
    @Override
    protected List<String> getGeneratorsJsPaths() {
        List<String> paths = new ArrayList<String>(1);
        paths.add("sample_sections/generators.js");
        return paths;
    }

    @Override
    protected void onLoadInitialWorkspace() {
        try {
            getController().loadWorkspaceContents(getAssets().open(
                    "sample_sections/mock_block_initial_workspace.xml"));
        } catch (IOException | BlockLoadingException e) {
            Log.e(TAG, "Couldn't load initial workspace.", e);
            // Compile-time assets are assumed good.
            throw new IllegalStateException(e);
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

    @Override
    protected void onAutosave() {
        // Dev tests doesn't autosave/restore the user's workspace by default as we load a specific
        // workspace in onLoadInitialWorkspace.
        return;
    }

    @Override
    protected boolean onAutoload() {
        // Dev tests doesn't autosave/restore the user's workspace by default as we load a specific
        // workspace in onLoadInitialWorkspace.
        return false;
    }

    @Override
    @NonNull
    protected String getWorkspaceSavePath() {
        return SAVE_FILENAME;
    }
}
