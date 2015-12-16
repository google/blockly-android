/*
 *  Copyright  2015 Google Inc. All Rights Reserved.
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

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.AssetManager;
import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.Toast;

import com.google.blockly.model.BlockFactory;
import com.google.blockly.model.BlocklySerializerException;
import com.google.blockly.model.Workspace;
import com.google.blockly.utils.CodeGenerationRequest;
import com.google.blockly.utils.StringOutputStream;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Activity holding a full blockly workspace with multiple sections, a toolbox, and a trash can.
 */
public class BlocklySectionsActivity extends AbsBlocklyActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {
    private static final String TAG = "BlocklySectionsActivity";

    protected CodeGenerationRequest.CodeGeneratorCallback mCodeGeneratorCallback =
            new CodeGenerationRequest.CodeGeneratorCallback() {
                @Override
                public void onFinishCodeGeneration(String generatedCode) {
                    // Sample callback.
                    if (generatedCode.isEmpty()) {
                        Toast.makeText(getApplicationContext(),
                                "Something went wrong while we were lovingly handcrafting your" +
                                        " artisan code", Toast.LENGTH_LONG).show();
                    } else {
                        Log.d(TAG, "code: " + generatedCode);
                        Toast.makeText(getApplicationContext(), generatedCode,
                                Toast.LENGTH_LONG).show();
                    }
                }
            };

