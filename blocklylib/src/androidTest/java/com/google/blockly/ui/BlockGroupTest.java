/*
 *  Copyright  2015 Google Inc. All Rights Reserved.
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

package com.google.blockly.ui;

import com.google.blockly.MockitoAndroidTestCase;
import com.google.blockly.R;
import com.google.blockly.TestUtils;
import com.google.blockly.control.ConnectionManager;
import com.google.blockly.model.Block;
import com.google.blockly.model.BlockFactory;

import org.mockito.Mock;

import java.util.ArrayList;

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
        mWorkspaceHelper = new WorkspaceHelper(getContext(), null);
        mBlockFactory = new BlockFactory(getContext(), new int[]{R.raw.toolbox_blocks});
    }

    public void testLastChildBlock() {
        // Empty block group
        BlockGroup bg = new BlockGroup(getContext(), mWorkspaceHelper);
        assertNull(bg.lastChildBlock());

        // One child block
        Block childBlock = mBlockFactory.obtainBlock("simple_input_output", "child block");
        // Add the block's view to bg.
        mWorkspaceHelper.obtainBlockView(getContext(), childBlock, bg, mConnectionManager, null);
        assertSame(childBlock, bg.lastChildBlock());

        // Two child blocks.  The blocks don't have to be connected in the model.
        childBlock = mBlockFactory.obtainBlock("multiple_input_output", "child block");
        mWorkspaceHelper.obtainBlockView(getContext(), childBlock, bg, mConnectionManager, null);
        assertSame(childBlock, bg.lastChildBlock());
    }

    public void testGetLastInputConnectionSimples() {
        // Two simple input blocks
        ArrayList<Block> blocks = new ArrayList<>();
        Block first = mBlockFactory.obtainBlock("simple_input_output", "first block");
        Block second = mBlockFactory.obtainBlock("simple_input_output", "second block");
        first.getOnlyValueInput().getConnection().connect(second.getOutputConnection());
        blocks.add(first);

        TestUtils.createViews(blocks, getContext(), mWorkspaceHelper, mConnectionManager,
                mWorkspaceView);

        BlockGroup rootBlockGroup = (BlockGroup) first.getView().getParent();
        assertSame(second.getInputByName("value").getConnection(),
                rootBlockGroup.getLastInputConnection());
    }

    public void testGetLastInputConnectionEmpty() {
        // Empty block group.
        BlockGroup bg = new BlockGroup(getContext(), mWorkspaceHelper);
        assertNull(bg.getLastInputConnection());
    }

    public void testGetLastInputConnectionBranch() {
        // Branch at end.
        ArrayList<Block> blocks = new ArrayList<>();
        Block first = mBlockFactory.obtainBlock("simple_input_output", "first block");
        Block second = mBlockFactory.obtainBlock("simple_input_output", "second block");
        Block third = mBlockFactory.obtainBlock("multiple_input_output", "second block");
        first.getOnlyValueInput().getConnection().connect(second.getOutputConnection());
        second.getOnlyValueInput().getConnection().connect(third.getOutputConnection());
        blocks.add(first);

        TestUtils.createViews(blocks, getContext(), mWorkspaceHelper, mConnectionManager,
                mWorkspaceView);

        BlockGroup rootBlockGroup = (BlockGroup) first.getView().getParent();
        assertNull(rootBlockGroup.getLastInputConnection());
    }

    public void testGetLastInputConnectionNoInput() {
        ArrayList<Block> blocks = new ArrayList<>();
        Block first = mBlockFactory.obtainBlock("simple_input_output", "first block");
        Block second = mBlockFactory.obtainBlock("simple_input_output", "second block");
        Block third = mBlockFactory.obtainBlock("output_no_input", "second block");
        first.getOnlyValueInput().getConnection().connect(second.getOutputConnection());
        second.getOnlyValueInput().getConnection().connect(third.getOutputConnection());
        blocks.add(first);

        TestUtils.createViews(blocks, getContext(), mWorkspaceHelper, mConnectionManager,
                mWorkspaceView);

        BlockGroup rootBlockGroup = (BlockGroup) first.getView().getParent();
        assertNull(rootBlockGroup.getLastInputConnection());
    }
}
