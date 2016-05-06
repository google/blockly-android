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
import com.google.blockly.android.R;
import com.google.blockly.android.MockitoAndroidTestCase;
import com.google.blockly.android.TestUtils;
import com.google.blockly.android.control.ConnectionManager;
import com.google.blockly.android.ui.vertical.VerticalBlockViewFactory;
import com.google.blockly.model.Block;
import com.google.blockly.model.BlockFactory;

import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tests for the {@link WorkspaceHelper}.
 */
public class WorkspaceHelperTest extends MockitoAndroidTestCase {
    private WorkspaceHelper mWorkspaceHelper;
    private WorkspaceView mWorkspaceView;
    private BlockViewFactory mViewFactory;
    private BlockFactory mBlockFactory;

    @Mock
    private ConnectionManager mockConnectionManager;
    @Mock
    private BlockTouchHandler mockTouchHandler;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        mWorkspaceView = new WorkspaceView(getContext());
        mWorkspaceHelper = new WorkspaceHelper(getContext());
        mViewFactory = new VerticalBlockViewFactory(getContext(), mWorkspaceHelper);
        mBlockFactory = new BlockFactory(getContext(), new int[] {R.raw.test_blocks});
    }

    // test getParentBlockGroup
    public void testGetNearestParentBlockGroup() throws InterruptedException {
        final List<Block> blocks = new ArrayList<>();
        Block root = mBlockFactory.obtainBlock("statement_no_input", null);
        Block cur = root;
        // Make a chain of statement blocks, all of which will be in the same block group.
        for (int i = 0; i < 3; i++) {
            cur.getNextConnection().connect(
                    mBlockFactory.obtainBlock("statement_no_input", null).getPreviousConnection());
            cur = cur.getNextBlock();
        }

        // Add a block that has inputs at the end of the chain.
        cur.getNextConnection().connect(
                mBlockFactory.obtainBlock("statement_value_input", null).getPreviousConnection());
        cur = cur.getNextBlock();

        // Connect a block as an input.  It should be in its own block group.
        Block hasOutput = mBlockFactory.obtainBlock("output_no_input", null);
        cur.getInputByName("value").getConnection().connect(hasOutput.getOutputConnection());

        blocks.add(root);

        // Add a completely unconnected block.
        blocks.add(mBlockFactory.obtainBlock("statement_no_input", null));

        TestUtils.createViews(blocks, mViewFactory, mockConnectionManager, mWorkspaceView);

        assertSame(mWorkspaceHelper.getParentBlockGroup(root),
                mWorkspaceHelper.getParentBlockGroup(cur));

        assertNotSame(mWorkspaceHelper.getParentBlockGroup(blocks.get(0)),
                mWorkspaceHelper.getParentBlockGroup(blocks.get(1)));

        assertNotSame(mWorkspaceHelper.getParentBlockGroup(root),
                mWorkspaceHelper.getParentBlockGroup(hasOutput));
    }


    // test getDraggableBlockGroup
    public void testGetRootBlockGroup() throws InterruptedException {
        final List<Block> blocks = new ArrayList<>();
        Block root = mBlockFactory.obtainBlock("statement_statement_input", null);
        Block cur = root;
        // Make a chain of blocks with statement inputs.  Each block will be connected to a
        // statement input on the block above.
        for (int i = 0; i < 3; i++) {
            cur.getInputByName("statement input").getConnection().connect(
                    mBlockFactory.obtainBlock("statement_statement_input", null)
                            .getPreviousConnection());
            cur = cur.getInputByName("statement input").getConnection().getTargetBlock();
        }
        // At the end of the chain, add a block as a "next".  It will still be in the same root
        // block group.
        Block finalBlock = mBlockFactory.obtainBlock("statement_no_input", null);
        cur.getNextConnection().connect(finalBlock.getPreviousConnection());
        blocks.add(root);

        // Add a completely unconnected block.
        blocks.add(mBlockFactory.obtainBlock("empty_block", null));

        TestUtils.createViews(blocks, mViewFactory, mockConnectionManager, mWorkspaceView);

        assertSame(mWorkspaceHelper.getRootBlockGroup(root),
                mWorkspaceHelper.getRootBlockGroup(cur));

        assertSame(mWorkspaceHelper.getRootBlockGroup(root),
                mWorkspaceHelper.getRootBlockGroup(finalBlock));

        assertNotSame(Arrays.toString(blocks.toArray()),
                mWorkspaceHelper.getRootBlockGroup(blocks.get(0)),
                mWorkspaceHelper.getRootBlockGroup(blocks.get(1)));
    }
 }
