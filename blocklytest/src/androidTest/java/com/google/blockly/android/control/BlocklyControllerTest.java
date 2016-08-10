/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.blockly.android.control;

import com.google.blockly.android.MockitoAndroidTestCase;
import com.google.blockly.android.R;
import com.google.blockly.android.testui.TestableBlockGroup;
import com.google.blockly.android.testui.TestableBlockViewFactory;
import com.google.blockly.android.ui.AbstractBlockView;
import com.google.blockly.android.ui.BlockGroup;
import com.google.blockly.android.ui.BlockView;
import com.google.blockly.android.ui.BlockViewFactory;
import com.google.blockly.android.ui.WorkspaceHelper;
import com.google.blockly.android.ui.WorkspaceView;
import com.google.blockly.model.Block;
import com.google.blockly.model.BlockFactory;
import com.google.blockly.model.BlockTestStrings;
import com.google.blockly.model.Connection;
import com.google.blockly.model.FieldVariable;
import com.google.blockly.model.Workspace;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for {@link BlocklyController}.
 */
public class BlocklyControllerTest extends MockitoAndroidTestCase {
    // Controller under test.
    BlocklyController mController;
    BlockFactory mBlockFactory;
    BlockViewFactory mViewFactory;
    Workspace mWorkspace;
    WorkspaceHelper mHelper;
    ConnectionManager mConnectionManager;
    WorkspaceView mWorkspaceView;

    List<BlocklyEvent> mEventsFired = new ArrayList<>();
    BlocklyController.EventsCallback mCallback = new BlocklyController.EventsCallback() {
        @Override
        public int getTypesBitmask() {
            return BlocklyEvent.TYPE_ALL;
        }

        @Override
        public void onEventGroup(List<BlocklyEvent> events) {
            mEventsFired.addAll(events);
        }
    };

    MockVariableCallback mVariableCallback = new MockVariableCallback();

    @Override
    public void setUp() throws Exception {
        super.setUp();

        mHelper = new WorkspaceHelper(getContext());
        mViewFactory = new TestableBlockViewFactory(getContext(), mHelper);
        mController = new BlocklyController.Builder(getContext())
                .setWorkspaceHelper(mHelper)
                .setBlockViewFactory(mViewFactory)
                .addBlockDefinitions(R.raw.test_blocks)
                .build();
        mController.addCallback(mCallback);
        mController.setVariableCallback(mVariableCallback);
        mBlockFactory = mController.getBlockFactory();
        mWorkspace = mController.getWorkspace();
        mConnectionManager = mController.getWorkspace().getConnectionManager();

        mWorkspaceView = new WorkspaceView(getContext());
    }

    public void testAddRootBlock() {
        assertTrue(mEventsFired.isEmpty());

        Block block = mBlockFactory.obtainBlock("simple_input_output", "connectTarget");
        mController.addRootBlock(block);

        assertTrue(mWorkspace.getRootBlocks().contains(block));
        assertEquals(mEventsFired.size(), 1);
        assertEquals(mEventsFired.get(0).getTypeId(), BlocklyEvent.TYPE_CREATE);
        assertEquals(mEventsFired.get(0).getBlockId(), block.getId());
    }

    public void testTrashRootBlock() {
        Block block = mBlockFactory.obtainBlock("simple_input_output", "connectTarget");
        mController.addRootBlock(block);

        mEventsFired.clear();
        mController.trashRootBlock(block);

        assertTrue(mWorkspace.getRootBlocks().isEmpty());
        assertEquals(mEventsFired.size(), 1);
        assertEquals(mEventsFired.get(0).getTypeId(), BlocklyEvent.TYPE_DELETE);
        assertEquals(mEventsFired.get(0).getBlockId(), block.getId());
    }

    public void testAddBlockFromTrash() {
        Block block = mBlockFactory.obtainBlock("simple_input_output", "connectTarget");
        mController.addRootBlock(block);
        mController.trashRootBlock(block);

        mEventsFired.clear();
        mController.addBlockFromTrash(block);

        assertTrue(mWorkspace.getRootBlocks().contains(block));
        assertEquals(mEventsFired.size(), 1);
        assertEquals(mEventsFired.get(0).getTypeId(), BlocklyEvent.TYPE_CREATE);
        assertEquals(mEventsFired.get(0).getBlockId(), block.getId());
    }

    public void testConnect_outputToInput_headless() {
        testConnect_outputToInput(false);
    }

    public void testConnect_outputToInput_withViews() {
        testConnect_outputToInput(true);
    }

    private void testConnect_outputToInput(boolean withViews) {
        // Setup
        Block target = mBlockFactory.obtainBlock("simple_input_output", "connectTarget");
        Block source = mBlockFactory.obtainBlock("simple_input_output", "connectSource");
        Connection targetConnection = target.getOnlyValueInput().getConnection();
        Connection sourceConnection = source.getOutputConnection();
        mController.addRootBlock(target);
        mController.addRootBlock(source);

        Block shadow = new Block.Builder(target).setUuid("connectShadow").setShadow(true).build();

        if (withViews) {
            mController.initWorkspaceView(mWorkspaceView);
            fakeOnAttachToWindow(target, source);

            // Validate initial view state.
            BlockView targetView = mHelper.getView(target);
            BlockView sourceView = mHelper.getView(source);
            assertNotNull(targetView);
            assertNotNull(sourceView);
            assertNotSame(mHelper.getRootBlockGroup(target),
                    mHelper.getRootBlockGroup(source));
        }

        // Perform test: connection source's output to target's input.
        mController.connect(sourceConnection, targetConnection);

        // Validate model changes.
        assertTrue(mWorkspace.isRootBlock(target));
        assertFalse(mWorkspace.isRootBlock(source));
        assertSame(target, sourceConnection.getTargetBlock());

        if (withViews) {
            // Validate view changes
            BlockGroup targetGroup = mHelper.getParentBlockGroup(target);
            assertSame(targetGroup, mHelper.getRootBlockGroup(target));
            assertSame(targetGroup, mHelper.getRootBlockGroup(source));
        }

        // Add the shadow connection and disconnect the block
        targetConnection.setShadowConnection(shadow.getOutputConnection());
        mController.extractBlockAsRoot(source);
        assertNull(source.getParentBlock());
        // Validate the block was replaced by the shadow
        assertEquals(targetConnection.getTargetBlock(), shadow);

        if (withViews) {
            // Check that the shadow block now has views
            assertNotNull(mHelper.getView(shadow));
            BlockGroup shadowGroup = mHelper.getParentBlockGroup(target);
            assertSame(shadowGroup, mHelper.getRootBlockGroup(shadow));
        }

        // Reattach the block and verify the shadow is hidden again
        mController.connect(sourceConnection, targetConnection);
        assertEquals(targetConnection.getTargetBlock(), source);
        assertNull(shadow.getOutputConnection().getTargetBlock());

        if (withViews) {
            assertNull(mHelper.getView(shadow));
        }
    }

