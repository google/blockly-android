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

package com.google.blockly.ui.vertical;

import android.content.Context;
import android.content.res.TypedArray;
import android.widget.TextView;

import com.google.blockly.model.Field;
import com.google.blockly.ui.fieldview.FieldView;
import com.google.blockly.ui.vertical.R;

/**
 * Renders text as part of a BlockView.
 */
public class FieldLabelView extends TextView
        implements com.google.blockly.ui.fieldview.FieldLabelView {

    private final Field.FieldLabel mLabelField;

    /**
     * Create a view for the given field using the specified style.
     *
     * @param context The context for creating the view and loading resources.
     * @param labelField The label this view is rendering.
     * @param styleAttributes The style to use on this view.
     */
    public FieldLabelView(Context context, Field labelField, TypedArray styleAttributes) {
        super(context, null, 0);

        mLabelField = (Field.FieldLabel) labelField;

        applyStyle(styleAttributes);

        setBackground(null);
        setText(mLabelField.getText());
        labelField.setView(this);
    }

    private void applyStyle(TypedArray styleAttributes) {
        if (styleAttributes == null) {  // Simplifies testing.
            return;
        }

        int textStyle = styleAttributes.getResourceId(
                R.styleable.BlocklyFieldView_textAppearance, 0);
        if (textStyle != 0) {
            setTextAppearance(getContext(), textStyle);
        }

        int minHeight = (int) styleAttributes.getDimension(
                R.styleable.BlocklyFieldView_fieldMinHeight, 0);
        if (minHeight != 0) {
            setMinimumHeight(minHeight);
        }
    }

    @Override
    public void unlinkModel() {
        mLabelField.setView(null);
        // TODO(#45): Remove model from view. Set mLabelField to null, and handle null cases above.
    }
}
