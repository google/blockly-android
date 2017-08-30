/*
 *  Copyright 2017 Google Inc. All Rights Reserved.
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

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.blockly.android.clipboard.BlockClipDataHelper;
import com.google.blockly.android.clipboard.SingleMimeTypeClipDataHelper;
import com.google.blockly.android.codegen.CodeGenerationRequest;
import com.google.blockly.android.codegen.CodeGeneratorManager;
import com.google.blockly.android.codegen.LanguageDefinition;
import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.android.ui.BlockListUI;
import com.google.blockly.android.ui.BlockViewFactory;
import com.google.blockly.android.ui.DefaultVariableCallback;
import com.google.blockly.android.ui.MutatorFragment;
import com.google.blockly.android.ui.WorkspaceHelper;
import com.google.blockly.model.Block;
import com.google.blockly.model.BlockExtension;
import com.google.blockly.model.BlockFactory;
import com.google.blockly.model.BlocklySerializerException;
import com.google.blockly.model.CustomCategory;
import com.google.blockly.model.DefaultBlocks;
import com.google.blockly.model.Mutator;
import com.google.blockly.model.Workspace;
import com.google.blockly.utils.BlockLoadingException;
import com.google.blockly.utils.StringOutputStream;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

/**
 * Class to facilitate Blockly setup on an Activity.
 *
 * {@link BlocklyActivityHelper#onFindFragments} looks for
 * the {@link WorkspaceFragment}, the toolbox's {@link BlockListUI}, and the trash's
 * {@link BlockListUI} via fragment ids {@link R.id#blockly_workspace},
 * {@link R.id#blockly_toolbox_ui}, and {@link R.id#blockly_trash_ui}, respectively.
 * <p/>
 * The activity can also contain a few buttons to control the workspace.
 * {@link R.id#blockly_zoom_in_button} and {@link R.id#blockly_zoom_out_button} control the
 * workspace zoom scale, and {@link R.id#blockly_center_view_button} will reset it.
 * {@link R.id#blockly_trash_icon} will toggle a closeable {@link R.id#blockly_trash_ui}
 * {@link BlockListUI} (such as {@link FlyoutFragment}), and also act as a block drop target to
 * delete blocks.  The methods {@link #onConfigureTrashIcon()}, {@link #onConfigureZoomInButton()},
 * {@link #onConfigureZoomOutButton()}, and {@link #onConfigureCenterViewButton()} will search for
 * these views and set the respective behavior.
 */

public class BlocklyActivityHelper {
    private static final String TAG = "BlocklyActivityHelper";

    protected AppCompatActivity mActivity;

    protected WorkspaceHelper mWorkspaceHelper;
    protected BlockViewFactory mBlockViewFactory;
    protected BlockClipDataHelper mClipDataHelper;
    protected WorkspaceFragment mWorkspaceFragment;
    protected BlockListUI mToolboxBlockList;
    protected BlockListUI mTrashBlockList;
    protected CategorySelectorFragment mCategoryFragment;
    protected Mutator mCurrMutator;
    protected DialogFragment mDialogFragment;

    protected BlocklyController mController;
    protected CodeGeneratorManager mCodeGeneratorManager;

    protected MutatorFragment.DismissListener mMutatorDismissListener = new MutatorFragment
            .DismissListener() {
        @Override
        public void onDismiss(MutatorFragment dialog) {
            if (dialog == mDialogFragment) {
                mCurrMutator = null;
                mDialogFragment = null;
            } else {
                Log.d(TAG, "Received dismiss call for unknown dialog");
            }
        }
    };

