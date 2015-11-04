package com.google.blockly.ui.fieldview;

import android.test.AndroidTestCase;

import com.google.blockly.MockitoAndroidTestCase;
import com.google.blockly.model.Field;
import com.google.blockly.ui.WorkspaceHelper;

import org.mockito.Mock;

/**
 * Tests for {@link FieldCheckboxView}.
 */
public class FieldCheckboxViewTest extends MockitoAndroidTestCase {

    @Mock
    private WorkspaceHelper mMockWorkspaceHelper;

    // Cannot mock final classes.
    private Field.FieldCheckbox mFieldCheckbox;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        mFieldCheckbox = new Field.FieldCheckbox("FieldCheckbox", false);
    }

    // Verify object instantiation.
    public void testInstantiation() {
        final FieldCheckboxView view =
                new FieldCheckboxView(getContext(), mFieldCheckbox, mMockWorkspaceHelper);
        assertSame(view, mFieldCheckbox.getView());
    }

    // Verify field object gets updated when view is checked/unchecked.
    public void testFieldUpdates() {
        final FieldCheckboxView view =
                new FieldCheckboxView(getContext(), mFieldCheckbox, mMockWorkspaceHelper);
        assertFalse(mFieldCheckbox.isChecked());
        assertEquals(mFieldCheckbox.isChecked(), view.isChecked());

        view.performClick();
        assertTrue(mFieldCheckbox.isChecked());

        view.performClick();
        assertFalse(mFieldCheckbox.isChecked());
    }
}
