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

import com.google.blockly.android.BlocklyTestCase;
import com.google.blockly.android.TestUtils;
import com.google.blockly.android.control.ConnectionManager;
import com.google.blockly.android.test.R;
import com.google.blockly.android.ui.vertical.VerticalBlockViewFactory;
import com.google.blockly.model.Block;
import com.google.blockly.model.BlockFactory;
import com.google.blockly.utils.BlockLoadingException;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.google.blockly.model.BlockFactory.block;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for the {@link WorkspaceHelper}.
 */
public class WorkspaceHelperTest extends BlocklyTestCase {
    private WorkspaceHelper mWorkspaceHelper;
    private WorkspaceView mWorkspaceView;
    private BlockViewFactory mViewFactory;
    private BlockFactory mBlockFactory;

    private ConnectionManager mockConnectionManager;
    private BlockTouchHandler mockTouchHandler;

    @Before
    public void setUp() throws Exception {
        configureForThemes();
        mockConnectionManager = mock(ConnectionManager.class);
        mockTouchHandler = mock(BlockTouchHandler.class);
        mWorkspaceView = new WorkspaceView(getContext());
        mWorkspaceHelper = new WorkspaceHelper(getContext());
        mViewFactory = new VerticalBlockViewFactory(getContext(), mWorkspaceHelper);
        // TODO(#435): Replace R.raw.test_blocks
        mBlockFactory = new BlockFactory(getContext(), new int[] {R.raw.test_blocks});
    }

    // test getParentBlockGroup
    @Test
    public void testGetNearestParentBlockGroup()
            throws InterruptedException, BlockLoadingException {
        final List<Block> blocks = new ArrayList<>();
        Block root = mBlockFactory.obtain(block().ofType("statement_no_input"));
        Block cur = root;
        // Make a chain of statement blocks, all of which will be in the same block group.
        for (int i = 0; i < 3; i++) {
            cur.getNextConnection().connect(
                    mBlockFactory.obtain(block().ofType("statement_no_input"))
                    .getPreviousConnection());
            cur = cur.getNextBlock();
        }

        // Add a block that has inputs at the end of the chain.
        cur.getNextConnection().connect(
                mBlockFactory.obtain(block().ofType("statement_value_input"))
                .getPreviousConnection());
        cur = cur.getNextBlock();

        // Connect a block as an input.  It should be in its own block group.
        Block hasOutput = mBlockFactory.obtain(block().ofType("output_no_input"));
        cur.getInputByName("value").getConnection().connect(hasOutput.getOutputConnection());

        blocks.add(root);

        // Add a completely unconnected block.
        blocks.add(mBlockFactory.obtain(block().ofType("statement_no_input")));

        TestUtils.createViews(blocks, mViewFactory, mockConnectionManager, mWorkspaceView);

        assertThat(mWorkspaceHelper.getParentBlockGroup(root))
                .isSameAs(mWorkspaceHelper.getParentBlockGroup(cur));
        assertThat(mWorkspaceHelper.getParentBlockGroup(blocks.get(0)))
                .isNotSameAs(mWorkspaceHelper.getParentBlockGroup(blocks.get(1)));
        assertThat(mWorkspaceHelper.getParentBlockGroup(root))
                .isNotSameAs(mWorkspaceHelper.getParentBlockGroup(hasOutput));
    }


    // test getDraggableBlockGroup
    @Test
    public void testGetRootBlockGroup() throws InterruptedException, BlockLoadingException {
        final List<Block> blocks = new ArrayList<>();
        Block root = mBlockFactory.obtain(block().ofType("statement_statement_input"));
        Block cur = root;
        // Make a chain of blocks with statement inputs.  Each block will be connected to a
        // statement input on the block above.
        for (int i = 0; i < 3; i++) {
            cur.getInputByName("statement input").getConnection().connect(
                    mBlockFactory.obtain(block().ofType("statement_statement_input"))
                            .getPreviousConnection());
            cur = cur.getInputByName("statement input").getConnection().getTargetBlock();
        }
        // At the end of the chain, add a block as a "next".  It will still be in the same root
        // block group.
        Block finalBlock = mBlockFactory.obtain(block().ofType("statement_no_input"));
        cur.getNextConnection().connect(finalBlock.getPreviousConnection());
        blocks.add(root);

        // Add a completely unconnected block.
        blocks.add(mBlockFactory.obtain(block().ofType("empty_block")));

        TestUtils.createViews(blocks, mViewFactory, mockConnectionManager, mWorkspaceView);

        assertThat(mWorkspaceHelper.getRootBlockGroup(root))
                .isSameAs(mWorkspaceHelper.getRootBlockGroup(cur));
        assertThat(mWorkspaceHelper.getRootBlockGroup(root))
                .isSameAs(mWorkspaceHelper.getRootBlockGroup(finalBlock));
        assertThat(Arrays.toString(blocks.toArray()))
                .isNotSameAs(mWorkspaceHelper.getRootBlockGroup(blocks.get(0)));
        assertThat(Arrays.toString(blocks.toArray()))
                .isNotSameAs(mWorkspaceHelper.getRootBlockGroup(blocks.get(1)));
    }
 }
