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

package com.google.blockly.android.ui;

import android.support.annotation.NonNull;
import android.test.suitebuilder.annotation.SmallTest;
import android.view.MotionEvent;

import com.google.blockly.android.MockitoAndroidTestCase;
import com.google.blockly.android.R;
import com.google.blockly.android.control.ConnectionManager;
import com.google.blockly.model.Block;
import com.google.blockly.model.BlockFactory;

import org.mockito.Mock;

import java.util.ArrayList;


/**
 * Tests for {@link AbstractBlockView}.
 */
@SmallTest
public class AbstractBlockViewTest extends MockitoAndroidTestCase {

    private BlockFactory mBlockFactory;
    private Block mEmptyBlock;

    @Mock
    private ConnectionManager mMockConnectionManager;

    @Mock
    private WorkspaceView mMockWorkspaceView;


    @Mock
    private WorkspaceHelper mMockHelper;

    @Mock
    private BlockViewFactory mMockViewFactory;

    @Mock
    private BlockGroup mMockBlockGroup;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        mBlockFactory = new BlockFactory(getContext(), new int[]{R.raw.test_blocks});
        mEmptyBlock = mBlockFactory.obtainBlock("empty_block", "fake_id");
    }

    // Verify correct object state after construction.
    public void testConstructor() {
        final BlockView blockView = makeBlockView(mEmptyBlock);

        // Verify Block and BlockView are linked both ways.
        assertSame(mEmptyBlock, blockView.getBlock());
    }

    // Make a BlockView for the given Block and default mock objects otherwise.
    @NonNull
    private AbstractBlockView makeBlockView(Block block) {
        return new AbstractBlockView(getContext(), mMockHelper, null, block, new ArrayList(),
                mMockConnectionManager, null) {

            @Override
            protected boolean hitTest(MotionEvent event) {
                return false;
            }

            @Override
            public int getNextBlockVerticalOffset() {
                return 0;
            }

            @Override
            public int getOutputConnectorMargin() {
                return 0;
            }

            @Override
            protected void onLayout(boolean changed, int l, int t, int r, int b) {
                // Fake. Do nothing.
            }
        };
    }
}
