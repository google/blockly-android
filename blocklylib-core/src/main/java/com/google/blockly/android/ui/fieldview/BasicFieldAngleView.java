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
import android.content.res.Configuration;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.AppCompatTextView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;

import com.google.blockly.android.BuildConfig;
import com.google.blockly.model.Field;
import com.google.blockly.model.FieldAngle;

/**
 * Renders an angle as part of a Block.
 */
public class BasicFieldAngleView extends AppCompatTextView implements FieldView {
    private static final char DEGREE_SYMBOL = '\u00B0';

    /** Whether the degree symbol comes before the numeric text, the RTL standard. */
    protected boolean mDegreeAtStart = false;

    protected Field.Observer mFieldObserver = new Field.Observer() {
        @Override
        public void onValueChanged(Field angleField, String oldValue, String newValue) {
            if (BuildConfig.DEBUG && !(angleField == mAngleField)) {
                throw new AssertionError(
                        String.format("angleField (%s) must match mAngleField (%s)",
                            angleField, mAngleField));
            }

            String curDisplayText = removeSymbol(getText().toString());
            if (!newValue.contentEquals(curDisplayText)) {
                setRawValue(newValue);
            }
        }
    };

    protected FieldAngle mAngleField = null;

    public BasicFieldAngleView(Context context) {
        super(context);
        init();
    }

    public BasicFieldAngleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BasicFieldAngleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Override
    public void setField(Field field) {
        FieldAngle angleField = (FieldAngle) field;
        if (mAngleField == angleField) {
            return;
        }

        if (mAngleField != null) {
            mAngleField.unregisterObserver(mFieldObserver);
        }
        mAngleField = angleField;
        if (mAngleField != null) {
            setRawValue(Float.toString(mAngleField.getAngle()));
            mAngleField.registerObserver(mFieldObserver);
        } else {
            setText("");
        }
    }

    @Override
    public Field getField() {
        return mAngleField;
    }

    @Override
    public void unlinkField() {
        setField(null);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateDegreeOnStart();
    }

    /**
     * Assigns the text value from String value acquire from the field, appending or prepending the
     * degree symbol.
     * @param value The string value without the degree symbol.
     */
    private void setRawValue(String value) {
        if (mDegreeAtStart) {
            setText(DEGREE_SYMBOL + value);
        } else {
            setText(value + DEGREE_SYMBOL);
        }
    }

    /**
     * Initialize the view, including degree symbol placement and text change listener.
     */
    private void init() {
        updateDegreeOnStart();

        // Initialize text watcher
        addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (mAngleField != null) {
                    mAngleField.setFromString(s.toString());
                }
            }
        });
    }

    /**
     * Updates whether the degree symbol should be before the numeric value.
     */
    private void updateDegreeOnStart() {
        mDegreeAtStart = ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL;
    }

    /**
     * Removes the degree symbol from the beginning or end of the text, if any.
     * @param value The value as a string, with possible degree symbol.
     * @return The string value without the degree symbol.
     */
    private static String removeSymbol(String value) {
        int len = value.length();

        // Trim the degree symbol
        if (len > 0){
            if (value.charAt(0) == DEGREE_SYMBOL) {
                value = value.substring(1);
            } else if (value.charAt(len - 1) == DEGREE_SYMBOL) {
                value = value.substring(0, len - 1);
            }
        }

        return value;
    }
}
