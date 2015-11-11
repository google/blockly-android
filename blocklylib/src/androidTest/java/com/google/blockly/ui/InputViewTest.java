package com.google.blockly.ui;

import android.support.annotation.NonNull;
import android.test.suitebuilder.annotation.SmallTest;
import android.view.View;

import com.google.blockly.MockitoAndroidTestCase;
import com.google.blockly.model.Input;

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

        final View mockView = Mockito.mock(View.class);
        inputView.setChildView(mockView);
        assertSame(mockView, inputView.getChildView());
        assertEquals(1, inputView.getChildCount());
    }

    // Verify child view can be set, unset, then set again.
    public void testUnsetChildView() {
        final InputView inputView = makeDefaultInputView();

        final View mockView = Mockito.mock(View.class);
        inputView.setChildView(mockView);
        inputView.unsetChildView();
        assertNull(inputView.getChildView());
        assertEquals(0, inputView.getChildCount());

        inputView.setChildView(mockView);
        assertSame(mockView, inputView.getChildView());
        assertEquals(1, inputView.getChildCount());
    }

    // Verify exception is thrown when calling setChildView with null (must use unsetChildView).
    public void testSetChildViewNull() {
        final InputView inputView = makeDefaultInputView();

        // TODO(rohlfingt): find a way to do this using @Rule and ExpectedException (not working
        // with current test runner(s).
        try {
            inputView.setChildView(null);
        } catch (IllegalArgumentException expected) {
            return;
        }

        fail("Expected IllegalArgumentException not thrown.");
    }

    // Verify exception is thrown when calling setChildView repeatedly without unsetChildView.
    public void testSetChildViewMustUnset() {
        final InputView inputView = makeDefaultInputView();

        final View mockView = Mockito.mock(View.class);
        inputView.setChildView(mockView);

        try {
            inputView.setChildView(mockView);
        } catch (IllegalStateException expected) {
            return;
        }

        fail("Expected IllegalStateException not thrown.");
    }

    @NonNull
    private InputView makeDefaultInputView() {
        return new InputView(
                getContext(), 0 /* blockStyle */, mMockInput, mMockWorkspaceHelper);
    }
}
