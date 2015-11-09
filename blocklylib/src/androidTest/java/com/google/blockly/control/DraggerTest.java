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

import android.view.DragEvent;

import com.google.blockly.MockitoAndroidTestCase;
import com.google.blockly.R;
import com.google.blockly.TestUtils;
import com.google.blockly.model.Block;
import com.google.blockly.model.BlockFactory;
import com.google.blockly.ui.BlockView;
import com.google.blockly.ui.WorkspaceHelper;
import com.google.blockly.ui.WorkspaceView;

import org.mockito.Mock;

import java.util.ArrayList;

import static org.mockito.Mockito.when;

/**
 * Tests for the {@link Dragger}.
 */
public class DraggerTest extends MockitoAndroidTestCase {
    private ConnectionManager mConnectionManager;

    @Mock
    private DragEvent mDragEvent;

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
        mConnectionManager = new ConnectionManager();
        mWorkspaceHelper = new WorkspaceHelper(mWorkspaceView, null);
        mDragger = new Dragger(mWorkspaceHelper, mWorkspaceView, mConnectionManager, mBlocks);
    }

    /** This set of tests exercises the many paths through reconnectViews. **/

    public void testConnectAsChild() {
        // Setup
        Block first = mBlockFactory.obtainBlock("simple_input_output", "first block");
        Block second = mBlockFactory.obtainBlock("simple_input_output", "second block");
        mBlocks.add(first);
        mBlocks.add(second);
        TestUtils.createViews(mBlocks, getContext(), mWorkspaceHelper, mConnectionManager,
                mWorkspaceView);

        // No bump, no splice.
        mDragger.reconnectViews(second.getOutputConnection(),
                first.getOnlyValueInput().getConnection(), second, null);

        // Second is now a child of first.
        assertSame(first, second.getOutputConnection().getTargetBlock());
        assertNotSame(mWorkspaceHelper.getNearestParentBlockGroup(first),
                mWorkspaceHelper.getNearestParentBlockGroup(second));
        assertSame(mWorkspaceHelper.getRootBlockGroup(first),
                mWorkspaceHelper.getRootBlockGroup(second));
    }

    public void testConnectAsChildBumpNoInput() {
        // Setup
        Block first = mBlockFactory.obtainBlock("simple_input_output", "first block");
        Block second = mBlockFactory.obtainBlock("simple_input_output", "second block");
        Block third = mBlockFactory.obtainBlock("output_no_input", "fourth block");

        // Connect the output of second to the input of first.
        second.getOutputConnection().connect(first.getOnlyValueInput().getConnection());

        mBlocks.add(first);
        mBlocks.add(third);
        TestUtils.createViews(mBlocks, getContext(), mWorkspaceHelper, mConnectionManager,
                mWorkspaceView);

        // Bump: no next input
        mDragger.reconnectViews(third.getOutputConnection(),
                first.getOnlyValueInput().getConnection(), third, null);

        // Third is now a child of first.
        assertSame(first, third.getOutputConnection().getTargetBlock());
        assertNotSame(mWorkspaceHelper.getNearestParentBlockGroup(first),
                mWorkspaceHelper.getNearestParentBlockGroup(third));
        assertSame(mWorkspaceHelper.getRootBlockGroup(first),
                mWorkspaceHelper.getRootBlockGroup(third));

        // Second has been returned to the workspace root.
        assertNull(second.getOutputConnection().getTargetBlock());
        assertTrue(mBlocks.contains(second));
        assertNotSame(mWorkspaceHelper.getRootBlockGroup(first),
                mWorkspaceHelper.getRootBlockGroup(second));
    }

    public void testConnectAsChildBumpMultipleInputs() {
        // Setup
        Block first = mBlockFactory.obtainBlock("simple_input_output", "first block");
        Block second = mBlockFactory.obtainBlock("simple_input_output", "second block");
        Block third = mBlockFactory.obtainBlock("multiple_input_output", "third block");

        // Connect the output of second to the input of first.
        second.getOutputConnection().connect(first.getOnlyValueInput().getConnection());

        mBlocks.add(first);
        mBlocks.add(third);
        TestUtils.createViews(mBlocks, getContext(), mWorkspaceHelper, mConnectionManager,
                mWorkspaceView);

        // Bump: Child block has branching inputs
        mDragger.reconnectViews(third.getOutputConnection(),
                first.getOnlyValueInput().getConnection(), third, null);

        // Third is now a child of first
        assertSame(first, third.getOutputConnection().getTargetBlock());
        assertNotSame(mWorkspaceHelper.getNearestParentBlockGroup(first),
                mWorkspaceHelper.getNearestParentBlockGroup(third));
        assertSame(mWorkspaceHelper.getRootBlockGroup(first),
                mWorkspaceHelper.getRootBlockGroup(third));

        // Second has been returned to the workspace root.
        assertNull(second.getOutputConnection().getTargetBlock());
        assertTrue(mBlocks.contains(second));
        assertNotSame(mWorkspaceHelper.getRootBlockGroup(first),
                mWorkspaceHelper.getRootBlockGroup(second));
    }

    public void testConnectAsChildSplice() {
        // Setup
        Block first = mBlockFactory.obtainBlock("simple_input_output", "first block");
        Block second = mBlockFactory.obtainBlock("multiple_input_output", "third block");
        Block third = mBlockFactory.obtainBlock("simple_input_output", "second block");

        // Connect the output of second to the input of first.
        second.getOutputConnection().connect(first.getOnlyValueInput().getConnection());

        mBlocks.add(first);
        mBlocks.add(third);

        TestUtils.createViews(mBlocks, getContext(), mWorkspaceHelper, mConnectionManager,
                mWorkspaceView);

        // Splice third between second and first.
        mDragger.reconnectViews(third.getOutputConnection(),
                first.getOnlyValueInput().getConnection(), third, null);

        assertSame(first, third.getOutputConnection().getTargetBlock());
        assertSame(third, second.getOutputConnection().getTargetBlock());

        assertNotSame(mWorkspaceHelper.getNearestParentBlockGroup(first),
                mWorkspaceHelper.getNearestParentBlockGroup(third));
        assertNotSame(mWorkspaceHelper.getNearestParentBlockGroup(second),
                mWorkspaceHelper.getNearestParentBlockGroup(third));

        assertSame(mWorkspaceHelper.getRootBlockGroup(first),
                mWorkspaceHelper.getRootBlockGroup(third));
        assertSame(mWorkspaceHelper.getRootBlockGroup(first),
                mWorkspaceHelper.getRootBlockGroup(second));
    }

    public void testConnectAfter() {
        // setup
        Block first = mBlockFactory.obtainBlock("statement_no_input", "first block");
        Block second = mBlockFactory.obtainBlock("statement_no_input", "second block");
        mBlocks.add(first);
        mBlocks.add(second);
        TestUtils.createViews(mBlocks, getContext(), mWorkspaceHelper, mConnectionManager,
                mWorkspaceView);

        // Connect "second" after "first": no bump, no splice
        mDragger.reconnectViews(second.getPreviousConnection(),
                first.getNextConnection(), second, null);

        assertSame(first, second.getPreviousBlock());
        assertSame(mWorkspaceHelper.getNearestParentBlockGroup(first),
                mWorkspaceHelper.getNearestParentBlockGroup(second));
        assertSame(mWorkspaceHelper.getRootBlockGroup(first),
                mWorkspaceHelper.getRootBlockGroup(second));
    }

    public void testConnectAfterSplice() {
            // setup
        Block first = mBlockFactory.obtainBlock("statement_no_input", "first block");
        Block second = mBlockFactory.obtainBlock("statement_no_input", "second block");
        Block third = mBlockFactory.obtainBlock("statement_no_input", "third block");

        second.getPreviousConnection().connect(first.getNextConnection());

        mBlocks.add(first);
        mBlocks.add(third);
        TestUtils.createViews(mBlocks, getContext(), mWorkspaceHelper, mConnectionManager,
                mWorkspaceView);

        // Connect "third" after "first", causing a splice.
        mDragger.reconnectViews(third.getPreviousConnection(),
                first.getNextConnection(), third, null);

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
    }

    public void testConnectAfterBump() {
        // setup
        Block first = mBlockFactory.obtainBlock("statement_no_input", "first block");
        Block second = mBlockFactory.obtainBlock("statement_no_input", "second block");
        Block third = mBlockFactory.obtainBlock("statement_no_input", "third block");
        Block fourth = mBlockFactory.obtainBlock("statement_no_next", "fourth block");

        // Make a stack of three statements.
        second.getPreviousConnection().connect(first.getNextConnection());
        third.getPreviousConnection().connect(second.getNextConnection());

        mBlocks.add(first);
        mBlocks.add(fourth);
        TestUtils.createViews(mBlocks, getContext(), mWorkspaceHelper, mConnectionManager,
                mWorkspaceView);

        // Connect "fourth" after "first".  Since "fourth" has no next connection, bump.
        mDragger.reconnectViews(fourth.getPreviousConnection(),
                first.getNextConnection(), fourth, null);

        assertSame(first, fourth.getPreviousBlock());
        // Second has been returned to the workspace root.
        assertNull(second.getPreviousBlock());
        assertTrue(mBlocks.contains(second));
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

    public void testConnectToStatement() {
        // setup
        Block first = mBlockFactory.obtainBlock("statement_statement_input", "first block");
        Block second = mBlockFactory.obtainBlock("statement_statement_input", "second block");
        mBlocks.add(first);
        mBlocks.add(second);
        TestUtils.createViews(mBlocks, getContext(), mWorkspaceHelper, mConnectionManager,
                mWorkspaceView);

        // No bump, no splice
        mDragger.reconnectViews(second.getPreviousConnection(),
                first.getInputByName("statement input").getConnection(), second, null);

        assertSame(first, second.getPreviousBlock());
        assertNotSame(mWorkspaceHelper.getNearestParentBlockGroup(first),
                mWorkspaceHelper.getNearestParentBlockGroup(second));
        assertSame(mWorkspaceHelper.getRootBlockGroup(first),
                mWorkspaceHelper.getRootBlockGroup(second));
    }

    public void testConnectToStatementSplice() {
        // setup
        Block first = mBlockFactory.obtainBlock("statement_statement_input", "first block");
        Block second = mBlockFactory.obtainBlock("statement_statement_input", "second block");
        Block third = mBlockFactory.obtainBlock("statement_statement_input", "third block");

        // Connect first and second together.
        first.getInputByName("statement input").getConnection()
                .connect(second.getPreviousConnection());

        mBlocks.add(first);
        mBlocks.add(third);
        TestUtils.createViews(mBlocks, getContext(), mWorkspaceHelper, mConnectionManager,
                mWorkspaceView);

        // Splice third in between first and second.
        mDragger.reconnectViews(third.getPreviousConnection(),
                first.getInputByName("statement input").getConnection(), third, null);

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
    }

    public void testConnectToStatementBump() {
        Block first = mBlockFactory.obtainBlock("statement_statement_input", "first block");
        Block second = mBlockFactory.obtainBlock("statement_statement_input", "second block");
        Block third = mBlockFactory.obtainBlock("statement_statement_input", "third block");
        Block fourth = mBlockFactory.obtainBlock("statement_no_next", "fourth block");

        // Connect first, second, and third together.
        first.getInputByName("statement input").getConnection()
                .connect(second.getPreviousConnection());
        second.getInputByName("statement input").getConnection()
                .connect(third.getPreviousConnection());

        mBlocks.add(first);
        mBlocks.add(fourth);
        TestUtils.createViews(mBlocks, getContext(), mWorkspaceHelper, mConnectionManager,
                mWorkspaceView);

        // Connect fourth where second is currently connected.  This will bump second (and third,
        // which is connected to it) back to the root.
        mDragger.reconnectViews(fourth.getPreviousConnection(),
                first.getInputByName("statement input").getConnection(), fourth, null);

        assertSame(first, fourth.getPreviousBlock());
        // Second has been returned to the workspace root.
        assertNull(second.getPreviousBlock());
        assertTrue(mBlocks.contains(second));
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

    /* This set of tests covers the full sequence of dragging operations.
     * Because we're skipping layout, the connector locations will never be exactly perfect.
     * Calling updateConnectorLocations puts all of the connections on a block at the
     * workspace position of that block.
     */

    // Drag together two compatible blocks.
    public void testDragConnect() {
        // Setup
        Block first = mBlockFactory.obtainBlock("simple_input_output", "first block");
        Block second = mBlockFactory.obtainBlock("output_no_input", "second block");
        setupDrag(first, second);
        dragBlockToTarget(first, second);

        assertEquals(1, mBlocks.size());
        assertEquals(1, mWorkspaceView.getChildCount());
        assertSame(first.getInputByName("value").getConnection().getTargetConnection(),
                second.getOutputConnection());
    }

    // Drag together two incompatible blocks.
    public void testDragNoConnect() {
        // Setup
        Block first = mBlockFactory.obtainBlock("simple_input_output", "first block");
        Block second = mBlockFactory.obtainBlock("statement_no_next", "second block");
        setupDrag(first, second);
        dragBlockToTarget(first, second);

        assertEquals(2, mBlocks.size());
        assertEquals(2, mWorkspaceView.getChildCount());
        assertFalse(first.getInputByName("value").getConnection().isConnected());
        assertFalse(second.getPreviousConnection().isConnected());
        assertFalse(first.getOutputConnection().isConnected());
    }

    private void setupDrag(Block first, Block second) {
        // Set the blocks to not be in the same place.
        first.setPosition(100, 100);
        second.setPosition(0, 50);
        mBlocks.add(first);
        mBlocks.add(second);
        TestUtils.createViews(mBlocks, getContext(), mWorkspaceHelper, mConnectionManager,
                mWorkspaceView);

        // Layout never happens during this test, so we're forcing the connection locations
        // to be set from the block positions before we try to use them.
        first.getView().updateConnectorLocations();
        second.getView().updateConnectorLocations();
    }

    /**
     * Drag a block to sit directly on top of another block.
     *
     * @param toDrag The {@link Block} to move.
     * @param stationary The {@link Block} to move to.
     */
    private void dragBlockToTarget(Block toDrag, Block stationary) {
        BlockView bv = toDrag.getView();
        int startX = mWorkspaceHelper.workspaceToVirtualViewUnits(toDrag.getPosition().x);
        int startY = mWorkspaceHelper.workspaceToVirtualViewUnits(toDrag.getPosition().y);
        mDragger.startDragging(bv, startX, startY);

        // Pretend to be the last DragEvent that registers, which should be right by the
        // stationary block.
        // getX() returns float, even though we'll cast back to int immediately.
        when(mDragEvent.getX()).thenReturn(
                (float) mWorkspaceHelper.workspaceToVirtualViewUnits(stationary.getPosition().x));
        when(mDragEvent.getY()).thenReturn(
                (float) mWorkspaceHelper.workspaceToVirtualViewUnits(stationary.getPosition().y));

        mDragger.continueDragging(mDragEvent);

        // Force the connector locations to update before the call to finishDragging().
        toDrag.getView().updateConnectorLocations();
        stationary.getView().updateConnectorLocations();

        mDragger.finishDragging();
    }
}
