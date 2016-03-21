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
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;

import com.google.blockly.android.MockitoAndroidTestCase;
import com.google.blockly.android.ui.WorkspaceHelper;
import com.google.blockly.model.Field;

import org.mockito.Mock;

/**
 * Tests for {@link BasicFieldVariableView}.
 */
public class BasicFieldVariableViewTest extends MockitoAndroidTestCase {

    @Mock
    private WorkspaceHelper mMockWorkspaceHelper;

    private Field.FieldVariable mFieldVariable;
    private String[] mVariables = new String[] {"var1", "var2", "var3"};
    private SpinnerAdapter mVariableAdapter;


    @Override
    public void setUp() throws Exception {
        super.setUp();

        mFieldVariable = new Field.FieldVariable("FieldVariable", "Var2");
        assertNotNull(mFieldVariable);
        assertEquals("Var2", mFieldVariable.getVariable());

        mVariableAdapter = new ArrayAdapter<String>(getContext(),
                android.R.layout.simple_spinner_item, mVariables);
    }

    // Verify object instantiation.
    public void testInstantiation() {
        final BasicFieldVariableView view = makeFieldVariableView();
        assertSame(view, mFieldVariable.getView());
        assertEquals(mVariables.length, view.getCount());
        assertEquals(mFieldVariable.getVariable().toLowerCase(), view.getSelectedItem().toString());
    }

    // Verify update of field when an item is selected from the dropdown.
    // TODO(#69): need tests (using Espresso?) to confirm that user interaction has the same
    //            effect as calling FieldVariableView.setSelection().
    public void testUpdateFieldFromView() {
        final BasicFieldVariableView view = makeFieldVariableView();

        view.setSelection(2);
        assertEquals(mVariables[2], mFieldVariable.getVariable());
        assertEquals(view.getSelectedItem().toString(), mFieldVariable.getVariable());

        view.setSelection(0);
        assertEquals(mVariables[0], mFieldVariable.getVariable());
        assertEquals(view.getSelectedItem().toString(), mFieldVariable.getVariable());

        view.setSelection(1);
        assertEquals(mVariables[1], mFieldVariable.getVariable());
        assertEquals(view.getSelectedItem().toString(), mFieldVariable.getVariable());
    }

    // Test update of view if variable selection changes.
    public void testUpdateViewFromField() {
        final BasicFieldVariableView view = makeFieldVariableView();

        mFieldVariable.setVariable(mVariables[0]);
        assertEquals(mVariables[0], view.getSelectedItem().toString());

        mFieldVariable.setVariable(mVariables[1]);
        assertEquals(mVariables[1], view.getSelectedItem().toString());

        mFieldVariable.setVariable(mVariables[2]);
        assertEquals(mVariables[2], view.getSelectedItem().toString());
    }

    @NonNull
    private BasicFieldVariableView makeFieldVariableView() {
        return new BasicFieldVariableView(getContext(), mFieldVariable, mVariableAdapter);
    }
}
