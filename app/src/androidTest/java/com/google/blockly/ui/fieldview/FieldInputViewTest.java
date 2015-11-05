package com.google.blockly.ui.fieldview;

import android.content.ClipData;
import android.content.ClipDescription;
import android.os.Parcel;
import android.view.DragEvent;

import com.google.blockly.MockitoAndroidTestCase;
import com.google.blockly.model.Field;
import com.google.blockly.ui.WorkspaceHelper;

import org.mockito.Mock;

/**
 * Tests for {@link FieldInputView}.
 */
public class FieldInputViewTest extends MockitoAndroidTestCase {

    private static final String INIT_TEXT_VALUE = "someTextToInitializeInput";
    private static final String SET_TEXT_VALUE = "differentTextToSet";

    @Mock
    private WorkspaceHelper mMockWorkspaceHelper;

    // Cannot mock final classes.
    private Field.FieldInput mFieldInput;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        mFieldInput = new Field.FieldInput("FieldCheckbox", INIT_TEXT_VALUE);
        assertNotNull(mFieldInput);
    }

    // Verify object instantiation.
    public void testInstantiation() {
        final FieldInputView view =
                new FieldInputView(getContext(), mFieldInput, mMockWorkspaceHelper);
        assertSame(view, mFieldInput.getView());
        assertEquals(INIT_TEXT_VALUE, view.getText().toString());  // Fails without .toString()
    }

    // Verify setting text in the view propagates to the field.
    public void testSetText() {
        final FieldInputView view =
                new FieldInputView(getContext(), mFieldInput, mMockWorkspaceHelper);

        view.setText(SET_TEXT_VALUE);
        assertEquals(SET_TEXT_VALUE, mFieldInput.getText());
    }
}
