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
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.google.blockly.control.BlocklyController;
import com.google.blockly.model.Block;
import com.google.blockly.model.ToolboxCategory;
import com.google.blockly.model.WorkspacePoint;
import com.google.blockly.ui.BlockGroup;
import com.google.blockly.ui.ItemSpacingDecoration;
import com.google.blockly.ui.BlockTouchHandler;
import com.google.blockly.ui.BlockView;
import com.google.blockly.ui.ToolboxAdapter;
import com.google.blockly.ui.WorkspaceHelper;

/**
 * Fragment to hold views of all of the available blocks in the toolbox. No name will be shown for
 * the top-level category, but names for all subcategories will be shown.
 */
public class ToolboxFragment extends Fragment {
    protected static final String TAG = "ToolboxFragment";
    protected final Point mTempScreenPosition = new Point();
    protected final WorkspacePoint mTempWorkspacePosition = new WorkspacePoint();

    protected RecyclerView mRecyclerView;
    protected RecyclerView.Adapter mAdapter;

    protected BlocklyController mController;
    protected WorkspaceHelper mWorkspaceHelper;
    protected BlockTouchHandler mBlockTouchHandler;

    private ToolboxCategory mTopLevelCategory;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mRecyclerView = (RecyclerView) inflater.inflate(
                R.layout.fragment_toolbox, container, false);
        // use a linear layout manager
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mLayoutManager);
        return mRecyclerView;
    }

    public void setController(BlocklyController controller) {
        mController = controller;
        if (mController == null) {
            mWorkspaceHelper = null;
            mBlockTouchHandler = null;
            return;
        }

        mWorkspaceHelper = mController.getWorkspaceHelper();
        mBlockTouchHandler = new BlockTouchHandler() {
            @Override
            public boolean onTouchBlock(BlockView blockView, MotionEvent motionEvent) {
                if (motionEvent.getAction() != MotionEvent.ACTION_DOWN) {
                    return false;
                }

                Block copiedModel = blockView.getBlock().getRootBlock().deepCopy();

                // Make the pointer be in the same relative position on the block as it was in the
                // toolbox.
                mTempScreenPosition.set((int) motionEvent.getRawX() - (int) motionEvent.getX(),
                        (int) motionEvent.getRawY() - (int) motionEvent.getY());
                mWorkspaceHelper.screenToWorkspaceCoordinates(
                        mTempScreenPosition, mTempWorkspacePosition);
                copiedModel.setPosition(mTempWorkspacePosition.x, mTempWorkspacePosition.y);
                mController.addBlockFromToolbox(copiedModel, motionEvent);

                mController.maybeCloseToolboxFragment(ToolboxFragment.this);
                return true;
            }

            @Override
            public boolean onInterceptTouchEvent(BlockView blockView, MotionEvent motionEvent) {
                return false;
            }
        };
    }

    /**
     * Sets the contents that should be displayed in the toolbox.
     *
     * @param category The top-level category in the toolbox.
     */
    public void setContents(ToolboxCategory category) {
        mTopLevelCategory = category;
        mAdapter = new ToolboxAdapter(category, mWorkspaceHelper, mBlockTouchHandler, getContext());
        mAdapter.setHasStableIds(true);

        // Allow setContents(..) to be called before onCreateView(..).
        if (mRecyclerView != null) {
            mRecyclerView.setAdapter(mAdapter);
            mRecyclerView.addItemDecoration(new ItemSpacingDecoration(mAdapter));
        }
    }

    /**
     * @return The contents displayed in the toolbox.
     */
    public ToolboxCategory getContents() {
        return mTopLevelCategory;
    }

    public RecyclerView.Adapter getAdapter() {
        return mAdapter;
    }

}
