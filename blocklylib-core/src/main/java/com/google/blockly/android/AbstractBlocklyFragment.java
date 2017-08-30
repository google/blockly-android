/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

package com.google.blockly.android;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

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
 * Base class for a Blockly fragment containing the workspace, toolbar, and trash flyout.
 * <p/>
 * By default, it uses the {@code blockly_unified_workspace} layout, but this can be changed by
 * overriding {@link #onCreateSubViews}. Afterwards, a {@link BlocklyActivityHelper}
 * is constructed to help initialize the Blockly fragments, controller, and supporting UI. If
 * overriding {@link #onCreateSubViews} without {@code unified_blockly_workspace.xml} or
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
 * <p/>
 * By default, this fragment generates its own menu options. This can be disabled by calling
 * {@code setHasOptionsMenu(false)} or customized by overriding {@link #onCreateOptionsMenu}.
 */
public abstract class AbstractBlocklyFragment extends Fragment {
    private static final String TAG = "AbstractBlocklyFragment";

    protected ViewGroup mRootView = null;
    protected BlocklyActivityHelper mBlocklyActivityHelper = null;

    /**
     * Creates the activity's views and subfragments (via {@link #onCreateSubViews}, and then
     * initializes Blockly via {@link #onCreateActivityHelper()}, using the values from
     * {@link #getBlockDefinitionsJsonPaths} and {@link #getToolboxContentsXmlPath}.
     * Subclasses should override those methods to configure the Blockly environment.
     * <p/>
     * Once the controller and fragments are configured, if {@link #checkAllowRestoreBlocklyState}
     * returns true, the activity will attempt to load a prior workspace from the instance state
     * bundle.  If no workspace is loaded, it defers to {@link #onLoadInitialWorkspace}.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        mRootView = onCreateSubViews(inflater);
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

        setHasOptionsMenu(true);

        return mRootView;
    }

    /**
     * Constructions action bar menu items for this workspace.
     * @param menu The menu in the host toolbar or action bar.
     * @param inflater The activity's {@link MenuInflater}.
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(getActionBarMenuResId(), menu);
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
     * {@link #getWorkspaceSavePath()}..
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
     * Propagate back event to BlocklyActivityHelper to close Blockly trash or toolbox flyouts.
     * @return Whether the back event triggered closing a Blockly flyout.
     *
     * @see BlocklyActivityHelper#onBackToCloseFlyouts()
     */
    public boolean onBackPressed() {
        // Try to close any open drawer / toolbox before backing out of the Activity.
        return mBlocklyActivityHelper.onBackToCloseFlyouts();
    }

    /**
     * Create a {@link BlocklyActivityHelper} to use for this Activity.
     */
    protected BlocklyActivityHelper onCreateActivityHelper() {
        return new BlocklyActivityHelper(
                (AppCompatActivity) getActivity(), getChildFragmentManager());
    }

    /** Propagate lifecycle event to BlocklyActivityHelper. */
    @Override
    public void onStart() {
        super.onStart();
        mBlocklyActivityHelper.onStart();
    }

    /** Propagate lifecycle event to BlocklyActivityHelper. */
    @Override
    public void onPause() {
        super.onPause();
        mBlocklyActivityHelper.onPause();
        onAutosave();
    }

    /** Propagate lifecycle event to BlocklyActivityHelper. */
    @Override
    public void onResume() {
        super.onResume();
        mBlocklyActivityHelper.onResume();
    }

    /** Propagate lifecycle event to BlocklyActivityHelper. */
    @Override
    public void onStop() {
        super.onStop();
        mBlocklyActivityHelper.onStop();
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
        } else {
            return false;
        }
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

            File file = getActivity().getFileStreamPath(filePath);
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
     * @return The id of the menu resource used to populate the {@link ActionBar}.
     */
    protected int getActionBarMenuResId() {
        return com.google.blockly.android.R.menu.blockly_default_actionbar;
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
     * Constructs (or inflates) the primary content view of the Activity.
     *
     * @return The {@link ViewGroup} constructed. If using a {@link Fragment}, return null.
     */
    protected ViewGroup onCreateSubViews(LayoutInflater inflater) {
        return (ViewGroup) inflater.inflate(R.layout.blockly_unified_workspace, null);
    }

    /**
     * @return The {@link View} to be used for the navigation menu. Otherwise null.
     */
    protected View onCreateAppNavigationDrawer() {
        return null;
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
}
