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

import com.google.blockly.android.codegen.CodeGeneratorService;
import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.android.ui.BlockViewFactory;
import com.google.blockly.android.ui.DeleteVariableDialog;
import com.google.blockly.android.ui.NameVariableDialog;
import com.google.blockly.android.ui.WorkspaceHelper;
import com.google.blockly.android.ui.BlocklyUnifiedWorkspace;
import com.google.blockly.android.codegen.CodeGenerationRequest;
import com.google.blockly.model.Block;
import com.google.blockly.model.BlockFactory;
import com.google.blockly.model.BlocklySerializerException;
import com.google.blockly.model.Workspace;
import com.google.blockly.utils.StringOutputStream;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * Base class for a Blockly activities that use a material design style tool bar, and optionally a
 * navigation menu.
 * <p/>
 * The default layout is filled with a workspace and with the toolbox and trash configured as
 * fly-out views (via the {@link BlocklyUnifiedWorkspace}).  Everything below the
 * {@link ActionBar} can be replaced by overriding {@link #onCreateContentView}.  After
 * {@link #onCreateContentView}, the base implementation of {@link #onCreateFragments()} looks for
 * the {@link WorkspaceFragment}, the {@link ToolboxFragment}, and the {@link TrashFragment} via
 * fragment ids {@link R.id#blockly_workspace}, {@link R.id#blockly_toolbox}, and
 * {@link R.id#blockly_trash}, respectively. If overriding {@link #onCreateContentView} without
 * {@code unified_blockly_workspace.xml} or those fragment ids, override
 * {@link #onCreateFragments()}, appropriately.
 * <p/>
 * The activity can also contain a few buttons to control the workspace.
 * {@link R.id#blockly_zoom_in_button} and {@link R.id#blockly_zoom_out_button} control the
 * workspace zoom scale, and {@link R.id#blockly_center_view_button} will reset it.
 * {@link R.id#blockly_trash_icon} will toggle a closeable {@link TrashFragment}, and also act as
 * a block drop target to delete blocks.  The methods {@link #onConfigureTrashIcon()},
 * {@link #onConfigureZoomInButton()}, {@link #onConfigureZoomOutButton()}, and
 * {@link #onConfigureCenterViewButton()} will search for these views
 * and set the respective behavior.  By default, these buttons are provided by
 * {@link BlocklyUnifiedWorkspace} in {@link #onCreateContentView}.
 * <p/>
 * Configure the workspace by providing definitions for {@link #getBlockDefinitionsJsonPaths()},
 * {@link #getToolboxContentsXmlPath()}, and {@link #onCreateBlockViewFactory}.  An initial
 * workspace can be defined by overriding {@link #getStartingWorkspacePath()}.
 * <p/>
 * The block definitions can be updated at any time by calling {@link #reloadBlockDefinitions()},
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

    public static final String DEFAULT_WORKSPACE_FILENAME = "workspace.xml";

    protected ActionBar mActionBar;
    protected DrawerLayout mDrawerLayout;

    protected WorkspaceHelper mWorkspaceHelper;
    protected BlockViewFactory mBlockViewFactory;
    protected WorkspaceFragment mWorkspaceFragment;
    protected ToolboxFragment mToolboxFragment;
    protected TrashFragment mTrashFragment;

    // These two may be null if {@link #onCreateAppNavigationDrawer} returns null.
    protected View mNavigationDrawer;
    protected ActionBarDrawerToggle mDrawerToggle;

    // This may be null if {@link #getVariableCallback} never sets it.
    private BlocklyController.VariableCallback mVariableCb;
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
     * Creates the Activity Views, Fragments, and BlocklyController via a sequence of calls to
     * <ul>
     *     <li>{@link #onCreateActivityRootView}</li>
     *     <li>{@link #onCreateFragments}</li>
     *     <li>{@link #onCreateBlockViewFactory}</li>
     *     <li>{@link #getBlockDefinitionsJsonPaths}</li>
     *     <li>{@link #getToolboxContentsXmlPath}</li>
     * </ul>
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
        onCreateFragments();
        if (mWorkspaceFragment == null) {
            throw new IllegalStateException("mWorkspaceFragment is null");
        }

        mWorkspaceHelper = new WorkspaceHelper(this);
        mBlockViewFactory = onCreateBlockViewFactory(mWorkspaceHelper);

        BlocklyController.Builder builder = new BlocklyController.Builder(this)
                .setAssetManager(getAssets())  // TODO(#128) Remove
                .setWorkspaceHelper(mWorkspaceHelper)
                .setBlockViewFactory(mBlockViewFactory)
                .setVariableCallback(getVariableCallback())
                .setWorkspaceFragment(mWorkspaceFragment)
                .addBlockDefinitionsFromAssets(getBlockDefinitionsJsonPaths())
                .setToolboxConfigurationAsset(getToolboxContentsXmlPath())
                .setTrashFragment(mTrashFragment)
                .setToolboxFragment(mToolboxFragment, mDrawerLayout);
        mController = builder.build();
        onConfigureTrashIcon();
        onConfigureZoomInButton();
        onConfigureZoomOutButton();
        onConfigureCenterViewButton();

        boolean loadedPriorInstance = checkAllowRestoreBlocklyState(savedInstanceState)
                && mController.onRestoreSnapshot(savedInstanceState);
        if (!loadedPriorInstance) {
            onLoadInitialWorkspace();
        }
    }

    /**
     * Constructs the {@link BlockViewFactory} used by all fragments in this activity.  The Blockly
     * core library does not include a factory implementation, and the app developer will need to
     * include blockly vertical or another block rendering implementation.
     *
     * @param helper The Workspace helper for this activity.
     * @return The {@link BlockViewFactory} used by all fragments in this activity.
     */
    public abstract BlockViewFactory onCreateBlockViewFactory(WorkspaceHelper helper);

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
     * Returns a callback for handling user requests to change the list of variables (create,
     * rename, delete). This can be used to provide UI for confirming a deletion or renaming a
     * variable.
     *
     * @return A {@link com.google.blockly.android.control.BlocklyController.VariableCallback} for
     *         handling variable updates from the controller.
     */
    protected BlocklyController.VariableCallback getVariableCallback() {
        if (mVariableCb == null) {
            mVariableCb = new DefaultVariableCallback(new DeleteVariableDialog(),
                    new NameVariableDialog());
        }
        return mVariableCb;
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
     * Always called once from {@link #onCreate} and before {@link #mController} is instantiated.
     */
    protected void onCreateFragments() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        mWorkspaceFragment = (WorkspaceFragment)
                fragmentManager.findFragmentById(R.id.blockly_workspace);
        mToolboxFragment =
                (ToolboxFragment) fragmentManager.findFragmentById(R.id.blockly_toolbox);
        mTrashFragment = (TrashFragment) fragmentManager.findFragmentById(R.id.blockly_trash);

        if (mTrashFragment != null) {
            // TODO(#14): Make trash list a drop location.
        }
    }

    /**
     * This method finds and configures {@link R.id#blockly_trash_icon} from the view hierarchy as
     * the button to open and close the {@link TrashFragment}, and a drop location for deleting
     * blocks. If {@link R.id#blockly_trash_icon} is not found, it does nothing.
     * <p/>
     * This is called after {@link #mController} is initialized, but before any blocks are loaded
     * into the workspace.
     */
    protected void onConfigureTrashIcon() {
        View trashIcon = findViewById(R.id.blockly_trash_icon);
        if (mTrashFragment != null && trashIcon != null) {
            if (mTrashFragment.isCloseable()) {
                mTrashFragment.setOpened(false);

                trashIcon.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Toggle opened state.
                        mTrashFragment.setOpened(!mTrashFragment.isOpened());
                    }
                });
            }
            mController.setTrashIcon(trashIcon);
        }
    }

    /**
     * This method finds and configures {@link R.id#blockly_zoom_in_button} from the view hierarchy
     * as the button to zoom in (e.g., enlarge) the workspace view. If
     * {@link R.id#blockly_zoom_in_button} is not found, it does nothing.
     * <p/>
     * This is called after {@link #mController} is initialized, but before any blocks are loaded
     * into the workspace.
     */
    protected void onConfigureZoomInButton() {
        View zoomInButton = findViewById(R.id.blockly_zoom_in_button);
        if (zoomInButton != null) {
            zoomInButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mController.zoomIn();
                }
            });
        }
    }

    /**
     * This method finds and configures {@link R.id#blockly_zoom_out_button} from the view hierarchy
     * as the button to zoom out (e.g., shrink) the workspace view. If
     * {@link R.id#blockly_zoom_out_button} is not found, it does nothing.
     * <p/>
     * This is called after {@link #mController} is initialized, but before any blocks are loaded
     * into the workspace.
     */
    protected void onConfigureZoomOutButton() {
        View zoomOutButton = findViewById(R.id.blockly_zoom_out_button);
        if (zoomOutButton != null) {
            zoomOutButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mController.zoomOut();
                }
            });
        }
    }

    /**
     * This method finds and configures {@link R.id#blockly_center_view_button} from the view
     * hierarchy as the button to reset the workspace view. If
     * {@link R.id#blockly_center_view_button} is not found, it does nothing.
     * <p/>
     * This is called after {@link #mController} is initialized, but before any blocks are loaded
     * into the workspace.
     */
    protected void onConfigureCenterViewButton() {
        View recenterButton = findViewById(R.id.blockly_center_view_button);
        if (recenterButton != null) {
            recenterButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mController.recenterWorkspace();
                }
            });
        }
    }

    /**
     * Runs the code generator. Called when user selects "Run" action.
     * @see #getCodeGenerationCallback()
     */
    protected void onRunCode() {
        try {
            if (mWorkspaceFragment.getWorkspace().getRootBlocks().size() == 0) {
                Log.i(TAG, "No blocks in workspace. Skipping run request.");
                return;
            }
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
    protected void reloadToolbox() {
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
        Iterator<String> iter = blockDefsPaths.iterator();
        while (iter.hasNext()) {
            blockDefsPath = iter.next();
            try {
                factory.addBlocks(assetManager.open(blockDefsPath));
            } catch (IOException e) {
                factory.clear();  // Clear any partial loaded block sets.
                // Compile-time bundled assets are assumed to be valid.
                throw new IllegalStateException("Failed to load block definitions from asset: "
                        + blockDefsPath, e);
            }
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

    private class DefaultVariableCallback extends BlocklyController.VariableCallback {
        private final DeleteVariableDialog mDeleteDialog;
        private final NameVariableDialog mNameDialog;

        private final NameVariableDialog.Callback mRenameCallback = new NameVariableDialog
                .Callback() {
            @Override
            public void onNameConfirmed(String oldName, String newName) {
                getController().renameVariable(oldName, newName);
            }
        };
        private final NameVariableDialog.Callback mCreateCallback = new NameVariableDialog
                .Callback() {
            @Override
            public void onNameConfirmed(String originalName, String newName) {
                getController().addVariable(newName);
            }
        };


        public DefaultVariableCallback(DeleteVariableDialog deleteVariableDialog,
                NameVariableDialog nameVariableDialog) {
            mDeleteDialog = deleteVariableDialog;
            mNameDialog = nameVariableDialog;
        }

        @Override
        public boolean onDeleteVariable(String variable) {
            BlocklyController controller = getController();
            if (!controller.isVariableInUse(variable)) {
                return true;
            }
            List<Block> blocks = controller.getBlocksWithVariable(variable);
            if (blocks.size() == 1) {
                // For one block just let the controller delete it.
                return true;
            }

            mDeleteDialog.setController(controller);
            mDeleteDialog.setVariable(variable, blocks.size());
            mDeleteDialog.show(getSupportFragmentManager(), "DeleteVariable");
            return false;
        }

        @Override
        public boolean onRenameVariable(String variable, String newName) {
            mNameDialog.setVariable(variable, mRenameCallback);
            mNameDialog.show(getSupportFragmentManager(), "RenameVariable");
            return false;
        }

        @Override
        public boolean onCreateVariable(String variable) {
            mNameDialog.setVariable(variable, mCreateCallback);
            mNameDialog.show(getSupportFragmentManager(), "CreateVariable");
            return false;
        }
    }
}
