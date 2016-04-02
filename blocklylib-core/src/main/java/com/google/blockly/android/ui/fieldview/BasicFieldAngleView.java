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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.TextView;

import com.google.blockly.model.Field;

/**
 * Renders an angle as part of a Block.
 */
public class BasicFieldAngleView extends TextView implements FieldView {
    protected Field.FieldAngle.Observer mFieldObserver = new Field.FieldAngle.Observer() {
        @Override
        public void onAngleChanged(Field field, int oldAngle, int newAngle) {
            setText(Integer.toString(newAngle));
        }
    };

    protected Field.FieldAngle mAngleField = null;

    public BasicFieldAngleView(Context context) {
        super(context);
        initTextWatcher();
    }

    public BasicFieldAngleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initTextWatcher();
    }

    public BasicFieldAngleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initTextWatcher();
    }

    private void initTextWatcher() {
        addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                mAngleField.setFromString(s.toString());
            }
        });
    }

    /**
     * Sets the {@link Field} model for this view, if not null. Otherwise, disconnects the prior
     * field model.
     *
     * @param angleField The angle field model to view.
     */
    public void setField(Field.FieldAngle angleField) {
        if (mAngleField == angleField) {
            return;
        }

        if (mAngleField != null) {
            mAngleField.unregisterObserver(mFieldObserver);
        }
        mAngleField = angleField;
        if (mAngleField != null) {
            setText(Integer.toString(mAngleField.getAngle()));
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
        // TODO(#45): Remove model from view. Set mAngleField to null, and handle null cases above.
    }
}
