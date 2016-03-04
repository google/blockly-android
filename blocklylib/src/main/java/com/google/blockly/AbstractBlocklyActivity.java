/*
 *  Copyright 2016 Google Inc. All Rights Reserved.
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
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.blockly.control.BlocklyController;
import com.google.blockly.model.BlockFactory;
import com.google.blockly.model.BlocklySerializerException;
import com.google.blockly.model.Workspace;
import com.google.blockly.utils.CodeGenerationRequest;
import com.google.blockly.utils.StringOutputStream;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * Base class for a Blockly activities that use a material design style tool bar, and optionally a
 * navigation menu.
 * <p/>
 * Configure Block by providing defintions for {@link #getBlockDefinitionsJsonPaths()},
 * {@link #getToolboxContentsXmlPath()}, and {@link #getGeneratorsJsPaths()}.  An initial
 * workspace can be defined by overriding {@link #getStartingWorkspacePath()}.
 * <p/>
 * The central app views can be replaced by overloading {@link #onCreateContentView} and the
 * navigation menu will automatically be configured if {@link #onCreateAppNavigationDrawer} returns
 * a view.  By default, {@link #onCreateFragments()} looks for the {@link WorkspaceFragment}, the
 * {@link ToolboxFragment}, and the {@link TrashFragment} via ids {@link R.id#blockly_workspace},
 * {@link R.id#blockly_toolbox}, and {@link R.id#blockly_trash}, respectively.  If this is not the
 * same in your layout, make sure you override {@link #onCreateFragments()}, too.
 */
public abstract class AbstractBlocklyActivity extends AppCompatActivity {
    /**
     * Per the design guidelines, you should show the drawer on launch until the user manually
     * expands it. This shared preference tracks this.
     */
    private static final String PREF_USER_LEARNED_DRAWER = "navigation_drawer_learned";

    private static final String TAG = "AbstractBlocklyActivity";

    public static final String DEFAULT_WORKSPACE_FILENAME = "workspace.xml";

    protected ActionBar mActionBar;
    protected DrawerLayout mDrawerLayout;
    protected WorkspaceFragment mWorkspaceFragment;
    protected ToolboxFragment mToolboxFragment;
    protected TrashFragment mTrashFragment;

    // These two may be null if {@link #onCreateAppNavigationDrawer} returns null.
    protected View mNavigationDrawer;
    protected ActionBarDrawerToggle mDrawerToggle;
    private boolean mUserLearnedDrawer;

    protected BlocklyController mController;

    // TODO(#282): Wrap service binding into a self-contained, reusable class.
    protected CodeGeneratorService mCodeGeneratorService;
    protected boolean mBound = false;

