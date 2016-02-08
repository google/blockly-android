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
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.google.blockly.control.BlocklyController;
import com.google.blockly.control.ConnectionManager;
import com.google.blockly.model.Block;
import com.google.blockly.model.WorkspacePoint;
import com.google.blockly.ui.BlockListView;
import com.google.blockly.ui.BlockTouchHandler;
import com.google.blockly.ui.BlockView;
import com.google.blockly.ui.WorkspaceHelper;

import java.util.List;

/**
 * Fragment for viewing the contents of the trash can.
 */
public class TrashFragment extends Fragment {
    private static final String TAG = "TrashFragment";

    private BlocklyController mController;
    private WorkspaceHelper mHelper;
    private BlockListView mBlockListView;

    private boolean mAutohideEnabled = false;

    protected final Point mTempScreenPosition = new Point();
    protected final WorkspacePoint mTempWorkspacePosition = new WorkspacePoint();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        mBlockListView = (BlockListView) inflater.inflate(R.layout.fragment_trash, null);
        LinearLayoutManager layout = new LinearLayoutManager(getContext());
        layout.setOrientation(LinearLayoutManager.VERTICAL);
        mBlockListView.setLayoutManager(layout);

        maybeUpdateTouchHandler();
        return mBlockListView;
    }

    public void setController(BlocklyController controller) {
        mController = controller;
        mHelper = controller.getWorkspaceHelper();

        maybeUpdateTouchHandler();
    }

    public void setAutoHideEnabled(boolean autoHideEnabled) {
        mAutohideEnabled = autoHideEnabled;
    }

    public void setContents(List<Block> blocks) {
        mBlockListView.setContents(blocks);
    }

    public void show() {
        getActivity().getSupportFragmentManager().beginTransaction().show(this).commit();
    }

    public void hide() {
        getActivity().getSupportFragmentManager().beginTransaction().hide(this).commit();
    }

    public void onBlockTrashed(Block block) {
        mBlockListView.addBlock(0, block);
    }

    private void maybeUpdateTouchHandler() {
        if (mBlockListView != null && mController != null) {
            ConnectionManager connectionMan = mController.getWorkspace().getConnectionManager();
            mBlockListView.init(mHelper, connectionMan, new BlockTouchHandler() {
                @Override
                public boolean onTouchBlock(BlockView blockView, MotionEvent motionEvent) {
                    if (motionEvent.getAction() != MotionEvent.ACTION_DOWN) {
                        return false;
                    }

                    Block rootBlock = blockView.getBlock().getRootBlock();
                    // TODO(#376): Optimize to avoid copying the model and view trees.
                    Block copiedModel = rootBlock.deepCopy();

                    // Make the pointer be in the same relative position on the block as it was in the
                    // toolbox.
                    mTempScreenPosition.set((int) motionEvent.getRawX() - (int) motionEvent.getX(),
                            (int) motionEvent.getRawY() - (int) motionEvent.getY());
                    mHelper.screenToWorkspaceCoordinates(
                            mTempScreenPosition, mTempWorkspacePosition);
                    copiedModel.setPosition(mTempWorkspacePosition.x, mTempWorkspacePosition.y);
                    mController.addBlockFromToolbox(copiedModel, motionEvent);
                    mBlockListView.removeBlock(rootBlock);

                    if (mAutohideEnabled) {
                        hide();
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
