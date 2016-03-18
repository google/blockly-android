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

package com.google.blockly.android.ui;

import com.google.blockly.android.MockitoAndroidTestCase;
import com.google.blockly.android.R;
import com.google.blockly.android.control.ConnectionManager;
import com.google.blockly.model.BlockFactory;

import org.mockito.Mock;

/**
 * Tests for {@link BlockGroup}.
 */
public class BlockGroupTest extends MockitoAndroidTestCase {
    @Mock
    ConnectionManager mConnectionManager;
    @Mock
    private WorkspaceView mWorkspaceView;

    private BlockFactory mBlockFactory;
    private WorkspaceHelper mWorkspaceHelper;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mWorkspaceHelper = new WorkspaceHelper(getContext());
        mBlockFactory = new BlockFactory(getContext(), new int[]{R.raw.test_blocks});
    }
}
