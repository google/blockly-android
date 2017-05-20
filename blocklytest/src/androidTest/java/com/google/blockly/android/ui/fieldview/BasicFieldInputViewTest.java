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
import android.support.test.InstrumentationRegistry;

import com.google.blockly.model.FieldInput;

import org.junit.Before;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests for {@link BasicFieldInputView}.
 */
public class BasicFieldInputViewTest {

    private static final String INIT_TEXT_VALUE = "someTextToInitializeInput";
    private static final String SET_TEXT_VALUE = "differentTextToSet";

    // Cannot mock final classes.
    private FieldInput mFieldInput;

    @Before
     public void setUp() throws Exception {
        mFieldInput = new FieldInput("FieldInput", INIT_TEXT_VALUE);
        assertThat(mFieldInput).isNotNull();
    }

    // Verify object instantiation.
    @Test
    public void testInstantiation() {
        final BasicFieldInputView view = makeFieldInputView();
        assertThat(mFieldInput).isSameAs(view.getField());
        assertThat(view.getText().toString())
                .isEqualTo(INIT_TEXT_VALUE);  // Fails without .toString()
    }

    // Verify setting text in the view propagates to the field.
    @Test
    public void testViewUpdatesField() {
        final BasicFieldInputView view = makeFieldInputView();
        view.setText(SET_TEXT_VALUE);
        assertThat(mFieldInput.getText()).isEqualTo(SET_TEXT_VALUE);
    }

    // Verify setting text in the field propagates to the view.
    @Test
    public void testFieldUpdatesView() {
        final BasicFieldInputView view = makeFieldInputView();

        mFieldInput.setText(SET_TEXT_VALUE);
        assertThat(view.getText().toString())
                .isEqualTo(SET_TEXT_VALUE);  // Fails without .toString()
    }

    @NonNull
    private BasicFieldInputView makeFieldInputView() {
        BasicFieldInputView view = new BasicFieldInputView(InstrumentationRegistry.getContext());
        view.onFinishInflate(); // This must be called to register the text change watcher.
        view.setField(mFieldInput);
        return view;
    }
}
