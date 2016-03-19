package com.google.blockly.android.ui;

import android.support.annotation.NonNull;
import android.test.suitebuilder.annotation.SmallTest;

import com.google.blockly.android.MockitoAndroidTestCase;
import com.google.blockly.model.Block;
import com.google.blockly.model.BlockFactory;
import com.google.blockly.model.Input;

import org.mockito.Mock;
import org.mockito.Mockito;

/**
 * Tests for {@link AbstractInputView}.
 */
@SmallTest
public class AbstractInputViewTest extends MockitoAndroidTestCase {

    private BlockFactory mBlockFactory;
    private Input mDummyInput;
    //private Input mValueInput;
    //private Input mStatementInput;

    @Mock
    private WorkspaceHelper mMockWorkspaceHelper;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        // Use the BlockFactory to make sure we have real inputs.
        BlockFactory factory =
                new BlockFactory(getContext(), new int[]{com.google.blockly.android.R.raw.test_blocks});
        Block block = factory.obtainBlock("test_block_one_input_each_type", "fake_id");
        mDummyInput = block.getInputs().get(0);
        assertEquals(Input.TYPE_DUMMY, mDummyInput.getType());
        //mValueInput = block.getInputs().get(0);
        //assertEquals(Input.TYPE_VALUE, mValueInput.getType());
        //mStatementInput = block.getInputs().get(0);
        //assertEquals(Input.TYPE_STATEMENT, mStatementInput.getType());
    }

    // Verify correct object state after construction.
    public void testConstructor() {
        final AbstractInputView inputView = makeDefaultInputView();

        // Verify Input and InputView are linked both ways.
        assertSame(mDummyInput, inputView.getInput());
        assertSame(inputView, mDummyInput.getView());
    }

    // Verify child view can be set.
    public void testSetChildView() {
        final AbstractInputView inputView = makeDefaultInputView();
        assertEquals(0, inputView.getChildCount());

        final BlockGroup mockGroup = Mockito.mock(BlockGroup.class);
        inputView.setConnectedBlockGroup(mockGroup);
        assertSame(mockGroup, inputView.getConnectedBlockGroup());
        assertEquals(1, inputView.getChildCount());
    }

    // Verify child view can be set, unset, then set again.
    public void testUnsetChildView() {
        final AbstractInputView inputView = makeDefaultInputView();

        final BlockGroup mockGroup = Mockito.mock(BlockGroup.class);
        inputView.setConnectedBlockGroup(mockGroup);
        inputView.disconnectBlockGroup();
        assertNull(inputView.getConnectedBlockGroup());
        assertEquals(0, inputView.getChildCount());

        inputView.setConnectedBlockGroup(mockGroup);
        assertSame(mockGroup, inputView.getConnectedBlockGroup());
        assertEquals(1, inputView.getChildCount());
    }

    // Verify exception is thrown when calling setChildView with null (must use disconnectBlockGroup).
    public void testSetChildViewNull() {
        final AbstractInputView inputView = makeDefaultInputView();

        // TODO(#68): Do this using @Rule and ExpectedException; not working with current runner(s).
        try {
            inputView.setConnectedBlockGroup(null);
        } catch (IllegalArgumentException expected) {
            return;
        }

        fail("Expected IllegalArgumentException not thrown.");
    }

    // Verify exception is thrown when calling setChildView repeatedly without disconnectBlockGroup.
    public void testSetChildViewMustUnset() {
        final AbstractInputView inputView = makeDefaultInputView();

        final BlockGroup mockView = Mockito.mock(BlockGroup.class);
        inputView.setConnectedBlockGroup(mockView);

        try {
            inputView.setConnectedBlockGroup(mockView);
        } catch (IllegalStateException expected) {
            return;
        }

        fail("Expected IllegalStateException not thrown.");
    }

    @NonNull
    private AbstractInputView makeDefaultInputView() {
        return new AbstractInputView(getContext(), mMockWorkspaceHelper, mDummyInput) {
            @Override
            protected void onLayout(boolean changed, int l, int t, int r, int b) {
                // Fake. Do nothing.
            }
        };
    }
}