    public void testConnect_outputToInputBumpNoInput_headless() {
        testConnect_outputToInputBumpNoInput(false);
    }

    public void testConnect_outputToInputBumpNoInput_withViews() {
        testConnect_outputToInputBumpNoInput(true);
    }

    private void testConnect_outputToInputBumpNoInput(boolean withViews) {
        // Setup
        Block target = mBlockFactory.obtainBlock("simple_input_output", "target");
        Block tail = mBlockFactory.obtainBlock("simple_input_output", "tail");
        Block source = mBlockFactory.obtainBlock("output_no_input", "source");

        // Connect the output of tail to the input of target.
        target.getOnlyValueInput().getConnection().connect(tail.getOutputConnection());

        mController.addRootBlock(target);
        mController.addRootBlock(source);
        if (withViews) {
            mController.initWorkspaceView(mWorkspaceView);
            fakeOnAttachToWindow(target, source);
        }

        // Validate preconditions
        assertEquals(2, mWorkspace.getRootBlocks().size());
        assertTrue(mWorkspace.isRootBlock(target));
        assertFalse(mWorkspace.isRootBlock(tail));
        assertTrue(mWorkspace.isRootBlock(source));

        // Perform test: Connect the source to where the tail is currently attached.
        mController.connect(
                source.getOutputConnection(), target.getOnlyValueInput().getConnection());

        // source is now a child of target, and tail is a new root block
        assertEquals(2, mWorkspace.getRootBlocks().size());
        assertTrue(mWorkspace.isRootBlock(target));
        assertFalse(mWorkspace.isRootBlock(source));
        assertTrue(mWorkspace.isRootBlock(tail));
        assertSame(target, target.getRootBlock());
        assertSame(target, source.getRootBlock());
        assertSame(tail, tail.getRootBlock());
        assertNull(tail.getOutputConnection().getTargetBlock());

        if (withViews) {
            BlockGroup targetGroup = mHelper.getParentBlockGroup(target);
            BlockGroup tailGroup = mHelper.getParentBlockGroup(tail);
            assertSame(targetGroup, mHelper.getRootBlockGroup(target));
            assertSame(targetGroup, mHelper.getRootBlockGroup(source));
            assertSame(tailGroup, mHelper.getRootBlockGroup(tail));
            assertNotSame(targetGroup, tailGroup);

            // Check that tail has been bumped far enough away.
            assertTrue(mHelper.getMaxSnapDistance() <=
                    tail.getOutputConnection().distanceFrom(source.getOutputConnection()));
        }
    }

    public void testConnect_outputToInputBumpMultipleInputs_headless() {
        testConnect_outputToInputBumpMultipleInputs(false);
    }

    public void testConnect_outputToInputBumpMultipleInputs_withViews() {
        testConnect_outputToInputBumpMultipleInputs(true);
    }

    private void testConnect_outputToInputBumpMultipleInputs(boolean withViews) {
        // Setup
        Block target = mBlockFactory.obtainBlock("simple_input_output", "target");
        Block tail = mBlockFactory.obtainBlock("simple_input_output", "tail");
        Block source = mBlockFactory.obtainBlock("multiple_input_output", "source");

        // Connect the output of tail to the input of target.
        tail.getOutputConnection().connect(target.getOnlyValueInput().getConnection());

        mController.addRootBlock(target);
        mController.addRootBlock(source);
        if (withViews) {
            mController.initWorkspaceView(mWorkspaceView);
            fakeOnAttachToWindow(target, source);
        }

        // Perform test: Connect the source to where the tail is currently attached.
        mController.connect(
                source.getOutputConnection(), target.getOnlyValueInput().getConnection());

        // Source is now a child of target
        assertTrue(mWorkspace.isRootBlock(target));
        assertFalse(mWorkspace.isRootBlock(source));
        assertSame(target, source.getOutputConnection().getTargetBlock());
        assertSame(target, source.getRootBlock());

        // Tail has been returned to the workspace root, bumped some distance.
        assertNull(tail.getOutputConnection().getTargetBlock());
        assertTrue(mWorkspace.isRootBlock(tail));

        if (withViews) {
            BlockGroup targetGroup = mHelper.getParentBlockGroup(target);
            BlockGroup tailGroup = mHelper.getParentBlockGroup(tail);

            targetGroup.updateAllConnectorLocations();
            tailGroup.updateAllConnectorLocations();

            assertSame(targetGroup, mHelper.getRootBlockGroup(target));
            assertSame(targetGroup, mHelper.getRootBlockGroup(source));
            assertSame(tailGroup, mHelper.getRootBlockGroup(tail));
            assertNotSame(targetGroup, tailGroup);
            assertTrue(mHelper.getMaxSnapDistance() <=
                    source.getOutputConnection().distanceFrom(tail.getOutputConnection()));
        }
    }

    public void testConnect_outputToInputShadowSplice_headless() {
        testConnect_outputToInputShadowSplice(false);
    }

    public void testConnect_outputToInputShadowSplice_withViews() {
        testConnect_outputToInputShadowSplice(true);
    }

