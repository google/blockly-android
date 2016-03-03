package com.google.blockly.ui.fieldview;

import android.support.annotation.NonNull;
import android.support.v4.util.SimpleArrayMap;

import com.google.blockly.MockitoAndroidTestCase;
import com.google.blockly.model.Field;
import com.google.blockly.ui.WorkspaceHelper;

import org.mockito.Mock;

/**
 * Tests for {@link FieldDropdownView}.
 */
public class FieldDropdownViewTest extends MockitoAndroidTestCase {

    @Mock
    private WorkspaceHelper mMockWorkspaceHelper;

    // Cannot mock final classes.
    private Field.FieldDropdown mFieldDropdown;
    private SimpleArrayMap<String, String> mNameValueMap = new SimpleArrayMap<>();

    @Override
    public void setUp() throws Exception {
        super.setUp();

        mNameValueMap.put("Label1", "Value1");
        mNameValueMap.put("Label2", "Value2");
        mNameValueMap.put("Label3", "Value3");

        mFieldDropdown = new Field.FieldDropdown("FieldCheckbox", mNameValueMap);
        assertNotNull(mFieldDropdown);
        assertEquals(mNameValueMap.size(), mFieldDropdown.getOptions().size());
    }

    // Verify object instantiation.
    public void testInstantiation() {
        mFieldDropdown.setSelectedIndex(2);
        final FieldDropdownView view = makeFieldDropdownView();
        assertSame(view, mFieldDropdown.getView());
        assertEquals(mNameValueMap.size(), view.getCount());
        assertEquals(mFieldDropdown.getSelectedIndex(), view.getSelectedItemPosition());
        assertEquals(mFieldDropdown.getSelectedDisplayName(), view.getSelectedItem().toString());
    }

    // Verify update of field when an item is selected from the dropdown.
    // TODO(#69): Make tests (using Espresso?) to confirm that user interaction has the same
    //            effect as calling FieldDropdownView.setSelection().
    public void testUpdateFieldFromView() {
        final FieldDropdownView view = makeFieldDropdownView();

        view.setSelection(2);
        assertEquals(view.getSelectedItemPosition(), mFieldDropdown.getSelectedIndex());
        assertEquals(view.getSelectedItem().toString(), mFieldDropdown.getSelectedDisplayName());

        view.setSelection(0);
        assertEquals(view.getSelectedItemPosition(), mFieldDropdown.getSelectedIndex());
        assertEquals(view.getSelectedItem().toString(), mFieldDropdown.getSelectedDisplayName());

        view.setSelection(1);
        assertEquals(view.getSelectedItemPosition(), mFieldDropdown.getSelectedIndex());
        assertEquals(view.getSelectedItem().toString(), mFieldDropdown.getSelectedDisplayName());
    }

    // Test update of view if field selection changes.
    public void testUpdateViewFromField() {
        final FieldDropdownView view = makeFieldDropdownView();

        mFieldDropdown.setSelectedIndex(2);
        assertEquals(mFieldDropdown.getSelectedIndex(), view.getSelectedItemPosition());
        assertEquals(mFieldDropdown.getSelectedDisplayName(), view.getSelectedItem().toString());

        mFieldDropdown.setSelectedIndex(0);
        assertEquals(mFieldDropdown.getSelectedIndex(), view.getSelectedItemPosition());
        assertEquals(mFieldDropdown.getSelectedDisplayName(), view.getSelectedItem().toString());

        mFieldDropdown.setSelectedIndex(1);
        assertEquals(mFieldDropdown.getSelectedIndex(), view.getSelectedItemPosition());
        assertEquals(mFieldDropdown.getSelectedDisplayName(), view.getSelectedItem().toString());
    }

    @NonNull
    private FieldDropdownView makeFieldDropdownView() {
        return new FieldDropdownView(getContext(), mFieldDropdown, mMockWorkspaceHelper);
    }
}
