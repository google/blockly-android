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
import android.support.test.InstrumentationRegistry;
import android.test.suitebuilder.annotation.SmallTest;
import android.view.MotionEvent;

import com.google.blockly.android.control.ConnectionManager;
import com.google.blockly.android.test.R;
import com.google.blockly.model.Block;
import com.google.blockly.model.BlockFactory;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AbstractBlockView}.
 */
@SmallTest
public class AbstractBlockViewTest {

    private Block mEmptyBlock;

    private ConnectionManager mMockConnectionManager;
    private WorkspaceHelper mMockHelper;
    private BlockViewFactory mMockViewFactory;

    @Before
     public void setUp() throws Exception {

        mMockConnectionManager = mock(ConnectionManager.class);
        mMockHelper = mock(WorkspaceHelper.class);
        mMockViewFactory = mock(BlockViewFactory.class);

        BlockFactory mBlockFactory = new BlockFactory(InstrumentationRegistry.getContext(), new int[]{R.raw.test_blocks});
        mEmptyBlock = mBlockFactory.obtainBlock("empty_block", "fake_id");
    }

    // Verify correct object state after construction.
    @Test
    public void testConstructor() {
        final BlockView blockView = makeBlockView(mEmptyBlock);

        // Verify Block and BlockView are linked both ways.
        assertThat(mEmptyBlock).isSameAs(blockView.getBlock());
    }

    // Make a BlockView for the given Block and default mock objects otherwise.
    @NonNull
    private AbstractBlockView makeBlockView(Block block) {
        return new AbstractBlockView<InputView>(InstrumentationRegistry.getContext(), mMockHelper, mMockViewFactory, block,
                new ArrayList<InputView>(), mMockConnectionManager, null) {

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
