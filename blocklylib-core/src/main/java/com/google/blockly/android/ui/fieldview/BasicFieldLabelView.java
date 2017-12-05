/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.blockly.android.ui.fieldview;

import android.content.Context;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;

import com.google.blockly.model.Field;
import com.google.blockly.model.FieldLabel;

/**
 * Renders text as part of a BlockView.
 */
public class BasicFieldLabelView extends AppCompatTextView implements FieldView {
    protected final Field.Observer mFieldObserver = new Field.Observer() {
        @Override
        public void onValueChanged(Field field, String oldValue, String newValue) {
            setText(newValue);
        }
    };

    protected FieldLabel mLabelField;

    /**
     * Constructs a new {@link BasicFieldLabelView}.
     *
     * @param context The application's context.
     */
    public BasicFieldLabelView(Context context) {
        super(context);
    }

    public BasicFieldLabelView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BasicFieldLabelView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setField(Field field) {
        FieldLabel labelField = (FieldLabel) field;
        if (mLabelField == labelField) {
            return;
        }

        if (mLabelField != null) {
            mLabelField.unregisterObserver(mFieldObserver);
        }
        mLabelField = labelField;
        if (mLabelField != null) {
            setText(mLabelField.getText());
            mLabelField.registerObserver(mFieldObserver);
        } else {
            setText("");
        }
    }

    @Override
    public Field getField() {
        return mLabelField;
    }

    @Override
    public void unlinkField() {
        setField(null);
    }
}
