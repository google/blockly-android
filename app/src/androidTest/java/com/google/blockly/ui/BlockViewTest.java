package com.google.blockly.ui;

import android.support.annotation.NonNull;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.google.blockly.control.ConnectionManager;
import com.google.blockly.model.Block;
import com.google.blockly.model.Input;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;


/**
 * Tests for {@link BlockView}.
 */
@SmallTest
public class BlockViewTest extends AndroidTestCase {

    @Mock
    ConnectionManager mMockConnectionManager;

    @Mock
    WorkspaceHelper mMockWorkspaceHelper;

    @Mock
    Block mMockBlock;

    @Mock
    BlockGroup mMockBlockGroup;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        // To solve some issue with Dexmaker.  This allows us to use mockito.
        System.setProperty("dexmaker.dexcache", getContext().getCacheDir().getPath());
        MockitoAnnotations.initMocks(this);
    }

    // Verify correct object state after construction.
    public void testConstructor() {
        final BlockView blockView = makeBlockView(mMockBlock);

        // Verify Block and BlockView are linked both ways.
        assertEquals(mMockBlock, blockView.getBlock());
        Mockito.verify(mMockBlock, Mockito.times(1)).setView(blockView);
    }

    // Verify construction of a BlockView for a Block with inputs.
    public void testConstructorBlockWithInputs() {
        final Block block = makeBlockWithInputs();

        final BlockView blockView = makeBlockView(block);
        assertEquals(block, blockView.getBlock());
        assertEquals(blockView, block.getView());

        // One InputView per Input?
        assertEquals(3, blockView.getInputViewCount());

        for (int inputIdx = 0; inputIdx < 3; ++inputIdx) {
            // Each InputView points to an Input?
            assertNotNull(blockView.getInputView(inputIdx).getInput());
            // Each InputView points is a child of the BlockView?
            assertEquals(blockView.getInputView(inputIdx), blockView.getChildAt(inputIdx));
            // Each input view points to the correct Input?
            assertEquals(block.getInputs().get(inputIdx),
                    blockView.getInputView(inputIdx).getInput());
        }
    }

    // Make a BlockView for the given Block and default mock objects otherwise.
    @NonNull
    private BlockView makeBlockView(Block block) {
        return new BlockView(getContext(), block, mMockWorkspaceHelper,
                mMockBlockGroup, mMockConnectionManager);
    }

    @NonNull
    private Block makeBlockWithInputs() {
        return new Block.Builder("name")
                .addInput(new Input.InputDummy("input0", null))
                .addInput(new Input.InputValue("input1", null, null))
                .addInput(new Input.InputStatement("input2", null, null))
                .build();
    }
}