    private void testConnect_outputToInputShadowSplice(boolean withViews) {
        // Setup
        Block target = mBlockFactory.obtainBlock("simple_input_output", "target");
        Block tail = mBlockFactory.obtainBlock("simple_input_output", "tail");
        Block source = mBlockFactory.obtainBlock("simple_input_output", "source");
        Block shadow = new Block.Builder(source).setShadow(true).setUuid("shadow").build();
        Connection sourceInputConnection = source.getOnlyValueInput().getConnection();

        // Connect the output of tail to the input of target.
        target.getOnlyValueInput().getConnection().connect(tail.getOutputConnection());
        // Add the shadow to the source
        sourceInputConnection.setShadowConnection(shadow.getOutputConnection());
        sourceInputConnection.connect(shadow.getOutputConnection());

        mController.addRootBlock(target);
        mController.addRootBlock(source);
        if (withViews) {
            mController.initWorkspaceView(mWorkspaceView);
            fakeOnAttachToWindow(target, source);
        }

        // Validate preconditions
        assertEquals(2, mWorkspace.getRootBlocks().size());
        assertTrue(mWorkspace.isRootBlock(target));
        assertFalse(mWorkspace.isRootBlock(tail));
        assertTrue(mWorkspace.isRootBlock(source));

        // Perform test: Connect the source to where the tail is currently attached.
        mController.connect(
                source.getOutputConnection(), target.getOnlyValueInput().getConnection());

        // source is now a child of target, and tail replaced the shadow
        assertEquals(1, mWorkspace.getRootBlocks().size());
        assertTrue(mWorkspace.isRootBlock(target));
        assertFalse(mWorkspace.isRootBlock(source));
        assertFalse(mWorkspace.isRootBlock(tail));
        assertSame(target, target.getRootBlock());
        assertSame(target, source.getRootBlock());
        assertSame(target, tail.getRootBlock());
        assertSame(source, tail.getParentBlock());

        if (withViews) {
            BlockGroup targetGroup = mHelper.getParentBlockGroup(target);
            assertSame(targetGroup, mHelper.getRootBlockGroup(target));
            assertSame(targetGroup, mHelper.getRootBlockGroup(source));
            assertSame(targetGroup, mHelper.getRootBlockGroup(tail));
            assertNull(mHelper.getView(shadow));
        }
    }

    public void testConnect_outputToInputSplice_headless() {
        testConnect_outputToInputSplice(false);
    }

    public void testConnect_outputToInputSplice_withViews() {
        testConnect_outputToInputSplice(true);
    }

    private void testConnect_outputToInputSplice(boolean withViews) {
        // Setup
        Block target = mBlockFactory.obtainBlock("simple_input_output", "target");
        Block tail = mBlockFactory.obtainBlock("multiple_input_output", "tail");
        Block source = mBlockFactory.obtainBlock("simple_input_output", "source");
        Block shadow = new Block.Builder(tail).setShadow(true).setUuid("shadow").build();

        // Add a hidden shadow to the target to ensure it has no effect.
        target.getOnlyValueInput().getConnection()
                .setShadowConnection(shadow.getOutputConnection());

        // Connect the output of tail to the input of source.
        tail.getOutputConnection().connect(target.getOnlyValueInput().getConnection());

        mController.addRootBlock(target);
        mController.addRootBlock(source);
        if (withViews) {
            mController.initWorkspaceView(mWorkspaceView);
            fakeOnAttachToWindow(target, source);
        }

        // Splice third between second and first.
        mController.connect(
                source.getOutputConnection(), target.getOnlyValueInput().getConnection());

        // Validate
        assertTrue(mWorkspace.isRootBlock(target));
        assertFalse(mWorkspace.isRootBlock(tail));
        assertFalse(mWorkspace.isRootBlock(source));
        assertSame(target, source.getOutputConnection().getTargetBlock());
        assertSame(source, tail.getOutputConnection().getTargetBlock());

        if (withViews) {
            BlockGroup targetGroup = mHelper.getParentBlockGroup(target);
            assertSame(targetGroup, mHelper.getRootBlockGroup(target));
            assertSame(targetGroup, mHelper.getRootBlockGroup(tail));
            assertSame(targetGroup, mHelper.getRootBlockGroup(source));
        }
    }

    public void testConnect_previousToNext_headless() {
        testConnect_previousToNext(false);
    }

    public void testConnect_previousToNext_withViews() {
        testConnect_previousToNext(true);
    }

    private void testConnect_previousToNext(boolean withViews) {
        // setup
        Block target = mBlockFactory.obtainBlock("statement_no_input", "target");
        Block source = mBlockFactory.obtainBlock("statement_no_input", "source");
        Block shadow = new Block.Builder(target).setUuid("connectShadow").setShadow(true).build();
        BlockView targetView = null, sourceView = null;

        mController.addRootBlock(target);
        mController.addRootBlock(source);
        if (withViews) {
            mController.initWorkspaceView(mWorkspaceView);
            fakeOnAttachToWindow(target, source);

            targetView = mHelper.getView(target);
            sourceView = mHelper.getView(source);

            assertNotNull(targetView);
            assertNotNull(sourceView);
        }

        // Connect source after target. No prior connection to bump or splice.
        mController.connect(source.getPreviousConnection(), target.getNextConnection());

        // Validate
        assertTrue(mWorkspace.isRootBlock(target));
        assertFalse(mWorkspace.isRootBlock(source));
        assertSame(target, source.getPreviousBlock());

        if (withViews) {
            BlockGroup rootGroup = mHelper.getRootBlockGroup(target);
            assertSame(rootGroup, mHelper.getParentBlockGroup(target));
            assertSame(rootGroup, mHelper.getParentBlockGroup(source));
            assertSame(targetView, rootGroup.getChildAt(0));
            assertSame(sourceView, rootGroup.getChildAt(1));
        }

        // Add the shadow to the target's next connection so the view will be created.
        target.getNextConnection().setShadowConnection(shadow.getPreviousConnection());
        mController.extractBlockAsRoot(source);

        assertTrue(mWorkspace.isRootBlock(source));
        assertSame(target.getNextBlock(), shadow);

        if (withViews) {
            BlockGroup rootGroup = mHelper.getRootBlockGroup(target);
            assertSame(rootGroup, mHelper.getParentBlockGroup(shadow));
            assertNotSame(rootGroup, mHelper.getParentBlockGroup(source));
            assertSame(mHelper.getView(shadow), rootGroup.getChildAt(1));
        }

        // Reattach the source and verify the shadow went away.
        mController.connect(source.getPreviousConnection(), target.getNextConnection());
        assertFalse(mWorkspace.isRootBlock(source));
        assertSame(target.getNextBlock(), source);
        assertNull(shadow.getPreviousBlock());

        if (withViews) {
            assertNull(mHelper.getView(shadow));
        }
    }

    public void testConnect_previousToNextSplice_headless() {
        testConnect_previousToNextSplice(false);
    }

    public void testConnect_previousToNextSplice_withViews() {
        testConnect_previousToNextSplice(true);
    }

