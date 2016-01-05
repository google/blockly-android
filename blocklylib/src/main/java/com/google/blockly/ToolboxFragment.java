/*
 *  Copyright  2015 Google Inc. All Rights Reserved.
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
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.google.blockly.model.Block;
import com.google.blockly.model.ToolboxCategory;
import com.google.blockly.model.Workspace;
import com.google.blockly.model.WorkspacePoint;
import com.google.blockly.ui.BlockGroup;
import com.google.blockly.ui.BlockView;
import com.google.blockly.ui.ToolboxAdapter;
import com.google.blockly.ui.WorkspaceHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment to hold views of all of the available blocks in the toolbox.
 * No name will be shown for the top-level category, but names for all subcategories will be shown.
 */
public class ToolboxFragment extends Fragment {
    protected static final String TAG = "ToolboxFragment";
    protected final Point mTempScreenPosition = new Point();
    protected final WorkspacePoint mTempWorkspacePosition = new WorkspacePoint();

    protected RecyclerView mRecyclerView;
    protected RecyclerView.Adapter mAdapter;
    protected Workspace mWorkspace;
    protected WorkspaceHelper mWorkspaceHelper;
    protected WorkspaceHelper.BlockTouchHandler mBlockTouchHandler;

    // TODO (fenichel): Load from resources
    // Minimum pixel distance between blocks in the toolbox.
    private int mBlockMargin = 10;
    private int CARPET_SIZE = 1000;
    private ToolboxCategory mTopLevelCategory;

    public void setWorkspace(Workspace workspace) {
        mWorkspace = workspace;
        mWorkspaceHelper = mWorkspace.getWorkspaceHelper();
        mBlockTouchHandler = new WorkspaceHelper.BlockTouchHandler() {
            @Override
            public boolean onTouchBlock(BlockView blockView, MotionEvent motionEvent) {
                if (motionEvent.getAction() != MotionEvent.ACTION_DOWN) {
                    return false;
                }

                BlockGroup bg = mWorkspaceHelper.getRootBlockGroup(blockView.getBlock());
                Block copiedModel = ((BlockView) bg.getChildAt(0)).getBlock().deepCopy();

                // Make the pointer be in the same relative position on the block as it was in the
                // toolbox.
                mTempScreenPosition.set((int) motionEvent.getRawX() - (int) motionEvent.getX(),
                        (int) motionEvent.getRawY() - (int) motionEvent.getY());
                mWorkspaceHelper.screenToWorkspaceCoordinates(
                        mTempScreenPosition, mTempWorkspacePosition);
                copiedModel.setPosition(mTempWorkspacePosition.x, mTempWorkspacePosition.y);
                mWorkspace.addBlockFromToolbox(copiedModel, motionEvent, ToolboxFragment.this);
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
        // TODO(rachel-fenichel): fix lifecycle such that setContents() is never called before
        // onCreateView().
        if (mRecyclerView != null) {
            mRecyclerView.setAdapter(mAdapter);
        }
    }

    /**
     * Drop one instance of each block in the toolbox, all in the same place.
     */
    public void airstrike() {
        List<Block> blocks = new ArrayList<>();
        mTopLevelCategory.getAllBlocksRecursive(blocks);
        for (int i = 0; i < blocks.size(); i++) {
            Block copiedModel = blocks.get(i).deepCopy();
            copiedModel.setPosition(0, 0);
            mWorkspace.addBlockWithView(copiedModel);
        }
    }

    /**
     * Drop one instance of each block in the toolbox, randomly placed across a section of the
     * workspace.
     */
    public void carpetBomb() {
        List<Block> blocks = new ArrayList<>();
        mTopLevelCategory.getAllBlocksRecursive(blocks);
        for (int i = 0; i < blocks.size(); i++) {
            Block copiedModel = blocks.get(i).deepCopy();
            copiedModel.setPosition((int) (Math.random() * CARPET_SIZE) - CARPET_SIZE / 2,
                    (int) (Math.random() * CARPET_SIZE) - CARPET_SIZE / 2);
            mWorkspace.addBlockWithView(copiedModel);
        }
    }

    public RecyclerView.Adapter getAdapter() {
        return mAdapter;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRecyclerView = (RecyclerView) inflater.inflate(
                R.layout.fragment_toolbox, container, false);
        // use a linear layout manager
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.addItemDecoration(new ItemDecoration());
        return mRecyclerView;
    }

    private class ItemDecoration extends RecyclerView.ItemDecoration {
        @Override
        public void getItemOffsets(
                Rect outRect, View child, RecyclerView parent, RecyclerView.State state) {
            int itemPosition = parent.getChildPosition(child);
            int bottomMargin = (itemPosition == (mAdapter.getItemCount() - 1)) ? mBlockMargin : 0;
            outRect.set(mBlockMargin, mBlockMargin, mBlockMargin, bottomMargin);
        }
    }
}
