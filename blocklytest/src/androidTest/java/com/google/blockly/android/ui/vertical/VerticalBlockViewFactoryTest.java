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

package com.google.blockly.android.ui.vertical;

import android.support.annotation.NonNull;
import android.test.suitebuilder.annotation.SmallTest;

import com.google.blockly.android.MockitoAndroidTestCase;
import com.google.blockly.android.R;
import com.google.blockly.android.control.ConnectionManager;
import com.google.blockly.android.ui.BlockGroup;
import com.google.blockly.android.ui.WorkspaceHelper;
import com.google.blockly.model.Block;
import com.google.blockly.model.BlockFactory;

import org.mockito.Mock;


/**
 * Tests for {@link VerticalBlockViewFactory}.
 */
@SmallTest
public class VerticalBlockViewFactoryTest extends MockitoAndroidTestCase {

    private BlockFactory mBlockFactory;
    private VerticalBlockViewFactory mViewFactory;

    private BlockGroup mBlockGroup;

    @Mock
    private WorkspaceHelper mMockWorkspaceHelper;

    @Mock
    private ConnectionManager mMockConnectionManager;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        mBlockFactory = new BlockFactory(getContext(), new int[]{R.raw.test_blocks});
        mViewFactory = new VerticalBlockViewFactory(getContext(), mMockWorkspaceHelper);
        mBlockGroup = mViewFactory.buildBlockGroup();
    }

    // Verify construction of a BlockView for a Block with inputs.
    public void testBuildBlockViewWithInputs() {
        final Block block = mBlockFactory.obtainBlock(
                "test_block_one_input_each_type", "TestBlock");
        final BlockView blockView = makeBlockView(block);
        assertNotNull(block);

        assertSame(block, blockView.getBlock());

        // One InputView per Input?
        assertEquals(3, blockView.getInputViewCount());

        for (int inputIdx = 0; inputIdx < 3; ++inputIdx) {
            // Each InputView points to an Input?
            assertNotNull(blockView.getInputView(inputIdx).getInput());
            // Each InputView is a child of the BlockView?
            assertSame(blockView.getInputView(inputIdx), blockView.getChildAt(inputIdx));
            // Each input view points to the correct Input?
            assertSame(block.getInputs().get(inputIdx),
                    blockView.getInputView(inputIdx).getInput());
        }
    }

    // Make a BlockView for the given Block and default mock objects otherwise.
    @NonNull
    private BlockView makeBlockView(Block block) {
        return mViewFactory.buildBlockViewTree(block, mBlockGroup, mMockConnectionManager, null);
    }
}
