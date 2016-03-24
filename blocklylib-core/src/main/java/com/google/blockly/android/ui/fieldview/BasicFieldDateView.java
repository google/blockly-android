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

package com.google.blockly.android.ui.fieldview;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;

import com.google.blockly.model.Field;

/**
 * Renders a date and a date picker as part of a Block.
 */
public class BasicFieldDateView extends TextView implements FieldDateView {
    protected final Field.FieldDate mDateField;

    /**
     * Constructs a new {@link BasicFieldDateView}.
     *
     * @param context The application's context.
     * @param dateField The {@link Field} of type {@link Field#TYPE_DATE} represented.
     */
    public BasicFieldDateView(Context context, Field dateField) {
        super(context);

        mDateField = (Field.FieldDate) dateField;

        setBackground(null);
        setText(mDateField.getDateString());
        dateField.setView(this);

        addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                mDateField.setFromString(s.toString());
            }
        });
    }

    @Override
    public void unlinkModel() {
        mDateField.setView(null);
        // TODO(#45): Remove model from view. Set mDateField to null, and handle null cases above.
    }
}