    private void testConnect_previousToNextSplice(boolean withViews) {
        // setup
        Block target = mBlockFactory.obtainBlock("statement_no_input", "target");
        Block tail = mBlockFactory.obtainBlock("statement_no_input", "tail");
        Block source = mBlockFactory.obtainBlock("statement_no_input", "source");
        Block shadow = new Block.Builder("tail").setShadow(true).setUuid("shadow").build();
        BlockView targetView = null, tailView = null, sourceView = null;

        // Add a shadow to make sure it doesn't have any effects.
        target.getNextConnection().setShadowConnection(shadow.getPreviousConnection());
        tail.getPreviousConnection().connect(target.getNextConnection());

        mController.addRootBlock(target);
        mController.addRootBlock(source);
        if (withViews) {
            mController.initWorkspaceView(mWorkspaceView);
            fakeOnAttachToWindow(target, source);

            targetView = mHelper.getView(target);
            tailView = mHelper.getView(tail);
            sourceView = mHelper.getView(source);

            assertNotNull(targetView);
            assertNotNull(tailView);
            assertNotNull(sourceView);
        }

        // Connect source after target, where tail is currently attached, causing a splice.
        mController.connect(source.getPreviousConnection(), target.getNextConnection());

        assertSame(target, source.getPreviousBlock());
        assertSame(source, tail.getPreviousBlock());

        if (withViews) {
            BlockGroup rootGroup = mHelper.getRootBlockGroup(target);
            assertSame(rootGroup, mHelper.getParentBlockGroup(target));
            assertSame(rootGroup, mHelper.getParentBlockGroup(tail));
            assertSame(rootGroup, mHelper.getParentBlockGroup(source));
            assertSame(targetView, rootGroup.getChildAt(0));
            assertSame(sourceView, rootGroup.getChildAt(1));  // Spliced in between.
            assertSame(tailView, rootGroup.getChildAt(2));
        }
    }

    public void testConnect_previousToNextBumpRemainder_headless() {
        testConnect_previousToNextBumpRemainder(false);
    }

    public void testConnect_previousToNextBumpRemainder_withViews() {
        testConnect_previousToNextBumpRemainder(true);
    }

    private void testConnect_previousToNextBumpRemainder(boolean withViews) {
        // setup
        Block target = mBlockFactory.obtainBlock("statement_no_input", "target");
        Block tail1 = mBlockFactory.obtainBlock("statement_no_input", "tail1");
        Block tail2 = mBlockFactory.obtainBlock("statement_no_input", "tail2");
        Block source = mBlockFactory.obtainBlock("statement_no_next", "source");
        BlockView targetView = null, tailView1 = null, tailView2 = null, sourceView = null;

        // Create a sequence of target, tail1, and tail2.
        tail1.getPreviousConnection().connect(target.getNextConnection());
        tail2.getPreviousConnection().connect(tail1.getNextConnection());

        mController.addRootBlock(target);
        mController.addRootBlock(source);
        if (withViews) {
            mController.initWorkspaceView(mWorkspaceView);
            fakeOnAttachToWindow(target, source);

            targetView = mHelper.getView(target);
            tailView1 = mHelper.getView(tail1);
            tailView2 = mHelper.getView(tail2);
            sourceView = mHelper.getView(source);

            assertNotNull(targetView);
            assertNotNull(tailView1);
            assertNotNull(tailView2);
            assertNotNull(sourceView);
        }

        // Run test: Connect source after target, where tail is currently attached.
        // Since source does not have a next connection, bump the tail.
        mController.connect(source.getPreviousConnection(), target.getNextConnection());

        // Target and source are connected.
        assertTrue(mWorkspace.isRootBlock(target));
        assertSame(target, source.getPreviousBlock());

        // Tail has been returned to the workspace root.
        assertTrue(mWorkspace.isRootBlock(tail1));
        assertNull(tail1.getPreviousBlock());
        assertFalse(mWorkspace.isRootBlock(tail2));
        assertFalse(mWorkspace.isRootBlock(source));
        assertSame(tail1, tail1.getRootBlock());
        assertSame(tail1, tail2.getRootBlock());

        if (withViews) {
            BlockGroup targetRootGroup = mHelper.getRootBlockGroup(target);
            BlockGroup tailRootGroup = mHelper.getRootBlockGroup(tail1);
            assertSame(targetRootGroup, mHelper.getParentBlockGroup(target));
            assertSame(targetRootGroup, mHelper.getRootBlockGroup(source));
            assertNotSame(targetRootGroup, tailRootGroup);
            assertSame(tailRootGroup, mHelper.getRootBlockGroup(tail2));
            assertSame(targetView, targetRootGroup.getChildAt(0));
            assertSame(sourceView, targetRootGroup.getChildAt(1));
            assertSame(tailView1, tailRootGroup.getChildAt(0));
            assertSame(tailView2, tailRootGroup.getChildAt(1));

            // Check that tail has been bumped far enough away.
            assertTrue(mHelper.getMaxSnapDistance() <=
                    tail1.getPreviousConnection().distanceFrom(target.getNextConnection()));
        }
    }


    public void testConnect_previousToNextShadowSplice_headless() {
        testConnect_previousToNextShadowSplice(false);
    }

    public void testConnect_previousToNextShadowSplice_withViews() {
        testConnect_previousToNextShadowSplice(true);
    }

    private void testConnect_previousToNextShadowSplice(boolean withViews) {
        // setup
        Block target = mBlockFactory.obtainBlock("statement_no_input", "target");
        Block tail1 = mBlockFactory.obtainBlock("statement_no_input", "tail1");
        Block tail2 = mBlockFactory.obtainBlock("statement_no_input", "tail2");
        Block source = mBlockFactory.obtainBlock("statement_no_input", "source");
        Block shadowTail = new Block.Builder(tail1).setShadow(true).setUuid("shadowTail").build();
        BlockView targetView = null, tailView1 = null, tailView2 = null, sourceView = null;

        // Create a sequence of target, tail1, and tail2.
        tail1.getPreviousConnection().connect(target.getNextConnection());
        tail2.getPreviousConnection().connect(tail1.getNextConnection());
        source.getNextConnection().setShadowConnection(shadowTail.getPreviousConnection());
        source.getNextConnection().connect(shadowTail.getPreviousConnection());

        mController.addRootBlock(target);
        mController.addRootBlock(source);
        if (withViews) {
            mController.initWorkspaceView(mWorkspaceView);
            fakeOnAttachToWindow(target, source);

            targetView = mHelper.getView(target);
            tailView1 = mHelper.getView(tail1);
            tailView2 = mHelper.getView(tail2);
            sourceView = mHelper.getView(source);

            assertNotNull(targetView);
            assertNotNull(tailView1);
            assertNotNull(tailView2);
            assertNotNull(sourceView);
            assertNotNull(mHelper.getView(shadowTail));
        }

        // Run test: Connect source after target, where tail is currently attached.
        // Since source has a shadow connected to next, tail should replace it.
        mController.connect(source.getPreviousConnection(), target.getNextConnection());

        // Target and source are connected.
        assertTrue(mWorkspace.isRootBlock(target));
        assertSame(target, source.getPreviousBlock());

        // Tail has replaced the shadow.
        assertFalse(mWorkspace.isRootBlock(tail1));
        assertNull(shadowTail.getPreviousBlock());
        assertSame(source, tail1.getParentBlock());
        assertFalse(mWorkspace.isRootBlock(tail2));
        assertFalse(mWorkspace.isRootBlock(source));
        assertSame(target, tail1.getRootBlock());
        assertSame(target, tail2.getRootBlock());

        if (withViews) {
            BlockGroup targetRootGroup = mHelper.getRootBlockGroup(target);
            assertSame(targetRootGroup, mHelper.getParentBlockGroup(target));
            assertSame(targetRootGroup, mHelper.getRootBlockGroup(source));
            assertSame(targetRootGroup, mHelper.getRootBlockGroup(tail1));
            assertSame(targetRootGroup, mHelper.getRootBlockGroup(tail2));

            assertSame(targetView, targetRootGroup.getChildAt(0));
            assertSame(sourceView, targetRootGroup.getChildAt(1));
            assertSame(tailView1, targetRootGroup.getChildAt(2));
            assertSame(tailView2, targetRootGroup.getChildAt(3));

            assertNull(mHelper.getView(shadowTail));
        }
    }

