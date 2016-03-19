package com.google.blockly.android.ui.vertical;

import android.support.annotation.NonNull;
import android.test.suitebuilder.annotation.SmallTest;

import com.google.blockly.android.MockitoAndroidTestCase;
import com.google.blockly.android.R;
import com.google.blockly.android.control.ConnectionManager;
import com.google.blockly.android.ui.*;
import com.google.blockly.model.Block;
import com.google.blockly.model.BlockFactory;

import org.mockito.Mock;


/**
 * Tests for {@link VerticalBlocksViewFactory}.
 */
@SmallTest
public class VerticalBlocksViewFactoryTest extends MockitoAndroidTestCase {

    private BlockFactory mBlockFactory;
    private VerticalBlocksViewFactory mViewFactory;

    private BlockGroup mBlockGroup;

    @Mock
    private WorkspaceHelper mMockWorkspaceHelper;

    @Mock
    private ConnectionManager mMockConnectionManager;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        mBlockGroup = new BlockGroup(getContext(), mMockWorkspaceHelper);

        mBlockFactory = new BlockFactory(getContext(), new int[]{R.raw.test_blocks});
        mViewFactory = new VerticalBlocksViewFactory(getContext(), mMockWorkspaceHelper);
    }

    // Verify construction of a BlockView for a Block with inputs.
    public void testBuildBlockViewWithInputs() {
        final Block block = mBlockFactory.obtainBlock(
                "test_block_one_input_each_type", "TestBlock");
        final BlockView blockView = makeBlockView(block);
        assertNotNull(block);

        assertSame(block, blockView.getBlock());

        // One InputView per Input?
        assertEquals(3, blockView.getInputViewCount());

        for (int inputIdx = 0; inputIdx < 3; ++inputIdx) {
            // Each InputView points to an Input?
            assertNotNull(blockView.getInputView(inputIdx).getInput());
            // Each InputView is a child of the BlockView?
            assertSame(blockView.getInputView(inputIdx), blockView.getChildAt(inputIdx));
            // Each input view points to the correct Input?
            assertSame(block.getInputs().get(inputIdx),
                    blockView.getInputView(inputIdx).getInput());
        }
    }

    // Make a BlockView for the given Block and default mock objects otherwise.
    @NonNull
    private BlockView makeBlockView(Block block) {
        return mViewFactory.buildBlockViewTree(block, mBlockGroup, mMockConnectionManager, null);
    }
}
