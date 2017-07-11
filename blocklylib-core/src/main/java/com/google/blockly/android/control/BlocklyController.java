/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.blockly.android.control;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.google.blockly.android.ui.BlockListUI;
import com.google.blockly.android.ui.CategorySelectorUI;
import com.google.blockly.android.WorkspaceFragment;
import com.google.blockly.android.clipboard.BlockClipDataHelper;
import com.google.blockly.android.clipboard.SingleMimeTypeClipDataHelper;
import com.google.blockly.android.ui.Dragger;
import com.google.blockly.android.ui.BlockGroup;
import com.google.blockly.android.ui.BlockTouchHandler;
import com.google.blockly.android.ui.BlockView;
import com.google.blockly.android.ui.BlockViewFactory;
import com.google.blockly.android.ui.InputView;
import com.google.blockly.android.ui.PendingDrag;
import com.google.blockly.android.ui.ViewPoint;
import com.google.blockly.android.ui.VirtualWorkspaceView;
import com.google.blockly.android.ui.WorkspaceHelper;
import com.google.blockly.android.ui.WorkspaceView;
import com.google.blockly.android.ui.fieldview.VariableRequestCallback;
import com.google.blockly.model.Block;
import com.google.blockly.model.BlockExtension;
import com.google.blockly.model.BlockFactory;
import com.google.blockly.model.BlocklyCategory;
import com.google.blockly.model.BlocklyEvent;
import com.google.blockly.model.BlocklySerializerException;
import com.google.blockly.model.CustomCategory;
import com.google.blockly.model.Connection;
import com.google.blockly.model.FieldVariable;
import com.google.blockly.model.Input;
import com.google.blockly.model.Mutator;
import com.google.blockly.model.ProcedureInfo;
import com.google.blockly.model.VariableInfo;
import com.google.blockly.model.Workspace;
import com.google.blockly.model.mutator.AbstractProcedureMutator;
import com.google.blockly.utils.BlockLoadingException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Controller to coordinate the state among all the major Blockly components: Workspace, Toolbox,
 * Trash, models, and views.
 * <p/>
 * All calls are expected to be called in the main thread/looper, because they create events that
 * are processed immediately. Several methods will throw an IllegalStateExceptions if called on a
 * different thread.
 */
public class BlocklyController {
    private static final String TAG = "BlocklyController";

    private static final String SNAPSHOT_BUNDLE_KEY = "com.google.blockly.snapshot";
    private static final String SERIALIZED_WORKSPACE_KEY = "SERIALIZED_WORKSPACE";

    /**
     * Callback interface for {@link BlocklyEvent}s.
     */
    public interface EventsCallback {
        /**
         * @return The bitmask of event types handled by this callback.  Must not change.
         */
        @BlocklyEvent.EventType int getTypesBitmask();

        /**
         * Called when a group of events are fired and at least one event is of a type specified by
         * {@link #getTypesBitmask}. While events in the group will always include at least one
         * event of the requested type, the group may also contain other events.
         *
         * @param events List of all the events in this group.
         */
        void onEventGroup(List<BlocklyEvent> events);
    }

    private final Context mContext;
    private final Looper mMainLooper;
    private final BlockFactory mModelFactory;
    private final BlockViewFactory mViewFactory;
    private final WorkspaceHelper mHelper;
    private final BlockClipDataHelper mClipHelper;

    private final Workspace mWorkspace;
    private final ConnectionManager mConnectionManager;
    private final ArrayList<EventsCallback> mListeners = new ArrayList<>();

    // Whether the current call stack is actively executing code intended to group and fire events.
    // See groupAndFireEvents(Runnable)
    private boolean mInEventGroup = false;

    private ArrayList<BlocklyEvent> mPendingEvents;
    private int mPendingEventsMask = 0;
    private int mEventCallbackMask = 0;

    private VirtualWorkspaceView mVirtualWorkspaceView;
    private WorkspaceView mWorkspaceView;
    private WorkspaceFragment mWorkspaceFragment = null;
    private Dragger mDragger;
    private VariableCallback mVariableCallback = null;

    private List<Block> mTempBlocks = new ArrayList<>();

    @VisibleForTesting
    FlyoutController mFlyoutController;

    // For use in bumping neighbors; instance variable only to avoid repeated allocation.
    private final ArrayList<Connection> mTempConnections = new ArrayList<>();

