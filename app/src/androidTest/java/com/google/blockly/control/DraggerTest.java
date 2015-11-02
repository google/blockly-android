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

package com.google.blockly.control;

import com.google.blockly.MockitoAndroidTestCase;
import com.google.blockly.R;
import com.google.blockly.TestUtils;
import com.google.blockly.model.Block;
import com.google.blockly.model.BlockFactory;
import com.google.blockly.ui.WorkspaceHelper;
import com.google.blockly.ui.WorkspaceView;

import org.mockito.Mock;

import java.util.ArrayList;

/**
 * Tests for the {@link Dragger}.
 */
public class DraggerTest extends MockitoAndroidTestCase {
    @Mock
    ConnectionManager mConnectionManager;
    private WorkspaceHelper mWorkspaceHelper;
    private WorkspaceView mWorkspaceView;
    private Dragger mDragger;
    private BlockFactory mBlockFactory;
    private ArrayList<Block> mBlocks;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mBlockFactory = new BlockFactory(getContext(), new int[]{R.raw.toolbox_blocks});

        mBlocks = new ArrayList<>();
        mWorkspaceView = new WorkspaceView(getContext());
        mWorkspaceHelper = new WorkspaceHelper(mWorkspaceView, null);
        mDragger = new Dragger(mWorkspaceHelper, mWorkspaceView, mConnectionManager, mBlocks);
    }

    public void testConnectAsChild() {
        // Setup
        Block first = mBlockFactory.obtainBlock("simple_input_output", "first block");
        Block second = mBlockFactory.obtainBlock("simple_input_output", "second block");
        Block third = mBlockFactory.obtainBlock("multiple_input_output", "third block");
        Block fourth = mBlockFactory.obtainBlock("output_no_input", "fourth block");
        mBlocks.add(first);
        mBlocks.add(second);
        mBlocks.add(third);
        mBlocks.add(fourth);
        TestUtils.createViews(mBlocks, getContext(), mWorkspaceHelper, mConnectionManager,
                mWorkspaceView);

        // No bump, no splice.
        mDragger.removeFromRoot(second);
        mDragger.connectAsChild(first.getOnlyValueInput().getConnection(),
                second.getOutputConnection());

        // Second is now a child of first.
        assertSame(first, second.getOutputConnection().getTargetBlock());
        assertNotSame(mWorkspaceHelper.getNearestParentBlockGroup(first),
                mWorkspaceHelper.getNearestParentBlockGroup(second));
        assertSame(mWorkspaceHelper.getRootBlockGroup(first),
                mWorkspaceHelper.getRootBlockGroup(second));

        // Bump: no next input
        mDragger.removeFromRoot(fourth);
        mDragger.connectAsChild(first.getOnlyValueInput().getConnection(),
                fourth.getOutputConnection());

        // Fourth is now a child of first.
        assertSame(first, fourth.getOutputConnection().getTargetBlock());
        assertNotSame(mWorkspaceHelper.getNearestParentBlockGroup(first),
                mWorkspaceHelper.getNearestParentBlockGroup(fourth));
        assertSame(mWorkspaceHelper.getRootBlockGroup(first),
                mWorkspaceHelper.getRootBlockGroup(fourth));

        // Second has been returned to the workspace root.
        assertNull(second.getOutputConnection().getTargetBlock());
        assertTrue(mBlocks.contains(second));
        assertNotSame(mWorkspaceHelper.getRootBlockGroup(first),
                mWorkspaceHelper.getRootBlockGroup(second));

        // Bump: Child block has branching inputs
        mDragger.removeFromRoot(third);
        mDragger.connectAsChild(first.getOnlyValueInput().getConnection(),
                third.getOutputConnection());

        // Third is now a child of first
        assertSame(first, third.getOutputConnection().getTargetBlock());
        assertNotSame(mWorkspaceHelper.getNearestParentBlockGroup(first),
                mWorkspaceHelper.getNearestParentBlockGroup(third));
        assertSame(mWorkspaceHelper.getRootBlockGroup(first),
                mWorkspaceHelper.getRootBlockGroup(third));

        // Fourth has been returned to the workspace root.
        assertNull(fourth.getOutputConnection().getTargetBlock());
        assertTrue(mBlocks.contains(fourth));
        assertNotSame(mWorkspaceHelper.getRootBlockGroup(first),
                mWorkspaceHelper.getRootBlockGroup(fourth));

        // Splice
        mDragger.removeFromRoot(second);
        mDragger.connectAsChild(first.getOnlyValueInput().getConnection(),
                second.getOutputConnection());

        assertSame(first, second.getOutputConnection().getTargetBlock());
        assertSame(second, third.getOutputConnection().getTargetBlock());

        assertNotSame(mWorkspaceHelper.getNearestParentBlockGroup(first),
                mWorkspaceHelper.getNearestParentBlockGroup(second));
        assertNotSame(mWorkspaceHelper.getNearestParentBlockGroup(third),
                mWorkspaceHelper.getNearestParentBlockGroup(second));

        assertSame(mWorkspaceHelper.getRootBlockGroup(first),
                mWorkspaceHelper.getRootBlockGroup(second));
        assertSame(mWorkspaceHelper.getRootBlockGroup(first),
                mWorkspaceHelper.getRootBlockGroup(third));
    }

    public void testConnectAfter() {
        // setup
        Block first = mBlockFactory.obtainBlock("statement_no_input", "first block");
        Block second = mBlockFactory.obtainBlock("statement_no_input", "second block");
        Block third = mBlockFactory.obtainBlock("statement_no_input", "third block");
        Block fourth = mBlockFactory.obtainBlock("statement_no_next", "fourth block");
        mBlocks.add(first);
        mBlocks.add(second);
        mBlocks.add(third);
        mBlocks.add(fourth);
        TestUtils.createViews(mBlocks, getContext(), mWorkspaceHelper, mConnectionManager,
                mWorkspaceView);

        // no bump, no splice
        mDragger.removeFromRoot(second);
        // Connect "second" after "first".
        mDragger.connectAfter(first, second);
        assertSame(first, second.getPreviousBlock());
        assertSame(mWorkspaceHelper.getNearestParentBlockGroup(first),
                mWorkspaceHelper.getNearestParentBlockGroup(second));
        assertSame(mWorkspaceHelper.getRootBlockGroup(first),
                mWorkspaceHelper.getRootBlockGroup(second));

        // splice, no bump
        mDragger.removeFromRoot(third);
        // Connect "third" after "first".
        mDragger.connectAfter(first, third);

        assertSame(first, third.getPreviousBlock());
        assertSame(third, second.getPreviousBlock());

        assertSame(mWorkspaceHelper.getNearestParentBlockGroup(first),
                mWorkspaceHelper.getNearestParentBlockGroup(second));
        assertSame(mWorkspaceHelper.getNearestParentBlockGroup(third),
                mWorkspaceHelper.getNearestParentBlockGroup(second));

        assertSame(mWorkspaceHelper.getRootBlockGroup(first),
                mWorkspaceHelper.getRootBlockGroup(second));
        assertSame(mWorkspaceHelper.getRootBlockGroup(first),
                mWorkspaceHelper.getRootBlockGroup(third));

        // bump, no splice
        mDragger.removeFromRoot(fourth);
        // Connect "fourth" after "first".  Since "fourth" has no next connection, bump.
        mDragger.connectAfter(first, fourth);

        assertSame(first, fourth.getPreviousBlock());
        // Third has been returned to the workspace root.
        assertNull(third.getPreviousBlock());
        assertTrue(mBlocks.contains(third));
        // First and fourth are connected.
        assertSame(mWorkspaceHelper.getRootBlockGroup(first),
                mWorkspaceHelper.getRootBlockGroup(fourth));
        // Second and third are separate from first and fourth.
        assertNotSame(mWorkspaceHelper.getRootBlockGroup(first),
                mWorkspaceHelper.getRootBlockGroup(third));
        // At workspace root.
        assertSame(mWorkspaceHelper.getRootBlockGroup(third),
                mWorkspaceHelper.getRootBlockGroup(second));
    }

    // Test connect statement
    public void testConnectToStatement() {
        // setup
        Block first = mBlockFactory.obtainBlock("statement_statement_input", "first block");
        Block second = mBlockFactory.obtainBlock("statement_statement_input", "second block");
        Block third = mBlockFactory.obtainBlock("statement_statement_input", "third block");
        Block fourth = mBlockFactory.obtainBlock("statement_no_next", "fourth block");
        mBlocks.add(first);
        mBlocks.add(second);
        mBlocks.add(third);
        mBlocks.add(fourth);
        TestUtils.createViews(mBlocks, getContext(), mWorkspaceHelper, mConnectionManager,
                mWorkspaceView);

        // No bump, no splice
        mDragger.removeFromRoot(second);
        mDragger.connectToStatement(first.getInputByName("statement input").getConnection(),
                second);

        assertSame(first, second.getPreviousBlock());
        assertNotSame(mWorkspaceHelper.getNearestParentBlockGroup(first),
                mWorkspaceHelper.getNearestParentBlockGroup(second));
        assertSame(mWorkspaceHelper.getRootBlockGroup(first),
                mWorkspaceHelper.getRootBlockGroup(second));

        // Splice, no bump
        mDragger.removeFromRoot(third);
        mDragger.connectToStatement(first.getInputByName("statement input").getConnection(),
                third);

        assertSame(first, third.getPreviousBlock());
        assertSame(third, second.getPreviousBlock());

        assertNotSame(mWorkspaceHelper.getNearestParentBlockGroup(first),
                mWorkspaceHelper.getNearestParentBlockGroup(second));
        assertSame(mWorkspaceHelper.getNearestParentBlockGroup(third),
                mWorkspaceHelper.getNearestParentBlockGroup(second));

        assertSame(mWorkspaceHelper.getRootBlockGroup(first),
                mWorkspaceHelper.getRootBlockGroup(second));
        assertSame(mWorkspaceHelper.getRootBlockGroup(first),
                mWorkspaceHelper.getRootBlockGroup(third));

        // Bump, no splice
        mDragger.removeFromRoot(fourth);
        mDragger.connectToStatement(first.getInputByName("statement input").getConnection(),
                fourth);

        assertSame(first, fourth.getPreviousBlock());
        // Third has been returned to the workspace root.
        assertNull(third.getPreviousBlock());
        assertTrue(mBlocks.contains(third));
        // First and fourth are connected.
        assertSame(mWorkspaceHelper.getRootBlockGroup(first),
                mWorkspaceHelper.getRootBlockGroup(fourth));
        // Second and third are separate from first and fourth.
        assertNotSame(mWorkspaceHelper.getRootBlockGroup(first),
                mWorkspaceHelper.getRootBlockGroup(third));
        // At workspace root.
        assertSame(mWorkspaceHelper.getRootBlockGroup(third),
                mWorkspaceHelper.getRootBlockGroup(second));
    }
}