    public void testConnect_previousToStatement_headless() {
        testConnect_previousToStatement(false);
    }

    public void testConnect_previousToStatement_withViews() {
        testConnect_previousToStatement(true);
    }

    private void testConnect_previousToStatement(boolean withViews) {
        // setup
        Block target = mBlockFactory.obtainBlock("statement_statement_input", "target");
        Block source = mBlockFactory.obtainBlock("statement_statement_input", "source");
        Block shadow = new Block.Builder(source).setShadow(true).setUuid("shadow").build();

        Connection statementConnection = target.getInputByName("statement input").getConnection();

        mController.addRootBlock(target);
        mController.addRootBlock(source);
        if (withViews) {
            mController.initWorkspaceView(mWorkspaceView);
            fakeOnAttachToWindow(target, source);
        }

        // Run test: Connect source inside target. No prior connection to bump.
        mController.connect(source.getPreviousConnection(), statementConnection);

        assertTrue(mWorkspace.isRootBlock(target));
        assertFalse(mWorkspace.isRootBlock(source));
        assertSame(target, source.getPreviousBlock());

        if (withViews) {
            BlockGroup rootGroup = mHelper.getRootBlockGroup(target);
            assertSame(rootGroup, mHelper.getParentBlockGroup(target));
            assertSame(rootGroup, mHelper.getRootBlockGroup(source));
        }

        // Add the shadow block
        statementConnection.setShadowConnection(shadow.getPreviousConnection());
        // disconnect the real blocks which should attach the shadow to replace it.
        mController.extractBlockAsRoot(source);

        assertTrue(mWorkspace.isRootBlock(source));
        assertFalse(mWorkspace.isRootBlock(shadow));
        assertSame(statementConnection.getTargetBlock(), shadow);

        if (withViews) {
            assertNotNull(mHelper.getView(shadow));
            assertSame(mHelper.getRootBlockGroup(target), mHelper.getRootBlockGroup(shadow));
        }

        // Reconnect the source and make sure the shadow goes away
        mController.connect(source.getPreviousConnection(), statementConnection);
        assertNull(shadow.getPreviousBlock());
        assertSame(statementConnection.getTargetBlock(), source);
        assertFalse(mWorkspace.isRootBlock(shadow));

        if (withViews) {
            assertNull(mHelper.getView(shadow));
            assertSame(mHelper.getRootBlockGroup(target), mHelper.getRootBlockGroup(source));
        }
    }

    public void testConnect_previousToStatementSpliceRemainder_headless() {
        testConnect_previousToStatementSpliceRemainder(false);
    }

    public void testConnect_previousToStatementSpliceRemainder_withViews() {
        testConnect_previousToStatementSpliceRemainder(true);
    }

    private void testConnect_previousToStatementSpliceRemainder(boolean withViews) {
        // setup
        Block target = mBlockFactory.obtainBlock("statement_statement_input", "target");
        Block tail = mBlockFactory.obtainBlock("statement_statement_input", "tail");
        Block source = mBlockFactory.obtainBlock("statement_statement_input", "source");
        Block shadow = new Block.Builder(tail).setShadow(true).setUuid("shadow").build();
        BlockView targetView = null, tailView = null, sourceView = null;

        Connection statementConnection =  target.getInputByName("statement input").getConnection();
        // and set a shadow to make sure it has no effects
        statementConnection.setShadowConnection(shadow.getPreviousConnection());
        // Connect the tail inside target.
        statementConnection.connect(tail.getPreviousConnection());

        mController.addRootBlock(target);
        mController.addRootBlock(source);
        if (withViews) {
            mController.initWorkspaceView(mWorkspaceView);
            fakeOnAttachToWindow(target, source);

            targetView = mHelper.getView(target);
            tailView = mHelper.getView(tail);
            sourceView = mHelper.getView(source);

            assertNotNull(targetView);
            assertNotNull(tailView);
            assertNotNull(sourceView);
        }

        // Run test: Connect source inside target, where tail is attached, resulting in a splice.
        mController.connect(source.getPreviousConnection(), statementConnection);

        // Validate result.
        assertTrue(mWorkspace.isRootBlock(target));
        assertFalse(mWorkspace.isRootBlock(tail));
        assertFalse(mWorkspace.isRootBlock(source));
        assertSame(target, source.getPreviousBlock());
        assertSame(source, tail.getPreviousBlock());

        if (withViews) {
            BlockGroup rootGroup = mHelper.getRootBlockGroup(target);
            BlockGroup secondGroup = mHelper.getParentBlockGroup(tail);
            assertSame(rootGroup, mHelper.getRootBlockGroup(tail));
            assertNotSame(rootGroup, secondGroup);
            assertSame(secondGroup, mHelper.getParentBlockGroup(source));
            assertSame(sourceView, secondGroup.getChildAt(0));
            assertSame(tailView, secondGroup.getChildAt(1));
        }
    }

    public void testConnect_previousToStatemenBumpRemainder_headless() {
        testConnect_previousToStatementBumpRemainder(false);
    }

