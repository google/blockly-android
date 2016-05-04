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

package com.google.blockly.android.testui;

import android.content.Context;
import android.support.annotation.Nullable;

import com.google.blockly.android.control.ConnectionManager;
import com.google.blockly.android.ui.BlockGroup;
import com.google.blockly.android.ui.BlockTouchHandler;
import com.google.blockly.android.ui.WorkspaceHelper;
import com.google.blockly.android.ui.WorkspaceView;
import com.google.blockly.android.ui.vertical.BlockView;
import com.google.blockly.android.ui.vertical.InputView;
import com.google.blockly.android.ui.vertical.VerticalBlockViewFactory;
import com.google.blockly.model.Block;

import java.util.List;

/**
 * Created by marshalla on 5/3/16.
 */
public class TestableBlockView extends BlockView {
    /**
     * Create a testable version of the vertical {@link BlockView}.
     */
    TestableBlockView(Context context, WorkspaceHelper helper, VerticalBlockViewFactory factory,
                      Block block, List<InputView> inputViews, ConnectionManager connectionManager,
                      @Nullable BlockTouchHandler touchHandler) {
        super(context, helper, factory, block, inputViews, connectionManager, touchHandler);
    }

    /**
     * This method sets the WorkspaceView that would normally occur during onAttachedToWindow().
     * It also recurses on all inputs.
     */
    public void setWorkspaceView(WorkspaceView workspaceView) {
        this.mWorkspaceView = workspaceView;

        int count = getInputViewCount();
        for (int i = 0; i < count; ++i) {
            InputView input = getInputView(i);
            BlockGroup bg = input.getConnectedBlockGroup();
            if (bg != null) {
                ((TestableBlockGroup) bg).setWorkspaceView(workspaceView);
            }
        }
    }
}
