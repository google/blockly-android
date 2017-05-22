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

import com.google.blockly.android.BlocklyTestCase;
import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.android.control.ConnectionManager;
import com.google.blockly.android.ui.BlockGroup;
import com.google.blockly.android.ui.WorkspaceHelper;
import com.google.blockly.model.Block;
import com.google.blockly.model.BlockFactory;
import com.google.blockly.model.BlockTemplate;
import com.google.blockly.utils.BlockLoadingException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link VerticalBlockViewFactory}.
 */
public class VerticalBlockViewFactoryTest extends BlocklyTestCase {

    private BlockFactory mBlockFactory;
    private VerticalBlockViewFactory mViewFactory;
    private BlockGroup mBlockGroup;

    private BlocklyController mMockController;
    private WorkspaceHelper mMockWorkspaceHelper;
    private ConnectionManager mMockConnectionManager;

    @Before
     public void setUp() throws Exception {
        mMockController = Mockito.mock(BlocklyController.class);
        mMockWorkspaceHelper = mock(WorkspaceHelper.class);
        mMockConnectionManager = mock(ConnectionManager.class);
        mBlockFactory = new BlockFactory();
        mBlockFactory.addJsonDefinitions(getContext().getAssets()
                .open("default/test_blocks.json"));
        mBlockFactory.setController(mMockController);
        mViewFactory = new VerticalBlockViewFactory(getContext(), mMockWorkspaceHelper);
        mBlockGroup = mViewFactory.buildBlockGroup();
    }

    // Verify construction of a BlockView for a Block with inputs.
    @Test
    public void testBuildBlockViewWithInputs() throws BlockLoadingException {
        final Block block = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("test_block_one_input_each_type"));
        final BlockView blockView = makeBlockView(block);
        assertThat(block).isNotNull();

        assertThat(block).isSameAs(blockView.getBlock());

        // One InputView per Input?
        assertThat(blockView.getInputViewCount()).isEqualTo(3);

        for (int inputIdx = 0; inputIdx < 3; ++inputIdx) {
            // Each InputView points to an Input?
            assertThat(blockView.getInputView(inputIdx).getInput()).isNotNull();
            // Each InputView is a child of the BlockView?
            assertThat(blockView.getInputView(inputIdx)).isSameAs(blockView.getChildAt(inputIdx));
            // Each input view points to the correct Input?
            assertThat(block.getInputs().get(inputIdx))
                    .isSameAs(blockView.getInputView(inputIdx).getInput());
        }
    }

    // Make a BlockView for the given Block and default mock objects otherwise.
    @NonNull
    private BlockView makeBlockView(Block block) {
        return mViewFactory.buildBlockViewTree(block, mBlockGroup, mMockConnectionManager, null);
    }
}