    public void testConnect_previousToStatemenBumpRemainder_withViews() {
        testConnect_previousToStatementBumpRemainder(true);
    }

    private void testConnect_previousToStatementBumpRemainder(boolean withViews) {
        Block target = mBlockFactory.obtainBlock("statement_statement_input", "target");
        Block tail = mBlockFactory.obtainBlock("statement_statement_input", "tail");
        Block source = mBlockFactory.obtainBlock("statement_no_next", "source");
        BlockView sourceView = null;

        // Connect tail inside target.
        target.getInputByName("statement input").getConnection()
                .connect(tail.getPreviousConnection());

        mController.addRootBlock(target);
        mController.addRootBlock(source);
        if (withViews) {
            mController.initWorkspaceView(mWorkspaceView);
            fakeOnAttachToWindow(target, source);

            sourceView = mHelper.getView(source);
            assertNotNull(sourceView);
        }

        // Connect source inside target, where tail is attached.  Source does not have a next, so
        // this will bump tail back to the root.
        mController.connect(source.getPreviousConnection(),
                            target.getInputByName("statement input").getConnection());

        // Validate
        assertTrue(mWorkspace.isRootBlock(target));
        assertTrue(mWorkspace.isRootBlock(tail));
        assertFalse(mWorkspace.isRootBlock(source));
        assertSame(target.getInputByName("statement input").getConnection(),
                source.getPreviousConnection().getTargetConnection());
        assertNull(tail.getPreviousBlock());

        if (withViews) {
            BlockGroup targetRootGroup = mHelper.getRootBlockGroup(target);
            BlockGroup tailRootGroup = mHelper.getRootBlockGroup(tail);
            BlockGroup sourceGroup = mHelper.getParentBlockGroup(source);
            assertSame(targetRootGroup, mHelper.getParentBlockGroup(target));
            assertNotSame(targetRootGroup, tailRootGroup);
            assertSame(tailRootGroup, mHelper.getParentBlockGroup(tail));
            assertSame(targetRootGroup, mHelper.getRootBlockGroup(source));
            assertSame(sourceGroup.getParent(), target.getInputByName("statement input").getView());
            assertSame(sourceView, sourceGroup.getChildAt(0));
            assertTrue(mHelper.getMaxSnapDistance() <=
                    source.getPreviousConnection().distanceFrom(tail.getPreviousConnection()));
        }
    }

    public void testConnect_previousToStatementShadowSplice_headless() {
        testConnect_previousToStatementShadowSplice(false);
    }

    public void testConnect_previousToStatementShadowSplice_withViews() {
        testConnect_previousToStatementShadowSplice(true);
    }

    private void testConnect_previousToStatementShadowSplice(boolean withViews) {
        Block target = mBlockFactory.obtainBlock("statement_statement_input", "target");
        Block tail = mBlockFactory.obtainBlock("statement_statement_input", "tail");
        Block source = mBlockFactory.obtainBlock("statement_no_input", "source");
        Block shadow = new Block.Builder(source).setShadow(true).setUuid("shadow").build();
        BlockView sourceView = null;

        // Connect tail inside target.
        Connection statementConnection = target.getInputByName("statement input").getConnection();
        statementConnection.connect(tail.getPreviousConnection());

        // Add the shadow to the source
        source.getNextConnection().setShadowConnection(shadow.getPreviousConnection());
        source.getNextConnection().connect(shadow.getPreviousConnection());

        mController.addRootBlock(target);
        mController.addRootBlock(source);
        if (withViews) {
            mController.initWorkspaceView(mWorkspaceView);
            fakeOnAttachToWindow(target, source);

            sourceView = mHelper.getView(source);
            assertNotNull(sourceView);
            assertNotNull(mHelper.getView(shadow));
        }

        // Connect source inside target, where tail is attached.  Source has a shadow on next, so
        // tail should replace it.
        mController.connect(source.getPreviousConnection(), statementConnection);

        // Validate
        assertTrue(mWorkspace.isRootBlock(target));
        assertFalse(mWorkspace.isRootBlock(tail));
        assertFalse(mWorkspace.isRootBlock(source));
        assertSame(statementConnection, source.getPreviousConnection().getTargetConnection());
        assertSame(source, tail.getPreviousBlock());
        assertNull(shadow.getParentBlock());

        if (withViews) {
            BlockGroup targetRootGroup = mHelper.getRootBlockGroup(target);
            BlockGroup sourceGroup = mHelper.getParentBlockGroup(source);
            assertSame(targetRootGroup, mHelper.getParentBlockGroup(target));
            assertSame(targetRootGroup, mHelper.getRootBlockGroup(tail));
            assertSame(sourceGroup, mHelper.getParentBlockGroup(tail));
            assertSame(targetRootGroup, mHelper.getRootBlockGroup(source));
            assertSame(sourceGroup.getParent(), target.getInputByName("statement input").getView());
            assertSame(sourceView, sourceGroup.getChildAt(0));
            assertSame(mHelper.getView(tail), sourceGroup.getChildAt(1));

            assertNull(mHelper.getView(shadow));
        }

        // Make sure nothing breaks when we detach the tail and the shadow comes back
        mController.extractBlockAsRoot(tail);
    }

    public void testExtractAsRootBlock_alreadyRoot_headless() {
        testExtractAsRootBlock_alreadyRoot(false);
    }

    public void testExtractAsRootBlock_alreadyRoot_withViews() {
        testExtractAsRootBlock_alreadyRoot(true);
    }

    private void testExtractAsRootBlock_alreadyRoot(boolean withViews) {
        // Configure
        Block block = mBlockFactory.obtainBlock("statement_statement_input", "block");
        mController.addRootBlock(block);
        if (withViews) {
            mController.initWorkspaceView(mWorkspaceView);
            fakeOnAttachToWindow(block);
        }

        // Check Preconditions
        assertEquals(mWorkspace.getRootBlocks().size(), 1);
        assertTrue(mWorkspace.getRootBlocks().contains(block));

        // Run test: Make block a root (even though it already is).
        mController.extractBlockAsRoot(block);

        // Validate (no change)
        assertEquals(mWorkspace.getRootBlocks().size(), 1);
        assertTrue(mWorkspace.getRootBlocks().contains(block));

        if (withViews) {
            BlockGroup firstGroup = mHelper.getParentBlockGroup(block);
            assertSame(firstGroup, mHelper.getRootBlockGroup(block));
        }
    }

    public void testExtractBlockAsRoot_fromInput_headless() {
        testExtractBlockAsRoot_fromInput(false);
    }

