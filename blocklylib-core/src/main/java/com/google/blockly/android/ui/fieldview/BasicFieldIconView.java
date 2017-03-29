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
import android.util.AttributeSet;

import com.google.blockly.model.Field;

/**
 * Renders an image bitmap.
 */
public class BasicFieldIconView extends android.support.v7.widget.AppCompatImageView implements
        FieldView {
    private static String TAG = "BasicFieldMutatorView";

    /**
     * Constructs a new {@link BasicFieldIconView}.
     *
     * @param context The application's context.
     */
    public BasicFieldIconView(Context context) {
        this(context, null);
    }

    /**
     * Constructs a new {@link BasicFieldIconView}.
     *
     * @param context The application's context.
     * @param attrs Attributes from the layout/theme.
     */
    public BasicFieldIconView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public Field getField() {
        return null;
    }

    @Override
    public void setField(Field field) {

    }

    @Override
    public void unlinkField() {

    }
}
