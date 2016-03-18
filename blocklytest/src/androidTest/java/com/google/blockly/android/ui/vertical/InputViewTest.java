package com.google.blockly.android.ui.vertical;

import android.support.annotation.NonNull;
import android.test.suitebuilder.annotation.SmallTest;

import com.google.blockly.android.MockitoAndroidTestCase;
import com.google.blockly.model.Input;
import com.google.blockly.android.ui.BlockGroup;
import com.google.blockly.android.ui.WorkspaceHelper;

import org.mockito.Mock;
import org.mockito.Mockito;

/**
 * Tests for {@link InputView}.
 */
@SmallTest
public class InputViewTest extends MockitoAndroidTestCase {

    @Mock
    private Input mMockInput;

    @Mock
    private WorkspaceHelper mMockWorkspaceHelper;

    @Mock
    private VerticalBlocksViewFactory mMockViewFactory;

    // Verify correct object state after construction.
    public void testConstructor() {
        final InputView inputView = makeDefaultInputView();

        // Verify Input and InputView are linked both ways.
        assertSame(mMockInput, inputView.getInput());
        Mockito.verify(mMockInput, Mockito.times(1)).setView(inputView);
    }

    // Verify child view can be set.
    public void testSetChildView() {
        final InputView inputView = makeDefaultInputView();
        assertEquals(0, inputView.getChildCount());

        final BlockGroup mockGroup = Mockito.mock(BlockGroup.class);
        inputView.setConnectedBlockGroup(mockGroup);
        assertSame(mockGroup, inputView.getConnectedBlockGroup());
        assertEquals(1, inputView.getChildCount());
    }

    // Verify child view can be set, unset, then set again.
    public void testUnsetChildView() {
        final InputView inputView = makeDefaultInputView();

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
        final InputView inputView = makeDefaultInputView();

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
        final InputView inputView = makeDefaultInputView();

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
    private InputView makeDefaultInputView() {
        return new InputView(getContext(), mMockViewFactory, mMockInput);
    }
}
