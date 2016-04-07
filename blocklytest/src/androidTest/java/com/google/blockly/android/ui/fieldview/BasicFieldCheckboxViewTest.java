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

import com.google.blockly.android.MockitoAndroidTestCase;
import com.google.blockly.android.ui.WorkspaceHelper;
import com.google.blockly.model.FieldCheckbox;

import org.mockito.Mock;

/**
 * Tests for {@link BasicFieldCheckboxView}.
 */
public class BasicFieldCheckboxViewTest extends MockitoAndroidTestCase {
    private FieldCheckbox mFieldCheckbox;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        mFieldCheckbox = new FieldCheckbox("FieldCheckbox", false);
    }

    // Verify field object gets updated when view is checked/unchecked.
    public void testFieldUpdatesFromView() {
        final BasicFieldCheckboxView view = makeFieldCheckboxView();
        assertFalse(mFieldCheckbox.isChecked());
        assertEquals(mFieldCheckbox.isChecked(), view.isChecked());

        view.performClick();
        assertTrue(mFieldCheckbox.isChecked());

        view.performClick();
        assertFalse(mFieldCheckbox.isChecked());
    }

    // Verify that view gets updated if field changes.
    public void testViewUpdatesFromField() {
        final BasicFieldCheckboxView view = makeFieldCheckboxView();
        assertEquals(mFieldCheckbox.isChecked(), view.isChecked());

        mFieldCheckbox.setChecked(true);
        assertTrue(view.isChecked());

        mFieldCheckbox.setChecked(false);
        assertFalse(view.isChecked());

        mFieldCheckbox.setChecked(false);
        assertFalse(view.isChecked());
    }

    @NonNull
    private BasicFieldCheckboxView makeFieldCheckboxView() {
        BasicFieldCheckboxView view = new BasicFieldCheckboxView(getContext());
        view.setField(mFieldCheckbox);
        return view;
    }
}
