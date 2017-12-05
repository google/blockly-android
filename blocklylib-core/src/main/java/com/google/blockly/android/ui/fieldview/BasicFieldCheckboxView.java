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
import android.support.v7.widget.AppCompatCheckBox;
import android.util.AttributeSet;
import android.widget.CompoundButton;

import com.google.blockly.model.Field;
import com.google.blockly.model.FieldCheckbox;

/**
 * Renders a checkbox as part of a BlockView.
 */
public class BasicFieldCheckboxView extends AppCompatCheckBox implements FieldView {
    protected final Field.Observer mFieldObserver = new Field.Observer() {
        @Override
        public void onValueChanged(Field field, String oldStrValue, String newStrValue) {
            setChecked(mCheckboxField.isChecked());
        }
    };

    protected FieldCheckbox mCheckboxField = null;

    public BasicFieldCheckboxView(Context context) {
        super(context);
        initOnCheckedChangeListener();
    }

    public BasicFieldCheckboxView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initOnCheckedChangeListener();
    }

    public BasicFieldCheckboxView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initOnCheckedChangeListener();
    }

    private void initOnCheckedChangeListener() {
        setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mCheckboxField != null) {
                    mCheckboxField.setChecked(isChecked);
                }
            }
        });
    }

    @Override
    public void setField(Field field) {
        FieldCheckbox checkboxField = (FieldCheckbox) field;
        if (mCheckboxField == checkboxField) {
            return;
        }

        if (mCheckboxField != null) {
            mCheckboxField.unregisterObserver(mFieldObserver);
        }
        mCheckboxField = checkboxField;
        if (mCheckboxField != null) {
            setChecked(mCheckboxField.isChecked());
            mCheckboxField.registerObserver(mFieldObserver);
        }
    }

    @Override
    public Field getField() {
        return mCheckboxField;
    }

    @Override
    public void unlinkField() {
        setField(null);
    }
}
