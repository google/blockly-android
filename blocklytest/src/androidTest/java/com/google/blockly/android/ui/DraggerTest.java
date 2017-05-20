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

import android.content.ClipDescription;
import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Pair;
import android.view.DragEvent;
import android.view.MotionEvent;

import com.google.blockly.android.BlocklyTestCase;
import com.google.blockly.android.clipboard.BlockClipDataHelper;
import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.android.control.ConnectionManager;
import com.google.blockly.android.ui.vertical.VerticalBlockViewFactory;
import com.google.blockly.model.Block;
import com.google.blockly.model.BlockFactory;
import com.google.blockly.model.BlockTemplate;
import com.google.blockly.model.Connection;
import com.google.blockly.model.Workspace;
import com.google.blockly.utils.BlockLoadingException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link Dragger}.
 */
public class DraggerTest extends BlocklyTestCase {
    private Context mMockContext;
    private BlocklyController mMockController;
    private BlockClipDataHelper mMockBlockClipDataHelper;
    private Workspace mMockWorkspace;
    private ConnectionManager mMockConnectionManager;

    private DragEvent mDragStartedEvent;
    private DragEvent mDragLocationEvent;
    private DragEvent mDropEvent;

    private ViewPoint mTempViewPoint = new ViewPoint();
    private WorkspaceHelper mWorkspaceHelper;
    private BlockViewFactory mViewFactory;
    private WorkspaceView mWorkspaceView;
    private Dragger mDragger;
    private BlockTouchHandler mTouchHandler;
    private BlockFactory mBlockFactory;

    // Drag gesture state variables
    private Block mTouchedBlock;
    private Block mDraggedBlock;
    private Block mTargetBlock;
    private BlockView mTouchedView;

    Dragger.DragHandler mDragHandler = new Dragger.DragHandler() {
        @Nullable
        @Override
        public Runnable maybeGetDragGroupCreator(PendingDrag pendingDrag) {
            mPendingDrag = pendingDrag;
            return mDragGroupCreator;
        }

        @Override
        public boolean onBlockClicked(PendingDrag pendingDrag) {
            return false;  // not yet tests
        }
    };

    int mDragGroupCreatorCallCount = 0;
    final CountDownLatch mDragGroupCreatorLatch = new CountDownLatch(1);
    final Runnable mDragGroupCreator = new Runnable() {
        @Override
        public void run() {
            ++mDragGroupCreatorCallCount;
            mPendingDrag.startDrag(mWorkspaceView, mDragGroup, new ViewPoint());
            mDragGroupCreatorLatch.countDown();
        }
    };

    PendingDrag mPendingDrag;
    BlockGroup mDragGroup;
    long mDragStartTime;
    float mDragReleaseX, mDragReleaseY;

    @Before
    public void setUp() throws Exception {
        configureForThemes();
        configureForUIThread();

        mMockContext = mock(Context.class, AdditionalAnswers.delegatesTo(getContext()));
        doReturn(mTargetMainLooper).when(mMockContext).getMainLooper();

        mMockController = mock(BlocklyController.class);
        mMockBlockClipDataHelper = mock(BlockClipDataHelper.class);
        mMockWorkspace = mock(Workspace.class);
        mMockConnectionManager = mock(ConnectionManager.class);
        mDragStartedEvent = mock(DragEvent.class);
        mDragLocationEvent = mock(DragEvent.class);
        mDropEvent = mock(DragEvent.class);
        
        runAndSync(new Runnable() {
            @Override
            public void run() {
                mBlockFactory = new BlockFactory();

                try {
                    mBlockFactory.addJsonDefinitions(mMockContext.getAssets()
                            .open("default/test_blocks.json"));
                } catch (IOException | BlockLoadingException e) {
                    throw new RuntimeException(e);
                }
                mBlockFactory.setController(mMockController);
                mWorkspaceView = new WorkspaceView(mMockContext);
                mWorkspaceHelper = new WorkspaceHelper(mMockContext);
                mWorkspaceHelper.setWorkspaceView(mWorkspaceView);
                mViewFactory = new VerticalBlockViewFactory(mMockContext, mWorkspaceHelper);

                // The following are queried by the Dragger.
                stub(mMockWorkspace.getConnectionManager()).toReturn(mMockConnectionManager);
                stub(mMockController.getBlockFactory()).toReturn(mBlockFactory);
                stub(mMockController.getWorkspace()).toReturn(mMockWorkspace);
                stub(mMockController.getWorkspaceHelper()).toReturn(mWorkspaceHelper);
                stub(mMockController.getContext()).toReturn(mMockContext);
                stub(mMockController.getClipDataHelper()).toReturn(mMockBlockClipDataHelper);

                Mockito.when(mMockBlockClipDataHelper.getPendingDrag(any(DragEvent.class)))
                        .then(new Answer<PendingDrag>() {
                            @Override
                            public PendingDrag answer(InvocationOnMock invocationOnMock)
                                    throws Throwable
                            {
                                return mPendingDrag;
                            }
                        });

                mDragger = new Dragger(mMockController);
                mDragger.setWorkspaceView(mWorkspaceView);
                mTouchHandler = mDragger.buildSloppyBlockTouchHandler(mDragHandler);

                // Since we can't create DragEvents...
                when(mDragStartedEvent.getAction()).thenReturn(DragEvent.ACTION_DRAG_STARTED);
                when(mDragLocationEvent.getAction()).thenReturn(DragEvent.ACTION_DRAG_LOCATION);
                when(mDropEvent.getAction()).thenReturn(DragEvent.ACTION_DROP);
            }
        });

    }

