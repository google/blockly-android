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
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.google.blockly.model.Block;
import com.google.blockly.model.Workspace;
import com.google.blockly.model.WorkspacePoint;
import com.google.blockly.ui.BlockGroup;
import com.google.blockly.ui.BlockGroupAdapter;
import com.google.blockly.ui.BlockView;
import com.google.blockly.ui.WorkspaceHelper;

import java.util.List;

/**
 * Fragment to hold views of all of the available blocks in the toolbox.
 */
public class ToolboxFragment extends Fragment {
    private static final String TAG = "ToolboxFragment";
    protected final Point mTempScreenPosition = new Point();
    protected final WorkspacePoint mTempWorkspacePosition = new WorkspacePoint();
    protected RecyclerView mRecyclerView;
    protected RecyclerView.Adapter mAdapter;
    protected Workspace mWorkspace;
    protected WorkspaceHelper mWorkspaceHelper;
    protected List<Block> mContents;
    protected DrawerLayout mDrawerLayout;
    // TODO (fenichel): Load from resources
    // Minimum pixel distance between blocks in the toolbox.
    protected int mBlockMargin = 10;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWorkspaceHelper = new WorkspaceHelper(getContext(), null);
        mWorkspaceHelper.setBlockTouchHandler(new WorkspaceHelper.BlockTouchHandler() {
            @Override
            public boolean onTouchBlock(BlockView blockView, MotionEvent motionEvent) {
                if (motionEvent.getAction() != MotionEvent.ACTION_DOWN) {
                    return false;
                }
                mDrawerLayout.closeDrawers();

                BlockGroup bg = mWorkspaceHelper.getRootBlockGroup(blockView.getBlock());
                int pos = ((RecyclerView) bg.getParent()).getChildAdapterPosition(bg);
                Block copiedModel = mContents.get(pos).deepCopy();

                // Make the pointer be in the same relative position on the block as it was in the
                // toolbox.
                mTempScreenPosition.set((int) motionEvent.getRawX() - (int) motionEvent.getX(),
                        (int) motionEvent.getRawY() - (int) motionEvent.getY());
                mWorkspace.getWorkspaceHelper().screenToWorkspaceCoordinates(
                        mTempScreenPosition, mTempWorkspacePosition);
                copiedModel.setPosition(mTempWorkspacePosition.x, mTempWorkspacePosition.y);
                mWorkspace.addBlockFromToolbox(copiedModel, getContext(), motionEvent);
                return true;
            }
        });
    }

    public void setWorkspace(Workspace workspace) {
        mWorkspace = workspace;
    }

    public void setContents(List<Block> contents) {
        mContents = contents;
        mAdapter = new BlockGroupAdapter(mContents, mWorkspaceHelper, getContext());
        mRecyclerView.setAdapter(mAdapter);
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

    public void setDrawerLayout(DrawerLayout drawerLayout) {
        mDrawerLayout = drawerLayout;
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
