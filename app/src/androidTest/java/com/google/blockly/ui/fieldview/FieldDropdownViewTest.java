package com.google.blockly.ui.fieldview;

import android.support.v4.util.SimpleArrayMap;
import android.util.Log;

import com.google.blockly.MockitoAndroidTestCase;
import com.google.blockly.model.Field;
import com.google.blockly.ui.WorkspaceHelper;

import junit.framework.TestCase;

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
        final FieldDropdownView view =
                new FieldDropdownView(getContext(), mFieldDropdown, mMockWorkspaceHelper);
        assertSame(view, mFieldDropdown.getView());
        assertEquals(mNameValueMap.size(), view.getCount());
        assertEquals(mFieldDropdown.getSelectedIndex(), view.getSelectedItemPosition());
        assertEquals(mFieldDropdown.getSelectedDisplayName(), view.getSelectedItem().toString());
    }

    // Verify update of field when an item is selected from the dropdown.
    public void testUpdateFieldFromView() {
        final FieldDropdownView view =
                new FieldDropdownView(getContext(), mFieldDropdown, mMockWorkspaceHelper);

        Log.d("Test", "setSelection 2");
        view.getChildAt(2).performClick();
        view.setSelection(2);
        assertEquals(view.getSelectedItemPosition(), mFieldDropdown.getSelectedIndex());
        assertEquals(view.getSelectedItem().toString(), mNameValueMap.keyAt(2));

        Log.d("Test", "setSelection 0");
        view.setSelection(0);
        assertEquals(view.getSelectedItemPosition(), mFieldDropdown.getSelectedIndex());
        assertEquals(view.getSelectedItem().toString(), mNameValueMap.keyAt(0));

        Log.d("Test", "setSelection 1");
        view.setSelection(1);
        assertEquals(view.getSelectedItemPosition(), mFieldDropdown.getSelectedIndex());
        assertEquals(view.getSelectedItem().toString(), mNameValueMap.keyAt(1));
    }
}
