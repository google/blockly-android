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

import com.google.blockly.android.control.ConnectionManager;
import com.google.blockly.android.ui.BlockGroup;
import com.google.blockly.android.ui.BlockTouchHandler;
import com.google.blockly.android.ui.WorkspaceHelper;
import com.google.blockly.android.ui.vertical.BlockView;
import com.google.blockly.android.ui.vertical.InputView;
import com.google.blockly.android.ui.vertical.VerticalBlockViewFactory;
import com.google.blockly.model.Block;

import java.util.List;

/**
 * This factory builds "Testable" Block UI components.
 */
public class TestableBlockViewFactory extends VerticalBlockViewFactory {
    public TestableBlockViewFactory(Context context, WorkspaceHelper helper) {
        super(context, helper);
    }

    @Override
    public BlockGroup buildBlockGroup() {
        return new TestableBlockGroup(mContext, mHelper);
    }

    @Override
    protected BlockView buildBlockView(Block block, List<InputView> inputViews,
                                       ConnectionManager connectionManager,
                                       BlockTouchHandler touchHandler) {
        return new TestableBlockView(mContext, mHelper, this, block, inputViews,
                                     connectionManager, touchHandler);
    }
}
