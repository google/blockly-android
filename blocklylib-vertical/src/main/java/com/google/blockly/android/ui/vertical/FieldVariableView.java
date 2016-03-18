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

package com.google.blockly.android.ui.vertical;

import android.content.Context;
import android.database.DataSetObserver;
import android.text.TextUtils;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.google.blockly.model.Field;
import com.google.blockly.android.control.NameManager;

/**
 * Renders a dropdown field containing the workspace's variables as part of a Block.
 */
public class FieldVariableView extends Spinner
        implements com.google.blockly.android.ui.fieldview.FieldVariableView {

    private final Field.FieldVariable mVariableField;

    public FieldVariableView(Context context, Field variableField, SpinnerAdapter variablesAdapter) {
        super(context);

        mVariableField = (Field.FieldVariable) variableField;

        setAdapter(variablesAdapter);
        setSelection(mVariableField.getVariable());
        mVariableField.setView(this);
    }

    @Override
    public void setSelection(int position) {
        if (position == getSelectedItemPosition()) {
            return;
        }
        super.setSelection(position);
        mVariableField.setVariable(getAdapter().getItem(position).toString());
    }

    @Override
    public void unlinkModel() {
        mVariableField.setView(null);
        // TODO(#45): Remove model from view. Set mVariableField to null. Handle null cases above.
    }

    /**
     * Set the selection from a variable name, ignoring case. The variable must be a variable in
     * the workspace.
     *
     * @param variableName The name of the variable, ignoring case.
     */
    public void setSelection(String variableName) {
        if (TextUtils.isEmpty(variableName)) {
            throw new IllegalArgumentException("Cannot set an empty variable name.");
        }
        Adapter adapter = getAdapter();
        int size = adapter.getCount();
        if (size == 0) {
            throw new IllegalStateException("Attempted to set variable, but there are none.");
        }
        for (int i = 0; i < size; i++) {
            if (variableName.equalsIgnoreCase(adapter.getItem(i).toString())) {
                setSelection(i);
                return;
            }
        }
        throw new IllegalArgumentException("The variable " + variableName + " does not exist.");
    }

    /**
     * An implementation of {@link ArrayAdapter} that wraps a name manager to create a list of
     * items.
     */
    public static class VariableAdapter extends ArrayAdapter<String> {
        private final NameManager mVariableNameManager;

        /**
         * @param variableNameManager The name manager containing the variables.
         * @param context A context for inflating layouts.
         * @param resource The {@link TextView} layout to use when inflating items.
         */
        public VariableAdapter(NameManager variableNameManager, Context context, int resource) {
            super(context, resource);
            mVariableNameManager = variableNameManager;
            refreshVariables();
            variableNameManager.registerObserver(new DataSetObserver() {
                @Override
                public void onChanged() {
                    refreshVariables();
                }
            });
        }

        private void refreshVariables() {
            clear();
            for (int i = 0; i < mVariableNameManager.size(); i++) {
                add(mVariableNameManager.get(i));
            }
        }
    }
}