    public void testExtractBlockAsRoot_fromInput_withViews() {
        testExtractBlockAsRoot_fromInput(true);
    }

    private void testExtractBlockAsRoot_fromInput(boolean withViews) {
        Block first = mBlockFactory.obtainBlock("simple_input_output", "first block");
        Block second = mBlockFactory.obtainBlock("simple_input_output", "second block");
        mController.connect(
                second.getOutputConnection(), first.getOnlyValueInput().getConnection());
        mController.addRootBlock(first);
        if (withViews) {
            mController.initWorkspaceView(mWorkspaceView);
            fakeOnAttachToWindow(first, second);
        }

        // Check preconditions
        List<Block> rootBlocks = mWorkspace.getRootBlocks();
        assertEquals(rootBlocks.size(), 1);
        assertEquals(rootBlocks.get(0), first);

        // Run test: Extract second out from under first.
        mController.extractBlockAsRoot(second);

        rootBlocks = mWorkspace.getRootBlocks();
        assertEquals(rootBlocks.size(), 2);
        assertTrue(rootBlocks.contains(first));
        assertTrue(rootBlocks.contains(second));
        assertFalse(first.getOnlyValueInput().getConnection().isConnected());
        assertFalse(second.getOutputConnection().isConnected());

        if (withViews) {
            BlockGroup firstGroup = mHelper.getParentBlockGroup(first);
            assertNotNull(firstGroup);
            BlockGroup secondGroup = mHelper.getParentBlockGroup(second);
            assertNotNull(secondGroup);
            assertNotSame(secondGroup, firstGroup);
        }
    }

    public void testExtractBlockAsRoot_fromNext_headless() {
        testExtractBlockAsRoot_fromNext(false);
    }

    public void testExtractBlockAsRoot_fromNext_withViews() {
        testExtractBlockAsRoot_fromNext(true);
    }

    private void testExtractBlockAsRoot_fromNext(boolean withViews) {
        // Configure
        Block first = mBlockFactory.obtainBlock("statement_statement_input", "first block");
        Block second = mBlockFactory.obtainBlock("statement_statement_input", "second block");
        mController.connect(second.getPreviousConnection(), first.getNextConnection());
        mController.addRootBlock(first);
        if (withViews) {
            mController.initWorkspaceView(mWorkspaceView);
            fakeOnAttachToWindow(first, second);
        }

        // Check preconditions
        List<Block> rootBlocks = mWorkspace.getRootBlocks();
        assertEquals(rootBlocks.size(), 1);
        assertEquals(rootBlocks.get(0), first);
        assertEquals(second, first.getNextConnection().getTargetBlock());
        if (withViews) {
            assertSame(mHelper.getParentBlockGroup(first),
                    mHelper.getParentBlockGroup(second));
        }

        // Run test: Extract second out from under first.
        mController.extractBlockAsRoot(second);

        // Validate
        rootBlocks = mWorkspace.getRootBlocks();
        assertEquals(rootBlocks.size(), 2);
        assertTrue(rootBlocks.contains(first));
        assertTrue(rootBlocks.contains(second));
        assertFalse(first.getNextConnection().isConnected());
        assertFalse(second.getPreviousConnection().isConnected());

        if (withViews) {
            BlockGroup firstGroup = mHelper.getParentBlockGroup(first);
            assertNotNull(firstGroup);
            BlockGroup secondGroup = mHelper.getParentBlockGroup(second);
            assertNotNull(secondGroup);
            assertNotSame(secondGroup, firstGroup);
        }
    }

    public void testVariableCallback_onCreate() {
        NameManager.VariableNameManager nameManager =
                (NameManager.VariableNameManager) mController.getWorkspace()
                        .getVariableNameManager();

        assertNull(mVariableCallback.onCreateVariable);

        // Calling requestAddVariable and the callback blocking creation
        mVariableCallback.whenOnCreateCalled = false;
        mController.requestAddVariable("var1");
        assertEquals("var1", mVariableCallback.onCreateVariable);
        assertEquals(0, nameManager.getUsedNames().size());

        // Calling addVariable bypasses the callback
        mVariableCallback.onCreateVariable = null;
        mController.addVariable("var1");
        assertNull(mVariableCallback.onCreateVariable);
        assertTrue(nameManager.contains("var1"));

        // Calling requestAddVariable and the callback allows creation
        mVariableCallback.reset();
        mVariableCallback.whenOnCreateCalled = true;
        mController.requestAddVariable("var2");
        assertEquals("var2", mVariableCallback.onCreateVariable);
        assertTrue(nameManager.contains("var2"));
    }

    public void testVariableCallback_onRename() {
        NameManager.VariableNameManager nameManager =
                (NameManager.VariableNameManager) mController.getWorkspace()
                        .getVariableNameManager();
        mController.addVariable("var1");
        mController.addVariable("var2");

        // Calling rename without forcing and the callback blocks it
        mVariableCallback.whenOnRenameCalled = false;
        mController.requestRenameVariable("var1", "var3");
        assertEquals("var1", mVariableCallback.onRenameVariable);
        assertFalse(nameManager.contains("var3"));
        assertTrue(nameManager.contains("var1"));

        // Calling rename with forcing skips the callback
        mVariableCallback.onRenameVariable = null;
        mController.renameVariable("var1", "var3");
        assertNull(mVariableCallback.onRenameVariable);
        assertTrue(nameManager.contains("var3"));
        assertFalse(nameManager.contains("var1"));

        // Calling rename without forcing and the callback allows it
        mVariableCallback.whenOnRenameCalled = true;
        mController.requestRenameVariable("var2", "var4");
        assertEquals("var2", mVariableCallback.onRenameVariable);
        assertTrue(nameManager.contains("var4"));
        assertFalse(nameManager.contains("var2"));

        // Verify that we have two variables still
        assertEquals(2, nameManager.size());
    }

    public void testVariableCallback_onRemove() {
        NameManager.VariableNameManager nameManager =
                (NameManager.VariableNameManager) mController.getWorkspace()
                        .getVariableNameManager();
        mController.addVariable("var3");
        mController.addVariable("var4");

        // Calling delete without forcing and the callback blocks it
        mVariableCallback.whenOnDeleteCalled = false;
        mController.requestDeleteVariable("var3");
        assertEquals("var3", mVariableCallback.onDeleteVariable);
        assertTrue(nameManager.contains("var3"));

        // Calling delete with forcing skips callback
        mVariableCallback.onDeleteVariable = null;
        mController.deleteVariable("var3");
        assertNull(mVariableCallback.onDeleteVariable);
        assertFalse(nameManager.contains("var3"));

        // Calling delete without forcing and callback allows it
        mVariableCallback.whenOnDeleteCalled = true;
        mController.requestDeleteVariable("var4");
        assertEquals("var4", mVariableCallback.onDeleteVariable);
        assertFalse(nameManager.contains("var4"));

        // Verify that we have no variables left
        assertEquals(0, nameManager.size());
    }