    /**
     * Creates the activity helper and initializes Blockly. Must be called during
     * {@link Activity#onCreate}. Executes the following sequence of calls during initialization:
     * <ul>
     *     <li>{@link #onFindFragments} to find fragments</li>
     *     <li>{@link #onCreateBlockViewFactory}</li>
     * </ul>
     * Subclasses should override those methods to configure the Blockly environment.
     *
     * @throws IllegalStateException If error occurs during initialization. Assumes all initial
     *                               compile-time assets are known to be valid.
     * @deprecated Use {@link #BlocklyActivityHelper(AppCompatActivity, FragmentManager)}
     */
    @Deprecated
    public BlocklyActivityHelper(AppCompatActivity activity) {
        this(activity, activity.getSupportFragmentManager());
    }

    /**
     * Creates the activity helper and initializes Blockly. Must be called during
     * {@link Activity#onCreate}. Executes the following sequence of calls during initialization:
     * <ul>
     *     <li>{@link #onFindFragments} to find fragments</li>
     *     <li>{@link #onCreateBlockViewFactory}</li>
     * </ul>
     * Subclasses should override those methods to configure the Blockly environment.
     *
     * @throws IllegalStateException If error occurs during initialization. Assumes all initial
     *                               compile-time assets are known to be valid.
     */
    public BlocklyActivityHelper(AppCompatActivity activity, FragmentManager fragmentManager) {
        mActivity = activity;

        onFindFragments(fragmentManager);
        if (mWorkspaceFragment == null) {
            throw new IllegalStateException("mWorkspaceFragment is null");
        }

        mWorkspaceHelper = new WorkspaceHelper(activity);
        mBlockViewFactory = onCreateBlockViewFactory(mWorkspaceHelper);
        mClipDataHelper = onCreateClipDataHelper();
        mCodeGeneratorManager = new CodeGeneratorManager(activity);

        BlocklyController.Builder builder = new BlocklyController.Builder(activity)
                .setClipDataHelper(mClipDataHelper)
                .setWorkspaceHelper(mWorkspaceHelper)
                .setBlockViewFactory(mBlockViewFactory)
                .setWorkspaceFragment(mWorkspaceFragment)
                .setTrashUi(mTrashBlockList)
                .setToolboxUi(mToolboxBlockList, mCategoryFragment);
        mController = builder.build();

        onCreateVariableCallback();
        onCreateMutatorListener();

        onConfigureTrashIcon();
        onConfigureZoomInButton();
        onConfigureZoomOutButton();
        onConfigureCenterViewButton();
    }

    /**
     * Lifecycle hook that must be called from {@link Activity#onStart()}.
     * Does nothing yet, but required for future compatibility.
     */
    public void onStart() {
        // Do nothing.
    }

    /**
     * Lifecycle hook that must be called from {@link Activity#onResume()}.
     */
    public void onResume() {
        mCodeGeneratorManager.onResume();
    }

    /**
     * @return the {@link BlocklyController} for this activity.
     */
    public BlocklyController getController() {
        return mController;
    }

    /**
     * Save the workspace to the given file in the application's private data directory, and show a
     * status toast. If the save fails, the error is logged.
     */
    public void saveWorkspaceToAppDir(String filename)
            throws FileNotFoundException, BlocklySerializerException{
        Workspace workspace = mWorkspaceFragment.getWorkspace();
        workspace.serializeToXml(mActivity.openFileOutput(filename, Context.MODE_PRIVATE));
    }

    /**
     * Save the workspace to the given file in the application's private data directory, and show a
     * status toast. If the save fails, the error is logged.
     * @return True if the save was successful. Otherwise false.
     */
    public boolean saveWorkspaceToAppDirSafely(String filename) {
        try {
            saveWorkspaceToAppDir(filename);
            Toast.makeText(mActivity, R.string.toast_workspace_saved,
                    Toast.LENGTH_LONG).show();
            return true;
        } catch (FileNotFoundException | BlocklySerializerException e) {
            Toast.makeText(mActivity, R.string.toast_workspace_not_saved,
                    Toast.LENGTH_LONG).show();
            Log.e(TAG, "Failed to save workspace to " + filename, e);
            return false;
        }
    }

