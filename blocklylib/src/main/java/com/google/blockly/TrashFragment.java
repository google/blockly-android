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

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.google.blockly.model.Block;
import com.google.blockly.ui.BlockGroup;
import com.google.blockly.ui.BlockView;
import com.google.blockly.ui.WorkspaceHelper;

/**
 * Fragment for viewing the contents of the trash can.
 */
public class TrashFragment extends ToolboxFragment {
    private static final String TAG = "TrashFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWorkspaceHelper.setBlockTouchHandler(new WorkspaceHelper.BlockTouchHandler() {
            @Override
            public boolean onTouchBlock(BlockView blockView, MotionEvent motionEvent) {
                if (motionEvent.getAction() != MotionEvent.ACTION_DOWN) {
                    return false;
                }
                // TODO(fenichel): make the trash can close when blocks are selected.

                BlockGroup bg = mWorkspaceHelper.getRootBlockGroup(blockView.getBlock());
                Block copiedModel = ((BlockView) bg.getChildAt(0)).getBlock().deepCopy();

                mTempScreenPosition.set((int) motionEvent.getRawX(), (int) motionEvent.getRawY());
                mWorkspace.getWorkspaceHelper().screenToWorkspaceCoordinates(
                        mTempScreenPosition, mTempWorkspacePosition);
                copiedModel.setPosition(mTempWorkspacePosition.x, mTempWorkspacePosition.y);
                mWorkspace.addBlockFromToolbox(copiedModel, motionEvent);
                return true;
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        ((LinearLayoutManager) (mRecyclerView.getLayoutManager()))
                .setOrientation(LinearLayoutManager.HORIZONTAL);
        return mRecyclerView;
    }
}
