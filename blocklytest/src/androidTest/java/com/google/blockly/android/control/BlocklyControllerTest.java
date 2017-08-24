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

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import com.google.blockly.android.BlocklyTestCase;
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
import com.google.blockly.model.BlockTemplate;
import com.google.blockly.model.BlockTestStrings;
import com.google.blockly.model.BlocklyEvent;
import com.google.blockly.model.Connection;
import com.google.blockly.model.FieldVariable;
import com.google.blockly.model.VariableInfo;
import com.google.blockly.model.Workspace;
import com.google.blockly.utils.BlockLoadingException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.AdditionalAnswers;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link BlocklyController}.
 */
public class BlocklyControllerTest extends BlocklyTestCase {
    private Context mMockContext;

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

    StubVariableCallback mVariableCallback = new StubVariableCallback();

    @Before
    public void setUp() throws Exception {
        configureForThemes();
        configureForUIThread();

        mMockContext = mock(Context.class, AdditionalAnswers.delegatesTo(getContext()));
        doReturn(InstrumentationRegistry.getTargetContext().getMainLooper())
                .when(mMockContext).getMainLooper();

        mHelper = new WorkspaceHelper(mMockContext);
        mViewFactory = new TestableBlockViewFactory(mMockContext, mHelper);
        mController = new BlocklyController.Builder(mMockContext)
                .setWorkspaceHelper(mHelper)
                .setBlockViewFactory(mViewFactory)
                .addBlockDefinitionsFromAsset("default/test_blocks.json")
                .build();
        mController.addCallback(mCallback);
        mController.setVariableCallback(mVariableCallback);
        mBlockFactory = mController.getBlockFactory();
        mWorkspace = mController.getWorkspace();
        mConnectionManager = mController.getWorkspace().getConnectionManager();

        mWorkspaceView = new WorkspaceView(getContext());
    }

    @Test
    public void testAddRootBlock() throws BlockLoadingException {
        assertThat(mEventsFired.isEmpty()).isTrue();

        final Block block = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("simple_input_output").withId("connectTarget"));

