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

package com.google.blockly.ui;

import android.content.Context;

import com.google.blockly.control.ConnectionManager;
import com.google.blockly.model.Block;
import com.google.blockly.model.Field;
import com.google.blockly.model.NameManager;
import com.google.blockly.ui.fieldview.FieldView;

/**
 *
 */
public abstract class BlockViewFactory {
    protected Context mContext;
    protected WorkspaceHelper mHelper;

    protected BlockViewFactory(Context context, WorkspaceHelper helper) {
        mContext = context;
        mHelper = helper;

        helper.setBlockViewFactory(this);
    }

    public WorkspaceHelper getWorkspaceHelper() {
        return mHelper;
    }

    public abstract void setVariableNameManager(NameManager variableNameManager);

    public abstract BlockGroup buildBlockGroupTree(Block rootBlock,
                                          ConnectionManager connectionManager,
                                          BlockTouchHandler touchHandler);

    public abstract BlockView buildBlockViewTree(Block block, BlockGroup parentGroup,
                                        ConnectionManager connectionManager,
                                        BlockTouchHandler touchHandler);

    public abstract FieldView buildFieldView(Field field);

    public abstract BlockView getView(Block block);
}
