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
package com.google.blockly.android.ui;

import android.content.Context;
import android.database.DataSetObserver;
import android.support.annotation.LayoutRes;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.google.blockly.android.control.NameManager;

/**
 * An implementation of {@link ArrayAdapter} that wraps the {@link NameManager.VariableNameManager}
 * to create the variable item views.
 */
public class VariableViewAdapter extends ArrayAdapter<String> {
    private final NameManager mVariableNameManager;

    /**
     * @param variableNameManager The name manager containing the variables.
     * @param context A context for inflating layouts.
     * @param resource The {@link TextView} layout to use when inflating items.
     */
    public VariableViewAdapter(Context context, NameManager variableNameManager,
                               @LayoutRes int resource) {

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

    /**
     * Retrieves the index for the given variable name, creating a new variable if it is not found.
     *
     * @param variableName The name of the variable to retrieve.
     * @return The index of the variable.
     */
    public int getOrCreateVariableIndex(String variableName) {
        int count = mVariableNameManager.size();
        for (int i = 0; i < count; i++) {
            if (variableName.equalsIgnoreCase(getItem(i))) {
                return i;
            }
        }

        // No match found.  Create it.
        mVariableNameManager.addName(variableName);

        // Reindex, finding the new index along the way.
        count = mVariableNameManager.size();
        clear();
        int insertionIndex = -1;
        for (int i = 0; i < count; i++) {
            add(mVariableNameManager.get(i));
            if (variableName.equals(getItem(i))) {
                insertionIndex = i;
            }
        }
        if (insertionIndex == -1) {
            throw new IllegalStateException("Variable not found after add.");
        }

        notifyDataSetChanged();
        return insertionIndex;
    }

    /**
     * Updates the ArrayAdapter internal list with the latest list from the NameManager.
     */
    private void refreshVariables() {
        clear();
        for (int i = 0; i < mVariableNameManager.size(); i++) {
            add(mVariableNameManager.get(i));
        }
        notifyDataSetChanged();
    }
}