    // This set of tests covers the full sequence of dragging operations.
    // Because we're skipping layout, the connector locations will never be exactly perfect.
    // Calling updateConnectorLocations puts all of the connections on a block at the
    // workspace position of that block.

    /** Drag events are not always block related.  Ignore other blocks. */
    @Test
    public void testIgnoreDragThatIsntBlocks() throws BlockLoadingException {
        // Setup
        mTouchedBlock = mDraggedBlock = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("simple_input_output"));
        mTargetBlock = mBlockFactory.obtainBlockFrom(new BlockTemplate().ofType("output_no_input"));

        Mockito.when(mMockBlockClipDataHelper.isBlockData(any(ClipDescription.class)))
                .thenReturn(false);

        setupDrag();
        dragBlockToTarget();

        Mockito.verify(mMockConnectionManager, never())
                .findBestConnection(Matchers.same(mTouchedBlock), anyInt());
        Mockito.verify(mMockController, never())
                .connect(any(Connection.class), any(Connection.class));
    }

    /** Drag together two compatible blocks. */
    @Test
    public void testDragConnect() throws BlockLoadingException {
        // Setup
        mTouchedBlock = mDraggedBlock = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("simple_input_output"));
        mTargetBlock = mBlockFactory.obtainBlockFrom(new BlockTemplate().ofType("output_no_input"));

        Mockito.when(mMockBlockClipDataHelper.isBlockData(any(ClipDescription.class)))
                .thenReturn(true);
        Mockito.when(
                mMockConnectionManager.findBestConnection(Matchers.same(mTouchedBlock), anyInt()))
                .thenReturn(Pair.create(mTouchedBlock.getOnlyValueInput().getConnection(),
                        mTargetBlock.getOutputConnection()));

        setupDrag();
        dragBlockToTarget();

        Mockito.verify(mMockConnectionManager, atLeastOnce())
                .findBestConnection(Matchers.same(mTouchedBlock), anyInt());
        Mockito.verify(mMockController).connect(
                mTouchedBlock.getOnlyValueInput().getConnection(),
                mTargetBlock.getOutputConnection());
    }

    /** Drag together two incompatible blocks. */
    @Test
    public void testDragNoConnect() throws BlockLoadingException {
        // Setup
        mTouchedBlock = mDraggedBlock = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("simple_input_output"));
        mTargetBlock = mBlockFactory.obtainBlockFrom(new BlockTemplate().ofType("output_no_input"));

        Mockito.when(mMockBlockClipDataHelper.isBlockData(any(ClipDescription.class)))
                .thenReturn(true);
        Mockito.when(mMockConnectionManager.findBestConnection(any(Block.class), anyInt()))
                .thenReturn(null);

        setupDrag();
        dragBlockToTarget();

        Mockito.verify(mMockConnectionManager, atLeastOnce())
                .findBestConnection(Matchers.same(mTouchedBlock), anyInt());
        Mockito.verify(mMockController, never())
                .connect(any(Connection.class), any(Connection.class));
    }

    @Test
    public void testRemoveConnectionsDuringDrag() throws BlockLoadingException {
        // Setup
        mTargetBlock = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("simple_input_output"));
        mTouchedBlock = mDraggedBlock = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("simple_input_output"));
        Block draggedChild = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("multiple_input_output"));
        mDraggedBlock.getOnlyValueInput().getConnection().connect(
                draggedChild.getOutputConnection());

        Mockito.when(mMockBlockClipDataHelper.isBlockData(any(ClipDescription.class)))
                .thenReturn(true);

        ArrayList<Connection> draggedConnections = new ArrayList<>();
        mDraggedBlock.getAllConnectionsRecursive(draggedConnections);
        assertThat(draggedConnections.size()).isEqualTo(5);

        final ArrayList<Connection> removedConnections = new ArrayList<>();
        final ArrayList<Connection> addedConnections = new ArrayList<>();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                removedConnections.add((Connection) invocationOnMock.getArguments()[0]);
                return null;
            }
        }).when(mMockConnectionManager).removeConnection(any(Connection.class));
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                addedConnections.add((Connection) invocationOnMock.getArguments()[0]);
                return null;
            }
        }).when(mMockConnectionManager).addConnection(any(Connection.class));

        setupDrag();
        dragTouch();

        assertThat(addedConnections.size()).isEqualTo(0);
        assertThat(removedConnections.size()).isEqualTo(0);

        dragMove();

        assertThat(removedConnections.size()).isEqualTo(draggedConnections.size());
        assertThat(addedConnections.size()).isEqualTo(0);
        for (Connection conn : draggedConnections) {
            assertThat(removedConnections.contains(conn)).isTrue();
            assertThat(conn.inDragMode()).isTrue();
        }

        // Complete the drag.
        dragRelease();

        assertThat(removedConnections.size()).isEqualTo(draggedConnections.size());
        assertThat(addedConnections.size()).isEqualTo(draggedConnections.size());
        for (Connection conn : draggedConnections) {
            assertThat(addedConnections.contains(conn)).isTrue();
            assertThat(conn.inDragMode()).isFalse();
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


        stub(mMockWorkspace.isRootBlock(mDraggedBlock)).toReturn(true);

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
        runAndSync(new Runnable() {
            @Override
            public void run() {
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
                mDragger.onTouchBlockImpl(
                        Dragger.DRAG_MODE_SLOPPY, mDragHandler, mTouchedView, me, false);
            }
        });
    }

    private void dragMove() {
        runAndSync(new Runnable() {
            @Override
            public void run() {
                long time = mDragStartTime + 10L;
                MotionEvent me =
                        MotionEvent.obtain(time, time, MotionEvent.ACTION_MOVE, 30, -10, 0);
                assertWithMessage("Events that initiate drags must be claimed")
                        .that(mDragger.onTouchBlockImpl(
                                Dragger.DRAG_MODE_SLOPPY, mDragHandler, mTouchedView, me, false))
                        .isTrue();
            }
        });

        // Allow mDragGroupCreator to run.
        awaitTimeout(mDragGroupCreatorLatch);
        assertWithMessage("Dragger must call Runnable to construct draggable BlockGroup")
                .that(mDragGroupCreatorCallCount).isEqualTo(1);

        runAndSync(new Runnable() {
            @Override
            public void run() {
                mDragger.getDragEventListener().onDrag(mWorkspaceView, mDragStartedEvent);
            }
        });

    }

    private void dragRelease() {
        runAndSync(new Runnable() {
            @Override
            public void run() {
                // Mock the last drag location event, which should be right by the stationary block.
                // getX() returns float, even though we'll cast back to int immediately.
                when(mDragLocationEvent.getX()).thenReturn(mDragReleaseX);
                when(mDragLocationEvent.getY()).thenReturn(mDragReleaseY);

                mDragger.getDragEventListener().onDrag(mWorkspaceView, mDragLocationEvent);

                // Force the connector locations to update before the call to finishDragging().
                mDragGroup.updateAllConnectorLocations();
                mWorkspaceHelper.getView(mTargetBlock).updateConnectorLocations();

                // Mock the drop event, which is in the same place.
                when(mDropEvent.getX()).thenReturn(mDragReleaseX);
                when(mDropEvent.getY()).thenReturn(mDragReleaseY);

                mDragger.getDragEventListener().onDrag(mWorkspaceView, mDropEvent);
            }
        });
    }
}
