/*
 *  Copyright 2015 Google Inc. All Rights Reserved.
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

import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.google.blockly.control.BlocklyController;
import com.google.blockly.control.ConnectionManager;
import com.google.blockly.model.Block;
import com.google.blockly.model.WorkspacePoint;
import com.google.blockly.ui.BlockDrawerFragment;
import com.google.blockly.ui.BlockListView;
import com.google.blockly.ui.BlockTouchHandler;
import com.google.blockly.ui.BlockView;
import com.google.blockly.ui.WorkspaceHelper;

import java.util.List;

/**
 * Fragment for viewing the {@link Block} contents of the trash can. {@code TrashFragment} inheirts
 * the configurability of {@link BlockDrawerFragment}, via the {@code closeable} and
 * {@code scrollOrientation} attributes.
 * <p/>
 * For example:
 * <blockquote><pre>
 * &lt;fragment
 *     xmlns:android="http://schemas.android.com/apk/res/android"
 *     xmlns:blockly="http://schemas.android.com/apk/res-auto"
 *     android:name="com.google.blockly.TrashFragment"
 *     android:id="@+id/blockly_toolbox"
 *     android:layout_width="wrap_content"
 *     android:layout_height="match_parent"
 *     <b>blockly:closeable</b>="true"
 *     <b>blockly:scrollOrientation</b>="vertical"
 *     /&gt;
 * </pre></blockquote>
 * <p/>
 * When {@code blockly:closeable} is true, the drawer will hide (visibility {@link View#GONE}).  The
 * tabs will always be visible as long as either there are multiple tabs or the drawer is closeable.
 * This provides the user a way to swtich categories or open and close the drawers.
 * <p/>
 * {@code blockly:scrollOrientation} controls the block list, and can be either {@code horizontal}
 * or {@code vertical}.
 *
 * @attr ref com.google.blockly.R.styleable#BlockDrawerFragment_closeable
 * @attr ref com.google.blockly.R.styleable#BlockDrawerFragment_scrollOrientation
 */
public class TrashFragment extends BlockDrawerFragment {
    private static final boolean DEFAULT_CLOSEABLE = false;

    private BlocklyController mController;
    private WorkspaceHelper mHelper;
    private BlockListView mBlockListView;

    protected final Point mTempScreenPosition = new Point();
    protected final WorkspacePoint mTempWorkspacePosition = new WorkspacePoint();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // Read configure
        readArgumentsFromBundle(getArguments());
        readArgumentsFromBundle(savedInstanceState);  // Overwrite initial state with stored state.

        mBlockListView = new BlockListView(getContext());
        mBlockListView.setLayoutManager(createLinearLayoutManager());
        mBlockListView.setBackgroundColor(
                getResources().getColor(R.color.blockly_trash_bg, null));  // Replace with attribute

        maybeUpdateTouchHandler();

        return mBlockListView;
    }

    /**
     * Connects the {@link TrashFragment} to the application's {@link BlocklyController}.  It is
     * called by {@link BlocklyController#setTrashFragment(TrashFragment)} and should not be called
     * by the application developer.
     *
     * @param controller The application's {@link BlocklyController}.
     */
    public void setController(BlocklyController controller) {
        if (mController != null && mController.getTrashFragment() != this) {
            throw new IllegalStateException("Call BlockController.setTrashFragment(..) instead of"
                    + " TrashFragment.setController(..).");
        }

        mController = controller;
        if (controller != null) {
            mHelper = controller.getWorkspaceHelper();
        }

        maybeUpdateTouchHandler();
    }

    /**
     * Immediately executes a transaction that will open or close the {@code TrashFragment}.
     *
     * @param open Opens the trash if {@code true} and trash is closed. Closes it if {@code false}
     *             and previously opened.
     * @return Whether there was be a state change.
     */
    public boolean setOpened(boolean open) {
        FragmentTransaction transaction =
                getActivity().getSupportFragmentManager().beginTransaction();
        boolean result = setOpened(open, transaction);
        transaction.commit();  // Posisble no-op if result is false.
        return result;
    }

    /**
     * Appends to {@code transaction} the necessary step (if any) to show or hide the
     * {@code TrashFragment}.
     *
     * @param open Opens the trash if {@code true} and trash is closed. Closes it if {@code false}
     *             and previously opened.
     * @param transaction {@link FragmentTransaction} in which the action will occur.
     * @return Whether there will be a state change.
     */
    // TODO(#384): Add animation hooks for subclasses.
    public boolean setOpened(boolean open, FragmentTransaction transaction) {
        if (!mCloseable && !open) {
            throw new IllegalStateException("Not configured as closeable.");
        }
        if (open) {
            if (isHidden()) {
                transaction.show(this);
                return true;
            }
        } else {
            if (!isHidden()) {
                transaction.hide(this);
                return true;
            }
        }
        return false;
    }

    public void setContents(List<Block> blocks) {
        mBlockListView.setContents(blocks);
    }

    public void onBlockTrashed(Block block) {
        mBlockListView.addBlock(0, block);
    }

    private void maybeUpdateTouchHandler() {
        if (mBlockListView != null && mController != null) {
            ConnectionManager connectionMan = mController.getWorkspace().getConnectionManager();
            mBlockListView.init(mHelper, new BlockTouchHandler() {
                @Override
                public boolean onTouchBlock(BlockView blockView, MotionEvent motionEvent) {
                    if (motionEvent.getAction() != MotionEvent.ACTION_DOWN) {
                        return false;
                    }

                    Block rootBlock = blockView.getBlock().getRootBlock();
                    // TODO(#376): Optimize to avoid copying the model and view trees.
                    Block copiedModel = rootBlock.deepCopy();

                    // Make the pointer be in the same relative position on the block as it was in
                    // the toolbox.
                    mTempScreenPosition.set((int) motionEvent.getRawX() - (int) motionEvent.getX(),
                            (int) motionEvent.getRawY() - (int) motionEvent.getY());
                    mHelper.screenToWorkspaceCoordinates(
                            mTempScreenPosition, mTempWorkspacePosition);
                    copiedModel.setPosition(mTempWorkspacePosition.x, mTempWorkspacePosition.y);
                    mController.addBlockFromToolbox(copiedModel, motionEvent);
                    mBlockListView.removeBlock(rootBlock);

                    if (mCloseable) {
                        setOpened(false);
                    }
                    return true;
                }

                @Override
                public boolean onInterceptTouchEvent(BlockView blockView, MotionEvent motionEvent) {
                    return false;
                }
            });
        }
    }
}
