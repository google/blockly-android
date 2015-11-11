package com.google.blockly.ui.fieldview;

import android.support.annotation.NonNull;

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
        final FieldCheckboxView view = makeFieldCheckboxView();
        assertSame(view, mFieldCheckbox.getView());
    }

    // Verify field object gets updated when view is checked/unchecked.
    public void testFieldUpdatesFromView() {
        final FieldCheckboxView view = makeFieldCheckboxView();
        assertFalse(mFieldCheckbox.isChecked());
        assertEquals(mFieldCheckbox.isChecked(), view.isChecked());

        view.performClick();
        assertTrue(mFieldCheckbox.isChecked());

        view.performClick();
        assertFalse(mFieldCheckbox.isChecked());
    }

    // Verify that view gets updated if field changes.
    public void testViewUpdatesFromField() {
        final FieldCheckboxView view = makeFieldCheckboxView();
        assertEquals(mFieldCheckbox.isChecked(), view.isChecked());

        mFieldCheckbox.setChecked(true);
        assertTrue(view.isChecked());

        mFieldCheckbox.setChecked(false);
        assertFalse(view.isChecked());

        mFieldCheckbox.setChecked(false);
        assertFalse(view.isChecked());
    }

    @NonNull
    private FieldCheckboxView makeFieldCheckboxView() {
        return new FieldCheckboxView(getContext(), mFieldCheckbox, mMockWorkspaceHelper);
    }
}
