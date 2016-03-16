package com.google.blockly.control;

import com.google.blockly.MockitoAndroidTestCase;
import com.google.blockly.R;
import com.google.blockly.model.Block;
import com.google.blockly.model.BlockFactory;
import com.google.blockly.model.BlockTestStrings;
import com.google.blockly.model.Workspace;
import com.google.blockly.ui.BlockGroup;
import com.google.blockly.ui.BlockView;
import com.google.blockly.ui.BlockViewFactory;
import com.google.blockly.ui.WorkspaceHelper;
import com.google.blockly.ui.WorkspaceView;
import com.google.blockly.ui.vertical.VerticalBlocksViewFactory;

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

    @Override
    public void setUp() throws Exception {
        super.setUp();

        mHelper = new WorkspaceHelper(getContext());
        mViewFactory = new VerticalBlocksViewFactory(getContext(), mHelper);
        mController = new BlocklyController.Builder(getContext())
                .setWorkspaceHelper(mHelper)
                .setBlockViewFactory(mViewFactory)
                .addBlockDefinitions(R.raw.test_blocks)
                .build();
        mBlockFactory = mController.getBlockFactory();
        mWorkspace = mController.getWorkspace();
        mHelper = mController.getWorkspaceHelper();
        mConnectionManager = mController.getWorkspace().getConnectionManager();

        mWorkspaceView = new WorkspaceView(getContext());
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
        mController.addRootBlock(target);
        mController.addRootBlock(source);

        if (withViews) {
            mController.initWorkspaceView(mWorkspaceView);

            // Validate initial view state.
            BlockView targetView = mHelper.getView(target);
            BlockView sourceView = mHelper.getView(source);
            assertNotNull(targetView);
            assertNotNull(sourceView);
            assertNotSame(mHelper.getRootBlockGroup(target),
                    mHelper.getRootBlockGroup(source));
        }

        // Perform test: connection source's output to target's input.
        mController.connect(source, source.getOutputConnection(),
                target.getOnlyValueInput().getConnection());

        // Validate model changes.
        assertTrue(mWorkspace.isRootBlock(target));
        assertFalse(mWorkspace.isRootBlock(source));
        assertSame(target, source.getOutputConnection().getTargetBlock());

        if (withViews) {
            // Validate view changes
            BlockGroup targetGroup = mHelper.getParentBlockGroup(target);
            assertSame(targetGroup, mHelper.getRootBlockGroup(target));
            assertSame(targetGroup, mHelper.getRootBlockGroup(source));
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
        }

        // Validate preconditions
        assertEquals(2, mWorkspace.getRootBlocks().size());
        assertTrue(mWorkspace.isRootBlock(target));
        assertFalse(mWorkspace.isRootBlock(tail));
        assertTrue(mWorkspace.isRootBlock(source));

        // Perform test: Connect the source to where the tail is currently attached.
        mController.connect(source, source.getOutputConnection(),
                target.getOnlyValueInput().getConnection());

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
        }

        // Perform test: Connect the source to where the tail is currently attached.
        mController.connect(source, source.getOutputConnection(),
                target.getOnlyValueInput().getConnection());

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
            assertSame(targetGroup, mHelper.getRootBlockGroup(target));
            assertSame(targetGroup, mHelper.getRootBlockGroup(source));
            assertSame(tailGroup, mHelper.getRootBlockGroup(tail));
            assertNotSame(targetGroup, tailGroup);
            assertTrue(mHelper.getMaxSnapDistance() <=
                    source.getOutputConnection().distanceFrom(tail.getOutputConnection()));
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

        // Connect the output of tail to the input of source.
        tail.getOutputConnection().connect(target.getOnlyValueInput().getConnection());

        mController.addRootBlock(target);
        mController.addRootBlock(source);
        if (withViews) {
            mController.initWorkspaceView(mWorkspaceView);
        }

        // Splice third between second and first.
        mController.connect(source, source.getOutputConnection(),
                target.getOnlyValueInput().getConnection());

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
        BlockView targetView = null, sourceView = null;
        mController.addRootBlock(target);
        mController.addRootBlock(source);
        if (withViews) {
            mController.initWorkspaceView(mWorkspaceView);
            targetView = mHelper.getView(target);
            sourceView = mHelper.getView(source);

            assertNotNull(targetView);
            assertNotNull(sourceView);
        }

        // Connect source after target. No prior connection to bump or splice.
        mController.connect(source, source.getPreviousConnection(),
                target.getNextConnection());

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
        BlockView targetView = null, tailView = null, sourceView = null;

        tail.getPreviousConnection().connect(target.getNextConnection());

        mController.addRootBlock(target);
        mController.addRootBlock(source);
        if (withViews) {
            mController.initWorkspaceView(mWorkspaceView);
            targetView = mHelper.getView(target);
            tailView = mHelper.getView(tail);
            sourceView = mHelper.getView(source);

            assertNotNull(targetView);
            assertNotNull(tailView);
            assertNotNull(sourceView);
        }

        // Connect source after target, where tail is currently attached, causing a splice.
        mController.connect(source, source.getPreviousConnection(),
                target.getNextConnection());

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
        mController.connect(source, source.getPreviousConnection(),
                target.getNextConnection());

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
        mController.addRootBlock(target);
        mController.addRootBlock(source);
        if (withViews) {
            mController.initWorkspaceView(mWorkspaceView);
        }

        // Run test: Connect source inside target. No prior connection to bump.
        mController.connect(source, source.getPreviousConnection(),
                target.getInputByName("statement input").getConnection());

        assertTrue(mWorkspace.isRootBlock(target));
        assertFalse(mWorkspace.isRootBlock(source));
        assertSame(target, source.getPreviousBlock());

        if (withViews) {
            BlockGroup rootGroup = mHelper.getRootBlockGroup(target);
            assertSame(rootGroup, mHelper.getParentBlockGroup(target));
            assertSame(rootGroup, mHelper.getRootBlockGroup(source));
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
        BlockView targetView = null, tailView = null, sourceView = null;

        // Connect the tail inside target.
        target.getInputByName("statement input").getConnection()
                .connect(tail.getPreviousConnection());

        mController.addRootBlock(target);
        mController.addRootBlock(source);
        if (withViews) {
            mController.initWorkspaceView(mWorkspaceView);
            targetView = mHelper.getView(target);
            tailView = mHelper.getView(tail);
            sourceView = mHelper.getView(source);

            assertNotNull(targetView);
            assertNotNull(tailView);
            assertNotNull(sourceView);
        }

        // Run test: Connect source inside target, where tail is attached, resulting in a splice.
        mController.connect(source, source.getPreviousConnection(),
                target.getInputByName("statement input").getConnection());

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
        testConnect_previousToStatemenBumpRemainder(false);
    }

    public void testConnect_previousToStatemenBumpRemainder_withViews() {
        testConnect_previousToStatemenBumpRemainder(true);
    }

    private void testConnect_previousToStatemenBumpRemainder(boolean withViews) {
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
            sourceView = mHelper.getView(source);
            assertNotNull(sourceView);
        }

        // Connect source inside target, where tail is attached.  Source does not have a next, so
        // this will bump tail back to the root.
        mController.connect(source, source.getPreviousConnection(),
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
        mController.connect(second, second.getOutputConnection(),
                first.getOnlyValueInput().getConnection());
        mController.addRootBlock(first);
        if (withViews) {
            mController.initWorkspaceView(mWorkspaceView);
            assertNotNull(mHelper.getView(first));
            assertNotNull(mHelper.getView(second));
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

    public void testExtractBlockAsRoot_fromNext(boolean withViews) {
        // Configure
        Block first = mBlockFactory.obtainBlock("statement_statement_input", "first block");
        Block second = mBlockFactory.obtainBlock("statement_statement_input", "second block");
        mController.connect(second, second.getPreviousConnection(), first.getNextConnection());
        mController.addRootBlock(first);
        if (withViews) {
            mController.initWorkspaceView(mWorkspaceView);
            assertNotNull(mHelper.getView(first));
            assertNotNull(mHelper.getView(second));
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
}
