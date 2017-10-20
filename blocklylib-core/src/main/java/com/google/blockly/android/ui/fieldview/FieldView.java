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

import android.support.annotation.UiThread;
import android.view.View;

import com.google.blockly.model.Field;

/**
 * Describes methods that all views that are representing a Field must implement.
 * <p/>
 * Implementations of {@link FieldView} must extend {@link View} or one of its subclasses.
 */
public interface FieldView {
    /**
     * @return The field represented by this view.
     */
    Field getField();

    /**
     * Sets the {@link Field} model for this view. If null the current field will be disconnected
     * from the view if one is set. The field must be of the appropriate type for the specific view
     * implementation or a {@link ClassCastException} may be thrown.
     *
     * @param field The field backing this view.
     */
    @UiThread
    void setField(Field field);

    /**
     * Disconnect the model from this view.
     */
    @UiThread
    void unlinkField();
}