    /**
     * Loads the workspace from the given file in the application's private data directory.
     * @param filename The path to the file, in the application's local storage.
     * @throws IOException If there is a underlying problem with the input.
     * @throws BlockLoadingException If there is a error with the workspace XML format or blocks.
     */
    public void loadWorkspaceFromAppDir(String filename) throws IOException, BlockLoadingException {
        mController.loadWorkspaceContents(mActivity.openFileInput(filename));
    }

    /**
     * Loads the workspace from the given file in the application's private data directory. If it
     * fails to load, a toast will be shown and the error will be logged.
     * @param filename The path to the file, in the application's local storage.
     * @return True if loading the workspace succeeds. Otherwise, false and the error will be
     *         logged.
     */
    public boolean loadWorkspaceFromAppDirSafely(String filename) {
        try {
            loadWorkspaceFromAppDir(filename);
            return true;
        } catch (FileNotFoundException e) {
            Toast.makeText(mActivity, R.string.toast_workspace_file_not_found, Toast.LENGTH_LONG)
                    .show();
            Log.e(TAG, "Failed to load workspace", e);
        } catch (IOException | BlockLoadingException e) {
            Toast.makeText(mActivity, R.string.toast_workspace_load_failed, Toast.LENGTH_LONG)
                    .show();
            Log.e(TAG, "Failed to load workspace", e);
        }
        return false;
    }

    /**
     * @return True if the action was handled to close a previously open (and closable) toolbox,
     *         trash UI, or dialog. Otherwise false.
     */
    public boolean onBackToCloseFlyouts() {
        return closeDialogFragment() || mController.closeFlyouts();
    }

    /**
     * Requests code generation using the blocks in the {@link Workspace}/{@link WorkspaceFragment}.
     *
     * @param codeGeneratorLanguage The language definition for the generators.
     * @param blockDefinitionsJsonPaths The asset path to the JSON block definitions.
     * @param generatorsJsPaths The asset paths to the JavaScript generators, and optionally the
     *                          JavaScript block extension/mutator sources.
     * @param codeGenerationCallback The {@link CodeGenerationRequest.CodeGeneratorCallback} to use
     *                               upon completion.
     */
    public void requestCodeGeneration(
            LanguageDefinition codeGeneratorLanguage,
            List<String> blockDefinitionsJsonPaths,
            List<String> generatorsJsPaths,
            CodeGenerationRequest.CodeGeneratorCallback codeGenerationCallback) {

        final StringOutputStream serialized = new StringOutputStream();
        try {
            mController.getWorkspace().serializeToXml(serialized);
        } catch (BlocklySerializerException e) {
            // Not using a string resource because no non-developer should see this.
            String msg = "Failed to serialize workspace during code generation.";
            Log.wtf(TAG, msg, e);
            Toast.makeText(mActivity, msg, Toast.LENGTH_LONG).show();
            throw new IllegalStateException(msg, e);
        }

        mCodeGeneratorManager.requestCodeGeneration(
                new CodeGenerationRequest(
                        serialized.toString(),
                        codeGenerationCallback,
                        codeGeneratorLanguage,
                        blockDefinitionsJsonPaths,
                        generatorsJsPaths));
        try {
            serialized.close();
        } catch (IOException e) {
            // Ignore error on close().
        }
    }


    /**
     * Lifecycle hook that must be called from {@link Activity#onPause()}.
     */
    public void onPause() {
        mCodeGeneratorManager.onPause();
    }

    /**
     * Lifecycle hook that must be called from {@link Activity#onStop()}.
     * Does nothing yet, but required for future compatibility.
     */
    public void onStop() {
        // Do nothing.
    }

    /**
     * Lifecycle hook that must be called from {@link Activity#onRestart()}.
     * Does nothing yet, but required for future compatibility.
     */
    public void onRestart() {
        // Do nothing.
    }

