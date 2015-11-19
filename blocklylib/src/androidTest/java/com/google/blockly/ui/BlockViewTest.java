package com.google.blockly.ui;

import android.support.annotation.NonNull;
import android.test.suitebuilder.annotation.SmallTest;

import com.google.blockly.MockitoAndroidTestCase;
import com.google.blockly.R;
import com.google.blockly.control.ConnectionManager;
import com.google.blockly.model.Block;
import com.google.blockly.model.BlockFactory;

import org.mockito.Mock;
import org.mockito.Mockito;


/**
 * Tests for {@link BlockView}.
 */
@SmallTest
public class BlockViewTest extends MockitoAndroidTestCase {

    @Mock
    private ConnectionManager mMockConnectionManager;

    @Mock
    private WorkspaceHelper mMockWorkspaceHelper;

    @Mock
    private Block mMockBlock;

    @Mock
    private BlockGroup mMockBlockGroup;

    private BlockFactory mBlockFactory;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        mBlockFactory = new BlockFactory(getContext());
        mBlockFactory.loadBlocksFromResource(R.raw.test_blocks);
    }

    // Verify correct object state after construction.
    public void testConstructor() {
        final BlockView blockView = makeBlockView(mMockBlock);

        // Verify Block and BlockView are linked both ways.
        assertSame(mMockBlock, blockView.getBlock());
        Mockito.verify(mMockBlock, Mockito.times(1)).setView(blockView);
    }

    // Verify construction of a BlockView for a Block with inputs.
    public void testConstructorBlockWithInputs() {
        final Block block = mBlockFactory.obtainBlock(
                "test_block_one_input_each_type", "TestBlock");
        assertNotNull(block);

        final BlockView blockView = makeBlockView(block);
        assertSame(block, blockView.getBlock());
        assertSame(blockView, block.getView());

        // One InputView per Input?
        assertEquals(3, blockView.getInputViewCount());

        for (int inputIdx = 0; inputIdx < 3; ++inputIdx) {
            // Each InputView points to an Input?
            assertNotNull(blockView.getInputView(inputIdx).getInput());
            // Each InputView points is a child of the BlockView?
            assertSame(blockView.getInputView(inputIdx), blockView.getChildAt(inputIdx));
            // Each input view points to the correct Input?
            assertSame(block.getInputs().get(inputIdx),
                    blockView.getInputView(inputIdx).getInput());
        }
    }

    // Make a BlockView for the given Block and default mock objects otherwise.
    @NonNull
    private BlockView makeBlockView(Block block) {
        return new BlockView(getContext(), block, mMockWorkspaceHelper, mMockBlockGroup,
                mMockConnectionManager, null);
    }
}
