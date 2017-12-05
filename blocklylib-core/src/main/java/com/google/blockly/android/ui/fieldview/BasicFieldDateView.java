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

import android.content.Context;
import android.support.v7.widget.AppCompatTextView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;

import com.google.blockly.model.Field;
import com.google.blockly.model.FieldDate;

import java.text.DateFormat;

/**
 * Renders a date and a date picker as part of a Block.
 */
public class BasicFieldDateView extends AppCompatTextView implements FieldView {
    private static final DateFormat LOCALIZED_FORMAT = DateFormat.getDateInstance(DateFormat.SHORT);

    protected Field.Observer mFieldObserver = new Field.Observer() {
        @Override
        public void onValueChanged(Field field, String oldValue, String newValue) {
            String dateStr = LOCALIZED_FORMAT.format(mDateField.getDate());
            if (!dateStr.contentEquals(getText())) {
                setText(dateStr);
            }
        }
    };

    protected FieldDate mDateField;

    public BasicFieldDateView(Context context) {
        super(context);
    }

    public BasicFieldDateView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BasicFieldDateView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setBackground(null);

        addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (mDateField != null) {
                    mDateField.setFromString(s.toString());
                }
            }
        });
    }

    @Override
    public void setField(Field field) {
        FieldDate dateField = (FieldDate) field;
        if (mDateField == dateField) {
            return;
        }

        if (mDateField != null) {
            mDateField.unregisterObserver(mFieldObserver);
        }
        mDateField = dateField;
        if (mDateField != null) {
            setText(mDateField.getLocalizedDateString());
            mDateField.registerObserver(mFieldObserver);
        } else {
            setText("");
        }
    }

    @Override
    public Field getField() {
        return mDateField;
    }

    @Override
    public void unlinkField() {
        setField(null);
    }
}