    /**
     * Lifecycle hook that must be called from {@link Activity#onDestroy()}.
     * Does nothing yet, but required for future compatibility.
     */
    public void onDestroy() {
        // Do nothing.
    }

    /**
     * Default implementation for {@link AbstractBlocklyActivity#configureMutators()}. This adds
     * all mutators in {@link DefaultBlocks} and their UIs to the {@link BlockFactory} and
     * {@link BlockViewFactory}.
     * <p/>
     * Mutators with the same key will replace existing mutators, so it is safe
     * to call super and then update specific mutators.
     */
    public void configureMutators() {
        BlockFactory blockFactory = mController.getBlockFactory();
        Map<String, Mutator.Factory> defaultMutators = DefaultBlocks.getMutators();
        for (String key : defaultMutators.keySet()) {
            blockFactory.registerMutator(key, defaultMutators.get(key));
        }

        Map<String, MutatorFragment.Factory> defaultMutatorUis = DefaultBlocks.getMutatorUis();
        for (String key : defaultMutatorUis.keySet()) {
            mBlockViewFactory.registerMutatorUi(key, defaultMutatorUis.get(key));
        }
    }

    /**
     * Default implementation for {@link AbstractBlocklyActivity#configureBlockExtensions()}. This
     * adds all extensions in {@link DefaultBlocks} to the {@link BlockFactory}.
     * <p/>
     * Extensions with the same key will replace existing extensions, so it is safe
     * to call super and then update specific extensions.
     */
    public void configureExtensions() {
        BlockFactory blockFactory = mController.getBlockFactory();
        Map<String, BlockExtension> extensions = DefaultBlocks.getExtensions();
        for (String key : extensions.keySet()) {
            blockFactory.registerExtension(key, extensions.get(key));
        }
    }

    /**
     * This method provides a hook to register custom {@link CustomCategory}s that support
     * toolboxes in this activity. The default implementation registers
     * {@link DefaultBlocks#getToolboxCustomCategories the categories in DefaultBlocks}.
     * <p/>
     * Most subclasses will want to include these default custom categories, calling
     * {@code super.configureCategoryFactories()} if overridden. Category factories with the same
     * {@code custom} key will replace existing {@link CustomCategory}s, so it is safe to call
     * super and then update specific categories.
     */
    public void configureCategoryFactories() {
        Map <String, CustomCategory> factoryMap =
                DefaultBlocks.getToolboxCustomCategories(mController);
        for (String key : factoryMap.keySet()) {
            mController.registerCategoryFactory(key, factoryMap.get(key));
        }
    }

    /**
     * Shows a dialog UI to the user. Only one dialog will be shown at a time and if there is
     * already an existing one it will be replaced. This can be used for showing application or
     * block specific UIs, such as a mutator UI. When the user finishes using the fragment
     * {@link #closeDialogFragment()} should be called.
     *
     * @param fragment The fragment to show.
     */
    public void showDialogFragment(@NonNull DialogFragment fragment) {
        if (fragment == mDialogFragment) {
            return;
        }

        FragmentTransaction ft = mActivity.getSupportFragmentManager().beginTransaction();
        if (mDialogFragment != null) {
            ft.remove(mDialogFragment);
        }
        fragment.show(ft, "blockly_dialog");
        mDialogFragment = fragment;
    }

    /**
     * Closes the currently open {@link DialogFragment} if there is one.
     *
     * @return True if there was a dialog and it was closed, false otherwise.
     */
    public boolean closeDialogFragment() {
        if (mDialogFragment != null) {
            FragmentTransaction ft = mActivity.getSupportFragmentManager().beginTransaction();
            ft.remove(mDialogFragment);
            ft.commit();
            mDialogFragment = null;
            return true;
        }
        return false;
    }