    /** Defines service binding callbacks. Passed to bindService(). */
    private final ServiceConnection mCodeGenerationConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mNavigationDrawer == null || !mDrawerLayout.isDrawerOpen(mNavigationDrawer)) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(getActionBarMenuResId(), menu);
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

        if (id == R.id.action_save) {
            onSaveWorkspace();
            return true;
        } else if (id == R.id.action_load) {
            onLoadWorkspace();
            return true;
        } else if (id == R.id.action_clear) {
            onClearWorkspace();
            return true;
        } else if (id == R.id.action_run) {
            onRunCode();
            return true;
        } else if (id == android.R.id.home && mNavigationDrawer != null) {
            setNavDrawerOpened(!isNavDrawerOpen());
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * @return Whether the navigation drawer is currently open.
     */
    public boolean isNavDrawerOpen() {
        return mNavigationDrawer != null && mDrawerLayout.isDrawerOpen(mNavigationDrawer);
    }

    /**
     * Opens or closes the navigation drawer.
     * @param open Opens the navigation drawer if true and closes it if false.
     */
    public void setNavDrawerOpened(boolean open) {
        boolean alreadyOpen = mDrawerLayout.isDrawerOpen(mNavigationDrawer);
        if (open != alreadyOpen) {
            if (open) {
                mDrawerLayout.openDrawer(mNavigationDrawer);
            } else {
                mDrawerLayout.closeDrawer(mNavigationDrawer);
            }
            restoreActionBar();
        }
    }

    /**
     * Called when the user clicks the save action.  Default implementation delegates handling to
     * {@link #saveWorkspaceToAppDir(String)} using {@link #DEFAULT_WORKSPACE_FILENAME}.
     */
    public void onSaveWorkspace() {
        saveWorkspaceToAppDir(DEFAULT_WORKSPACE_FILENAME);
    }

    /**
     * Save the workspace to the given file in the application's private data directory.
     */
    public void saveWorkspaceToAppDir(String filename) {
        Workspace workspace = mWorkspaceFragment.getWorkspace();
        try {
            workspace.serializeToXml(openFileOutput(filename, Context.MODE_PRIVATE));
            Toast.makeText(getApplicationContext(), R.string.toast_workspace_saved,
                    Toast.LENGTH_LONG).show();
        } catch (FileNotFoundException | BlocklySerializerException e) {
            Toast.makeText(getApplicationContext(), R.string.toast_workspace_not_saved,
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Called when the user clicks the load action.  Default implementation delegates handling to
     * {@link #loadWorkspaceFromAppDir(String)}.
     */
    public void onLoadWorkspace() {
        loadWorkspaceFromAppDir(DEFAULT_WORKSPACE_FILENAME);
    }

    /**
     * Loads the workspace from the given file in the application's private data directory.
     */
    public void loadWorkspaceFromAppDir(String filename) {
        try {
            mController.loadWorkspaceContents(openFileInput(filename));
        } catch (FileNotFoundException e) {
            Toast.makeText(getApplicationContext(), R.string.toast_workspace_file_not_found,
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Called when the user clicks the clear action.  Default implementation resets the
     * workspace, removing all blocks from the workspace, and then calls
     * {@link #onInitBlankWorkspace()}.
     */
    public void onClearWorkspace() {
        mController.resetWorkspace();
        onInitBlankWorkspace();
    }

    /**
     * Saves a snapshot of the workspace to {@code outState}.
     *
     * @param outState The {@link Bundle} to save to.
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        onSaveWorkspaceSnapshot(outState);
    }

    /**
     * @return The {@link BlocklyController} controlling the workspace in this activity.
     */
    public final BlocklyController getController() {
        return mController;
    }

    /**
     * Handles the back button.  Default implementation attempts to close the navigation menu, then
     * the toolbox, then the trash, before allowing the system to back out of the activity.
     *
     * @see #onBackToCloseNavMenu()
     * @see #onBackToCloseToolbox()
     * @see #onBackToCloseTrash()
     */
    @Override
    public void onBackPressed() {
        // Try to close any open drawer / toolbox before backing out of the Activity.
        if (!onBackToCloseNavMenu() && !onBackToCloseToolbox() && !onBackToCloseTrash()) {
            super.onBackPressed();
        }
    }

    /**
     * Creates the Activity Views, Fragments, and Blocklycontroller via a sequence of calls to
     * {@link #onCreateActivityRootView()}, {@link #onCreateFragments()}, and
     * {@link #onCreateController}.  Subclasses should prefer to override those classes.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        onCreateActivityRootView();
        onCreateFragments();
        if (mWorkspaceFragment == null) {
            throw new IllegalStateException("mWorkspaceFragment is null");
        }
        mController = onCreateController();

        boolean loadedPriorInstance = checkAllowRestoreBlocklyState(savedInstanceState)
                && mController.onRestoreSnapshot(savedInstanceState);
        if (!loadedPriorInstance) {
            onLoadInitialWorkspace();
        }
    }

    /**
     * Creates the BlocklyController, if necessary, and configures it. This must be called during
     * {@link #onCreate}. It may also be called while the activity is running to reconfigure the
     * controller.
     */
    @NonNull
    protected BlocklyController onCreateController() {
        String toolboxPath = getToolboxContentsXmlPath();
        List<String> blockDefsPaths = getBlockDefinitionsJsonPaths();

        BlocklyController.Builder builder = new BlocklyController.Builder(this)
                .setBlocklyStyle(getStyleResId())
                .setAssetManager(getAssets())
                .addBlockDefinitionsFromAssets(blockDefsPaths)
                .setToolboxConfigurationAsset(toolboxPath)
                .setWorkspaceFragment(mWorkspaceFragment)
                .setTrashFragment(mTrashFragment)
                .setToolboxFragment(mToolboxFragment, mDrawerLayout)
                .setFragmentManager(getSupportFragmentManager());

        return builder.build();
    }

    /**
     * Returns true if the app should proceed to restore the blockly state from the
     * {@code savedInstanceState} Bundle. By default, it always returns true, but Activity
     * developers can override this method to add conditional logic.
     * <p/>
     * This does not prevent the state from saving to a Bundle during {@link #onSaveInstanceState}.
     *
     * @param savedInstanceState The Bundle to restore state from.
     * @return True if Blockly state should be restored. Otherwise, null.
     */
    protected boolean checkAllowRestoreBlocklyState(Bundle savedInstanceState) {
        return true;
    }

    /**
     * Hook for subclasses to load an initial workspace. Default implementation just calls
     * {@link #onInitBlankWorkspace()}.
     */
    protected void onLoadInitialWorkspace() {
        onInitBlankWorkspace();
    }

    /**
     * Hook for subclasses to initialize a new blank workspace. Initialization may include
     * configuring default variables or other setup.
     */
    protected void onInitBlankWorkspace() {}

    /**
     * @return The id of the menu resource used to populate the {@link ActionBar}.
     */
    protected int getActionBarMenuResId() {
        return R.menu.blockly_default_actionbar;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = new Intent(this, CodeGeneratorService.class);
        bindService(intent, mCodeGenerationConnection, Context.BIND_AUTO_CREATE);

        if (mNavigationDrawer != null) {
            // Read in the flag indicating whether or not the user has demonstrated awareness of the
            // drawer. See PREF_USER_LEARNED_DRAWER for details.
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
            mUserLearnedDrawer = sp.getBoolean(PREF_USER_LEARNED_DRAWER, false);
            if (!mUserLearnedDrawer) {
                mDrawerLayout.openDrawer(mNavigationDrawer);
            }
        }
    }

    /**
     * Saves a snapshot of the current workspace.  Called during {@link #onSaveInstanceState}. By
     * default, it just calls {@link BlocklyController#onSaveSnapshot}, but subclasses can overload
     * it change the behavior (e.g., only save based on some condition.).
     *
     * @param outState
     */
    protected void onSaveWorkspaceSnapshot(Bundle outState) {
        mController.onSaveSnapshot(outState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(mCodeGenerationConnection);
    }

    /**
     * @return The name to show in the {@link ActionBar}.  Defaults to the activity name.
     */
    @NonNull
    protected CharSequence getWorkspaceTitle() {
        return getTitle();
    }

    /**
     * @return The asset path for the xml toolbox config.
     */
    @NonNull
    abstract protected String getToolboxContentsXmlPath();

    /**
     * @return The asset path for the json block definitions.
     */
    @NonNull
    abstract protected List<String> getBlockDefinitionsJsonPaths();

    /**
     * Returns the asset file paths to the generators (JS files) to use for the most
     * recently requested "Run" action. Called from {@link #onRunCode()}.This is expected to be a
     * list of JavaScript files that contain the block generators.
     *
     * @return The list of file paths to the block generators.
     */
    @NonNull
    abstract protected List<String> getGeneratorsJsPaths();

    /**
     * Returns a generation callback to use for the most recently requested "Run" action.
     * Called from {@link #onRunCode()}.
     *
     * @return The generation callback.
     */
    @NonNull
    abstract protected CodeGenerationRequest.CodeGeneratorCallback getCodeGenerationCallback();

    /**
     * Returns the asset path to the initial workspace to load.  If null, no workspace file will be
     * loaded.
     *
     * @return The asset path to the initial workspace to load.
     */
    @Nullable
    protected String getStartingWorkspacePath() {
        return null;
    }

    /**
     * Returns a style to override the application's theme with when rendering Blockly. If 0 is
     * returned the activity or application's theme will be used, with attributes defaulting to
     * those in {@link R.style#BlocklyTheme}.
     *
     * @return A style that inherits from {@link R.style#BlocklyTheme} or 0.
     */
    protected int getStyleResId() {
        return 0;
    }

    /**
     * Creates or loads the root content view (by default, {@link R.layout#drawers_and_action_bar})
     * for the Activity.  It is also responsible for assigning {@link #mActionBar} and
     * {@link #mDrawerLayout}, and adding the view returned by {@link #onCreateContentView}.
     */
    protected void onCreateActivityRootView() {
        setContentView(R.layout.drawers_and_action_bar);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        mActionBar = getSupportActionBar();
        mActionBar.setDisplayShowTitleEnabled(true);

        // Create and attach content view into content container.  If content is a fragment, content
        // will be null here and the container will be populated during the FragmentTransaction.
        View content = onCreateContentView(R.id.content_container);
        if (content != null) {
            FrameLayout contentContainer = (FrameLayout) findViewById(R.id.content_container);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            if (content.getParent() != contentContainer) {
                contentContainer.addView(content, lp);
            } else {
                content.setLayoutParams(lp);
            }
        }

        mNavigationDrawer = onCreateAppNavigationDrawer();
        if (mNavigationDrawer != null) {
            setupAppNaviagtionDrawer();
        }
    }

    /**
     * Constructs (or inflates) the primary content view of the Activity.
     *
     * @param containerId The container id to target if using a {@link Fragment}
     * @return The {@link View} constructed. If using a {@link Fragment}, return null.
     */
    protected View onCreateContentView(int containerId) {
        return getLayoutInflater().inflate(R.layout.blockly_unified_workspace, null);
    }

    /**
     * @return The {@link View} to be used for the navigation menu. Otherwise null.
     */
    protected View onCreateAppNavigationDrawer() {
        return null;
    }

    /**
     * Configures the activity to support a navigation menu and drawer provided by
     * {@link #onCreateAppNavigationDrawer}.
     */
    protected void setupAppNaviagtionDrawer() {
        DrawerLayout.LayoutParams lp = new DrawerLayout.LayoutParams(
                getResources().getDimensionPixelSize(R.dimen.navigation_drawer_width),
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.START);
        // Add navigation drawer above the content view, as the first drawer.
        mDrawerLayout.addView(mNavigationDrawer, 1, lp);
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow,
                GravityCompat.START);

        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setHomeButtonEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the navigation drawer and the action bar app icon.
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.string.navigation_drawer_open,  /* "open drawer" description for accessibility */
                R.string.navigation_drawer_close  /* "close drawer" description for accessibility */
        ) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);

                supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                if (!mUserLearnedDrawer) {
                    // The user manually opened the drawer; store this flag to prevent auto-showing
                    // the navigation drawer automatically in the future.
                    mUserLearnedDrawer = true;
                    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(
                            AbstractBlocklyActivity.this);
                    sp.edit().putBoolean(PREF_USER_LEARNED_DRAWER, true).apply();
                }

                supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }
        };

        // Defer code dependent on restoration of previous instance state.
        mDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                mDrawerToggle.syncState();
            }
        });
        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    /**
     * Creates the Views and Fragments before the BlocklyController is constructed.  Override to
     * load a custom View hierarchy.  Responsible for assigning
     * {@link #mWorkspaceFragment}, and optionally, {@link #mToolboxFragment} and
     * {@link #mTrashFragment}. This base implementation attempts to acquire references to the
     * {@link #mToolboxFragment} and {@link #mTrashFragment} using the layout ids
     * {@link R.id#} and {@link R.id#blockly_trash}, respectively. Subclasses may leave these
     * {@code null} if the views are not present in the UI.
     * <p>
     * Always called once from {@link #onCreate} and before {@link #onCreateController}.
     */
    protected void onCreateFragments() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        mWorkspaceFragment = (WorkspaceFragment)
                fragmentManager.findFragmentById(R.id.blockly_workspace);
        mToolboxFragment =
                (ToolboxFragment) fragmentManager.findFragmentById(R.id.blockly_toolbox);
        mTrashFragment = (TrashFragment) fragmentManager.findFragmentById(R.id.blockly_trash);

        if (mTrashFragment != null) {
            if (mTrashFragment.isCloseable()) {
                mTrashFragment.setOpened(false);

                mWorkspaceFragment.setTrashClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mTrashFragment.setOpened(true);
                    }
                });
            } else {
                // TODO(#14): Don't show trashcan
            }
        }
    }

    /**
     * Runs the code generator. Called when user selects "Run" action.
     * @see #getCodeGenerationCallback()
     */
    protected void onRunCode() {
        try {
            if (mBound) {
                final StringOutputStream serialized = new StringOutputStream();
                mWorkspaceFragment.getWorkspace().serializeToXml(serialized);

                mCodeGeneratorService.requestCodeGeneration(
                        new CodeGenerationRequest(serialized.toString(),
                                getCodeGenerationCallback(),
                                getBlockDefinitionsJsonPaths(),
                                getGeneratorsJsPaths()));
            }
        } catch (BlocklySerializerException e) {
            Log.wtf(TAG, e);
            Toast.makeText(getApplicationContext(), "Code generation failed.",
                    Toast.LENGTH_LONG).show();

        }
    }

    /**
     * Restores the {@link ActionBar} contents when the navigation window closes, per <a
     * href="http://developer.android.com/design/material/index.html">Material design
     * guidelines</a>.
     */
    protected void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(getWorkspaceTitle());
    }

    /**
     * Reloads the block definitions and toolbox contents.
     * @see #getToolboxContentsXmlPath()
     */
    protected void reloadToolbar() {
        AssetManager assetManager = getAssets();
        String toolboxPath = getToolboxContentsXmlPath();

        BlocklyController controller = getController();
        try {
            controller.loadToolboxContents(assetManager.open(getToolboxContentsXmlPath()));
        } catch (IOException e) {
            throw new IllegalArgumentException("Error opening toolbox at " + toolboxPath, e);
        }
    }

    /**
     * Reloads the block definitions.
     * @see #getBlockDefinitionsJsonPaths()
     */
    protected void reloadBlockDefinitions() {
        AssetManager assetManager = getAssets();
        List<String> blockDefsPaths = getBlockDefinitionsJsonPaths();

        BlockFactory factory = getController().getBlockFactory();
        factory.clear();

        String blockDefsPath = null;
        try {
            Iterator<String> iter = blockDefsPaths.iterator();
            while (iter.hasNext()) {
                blockDefsPath = iter.next();
                factory.addBlocks(assetManager.open(blockDefsPath));
            }
        } catch (IOException e) {
            factory.clear();  // Clear any partial loaded block sets.
            throw new IllegalArgumentException("Error opening block defs at " + blockDefsPath, e);
        }
    }


    /**
     * @return True if the action consumed to close a previously open navigation menu. Otherwise
     *         false.
     */
    protected boolean onBackToCloseNavMenu() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
            return true;
        }
        return false;
    }

    /**
     * @return True if the action was handled to close a previously open (and closable) toolbox.
     *         Otherwise false.
     */
    protected boolean onBackToCloseToolbox() {
        return mToolboxFragment != null
                && mToolboxFragment.isCloseable()
                && mToolboxFragment.closeBlocksDrawer();
    }

    /**
     * @return True if the action was handled to close a previously open (and closable) trash.
     *         Otherwise false.
     */
    protected boolean onBackToCloseTrash() {
        return mTrashFragment != null
                && mTrashFragment.isCloseable()
                && mTrashFragment.setOpened(false);
    }
}