    public void testRemoveVariable() {
        mController.addVariable("var1");
        mController.addVariable("var2");
        mController.addVariable("var3");
        mController.addVariable("var4");

        Block set1 = mBlockFactory.obtainBlock("set_variable", "first block");
        Block set2 = mBlockFactory.obtainBlock("set_variable", "second block");
        Block set3 = mBlockFactory.obtainBlock("set_variable", "third block");
        Block set4 = mBlockFactory.obtainBlock("set_variable", "fourth block");
        Block set5 = mBlockFactory.obtainBlock("set_variable", "fifth block");
        Block set6 = mBlockFactory.obtainBlock("set_variable", "sixth block");
        Block statement = mBlockFactory.obtainBlock("statement_statement_input", "statement block");
        Block get1 = mBlockFactory.obtainBlock("get_variable", "get1");
        Block get2 = mBlockFactory.obtainBlock("get_variable", "get2");
        Block get3 = mBlockFactory.obtainBlock("get_variable", "get3");

        mController.connect(statement.getInputs().get(0).getConnection(),
                set1.getPreviousConnection());
        mController.connect(set1.getNextConnection(), set2.getPreviousConnection());
        mController.connect(set2.getNextConnection(), set3.getPreviousConnection());
        mController.connect(set3.getNextConnection(), set4.getPreviousConnection());
        mController.connect(set4.getNextConnection(), set5.getPreviousConnection());
        mController.connect(set2.getOnlyValueInput().getConnection(), get1.getOutputConnection());
        mController.connect(set5.getOnlyValueInput().getConnection(), get2.getOutputConnection());

        FieldVariable var = (FieldVariable) set1.getFieldByName("variable");
        var.setVariable("var1");
        var = (FieldVariable) set2.getFieldByName("variable");
        var.setVariable("var2");
        var = (FieldVariable) set3.getFieldByName("variable");
        var.setVariable("var1");
        var = (FieldVariable) set4.getFieldByName("variable");
        var.setVariable("var3");
        var = (FieldVariable) set5.getFieldByName("variable");
        var.setVariable("var1");
        var = (FieldVariable) set6.getFieldByName("variable");
        var.setVariable("var1");
        var = (FieldVariable) get1.getFieldByName("variable");
        var.setVariable("var1");
        var = (FieldVariable) get2.getFieldByName("variable");
        var.setVariable("var4");
        var = (FieldVariable) get3.getFieldByName("variable");
        var.setVariable("var1");

        mController.addRootBlock(statement);
        mController.addRootBlock(set6);
        mController.addRootBlock(get3);

        // Workspace setup:
        // Statement block with a statement input
        //     set1 "var1"
        //     set2 "var2" <- get1 "var1"
        //     set3 "var1"
        //     set4 "var3"
        //     set5 "var1" <- get2 "var4"
        //
        // set6 "var1"
        //
        // get3 "var1"

        // Expected state after deleting var1:
        // Statement block with a statement input
        //     set2 "var2"
        //     set4 "var 3"
        List<Block> rootBlocks = mController.getWorkspace().getRootBlocks();
        assertEquals(3, rootBlocks.size());

        mVariableCallback.whenOnDeleteCalled = true;
        mVariableCallback.onDeleteVariable = null;
        mController.requestDeleteVariable("var1");

        assertEquals("var1", mVariableCallback.onDeleteVariable);
        assertEquals(1, rootBlocks.size());

        Block block = rootBlocks.get(0);
        assertSame(block, statement);
        block = block.getInputs().get(0).getConnectedBlock();
        assertSame(block, set2);

        block = block.getNextBlock();
        assertSame(block, set4);

        block = block.getNextBlock();
        assertNull(block);
    }

    public void testLoadWorkspaceContents_andReset() {
        mController.initWorkspaceView(mWorkspaceView);
        assertEquals(0, mWorkspace.getRootBlocks().size());
        assertEquals(0, mWorkspaceView.getChildCount());

        mController.loadWorkspaceContents(
                BlockTestStrings.EMPTY_BLOCK_WITH_POSITION +
                BlockTestStrings.EMPTY_BLOCK_WITH_POSITION.replace(
                        BlockTestStrings.EMPTY_BLOCK_ID,
                        BlockTestStrings.EMPTY_BLOCK_ID + '2'));
        assertEquals(2, mWorkspace.getRootBlocks().size());
        assertEquals(2, mWorkspaceView.getChildCount());

        mController.resetWorkspace();
        assertEquals(0, mWorkspace.getRootBlocks().size());
        assertEquals(0, mWorkspaceView.getChildCount());
    }

    /**
     * Sets the {@link WorkspaceView}, which is the main effect of calling
     * {@link AbstractBlockView#onAttachedToWindow()}.
     *
     * @param blocks Blocks in the views tree for which to assign the {@link WorkspaceView}.
     */
    private void fakeOnAttachToWindow(Block... blocks) {
        for (int i = 0; i < blocks.length; ++i) {
            ((TestableBlockGroup) mHelper.getRootBlockGroup(blocks[i]))
                    .setWorkspaceView(mWorkspaceView);
        }
    }

    private static class MockVariableCallback extends BlocklyController.VariableCallback {
        String onDeleteVariable = null;
        String onCreateVariable = null;
        String onRenameVariable = null;

        boolean whenOnDeleteCalled = true;
        boolean whenOnCreateCalled = true;
        boolean whenOnRenameCalled = true;

        @Override
        public boolean onDeleteVariable(String variable) {
            onDeleteVariable = variable;
            return whenOnDeleteCalled;
        }

        @Override
        public boolean onCreateVariable(String varName) {
            onCreateVariable = varName;
            return whenOnCreateCalled;
        }

        @Override
        public boolean onRenameVariable(String variable, String newVariable) {
            onRenameVariable = variable;
            return whenOnRenameCalled;
        }

        public void reset() {
            onDeleteVariable = null;
            onCreateVariable = null;
            onRenameVariable = null;
            whenOnDeleteCalled = true;
            whenOnCreateCalled = true;
            whenOnRenameCalled = true;
        }
    }
}
