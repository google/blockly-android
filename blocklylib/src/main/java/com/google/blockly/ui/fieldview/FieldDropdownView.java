/*
 *  Copyright 2015 Google Inc. All Rights Reserved.
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

package com.google.blockly.ui.fieldview;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.google.blockly.model.Field;
import com.google.blockly.ui.WorkspaceHelper;

/**
 * Renders a dropdown field as part of a Block.
 */
public class FieldDropdownView extends Spinner implements FieldView {
    private final Field.FieldDropdown mDropdownField;

    public FieldDropdownView(Context context, Field dropdownField, WorkspaceHelper helper) {
        super(context);

        mDropdownField = (Field.FieldDropdown) dropdownField;

        ArrayAdapter adapter = new ArrayAdapter<>(context, helper.getSpinnerLayout(),
                mDropdownField.getDisplayNames());
        adapter.setDropDownViewResource(helper.getSpinnerDropDownLayout());
        setAdapter(adapter);

        super.setSelection(mDropdownField.getSelectedIndex());
        mDropdownField.setView(this);
    }

    @Override
    public void setSelection(int position) {
        if (position == getSelectedItemPosition()) {
            return;
        }
        super.setSelection(position);
        mDropdownField.setSelectedIndex(position);
    }

    @Override
    public void unlinkModel() {
        mDropdownField.setView(null);
        // TODO(#45): Remove model from view. Set mDropdownField to null,
        //             and handle null cases above.
    }
}
