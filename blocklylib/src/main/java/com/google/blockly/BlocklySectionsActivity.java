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

package com.google.blockly;

import android.content.res.AssetManager;
import android.support.annotation.NonNull;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

import com.google.blockly.control.BlocklyController;
import com.google.blockly.model.BlockFactory;
import com.google.blockly.model.Workspace;

import java.io.IOException;
import java.io.InputStream;

/**
 * Activity holding a full-screen Blockly workspace with multiple sections in the navigation menu.
 */
public abstract class BlocklySectionsActivity extends AbstractBlocklyActivity {
    private static final String TAG = BlocklySectionsActivity.class.getSimpleName();

    protected int mCurrentSection = 0;
    protected ListAdapter mLevelsAdapter;


    @Override
    public void onNavigationDrawerItemSelected(int position) {
        if (position == mCurrentSection) {
            return;
        }

        changeLevel(position);
        mCurrentSection = position;
    }

    /**
     * Updates {@link #mController} to the the currently selected section.
     */
    protected void updateBlockly() {
        String toolboxPath = getWorkspaceToolboxPath();
        String blockDefsPath = getWorkspaceBlocksPath();
        AssetManager assetManager = getAssets();

        BlocklyController controller = getController();
        BlockFactory factory = controller.getBlockFactory();
        factory.clear();

        try {
            factory.addBlocks(assetManager.open(blockDefsPath));
        } catch (IOException e) {
            throw new IllegalArgumentException("Error opening block defs at " + blockDefsPath, e);
        }
        try {
            controller.loadToolboxContents(assetManager.open(getWorkspaceToolboxPath()));
        } catch (IOException e) {
            throw new IllegalArgumentException("Error opening toolbox at " + toolboxPath, e);
        }
    }

    /**
     * @return The section that is currently displayed.
     */
    public final int getCurrentSection() {
        return mCurrentSection;
    }

    /**
     * @return The title of the current workspace / section.
     */
    protected CharSequence getWorkspaceTitle() {
        int section = getCurrentSection();
        if (section < getSectionCount()) {
            return (String) mLevelsAdapter.getItem(section);
        } else {
            // Use the Activity name.
            return getTitle();
        }
    }

    /**
     * Called after a section has been loaded. If you don't want to re-use the previous section's
     * code {@link Workspace#loadWorkspaceContents(InputStream)} should be called here.
     *
     * @param oldSection The previous level.
     * @param newSection The level that was just configured.
     */
    protected void onSectionChanged(int oldSection, int newSection) {
    }

    /**
     * @return The number of sections in this activity.
     */
    protected int getSectionCount() {
        return mLevelsAdapter.getCount();
    }

    /**
     * Populate the navigation menu with the list of available sections.
     *
     * @return An adapter of sections for the navigation menu.
     */
    @Override
    protected ListAdapter onCreateNavigationMenuAdapter() {
        mLevelsAdapter = onCreateSectionsAdapter();
        return mLevelsAdapter;
    }

    @NonNull
    protected ListAdapter onCreateSectionsAdapter() {
        mLevelsAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_activated_1,
                android.R.id.text1,
                new String[]{
                        getString(R.string.title_section1),
                        getString(R.string.title_section2),
                        getString(R.string.title_section3),
                });
        return mLevelsAdapter;
    }

    private void changeLevel(int level) {
        int oldLevel = mCurrentSection;
        mCurrentSection = level;
        updateBlockly();
        onSectionChanged(oldLevel, level);
    }
}
