/*
 *  Copyright 2016 Google Inc. All Rights Reserved.
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

import android.app.Activity;
import android.os.Bundle;

import com.google.blockly.control.BlocklyController;
import com.google.blockly.model.Workspace;
import com.google.blockly.ui.WorkspaceHelper;
import com.google.blockly.ui.WorkspaceView;

/**
 * Minimal Activity hosting a Workspace, for testing Blockly Views.
 */
public class TestWorkspaceViewActivity extends Activity {
    public BlocklyController mController;
    public WorkspaceHelper mWorkspaceHelper;
    public Workspace mWorkspace;
    public WorkspaceView mWorkspaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mController = new BlocklyController.Builder(this).build();
        mWorkspace = mController.getWorkspace();
        mWorkspaceHelper = mController.getWorkspaceHelper();

        mWorkspaceView = new WorkspaceView(this);
        mController.initWorkspaceView(mWorkspaceView);

        setContentView(mWorkspaceView);
    }
}