        runAndSync(new Runnable() {
            @Override
            public void run() {
                mController.addRootBlock(block);

                assertThat(mWorkspace.getRootBlocks().contains(block)).isTrue();
                assertThat(1).isEqualTo(mEventsFired.size());
                assertThat(BlocklyEvent.TYPE_CREATE).isEqualTo(mEventsFired.get(0).getTypeId());
                assertThat(block.getId()).isEqualTo(mEventsFired.get(0).getBlockId());
            }
        });
    }

    @Test
    public void testTrashRootBlock() throws BlockLoadingException {
        final Block block = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("simple_input_output").withId("connectTarget"));

        runAndSync(new Runnable() {
            @Override
            public void run() {
                mController.addRootBlock(block);

                mEventsFired.clear();
                assertThat(mController.trashRootBlock(block)).isTrue();

                assertThat(mWorkspace.getRootBlocks().isEmpty()).isTrue();
                assertThat(mEventsFired.size()).isEqualTo(1);
                assertThat(mEventsFired.get(0).getTypeId()).isEqualTo(BlocklyEvent.TYPE_DELETE);
                assertThat(mEventsFired.get(0).getBlockId()).isEqualTo(block.getId());
                assertThat(block.getEventWorkspaceId()).isEqualTo(BlocklyEvent.WORKSPACE_ID_TRASH);
            }
        });
    }

    @Test
    public void testTrashRootBlockNotDeletable() throws BlockLoadingException {
        final Block block = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("simple_input_output").withId("connectTarget"));

        runAndSync(new Runnable() {
            @Override
            public void run() {
                block.setDeletable(false);
                mController.addRootBlock(block);

                mEventsFired.clear();
                assertThat(mController.trashRootBlock(block)).isFalse();

                assertThat(mWorkspace.getRootBlocks().size()).isEqualTo(1);  // Still there!
                assertThat(mEventsFired.size()).isEqualTo(0);
            }
        });
    }

    @Test
    public void testTrashRootBlockIgnoringDeletable() throws BlockLoadingException {
        final Block block = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("simple_input_output").withId("connectTarget"));

        runAndSync(new Runnable() {
            @Override
            public void run() {
                block.setDeletable(false);
                mController.addRootBlock(block);

                mEventsFired.clear();
                assertThat(mController.trashRootBlockIgnoringDeletable(block)).isTrue();

                assertThat(mWorkspace.getRootBlocks().isEmpty()).isTrue();
                assertThat(mEventsFired.size()).isEqualTo(1);
                assertThat(mEventsFired.get(0).getTypeId()).isEqualTo(BlocklyEvent.TYPE_DELETE);
                assertThat(mEventsFired.get(0).getBlockId()).isEqualTo(block.getId());
            }
        });
    }

    @Test
    public void testAddBlockFromTrash() throws BlockLoadingException {
        final Block block = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("simple_input_output").withId("connectTarget"));

        runAndSync(new Runnable() {
            @Override
            public void run() {
                mController.addRootBlock(block);
                mController.trashRootBlock(block);

                mEventsFired.clear();
                mController.addBlockFromTrash(block);

                assertThat(mWorkspace.getRootBlocks().contains(block)).isTrue();
                assertThat(1).isEqualTo(mEventsFired.size());
                assertThat(BlocklyEvent.TYPE_CREATE).isEqualTo(mEventsFired.get(0).getTypeId());
                assertThat(block.getId()).isEqualTo(mEventsFired.get(0).getBlockId());
                assertThat(block.getEventWorkspaceId()).isEqualTo(mWorkspace.getId());
            }
        });
    }

    @Test
    public void testConnect_outputToInput_headless() throws BlockLoadingException {
        testConnect_outputToInput(false);
    }

    @Test
    public void testConnect_outputToInput_withViews() throws BlockLoadingException {
        testConnect_outputToInput(true);
    }

    private void testConnect_outputToInput(final boolean withViews) throws BlockLoadingException {
        // Setup
        final Block target = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("simple_input_output").withId("connectTarget"));
        final Block source = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("simple_input_output").withId("connectSource"));
        final Block shadow = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().shadow().copyOf(target).withId("connectShadow"));
        final Connection targetConnection = target.getOnlyValueInput().getConnection();
        final Connection sourceConnection = source.getOutputConnection();

        runAndSync(new Runnable() {
            @Override
            public void run() {
                mController.addRootBlock(target);
                mController.addRootBlock(source);

                if (withViews) {
                    mController.initWorkspaceView(mWorkspaceView);
                    fakeOnAttachToWindow(target, source);

                    // Validate initial view state.
                    BlockView targetView = mHelper.getView(target);
                    BlockView sourceView = mHelper.getView(source);
                    assertThat(targetView).isNotNull();
                    assertThat(sourceView).isNotNull();
                    assertThat(mHelper.getRootBlockGroup(target))
                            .isNotSameAs(mHelper.getRootBlockGroup(source));
                }

                // Perform test: connection source's output to target's input.
                mController.connect(sourceConnection, targetConnection);

                // Validate model changes.
                assertThat(mWorkspace.isRootBlock(target)).isTrue();
                assertThat(mWorkspace.isRootBlock(source)).isFalse();
                assertThat(target).isSameAs(sourceConnection.getTargetBlock());

                if (withViews) {
                    // Validate view changes
                    BlockGroup targetGroup = mHelper.getParentBlockGroup(target);
                    assertThat(targetGroup).isSameAs(mHelper.getRootBlockGroup(target));
                    assertThat(targetGroup).isSameAs(mHelper.getRootBlockGroup(source));
                }

                // Add the shadow connection and disconnect the block
                targetConnection.setShadowConnection(shadow.getOutputConnection());
                mController.extractBlockAsRoot(source);
                assertThat(source.getParentBlock()).isNull();
                // Validate the block was replaced by the shadow
                assertThat(shadow).isEqualTo(targetConnection.getTargetBlock());

                if (withViews) {
                    // Check that the shadow block now has views
                    assertThat(mHelper.getView(shadow)).isNotNull();
                    BlockGroup shadowGroup = mHelper.getParentBlockGroup(target);
                    assertThat(shadowGroup).isSameAs(mHelper.getRootBlockGroup(shadow));
                }

                // Reattach the block and verify the shadow is hidden again
                mController.connect(sourceConnection, targetConnection);
                assertThat(source).isEqualTo(targetConnection.getTargetBlock());
                assertThat(shadow.getOutputConnection().getTargetBlock()).isNull();

                if (withViews) {
                    assertThat(mHelper.getView(shadow)).isNull();
                }
            }
        });
    }

    @Test
    public void testConnect_outputToInputBumpNoInput_headless() throws BlockLoadingException {
        testConnect_outputToInputBumpNoInput(false);
    }

    @Test
    public void testConnect_outputToInputBumpNoInput_withViews() throws BlockLoadingException {
        testConnect_outputToInputBumpNoInput(true);
    }

    private void testConnect_outputToInputBumpNoInput(final boolean withViews)
            throws BlockLoadingException {
        // Setup
        final Block target = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("simple_input_output").withId("target"));
        final Block tail = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("simple_input_output").withId("tail"));
        final Block source = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("output_no_input").withId("source"));

        runAndSync(new Runnable() {
            @Override
            public void run() {
                // Connect the output of tail to the input of target.
                target.getOnlyValueInput().getConnection().connect(tail.getOutputConnection());

                mController.addRootBlock(target);
                mController.addRootBlock(source);
                if (withViews) {
                    mController.initWorkspaceView(mWorkspaceView);
                    fakeOnAttachToWindow(target, source);
                }

                // Validate preconditions
                assertThat(mWorkspace.getRootBlocks().size()).isEqualTo(2);
                assertThat(mWorkspace.isRootBlock(target)).isTrue();
                assertThat(mWorkspace.isRootBlock(tail)).isFalse();
                assertThat(mWorkspace.isRootBlock(source)).isTrue();

                // Perform test: Connect the source to where the tail is currently attached.
                mController.connect(
                        source.getOutputConnection(), target.getOnlyValueInput().getConnection());

                // source is now a child of target, and tail is a new root block
                assertThat(mWorkspace.getRootBlocks().size()).isEqualTo(2);
                assertThat(mWorkspace.isRootBlock(target)).isTrue();
                assertThat(mWorkspace.isRootBlock(source)).isFalse();
                assertThat(mWorkspace.isRootBlock(tail)).isTrue();
                assertThat(target).isSameAs(target.getRootBlock());
                assertThat(target).isSameAs(source.getRootBlock());
                assertThat(tail).isSameAs(tail.getRootBlock());
                assertThat(tail.getOutputConnection().getTargetBlock()).isNull();

                if (withViews) {
                    BlockGroup targetGroup = mHelper.getParentBlockGroup(target);
                    BlockGroup tailGroup = mHelper.getParentBlockGroup(tail);
                    assertThat(targetGroup).isSameAs(mHelper.getRootBlockGroup(target));
                    assertThat(targetGroup).isSameAs(mHelper.getRootBlockGroup(source));
                    assertThat(tailGroup).isSameAs(mHelper.getRootBlockGroup(tail));
                    assertThat(targetGroup).isNotSameAs(tailGroup);

                    // Check that tail has been bumped far enough away.
                    double connectionDist =
                            tail.getOutputConnection().distanceFrom(source.getOutputConnection());
                    assertThat(connectionDist).named("bumped connection distance")
                            .isGreaterThan((double) mHelper.getMaxSnapDistance());
                }
            }
        });
    }

    @Test
    public void testConnect_outputToInputBumpMultipleInputs_headless()
            throws BlockLoadingException {
        testConnect_outputToInputBumpMultipleInputs(false);
    }

    @Test
    public void testConnect_outputToInputBumpMultipleInputs_withViews()
            throws BlockLoadingException {
        testConnect_outputToInputBumpMultipleInputs(true);
    }

    private void testConnect_outputToInputBumpMultipleInputs(final boolean withViews)
            throws BlockLoadingException {
        // Setup
        final Block target = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("simple_input_output").withId("target"));
        final Block tail = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("simple_input_output").withId("tail"));
        final Block source = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("multiple_input_output").withId("source"));

        runAndSync(new Runnable() {
            @Override
            public void run() {
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
                assertThat(mWorkspace.isRootBlock(target)).isTrue();
                assertThat(mWorkspace.isRootBlock(source)).isFalse();
                assertThat(target).isSameAs(source.getOutputConnection().getTargetBlock());
                assertThat(target).isSameAs(source.getRootBlock());

                // Tail has been returned to the workspace root, bumped some distance.
                assertThat(tail.getOutputConnection().getTargetBlock()).isNull();
                assertThat(mWorkspace.isRootBlock(tail)).isTrue();

                if (withViews) {
                    BlockGroup targetGroup = mHelper.getParentBlockGroup(target);
                    BlockGroup tailGroup = mHelper.getParentBlockGroup(tail);

                    targetGroup.updateAllConnectorLocations();
                    tailGroup.updateAllConnectorLocations();

                    assertThat(targetGroup).isSameAs(mHelper.getRootBlockGroup(target));
                    assertThat(targetGroup).isSameAs(mHelper.getRootBlockGroup(source));
                    assertThat(tailGroup).isSameAs(mHelper.getRootBlockGroup(tail));
                    assertThat(targetGroup).isNotSameAs(tailGroup);

                    double connectionDist =
                            source.getOutputConnection().distanceFrom(tail.getOutputConnection());
                    assertThat(connectionDist).named("bumped connection distance")
                            .isGreaterThan((double) mHelper.getMaxSnapDistance());
                }
            }
        });
    }

    @Test
    public void testConnect_outputToInputShadowSplice_headless()
            throws BlockLoadingException {
        testConnect_outputToInputShadowSplice(false);
    }

    @Test
    public void testConnect_outputToInputShadowSplice_withViews()
            throws BlockLoadingException {
        testConnect_outputToInputShadowSplice(true);
    }

    private void testConnect_outputToInputShadowSplice(final boolean withViews)
            throws BlockLoadingException {
        // Setup
        final Block target = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("simple_input_output").withId("target"));
        final Block tail = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("simple_input_output").withId("tail"));
        final Block source = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("simple_input_output").withId("source"));
        final Block shadow = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().shadow().copyOf(source).withId("shadow"));
        final Connection sourceInputConnection = source.getOnlyValueInput().getConnection();

        runAndSync(new Runnable() {
            @Override
            public void run() {
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
                assertThat(mWorkspace.getRootBlocks().size()).isEqualTo(2);
                assertThat(mWorkspace.isRootBlock(target)).isTrue();
                assertThat(mWorkspace.isRootBlock(tail)).isFalse();
                assertThat(mWorkspace.isRootBlock(source)).isTrue();

                // Perform test: Connect the source to where the tail is currently attached.
                mController.connect(
                        source.getOutputConnection(), target.getOnlyValueInput().getConnection());

                // source is now a child of target, and tail replaced the shadow
                assertThat(mWorkspace.getRootBlocks().size()).isEqualTo(1);
                assertThat(mWorkspace.isRootBlock(target)).isTrue();
                assertThat(mWorkspace.isRootBlock(source)).isFalse();
                assertThat(mWorkspace.isRootBlock(tail)).isFalse();
                assertThat(target).isSameAs(target.getRootBlock());
                assertThat(target).isSameAs(source.getRootBlock());
                assertThat(target).isSameAs(tail.getRootBlock());
                assertThat(source).isSameAs(tail.getParentBlock());

                if (withViews) {
                    BlockGroup targetGroup = mHelper.getParentBlockGroup(target);
                    assertThat(targetGroup).isSameAs(mHelper.getRootBlockGroup(target));
                    assertThat(targetGroup).isSameAs(mHelper.getRootBlockGroup(source));
                    assertThat(targetGroup).isSameAs(mHelper.getRootBlockGroup(tail));
                    assertThat(mHelper.getView(shadow)).isNull();
                }
            }
        });
    }

    @Test
    public void testConnect_outputToInputSplice_headless() throws BlockLoadingException {
        testConnect_outputToInputSplice(false);
    }

    @Test
    public void testConnect_outputToInputSplice_withViews() throws BlockLoadingException {
        testConnect_outputToInputSplice(true);
    }

    private void testConnect_outputToInputSplice(final boolean withViews)
            throws BlockLoadingException {
        // Setup
        final Block target = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("simple_input_output").withId("target"));
        final Block tail = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("multiple_input_output").withId("tail"));
        final Block source = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("simple_input_output").withId("source"));
        final Block shadow = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().shadow().copyOf(tail).withId("shadow"));

        runAndSync(new Runnable() {
            @Override
            public void run() {
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
                assertThat(mWorkspace.isRootBlock(target)).isTrue();
                assertThat(mWorkspace.isRootBlock(tail)).isFalse();
                assertThat(mWorkspace.isRootBlock(source)).isFalse();
                assertThat(target).isSameAs(source.getOutputConnection().getTargetBlock());
                assertThat(source).isSameAs(tail.getOutputConnection().getTargetBlock());

                if (withViews) {
                    BlockGroup targetGroup = mHelper.getParentBlockGroup(target);
                    assertThat(targetGroup).isSameAs(mHelper.getRootBlockGroup(target));
                    assertThat(targetGroup).isSameAs(mHelper.getRootBlockGroup(tail));
                    assertThat(targetGroup).isSameAs(mHelper.getRootBlockGroup(source));
                }
            }
        });
    }

    @Test
    public void testConnect_previousToNext_headless() throws BlockLoadingException {
        testConnect_previousToNext(false);
    }

    @Test
    public void testConnect_previousToNext_withViews() throws BlockLoadingException {
        testConnect_previousToNext(true);
    }

    private void testConnect_previousToNext(final boolean withViews) throws BlockLoadingException {
        // setup
        final Block target = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("statement_no_input").withId("target"));
        final Block source = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("statement_no_input").withId("source"));
        final Block shadow = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().shadow().copyOf(target).withId("connectShadow"));

        runAndSync(new Runnable() {
            @Override
            public void run() {
                BlockView targetView = null, sourceView = null;

                mController.addRootBlock(target);
                mController.addRootBlock(source);
                if (withViews) {
                    mController.initWorkspaceView(mWorkspaceView);
                    fakeOnAttachToWindow(target, source);

                    targetView = mHelper.getView(target);
                    sourceView = mHelper.getView(source);

                    assertThat(targetView).isNotNull();
                    assertThat(sourceView).isNotNull();
                }

                // Connect source after target. No prior connection to bump or splice.
                mController.connect(source.getPreviousConnection(), target.getNextConnection());

                // Validate
                assertThat(mWorkspace.isRootBlock(target)).isTrue();
                assertThat(mWorkspace.isRootBlock(source)).isFalse();
                assertThat(target).isSameAs(source.getPreviousBlock());

                if (withViews) {
                    BlockGroup rootGroup = mHelper.getRootBlockGroup(target);
                    assertThat(rootGroup).isSameAs(mHelper.getParentBlockGroup(target));
                    assertThat(rootGroup).isSameAs(mHelper.getParentBlockGroup(source));
                    assertThat(targetView).isSameAs(rootGroup.getChildAt(0));
                    assertThat(sourceView).isSameAs(rootGroup.getChildAt(1));
                }

                // Add the shadow to the target's next connection so the view will be created.
                target.getNextConnection().setShadowConnection(shadow.getPreviousConnection());
                mController.extractBlockAsRoot(source);

                assertThat(mWorkspace.isRootBlock(source)).isTrue();
                assertThat(target.getNextBlock()).isSameAs(shadow);

                if (withViews) {
                    BlockGroup rootGroup = mHelper.getRootBlockGroup(target);
                    assertThat(rootGroup).isSameAs(mHelper.getParentBlockGroup(shadow));
                    assertThat(rootGroup).isNotSameAs(mHelper.getParentBlockGroup(source));
                    assertThat(mHelper.getView(shadow)).isSameAs(rootGroup.getChildAt(1));
                }

                // Reattach the source and verify the shadow went away.
                mController.connect(source.getPreviousConnection(), target.getNextConnection());
                assertThat(mWorkspace.isRootBlock(source)).isFalse();
                assertThat(target.getNextBlock()).isSameAs(source);
                assertThat(shadow.getPreviousBlock()).isNull();

                if (withViews) {
                    assertThat(mHelper.getView(shadow)).isNull();
                }
            }
        });
    }

    @Test
    public void testConnect_previousToNextSplice_headless() throws BlockLoadingException {
        testConnect_previousToNextSplice(false);
    }

    @Test
    public void testConnect_previousToNextSplice_withViews() throws BlockLoadingException {
        testConnect_previousToNextSplice(true);
    }

    private void testConnect_previousToNextSplice(final boolean withViews) throws BlockLoadingException {
        // setup
        final Block target = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("statement_no_input").withId("target"));
        final Block tail = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("statement_no_input").withId("tail"));
        final Block source = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("statement_no_input").withId("source"));
        final Block shadow = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().shadow().ofType("statement_no_input").withId("shadowTail"));

        runAndSync(new Runnable() {
            @Override
            public void run() {
                BlockView targetView = null, tailView = null, sourceView = null;

                // Add a shadow to make sure it doesn't have any effects.
                Connection targetNext = target.getNextConnection();
                targetNext.setShadowConnection(shadow.getPreviousConnection());
                tail.getPreviousConnection().connect(targetNext);

                mController.addRootBlock(target);
                mController.addRootBlock(source);
                if (withViews) {
                    mController.initWorkspaceView(mWorkspaceView);
                    fakeOnAttachToWindow(target, source);

                    targetView = mHelper.getView(target);
                    tailView = mHelper.getView(tail);
                    sourceView = mHelper.getView(source);

                    assertThat(targetView).isNotNull();
                    assertThat(tailView).isNotNull();
                    assertThat(sourceView).isNotNull();
                }

                // Connect source after target, where tail is currently attached, causing a splice.
                mController.connect(source.getPreviousConnection(), target.getNextConnection());

                assertThat(target).isSameAs(source.getPreviousBlock());
                assertThat(source).isSameAs(tail.getPreviousBlock());

                if (withViews) {
                    BlockGroup rootGroup = mHelper.getRootBlockGroup(target);
                    assertThat(rootGroup).isSameAs(mHelper.getParentBlockGroup(target));
                    assertThat(rootGroup).isSameAs(mHelper.getParentBlockGroup(tail));
                    assertThat(rootGroup).isSameAs(mHelper.getParentBlockGroup(source));
                    assertThat(targetView).isSameAs(rootGroup.getChildAt(0));
                    assertThat(sourceView).isSameAs(rootGroup.getChildAt(1));  // Spliced in between.
                    assertThat(tailView).isSameAs(rootGroup.getChildAt(2));
                }
            }
        });
    }

    @Test
    public void testConnect_previousToNextBumpRemainder_headless() throws BlockLoadingException {
        testConnect_previousToNextBumpRemainder(false);
    }

    @Test
    public void testConnect_previousToNextBumpRemainder_withViews() throws BlockLoadingException {
        testConnect_previousToNextBumpRemainder(true);
    }

    private void testConnect_previousToNextBumpRemainder(final boolean withViews)
            throws BlockLoadingException {
        // setup
        final Block target = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("statement_no_input").withId("target"));
        final Block tail1 = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("statement_no_input").withId("tail1"));
        final Block tail2 = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("statement_no_input").withId("tail2"));
        final Block source = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("statement_no_next").withId("source"));

        runAndSync(new Runnable() {
            @Override
            public void run() {
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

                    assertThat(targetView).isNotNull();
                    assertThat(tailView1).isNotNull();
                    assertThat(tailView2).isNotNull();
                    assertThat(sourceView).isNotNull();
                }

                // Run test: Connect source after target, where tail is currently attached.
                // Since source does not have a next connection, bump the tail.
                mController.connect(source.getPreviousConnection(), target.getNextConnection());

                // Target and source are connected.
                assertThat(mWorkspace.isRootBlock(target)).isTrue();
                assertThat(target).isSameAs(source.getPreviousBlock());

                // Tail has been returned to the workspace root.
                assertThat(mWorkspace.isRootBlock(tail1)).isTrue();
                assertThat(tail1.getPreviousBlock()).isNull();
                assertThat(mWorkspace.isRootBlock(tail2)).isFalse();
                assertThat(mWorkspace.isRootBlock(source)).isFalse();
                assertThat(tail1).isSameAs(tail1.getRootBlock());
                assertThat(tail1).isSameAs(tail2.getRootBlock());

                if (withViews) {
                    BlockGroup targetRootGroup = mHelper.getRootBlockGroup(target);
                    BlockGroup tailRootGroup = mHelper.getRootBlockGroup(tail1);
                    assertThat(targetRootGroup).isSameAs(mHelper.getParentBlockGroup(target));
                    assertThat(targetRootGroup).isSameAs(mHelper.getRootBlockGroup(source));
                    assertThat(targetRootGroup).isNotSameAs(tailRootGroup);
                    assertThat(tailRootGroup).isSameAs(mHelper.getRootBlockGroup(tail2));
                    assertThat(targetView).isSameAs(targetRootGroup.getChildAt(0));
                    assertThat(sourceView).isSameAs(targetRootGroup.getChildAt(1));
                    assertThat(tailView1).isSameAs(tailRootGroup.getChildAt(0));
                    assertThat(tailView2).isSameAs(tailRootGroup.getChildAt(1));

                    // Check that tail has been bumped far enough away.
                    double connectionDistance =
                            tail1.getPreviousConnection().distanceFrom(target.getNextConnection());
                    assertThat(connectionDistance).named("bumped connection distance")
                            .isGreaterThan((double) mHelper.getMaxSnapDistance());
                }
            }
        });
    }


    @Test
    public void testConnect_previousToNextShadowSplice_headless() throws BlockLoadingException {
        testConnect_previousToNextShadowSplice(false);
    }

    @Test
    public void testConnect_previousToNextShadowSplice_withViews() throws BlockLoadingException {
        testConnect_previousToNextShadowSplice(true);
    }

    private void testConnect_previousToNextShadowSplice(final boolean withViews)
            throws BlockLoadingException {
        // setup
        final Block target = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("statement_no_input").withId("target"));
        final Block tail1 = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("statement_no_input").withId("tail1"));
        final Block tail2 = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("statement_no_input").withId("tail2"));
        final Block source = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("statement_no_input").withId("source"));
        final Block shadowTail = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().shadow().copyOf(tail1).withId("shadowTail"));

        runAndSync(new Runnable() {
            @Override
            public void run() {
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

                    assertThat(targetView).isNotNull();
                    assertThat(tailView1).isNotNull();
                    assertThat(tailView2).isNotNull();
                    assertThat(sourceView).isNotNull();
                    assertThat(mHelper.getView(shadowTail)).isNotNull();
                }

                // Run test: Connect source after target, where tail is currently attached.
                // Since source has a shadow connected to next, tail should replace it.
                mController.connect(source.getPreviousConnection(), target.getNextConnection());

                // Target and source are connected.
                assertThat(mWorkspace.isRootBlock(target)).isTrue();
                assertThat(target).isSameAs(source.getPreviousBlock());

                // Tail has replaced the shadow.
                assertThat(mWorkspace.isRootBlock(tail1)).isFalse();
                assertThat(shadowTail.getPreviousBlock()).isNull();
                assertThat(source).isSameAs(tail1.getParentBlock());
                assertThat(mWorkspace.isRootBlock(tail2)).isFalse();
                assertThat(mWorkspace.isRootBlock(source)).isFalse();
                assertThat(target).isSameAs(tail1.getRootBlock());
                assertThat(target).isSameAs(tail2.getRootBlock());

                if (withViews) {
                    BlockGroup targetRootGroup = mHelper.getRootBlockGroup(target);
                    assertThat(targetRootGroup).isSameAs(mHelper.getParentBlockGroup(target));
                    assertThat(targetRootGroup).isSameAs(mHelper.getRootBlockGroup(source));
                    assertThat(targetRootGroup).isSameAs(mHelper.getRootBlockGroup(tail1));
                    assertThat(targetRootGroup).isSameAs(mHelper.getRootBlockGroup(tail2));

                    assertThat(targetView).isSameAs(targetRootGroup.getChildAt(0));
                    assertThat(sourceView).isSameAs(targetRootGroup.getChildAt(1));
                    assertThat(tailView1).isSameAs(targetRootGroup.getChildAt(2));
                    assertThat(tailView2).isSameAs(targetRootGroup.getChildAt(3));

                    assertThat(mHelper.getView(shadowTail)).isNull();
                }
            }
        });
    }

    @Test
    public void testConnect_previousToStatement_headless() throws BlockLoadingException {
        testConnect_previousToStatement(false);
    }

    @Test
    public void testConnect_previousToStatement_withViews() throws BlockLoadingException {
        testConnect_previousToStatement(true);
    }

    private void testConnect_previousToStatement(final boolean withViews)
            throws BlockLoadingException {
        // setup
        final Block target = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("statement_statement_input").withId("target"));
        final Block source = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("statement_statement_input").withId("source"));
        final Block shadow = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().shadow().copyOf(source).withId("shadow"));

        runAndSync(new Runnable() {
            @Override
            public void run() {
                Connection statementConnection = target.getInputByName("statement input").getConnection();

                mController.addRootBlock(target);
                mController.addRootBlock(source);
                if (withViews) {
                    mController.initWorkspaceView(mWorkspaceView);
                    fakeOnAttachToWindow(target, source);
                }

                // Run test: Connect source inside target. No prior connection to bump.
                mController.connect(source.getPreviousConnection(), statementConnection);

                assertThat(mWorkspace.isRootBlock(target)).isTrue();
                assertThat(mWorkspace.isRootBlock(source)).isFalse();
                assertThat(target).isSameAs(source.getPreviousBlock());

                if (withViews) {
                    BlockGroup rootGroup = mHelper.getRootBlockGroup(target);
                    assertThat(rootGroup).isSameAs(mHelper.getParentBlockGroup(target));
                    assertThat(rootGroup).isSameAs(mHelper.getRootBlockGroup(source));
                }

                // Add the shadow block
                statementConnection.setShadowConnection(shadow.getPreviousConnection());
                // disconnect the real blocks which should attach the shadow to replace it.
                mController.extractBlockAsRoot(source);

                assertThat(mWorkspace.isRootBlock(source)).isTrue();
                assertThat(mWorkspace.isRootBlock(shadow)).isFalse();
                assertThat(statementConnection.getTargetBlock()).isSameAs(shadow);

                if (withViews) {
                    assertThat(mHelper.getView(shadow)).isNotNull();
                    assertThat(mHelper.getRootBlockGroup(target))
                            .isSameAs(mHelper.getRootBlockGroup(shadow));
                }

                // Reconnect the source and make sure the shadow goes away
                mController.connect(source.getPreviousConnection(), statementConnection);
                assertThat(shadow.getPreviousBlock()).isNull();
                assertThat(statementConnection.getTargetBlock()).isSameAs(source);
                assertThat(mWorkspace.isRootBlock(shadow)).isFalse();

                if (withViews) {
                    assertThat(mHelper.getView(shadow)).isNull();
                    assertThat(mHelper.getRootBlockGroup(target))
                            .isSameAs(mHelper.getRootBlockGroup(source));
                }
            }
        });
    }

    @Test
    public void testConnect_previousToStatementSpliceRemainder_headless()
            throws BlockLoadingException {
        testConnect_previousToStatementSpliceRemainder(false);
    }

    @Test
    public void testConnect_previousToStatementSpliceRemainder_withViews()
            throws BlockLoadingException {
        testConnect_previousToStatementSpliceRemainder(true);
    }

    private void testConnect_previousToStatementSpliceRemainder(final boolean withViews)
            throws BlockLoadingException {
        // setup
        final Block target = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("statement_statement_input").withId("target"));
        final Block tail = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("statement_statement_input").withId("tail"));
        final Block source = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("statement_statement_input").withId("source"));
        final Block shadow = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().shadow().copyOf(source));

        runAndSync(new Runnable() {
            @Override
            public void run() {
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

                    assertThat(targetView).isNotNull();
                    assertThat(tailView).isNotNull();
                    assertThat(sourceView).isNotNull();
                }

                // Run test: Connect source inside target, where tail is attached, resulting in a splice.
                mController.connect(source.getPreviousConnection(), statementConnection);

                // Validate result.
                assertThat(mWorkspace.isRootBlock(target)).isTrue();
                assertThat(mWorkspace.isRootBlock(tail)).isFalse();
                assertThat(mWorkspace.isRootBlock(source)).isFalse();
                assertThat(target).isSameAs(source.getPreviousBlock());
                assertThat(source).isSameAs(tail.getPreviousBlock());

                if (withViews) {
                    BlockGroup rootGroup = mHelper.getRootBlockGroup(target);
                    BlockGroup secondGroup = mHelper.getParentBlockGroup(tail);
                    assertThat(rootGroup).isSameAs(mHelper.getRootBlockGroup(tail));
                    assertThat(rootGroup).isNotSameAs(secondGroup);
                    assertThat(secondGroup).isSameAs(mHelper.getParentBlockGroup(source));
                    assertThat(sourceView).isSameAs(secondGroup.getChildAt(0));
                    assertThat(tailView).isSameAs(secondGroup.getChildAt(1));
                }
            }
        });
    }

    @Test
    public void testConnect_previousToStatemenBumpRemainder_headless()
            throws BlockLoadingException {
        testConnect_previousToStatementBumpRemainder(false);
    }

    @Test
    public void testConnect_previousToStatemenBumpRemainder_withViews()
            throws BlockLoadingException {
        testConnect_previousToStatementBumpRemainder(true);
    }

    private void testConnect_previousToStatementBumpRemainder(final boolean withViews)
            throws BlockLoadingException {
        final Block target = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("statement_statement_input").withId("target"));
        final Block tail = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("statement_statement_input").withId("tail"));
        final Block source = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("statement_no_next").withId("source"));

        runAndSync(new Runnable() {
            @Override
            public void run() {
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
                    assertThat(sourceView).isNotNull();
                }

                // Connect source inside target, where tail is attached.  Source does not have a next, so
                // this will bump tail back to the root.
                mController.connect(source.getPreviousConnection(),
                        target.getInputByName("statement input").getConnection());

                // Validate
                assertThat(mWorkspace.isRootBlock(target)).isTrue();
                assertThat(mWorkspace.isRootBlock(tail)).isTrue();
                assertThat(mWorkspace.isRootBlock(source)).isFalse();
                assertThat(target.getInputByName("statement input").getConnection())
                        .isSameAs(source.getPreviousConnection().getTargetConnection());
                assertThat(tail.getPreviousBlock()).isNull();

                if (withViews) {
                    BlockGroup targetRootGroup = mHelper.getRootBlockGroup(target);
                    BlockGroup tailRootGroup = mHelper.getRootBlockGroup(tail);
                    BlockGroup sourceGroup = mHelper.getParentBlockGroup(source);
                    assertThat(targetRootGroup).isSameAs(mHelper.getParentBlockGroup(target));
                    assertThat(targetRootGroup).isNotSameAs(tailRootGroup);
                    assertThat(tailRootGroup).isSameAs(mHelper.getParentBlockGroup(tail));
                    assertThat(targetRootGroup).isSameAs(mHelper.getRootBlockGroup(source));
                    assertThat(sourceGroup.getParent())
                            .isSameAs(target.getInputByName("statement input").getView());
                    assertThat(sourceView).isSameAs(sourceGroup.getChildAt(0));

                    double connectionDist =
                            source.getPreviousConnection().distanceFrom(tail.getPreviousConnection());
                    assertThat(connectionDist).named("bumped connection distance")
                            .isGreaterThan((double) mHelper.getMaxSnapDistance());
                }
            }
        });
    }

    @Test
    public void testConnect_previousToStatementShadowSplice_headless()
            throws BlockLoadingException {
        testConnect_previousToStatementShadowSplice(false);
    }

    @Test
    public void testConnect_previousToStatementShadowSplice_withViews()
            throws BlockLoadingException {
        testConnect_previousToStatementShadowSplice(true);
    }

    private void testConnect_previousToStatementShadowSplice(final boolean withViews)
            throws BlockLoadingException {
        final Block target = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("statement_statement_input").withId("target"));
        final Block tail = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("statement_statement_input").withId("tail"));
        final Block source = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("statement_no_input").withId("source"));
        final Block shadow = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().shadow().copyOf(source).withId("shadow"));

        runAndSync(new Runnable() {
            @Override
            public void run() {
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
                    assertThat(sourceView).isNotNull();
                    assertThat(mHelper.getView(shadow)).isNotNull();
                }

                // Connect source inside target, where tail is attached.  Source has a shadow on next, so
                // tail should replace it.
                mController.connect(source.getPreviousConnection(), statementConnection);

                // Validate
                assertThat(mWorkspace.isRootBlock(target)).isTrue();
                assertThat(mWorkspace.isRootBlock(tail)).isFalse();
                assertThat(mWorkspace.isRootBlock(source)).isFalse();
                assertThat(statementConnection)
                        .isSameAs(source.getPreviousConnection().getTargetConnection());
                assertThat(source).isSameAs(tail.getPreviousBlock());
                assertThat(shadow.getParentBlock()).isNull();

                if (withViews) {
                    BlockGroup targetRootGroup = mHelper.getRootBlockGroup(target);
                    BlockGroup sourceGroup = mHelper.getParentBlockGroup(source);
                    assertThat(targetRootGroup).isSameAs(mHelper.getParentBlockGroup(target));
                    assertThat(targetRootGroup).isSameAs(mHelper.getRootBlockGroup(tail));
                    assertThat(sourceGroup).isSameAs(mHelper.getParentBlockGroup(tail));
                    assertThat(targetRootGroup).isSameAs(mHelper.getRootBlockGroup(source));
                    assertThat(sourceGroup.getParent())
                            .isSameAs(target.getInputByName("statement input").getView());
                    assertThat(sourceView).isSameAs(sourceGroup.getChildAt(0));
                    assertThat(mHelper.getView(tail)).isSameAs(sourceGroup.getChildAt(1));

                    assertThat(mHelper.getView(shadow)).isNull();
                }

                // Make sure nothing breaks when we detach the tail and the shadow comes back
                mController.extractBlockAsRoot(tail);
            }
        });
    }

    @Test
    public void testExtractAsRootBlock_alreadyRoot_headless() throws BlockLoadingException {
        testExtractAsRootBlock_alreadyRoot(false);
    }

    @Test
    public void testExtractAsRootBlock_alreadyRoot_withViews() throws BlockLoadingException {
        testExtractAsRootBlock_alreadyRoot(true);
    }

    private void testExtractAsRootBlock_alreadyRoot(final boolean withViews)
            throws BlockLoadingException {
        // Configure
        final Block block = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("statement_statement_input").withId("block"));

        runAndSync(new Runnable() {
            @Override
            public void run() {
                mController.addRootBlock(block);
                if (withViews) {
                    mController.initWorkspaceView(mWorkspaceView);
                    fakeOnAttachToWindow(block);
                }

                // Check Preconditions
                assertThat(1).isEqualTo(mWorkspace.getRootBlocks().size());
                assertThat(mWorkspace.getRootBlocks().contains(block)).isTrue();

                // Run test: Make block a root (even though it already is).
                mController.extractBlockAsRoot(block);

                // Validate (no change)
                assertThat(1).isEqualTo(mWorkspace.getRootBlocks().size());
                assertThat(mWorkspace.getRootBlocks().contains(block)).isTrue();

                if (withViews) {
                    BlockGroup firstGroup = mHelper.getParentBlockGroup(block);
                    assertThat(firstGroup).isSameAs(mHelper.getRootBlockGroup(block));
                }
            }
        });
    }

    @Test
    public void testExtractBlockAsRoot_fromInput_headless() throws BlockLoadingException {
        testExtractBlockAsRoot_fromInput(false);
    }

    @Test
    public void testExtractBlockAsRoot_fromInput_withViews() throws BlockLoadingException {
        testExtractBlockAsRoot_fromInput(true);
    }

    private void testExtractBlockAsRoot_fromInput(final boolean withViews)
            throws BlockLoadingException {
        final Block first = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("simple_input_output").withId("first block"));
        final Block second = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("simple_input_output").withId("second block"));

        runAndSync(new Runnable() {
            @Override
            public void run() {
                mController.connect(
                        second.getOutputConnection(), first.getOnlyValueInput().getConnection());
                mController.addRootBlock(first);
                if (withViews) {
                    mController.initWorkspaceView(mWorkspaceView);
                    fakeOnAttachToWindow(first, second);
                }

                // Check preconditions
                List<Block> rootBlocks = mWorkspace.getRootBlocks();
                assertThat(1).isEqualTo(rootBlocks.size());
                assertThat(first).isEqualTo(rootBlocks.get(0));

                // Run test: Extract second out from under first.
                mController.extractBlockAsRoot(second);

                rootBlocks = mWorkspace.getRootBlocks();
                assertThat(2).isEqualTo(rootBlocks.size());
                assertThat(rootBlocks.contains(first)).isTrue();
                assertThat(rootBlocks.contains(second)).isTrue();
                assertThat(first.getOnlyValueInput().getConnection().isConnected()).isFalse();
                assertThat(second.getOutputConnection().isConnected()).isFalse();

                if (withViews) {
                    BlockGroup firstGroup = mHelper.getParentBlockGroup(first);
                    assertThat(firstGroup).isNotNull();
                    BlockGroup secondGroup = mHelper.getParentBlockGroup(second);
                    assertThat(secondGroup).isNotNull();
                    assertThat(secondGroup).isNotSameAs(firstGroup);
                }
            }
        });
    }

    @Test
    public void testExtractBlockAsRoot_fromNext_headless() throws BlockLoadingException {
        testExtractBlockAsRoot_fromNext(false);
    }

    @Test
    public void testExtractBlockAsRoot_fromNext_withViews() throws BlockLoadingException {
        testExtractBlockAsRoot_fromNext(true);
    }

    private void testExtractBlockAsRoot_fromNext(final boolean withViews)
            throws BlockLoadingException {
        // Configure
        final Block first = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("statement_statement_input").withId("first block"));
        final Block second = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("statement_statement_input").withId("second block"));

        runAndSync(new Runnable() {
            @Override
            public void run() {
                mController.connect(second.getPreviousConnection(), first.getNextConnection());
                mController.addRootBlock(first);
                if (withViews) {
                    mController.initWorkspaceView(mWorkspaceView);
                    fakeOnAttachToWindow(first, second);
                }

                // Check preconditions
                List<Block> rootBlocks = mWorkspace.getRootBlocks();
                assertThat(1).isEqualTo(rootBlocks.size());
                assertThat(first).isEqualTo(rootBlocks.get(0));
                assertThat(first.getNextConnection().getTargetBlock()).isEqualTo(second);
                if (withViews) {
                    assertThat(mHelper.getParentBlockGroup(first))
                            .isSameAs(mHelper.getParentBlockGroup(second));
                }

                // Run test: Extract second out from under first.
                mController.extractBlockAsRoot(second);

                // Validate
                rootBlocks = mWorkspace.getRootBlocks();
                assertThat(2).isEqualTo(rootBlocks.size());
                assertThat(rootBlocks.contains(first)).isTrue();
                assertThat(rootBlocks.contains(second)).isTrue();
                assertThat(first.getNextConnection().isConnected()).isFalse();
                assertThat(second.getPreviousConnection().isConnected()).isFalse();

                if (withViews) {
                    BlockGroup firstGroup = mHelper.getParentBlockGroup(first);
                    assertThat(firstGroup).isNotNull();
                    BlockGroup secondGroup = mHelper.getParentBlockGroup(second);
                    assertThat(secondGroup).isNotNull();
                    assertThat(secondGroup).isNotSameAs(firstGroup);
                }
            }
        });
    }

    @Test
    public void testVariableCallback_onCreate() {
        runAndSync(new Runnable() {
            @Override
            public void run() {
                NameManager.VariableNameManager nameManager =
                        (NameManager.VariableNameManager) mController.getWorkspace()
                                .getVariableNameManager();

                assertThat(mVariableCallback.onCreateVariable).isNull();

                // Calling requestAddVariable and the callback blocking creation
                mVariableCallback.whenOnCreateCalled = false;
                mController.requestAddVariable("var1");
                assertThat(mVariableCallback.onCreateVariable).isEqualTo("var1");
                assertThat(nameManager.getUsedNames().size()).isEqualTo(0);

                // Calling addVariable bypasses the callback
                mVariableCallback.onCreateVariable = null;
                mController.addVariable("var1");
                assertThat(mVariableCallback.onCreateVariable).isNull();
                assertThat(nameManager.contains("var1")).isTrue();

                // Calling requestAddVariable and the callback allows creation
                mVariableCallback.reset();
                mVariableCallback.whenOnCreateCalled = true;
                mController.requestAddVariable("var2");
                assertThat(mVariableCallback.onCreateVariable).isEqualTo("var2");
                assertThat(nameManager.contains("var2")).isTrue();
            }
        });
    }

    @Test
    public void testVariableCallback_onRename() {
        runAndSync(new Runnable() {
            @Override
            public void run() {
                NameManager.VariableNameManager nameManager =
                        (NameManager.VariableNameManager) mController.getWorkspace()
                                .getVariableNameManager();
                mController.addVariable("var1");
                mController.addVariable("var2");

                // Calling rename without forcing and the callback blocks it
                mVariableCallback.whenOnRenameCalled = false;
                mController.requestRenameVariable("var1", "var3");
                assertThat(mVariableCallback.onRenameVariable).isEqualTo("var1");
                assertThat(nameManager.contains("var3")).isFalse();
                assertThat(nameManager.contains("var1")).isTrue();

                // Calling rename with forcing skips the callback
                mVariableCallback.onRenameVariable = null;
                mController.renameVariable("var1", "var3");
                assertThat(mVariableCallback.onRenameVariable).isNull();
                assertThat(nameManager.contains("var3")).isTrue();
                assertThat(nameManager.contains("var1")).isFalse();

                // Calling rename without forcing and the callback allows it
                mVariableCallback.whenOnRenameCalled = true;
                mController.requestRenameVariable("var2", "var4");
                assertThat(mVariableCallback.onRenameVariable).isEqualTo("var2");
                assertThat(nameManager.contains("var4")).isTrue();
                assertThat(nameManager.contains("var2")).isFalse();

                // Verify that we have two variables still
                assertThat(nameManager.size()).isEqualTo(2);
            }
        });
    }

    @Test
    public void testVariableCallback_onRemove() {
        runAndSync(new Runnable() {
            @Override
            public void run() {
                NameManager.VariableNameManager nameManager =
                        (NameManager.VariableNameManager) mController.getWorkspace()
                                .getVariableNameManager();
                mController.addVariable("var3");
                mController.addVariable("var4");

                // Calling delete without forcing and the callback blocks it
                mVariableCallback.whenOnDeleteCalled = false;
                mController.requestDeleteVariable("var3");
                assertThat(mVariableCallback.onDeleteVariable).isEqualTo("var3");
                assertThat(nameManager.contains("var3")).isTrue();

                // Calling delete with forcing skips callback
                mVariableCallback.onDeleteVariable = null;
                mController.deleteVariable("var3");
                assertThat(mVariableCallback.onDeleteVariable).isNull();
                assertThat(nameManager.contains("var3")).isFalse();

                // Calling delete without forcing and callback allows it
                mVariableCallback.whenOnDeleteCalled = true;
                mController.requestDeleteVariable("var4");
                assertThat(mVariableCallback.onDeleteVariable).isEqualTo("var4");
                assertThat(nameManager.contains("var4")).isFalse();

                // Verify that we have no variables left
                assertThat(nameManager.size()).isEqualTo(0);
            }
        });
    }

    @Test
    public void testCreateVariableDoesNotChangeCase() {
        runAndSync(new Runnable() {
            @Override
            public void run() {
                String name = "NEW VAR NAME";

                String finalName = mController.addVariable(name);

                assertThat(finalName).isEqualTo(name);
            }
        });
    }

    @Test
    public void testRenameVariableDoesNotChangeCase() {
        runAndSync(new Runnable() {
            @Override
            public void run() {
                String oldName = "oldName";
                String newName = "TEST";

                String finalName = mController.renameVariable(oldName, newName);

                assertThat(finalName).isEqualTo(newName);
            }
        });
    }

    @Test
    public void testCreateVariableDoesNotAllowDuplicateNamesWithDifferentCases() {
        runAndSync(new Runnable() {
            @Override
            public void run() {
                String name1 = "VAR";
                String name2 = "var";

                String finalName1 = mController.addVariable(name1);
                String finalName2 = mController.addVariable(name2);

                assertWithMessage("Second similar variable name (matching all but case) was renamed.")
                        .that(finalName2).isNotEqualTo(name2);
                assertWithMessage("Renamed second variable does not match first variable.")
                        .that(finalName1.toLowerCase()).isNotEqualTo(finalName2.toLowerCase());
            }
        });
    }

    @Test
    public void testRemoveVariable() throws BlockLoadingException {
        configureForUIThread();

        final Block set1 = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("set_variable").withId("first block"));
        final Block set2 = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("set_variable").withId("second block"));
        final Block set3 = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("set_variable").withId("third block"));
        final Block set4 = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("set_variable").withId("fourth block"));
        final Block set5 = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("set_variable").withId("fifth block"));
        final Block set6 = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("set_variable").withId("sixth block"));
        final Block statement = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("statement_statement_input").withId("statement block"));
        final Block get1 = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("get_variable").withId("get1"));
        final Block get2 = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("get_variable").withId("get2"));
        final Block get3 = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("get_variable").withId("get3"));

        runAndSync(new Runnable() {
            @Override
            public void run() {
                mController.addVariable("var1");
                mController.addVariable("var2");
                mController.addVariable("var3");
                mController.addVariable("var4");

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
                assertThat(rootBlocks.size()).isEqualTo(3);

                mVariableCallback.whenOnDeleteCalled = true;
                mVariableCallback.onDeleteVariable = null;
                mController.requestDeleteVariable("var1");

                assertThat(mVariableCallback.onDeleteVariable).isEqualTo("var1");
                assertThat(rootBlocks.size()).isEqualTo(1);

                Block block = rootBlocks.get(0);
                assertThat(block).isSameAs(statement);
                block = block.getInputs().get(0).getConnectedBlock();
                assertThat(block).isSameAs(set2);

                block = block.getNextBlock();
                assertThat(block).isSameAs(set4);

                block = block.getNextBlock();
                assertThat(block).isNull();
            }
        });
    }

    @Test
    public void testLoadWorkspaceContents_andReset() throws BlockLoadingException {
        mController.initWorkspaceView(mWorkspaceView);
        assertThat(mWorkspace.getRootBlocks()).hasSize(0);
        assertThat(mWorkspaceView.getChildCount()).isEqualTo(0);

        mController.loadWorkspaceContents(
                BlockTestStrings.EMPTY_BLOCK_WITH_POSITION +
                BlockTestStrings.EMPTY_BLOCK_WITH_POSITION.replace(
                        BlockTestStrings.EMPTY_BLOCK_ID,
                        BlockTestStrings.EMPTY_BLOCK_ID + '2'));
        assertThat(mWorkspace.getRootBlocks()).hasSize(2);
        assertThat(mWorkspaceView.getChildCount()).isEqualTo(2);

        mController.resetWorkspace();
        assertThat(mWorkspace.getRootBlocks()).hasSize(0);
        assertThat(mWorkspaceView.getChildCount()).isEqualTo(0);
    }

    @Test
    public void testLoadWorkspaceContents_andTrashAllBlocks() throws BlockLoadingException {
        runAndSync(new Runnable() {
            @Override
            public void run() {
                mController.initWorkspaceView(mWorkspaceView);
                assertThat(mWorkspace.getRootBlocks()).hasSize(0);
                assertThat(mWorkspace.getTrashCategory().getItems()).hasSize(0);

                try {
                    mController.loadWorkspaceContents(
                            BlockTestStrings.EMPTY_BLOCK_WITH_POSITION +
                                    BlockTestStrings.EMPTY_BLOCK_WITH_POSITION.replace(
                                            BlockTestStrings.EMPTY_BLOCK_ID,
                                            BlockTestStrings.EMPTY_BLOCK_ID + '2'));
                } catch (BlockLoadingException e) {
                    throw new IllegalStateException(e);  // Throw as RuntimeException.
                }
                assertThat(mWorkspace.getRootBlocks()).hasSize(2);
                assertThat(mWorkspace.getTrashCategory().getItems()).hasSize(0);

                mController.trashAllBlocks();
                assertThat(mWorkspace.getRootBlocks()).hasSize(0);
                assertThat(mWorkspace.getTrashCategory().getItems()).hasSize(2);
            }
        });
    }

    @Test
    public void testTrashAllBlocksSetsWorkspaceId() throws BlockLoadingException {
        // given
        final Block block = mBlockFactory.obtainBlockFrom(
            new BlockTemplate().ofType("simple_input_output").withId("connectTarget"));
        final String expectedWorkspaceId = BlocklyEvent.WORKSPACE_ID_TRASH;

        runAndSync(new Runnable() {
            @Override
            public void run() {
                // when
                mController.addRootBlock(block);
                mController.trashAllBlocks();

                // then
                assertThat(block.getEventWorkspaceId()).isEqualTo(expectedWorkspaceId);
            }
        });
    }

    @Test
    public void testRemoveBlockTreeSetsWorkspaceId() throws BlockLoadingException {
        // given
        final Block block = mBlockFactory.obtainBlockFrom(
            new BlockTemplate().ofType("simple_input_output").withId("connectTarget"));

        // when
        runAndSync(new Runnable() {
           @Override
           public void run() {
                mController.addRootBlock(block);
                mController.removeBlockTree(block);

                // then
                assertThat(block.getEventWorkspaceId()).isNull();
           }
        });
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

    private static class StubVariableCallback extends BlocklyController.VariableCallback {
        String onDeleteVariable = null;
        String onCreateVariable = null;
        String onRenameVariable = null;

        boolean whenOnDeleteCalled = true;
        boolean whenOnCreateCalled = true;
        boolean whenOnRenameCalled = true;

        @Override
        public boolean onDeleteVariable(String variable,  VariableInfo info) {
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

        @Override
        public void onAlertCannotDeleteProcedureArgument(String variableName, VariableInfo info) {
            // Do nothing
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