    protected WorkspaceFragment mWorkspaceFragment;

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;
    private ListAdapter mLevelsAdapter;
    private ToolboxFragment mToolboxFragment;
    private DrawerLayout mDrawerLayout;
    private TrashFragment mOscar;
    private CodeGeneratorService mCodeGeneratorService;
    private boolean mBound = false;
    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private final ServiceConnection mCodeGenerationConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            CodeGeneratorService.CodeGeneratorBinder binder =
                    (CodeGeneratorService.CodeGeneratorBinder) service;
            mCodeGeneratorService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
            mCodeGeneratorService = null;
        }
    };

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;
    private int mCurrentPosition;

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        if (position == mCurrentPosition) {
            return;
        }

        changeLevel(position);
        mCurrentPosition = position;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        } else if (id == R.id.action_save) {
            Workspace workspace = mWorkspaceFragment.getWorkspace();
            try {
                workspace.serializeToXml(openFileOutput("workspace.xml", Context.MODE_PRIVATE));
                Toast.makeText(getApplicationContext(), "Saved workspace contents",
                        Toast.LENGTH_LONG).show();
            } catch (FileNotFoundException | BlocklySerializerException e) {
                Toast.makeText(getApplicationContext(), "Couldn't save workspace.",
                        Toast.LENGTH_LONG).show();
            }
            return true;
        } else if (id == R.id.action_load) {
            Workspace workspace = mWorkspaceFragment.getWorkspace();
            try {
                workspace.loadFromXml(openFileInput("workspace.xml"));
                workspace.initBlockViews();
            } catch (FileNotFoundException e) {
                Toast.makeText(getApplicationContext(), "Couldn't find saved workspace.",
                        Toast.LENGTH_LONG).show();
            }
            return true;
        } else if (id == R.id.action_clear) {
            mWorkspaceFragment.getWorkspace().resetWorkspace();
            return true;
        } else if (id == R.id.action_airstrike) {
            mToolboxFragment.airstrike();
            return true;
        } else if (id == R.id.action_carpet_bomb) {
            mToolboxFragment.carpetBomb();
            return true;
        } else if (id == R.id.action_run) {
            try {
                if (mBound) {
                    final StringOutputStream serialized = new StringOutputStream();
                    mWorkspaceFragment.getWorkspace().serializeToXml(serialized);

                    mCodeGeneratorService.requestCodeGeneration(
                            new CodeGenerationRequest(serialized.toString(),
                                    mCodeGeneratorCallback,
                                    "sample_sections/definitions.json",
                                    "sample_sections/generators.js"));
                }
            } catch (BlocklySerializerException e) {
                Log.wtf(TAG, e);
                Toast.makeText(getApplicationContext(), "Code generation failed.",
                        Toast.LENGTH_LONG).show();

            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(mCodeGenerationConnection);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = new Intent(this, CodeGeneratorService.class);
        bindService(intent, mCodeGenerationConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * Apps should not override onCreate if using this class. If you have a completely custom view
     * extend from AbsBlocklyActivity instead and use this as an example for creating levels.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blockly);

        // Set up the workspace fragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        mWorkspaceFragment = (WorkspaceFragment) fragmentManager
                .findFragmentById(R.id.blockly_workspace_container);
        // The container only needs to be replaced the first time. When the activity is recreated
        // we can reuse the same fragment.
        if (mWorkspaceFragment == null) {
            mWorkspaceFragment = new WorkspaceFragment();
            fragmentManager.beginTransaction()
                    .replace(R.id.blockly_workspace_container, mWorkspaceFragment)
                    .commit();
        }

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        // Set up the drawer.
        mCurrentPosition = 0;
        mLevelsAdapter = onCreateSectionsAdapter();
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer, mDrawerLayout, mLevelsAdapter);

        mToolboxFragment =
                (ToolboxFragment) getSupportFragmentManager().findFragmentById(R.id.toolbox);

        // Set up the toolbox that lives inside the trash can.
        mOscar = (TrashFragment) getSupportFragmentManager().findFragmentById(R.id.trash);
        createWorkspace();
    }

    /**
     * Builds the workspace for this activity.
     * <p/>
     * Override to build a workspace with a custom configuration. If a custom layout is also being
     * used it must use the same ids for the fragments as those found in {@link
     * com.google.blockly.R.layout#activity_blockly}.
     */
    @Override
    protected Workspace onConfigureWorkspace() {
        int currLevel = getCurrentSection();
        String toolboxPath = getWorkspaceToolboxPath(currLevel);
        String blockDefsPath = getWorkspaceBlocksPath(currLevel);
        AssetManager assetManager = getAssets();

        Workspace workspace = getWorkspace();
        if (workspace == null) {
            Workspace.Builder bob = new Workspace.Builder(this);
            bob.setBlocklyStyle(R.style.BlocklyTheme);

            bob.setAssetManager(assetManager);
            bob.addBlockDefinitionsFromAsset(blockDefsPath);
            bob.setToolboxConfigurationAsset(toolboxPath);

            bob.setWorkspaceFragment(mWorkspaceFragment);
            bob.setTrashFragment(mOscar);
            bob.setToolboxFragment(mToolboxFragment, mDrawerLayout);
            bob.setFragmentManager(getSupportFragmentManager());

            bob.setStartingWorkspaceAsset("default/demo_workspace.xml");
            workspace = bob.build();
        } else {
            BlockFactory factory = workspace.getBlockFactory();
            factory.clear();

            try {
                factory.addBlocks(assetManager.open(blockDefsPath));
            } catch (IOException e) {
                throw new IllegalArgumentException("Error opening block defs at " + blockDefsPath,
                        e);
            }
            try {
                workspace.loadToolboxContents(assetManager.open(toolboxPath));
            } catch (IOException e) {
                throw new IllegalArgumentException("Error opening toolbox at " + toolboxPath, e);
            }
        }

        return workspace;
    }

    /**
     * @return The section that is currently displayed.
     */
    public final int getCurrentSection() {
        return mCurrentPosition;
    }

    /**
     * Returns the title to show in the action bar for this section.
     *
     * @param section The section to retrieve the title for.
     * @return The title of the section.
     */
    protected String getWorkspaceTitle(int section) {
        if (section < getSectionCount()) {
            return (String) mLevelsAdapter.getItem(section);
        } else {
            return "Secret cow level";
        }
    }

    /**
     * Called after a section has been loaded. If you don't want to re-use the previous section's
     * code {@link Workspace#loadFromXml(InputStream)} should be called here.
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
     * Creates an adapter for the sections drawer. This adapter is passed to {@link
     * android.widget.ListView#setAdapter(ListAdapter)}
     *
     * @return An adapter for the section items.
     */
    protected ListAdapter onCreateSectionsAdapter() {
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

    /**
     * Returns the path to the xml file that defines the toolbox for this section.
     *
     * @param section The section to retrieve the toolbox for.
     * @return The path for the xml toolbox config under assets.
     */
    protected String getWorkspaceToolboxPath(int section) {
        return "default/toolbox.xml";
    }

    /**
     * Returns the path to the json file that defines the blocks for this section.
     *
     * @param section The section to retrieve the block defs for.
     * @return The path for the json block definitions under assets.
     */
    protected String getWorkspaceBlocksPath(int section) {
        return "default/toolbox_blocks.json";
    }

    private void changeLevel(int level) {
        int oldLevel = mCurrentPosition;
        mCurrentPosition = level;
        mTitle = getWorkspaceTitle(level);
        createWorkspace();
        onSectionChanged(oldLevel, level);
    }

    private void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }
}
