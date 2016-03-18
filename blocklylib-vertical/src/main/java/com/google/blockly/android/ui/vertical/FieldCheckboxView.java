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

package com.google.blockly.android.ui.vertical;

import android.content.Context;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.google.blockly.model.Field;

/**
 * Renders a checkbox as part of a BlockView.
 */
public class FieldCheckboxView extends CheckBox
        implements com.google.blockly.android.ui.fieldview.FieldCheckboxView {

    private final Field.FieldCheckbox mCheckboxField;

    public FieldCheckboxView(Context context, Field checkboxField) {
        super(context);

        mCheckboxField = (Field.FieldCheckbox) checkboxField;

        setBackground(null);
        setChecked(mCheckboxField.isChecked());
        mCheckboxField.setView(this);

        setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mCheckboxField.setChecked(isChecked);
            }
        });
    }

    @Override
    public void unlinkModel() {
        mCheckboxField.setView(null);
        // TODO(#45): Remove model from view. Set mCheckboxField to null,
        //             and handle null cases above.
    }
}
