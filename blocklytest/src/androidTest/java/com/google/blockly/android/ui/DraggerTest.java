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

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.any;

import android.util.Pair;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;

import com.google.blockly.android.MockitoAndroidTestCase;
import com.google.blockly.android.R;
import com.google.blockly.android.TestUtils;
import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.android.control.ConnectionManager;
import com.google.blockly.android.ui.vertical.VerticalBlockViewFactory;
import com.google.blockly.model.Block;
import com.google.blockly.model.BlockFactory;
import com.google.blockly.model.Connection;
import com.google.blockly.model.Workspace;

import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link Dragger}.
 */
public class DraggerTest extends MockitoAndroidTestCase {

    @Mock
    BlocklyController mMockController;
    @Mock
    Workspace mMockWorkspace;
    @Mock
    ConnectionManager mMockConnectionManager;

    @Mock
    DragEvent mDragStartedEvent;
    @Mock
    DragEvent mDragLocationEvent;

    @Mock
    Dragger.DragHandler mMockDragHandler;


    private ViewPoint mTempViewPoint = new ViewPoint();
    private WorkspaceHelper mWorkspaceHelper;
    private BlockViewFactory mViewFactory;
    private WorkspaceView mWorkspaceView;
    private Dragger mDragger;
    private BlockFactory mBlockFactory;
    private ArrayList<Block> mBlocks;

    // Drag gesture state variables
    long dragStartTime;
    float dragReleaseX, dragReleaseY;


    @Override
    public void setUp() throws Exception {
        super.setUp();

        mBlockFactory = new BlockFactory(getContext(), new int[]{R.raw.test_blocks});
        mBlocks = new ArrayList<>();
        mWorkspaceView = new WorkspaceView(getContext());
        mWorkspaceHelper = new WorkspaceHelper(getContext());
        mWorkspaceHelper.setWorkspaceView(mWorkspaceView);
        mViewFactory = new VerticalBlockViewFactory(getContext(), mWorkspaceHelper);

        Mockito.stub(mMockWorkspace.getConnectionManager()).toReturn(mMockConnectionManager);
        Mockito.stub(mMockController.getBlockFactory()).toReturn(mBlockFactory);
        Mockito.stub(mMockController.getWorkspace()).toReturn(mMockWorkspace);
        Mockito.stub(mMockController.getWorkspaceHelper()).toReturn(mWorkspaceHelper);

        mDragger = new Dragger(mMockController);
        mDragger.setWorkspaceView(mWorkspaceView);
        when(mDragStartedEvent.getAction()).thenReturn(DragEvent.ACTION_DRAG_STARTED);
        when(mDragLocationEvent.getAction()).thenReturn(DragEvent.ACTION_DRAG_LOCATION);
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

        Mockito.when(mMockConnectionManager.findBestConnection(Matchers.same(first), anyInt()))
                .thenReturn(Pair.create(first.getOnlyValueInput().getConnection(),
                        second.getOutputConnection()));

        setupDrag(first, second);
        dragBlockToTarget(first, second);

        Mockito.verify(mMockConnectionManager, atLeastOnce())
                .findBestConnection(Matchers.same(first), anyInt());
        Mockito.verify(mMockController).connect(
                first, first.getOnlyValueInput().getConnection(), second.getOutputConnection());
    }

    // Drag together two incompatible blocks.
    public void testDragNoConnect() {
        // Setup
        Block first = mBlockFactory.obtainBlock("simple_input_output", "first block");
        Block second = mBlockFactory.obtainBlock("output_no_input", "second block");

        Mockito.when(mMockConnectionManager.findBestConnection(any(Block.class), anyInt()))
                .thenReturn(null);

        setupDrag(first, second);
        dragBlockToTarget(first, second);

        Mockito.verify(mMockConnectionManager, atLeastOnce())
                .findBestConnection(Matchers.same(first), anyInt());
        Mockito.verify(mMockController, never())
                .connect(any(Block.class), any(Connection.class), any(Connection.class));
    }

