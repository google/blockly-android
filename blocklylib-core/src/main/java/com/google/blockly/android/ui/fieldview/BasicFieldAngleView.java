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
import com.google.blockly.android.ui.fieldview.FieldView;

/**
 * Renders an angle as part of a Block.
 */
public class BasicFieldAngleView extends TextView implements FieldView {
    protected final Field.FieldAngle mAngleField;

    public BasicFieldAngleView(Context context, Field angleField) {
        super(context);

        mAngleField = (Field.FieldAngle) angleField;

        setBackground(null);
        setText(Integer.toString(mAngleField.getAngle()));
        angleField.setView(this);

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

    @Override
    public void unlinkModel() {
        mAngleField.setView(null);
        // TODO(#45): Remove model from view. Set mAngleField to null, and handle null cases above.
    }
}
