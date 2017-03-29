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

package com.google.blockly.android;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.util.ArrayMap;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.google.blockly.android.codegen.CodeGenerationRequest;
import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.android.ui.BlockViewFactory;
import com.google.blockly.model.BlockExtension;
import com.google.blockly.model.DefaultBlocks;
import com.google.blockly.model.BlockFactory;
import com.google.blockly.model.Mutator;
import com.google.blockly.utils.BlockLoadingException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for a Blockly activity that use a material design style tool bar, and optionally a
 * navigation menu.
 * <p/>
 * The default layout is filled with a workspace and the toolbox and trash each configured as
 * fly-out views.  Everything below the {@link ActionBar} can be replaced by overriding
 * {@link #onCreateContentView}. After {@link #onCreateContentView}, a {@link BlocklyActivityHelper}
 * is constructed to help initialize the Blockly fragments, controller, and supporting UI. If
 * overriding {@link #onCreateContentView} without {@code unified_blockly_workspace.xml} or
 * otherwise using standard blockly fragment and view ids ({@link R.id#blockly_workspace},
 * {@link R.id#blockly_toolbox_ui}, {@link R.id#blockly_trash_ui}, etc.), override
 * {@link #onCreateActivityHelper()} and {@link BlocklyActivityHelper#onCreateFragments()}
 * appropriately.
 * <p/>
 * Once the controller and fragments are configured, if {@link #checkAllowRestoreBlocklyState}
 * returns true, the activity will attempt to load a prior workspace from the instance state
 * bundle.  If no workspace is loaded, it defers to {@link #onLoadInitialWorkspace}.
 * <p/>
 * Configure the workspace by providing definitions for {@link #getBlockDefinitionsJsonPaths()},
 * {@link #getToolboxContentsXmlPath()}. Alternate {@link BlockViewFactory}s are supported via
 * {@link BlocklyActivityHelper#onCreateBlockViewFactory}. An initial workspace can be loaded during
 * {@link #onLoadInitialWorkspace()}.
 * <p/>
 * The block definitions can be updated at any time by calling {@link #resetBlockFactory()},
 * which triggers another call to {@link #getBlockDefinitionsJsonPaths()}.  Similarly, The toolbox
 * can be reloaded by calling  {@link #reloadToolbox()}, which triggers another call to
 * {@link #getToolboxContentsXmlPath()}.
 */
public abstract class AbstractBlocklyActivity extends AppCompatActivity {
    /**
     * Per the design guidelines, you should show the drawer on launch until the user manually
     * expands it. This shared preference tracks this.
     */
    private static final String PREF_USER_LEARNED_DRAWER = "navigation_drawer_learned";

    private static final String TAG = "AbstractBlocklyActivity";

    public static final Map<String, BlockExtension> DEFAULT_EXTENSIONS = new ArrayMap<>();
    public static final Map<String, Mutator.Factory> DEFAULT_MUTATORS = new ArrayMap<>();

    static {
        // TODO Set up default extensions and mutators
    }

    protected BlocklyActivityHelper mBlockly;

    protected ActionBar mActionBar;
    protected DrawerLayout mDrawerLayout;

    // These two may be null if {@link #onCreateAppNavigationDrawer} returns null.
    protected View mNavigationDrawer;
    protected ActionBarDrawerToggle mDrawerToggle;

    private boolean mUserLearnedDrawer;

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
            if (getController().getWorkspace().hasBlocks()) {
                onRunCode();
            } else {
                Log.i(TAG, "No blocks in workspace. Skipping run request.");
            }
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
     * {@link BlocklyActivityHelper#saveWorkspaceToAppDir(String)} using
     * {@link #getWorkspaceSavePath()}.
     */
    public void onSaveWorkspace() {
        mBlockly.saveWorkspaceToAppDirSafely(getWorkspaceSavePath());
    }

    /**
     * Save the workspace to the given file in the application's private data directory.
     * @deprecated Call {@code mBlockly.saveWorkspaceToAppDir(filename)} or
     *             {@code mBlockly.saveWorkspaceToAppDirSafely(filename)}.
     */
    public void saveWorkspaceToAppDir(String filename) {
        mBlockly.saveWorkspaceToAppDirSafely(filename);
    }

    /**
     * Called when the user clicks the load action.  Default implementation delegates handling to
     * {@link BlocklyActivityHelper#loadWorkspaceFromAppDir(String)}.
     */
    public void onLoadWorkspace() {
        mBlockly.loadWorkspaceFromAppDirSafely(getWorkspaceSavePath());
    }

    /**
     * Loads the workspace from the given file in the application's private data directory.
     * @deprecated Call {@code mBlockly.loadWorkspaceFromAppDir(filename)} or
     *             {@code mBlockly.loadWorkspaceFromAppDirSafely(filename)}.
     */
    @Deprecated
    public void loadWorkspaceFromAppDir(String filename) {
        mBlockly.loadWorkspaceFromAppDirSafely(filename);
    }

    /**
     * Called when the user clicks the clear action.  Default implementation resets the
     * workspace, removing all blocks from the workspace, and then calls
     * {@link #onInitBlankWorkspace()}.
     */
    public void onClearWorkspace() {
        getController().resetWorkspace();
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
        getController().onSaveSnapshot(outState);
    }

    /**
     * @return The {@link BlocklyController} controlling the workspace in this activity.
     */
    public final BlocklyController getController() {
        return mBlockly.getController();
    }

    /**
     * Handles the back button.  Default implementation attempts to close the navigation menu, then
     * the toolbox and trash flyouts, before allowing the system to back out of the activity.
     *
     * @see #onBackToCloseNavMenu()
     * @see BlocklyActivityHelper#onBackToCloseFlyouts()
     */
    @Override
    public void onBackPressed() {
        // Try to close any open drawer / toolbox before backing out of the Activity.
        if (!onBackToCloseNavMenu() && !mBlockly.onBackToCloseFlyouts()) {
            super.onBackPressed();
        }
    }

    /**
     * Creates the activity's views and fragments (via {@link #onCreateActivityRootView}, and then
     * initializes Blockly via {@link #onCreateActivityHelper()}, using the values from
     * {@link #getBlockDefinitionsJsonPaths} and {@link #getToolboxContentsXmlPath}.
     * Subclasses should override those methods to configure the Blockly environment.
     * <p/>
     * Once the controller and fragments are configured, if {@link #checkAllowRestoreBlocklyState}
     * returns true, the activity will attempt to load a prior workspace from the instance state
     * bundle.  If no workspace is loaded, it defers to {@link #onLoadInitialWorkspace}.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        onCreateActivityRootView();
        mBlockly = onCreateActivityHelper();
        if (mBlockly == null) {
            throw new IllegalStateException("BlocklyActivityHelper is null. "
                    + "onCreateActivityHelper must return a instance.");
        }
        resetBlockFactory();  // Initial load of block definitions, extensions, and mutators.
        reloadToolbox();

        // Load the workspace.
        boolean loadedPriorInstance = checkAllowRestoreBlocklyState(savedInstanceState)
                && getController().onRestoreSnapshot(savedInstanceState);
        if (!loadedPriorInstance) {
            onLoadInitialWorkspace();
        }
    }

    /**
     * Create a {@link BlocklyActivityHelper} to use for this Activity.
     */
    protected BlocklyActivityHelper onCreateActivityHelper() {
        return new BlocklyActivityHelper(this);
    }

    /** Propagate lifecycle event to BlocklyActivityHelper. */
    @Override
    protected void onStart() {
        super.onStart();
        mBlockly.onStart();
    }

    /** Propagate lifecycle event to BlocklyActivityHelper. */
    @Override
    protected void onPause() {
        super.onPause();
        mBlockly.onPause();
    }

    /** Propagate lifecycle event to BlocklyActivityHelper. */
    @Override
    protected void onResume() {
        super.onResume();
        mBlockly.onResume();

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

    /** Propagate lifecycle event to BlocklyActivityHelper. */
    @Override
    protected void onStop() {
        super.onStop();
        mBlockly.onStop();
    }

    /** Propagate lifecycle event to BlocklyActivityHelper. */
    @Override
    protected void onRestart() {
        super.onRestart();
        mBlockly.onRestart();
    }

    /**
     *
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
        getController().closeFlyouts();
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

    /**
     * Saves a snapshot of the current workspace.  Called during {@link #onSaveInstanceState}. By
     * default, it just calls {@link BlocklyController#onSaveSnapshot}, but subclasses can overload
     * it change the behavior (e.g., only save based on some condition.).
     *
     * @param bundle
     * @deprecated Call {@code getController().onSaveSnapshot(bundle);}
     */
    protected void onSaveWorkspaceSnapshot(Bundle bundle) {
        getController().onSaveSnapshot(bundle);
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
     * Loads the list of {@link BlockExtension}s that support the block definitions in this
     * activity. By default, returns a mutable version of {@link DefaultBlocks#getExtensions()} so
     * subclasses can easily append their own extensions. Called from {@link #resetBlockFactory()}.
     *
     * @return A list of extensions to use for future blocks. Null is treated like an empty list.
     */
    @Nullable
    protected Map<String, BlockExtension> getBlockExtensions() {
        // Create a new instance so it is easy to append by subclasses.  Not called very often.
        Map<String, BlockExtension> extensions = new ArrayMap<>();
        extensions.putAll(DefaultBlocks.getExtensions());
        return extensions;
    }

    /**
     * Loads the list of {@link Mutator.Factory}s that support the block definitions in this
     * activity. By default, returns a mutable versions of {@link DefaultBlocks#getMutators()} so
     * subclasses can easily append their own mutators. Called from {@link #resetBlockFactory()}.
     *
     * @return A list of mutator factories to use for future blocks. Null is treated like an empty
     *         list.
     */
    @Nullable
    protected Map<String, Mutator.Factory> getMutators() {
        // Create a new instance so it is easy to append by subclasses.  Not called very often.
        Map<String, Mutator.Factory> mutators = new ArrayMap<>();
        mutators.putAll(DefaultBlocks.getMutators());
        return mutators;
    }


    /**
     * @return The list of {@link Mutator Mutators} to use for building blocks.
     */
    @NonNull
    protected Map<String, Mutator.Factory> getBlockMutators() {
        return new HashMap<>(Mutator.STANDARD_MUTATORS);
    }

    /**
     * Returns the asset file paths to the generators (JS files) to use for the most
     * recently requested "Run" action. Called from {@link #onRunCode()}. This is expected to be a
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
     * @return The path to the saved workspace file on the local device. By default,
     *         "workspace.xml".
     */
    @NonNull
    protected String getWorkspaceSavePath() {
        return "workspace.xml";
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
        mDrawerLayout.addDrawerListener(mDrawerToggle);
    }

    /**
     * Runs the code generator. Called when user selects "Run" action.
     * <p/>
     * Gets the latest block definitions and generator code by calling
     * {@link #getBlockDefinitionsJsonPaths()} and {@link #getGeneratorsJsPaths()} just before
     * invoking generation.
     *
     * @see #getCodeGenerationCallback()
     */
    protected void onRunCode() {
        mBlockly.requestCodeGeneration(
            getBlockDefinitionsJsonPaths(),
            getGeneratorsJsPaths(),
            getCodeGenerationCallback());
    }

    /**
     * Restores the {@link ActionBar} contents when the navigation window closes, per <a
     * href="http://developer.android.com/design/material/index.html">Material design
     * guidelines</a>.
     */
    protected void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(getWorkspaceTitle());
        }
    }

    /**
     * Reloads the toolbox contents using the path provided by {@link #getToolboxContentsXmlPath()}.
     */
    protected void reloadToolbox() {
        mBlockly.reloadToolbox(getToolboxContentsXmlPath());
    }

    /**
     * Reloads the block definitions, including extensions and mutators. Calls
     * {@link #getBlockDefinitionsJsonPaths()} and {@link #getBlockExtensions()}.
     *
     * @throws IOException If there is a fundamental problem with the input.
     * @throws BlockLoadingException If the definition is malformed.
     */
    protected void resetBlockFactory() {
        mBlockly.resetBlockFactory(
                getBlockDefinitionsJsonPaths(),
                getBlockExtensions(),
                getMutators());
    }

    /**
     * @return True if the navigation menu was closed and the back event should be consumed.
     *         Otherwise false.
     */
    protected boolean onBackToCloseNavMenu() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
            return true;
        }
        return false;
    }
}
