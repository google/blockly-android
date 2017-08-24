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

package com.google.blockly.android.ui;

import android.content.Context;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.SpinnerAdapter;

import com.google.blockly.android.FlyoutFragment;
import com.google.blockly.android.WorkspaceFragment;
import com.google.blockly.android.control.ConnectionManager;
import com.google.blockly.android.control.NameManager;
import com.google.blockly.android.ui.fieldview.BasicFieldAngleView;
import com.google.blockly.android.ui.fieldview.BasicFieldCheckboxView;
import com.google.blockly.android.ui.fieldview.BasicFieldColorView;
import com.google.blockly.android.ui.fieldview.BasicFieldDateView;
import com.google.blockly.android.ui.fieldview.BasicFieldDropdownView;
import com.google.blockly.android.ui.fieldview.BasicFieldImageView;
import com.google.blockly.android.ui.fieldview.BasicFieldInputView;
import com.google.blockly.android.ui.fieldview.BasicFieldLabelView;
import com.google.blockly.android.ui.fieldview.BasicFieldNumberView;
import com.google.blockly.android.ui.fieldview.BasicFieldVariableView;
import com.google.blockly.android.ui.fieldview.FieldView;
import com.google.blockly.android.ui.fieldview.VariableRequestCallback;
import com.google.blockly.model.Block;
import com.google.blockly.model.Field;
import com.google.blockly.model.Input;
import com.google.blockly.model.Mutator;
import com.google.blockly.model.Workspace;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base factory class responsible for creating all the {@link BlockView}s, {@link InputView}s, and
 * {@link FieldView}s for a given block representation.  Complete view trees are constructed via
 * calls to {@link #buildBlockGroupTree} or {@link #buildBlockViewTree}.
 * <p/>
 * Subclasses must override {@link #buildBlockView}, {@link #buildInputView}, and
 * {@link #buildFieldView}. They may also override {@link #buildBlockGroup()}, to provide customized
 * implementations to the container view.
 */
public abstract class BlockViewFactory<BlockView extends com.google.blockly.android.ui.BlockView,
                                       InputView extends com.google.blockly.android.ui.InputView> {
    private static final String TAG = "BlockViewFactory";

    /**
     * Context for creating or loading views.
     */
    protected Context mContext;
    /**
     * Helper for doing conversions and style lookups.
     */
    protected WorkspaceHelper mHelper;
    /**
     * Name manager for the list of variables in this instance of Blockly.
     */
    protected NameManager mVariableNameManager;
    /**
     * The callback to use for views that can request changes to the list of variables.
     */
    protected VariableRequestCallback mVariableCallback;
    /**
     * The callback to use when something triggers showing or hiding a block's mutator.
     */
    protected MutatorToggleListener mMutatorListener;

    private SpinnerAdapter mVariableAdapter;
    private final Map<String, MutatorFragment.Factory> mMutatorUiFactories = new HashMap<>();

    // TODO(#137): Move to ViewPool class.
    protected final Map<String,WeakReference<BlockView>> mBlockIdToView
            = Collections.synchronizedMap(new HashMap<String, WeakReference<BlockView>>());

    protected BlockViewFactory(Context context, WorkspaceHelper helper) {
        mContext = context;
        mHelper = helper;
        helper.setBlockViewFactory(this);
    }

    /**
     * Sets the callback to use for variable view events, such as the user selected delete/rename.
     *
     * @param callback The callback to set on variable field views.
     */
    public void setVariableRequestCallback(VariableRequestCallback callback) {
        mVariableCallback = callback;
    }

    /**
     * Sets the listener to call when the user toggles a mutator. This is typically in response to
     * a mutator button being tapped on a block.
     *
     * @param listener The listener to call when a user toggles a mutator.
     */
    public void setMutatorToggleListener(MutatorToggleListener listener) {
        mMutatorListener = listener;
    }

    public WorkspaceHelper getWorkspaceHelper() {
        return mHelper;
    }

    /**
     * Set the {@link NameManager} being used to track variables in the workspace.
     *
     * @param variableNameManager The name manager for the variables in the associated workspace.
     */
    public void setVariableNameManager(NameManager variableNameManager) {
        mVariableNameManager = variableNameManager;
    }

    /**
     * Registers a {@link MutatorFragment.Factory} for the named mutator type. Mutators that have a
     * UI registered will display a user affordance for opening and closing the UI.
     *
     * @param mutatorId The name / id of this mutator type.
     * @param mutatorUiFactory The factory that builds fragments for this mutator type.
     */
    public void registerMutatorUi(String mutatorId, MutatorFragment.Factory mutatorUiFactory) {
        MutatorFragment.Factory old = mMutatorUiFactories.get(mutatorId);
        if (mutatorUiFactory == old) {
            return;
        }
        mMutatorUiFactories.put(mutatorId, mutatorUiFactory);
    }

    /**
     * @return True if there is a UI registered for the given mutator id.
     */
    public boolean hasUiForMutator(String mutatorId) {
        return mMutatorUiFactories.get(mutatorId) != null;
    }

    /**
     * Retrieves a {@link MutatorFragment} for the given mutator. They fragment may be displayed to
     * the user. If a fragment could not be found null is returned instead.
     *
     * @param mutator The mutator to return a fragment for.
     * @return A {@link MutatorFragment} that can be shown to the user.
     */
    @Nullable
    public MutatorFragment getMutatorFragment(Mutator mutator) {
        MutatorFragment.Factory factory = mMutatorUiFactories.get(mutator.getMutatorId());
        if (factory == null) {
            return null;
        }
        return factory.newMutatorFragment(mutator);
    }

    /**
     * Creates a {@link BlockGroup} for the given block and its children using the workspace's
     * default style.
     *
     * @param rootBlock The root block to generate a view for.
     * @param connectionManager The {@link ConnectionManager} to update when moving connections.
     * @param touchHandler The {@link BlockTouchHandler} to manage all touches.
     *
     * @return A view for the block.
     */
    public final BlockGroup buildBlockGroupTree(Block rootBlock,
                                          ConnectionManager connectionManager,
                                          BlockTouchHandler touchHandler) {
        BlockGroup bg = buildBlockGroup();
        buildBlockViewTree(rootBlock, bg, connectionManager, touchHandler);
        return bg;
    }

    /**
     * Called to construct the complete hierarchy of views representing a {@link Block} and its
     * subcomponents, added to {@code parentGroup}.
     *
     * @param block The root block to generate a view for.
     * @param parentGroup T
     * @param connectionManager The {@link ConnectionManager} to update when moving connections.
     * @param touchHandler The {@link BlockTouchHandler} to manage all touches.
     *
     * @return A view for the block and all its descendants.
     */
    public final BlockView buildBlockViewTree(Block block, BlockGroup parentGroup,
                                        ConnectionManager connectionManager,
                                        BlockTouchHandler touchHandler) {
        BlockView blockView = getView(block);
        if (blockView != null) {
            throw new IllegalStateException("BlockView already created.");
        }

        List<InputView> inputViews = buildInputViews(block, connectionManager, touchHandler);
        blockView = buildBlockView(block, inputViews, connectionManager, touchHandler);

        // TODO(#137): Move to ViewPool class.
        mBlockIdToView.put(block.getId(), new WeakReference<>(blockView));

        parentGroup.addView((View) blockView);

        Block next = block.getNextBlock();
        if (next != null) {
            // Recursively calls buildBlockViewTree(..) for the rest of the sequence.
            buildBlockViewTree(next, parentGroup, connectionManager, touchHandler);
        }

        return blockView;
    }

    /**
     * This returns the view constructed to represent a {@link Block}.  Each block is only allowed
     * one view instance among the views managed by this factory (including
     * {@link WorkspaceFragment} and any {@link FlyoutFragment FlyoutFragments}. Views are
     * constructed in {@link #buildBlockViewTree}, either directly or via recursion.  If the block
     * view has not been constructed, this method will return null.
     * <p/>
     * Calling {@link BlockView#unlinkModel()} (possibly via {@link BlockGroup#unlinkModel()}) will
     * disconnect this view from its model, and will it no longer be returned from this method.
     *
     * @param block The {@link Block} to get the view for.
     * @return The previously constructed and active view of {@code block}. Otherwise null.
     */
    @Nullable
    public final BlockView getView(Block block) {
        WeakReference<BlockView> viewRef = mBlockIdToView.get(block.getId());
        return viewRef == null ? null : viewRef.get();
    }

    /**
     * @return A new, empty {@link BlockGroup} container view for a sequence of blocks.
     */
    public BlockGroup buildBlockGroup() {
        return new BlockGroup(mContext, mHelper);
    }

    /**
     * Rebuilds a BlockView based on the latest model state, replacing it in-place in the view tree.
     * Some minor view state is not propagated (pressed, highlight, text selection, etc.) during the
     * replacement.
     *
     * @param original The original BlockView to be rebuilt and replaced.
     * @return The newly reconstructed BlockView.
     * @throws ClassCastException If original is connected to a parent that is not a ViewGroup.
     */
    // TODO(#588): Replace with in-place view shaping, to preserve view state (open dropdowns, text
    //             selection, etc.) and minimize risk of breaking object references.
    // TODO(#589): Testing
    @NonNull
    public BlockView rebuildBlockView(BlockView original) {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            throw new IllegalStateException("rebuildBlockView() must run on the main thread.");
        }

        Block model = original.getBlock();
        if (getView(model) != original) {
            throw new IllegalStateException(
                    "Refusing to rebuild a BlockView that is not the block's current/active view.");
        }

        // Get parent as ViewGroup first, so any failure to cast to ViewGroup fails early.
        ViewGroup parent = (ViewGroup) original.getParent();


        ConnectionManager connectionManager = original.getConnectionManager();
        BlockTouchHandler touchHandler = original.getTouchHandler();

        original.unlinkModel();

        for (Input input : model.getInputs()) {
            Block childBlock = input.getConnectedBlock();
            BlockView childBlockView = childBlock == null ? null : getView(childBlock);
            ViewGroup inputView = childBlockView == null ? null :
                    (ViewGroup)childBlockView.getParent();
            if (inputView != null) {
                inputView.removeView((View) childBlockView);
            }
        }

        List<InputView> inputViews = buildInputViews(model, connectionManager, touchHandler);
        BlockView newBlockView = buildBlockView(model, inputViews, connectionManager, touchHandler);

        if (parent != null) {
            View originalView = (View) original;
            int viewIndex = getIndexOfChild(parent, originalView);
            ViewGroup.LayoutParams lp = ((View) original).getLayoutParams();

            parent.removeView(originalView);
            parent.addView((View) newBlockView, viewIndex, lp);
        }

        mBlockIdToView.put(model.getId(), new WeakReference<>(newBlockView));
        return newBlockView;
    }

    /**
     * Build and populate the {@link BlockView} for {@code block}, using the provided
     * {@link InputView}s.
     * <p/>
     * This method should not recurse the model to generate more than one view.
     * {@link #buildBlockViewTree} will traverse the model and call this method for each
     * {@link Block}.
     *
     * @param block The {@link Block} to build a view for.
     * @param inputViews The list of {@link com.google.blockly.android.ui.InputView}s in this block.
     * @param connectionManager The {@link ConnectionManager} for the {@link Workspace}.
     * @param touchHandler The {@link BlockTouchHandler} this view should start with.
     * @return The new {@link com.google.blockly.android.ui.BlockView}.
     */
    protected abstract BlockView buildBlockView(Block block, List<InputView> inputViews,
                                                ConnectionManager connectionManager,
                                                BlockTouchHandler touchHandler);

    /**
     * Build and populate the {@link InputView} for {@code input}.
     * <p/>
     * This method should not recurse the model to generate more than one view.
     * {@link #buildBlockViewTree} will traverse the model and call this method for each
     * {@link Input}.
     *
     * @param input The {@link Input} to build a view for.
     * @param fieldViews The list of {@link FieldView}s in the constructed view.
     * @return The new {@link com.google.blockly.android.ui.InputView}.
     */
    protected abstract InputView buildInputView(Input input, List<FieldView> fieldViews);

    /**
     * Build and populate the {@link FieldView} for {@code field}.
     * <p>
     * Note: Variables need some extra setup when they are created by a custom
     * ViewFactory.
     * <ul>
     *     <li>If they use an adapter to display the list of variables it must be set.</li>
     *     <li>If they have delete/rename/create options they must have a
     *     {@link VariableRequestCallback} set on them. {@link #mVariableCallback} may be used for
     *     this purpose.</li>
     * </ul>
     *
     *
     * @param field The {@link Field} to build a view for.
     * @return The new {@link FieldView}.
     */
    protected FieldView buildFieldView(Field field) {
        @Field.FieldType int type = field.getType();
        switch (type) {
            case Field.TYPE_ANGLE: {
                BasicFieldAngleView fieldAngleView = new BasicFieldAngleView(mContext);
                fieldAngleView.setField(field);
                return fieldAngleView;
            }
            case Field.TYPE_CHECKBOX: {
                BasicFieldCheckboxView fieldCheckboxView = new BasicFieldCheckboxView(mContext);
                fieldCheckboxView.setField(field);
                return fieldCheckboxView;
            }
            case Field.TYPE_COLOR: {
                BasicFieldColorView fieldColorView = new BasicFieldColorView(mContext);
                fieldColorView.setField(field);
                return fieldColorView;
            }
            case Field.TYPE_DATE: {
                BasicFieldDateView fieldDateView = new BasicFieldDateView(mContext);
                fieldDateView.setField(field);
                return fieldDateView;
            }
            case Field.TYPE_DROPDOWN: {
                BasicFieldDropdownView fieldDropdownView = new BasicFieldDropdownView(mContext);
                fieldDropdownView.setField(field);
                return fieldDropdownView;
            }
            case Field.TYPE_IMAGE: {
                BasicFieldImageView fieldImageView = new BasicFieldImageView(mContext);
                fieldImageView.setField(field);
                return fieldImageView;
            }
            case Field.TYPE_INPUT: {
                BasicFieldInputView fieldInputView = new BasicFieldInputView(mContext);
                fieldInputView.setField(field);
                return fieldInputView;
            }
            case Field.TYPE_LABEL: {
                BasicFieldLabelView fieldLabelView = new BasicFieldLabelView(mContext);
                fieldLabelView.setField(field);
                return fieldLabelView;
            }
            case Field.TYPE_VARIABLE: {
                BasicFieldVariableView fieldVariableView = new BasicFieldVariableView(mContext);
                fieldVariableView.setAdapter(getVariableAdapter());
                fieldVariableView.setField(field);
                fieldVariableView.setVariableRequestCallback(mVariableCallback);
                return fieldVariableView;
            }
            case Field.TYPE_NUMBER: {
                BasicFieldNumberView fieldNumberView = new BasicFieldNumberView(mContext);
                fieldNumberView.setField(field);
                return fieldNumberView;
            }

            case Field.TYPE_UNKNOWN:
            default:
                throw new IllegalArgumentException("Unknown Field type: " + type);
        }
    }

    protected SpinnerAdapter getVariableAdapter() {
        if (mVariableNameManager == null) {
            throw new IllegalStateException("NameManager must be set before variable field is "
                    + "instantiated.");
        }
        if (mVariableAdapter == null) {
            mVariableAdapter = new BasicFieldVariableView.VariableViewAdapter(
                    mContext, mVariableNameManager, android.R.layout.simple_spinner_item);
        }
        return mVariableAdapter;
    }

    @NonNull
    protected List<InputView> buildInputViews(
            Block block, ConnectionManager connectionManager, BlockTouchHandler touchHandler) {
        List<Input> inputs = block.getInputs();
        final int inputCount = inputs.size();
        List<InputView> inputViews = new ArrayList<>(inputCount);
        for (int i = 0; i < inputCount; i++) {
            Input input = inputs.get(i);
            List<Field> fields = input.getFields();
            List<FieldView> fieldViews = new ArrayList<>(fields.size());
            for (int  j = 0; j < fields.size(); j++) {
                fieldViews.add(buildFieldView(fields.get(j)));
            }
            InputView inputView = buildInputView(input, fieldViews);

            if (input.getType() != Input.TYPE_DUMMY) {
                Block targetBlock = input.getConnection().getTargetBlock();
                if (targetBlock != null) {
                    // Blocks connected to inputs live in their own BlockGroups.
                    BlockGroup subgroup;
                    BlockView targetBlockView = getView(targetBlock);
                    if (targetBlockView == null) {
                        subgroup =
                                buildBlockGroupTree(targetBlock, connectionManager, touchHandler);
                    } else {
                        // BlockViews might already exist, especially in the case of children of
                        // mutated blocks.
                        ViewParent targetBlockViewParent = targetBlockView.getParent();
                        if (targetBlockViewParent == null) {
                            subgroup = buildBlockGroup();
                            subgroup.addView((View) targetBlockView);
                        } else if (targetBlockViewParent instanceof BlockGroup) {
                            if (targetBlockViewParent.getParent() != null) {
                                throw new IllegalStateException(
                                        "BlockView parent is already attached.");
                            }
                            if (((ViewGroup) targetBlockViewParent).getChildAt(0)
                                    != targetBlockView) {
                                throw new IllegalStateException(
                                        "Input BlockView is not first in parent BlockGroup.");
                            }
                            subgroup = (BlockGroup) targetBlockViewParent;
                        } else {
                            throw new IllegalStateException(
                                    "Unexpected BlockView parent is not a BlockGroup.");
                        }
                    }
                    inputView.setConnectedBlockGroup(subgroup);
                }
            }
            inputViews.add(inputView);
        }
        return inputViews;
    }

    /**
     * Removes the mapping to this view from its block.  This should only be called from
     * {@link BlockView#unlinkModel()}, which is already handled in {@link AbstractBlockView}.
     *
     *  @param blockView The BlockView to disassociate from its Block model.
     */
    // TODO(#137): Move to ViewPool class.
    protected final void unregisterView(BlockView blockView) {
        Block block = blockView.getBlock();
        mBlockIdToView.remove(block.getId());
    }

    /**
     * Constructs the drag and drop flags used by {@link ViewCompat#startDragAndDrop}.
     */
    public int getDragAndDropFlags() {
        int flags = 0;
        if (android.os.Build.VERSION.SDK_INT >= 24) {
            flags |= 0x00000100;  // View.DRAG_FLAG_GLOBAL
        }
        return flags;
    }

    /**
     * @return The index of the provided child view.
     */
    private static int getIndexOfChild(ViewGroup parent, View child) {
        int childCount = parent.getChildCount();
        for (int viewIndex = 0; viewIndex < childCount; ++viewIndex) {
            if (parent.getChildAt(viewIndex) == child) {
                return viewIndex;
            }
        }
        throw new IllegalStateException("Child View not found.");
    }

    /**
     * Handles a user toggling the UI for a mutator on a block. This is typically done through a
     * mutator button on a block, but other UIs may trigger it in other ways.
     */
    public interface MutatorToggleListener {
        void onMutatorToggled(Block block);
    }
}
