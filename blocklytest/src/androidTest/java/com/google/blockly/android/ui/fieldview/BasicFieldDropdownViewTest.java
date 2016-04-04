/*
 *  Copyright 2016 Google Inc. All Rights Reserved.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.google.blockly.android.ui.fieldview;

import android.support.annotation.NonNull;
import android.support.v4.util.SimpleArrayMap;

import com.google.blockly.android.MockitoAndroidTestCase;
import com.google.blockly.android.ui.WorkspaceHelper;
import com.google.blockly.model.FieldDropdown;

import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for {@link BasicFieldDropdownView}.
 */
public class BasicFieldDropdownViewTest extends MockitoAndroidTestCase {

    @Mock
    private WorkspaceHelper mMockWorkspaceHelper;

    // Cannot mock final classes.
    private FieldDropdown mFieldDropdown;
    private List<FieldDropdown.Option> mOptions = new ArrayList<>(3);

    @Override
    public void setUp() throws Exception {
        super.setUp();

        mOptions.add(new FieldDropdown.Option("Value1", "Label1"));
        mOptions.add(new FieldDropdown.Option("Value2", "Label2"));
        mOptions.add(new FieldDropdown.Option("Value3", "Label3"));

        mFieldDropdown = new FieldDropdown("FieldCheckbox", mOptions);
        assertNotNull(mFieldDropdown);
        assertEquals(mOptions.size(), mFieldDropdown.getOptionCount());
    }

    // Verify object instantiation.
    public void testInstantiation() {
        mFieldDropdown.setSelectedIndex(2);
        final BasicFieldDropdownView view = makeFieldDropdownView();
        assertSame(mFieldDropdown, view.getField());
        assertEquals(mOptions.size(), view.getCount());
        assertEquals(mFieldDropdown.getSelectedIndex(), view.getSelectedItemPosition());
        assertEquals(mFieldDropdown.getSelectedDisplayName(), view.getSelectedItem().toString());
    }

    // Verify update of field when an item is selected from the dropdown.
    // TODO(#69): Make tests (using Espresso?) to confirm that user interaction has the same
    //            effect as calling FieldDropdown.setSelection().
    public void testUpdateFieldFromView() {
        final BasicFieldDropdownView view = makeFieldDropdownView();

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
        final BasicFieldDropdownView view = makeFieldDropdownView();

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
    protected BasicFieldDropdownView makeFieldDropdownView() {
        BasicFieldDropdownView dropdown = new BasicFieldDropdownView(getContext());
        dropdown.setField(mFieldDropdown);
        return dropdown;
    }
}