    /**
     * Creates the Views and Fragments before the BlocklyController is constructed.  Override to
     * load a custom View hierarchy.  Responsible for assigning {@link #mWorkspaceFragment}, and
     * optionally, {@link #mToolboxBlockList} and {@link #mTrashBlockList}. This base
     * implementation attempts to acquire references to:
     * <ul>
     *   <li>the {@link WorkspaceFragment} with id {@link R.id#blockly_workspace}, assigned to
     *   {@link #mWorkspaceFragment}.</li>
     *   <li>the toolbox {@link CategorySelectorFragment} with id {@link R.id#blockly_categories},
     *   assigned to {@link #mCategoryFragment}.</li>
     *   <li>the toolbox {@link FlyoutFragment} with id {@link R.id#blockly_toolbox_ui},
     *   assigned to {@link #mToolboxBlockList}.</li>
     *   <li>the trash {@link FlyoutFragment} with id {@link R.id#blockly_trash_ui}, assigned to
     *   {@link #mTrashBlockList}.</li>
     * </ul>
     * Only the workspace fragment is required. The activity layout can choose not to include the
     * other fragments, and subclasses that override this method can leave the field null if that
     * are not used.
     * <p/>
     * This methods is always called once from the constructor before {@link #mController} is
     * instantiated.
     */
    protected void onFindFragments(FragmentManager fragmentManager) {
        mWorkspaceFragment = (WorkspaceFragment)
                fragmentManager.findFragmentById(R.id.blockly_workspace);
        mToolboxBlockList = (BlockListUI) fragmentManager
                .findFragmentById(R.id.blockly_toolbox_ui);
        mCategoryFragment = (CategorySelectorFragment) fragmentManager
                .findFragmentById(R.id.blockly_categories);
        mTrashBlockList = (BlockListUI) fragmentManager
                .findFragmentById(R.id.blockly_trash_ui);

        if (mTrashBlockList != null) {
            // TODO(#14): Make trash list a drop location.
        }
    }