    public void testRemoveConnectionsDuringDrag() {
        // Setup
        Block first = mBlockFactory.obtainBlock("simple_input_output", "first block");
        Block second = mBlockFactory.obtainBlock("simple_input_output", "second block");
        Block third = mBlockFactory.obtainBlock("multiple_input_output", "third block");
        second.getOnlyValueInput().getConnection().connect(third.getOutputConnection());

        ArrayList<Connection> draggedConnections = new ArrayList<>();
        second.getAllConnectionsRecursive(draggedConnections);
        assertEquals(5, draggedConnections.size());

        final ArrayList<Connection> removedConnections = new ArrayList<>();
        final ArrayList<Connection> addedConnections = new ArrayList<>();
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                removedConnections.add((Connection) invocationOnMock.getArguments()[0]);
                return null;
            }
        }).when(mMockConnectionManager).removeConnection(any(Connection.class));
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                addedConnections.add((Connection) invocationOnMock.getArguments()[0]);
                return null;
            }
        }).when(mMockConnectionManager).addConnection(any(Connection.class));

        setupDrag(second, first);
        dragTouch(second, first);

        assertEquals(0, addedConnections.size());
        assertEquals(0, removedConnections.size());

        dragMove(second, first);

        verify(mMockController).extractBlockAsRoot(second);
        assertEquals(draggedConnections.size(), removedConnections.size());
        assertEquals(0, addedConnections.size());
        for (Connection conn : draggedConnections) {
            assertTrue(removedConnections.contains(conn));
            assertTrue(conn.inDragMode());
        }

        // Complete the drag.
        dragRelease(second, first);

        assertEquals(draggedConnections.size(), removedConnections.size());
        assertEquals(draggedConnections.size(), addedConnections.size());
        for (Connection conn : draggedConnections) {
            assertTrue(addedConnections.contains(conn));
            assertFalse(conn.inDragMode());
        }
    }

    private void setupDrag(Block first, Block second) {
        // Set the blocks to not be in the same place.
        first.setPosition(100, 100);
        second.setPosition(0, 50);
        mBlocks.add(first);
        mBlocks.add(second);
        TestUtils.createViews(mBlocks, mViewFactory, mMockConnectionManager, mWorkspaceView);

        // Layout never happens during this test, so we're forcing the connection locations
        // to be set from the block positions before we try to use them.
        mWorkspaceHelper.getView(first).updateConnectorLocations();
        mWorkspaceHelper.getView(second).updateConnectorLocations();
    }

    /**
     * Drag a block to sit directly on top of another block.
     * @param toDrag The {@link Block} to move.
     * @param stationary The {@link Block} to move to.
     */
    private void dragBlockToTarget(Block toDrag, Block stationary) {
        dragTouch(toDrag, stationary);
        dragMove(toDrag, stationary);
        dragRelease(toDrag, stationary);
    }

    private void dragTouch(Block toDrag, Block stationary) {
        BlockView bv = mWorkspaceHelper.getView(toDrag);
        // This is how far we need to move the block by
        int diffX = mWorkspaceHelper.workspaceToVirtualViewUnits(
                stationary.getPosition().x - toDrag.getPosition().x);
        int diffY = mWorkspaceHelper.workspaceToVirtualViewUnits(
                stationary.getPosition().y - toDrag.getPosition().y);
        // Get the initial offset
        mWorkspaceHelper.getVirtualViewCoordinates((View) bv, mTempViewPoint);
        // And calculate the view position to move to to reach the stationary block
        dragReleaseX = diffX + mTempViewPoint.x;
        dragReleaseY= diffY + mTempViewPoint.y;

        dragStartTime = System.currentTimeMillis();
        MotionEvent me = MotionEvent.obtain(
                dragStartTime, dragStartTime, MotionEvent.ACTION_DOWN, 0, 0, 0);
        mDragger.onTouchBlockImpl(mMockDragHandler, bv, me);
    }

    private void dragMove(Block toDrag, Block stationary) {
        BlockView bv = mWorkspaceHelper.getView(toDrag);
        long time = dragStartTime + 10L;
        MotionEvent me = MotionEvent.obtain(time, time, MotionEvent.ACTION_MOVE, 30, -10, 0);
        mDragger.onTouchBlockImpl(mMockDragHandler, bv, me);
        mDragger.getDragEventListener().onDrag((View) bv, mDragStartedEvent);
    }

    private void dragRelease(Block toDrag, Block stationary) {
        BlockView bv = mWorkspaceHelper.getView(toDrag);
        // Pretend to be the last DragEvent that registers, which should be right by the
        // stationary block.
        // getX() returns float, even though we'll cast back to int immediately.
        when(mDragLocationEvent.getX()).thenReturn(dragReleaseX);
        when(mDragLocationEvent.getY()).thenReturn(dragReleaseY);

        mDragger.getDragEventListener().onDrag((View) bv, mDragLocationEvent);

        // Force the connector locations to update before the call to finishDragging().
        bv.updateConnectorLocations();
        mWorkspaceHelper.getView(stationary).updateConnectorLocations();

        mDragger.finishDragging();
    }
}
