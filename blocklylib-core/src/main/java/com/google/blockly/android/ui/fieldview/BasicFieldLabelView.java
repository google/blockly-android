/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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
import android.widget.TextView;

import com.google.blockly.model.Field;

/**
 * Renders text as part of a BlockView.
 */
public class BasicFieldLabelView extends TextView implements FieldLabelView {
    protected final Field.FieldLabel mLabelField;

    /**
     * Create a view for the given field using the specified style.
     *
     * @param context The context for creating the view and loading resources.
     * @param labelField The label this view is rendering.
     */
    public BasicFieldLabelView(Context context, Field labelField, int style) {
        super(context, null, style);

        mLabelField = (Field.FieldLabel) labelField;

        setBackground(null);
        setText(mLabelField.getText());
        labelField.setView(this);
    }

    @Override
    public void unlinkModel() {
        mLabelField.setView(null);
        // TODO(#45): Remove model from view. Set mLabelField to null, and handle null cases above.
    }
}
