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
import android.view.View;

import com.google.blockly.android.ui.BlockGroup;
import com.google.blockly.android.ui.WorkspaceHelper;
import com.google.blockly.android.ui.WorkspaceView;

/**
 * This {@link BlockGroup} exposes methods not usually available, specifically for testing.
 */
public class TestableBlockGroup extends BlockGroup {
    /**
     * Creates a BlockGroup with methods to facilitate testing Blockly.
     */
    public TestableBlockGroup(Context context, WorkspaceHelper helper) {
        super(context, helper);
    }

    /**
     * This method sets the WorkspaceView that would normally occur during onAttachedToWindow().
     */
    public void setWorkspaceView(WorkspaceView workspaceView) {
        int count = getChildCount();
        for (int i = 0; i < count; ++i) {
            View child = getChildAt(i);
            ((TestableBlockView) child).setWorkspaceView(workspaceView);
        }
    }
}
