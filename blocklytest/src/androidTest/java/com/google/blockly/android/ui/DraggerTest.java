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

import com.google.blockly.android.MockitoAndroidTestCase;
import com.google.blockly.android.R;
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

    private ViewPoint mTempViewPoint = new ViewPoint();
    private WorkspaceHelper mWorkspaceHelper;
    private BlockViewFactory mViewFactory;
    private WorkspaceView mWorkspaceView;
    private Dragger mDragger;
    private BlockTouchHandler mTouchHandler;
    private BlockFactory mBlockFactory;
    private Dragger.DragHandler mDragHandler = new Dragger.DragHandler() {
        @Override
        public void maybeAssignDragGroup(PendingDrag pendingDrag) {
            mPendingDrag = pendingDrag;
            pendingDrag.setDragGroup(mDragGroup);
        }
    };

    // Drag gesture state variables
    Block mTouchedBlock;
    Block mDraggedBlock;
    Block mTargetBlock;
    BlockView mTouchedView;
    BlockGroup mDragGroup;
    PendingDrag mPendingDrag;
    long mDragStartTime;
    float mDragReleaseX, mDragReleaseY;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        mBlockFactory = new BlockFactory(getContext(), new int[]{R.raw.test_blocks});
        mWorkspaceView = new WorkspaceView(getContext());
        mWorkspaceHelper = new WorkspaceHelper(getContext());
        mWorkspaceHelper.setWorkspaceView(mWorkspaceView);
        mViewFactory = new VerticalBlockViewFactory(getContext(), mWorkspaceHelper);

        // The following are queried by the Dragger.
        Mockito.stub(mMockWorkspace.getConnectionManager()).toReturn(mMockConnectionManager);
        Mockito.stub(mMockController.getBlockFactory()).toReturn(mBlockFactory);
        Mockito.stub(mMockController.getWorkspace()).toReturn(mMockWorkspace);
        Mockito.stub(mMockController.getWorkspaceHelper()).toReturn(mWorkspaceHelper);

        mDragger = new Dragger(mMockController);
        mDragger.setWorkspaceView(mWorkspaceView);
        mTouchHandler = mDragger.buildBlockTouchHandler(mDragHandler);

        // Since we can't create DragEvents...
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
        mTouchedBlock = mDraggedBlock = mBlockFactory.obtainBlock(
                "simple_input_output", "first block");
        mTargetBlock = mBlockFactory.obtainBlock("output_no_input", "second block");

        Mockito.when(mMockConnectionManager.findBestConnection(Matchers.same(mTouchedBlock), anyInt()))
                .thenReturn(Pair.create(mTouchedBlock.getOnlyValueInput().getConnection(),
                        mTargetBlock.getOutputConnection()));

        setupDrag();
        dragBlockToTarget();

        Mockito.verify(mMockConnectionManager, atLeastOnce())
                .findBestConnection(Matchers.same(mTouchedBlock), anyInt());
        Mockito.verify(mMockController).connect(
                mTouchedBlock, mTouchedBlock.getOnlyValueInput().getConnection(), mTargetBlock.getOutputConnection());
    }

    // Drag together two incompatible blocks.
    public void testDragNoConnect() {
        // Setup
        mTouchedBlock = mDraggedBlock = mBlockFactory.obtainBlock(
                "simple_input_output", "first block");
        mTargetBlock = mBlockFactory.obtainBlock("output_no_input", "second block");

        Mockito.when(mMockConnectionManager.findBestConnection(any(Block.class), anyInt()))
                .thenReturn(null);

        setupDrag();
        dragBlockToTarget();

        Mockito.verify(mMockConnectionManager, atLeastOnce())
                .findBestConnection(Matchers.same(mTouchedBlock), anyInt());
        Mockito.verify(mMockController, never())
                .connect(any(Block.class), any(Connection.class), any(Connection.class));
    }

    public void testRemoveConnectionsDuringDrag() {
        // Setup
        mTargetBlock = mBlockFactory.obtainBlock("simple_input_output", "first block");
        mTouchedBlock = mDraggedBlock = mBlockFactory.obtainBlock(
                "simple_input_output", "second block");
        Block draggedChild = mBlockFactory.obtainBlock("multiple_input_output", "third block");
        mDraggedBlock.getOnlyValueInput().getConnection().connect(
                draggedChild.getOutputConnection());

        ArrayList<Connection> draggedConnections = new ArrayList<>();
        mDraggedBlock.getAllConnectionsRecursive(draggedConnections);
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

        setupDrag();
        dragTouch();

        assertEquals(0, addedConnections.size());
        assertEquals(0, removedConnections.size());

        dragMove();

        assertEquals(draggedConnections.size(), removedConnections.size());
        assertEquals(0, addedConnections.size());
        for (Connection conn : draggedConnections) {
            assertTrue(removedConnections.contains(conn));
            assertTrue(conn.inDragMode());
        }

        // Complete the drag.
        dragRelease();

        assertEquals(draggedConnections.size(), removedConnections.size());
        assertEquals(draggedConnections.size(), addedConnections.size());
        for (Connection conn : draggedConnections) {
            assertTrue(addedConnections.contains(conn));
            assertFalse(conn.inDragMode());
        }
    }

    private void setupDrag() {
        if (mTouchedBlock == null || mDraggedBlock == null || mTargetBlock == null) {
            throw new IllegalStateException("Blocks must not be null");
        }
        if (mTouchedBlock == mTargetBlock || mDraggedBlock == mTargetBlock) {
            throw new IllegalStateException(
                    "Target block must be different than touched and dragged");
        }

        // Set the workspace blocks to not be in the same place.
        mDraggedBlock.setPosition(100, 100);
        mTargetBlock.setPosition(1000, 1000);

        BlockGroup touchedGroup = mViewFactory.buildBlockGroupTree(
                mTouchedBlock, mMockConnectionManager, mTouchHandler);
        mTouchedView = touchedGroup.getFirstBlockView();
        mDragGroup = (mDraggedBlock == mTouchedBlock) ? touchedGroup :
                mViewFactory.buildBlockGroupTree(mDraggedBlock, mMockConnectionManager,
                                                 mTouchHandler);
        BlockGroup targetGroup = mViewFactory.buildBlockGroupTree(
                mTargetBlock, mMockConnectionManager, null);

        mWorkspaceView.addView(mDragGroup);
        mWorkspaceView.addView(targetGroup);


        Mockito.stub(mMockWorkspace.isRootBlock(mDraggedBlock)).toReturn(true);

        // Layout never happens during this test, so we're forcing the connection locations
        // to be set from the block positions before we try to use them.
        mTouchedView.updateConnectorLocations();
        mDragGroup.updateAllConnectorLocations();
        targetGroup.updateAllConnectorLocations();
    }

    /**
     * Drag a block to sit directly on top of another block.
     */
    private void dragBlockToTarget() {
        dragTouch();
        dragMove();
        dragRelease();
    }

    private void dragTouch() {
        /// Anm REWRITE (this doesn't feel right)     /////////////////////  FIX BEFORE COMMIT <<<<<<<<<<<

        // This is how far we need to move the block by
        int diffX = mWorkspaceHelper.workspaceToVirtualViewUnits(
                mTargetBlock.getPosition().x - mDraggedBlock.getPosition().x);
        int diffY = mWorkspaceHelper.workspaceToVirtualViewUnits(
                mTargetBlock.getPosition().y - mDraggedBlock.getPosition().y);
        // Get the initial offset
        mWorkspaceHelper.getVirtualViewCoordinates(mDragGroup, mTempViewPoint);
        // And calculate the view position to move to to reach the stationary block
        mDragReleaseX = diffX + mTempViewPoint.x;
        mDragReleaseY = diffY + mTempViewPoint.y;

        mDragStartTime = System.currentTimeMillis();
        MotionEvent me = MotionEvent.obtain(
                mDragStartTime, mDragStartTime, MotionEvent.ACTION_DOWN, 0, 0, 0);
        mDragger.onTouchBlockImpl(mDragHandler, mTouchedView, me, false);
    }

    private void dragMove() {
        long time = mDragStartTime + 10L;
        MotionEvent me = MotionEvent.obtain(time, time, MotionEvent.ACTION_MOVE, 30, -10, 0);
        mDragger.onTouchBlockImpl(mDragHandler, mTouchedView, me, false);
        mDragger.getDragEventListener().onDrag(mWorkspaceView, mDragStartedEvent);
    }

    private void dragRelease() {
        // Pretend to be the last DragEvent that registers, which should be right by the
        // stationary block.
        // getX() returns float, even though we'll cast back to int immediately.
        when(mDragLocationEvent.getX()).thenReturn(mDragReleaseX);
        when(mDragLocationEvent.getY()).thenReturn(mDragReleaseY);

        mDragger.getDragEventListener().onDrag(mWorkspaceView, mDragLocationEvent);

        // Force the connector locations to update before the call to finishDragging().
        mDragGroup.updateAllConnectorLocations();
        mWorkspaceHelper.getView(mTargetBlock).updateConnectorLocations();

        mDragger.finishDragging();
    }
}
