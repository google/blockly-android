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
import com.google.blockly.android.control.NameManager;
import com.google.blockly.model.FieldVariable;

import org.junit.Before;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests for {@link BasicFieldVariableView}.
 */
public class BasicFieldVariableViewTest extends BlocklyTestCase {
    private FieldVariable mFieldVariable;
    private String[] mVariables = new String[] {"var1", "var2", "var3"};
    private NameManager mNameManager;
    private BasicFieldVariableView.VariableViewAdapter mVariableAdapter;

    @Before
    public void setUp() throws Exception {
        configureForUIThread();

        mFieldVariable = new FieldVariable("field", "var2");

        mNameManager = new NameManager.VariableNameManager();
        mNameManager.addName("var1");
        mNameManager.addName(mFieldVariable.getVariable());
        mNameManager.addName("var3");

        mVariableAdapter = new BasicFieldVariableView.VariableViewAdapter(
                getContext(), mNameManager, android.R.layout.simple_spinner_item);
    }

    // Verify object instantiation.
    @Test
    public void testInstantiation() {
        final BasicFieldVariableView[] view = new BasicFieldVariableView[1];
        runAndSync(new Runnable() {
            @Override
            public void run() {
                view[0] = makeFieldVariableView();
            }
        });

        assertThat(mFieldVariable).isSameAs(view[0].getField());
        assertThat(view[0].getCount()).isEqualTo(mVariables.length + 2);
        assertThat((String) (view[0].getSelectedItem())).isEqualTo(mFieldVariable.getVariable());
    }

    // Verify update of field when an item is selected from the dropdown.
    // TODO(#69): need tests (using Espresso?) to confirm that user interaction has the same
    //            effect as calling BasicFieldVariableView.setSelection().
    @Test
    public void testUpdateFieldFromView() {
        final BasicFieldVariableView view = makeFieldVariableView();

        runAndSync(new Runnable() {
            @Override
            public void run() {
                view.setSelection(2);
            }
        });
        assertThat(mFieldVariable.getVariable()).isEqualTo(mVariables[2]);
        assertThat(mFieldVariable.getVariable()).isEqualTo(view.getSelectedItem().toString());

        runAndSync(new Runnable() {
            @Override
            public void run() {
                view.setSelection(0);
            }
        });
        assertThat(mFieldVariable.getVariable()).isEqualTo(mVariables[0]);
        assertThat(mFieldVariable.getVariable()).isEqualTo(view.getSelectedItem().toString());

        runAndSync(new Runnable() {
            @Override
            public void run() {
                view.setSelection(1);
            }
        });
        assertThat(mFieldVariable.getVariable()).isEqualTo(mVariables[1]);
        assertThat(mFieldVariable.getVariable()).isEqualTo(view.getSelectedItem().toString());
    }

    // Test update of view if variable selection changes.
    @Test
    public void testUpdateViewFromField() {
        final BasicFieldVariableView view = makeFieldVariableView();

        // Updates complete asynchronously, so wait before testing.
        runAndSync(new Runnable() {
            @Override
            public void run() {
                mFieldVariable.setVariable(mVariables[0]);
            }
        });
        assertThat(view.getSelectedItem().toString()).isEqualTo(mVariables[0]);

        runAndSync(new Runnable() {
            @Override
            public void run() {
                mFieldVariable.setVariable(mVariables[1]);
            }
        });
        assertThat(view.getSelectedItem().toString()).isEqualTo(mVariables[1]);

        runAndSync(new Runnable() {
            @Override
            public void run() {
                mFieldVariable.setVariable(mVariables[2]);
            }
        });
        assertThat(view.getSelectedItem().toString()).isEqualTo(mVariables[2]);
    }

    @NonNull
    private BasicFieldVariableView makeFieldVariableView() {
        BasicFieldVariableView view = new BasicFieldVariableView(getContext());
        view.setAdapter(mVariableAdapter);
        view.setField(mFieldVariable);
        return view;
    }
}
