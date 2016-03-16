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

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.google.blockly.model.Workspace;

import java.io.InputStream;

/**
 * Activity holding a full-screen Blockly workspace with multiple sections in the navigation menu.
 */
public abstract class BlocklySectionsActivity extends AbstractBlocklyActivity
        implements AdapterView.OnItemClickListener {
    private static final String TAG = "BlocklySectionsActivity";

    protected int mCurrentSection = 0;
    protected ListView mSectionsListView;
    protected ListAdapter mListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * Creates {@link #mSectionsListView} and configures it with
     * {@link #onCreateSectionsListAdapter()} to use as the activity's navigation menu.
     *
     * @return {@link #mSectionsListView} for the navigation menu.
     */
    @Override
    protected View onCreateAppNavigationDrawer() {
        mSectionsListView = (ListView) getLayoutInflater().inflate(R.layout.sections_list, null);
        mListAdapter = onCreateSectionsListAdapter();
        mSectionsListView.setAdapter(mListAdapter);
        mSectionsListView.setOnItemClickListener(this);

        return mSectionsListView;
    }

    /**
     * Handles clicks to {@link #mSectionsListView} by calling {@link #onSectionItemClicked} with
     * the selected position.
     *
     * @param parent The {@link ListView}.
     * @param view The selected item view.
     * @param position The position in the list.
     * @param id The id of the list item.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        onSectionItemClicked(position);
    }

    /**
     * Handles the selection of a section, including closing the navigation drawer.
     * @param sectionIndex
     */
    public void onSectionItemClicked(int sectionIndex) {
        mSectionsListView.setItemChecked(sectionIndex, true);
        setNavDrawerOpened(false);
        if (mCurrentSection == sectionIndex) {
            return;
        }

        changeSection(sectionIndex);
        mCurrentSection = sectionIndex;
    }

    /**
     * @return The title of the current workspace / section.
     */
    @NonNull
    protected CharSequence getWorkspaceTitle() {
        int section = getCurrentSectionIndex();
        if (section < getSectionCount()) {
            return (String) mListAdapter.getItem(section);
        } else {
            // Use the Activity name.
            return getTitle();
        }
    }

    /**
     * Populate the navigation menu with the list of available sections.
     *
     * @return An adapter of sections for the navigation menu.
     */
    @NonNull
    abstract protected ListAdapter onCreateSectionsListAdapter();

    /**
     * @return The section that is currently displayed.
     */
    public final int getCurrentSectionIndex() {
        return mCurrentSection;
    }

    /**
     * Called to load a new Section. If you don't want to re-use the previous section's
     * code {@link Workspace#loadWorkspaceContents(InputStream)} should be called here.
     *
     * @param oldSection The previous level.
     * @param newSection The level that was just configured.
     * @return True if the new section was successfully loaded. Otherwise false.
     */
    abstract protected boolean onSectionChanged(int oldSection, int newSection);

    /**
     * @return The number of sections in this activity.
     */
    protected int getSectionCount() {
        return mListAdapter.getCount();
    }

    private void changeSection(int level) {
        int oldLevel = mCurrentSection;
        mCurrentSection = level;
        if (onSectionChanged(oldLevel, level)) {
            reloadBlockDefinitions();
            reloadToolbox();
        } else {
            mCurrentSection = oldLevel;
        }
    }
}
