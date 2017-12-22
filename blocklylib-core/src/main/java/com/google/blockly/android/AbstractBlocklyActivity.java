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
import android.support.v4.app.Fragment;
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
import com.google.blockly.android.codegen.LanguageDefinition;
import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.android.ui.BlockViewFactory;
import com.google.blockly.android.ui.MutatorFragment;
import com.google.blockly.model.BlockExtension;
import com.google.blockly.model.BlocklySerializerException;
import com.google.blockly.model.CustomCategory;
import com.google.blockly.model.DefaultBlocks;
import com.google.blockly.model.Mutator;
import com.google.blockly.utils.BlockLoadingException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

/**
 * Base class for a Blockly activity that use a material design style tool bar, and optionally a
 * navigation menu.
 * <p/>
 * The default layout is filled with a workspace and the toolbox and trash each configured as
 * fly-out views.  Everything below the {@link #mActionBar} can be replaced by overriding
 * {@link #onCreateContentView}. After {@link #onCreateContentView}, a {@link BlocklyActivityHelper}
 * is constructed to help initialize the Blockly fragments, controller, and supporting UI. If
 * overriding {@link #onCreateContentView} without {@code unified_blockly_workspace.xml} or
 * otherwise using standard blockly fragment and view ids ({@link R.id#blockly_workspace},
 * {@link R.id#blockly_toolbox_ui}, {@link R.id#blockly_trash_ui}, etc.), override
 * {@link #onCreateActivityHelper()} and {@link BlocklyActivityHelper#onFindFragments}
 * appropriately.
 * <p/>
 * Once the controller and fragments are configured, if {@link #checkAllowRestoreBlocklyState}
 * returns true, the activity will attempt to load a prior workspace from the instance state
 * bundle.  If no workspace is loaded, it defers to {@link #onLoadInitialWorkspace}.
 * <p/>
 * Configure the workspace by providing definitions for {@link #getBlockDefinitionsJsonPaths()} and
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

    protected BlocklyActivityHelper mBlocklyActivityHelper;

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

    /**
     * Handles the processing of Blockly's standard toolbar / actionbar menu items for this
     * workspace.
     * @param item One of Blockly's standard toolbar / actionbar menu items.
     * @return True if the item is recognized and the event was consumed. Otherwise false.
     */
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
        mBlocklyActivityHelper.saveWorkspaceToAppDirSafely(getWorkspaceSavePath());
    }

    /**
     * Called when the user clicks the load action.  Default implementation delegates handling to
     * {@link BlocklyActivityHelper#loadWorkspaceFromAppDir(String)} using
     * {@link #getWorkspaceSavePath()}.
     */
    public void onLoadWorkspace() {
        mBlocklyActivityHelper.loadWorkspaceFromAppDirSafely(getWorkspaceSavePath());
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
        return mBlocklyActivityHelper.getController();
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
        if (!onBackToCloseNavMenu() && !mBlocklyActivityHelper.onBackToCloseFlyouts()) {
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
        mBlocklyActivityHelper = onCreateActivityHelper();
        if (mBlocklyActivityHelper == null) {
            throw new IllegalStateException("BlocklyActivityHelper is null. "
                    + "onCreateActivityHelper must return a instance.");
        }
        resetBlockFactory();  // Initial load of block definitions, extensions, and mutators.
        configureCategoryFactories();  // After BlockFactory; before Toolbox
        reloadToolbox();

        // Load the workspace.
        boolean loadedPriorInstance = checkAllowRestoreBlocklyState(savedInstanceState)
                && (getController().onRestoreSnapshot(savedInstanceState) || onAutoload());
        if (!loadedPriorInstance) {
            onLoadInitialWorkspace();
        }
    }

    /**
     * Create a {@link BlocklyActivityHelper} to use for this Activity.
     */
    protected BlocklyActivityHelper onCreateActivityHelper() {
        return new BlocklyActivityHelper(this, this.getSupportFragmentManager());
    }

    /** Propagate lifecycle event to BlocklyActivityHelper. */
    @Override
    protected void onStart() {
        super.onStart();
        mBlocklyActivityHelper.onStart();
    }

    /** Propagate lifecycle event to BlocklyActivityHelper. */
    @Override
    protected void onPause() {
        super.onPause();
        mBlocklyActivityHelper.onPause();
        onAutosave();
    }

    /** Propagate lifecycle event to BlocklyActivityHelper. */
    @Override
    protected void onResume() {
        super.onResume();
        mBlocklyActivityHelper.onResume();

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
        mBlocklyActivityHelper.onStop();
    }

    /** Propagate lifecycle event to BlocklyActivityHelper. */
    @Override
    protected void onRestart() {
        super.onRestart();
        mBlocklyActivityHelper.onRestart();
    }

    /**
     *
     * Returns true if the app should proceed to restore the blockly state from the
     * {@code savedInstanceState} Bundle or the {@link #onAutoload() auto save} file. By default, it
     * always returns true, but Activity developers can override this method to add conditional
     * logic.
     * <p/>
     * This does not prevent the state from saving to a Bundle during {@link #onSaveInstanceState}
     * or saving to a file in {@link #onAutosave()}.
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
     * Called when an autosave of the workspace is triggered, typically by {@link #onPause()}.
     * By default this saves the workspace to a file in the app's directory.
     */
    protected void onAutosave() {
        try {
            mBlocklyActivityHelper.saveWorkspaceToAppDir(getWorkspaceAutosavePath());
        } catch (FileNotFoundException | BlocklySerializerException e) {
            Log.e(TAG, "Failed to autosaving workspace.", e);
        }
    }

    /**
     * Called when the activity tries to restore the autosaved workspace, typically by
     * {@link #onCreate(Bundle)} if there was no workspace data in the bundle.
     *
     * @return true if a previously saved workspace was loaded, false otherwise.
     */
    protected boolean onAutoload() {
        String filePath = getWorkspaceAutosavePath();
        try {
            mBlocklyActivityHelper.loadWorkspaceFromAppDir(filePath);
            return true;
        } catch (FileNotFoundException e) {
            // No workspace was saved previously.
        } catch (BlockLoadingException | IOException e) {
            Log.e(TAG, "Failed to load workspace", e);
            mBlocklyActivityHelper.getController().resetWorkspace();

            File file = getFileStreamPath(filePath);
            if (!file.delete()) {
                Log.e(TAG, "Failed to delete corrupted autoload workspace: " + filePath);
            }
        }
        return false;
    }

    /**
     * Hook for subclasses to initialize a new blank workspace. Initialization may include
     * configuring default variables or other setup.
     */
    protected void onInitBlankWorkspace() {}

    /**
     * @return The id of the menu resource used to populate the {@link #mActionBar}.
     */
    protected int getActionBarMenuResId() {
        return R.menu.blockly_default_actionbar;
    }

    /**
     * @return The name to show in the {@link #mActionBar}.  Defaults to the activity name.
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
     * @return The asset path for the core language file used to generate code.
     */
    @NonNull
    protected LanguageDefinition getBlockGeneratorLanguage() {
        return DefaultBlocks.LANGUAGE_DEFINITION;
    }

    /**
     * This method provides a hook to register {@link BlockExtension}s that support the block
     * definitions in this activity. By default, it adds all extensions in
     * {@link DefaultBlocks#getExtensions() DefaultBlocks} to the block factory, via the
     * {@link #onCreateActivityHelper() BlocklyActivityHelper}
     * {@link BlocklyActivityHelper#configureExtensions() implementation}.
     * <p/>
     * Extensions with the same key will replace existing extensions, so it is safe
     * to call super and then update specific extensions.
     * <p/>
     * Called from {@link #resetBlockFactory()}.
     */
    protected void configureBlockExtensions() {
        mBlocklyActivityHelper.configureExtensions();
    }

    /**
     * This method provides a hook to register {@link Mutator.Factory}s and
     * {@link MutatorFragment.Factory}s that support the block definitions in this activity. By
     * default, it adds the mutators in {@link DefaultBlocks#getMutators() DefaultBlocks} to the
     * BlockFactory, via the {@link #onCreateActivityHelper() BlocklyActivityHelper}
     * {@link BlocklyActivityHelper#configureMutators() implementation}.
     * <p/>
     * Mutators with the same key will replace existing mutators, so it is safe
     * to call super and then update specific mutators.
     * <p/>
     * Called from {@link #resetBlockFactory()}.
     */
    protected void configureMutators() {
        mBlocklyActivityHelper.configureMutators();
    }

    /**
     * This method provides a hook to register custom {@link CustomCategory}s that support
     * the toolboxes in this activity. By default, it registers the categories in
     * {@link DefaultBlocks}, via the {@link #onCreateActivityHelper() BlocklyActivityHelper}
     * {@link BlocklyActivityHelper#configureMutators() implementation}.
     * <p/>
     * Category factories with the same {@code custom} key will replace existing
     * {@link CustomCategory}s, so it is safe to call super and then update specific categories.
     * <p/>
     * Called once at activity creation.
     */
    protected void configureCategoryFactories() {
        mBlocklyActivityHelper.configureCategoryFactories();
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
     * @return The path to the automatically saved workspace file on the local device. By default,
     *         "autosave_workspace.xml".
     */
    @NonNull
    protected String getWorkspaceAutosavePath() {
        return "autosave_workspace.xml";
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
        mBlocklyActivityHelper.requestCodeGeneration(
                getBlockGeneratorLanguage(),
                getBlockDefinitionsJsonPaths(),
                getGeneratorsJsPaths(),
                getCodeGenerationCallback());
    }

    /**
     * Restores the {@link #mActionBar} contents when the navigation window closes, per <a
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
        mBlocklyActivityHelper.reloadToolbox(getToolboxContentsXmlPath());
    }

    /**
     * Reloads the block definitions, including extensions and mutators. Calls
     * {@link #getBlockDefinitionsJsonPaths()} and {@link #configureBlockExtensions()}.
     *
     * @throws IOException If there is a fundamental problem with the input.
     * @throws BlockLoadingException If the definition is malformed.
     */
    protected void resetBlockFactory() {
        mBlocklyActivityHelper.resetBlockFactory(
                getBlockDefinitionsJsonPaths());

        configureBlockExtensions();
        configureMutators();
        configureCategoryFactories();

        // Reload the toolbox?
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