    /**
     * Constructs the {@link BlockViewFactory} used by all fragments in this activity. The Blockly
     * core library does not include a factory implementation, and the app developer will need to
     * include blockly vertical or another block rendering implementation.
     * <p>
     * The default implementation attempts to instantiates a VerticalBlockViewFactory, which is
     * included in the blocklylib-vertical library.  An error will be thrown unless
     * blocklylib-vertical is included or this method is overridden to provide a custom
     * BlockViewFactory.
     *
     * @param helper The Workspace helper for this activity.
     * @return The {@link BlockViewFactory} used by all fragments in this activity.
     */
    public BlockViewFactory onCreateBlockViewFactory(WorkspaceHelper helper) {
        try {
            @SuppressWarnings("unchecked")
            Class<? extends BlockViewFactory> clazz =
                    (Class<? extends BlockViewFactory>)Class.forName(
                            "com.google.blockly.android.ui.vertical.VerticalBlockViewFactory");
            return clazz.getConstructor(Context.class, WorkspaceHelper.class)
                    .newInstance(mActivity, helper);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(
                    "Default BlockViewFactory not found. Did you include blocklylib-vertical?", e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Unable to instantiate VerticalBlockViewFactory", e);
        } catch (InstantiationException e) {
            throw new RuntimeException("Unable to instantiate VerticalBlockViewFactory", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Unable to instantiate VerticalBlockViewFactory", e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Unable to instantiate VerticalBlockViewFactory", e);
        }
    }

    /**
     * Constructs the {@link BlockClipDataHelper} for use by all Blockly components of
     * {@link #mActivity}. The instance will be passed to the controller and available via
     * {@link BlocklyController#getClipDataHelper()}.
     * <p/>
     * By default, it constructs a {@link SingleMimeTypeClipDataHelper} with a MIME type derived
     * from the application's package name. This assumes all Blockly workspaces in an app work with
     * the same shared set of blocks, and blocks can be dragged/copied/pasted between them, even if
     * they are in different Activities. It also ensures blocks from other applications will be
     * rejected.
     * <p/>
     * If your app uses different block sets for different workspaces, or you intend to interoperate
     * with other applications, you will need to override this method with your own implementation.
     *
     * @return A new {@link BlockClipDataHelper}.
     */
    protected BlockClipDataHelper onCreateClipDataHelper() {
        return SingleMimeTypeClipDataHelper.getDefault(mActivity);
    }

    /**
     * This method finds and configures {@link R.id#blockly_trash_icon} from the view hierarchy as
     * the button to open and close the trash, and a drop location for deleting
     * blocks. If {@link R.id#blockly_trash_icon} is not found, it does nothing.
     * <p/>
     * This is called from the constructor after {@link #mController} is initialized.
     */
    protected void onConfigureTrashIcon() {
        View trashIcon = mActivity.findViewById(R.id.blockly_trash_icon);
        if (mController != null && trashIcon != null) {
            mController.setTrashIcon(trashIcon);
        }
    }

    /**
     * This method finds and configures {@link R.id#blockly_zoom_in_button} from the view hierarchy
     * as the button to zoom in (e.g., enlarge) the workspace view. If
     * {@link R.id#blockly_zoom_in_button} is not found, it does nothing.
     * <p/>
     * This is called from the constructor after {@link #mController} is initialized.
     */
    protected void onConfigureZoomInButton() {
        View zoomInButton = mActivity.findViewById(R.id.blockly_zoom_in_button);
        if (zoomInButton != null) {
            zoomInButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mController.zoomIn();
                }
            });
            ZoomBehavior zoomBehavior = mWorkspaceHelper.getZoomBehavior();
            zoomInButton.setVisibility(zoomBehavior.isButtonEnabled()? View.VISIBLE : View.GONE);
        }
    }

    /**
     * This method finds and configures {@link R.id#blockly_zoom_out_button} from the view hierarchy
     * as the button to zoom out (e.g., shrink) the workspace view. If
     * {@link R.id#blockly_zoom_out_button} is not found, it does nothing.
     * <p/>
     * This is called from the constructor after {@link #mController} is initialized.
     */
    protected void onConfigureZoomOutButton() {
        View zoomOutButton = mActivity.findViewById(R.id.blockly_zoom_out_button);
        if (zoomOutButton != null) {
            zoomOutButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mController.zoomOut();
                }
            });
            ZoomBehavior zoomBehavior = mWorkspaceHelper.getZoomBehavior();
            zoomOutButton.setVisibility(zoomBehavior.isButtonEnabled()? View.VISIBLE : View.GONE);
        }
    }

    /**
     * This method finds and configures {@link R.id#blockly_center_view_button} from the view
     * hierarchy as the button to reset the workspace view. If
     * {@link R.id#blockly_center_view_button} is not found, it does nothing.
     * <p/>
     * This is called from the constructor after {@link #mController} is initialized.
     */
    protected void onConfigureCenterViewButton() {
        View recenterButton = mActivity.findViewById(R.id.blockly_center_view_button);
        if (recenterButton != null) {
            recenterButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mController.recenterWorkspace();
                }
            });
            ZoomBehavior zoomBehavior = mWorkspaceHelper.getZoomBehavior();
            recenterButton.setVisibility(zoomBehavior.isFixed()? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Constructs a {@link BlocklyController.VariableCallback} for handling user requests to change
     * the list of variables (create, rename, delete). This can be used to provide UI for confirming
     * a deletion or renaming a variable.
     * <p/>
     * By default, this method constructs a {@link DefaultVariableCallback}. Apps can override this
     * to provide an alternative implementation, or optionally override the method to do nothing (no
     * confirmation UI).
     * <p/>
     * This method is responsible for calling {@link BlocklyController#setVariableCallback}.
     *
     * @return A {@link com.google.blockly.android.control.BlocklyController.VariableCallback} for
     *         handling variable updates from the controller.
     */
    protected void onCreateVariableCallback() {
        BlocklyController.VariableCallback variableCb =
                new DefaultVariableCallback(mActivity, mController);
        mController.setVariableCallback(variableCb);
    }

    /**
     * Constructs a {@link BlockViewFactory.MutatorToggleListener} for handling user requests to
     * show/hide a mutator for a block.
     * <p/>
     * By default, this method will remove the open dialog fragment if there is one and show the
     * mutator's fragment if it wasn't just removed.
     * <p/>
     * This method is responsible for calling {@link BlockViewFactory#setMutatorToggleListener}.
     */
    protected void onCreateMutatorListener() {
        BlockViewFactory.MutatorToggleListener mutatorListener = new BlockViewFactory
                .MutatorToggleListener() {
            @Override
            public void onMutatorToggled(Block block) {
                handleMutatorToggled(block);
            }
        };
        mBlockViewFactory.setMutatorToggleListener(mutatorListener);
    }

    /**
     * Resets the {@link BlockFactory} with the provided block definitions and extensions.
     * @param blockDefinitionsJsonPaths The list of definition asset paths.
     * @throws IllegalStateException On any issues with the input.
     */
    public void resetBlockFactory(
            @Nullable List<String> blockDefinitionsJsonPaths) {
        AssetManager assets = mActivity.getAssets();
        BlockFactory factory = mController.getBlockFactory();
        factory.clear();

        String assetPath = null;
        try {
            if (blockDefinitionsJsonPaths != null) {
                for (String path : blockDefinitionsJsonPaths) {
                    assetPath = path;
                    factory.addJsonDefinitions(assets.open(path));
                }
            }
        } catch (IOException | BlockLoadingException e) {
            throw new IllegalStateException(
                    "Failed to load block definition asset file: " + assetPath, e);
        }
    }

    /**
     * Reloads the toolbox from assets.
     * @param toolboxContentsXmlPath The asset path to the toolbox XML
     * @throws IllegalStateException If error occurs during loading.
     */
    public void reloadToolbox(String toolboxContentsXmlPath) {
        AssetManager assetManager = mActivity.getAssets();
        BlocklyController controller = getController();
        try {
            controller.loadToolboxContents(assetManager.open(toolboxContentsXmlPath));
        } catch (IOException | BlockLoadingException e) {
            // compile time assets such as assets are assumed to be good.
            throw new IllegalStateException("Failed to load toolbox XML.", e);
        }
    }


    /**
     * Decide when and how to show UI for a mutator. This should be called when the user has tried
     * to toggle a mutator. The method will figure out if the mutator's dialog should be shown,
     * hidden, or ignored in response.
     *
     * @param block The block the user is toggling a mutator for.
     */
    private void handleMutatorToggled(Block block) {
        Mutator mutator = block.getMutator();
        if (mutator == mCurrMutator) {
            if (mDialogFragment != null) {
                FragmentTransaction ft = mActivity.getSupportFragmentManager().beginTransaction();
                ft.remove(mDialogFragment);
                ft.commit();
            } else {
                Log.w(TAG, "Had a mutator, but no dialog fragment to remove. Cleaning up state.");
            }
            mCurrMutator = null;
        }
        if (!mBlockViewFactory.hasUiForMutator(mutator.getMutatorId())) {
            Log.e(TAG, "Mutator without UI toggled.");
            return;
        }
        MutatorFragment fragment = mBlockViewFactory.getMutatorFragment(mutator);
        if (fragment == null) {
            throw new IllegalArgumentException("Mutator with UI has no Dialog Fragment to show.");
        }

        FragmentTransaction ft = mActivity.getSupportFragmentManager().beginTransaction();
        if (mDialogFragment != null) {
            ft.remove(mDialogFragment);
        }
        fragment.show(ft, "MUTATOR_DIALOG");
        fragment.setDismissListener(mMutatorDismissListener);
        mCurrMutator = mutator;
        mDialogFragment = fragment;
    }
}
