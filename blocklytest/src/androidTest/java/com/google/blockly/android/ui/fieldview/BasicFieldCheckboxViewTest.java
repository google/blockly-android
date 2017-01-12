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
import com.google.blockly.model.FieldCheckbox;

import org.junit.Before;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests for {@link BasicFieldCheckboxView}.
 */
public class BasicFieldCheckboxViewTest extends BlocklyTestCase {
    private FieldCheckbox mFieldCheckbox;

    @Before
     public void setUp() throws Exception {
        configureForUIThread();
        mFieldCheckbox = new FieldCheckbox("FieldCheckbox", false);
    }

    // Verify field object gets updated when view is checked/unchecked.
    @Test
    public void testFieldUpdatesFromView() {
        final BasicFieldCheckboxView view = makeFieldCheckboxView();
        assertThat(mFieldCheckbox.isChecked()).isFalse();
        assertThat(view.isChecked()).isEqualTo(mFieldCheckbox.isChecked());

        view.performClick();
        assertThat(mFieldCheckbox.isChecked()).isTrue();

        view.performClick();
        assertThat(mFieldCheckbox.isChecked()).isFalse();
    }

    // Verify that view gets updated if field changes.
    @Test
    public void testViewUpdatesFromField() {
        final BasicFieldCheckboxView view = makeFieldCheckboxView();
        assertThat(view.isChecked()).isEqualTo(mFieldCheckbox.isChecked());

        mFieldCheckbox.setChecked(true);
        assertThat(view.isChecked()).isTrue();

        mFieldCheckbox.setChecked(false);
        assertThat(view.isChecked()).isFalse();

        mFieldCheckbox.setChecked(false);
        assertThat(view.isChecked()).isFalse();
    }

    @NonNull
    private BasicFieldCheckboxView makeFieldCheckboxView() {
        BasicFieldCheckboxView view = new BasicFieldCheckboxView(getContext());
        view.setField(mFieldCheckbox);
        return view;
    }
}