    private View.OnClickListener mDismissClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mFlyoutController.closeFlyouts();
        }
    };

    private final Dragger.DragHandler mWorkspaceDragHandler = new Dragger.DragHandler() {
        @Override
        public Runnable maybeGetDragGroupCreator(final PendingDrag pendingDrag) {
            BlockView touchedView = pendingDrag.getTouchedBlockView();

            // If a shadow or other undraggable block is touched, and it is attached to a draggable
            // parent block, drag that block instead.
            final BlockView activeTouchedView = mHelper.getNearestActiveView(touchedView);
            if (activeTouchedView == null) {
                Log.i(TAG, "User touched a stack of blocks that may not be dragged");
                return null;
            }

            return new Runnable() {
                @Override
                public void run() {
                    // extractBlockAsRoot() fires MoveEvent immediately.
                    extractBlockAsRoot(activeTouchedView.getBlock());

                    // Since this block was already on the workspace, the block's position should
                    // have been assigned correctly during the most recent layout pass.
                    BlockGroup bg = mHelper.getRootBlockGroup(activeTouchedView);
                    bg.bringToFront();

                    // Measure and layout the block group to get the correct touch offset.
                    bg.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                    bg.layout(0, 0, bg.getMeasuredWidth(), bg.getMeasuredHeight());

                    ViewPoint touchOffset = new ViewPoint(
                            (int) (activeTouchedView.getX()
                                    + pendingDrag.getTouchDownViewOffsetX()),
                            (int) (activeTouchedView.getY()
                                    + pendingDrag.getTouchDownViewOffsetY()));
                    pendingDrag.startDrag(mWorkspaceView, bg, touchOffset);
                }
            };
        }

        @Override
        public boolean onBlockClicked(PendingDrag pendingDrag) {
            // TODO(#35): Mark block as focused / selected.
            return false;
        }
    };
    private final BlockTouchHandler mTouchHandler;

    /**
     * Creates a new Controller with Workspace and WorkspaceHelper. Most controllers will require
     * a {@link BlockViewFactory}, but headless (i.e. viewless) controllers are allowed, where it
     * could be null.
     *
     * @param context Android context, such as an Activity.
     * @param blockModelFactory Factory used to create new Blocks.
     * @param workspaceHelper Helper functions for workspace views and device resolution.
     * @param blockViewFactory Factory used to construct block views for this app.
     */
    private BlocklyController(Context context, BlockFactory blockModelFactory,
                              WorkspaceHelper workspaceHelper, BlockClipDataHelper clipHelper,
                              @Nullable BlockViewFactory blockViewFactory) {

        if (context == null) {
            throw new IllegalArgumentException("Context may not be null.");
        }
        if (blockModelFactory == null) {
            throw new IllegalArgumentException("BlockFactory may not be null.");
        }
        if (workspaceHelper == null) {
            throw new IllegalArgumentException("WorkspaceHelper may not be null.");
        }
        if (clipHelper == null) {
            throw new IllegalArgumentException("BlockClipDataHelper may not be null.");
        }
        mContext = context;
        mMainLooper = context.getMainLooper();
        mModelFactory = blockModelFactory;
        mModelFactory.setController(this);
        mHelper = workspaceHelper;
        mViewFactory = blockViewFactory;

        // mHelper, mModelFactory, and mViewFactory must be initialized before mClipHelper.
        mClipHelper = clipHelper;

        mWorkspace = new Workspace(mContext, this, mModelFactory);
        mConnectionManager = mWorkspace.getConnectionManager();

        if (mViewFactory != null) {
            // TODO(#81): Check if variables are enabled/disabled
            mViewFactory.setVariableNameManager(mWorkspace.getVariableNameManager());
            mViewFactory.setVariableRequestCallback(new VariableRequestCallback() {
                @Override
                public void onVariableRequest(int request, String variable) {
                    if (request == VariableRequestCallback.REQUEST_RENAME) {
                        requestRenameVariable(variable, variable);
                    } else if (request == VariableRequestCallback.REQUEST_DELETE) {
                        requestDeleteVariable(variable);
                    }
                }
            });
        }

        mDragger = new Dragger(this);
        mTouchHandler = mDragger.buildSloppyBlockTouchHandler(mWorkspaceDragHandler);

        mFlyoutController = new FlyoutController(this);
    }

    /**
     * Connects a WorkspaceFragment to this controller.
     *
     * @param workspaceFragment The fragment that contains the main workspace.
     */
    public void setWorkspaceFragment(@Nullable WorkspaceFragment workspaceFragment) {
        if (workspaceFragment != null && mViewFactory == null) {
            throw new IllegalStateException("Cannot set UIs without a BlockViewFactory.");
        }

        if (workspaceFragment == mWorkspaceFragment) {
            return;  // No-op
        }
        if (mWorkspaceFragment != null) {
            mWorkspaceFragment.setController(null);
        }
        mWorkspaceFragment = workspaceFragment;
        if (mWorkspaceFragment != null) {
            mWorkspaceFragment.setController(this);
        }
    }

    /**
     * Connects a {@link BlockListUI} and optional {@link CategorySelectorUI} to
     * this controller, so the user can drag new blocks into the attached
     * {@link WorkspaceFragment}.
     *
     * @param toolbox The flyout for displaying toolbox blocks.
     * @param categoryUi Optional ui for displaying toolbox categories.
     */
    public void setToolboxUi(BlockListUI toolbox,
                             @Nullable CategorySelectorUI categoryUi) {
        if (toolbox != null && mViewFactory == null) {
            throw new IllegalStateException("Cannot set UIs without a BlockViewFactory.");
        }
        mFlyoutController.setToolboxUiComponents(categoryUi, toolbox);
        mFlyoutController.setToolboxRoot(mWorkspace.getToolboxContents());
    }

    /**
     * Connects a {@link BlockListUI} for the trash to this controller.
     *
     * @param trashUi
     */
    public void setTrashUi(@Nullable BlockListUI trashUi) {
        if (trashUi != null && mViewFactory == null) {
            throw new IllegalStateException("Cannot set UIs without a BlockViewFactory.");
        }
        mFlyoutController.setTrashUi(trashUi);
        mFlyoutController.setTrashContents(mWorkspace.getTrashCategory());
    }

    /**
     * Assigns the view used for opening/closing the trash.
     *
     * @param trashIcon The trash icon for dropping blocks.
     */
    public void setTrashIcon(View trashIcon) {
        mFlyoutController.setTrashIcon(trashIcon);
    }

    /**
     * Sets the callback to notify when the user requests a variable change, such as deleting or
     * renaming a variable.
     *
     * @param variableCallback The callback to notify when a variable is being deleted.
     */
    public void setVariableCallback(VariableCallback variableCallback) {
        mVariableCallback = variableCallback;
    }

    public void registerCategoryFactory(String customCategoryName, CustomCategory factory) {
        // TODO: Alternative to the static map?
        BlocklyCategory.CUSTOM_CATEGORIES.put(customCategoryName, factory);
    }

    /**
     * @return The {@link Dragger} managing the drag behavior in connected views.
     */
    public Dragger getDragger() {
        return mDragger;
    }

    /**
     * Loads the toolbox contents from an XML resource file.
     *
     * @param toolboxJsonResId The resource id of JSON file (should be a raw resource file).
     */
    public void loadToolboxContents(int toolboxJsonResId) throws BlockLoadingException {
        mWorkspace.loadToolboxContents(toolboxJsonResId);
        updateToolbox();
    }

    /**
     * Loads the toolbox contents from a XML string.
     *
     * @param toolboxJsonString The JSON source of the set of blocks or block groups to show in the
     *                          toolbox.
     * @throws BlockLoadingException If toolbox was not loaded. May wrap an IOException or another
     *                               BlockLoadingException.
     */
    public void loadToolboxContents(String toolboxJsonString) throws BlockLoadingException {
        mWorkspace.loadToolboxContents(toolboxJsonString);
        updateToolbox();
    }

    /**
     * Loads the toolbox contents from a XML input stream.
     *
     * @param toolboxJsonStream A stream of the JSON source of the set of blocks or block groups to
     *                          show in the toolbox.
     */
    public void loadToolboxContents(InputStream toolboxJsonStream)
            throws IOException, BlockLoadingException {
        mWorkspace.loadToolboxContents(toolboxJsonStream);
        updateToolbox();
    }

    /**
     * Reads the workspace in from a XML stream. This will clear the workspace and replace it with
     * the contents of the xml.
     *
     * @param workspaceXmlString The XML source string to read from.
     * @throws BlockLoadingException If workspace was not loaded. May wrap an IOException or another
     *                               BlockLoadingException.
     */
    public void loadWorkspaceContents(String workspaceXmlString) throws BlockLoadingException {
        mWorkspace.loadWorkspaceContents(workspaceXmlString);
        initBlockViews();
    }

    /**
     * Reads the workspace in from a XML stream. This will clear the workspace and replace it with
     * the contents of the xml.
     *
     * @param workspaceXmlStream The input stream to read from.
     * @return True if successful. Otherwise, false with error logged.
     */
    public void loadWorkspaceContents(InputStream workspaceXmlStream) throws BlockLoadingException {
        mWorkspace.loadWorkspaceContents(workspaceXmlStream);
        initBlockViews();
    }

    /**
     * Saves a snapshot of current workspace contents to a temporary cache file, and saves the
     * filename to the instance state bundle.
     * @param mSavedInstanceState The output Bundle to write the state to.
     * @return True if all values were written successfully to the bundle. Otherwise, false with
     *         errors written to log.
     */
    public boolean onSaveSnapshot(Bundle mSavedInstanceState) {
        boolean success;
        Bundle blocklyState = new Bundle();

        // First attempt to save the workspace to a file.
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            mWorkspace.serializeToXml(out);
            blocklyState.putByteArray(SERIALIZED_WORKSPACE_KEY, out.toByteArray());
            success = true;
        } catch (BlocklySerializerException e) {
            Log.e(TAG, "Error serializing workspace.", e);
            success = false;
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                // Ignore.
            }
        }

        // TODO(#58): Save the rest of the state.

        mSavedInstanceState.putBundle(SNAPSHOT_BUNDLE_KEY, blocklyState);
        return success;
    }

    /**
     * Loads a Workspace state from an Android {@link Bundle}, previous saved in
     * {@link #onSaveSnapshot(Bundle)}.
     *
     * @param savedInstanceState The activity state Bundle passed into {@link Activity#onCreate} or
     *                           {@link Activity#onRestoreInstanceState}.
     * @return True if a Blockly state was found and successfully loaded into the Controller.
     *         Otherwise, false.
     */
    public boolean onRestoreSnapshot(@Nullable Bundle savedInstanceState) {

        Bundle blocklyState = (savedInstanceState == null) ? null :
                savedInstanceState.getBundle(SNAPSHOT_BUNDLE_KEY);
        if (blocklyState == null) {
            return  false;
        }
        byte[] bytes = blocklyState.getByteArray(SERIALIZED_WORKSPACE_KEY);
        if (bytes == null) {
            // Ignore all other workspace variables.
            return false;
        }

        boolean success = true;
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        try {
            loadWorkspaceContents(in);
        } catch (BlockLoadingException e) {
            Log.e(TAG, "Failed to load snapshot from Bundle.", e);
            success = false;
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                // Ignore.
            }
        }

        // TODO(#58): Restore the rest of the state.

        return success;
    }

    public Context getContext() {
        return mContext;
    }

    public Workspace getWorkspace() {
        return mWorkspace;
    }

    public BlockFactory getBlockFactory() {
        return mModelFactory;
    }

    public BlockViewFactory getBlockViewFactory() {
        return mViewFactory;
    }

    public WorkspaceHelper getWorkspaceHelper() {
        return mHelper;
    }

    public BlockClipDataHelper getClipDataHelper() {
        return mClipHelper;
    }

    /**
     * Closes any open flyouts if they are closeable.
     *
     * @return True if any flyouts were closed.
     */
    public boolean closeFlyouts() {
        return mFlyoutController.closeFlyouts();
    }

    /**
     * Registers a callback for Blockly events.
     *
     * @param callback The callback to add.
     */
    public void addCallback(EventsCallback callback) {
        if (!mListeners.contains(callback)) {
            mListeners.add(callback);
            mEventCallbackMask |= callback.getTypesBitmask();
        }
    }

    /**
     * Removes an events callback.
     *
     * @param callback The callback to remove.
     * @return True if the callback was found and removed.
     */
    public boolean removeCallback(EventsCallback callback) {
        boolean found = mListeners.remove(callback);
        if (found) {
            recalculateListenerEventMask();
        }
        return found;
    }

    /**
     * @return A count of registered {@link EventsCallback}s.
     */
    @VisibleForTesting
    public int getCallbackCount() {
        return mListeners.size();
    }

    /**
     * Runs a segment of code (immediately) such that all events caused by the changes are collected
     * into a single event group, and the group of events generated in that code is notified to
     * {@link EventsCallback}s at the completion of the code. If a {@code groupAndFireEvents()} call
     * is already in progress, the new code will integrate into that event group. This will catch
     * side-effect changes, such as block bumps or validation updates.
     * <p/>
     * {@code groupAndFireEvents()} must be called from the main thread/looper.
     */
    public void groupAndFireEvents(final Runnable runnable) {
        if (mMainLooper != Looper.myLooper()) {
            throw new IllegalStateException(
                    "groupAndFireEvents() must be called from main thread.");
        }
        if (mInEventGroup) {
            // We are already within an event group.  Execute immediately.
            runnable.run();
        } else {
            // Start a new event group, firing events when done.
            try {
                mInEventGroup = true;
                runnable.run();
            } finally {
                firePendingEvents();
                mInEventGroup = false;
            }
        }
    }

    /**
     * Adds {@code event} to the list of pending events. If this is called outside of a call to
     * {@link #groupAndFireEvents}, the event will be fired immediately, as its own group.
     * <p/>
     * {@code addPendingEvent()} must be called from the main thread/looper.
     *
     * @param event The event to append.
     */
    public void addPendingEvent(BlocklyEvent event) {
        if (mMainLooper != Looper.myLooper()) {
            throw new IllegalStateException("addPendingEvent() must be called from main thread.");
        }

        if (mPendingEvents == null) {
            mPendingEvents = new ArrayList<>();
        }
        mPendingEvents.add(event);
        mPendingEventsMask |= event.getTypeId();

        if (!mInEventGroup) {
            // Outside a prior event group.  Fire immediately.
            firePendingEvents();
        }
    }

    /**
     * Adds the provided block to the list of root blocks.  If the controller has an initialized
     * {@link WorkspaceView}, it will also create corresponding views.
     *
     * @param block The {@link Block} to add to the workspace.
     */
    public BlockGroup addRootBlock(final Block block) {
        if (block.getParentBlock() != null) {
            throw new IllegalArgumentException("New root block must not be connected.");
        }

        final BlockGroup newRootGroup[] = {null};
        groupAndFireEvents(new Runnable() {
            @Override
            public void run() {
                BlockGroup parentGroup = mHelper.getParentBlockGroup(block);
                newRootGroup[0] = addRootBlockImpl(
                        block, parentGroup, /* is new BlockView? */ parentGroup == null);
            }
        });
        return newRootGroup[0];
    }

    /**
     * Takes a block, and adds it to the root blocks, disconnecting previous or output connections,
     * if previously connected.  No action if the block was already a root block.
     *
     * @param block {@link Block} to extract as a root block in the workspace.
     */
    public void extractBlockAsRoot(final Block block) {
        groupAndFireEvents(new Runnable() {
            @Override
            public void run() {
                extractBlockAsRootImpl(block, false);
            }
        });
    }

    /**
     * Set up the {@link WorkspaceView} with this workspace's model. This method will perform the
     * following steps: <ul> <li>Set the block touch handler for the view.</li> <li>Configure the
     * dragger for the view.</li> <li>Recursively initialize views for all the blocks in the model
     * and add them to the view.</li> </ul>
     *
     * @param wv The root workspace view to add to.
     */
    public void initWorkspaceView(final WorkspaceView wv) {
        if (mVirtualWorkspaceView != null) {
            // Clear the old view's references so we don't get unwanted events.
            mVirtualWorkspaceView.setOnClickListener(null);
        }
        mVirtualWorkspaceView = (VirtualWorkspaceView) wv.getParent();
        if (mVirtualWorkspaceView != null) {
            mVirtualWorkspaceView.setOnClickListener(mDismissClickListener);
        }
        mWorkspaceView = wv;
        mWorkspaceView.setController(this);

        mHelper.setWorkspaceView(wv);
        mDragger.setWorkspaceView(mWorkspaceView);
        mWorkspaceView.setDragger(mDragger);
        initBlockViews();
    }

    /**
     * Recursively initialize views for all the blocks in the model and add them to the view.
     */
    public void initBlockViews() {
        if (mWorkspaceView != null) {
            List<Block> rootBlocks = mWorkspace.getRootBlocks();
            ConnectionManager connManager = mWorkspace.getConnectionManager();
            for (int i = 0; i < rootBlocks.size(); i++) {
                BlockGroup bg = mViewFactory.buildBlockGroupTree(
                        rootBlocks.get(i), connManager, mTouchHandler);
                mWorkspaceView.addView(bg);
            }
        }
    }

    /**
     * Returns the {@link VariableInfo} metadata about a specific variable.
     * @param variableName The variable queried.
     * @return The VariableInfo for {@code variableName}, or null if the name does not map to an
     *         existing variable.
     */
    @Nullable
    public VariableInfo getVariableInfo(String variableName) {
        return mWorkspace.getVariableInfo(variableName);
    }

    /**
     * Create a new variable with a given name. If a variable with the same name already exists the
     * name will be modified to be unique.
     *
     * @param variable The desired name of the variable to create.
     * @return The actual variable name that was created.
     */
    public String addVariable(final String variable) {
        final String[] resultVarName = { null };
        groupAndFireEvents(new Runnable() {
            @Override
            public void run() {
                resultVarName[0] = addVariableImpl(variable, true);
            }
        });
        return resultVarName[0];
    }

    /**
     * Attempt to create a new variable. If a {@link VariableCallback} is set
     * {@link VariableCallback#onCreateVariable(String)} will be called to check if the creation is
     * allowed. If a variable with the same name already exists the name will be modified to be
     * unique.
     *
     * @param variable The desired name of the variable to create.
     * @return The variable name that was created or null if creation was not allowed.
     */
    public String requestAddVariable(final String variable) {
        final String resultVarName[] = {null};
        groupAndFireEvents(new Runnable() {
            @Override
            public void run() {
                resultVarName[0] = addVariableImpl(variable, false);
            }
        });
        return resultVarName[0];
    }

    /**
     * Delete a variable from the workspace and remove all blocks using that variable.
     *
     * @param variable The variable to delete.
     *
     * @return True if the variable existed and was deleted, false otherwise.
     */
    public boolean deleteVariable(final String variable) {
        final boolean resultSuccess[] = {false};
        groupAndFireEvents(new Runnable() {
            @Override
            public void run() {
                resultSuccess[0] = deleteVariableImpl(variable, true);
            }
        });
        return resultSuccess[0];
    }

    /**
     * Attempt to delete a variable from the workspace. If a {@link VariableCallback} is set
     * {@link VariableCallback#onDeleteVariable} will be called to check if deletion is
     * allowed.
     *
     * @param variable The variable to delete.
     * @return True if the variable existed and was deleted, false otherwise.
     */
    public boolean requestDeleteVariable(final String variable) {
        final boolean[] resultSuccess = {false};
        groupAndFireEvents(new Runnable() {
            @Override
            public void run() {
                resultSuccess[0] = deleteVariableImpl(variable, false);
            }
        });
        return resultSuccess[0];
    }

    /**
     * Renames a variable in the workspace. If a variable already exists with the new name the
     * renamed variable will be modified to be unique. All fields that reference the renamed
     * variable will be updated to the new name.
     *
     * @param variable The variable to rename.
     * @param newVariable The new name for the variable.
     *
     * @return The new variable name that was saved.
     */
    public String renameVariable(final String variable, final String newVariable) {
        final String resultVarName[] = {null};
        groupAndFireEvents(new Runnable() {
            @Override
            public void run() {
                resultVarName[0] = renameVariableImpl(variable, newVariable, true);
            }
        });
        return resultVarName[0];
    }

    /**
     * Renames a variable in the workspace. If a {@link VariableCallback} is set
     * {@link VariableCallback#onRenameVariable(String, String)} will be called before renaming. If
     * a variable already exists with the new name the renamed variable will be modified to be
     * unique.
     *
     * @param variable The variable to rename.
     * @param newVariable The new name for the variable.
     *
     * @return The new variable name that was saved.
     */
    public String requestRenameVariable(final String variable, final String newVariable) {
        final String resultVarName[] = {null};
        groupAndFireEvents(new Runnable() {
            @Override
            public void run() {
                resultVarName[0] = renameVariableImpl(variable, newVariable, false);
            }
        });
        return resultVarName[0];
    }


    /**
     * Connects a block to a specific connection of another block.  The block must not already be
     * connected on the given connection; usually a root block. If another block is in the way
     * of making the connection (occupies the required workspace location), that block will be
     * bumped out of the way.
     * <p>
     * Note: The blocks involved are assumed to be in the workspace.
     *
     * @param blockConnection The open {@link Connection} on the block being connected.
     * @param otherConnection The target {@link Connection} to connect to. This may already be
     *                        connected.
     */
    public void connect(final Connection blockConnection, final Connection otherConnection) {
        groupAndFireEvents(new Runnable() {
            @Override
            public void run() {
                connectImpl(blockConnection, otherConnection);
            }
        });
    }

    /**
     * Offsets the root block of impingingConnection, to confusion occlusion.
     *
     * @param staticConnection The original connection of the block.
     * @param impingingConnection The connection of the block to offset.
     */
    public void bumpBlock(final Connection staticConnection, final Connection impingingConnection) {
        groupAndFireEvents(new Runnable() {
            @Override
            public void run() {
                bumpBlockImpl(staticConnection, impingingConnection);
            }
        });
    }

    /**
     * Move all neighbors of the current block and its sub-blocks so that they don't appear to be
     * connected to the current block.  Does not do anything in headless mode (no views attached).
     *
     * @param currentBlock The {@link Block} to bump others away from.
     */
    public void bumpNeighbors(final Block currentBlock) {
        groupAndFireEvents(new Runnable() {
            @Override
            public void run() {
                BlockGroup rootBlockGroup = mHelper.getRootBlockGroup(currentBlock);
                if (rootBlockGroup == null) {
                    return; // Do nothing, as connection locations are determined by views.
                }

                bumpNeighborsRecursively(currentBlock, rootBlockGroup);

                rootBlockGroup.requestLayout();
            }
        });
    }

    /**
     * Removes the given block from its parent, removes the block from the model, and then unlinks
     * all views.  All descendant of this block remain attached, and are thus also removed from the
     * workspace.
     *
     * @param block The {@link Block} to look up and remove.
     */
    public void removeBlockTree(final Block block) {
        groupAndFireEvents(new Runnable() {
            @Override
            public void run() {
                removeBlockTreeImpl(block);
            }
        });
    }

    /**
     * Remove a block and its descendants from the workspace and put it in the trash, respecting the
     * root block's deletable flag. Use this method for user actions.
     * <p>
     * Note: Child blocks marked undeletable may be deleted, and this behavior may change in the
     * future. See <a href="https://github.com/google/blockly-android/issues/370">issue #370</a>.
     *
     * @param block The block to remove, possibly with descendants attached.
     * @return True if the block was removed, false otherwise.
     */
    // TODO(#493): Sound Effect.
    public boolean trashRootBlock(final Block block) {
        final boolean rootFoundAndRemoved[] = {false};
        groupAndFireEvents(new Runnable() {
            @Override
            public void run() {
                rootFoundAndRemoved[0] = trashRootBlockImpl(block, true);
            }
        });
        return rootFoundAndRemoved[0];
    }

    /**
     * Remove a block and its descendants from the workspace and put it in the trash, regardless of
     * block's deletable state.  This method should only be used for programmatic manipulation of
     * the workspace.
     *
     * @param block The block to remove, possibly with descendants attached.
     * @return True if the block was removed, false otherwise.
     */
    public boolean trashRootBlockIgnoringDeletable(final Block block) {
        final boolean rootFoundAndRemoved[] = {false};
        groupAndFireEvents(new Runnable() {
            @Override
            public void run() {
                rootFoundAndRemoved[0] = trashRootBlockImpl(block, false);
            }
        });
        return rootFoundAndRemoved[0];
    }

    /**
     * Implements {@link #trashRootBlock(Block)}. The following events may be added to the
     * pending events:
     * <ol>
     *    <li>A delete of the block from the workspace.</li>
     * </ol>
     *
     * @param block {@link Block} to delete from the workspace.
     * @param respectDeletable If true, simply returns false when {@code block} is not deletable.
     * @return Whether the block was found among the root blocks and deleted.
     */
    // TODO(#56): Make this handle any block, not just root blocks.
    // TODO(#370): Handle isDeletable() in child blocks.
    private boolean trashRootBlockImpl(Block block, boolean respectDeletable) {
        if (respectDeletable && !block.isDeletable()) {
            return false;
        }

        boolean rootFoundAndRemoved = removeRootBlockImpl(block, true);
        if (rootFoundAndRemoved) {
            mWorkspace.addBlockToTrash(block);
            unlinkViews(block);

            if (hasCallback(BlocklyEvent.TYPE_DELETE)) {
                addPendingEvent(new BlocklyEvent.DeleteEvent(mWorkspace.getId(), block));
            }
        }

        return rootFoundAndRemoved;
    }
    /**
     * Moves a block (and the child blocks connected to it) from the trashed blocks (removing it
     * from the deleted blocks list), back to the workspace as a root block, including the
     * BlockGroup and other views in the trash {@link BlockListUI}.
     *
     * This method does not connect the block to existing blocks, even if the block was connected
     * before putting it in the trash.
     *
     * @param previouslyTrashedBlock The block in the trash to be moved back to the workspace.
     * @return The BlockGroup in the Workspace for the moved block.
     *
     * @throws IllegalArgumentException If {@code trashedBlock} is not found in the trashed blocks.
     */
    public BlockGroup addBlockFromTrash(final @NonNull Block previouslyTrashedBlock) {
        final BlockGroup trashedGroupRoot[] = {null};
        groupAndFireEvents(new Runnable() {
            @Override
            public void run() {
                trashedGroupRoot[0] = addBlockFromTrashImpl(previouslyTrashedBlock);
            }
        });
        return trashedGroupRoot[0];
    }

    /**
     * Implements {@link #addBlockFromTrash(Block)}. The following events may be added to the
     * pending events:
     * <ol>
     *    <li>A create event for adding the block back into the workspace.</li>
     * </ol>
     *
     * @param previouslyTrashedBlock {@link Block} to add back to the workspace from the trash.
     */
    private BlockGroup addBlockFromTrashImpl(@NonNull Block previouslyTrashedBlock) {
        BlockGroup bg = mHelper.getParentBlockGroup(previouslyTrashedBlock);
        if (bg != null) {
            ViewParent parent = bg.getParent();
            if (parent != null) {
                ((ViewGroup) parent).removeView(bg);
            }
            bg.setTouchHandler(mTouchHandler);
        }

        mWorkspace.addBlockFromTrash(previouslyTrashedBlock);
        if (mWorkspaceView != null) {
            if (bg == null) {
                bg = mViewFactory.buildBlockGroupTree(previouslyTrashedBlock,
                        mWorkspace.getConnectionManager(), mTouchHandler);
            }
            mWorkspaceView.addView(bg);
        }
        if (hasCallback(BlocklyEvent.TYPE_CREATE)) {
            addPendingEvent(new BlocklyEvent.CreateEvent(previouslyTrashedBlock));
        }
        return bg;
    }

    /**
     * Recursively unlinks models from the views, and disconnects the view tree including clearing
     * the parent {@link BlockGroup}.
     *
     * @param block The root block of the tree to unlink.
     */
    public void unlinkViews(Block block) {
        BlockView view = mHelper.getView(block);
        if (view == null) {
            return;  // No view to unlink.
        }

        // Verify the block has no parent.  Only unlink complete block sequences.
        if (block.getParentBlock() != null) {
            throw new IllegalArgumentException(
                    "Expected unconnected/root block; only allowed to unlink complete block trees");
        }

        BlockGroup parentGroup = mHelper.getParentBlockGroup(block);
        if (parentGroup != null) {
            if (parentGroup.getChildAt(0) != mHelper.getView(block)) {
                // If it doesn't have a parent, this Block view should have been first.
                throw new IllegalStateException("BlockGroup does not match model");
            }
            parentGroup.unlinkModel();
        } else {
            if (view != null) {
                view.unlinkModel();
            }
        }
    }

    /**
     * Zooms into the workspace (i.e., enlarges the blocks), if the WorkspaceView has been attached.
     *
     * @return True if a zoom was changed. Otherwise false.
     */
    public boolean zoomIn() {
        return (mVirtualWorkspaceView != null) && mVirtualWorkspaceView.zoomIn();
    }

    /**
     * Zooms out the workspace (i.e., smaller the blocks), if the WorkspaceView has been attached.
     *
     * @return True if a zoom was changed. Otherwise false.
     */
    public boolean zoomOut() {
        return (mVirtualWorkspaceView != null) && mVirtualWorkspaceView.zoomOut();
    }

    /**
     * Reset the view to the top-left corner of the virtual workspace (with a small margin), and
     * reset zoom to unit scale.
     */
    public void recenterWorkspace() {
        if (mVirtualWorkspaceView != null) {
            mVirtualWorkspaceView.resetView();
        }
    }

    /**
     * Clears the workspace of all blocks and the respective views from the {@link WorkspaceView},
     * if connected.
     */
    public void resetWorkspace() {
        // Unlink the Views before wiping out the model's root list.
        ArrayList<Block> rootBlocks = mWorkspace.getRootBlocks();
        for (int i = 0; i < rootBlocks.size(); ++i) {
            unlinkViews(rootBlocks.get(i));
        }
        List<Block> trashBlocks = new ArrayList<>();
        mWorkspace.getTrashCategory().getAllBlocksRecursive(trashBlocks);
        for (int i = 0; i < trashBlocks.size(); i++) {
            unlinkViews(trashBlocks.get(i));
        }
        mWorkspace.resetWorkspace();
        if (mWorkspaceView != null) {
            mWorkspaceView.removeAllViews();
            initBlockViews();
        }
        if (mFlyoutController != null) {
            mFlyoutController.setTrashContents(mWorkspace.getTrashCategory());
        }
    }

    /**
     * Removes all blocks from the {@link WorkspaceView} and puts them in the trash.
     */
    public void trashAllBlocks() {
        groupAndFireEvents(new Runnable() {
            @Override
            public void run() {
                List<Block> rootBlocks = new ArrayList<>(mWorkspace.getRootBlocks());
                for (Block block: rootBlocks) {
                    trashRootBlockImpl(block, true);
                }
            }
        });
    }

    /**
     * Adds the provided block as a root block.  If a {@link WorkspaceView} is attached, it will
     * also update the view, creating a new {@link BlockGroup} if not provided.
     * <p/>
     * If {@code isNewBlock} is {@code true}, the system will collect stats about the connections,
     * functions and variables. This should only be {@code  false} if re-adding a moodel previously
     * removed via {@link #removeRootBlockImpl(Block, boolean)} where {@code cleanupStats} was also
     * {@code false}.
     * <p/>
     * The following event may be added to the pending events:
     * <ol>
     *    <li>a create event if the block is new ({@code isNewBlock}).</li>
     * </ol>
     *
     * @param block The {@link Block} to add to the workspace.
     * @param bg The {@link BlockGroup} with block as the first {@link BlockView}.
     * @param isNewBlock Whether the block is new to the {@link Workspace} and the workspace should
     *                   collect stats for this tree.
     */
    private BlockGroup addRootBlockImpl(Block block, @Nullable BlockGroup bg, boolean isNewBlock) {
        mWorkspace.addRootBlock(block, isNewBlock);
        if (isNewBlock) {
            addPendingEvent(new BlocklyEvent.CreateEvent(block));
        }
        if (mWorkspaceView != null) {
            if (bg == null) {
                bg = mViewFactory.buildBlockGroupTree(block, mWorkspace.getConnectionManager(),
                        mTouchHandler);
            } else {
                bg.setTouchHandler(mTouchHandler);
            }
            mWorkspaceView.addView(bg);
        }
        return bg;
    }

    /**
     * Implements {@link #addVariable(String)}, without firing events.
     *
     * @param variable The desired name of the variable to create.
     * @param forced True to skip the variable callback and add the given variable name immediately.
     *               False to request confirmation from the callback first.
     * @return The actual variable name that was created or null if creation was blocked.
     */
    private String addVariableImpl(String variable, boolean forced) {
        if (!forced) {
            if (!mVariableCallback.onCreateVariable(variable)) {
                return null;
            }
        }
        String finalName = mWorkspace.addVariable(variable, true);
        // TODO: (#309) add new variable event
        return finalName;
    }

    /**
     * Implements {@link #deleteVariable(String)}, without firing events.
     *
     * @param variable The variable to remove.
     * @param forced True to force removal even if there's a callback to delegate the action to.
     *               This will not force the deletion of a procedure argument.
     * @return True if the variable was removed, false otherwise.
     */
    private boolean deleteVariableImpl(String variable, boolean forced) {
        VariableInfo varInfo = getVariableInfo(variable);
        if (varInfo != null) {
            if (varInfo.isProcedureArgument()) {
                if (mVariableCallback != null) {
                    mVariableCallback.onAlertCannotDeleteProcedureArgument(variable, varInfo);
                }
                return false;
            }

            if (!forced && mVariableCallback != null) {
                if (!mVariableCallback.onDeleteVariable(variable, varInfo)) {
                    return false;
                }
            }

            List<FieldVariable> fields = varInfo.getFields();
            mTempBlocks.clear(); // Visited / removed blocks (in case of block with multiple).
            int fieldCount = fields.size();
            for (int i = 0; i < fieldCount; ++i) {
                Block block = fields.get(i).getBlock();
                if (!mTempBlocks.contains(block)) {
                    removeBlockAndInputBlocksImpl(block);
                    mTempBlocks.add(block);
                }
            }
            mTempBlocks.clear();
        }
        // TODO: (#309) add remove variable event
        return mWorkspace.getVariableNameManager().remove(variable);
    }

    /**
     * Implements {@link #renameVariable(String, String)}.  The following events may be added to the
     * pending events:
     * <ol>
     *    <li>a change event for each variable field referencing the variable.</li>
     *    <li>a change mutation event for each procedure block.</li>
     * </ol>
     *
     * @param variable The variable to rename.
     * @param newVariable The new name for the variable.
     * @param forced True to skip the variable callback check and rename the variable immediately.
     * @return The new variable name that was saved.
     */
    private String renameVariableImpl(String variable, String newVariable, boolean forced) {
        if (!forced && mVariableCallback != null) {
            if (!mVariableCallback.onRenameVariable(variable, newVariable)) {
                return variable;
            }
        }

        NameManager varNameManager = mWorkspace.getVariableNameManager();
        if (variable == newVariable || !varNameManager.isValidName(newVariable)) {
            return variable;
        }
        String canonicalNewVar = varNameManager.makeCanonical(newVariable);

        newVariable = addVariableImpl(newVariable, true);

        VariableInfo oldVarInfo = mWorkspace.getVariableInfo(variable);
        if (oldVarInfo != null) {
            ProcedureManager procedureManager = mWorkspace.getProcedureManager();
            int procCount = oldVarInfo.getCountOfProceduresUsages();
            ArrayList<String> newArgs = new ArrayList<>();
            for (int i = 0; i < procCount; ++i) {
                String procName = oldVarInfo.getProcedureName(i);
                Block definition = procedureManager.getDefinitionBlocks().get(procName);
                ProcedureInfo oldProcInfo =
                        ((AbstractProcedureMutator) definition.getMutator()).getProcedureInfo();
                List<String> oldArgs = oldProcInfo.getArgumentNames();
                int argCount = oldArgs.size();

                newArgs.clear();
                for (int j = 0; j < argCount; ++j) {
                    String argName = oldArgs.get(j);
                    if (varNameManager.makeCanonical(argName).equals(canonicalNewVar)) {
                        argName = newVariable;
                    }
                    newArgs.add(argName);
                }

                // Mutate the procdure. This will mutate both the definition and the caller
                procedureManager.mutateProcedure(definition,
                        new ProcedureInfo(
                                procName, newArgs, oldProcInfo.getDefinitionHasStatementBody()));
            }

            List<FieldVariable> varRefs = oldVarInfo.getFields();
            if (varRefs != null) {
                for (FieldVariable field : varRefs) {
                    field.setVariable(newVariable);
                    BlocklyEvent.ChangeEvent change = BlocklyEvent.ChangeEvent
                            .newFieldValueEvent(field.getBlock(), field, variable, newVariable);
                    addPendingEvent(change);
                }
            }
        }

        deleteVariableImpl(variable, true);
        return newVariable;
    }

    /**
     * Implements {@link #removeBlockTree(Block)}. The following event may be added to the
     * pending events:
     * <ol>
     *    <li>a delete event for the block if found.</li>
     * </ol>
     *
     * @param block The {@link Block} to look up and remove.
     */
    private void removeBlockTreeImpl(Block block) {
        extractBlockAsRootImpl(block, false);
        if (removeRootBlockImpl(block, true)) {
            unlinkViews(block);
            addPendingEvent(new BlocklyEvent.DeleteEvent(getWorkspace().getId(), block));
        }
    }

    /**
     * Removes the given block from its parent and reparents its next block if it has one to its
     * former parent. Then removes the block from the model and unlinks all views.
     * <p>
     * This behaves similarly to {@link #removeBlockTree(Block)}, except it doesn't delete blocks
     * that come after the given block in sequence, only blocks connected to its inputs.
     * <p>
     * The following event may be added to the pending events:
     * <ol>
     *    <li>A move of the block to the workspace if it is not already a root block
     *        (via {@link #extractBlockAsRootImpl}).</li>
     *    <li>A move of the next block if a next block exists
     *        (via {@link #extractBlockAsRootImpl}).</li>
     *    <li>a delete event for the block if found.</li>
     * </ol>
     *
     * @param block The {@link Block} to look up and remove.
     * @return True if the block was removed, false if it wasn't found.
     */
    private boolean removeBlockAndInputBlocksImpl(Block block) {
        extractBlockAsRootImpl(block, true);
        boolean result = removeRootBlockImpl(block, true);
        unlinkViews(block);
        if (result) {
            addPendingEvent(new BlocklyEvent.DeleteEvent(getWorkspace().getId(), block));
        }
        return true;
    }

    /**
     * Removes the given block from the {@link Workspace} and removes its view from the
     * {@link WorkspaceView}.  If the block is not a root block of the workspace, the method does
     * nothing and returns false.
     * <p/>
     * If {@code cleanupStats} is {@code false}, the system will retain stats about the available
     * connections, function definition, function and variable references. This should only be used
     * if the block will be immediately re-added to the model view {@link #addRootBlock} with
     * {@code isNewBlock} also {@code false}.
     *
     * @param block The {@link Block} to look up and remove.
     * @param cleanupStats Removes connection info and other stats.
     */
    private boolean removeRootBlockImpl(Block block, boolean cleanupStats) {
        boolean rootFoundAndRemoved = mWorkspace.removeRootBlock(block, cleanupStats);
        if (rootFoundAndRemoved) {
            BlockView bv = mHelper.getView(block);
            if (bv != null) {
                BlockGroup group = bv.getParentBlockGroup();
                if (group != null) {
                    // Update UI
                    mWorkspaceView.removeView(group);
                }
            }
            if (cleanupStats) {
                mDragger.removeFromDraggingConnections(block);
            }
        }
        return rootFoundAndRemoved;
    }

    /**
     * Connect a block to a statement input of another block and update views as necessary.  If the
     * statement input already is connected to another block, splice the inferior block between
     * them.
     *
     * @param parentStatementConnection The {@link Connection} on the superior block to be connected
     * to.  Must be on a statement input.
     * @param toConnect The {@link Block} to connect to the statement input.
     */
    private void connectToStatementImpl(Connection parentStatementConnection, Block toConnect) {
        // Store the state of toConnect in its original location.
        // TODO: (#342) move the event up to the impl method
        BlocklyEvent.MoveEvent moveEvent = new BlocklyEvent.MoveEvent(toConnect);

        Block remainderBlock = parentStatementConnection.getTargetBlock();
        BlocklyEvent.MoveEvent remainderMove = null;
        // If there was already a block connected there.
        if (remainderBlock != null) {
            if (remainderBlock.isShadow()) {
                // If it was a shadow just remove it
                removeBlockTreeImpl(remainderBlock);
                remainderBlock = null;
            } else {
                // Store the original location of the remainder.
                remainderMove = new BlocklyEvent.MoveEvent(remainderBlock);

                // Disconnect the remainder and we'll reattach it below
                parentStatementConnection.disconnect();
                InputView parentInputView = parentStatementConnection.getInputView();
                if (parentInputView != null) {
                    parentInputView.setConnectedBlockGroup(null);
                }
            }
        }

        // Connect the new block to the parent
        connectAsInput(parentStatementConnection, toConnect.getPreviousConnection());
        moveEvent.recordNew(toConnect);
        addPendingEvent(moveEvent);

        // Reconnecting the remainder must be done after connecting the parent so that the parent
        // is considered in the workspace during connection checks.
        if (remainderBlock != null) {
            // Try to reconnect the remainder to the end of the new sequence. Shadows will be
            // replaced by the remainder.
            Block lastBlock = toConnect.getLastBlockInSequence();
            // If lastBlock doesn't have a next bump instead.
            if (lastBlock.getNextConnection() == null) {
                // Nothing to connect to.  Bump and add to root.
                addRootBlockImpl(
                        remainderBlock, mHelper.getParentBlockGroup(remainderBlock), false);

                bumpBlockImpl(parentStatementConnection, remainderBlock.getPreviousConnection());
            } else {
                // Connect the remainder
                connectAfter(lastBlock, remainderBlock);
            }

            if (remainderMove != null) {  // if not a shadow block.
                remainderMove.recordNew(remainderBlock);
                addPendingEvent(remainderMove);
            }
        }
    }

    /**
     * Connect a block after another block in the same block group.  Updates views as necessary.  If
     * the superior block already has a "next" block, splices the inferior block between the
     * superior block and its "next" block.
     * <p/>
     * Assumes that the inferior's previous connection is disconnected. Assumes that inferior's
     * blockGroup doesn't currently live at the root level.
     *
     * @param superior The {@link Block} after which the inferior block is connecting.
     * @param inferior The {@link Block} to be connected as the superior block's "next" block.
     */
    private void connectAfter(Block superior, Block inferior) {
        // Get the relevant BlockGroups.  Either may be null if view is not initialized.
        BlockGroup superiorBlockGroup = mHelper.getParentBlockGroup(superior);
        BlockGroup inferiorBlockGroup = mHelper.getParentBlockGroup(inferior);
        Block remainderBlock = superior.getNextBlock();
        BlockGroup remainderGroup = null;

        // To splice between two blocks, just need another call to connectAfter.
        if (remainderBlock != null) {
            if (remainderBlock.isShadow()) {
                // If there was a shadow connected just remove it
                removeBlockTreeImpl(remainderBlock);
                remainderBlock = null;
            } else {
                // Disconnect the remainder and save it for later
                remainderGroup = (superiorBlockGroup == null) ? null :
                        superiorBlockGroup.extractBlocksAsNewGroup(remainderBlock);
                superior.getNextConnection().disconnect();
            }
        }

        // Connect the new block to its parent
        connectAfter(superior, superiorBlockGroup, inferior, inferiorBlockGroup);

        // Reconnecting the remainder must be done after connecting the parent so that the parent
        // is considered in the workspace during connection checks.
        if (remainderBlock != null) {
            // Try to reconnect the remainder to the end of the new sequence. If the last block
            // has no next bump instead. Shadows will be replaced by the remainder.
            Block lastBlock = inferior.getLastBlockInSequence();
            if (lastBlock.getNextConnection() == null) {
                // Nothing to connect to.  Bump and add to root.
                addRootBlockImpl(remainderBlock, remainderGroup, false);
                bumpBlockImpl(inferior.getPreviousConnection(),
                        remainderBlock.getPreviousConnection());
            } else {
                // Connect the remainder
                connectAfter(lastBlock, superiorBlockGroup, remainderBlock, remainderGroup);
            }
        }
    }

    /**
     * Connects two blocks together in a previous-next relationship and merges the {@link
     * BlockGroup} of the inferior block into the {@link BlockGroup} of the superior block.
     *
     * @param superior The {@link Block} that the inferior block is moving to attach to.
     * @param superiorBlockGroup The {@link BlockGroup} belonging to the superior block.
     * @param inferior The {@link Block} that will follow immediately after the superior block.
     * @param inferiorBlockGroup The {@link BlockGroup} belonging to the inferior block.
     */
    private void connectAfter(Block superior, BlockGroup superiorBlockGroup,
                              Block inferior, BlockGroup inferiorBlockGroup) {
        // If there's still a next block at this point it should be a shadow. Double check and
        // remove it. If it's not a shadow something went wrong and connect() will crash.
        Block nextBlock = superior.getNextBlock();
        if (nextBlock != null && nextBlock.isShadow()) {
            removeBlockTree(nextBlock);
        }
        // The superior's next connection and the inferior's previous connections must already be
        // disconnected.
        superior.getNextConnection().connect(inferior.getPreviousConnection());
        if (superiorBlockGroup != null) {
            if (inferiorBlockGroup == null) {
                inferiorBlockGroup = mViewFactory.buildBlockGroupTree(
                        inferior, mWorkspace.getConnectionManager(), mTouchHandler);
            }
            superiorBlockGroup.moveBlocksFrom(inferiorBlockGroup, inferior);
        }
    }

    /**
     * Connect a block or block group to an input on another block and update views as necessary. If
     * the input was already connected, splice the child block or group in.
     *
     * @param parentConn The {@link Connection} on the superior block to connect to.  Must be an
     *                   input.
     * @param childConn The {@link Connection} on the inferior block.  Must be an output or previous
     *                  connection.
     */
    private void connectAsInput(Connection parentConn, Connection childConn) {
        InputView parentInputView = parentConn.getInputView();
        Block child = childConn.getBlock();
        BlockGroup childBlockGroup = mHelper.getParentBlockGroup(child);

        Connection previousTargetConnection = null;
        if (parentConn.isConnected()) {
            previousTargetConnection = parentConn.getTargetConnection();
            // If there was a shadow block here delete it from the hierarchy and forget about it.
            if (previousTargetConnection.getBlock().isShadow()) {
                removeBlockTreeImpl(previousTargetConnection.getBlock());
                previousTargetConnection = null;
            } else {
                // Otherwise just disconnect for now
                parentConn.disconnect();
                if (parentInputView != null) {
                    parentInputView.setConnectedBlockGroup(null);
                }
            }
        }

        // Connect the new block to its parent.
        parentConn.connect(childConn);

        // Try to reconnect the old block at the end.
        if (previousTargetConnection != null) {
            Block previousTargetBlock = previousTargetConnection.getBlock();

            // Traverse the tree to ensure it doesn't branch. We only reconnect if there's a
            // single place it could be reconnected to. The previousTarget will replace a shadow if
            // one was present.
            Connection lastInputConnection = child.getLastUnconnectedInputConnection();
            if (lastInputConnection == null) {
                // Bump and add back to root.
                BlockGroup previousTargetGroup =
                        mHelper.getParentBlockGroup(previousTargetBlock);
                addRootBlockImpl(previousTargetBlock, previousTargetGroup, false);
                bumpBlockImpl(parentConn, previousTargetConnection);
            } else {
                // Connect the previous part
                connectAsInput(lastInputConnection, previousTargetConnection);
            }
        }

        if (mWorkspaceView != null && parentInputView != null) {
            if (childBlockGroup == null) {
                childBlockGroup = mViewFactory.buildBlockGroupTree(
                        child, mWorkspace.getConnectionManager(), mTouchHandler);
            }
            parentInputView.setConnectedBlockGroup(childBlockGroup);
        }
    }

    /**
     * Implements {@link #extractBlockAsRoot(Block)}. The following events will be added to the
     * pending events:
     * <ol>
     *    <li>A move of the block to the workspace if it is not already a root block.</li>
     *    <li>A move of the next block if reattachNext is true and a next block exists.</li>
     * </ol>
     *
     * @param block {@link Block} to extract as a root block in the workspace.
     * @param reattachNext True to detach the next block if it exists and reattach it to the parent
     *                 (healing the stack), false to take all following blocks with this one.
     */
    private void extractBlockAsRootImpl(Block block, boolean reattachNext) {
        Block rootBlock = block.getRootBlock();
        if (block == rootBlock) {
            Block nextBlock = block.getNextBlock();
            if (reattachNext && nextBlock != null) {
                extractBlockAsRootImpl(nextBlock, false);
            }
            return;
        }
        // TODO: Document when this call valid but the root is not already part of the workspace.
        boolean isPartOfWorkspace = mWorkspace.isRootBlock(rootBlock);
        BlocklyEvent.MoveEvent moveEvent = new BlocklyEvent.MoveEvent(block);
        BlocklyEvent.MoveEvent remainderEvent = null;

        BlockView bv = mHelper.getView(block);
        BlockGroup bg = (bv == null) ? null : (BlockGroup) bv.getParent();
        BlockGroup originalRootBlockGroup = (mWorkspaceView == null) ? null
                : mHelper.getRootBlockGroup(block);
        Block remainderBlock = null;
        BlockGroup remainderGroup = null;
        if (reattachNext && block.getNextBlock() != null) {
            remainderBlock = block.getNextBlock();
            remainderEvent = new BlocklyEvent.MoveEvent(remainderBlock);

            remainderGroup = (bg == null) ? null :
                    bg.extractBlocksAsNewGroup(remainderBlock);
            block.getNextConnection().disconnect();
        }

        // Child block
        if (block.getParentConnection() != null) {
            Connection parentConnection = block.getParentConnection();
            Input in = parentConnection.getInput();
            if (in == null) {
                if (bg != null) {
                    // Next block
                    bg = bg.extractBlocksAsNewGroup(block);
                }
            } else {
                // Statement or value input
                // Disconnect view.
                InputView inView = in.getView();
                if (inView != null) {
                    inView.setConnectedBlockGroup(null);
                }
            }
            parentConnection.disconnect();
            // Check if we need to heal the stack, if not check if the block's old parent had a
            // shadow that we should create views for. If this is itself a shadow block the answer
            // is 'no'.
            if (remainderBlock != null && parentConnection
                    .canConnect(remainderBlock.getPreviousConnection())) {
                if (parentConnection.getInput() != null) {
                    connectToStatementImpl(parentConnection, remainderBlock);
                } else {
                    connectAfter(parentConnection.getBlock(), remainderBlock);
                }
            } else if (!block.isShadow() && parentConnection != null
                    && parentConnection.getShadowBlock() != null) {
                Block shadowBlock = parentConnection.getShadowBlock();
                // We add the shadow as a root and then connect it so we properly add all the
                // connectors and views.
                addRootBlockImpl(shadowBlock, null, true);
                connectImpl(parentConnection.getShadowConnection(), parentConnection);
            }
            // Add the remainder as a root block if it didn't get attached to anything
            if (remainderBlock != null && remainderBlock.getParentConnection() == null) {
                addRootBlockImpl(remainderBlock, remainderGroup, false);
            }
        }

        if (originalRootBlockGroup != null) {
            originalRootBlockGroup.requestLayout();
        }
        if (isPartOfWorkspace) {
            // Only add back to the workspace if the original tree is part of the workspace model.
            addRootBlockImpl(block, bg, false);
        }

        // Add pending events. Order is important to prevent side effects. Send the move event for
        // the first block, then the move event for its remainder.
        moveEvent.recordNew(block);
        addPendingEvent(moveEvent);
        if (remainderEvent != null) {
            remainderEvent.recordNew(remainderBlock);
            addPendingEvent(remainderEvent);
        }
    }

    /**
     * Populates the toolbox UIs with the current toolbox contents.
     */
    private void updateToolbox() {
        if (mFlyoutController != null) {
            mFlyoutController.setToolboxRoot(mWorkspace.getToolboxContents());
        }
    }

    /**
     * This implements {@link #connect(Connection, Connection)}, without firing events so multiple
     * events can accumulate in recursive calls.
     *
     * @param blockConnection The {@link Connection} on the block being moved.
     * @param otherConnection The target {@link Connection} to connect to.
     */
    private void connectImpl(Connection blockConnection, Connection otherConnection) {
        if (blockConnection.isConnected()) {
            throw new IllegalArgumentException("The blockConnection was already connected.");
        }
        Block block = blockConnection.getBlock();
        Block newParentBlock = otherConnection.getBlock();

        switch (blockConnection.getType()) {
            case Connection.CONNECTION_TYPE_OUTPUT:
                removeRootBlockImpl(block, false);
                connectAsInput(otherConnection, blockConnection);
                break;
            case Connection.CONNECTION_TYPE_PREVIOUS:
                removeRootBlockImpl(block, false);
                if (otherConnection.isStatementInput()) {
                    connectToStatementImpl(otherConnection, block);
                } else {
                    connectAfter(newParentBlock, block);
                }
                break;
            case Connection.CONNECTION_TYPE_NEXT:
                if (!otherConnection.isConnected()) {
                    removeRootBlockImpl(newParentBlock, false);
                }
                if (blockConnection.isStatementInput()) {
                    connectToStatementImpl(blockConnection, newParentBlock);
                } else {
                    connectAfter(block, newParentBlock);
                }
                break;
            case Connection.CONNECTION_TYPE_INPUT:
                if (!otherConnection.isConnected()) {
                    removeRootBlockImpl(newParentBlock, false);
                }
                connectAsInput(blockConnection, otherConnection);
                break;
            default:
                break;
        }

        BlockGroup rootBlockGroup = mHelper.getRootBlockGroup(block);
        if (rootBlockGroup != null) {
            bumpNeighborsRecursively(block, rootBlockGroup);
        } // otherwise we are probably running headless, without views.
    }

    /**
     * Implements {@link #bumpBlock(Connection, Connection)}. This is not responsible for firing
     * events.
     *
     * @param staticConnection The original connection of the block.
     * @param impingingConnection The connection of the block to offset.
     */
    private void bumpBlockImpl(Connection staticConnection, Connection impingingConnection) {
        Block rootBlock = impingingConnection.getBlock().getRootBlock();
        BlockGroup impingingBlockGroup = mHelper.getRootBlockGroup(rootBlock);

        int maxSnapDistance = mHelper.getMaxSnapDistance();
        float dx = (staticConnection.getPosition().x + maxSnapDistance)
                - impingingConnection.getPosition().x;
        float dy = (staticConnection.getPosition().y + maxSnapDistance)
                - impingingConnection.getPosition().y;
        rootBlock.setPosition(rootBlock.getPosition().x + dx, rootBlock.getPosition().y + dy);

        if (mWorkspaceView != null && impingingBlockGroup != null) {
            // Update UI
            impingingBlockGroup.bringToFront();
            impingingBlockGroup.updateAllConnectorLocations();
            mWorkspaceView.requestLayout();
        }
    }

    /**
     * Recursive implementation of {@link #bumpNeighbors(Block)}.  It is not responsible for firing
     * events.
     *
     * @param currentBlock The {@link Block} to bump others away from.
     * @param rootBlockGroup The root {@link BlockGroup} containing {@code currentBlock}.
     */
    private void bumpNeighborsRecursively(Block currentBlock, BlockGroup rootBlockGroup) {
        List<Connection> connectionsOnBlock = new ArrayList<>();
        rootBlockGroup.updateAllConnectorLocations();
        // Move this block before trying to bump others
        Connection prev = currentBlock.getPreviousConnection();
        if (prev != null && !prev.isConnected()) {
            bumpInferior(rootBlockGroup, prev);
        }
        Connection out = currentBlock.getOutputConnection();
        if (out != null && !out.isConnected()) {
            bumpInferior(rootBlockGroup, out);
        }

        currentBlock.getAllConnections(connectionsOnBlock);
        for (int i = 0; i < connectionsOnBlock.size(); i++) {
            Connection conn = connectionsOnBlock.get(i);
            if (conn.isHighPriority()) {
                if (conn.isConnected()) {
                    bumpNeighborsRecursively(conn.getTargetBlock(), rootBlockGroup);
                }
                bumpConnectionNeighbors(conn, rootBlockGroup);
            }
        }
    }

    /**
     * Bump the block containing {@code lowerPriority} away from the first nearby block it finds.
     *
     * @param rootBlockGroup The root block group of the block being bumped.
     * @param lowerPriority The low priority connection that is the center of the current bump
     * operation.
     */
    private void bumpInferior(BlockGroup rootBlockGroup, Connection lowerPriority) {
        getBumpableNeighbors(lowerPriority, mTempConnections);
        // Bump from the first one that isn't in the same block group.
        for (int j = 0; j < mTempConnections.size(); j++) {
            Connection curNeighbour = mTempConnections.get(j);
            if (mHelper.getRootBlockGroup(curNeighbour.getBlock()) != rootBlockGroup) {
                bumpBlockImpl(curNeighbour, lowerPriority);
                return;
            }
        }
    }

    /**
     * Find all connections near a given connection and bump their blocks away.
     *
     * @param conn The high priority connection that is at the center of the current bump
     * operation.
     * @param rootBlockGroup The root block group of the block conn belongs to.
     */
    private void bumpConnectionNeighbors(Connection conn, BlockGroup rootBlockGroup) {
        getBumpableNeighbors(conn, mTempConnections);
        for (int j = 0; j < mTempConnections.size(); j++) {
            Connection curNeighbour = mTempConnections.get(j);
            BlockGroup neighbourBlockGroup = mHelper.getRootBlockGroup(
                    curNeighbour.getBlock());
            if (neighbourBlockGroup != rootBlockGroup) {
                bumpBlockImpl(conn, curNeighbour);
            }
        }
    }

    private void getBumpableNeighbors(Connection conn, List<Connection> result) {
        int snapDistance = mHelper.getMaxSnapDistance();
        mConnectionManager.getNeighbors(conn, snapDistance, result);
    }

    private boolean hasCallback(@BlocklyEvent.EventType int typeQueryBitMask) {
        return (mEventCallbackMask & typeQueryBitMask) != 0;
    }

    private void recalculateListenerEventMask() {
        mEventCallbackMask = 0;
        for (EventsCallback listener : mListeners) {
            mEventCallbackMask |= listener.getTypesBitmask();
        }
    }

    private void firePendingEvents() {
        List<BlocklyEvent> unmodifiableEventList = null;
        for (EventsCallback listener : mListeners) {
            if ((mPendingEventsMask & listener.getTypesBitmask()) != 0) {
                if (unmodifiableEventList == null) {
                    unmodifiableEventList = Collections.unmodifiableList(mPendingEvents);
                }
                listener.onEventGroup(unmodifiableEventList);
            }
        }

        mPendingEvents = null;
        mPendingEventsMask = 0;
    }

    /**
     * Builder for configuring a new controller and workspace.
     */
    public static class Builder {
        private Context mContext;
        private WorkspaceHelper mWorkspaceHelper;
        private BlockClipDataHelper mClipHelper;
        private BlockViewFactory mViewFactory;
        private VariableCallback mVariableCallback;
        private WorkspaceFragment mWorkspaceFragment;
        private BlockListUI mToolbox;
        private CategorySelectorUI mCategoryUi;
        private BlockListUI mTrashUi;
        private View mTrashIcon;

        // TODO: Should these be part of the style?
        private int mToolboxResId;
        private String mToolboxAssetPath;
        private String mToolboxXml;
        private ArrayList<Integer> mBlockDefResources = new ArrayList<>();
        private ArrayList<String> mBlockDefAssets = new ArrayList<>();

        public Builder(Context context) {
            mContext = context;
        }

        public Builder setWorkspaceHelper(WorkspaceHelper workspaceHelper) {
            mWorkspaceHelper = workspaceHelper;
            return this;
        }

        public Builder setClipDataHelper(BlockClipDataHelper clipHelper) {
            mClipHelper = clipHelper;
            return this;
        }

        public Builder setBlockViewFactory(BlockViewFactory blockViewFactory) {
            mViewFactory = blockViewFactory;
            return this;
        }

        public Builder setVariableCallback(VariableCallback variableCallback) {
            mVariableCallback = variableCallback;
            return this;
        }

        public Builder setWorkspaceFragment(WorkspaceFragment workspace) {
            mWorkspaceFragment = workspace;
            return this;
        }

        /**
         * Sets the ui used for the toolbox, including the flyout with blocks and the
         * category tabs.
         * <p/>
         * <strong>Warning:</strong> It is possible this toolbox will be loaded during
         * {@link Builder#build()}, before the application has a chance to register any
         * {@link Mutator}s or {@link BlockExtension}s. Make sure, if using this convenience
         * {@code Builder} method, the referenced blocks do not depend upon such features.
         * Otherwise, set the toolbox later by calling {@link BlocklyController#setToolboxUi} on the
         * controller, post construction.
         *
         * @param toolbox The {@link BlockListUI} to use to display blocks in the current
         *                category.
         * @param categoryUi The BlockListUI for displaying the list of
         *                         categories.
         * @return this
         */
        public Builder setToolboxUi(BlockListUI toolbox,
                                    CategorySelectorUI categoryUi) {
            mToolbox = toolbox;
            mCategoryUi = categoryUi;
            return this;
        }

        /**
         * Sets the BlockListUI to be used for the trash. This must be a different ui than
         * the toolbox flyout ui.
         *
         * @param trash The {@link BlockListUI} to use to display blocks in the trash.
         * @return this
         */
        public Builder setTrashUi(BlockListUI trash) {
            mTrashUi = trash;
            return this;
        }

        /**
         * Sets a View to use as a drop target and click target for opening/closing
         * the trash. Since the icon may not be inflated yet this can be added after creating the
         * controller with {@link BlocklyController#setTrashIcon(View)}.
         *
         * @param trashIcon The view to use for opening/closing the trash.
         * @return this
         */
        public Builder setTrashIcon(View trashIcon) {
            mTrashIcon = trashIcon;
            return this;
        }

        /**
         * Add a set of block definitions to load from a resource file. These will be added to the
         * set of all known blocks, but will not appear in the user's toolbox unless they are also
         * defined in the toolbox configuration via {@link #setToolboxConfigurationResId(int)}.
         * <p/>
         * The resource must be a json file in the raw directory. If the file contains blocks that
         * were previously defined they will be overridden.
         * <p/>
         * A duplicate block is any block with the same {@link Block#getType() type}.
         *
         * @param blockDefinitionsResId The resource to load blocks from.
         * @return this
         */
        public Builder addBlockDefinitions(int blockDefinitionsResId) {
            mBlockDefResources.add(blockDefinitionsResId);
            return this;
        }

        /**
         * Add a set of block definitions to load from a JSON asset file. These will be added to the
         * set of all known blocks, but will not appear in the user's toolbox unless they are also
         * defined in the toolbox configuration via {@link #setToolboxConfigurationResId(int)}.
         * <p/>
         * The asset name must be a path to a file in the assets directory. If the file contains
         * blocks that were previously defined, they will be overridden. A duplicate block is any
         * block with the same {@link Block#getType() type}.
         *
         * @param assetName the path of the asset to load from.
         * @return this
         */
        public Builder addBlockDefinitionsFromAsset(String assetName) {
            mBlockDefAssets.add(assetName);
            return this;
        }

        /**
         * Add sets of block definitions to load from multiple JSON asset file. These will be added
         * to the set of all known blocks, but will not appear in the user's toolbox unless they are
         * also defined in the toolbox configuration via {@link #setToolboxConfigurationResId(int)}.
         * <p/>
         * The asset names must be a path to files in the assets directory. If the files contain
         * blocks that were previously defined, they will be overridden. A duplicate block is any
         * block with the same {@link Block#getType() type}.
         *
         * @param assetNames The paths of the assets to load.
         * @return this
         */
        public Builder addBlockDefinitionsFromAssets(List<String> assetNames) {
            for (String assetName : assetNames) {
                mBlockDefAssets.add(assetName);
            }
            return this;
        }

        /**
         * Sets the resource to load the toolbox configuration from. This must be an xml resource in
         * the raw directory.
         * <p/>
         * If this is set, {@link #setToolboxConfigurationAsset(String)} may not be set.
         *
         * @param toolboxResId The resource id for the toolbox config file.
         * @return this
         */
        public Builder setToolboxConfigurationResId(int toolboxResId) {
            if (mToolboxXml != null && mToolboxAssetPath != null) {
                throw new IllegalStateException("Toolbox res id may not be set if xml is set.");
            }
            mToolboxResId = toolboxResId;
            return this;
        }

        /**
         * Sets the asset to load the toolbox configuration from. The asset name must be a path to a
         * file in the assets directory.
         * <p/>
         * If this is set {@link #setToolboxConfigurationResId(int)} may not be set.
         *
         * @param assetName The asset for the toolbox config file.
         * @return this
         */
        public Builder setToolboxConfigurationAsset(String assetName) {
            if (mToolboxXml != null && mToolboxResId != 0) {
                throw new IllegalStateException("Toolbox res id may not be set if xml is set.");
            }
            mToolboxAssetPath = assetName;
            return this;
        }

        /**
         * Create a new workspace using the configuration in this builder.
         *
         * @return A new {@link BlocklyController}.
         */
        public BlocklyController build() {
            if (mViewFactory == null && (mWorkspaceFragment != null || mTrashUi != null
                    || mToolbox != null || mCategoryUi != null)) {
                throw new IllegalStateException(
                        "BlockViewFactory cannot be null when using UIs.");
            }

            if (mWorkspaceHelper == null) {
                mWorkspaceHelper = new WorkspaceHelper(mContext);
            }

            BlockClipDataHelper blockClipDataHelper = mClipHelper;
            if (blockClipDataHelper == null) {
                blockClipDataHelper = SingleMimeTypeClipDataHelper.getDefault(mContext);
            }

            BlockFactory factory = new BlockFactory();
            loadBlockDefinitionsFromResources(factory, mBlockDefResources);
            loadBlockDefinitionsFromAssets(factory, mBlockDefAssets);
            BlocklyController controller = new BlocklyController(
                    mContext, factory, mWorkspaceHelper, blockClipDataHelper, mViewFactory);
            loadToolbox(controller);

            // Any of the following may be null and result in a no-op.
            controller.setWorkspaceFragment(mWorkspaceFragment);
            controller.setTrashUi(mTrashUi);
            controller.setToolboxUi(mToolbox, mCategoryUi);
            controller.setTrashIcon(mTrashIcon);
            controller.setVariableCallback(mVariableCallback);

            return controller;
        }

        /**
         * Loads list of block definitions from asset files or fail with IllegalStateException.
         *
         * @param factory The factory to add block definitions to.
         * @param assetPaths The paths to JSON block definition files.
         */
        private void loadBlockDefinitionsFromAssets(BlockFactory factory, List<String> assetPaths) {
            for (int i = 0; i < assetPaths.size(); i++) {
                String assetPath = assetPaths.get(i);
                try {
                    factory.addJsonDefinitions(mContext.getAssets().open(assetPath));
                } catch (IOException | BlockLoadingException e) {
                    factory.clear();  // Clear partially loaded resources.
                    // Compile-time bundled assets are assumed to always be valid.
                    throw new IllegalStateException("Failed to load block definitions from asset: "
                            + assetPath, e);
                }
            }
        }

        /**
         * Loads list of block definitions from resources or fail with IllegalStateException.
         *
         * @param factory The factory to add block definitions to.
         * @param resIds The resource ids to raw XML files with the block definitions.
         */
        private void loadBlockDefinitionsFromResources(BlockFactory factory, List<Integer> resIds) {
            Resources resources = mContext.getResources();
            for (int i = 0; i < resIds.size(); i++) {
                int resourceId = resIds.get(i);
                try {
                    InputStream is = resources.openRawResource(resourceId);
                    factory.addJsonDefinitions(is);
                } catch (IOException | BlockLoadingException e) {
                    factory.clear();  // Clear partially loaded resources.
                    // Compile-time bundled assets are assumed to always be valid.
                    String resName = resources.getResourceName(resourceId);
                    throw new IllegalStateException(
                            "Failed to load block definition from resource id " + resourceId
                            + (resName == null ? "" : " \"" + resName + "\""), e);
                }
            }
        }

        /**
         * Loads the toolbox from {@link #mToolboxResId}, {@link #mToolboxXml}, or
         * {@link #mToolboxAssetPath}. If loading fails, throw an IllegalStateException since this
         * is a early initialization (and probably compile time) resource.
         *
         * @param controller The controller to load into.
         */
        private void loadToolbox(BlocklyController controller) {
            String fromClausePeriod = ".";
            try {
                if (mToolboxResId != 0) {
                    fromClausePeriod = " from resources.";
                    controller.loadToolboxContents(mToolboxResId);
                } else if (mToolboxXml != null) {
                    fromClausePeriod = " from raw XML.";
                    controller.loadToolboxContents(mToolboxXml);
                } else if (mToolboxAssetPath != null && mContext.getAssets() != null) {
                    fromClausePeriod = " from asset \"" + mToolboxAssetPath + "\".";

                    try {
                        controller.loadToolboxContents(
                                mContext.getAssets().open(mToolboxAssetPath));
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to initialize toolbox blocks from assets.", e);
                    }
                }
            } catch (BlockLoadingException e) {
                // Compile-time bundled assets are assumed to always be valid.
                throw new IllegalStateException("Failed to load toolbox" + fromClausePeriod, e);
            }
        }

    }

    /**
     * Callback for handling requests to modify the list of variables. This can be used to show a
     * confirmation dialog when deleting a variable, or customize the UI shown for creating/editing
     * a variable.
     */
    public abstract static class VariableCallback {

        /**
         * Sent when the user tries to remove a variable. If true is returned the variable and
         * any blocks referencing it will be deleted.
         *
         * @param variable The variable being deleted.
         * @return True to allow the delete, false to prevent it.
         */
        public boolean onDeleteVariable(String variable, VariableInfo info) {
            return true;
        }

        /**
         * Sent when the user tries to create a new variable. If true is returned a variable
         * will be created with the next available default name. If callers wish to modify and then
         * create a variable they should return false and then call
         * {@link BlocklyController#addVariable(String)} with the new variable and forced
         * set to true.
         *
         * @param varName The initial variable name or null if no starting name was specified.
         * @return True to create the named variable, false to handle it yourself.
         */
        public boolean onCreateVariable(String varName) {
            return true;
        }

        /**
         * Sent when the user tries to rename a variable. There is no default handling for variable
         * renaming in the controller, so an application must override this to support renaming
         * variables.
         *
         * @param variable The variable to rename.
         * @param newVariable The new name for the variable.
         * @return True to perform the rename, false to handle it yourself.
         */
        public boolean onRenameVariable(String variable, String newVariable) {
            return true;
        }

        /**
         * Sent when a user attempts to delete a variable that is used as a procedure argument.
         * @param variableName The name of the variable.
         * @param info The info for this variable.
         */
        public abstract void onAlertCannotDeleteProcedureArgument(
                String variableName, VariableInfo info);
    }
}
