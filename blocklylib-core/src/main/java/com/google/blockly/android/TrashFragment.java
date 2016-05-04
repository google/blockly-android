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

package com.google.blockly.android;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.android.ui.BlockDrawerFragment;
import com.google.blockly.android.ui.BlockGroup;
import com.google.blockly.android.ui.BlockListView;
import com.google.blockly.model.Block;
import com.google.blockly.model.WorkspacePoint;

import java.util.List;

/**
 * Fragment for viewing the {@link Block} contents of the trash can. {@code TrashFragment} inherits
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
 * When {@code blockly:closeable} is true, the drawer will hide (visibility {@link View#GONE}) hide
 * until clicked to open. When false, the block list will always fill the fragment container. The
 * tabs will always be visible as long as either there are multiple tabs or the drawer is closeable.
 * This provides the user a way to switch categories or open and close the drawers.
 * <p/>
 * {@code blockly:scrollOrientation} controls the block list, and can be either {@code horizontal}
 * or {@code vertical}.
 *
 * @attr ref com.google.blockly.R.styleable#BlockDrawerFragment_closeable
 * @attr ref com.google.blockly.R.styleable#BlockDrawerFragment_scrollOrientation
 */
public class TrashFragment extends BlockDrawerFragment {
    private BlocklyController mController;

    private BlockListView.OnDragListBlock mListDragHandler = new BlockListView.OnDragListBlock() {
        @Override
        public BlockGroup getDraggableBlockGroup(int index, Block blockInList,
                                                 WorkspacePoint initialBlockPosition) {
            blockInList.setPosition(initialBlockPosition.x, initialBlockPosition.y);
            BlockGroup workspaceGroup = mController.addBlockFromTrash(blockInList);
            if (mCloseable) {
                setOpened(false);
            }
            return workspaceGroup;
        }
    };

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
                getResources().getColor(R.color.blockly_trash_bg));  // Replace with attribute

        maybeInitListView();

        return mBlockListView;
    }

    /**
     * Connects the {@link TrashFragment} to the application's {@link BlocklyController}. It is
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
        maybeInitListView();
    }

    private void maybeInitListView() {
        if (mBlockListView != null && mController != null) {
            mBlockListView.init(mController, mListDragHandler);
        }
    }

    /**
     * @return True if the trash is open (i.e., not closeable or not hidden).
     */
    public boolean isOpened() {
        return !isCloseable() || !isHidden();
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
    // TODO(#80): Add animation hooks for subclasses.
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

    /**
     * Called by the {@link BlocklyController}, setting the list of blocks in the trash.
     *
     * @param blocks The trashed blocks.
     */
    public void setContents(List<Block> blocks) {
        mBlockListView.setContents(blocks);
    }

    /**
     * Called by the {@link BlocklyController}, notifying when a block was added to the trash.
     *
     * @param block The block to added to the trash view.
     */
    public void onBlockTrashed(Block block) {
        mBlockListView.addBlock(0, block);
    }

    /**
     * Called by the {@link BlocklyController}, notifying when a block was removed from the trash.
     *
     * @param block The block to remove from the trash view.
     */
    public void onBlockRemovedFromTrash(Block block) {
        mBlockListView.removeBlock(block);
    }
}
