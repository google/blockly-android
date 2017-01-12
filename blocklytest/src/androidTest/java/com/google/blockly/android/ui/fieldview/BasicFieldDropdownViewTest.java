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

import com.google.blockly.android.BlocklyTestCase;
import com.google.blockly.android.ui.WorkspaceHelper;
import com.google.blockly.model.FieldDropdown;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link BasicFieldDropdownView}.
 */
public class BasicFieldDropdownViewTest extends BlocklyTestCase {

    private WorkspaceHelper mMockWorkspaceHelper;

    // Cannot mock final classes.
    private FieldDropdown mFieldDropdown;
    private FieldDropdown.Options mOptions;

    @Before
    public void setUp() throws Exception {
        configureForUIThread();
        mMockWorkspaceHelper = mock(WorkspaceHelper.class);
        mOptions = new FieldDropdown.Options(Arrays.asList(
                new FieldDropdown.Option("Value1", "Label1"),
                new FieldDropdown.Option("Value2", "Label2"),
                new FieldDropdown.Option("Value3", "Label3")));
        mFieldDropdown = new FieldDropdown("FieldDropdown", mOptions);

        assertThat(mFieldDropdown).isNotNull();
        assertThat(mFieldDropdown.getOptions().size()).isEqualTo(mOptions.size());
    }

    // Verify object instantiation.
    @Test
    public void testInstantiation() {
        mFieldDropdown.setSelectedIndex(2);
        final BasicFieldDropdownView view = makeFieldDropdownView();
        assertThat(mFieldDropdown).isSameAs(view.getField());
        assertThat(view.getCount()).isEqualTo(mOptions.size());
        assertThat(view.getSelectedItemPosition()).isEqualTo(mFieldDropdown.getSelectedIndex());
        assertThat(view.getSelectedItem().toString())
                .isEqualTo(mFieldDropdown.getSelectedDisplayName());
    }

    // Verify update of field when an item is selected from the dropdown.
    // TODO(#69): Make tests (using Espresso?) to confirm that user interaction has the same
    //            effect as calling FieldDropdown.setSelection().
    @Test
    public void testUpdateFieldFromView() {
        final BasicFieldDropdownView view = makeFieldDropdownView();

        view.setSelection(2);
        assertThat(mFieldDropdown.getSelectedIndex()).isEqualTo(view.getSelectedItemPosition());
        assertThat(mFieldDropdown.getSelectedDisplayName())
                .isEqualTo(view.getSelectedItem().toString());

        view.setSelection(0);
        assertThat(mFieldDropdown.getSelectedIndex()).isEqualTo(view.getSelectedItemPosition());
        assertThat(mFieldDropdown.getSelectedDisplayName())
                .isEqualTo(view.getSelectedItem().toString());

        view.setSelection(1);
        assertThat(mFieldDropdown.getSelectedIndex()).isEqualTo(view.getSelectedItemPosition());
        assertThat(mFieldDropdown.getSelectedDisplayName())
                .isEqualTo(view.getSelectedItem().toString());
    }

    // Test update of view if field selection changes.
    @Test
    public void testUpdateViewFromField() {
        final BasicFieldDropdownView view = makeFieldDropdownView();

        mFieldDropdown.setSelectedIndex(2);
        assertThat(view.getSelectedItemPosition()).isEqualTo(mFieldDropdown.getSelectedIndex());
        assertThat(view.getSelectedItem().toString())
                .isEqualTo(mFieldDropdown.getSelectedDisplayName());

        mFieldDropdown.setSelectedIndex(0);
        assertThat(view.getSelectedItemPosition()).isEqualTo(mFieldDropdown.getSelectedIndex());
        assertThat(view.getSelectedItem().toString())
                .isEqualTo(mFieldDropdown.getSelectedDisplayName());

        mFieldDropdown.setSelectedIndex(1);
        assertThat(view.getSelectedItemPosition()).isEqualTo(mFieldDropdown.getSelectedIndex());
        assertThat(view.getSelectedItem().toString())
                .isEqualTo(mFieldDropdown.getSelectedDisplayName());
    }

    @NonNull
    protected BasicFieldDropdownView makeFieldDropdownView() {
        BasicFieldDropdownView dropdown = new BasicFieldDropdownView(getContext());
        dropdown.setField(mFieldDropdown);
        return dropdown;
    }
}
