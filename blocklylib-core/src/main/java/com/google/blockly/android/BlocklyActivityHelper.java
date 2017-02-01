package com.google.blockly.android;

import android.app.Activity;
import android.content.Context;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.google.blockly.android.clipboard.BlockClipDataHelper;
import com.google.blockly.android.clipboard.SingleMimeTypeClipDataHelper;
import com.google.blockly.android.codegen.CodeGenerationRequest;
import com.google.blockly.android.codegen.CodeGeneratorManager;
import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.android.ui.BlockViewFactory;
import com.google.blockly.android.ui.WorkspaceHelper;
import com.google.blockly.model.BlocklySerializerException;
import com.google.blockly.model.Workspace;

import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Class to facilitate Blockly setup on an Activity.
 *
 *
 * {@link BlocklyActivityHelper#onCreateFragments()} looks for
 * the {@link WorkspaceFragment}, the toolbox's {@link FlyoutFragment}, and the trash's
 * {@link FlyoutFragment} via fragment ids {@link R.id#blockly_workspace},
 * {@link R.id#blockly_toolbox}, and {@link R.id#blockly_trash}, respectively.
 * <p/>
 * The activity can also contain a few buttons to control the workspace.
 * {@link R.id#blockly_zoom_in_button} and {@link R.id#blockly_zoom_out_button} control the
 * workspace zoom scale, and {@link R.id#blockly_center_view_button} will reset it.
 * {@link R.id#blockly_trash_icon} will toggle a closeable {@link FlyoutFragment}, and also act as
 * a block drop target to delete blocks.  The methods {@link #onConfigureTrashIcon()},
 * {@link #onConfigureZoomInButton()}, {@link #onConfigureZoomOutButton()}, and
 * {@link #onConfigureCenterViewButton()} will search for these views
 * and set the respective behavior.
 */

public class BlocklyActivityHelper {
    protected AppCompatActivity mActivity;

    protected WorkspaceHelper mWorkspaceHelper;
    protected BlockViewFactory mBlockViewFactory;
    protected BlockClipDataHelper mClipDataHelper;
    protected WorkspaceFragment mWorkspaceFragment;
    protected FlyoutFragment mToolboxFlyoutFragment;
    protected FlyoutFragment mTrashFragment;
    protected CategorySelectorFragment mCategoryFragment;

    protected BlocklyController mController;
    protected CodeGeneratorManager mCodeGeneratorManager;

    /**
     * Creates the activity helper and initializes Blockly. Must be called during
     * {@link Activity#onCreate}. Executes the following sequence of calls during initialization:
     * <ul>
     *     <li>{@link #onCreateFragments} to find fragments</li>
     *     <li>{@link #onCreateBlockViewFactory}</li>
     * </ul>
     * Subclasses should override those methods to configure the Blockly environment.
     */
    public BlocklyActivityHelper(AppCompatActivity activity,
                                 List<String> blockDefinitionJsonPaths,
                                 String toolboxPath,
                                 BlocklyController.VariableCallback variableCallback) {
        mActivity = activity;

        onCreateFragments();
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
                .setVariableCallback(variableCallback)
                .setWorkspaceFragment(mWorkspaceFragment)
                .addBlockDefinitionsFromAssets(blockDefinitionJsonPaths)
                .setToolboxConfigurationAsset(toolboxPath)
                .setTrashFragment(mTrashFragment)
                .setToolboxFragment(mToolboxFlyoutFragment, mCategoryFragment);
        mController = builder.build();

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
     * Save the workspace to the given file in the application's private data directory.
     */
    public void saveWorkspaceToAppDir(String filename) {
        Workspace workspace = mWorkspaceFragment.getWorkspace();
        try {
            workspace.serializeToXml(mActivity.openFileOutput(filename, Context.MODE_PRIVATE));
            Toast.makeText(mActivity, R.string.toast_workspace_saved,
                    Toast.LENGTH_LONG).show();
        } catch (FileNotFoundException | BlocklySerializerException e) {
            Toast.makeText(mActivity, R.string.toast_workspace_not_saved,
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Loads the workspace from the given file in the application's private data directory.
     */
    public void loadWorkspaceFromAppDir(String filename) {
        try {
            mController.loadWorkspaceContents(mActivity.openFileInput(filename));
        } catch (FileNotFoundException e) {
            Toast.makeText(mActivity, R.string.toast_workspace_file_not_found,
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * @return True if the action was handled to close a previously open (and closable) flyout.
     *         Otherwise false.
     */
    public boolean onBackToCloseFlyouts() {
        return mController.closeFlyouts();
    }

    public void requestCodeGeneration(
            List<String> blockDefinitionsJsonPaths,
            List<String> generatorsJsPaths,
            CodeGenerationRequest.CodeGeneratorCallback codeGenerationCallback) {
        mCodeGeneratorManager.requestCodeGeneration(
                blockDefinitionsJsonPaths,
                generatorsJsPaths,
                mWorkspaceFragment.getWorkspace(),
                codeGenerationCallback);
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
     * Creates the Views and Fragments before the BlocklyController is constructed.  Override to
     * load a custom View hierarchy.  Responsible for assigning
     * {@link #mWorkspaceFragment}, and optionally, {@link #mToolboxFlyoutFragment} and
     * {@link #mTrashFragment}. This base implementation attempts to acquire references to the
     * {@link #mToolboxFlyoutFragment} and {@link #mTrashFragment} using the layout ids
     * {@link R.id#} and {@link R.id#blockly_trash}, respectively. Subclasses may leave these
     * {@code null} if the views are not present in the UI.
     * <p>
     * Always called once from {@link #BlocklyActivityHelper} and before {@link #mController} is
     * instantiated.
     */
//    the base implementation of {@link #onCreateFragments()} looks for
//            * the {@link WorkspaceFragment}, the toolbox's {@link FlyoutFragment} and
//            * {@link CategorySelectorFragment}, and the trash's {@link FlyoutFragment} via fragment ids
//            * {@link R.id#blockly_workspace}, {@link R.id#blockly_flyout}, {@link R.id#blockly_categories}, and
//    * {@link R.id#blockly_trash}.
    protected void onCreateFragments() {
        FragmentManager fragmentManager = mActivity.getSupportFragmentManager();
        mWorkspaceFragment = (WorkspaceFragment)
                fragmentManager.findFragmentById(R.id.blockly_workspace);
        mToolboxFlyoutFragment = (FlyoutFragment) fragmentManager
                .findFragmentById(R.id.blockly_toolbox_flyout);
        mCategoryFragment = (CategorySelectorFragment) fragmentManager
                .findFragmentById(R.id.blockly_categories);
        mTrashFragment = (FlyoutFragment) fragmentManager
                .findFragmentById(R.id.blockly_trash_flyout);

        if (mTrashFragment != null) {
            // TODO(#14): Make trash list a drop location.
        }
    }

    /**
     * Constructs the {@link BlockViewFactory} used by all fragments in this activity. The Blockly
     * core library does not include a factory implementation, and the app developer will need to
     * include blockly vertical or another block rendering implementation.
     * <p>
     * The default implementation atempts to instantiates a VerticalBlockViewFactory, which is
     * included in the blocklylib-vertical library.  An error will be thrown unless
     * blocklylib-vertical is included or this method is overridden to provide a custom
     * BlockViewFactory.
     *
     * @param helper The Workspace helper for this activity.
     * @return The {@link BlockViewFactory} used by all fragments in this activity.
     */
    public BlockViewFactory onCreateBlockViewFactory(WorkspaceHelper helper) {
        try {
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
}
