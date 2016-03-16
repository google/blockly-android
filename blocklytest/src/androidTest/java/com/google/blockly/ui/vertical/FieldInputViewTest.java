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

package com.google.blockly.ui.vertical;

import android.support.annotation.NonNull;

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

        mFieldInput = new Field.FieldInput("FieldInput", INIT_TEXT_VALUE);
        assertNotNull(mFieldInput);
    }

    // Verify object instantiation.
    public void testInstantiation() {
        final FieldInputView view = makeFieldInputView();
        assertSame(view, mFieldInput.getView());
        assertEquals(INIT_TEXT_VALUE, view.getText().toString());  // Fails without .toString()
    }

    // Verify setting text in the view propagates to the field.
    public void testViewUpdatesField() {
        final FieldInputView view = makeFieldInputView();
        view.setText(SET_TEXT_VALUE);
        assertEquals(SET_TEXT_VALUE, mFieldInput.getText());
    }

    // Verify setting text in the field propagates to the view.
    public void testFieldUpdatesView() {
        final FieldInputView view = makeFieldInputView();

        mFieldInput.setText(SET_TEXT_VALUE);
        assertEquals(SET_TEXT_VALUE, view.getText().toString());  // Fails without .toString()
    }

    @NonNull
    private FieldInputView makeFieldInputView() {
        FieldInputView view = new FieldInputView(getContext());
        view.onFinishInflate(); // This must be called to register the text change watcher.
        view.setField(mFieldInput);
        return view;
    }
}
