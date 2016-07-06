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

package com.google.blockly.model;

import android.text.TextUtils;

import org.json.JSONObject;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;

/**
 * Adds a text to an Input. This can be used to add text to the block or label
 * another field. The text is not modifiable by the user.
 */
public final class FieldLabel extends Field<FieldLabel.Observer> {
    private String mText;

    public FieldLabel(String name, String text) {
        super(name, TYPE_LABEL);
        mText = text == null ? "" : text;
    }

    public static FieldLabel fromJson(JSONObject json) {
        return new FieldLabel(
                json.optString("name", null),
                json.optString("text", ""));
    }

    @Override
    public FieldLabel clone() {
        return new FieldLabel(getName(), mText);
    }

    /**
     * @return The text for this label.
     */
    public String getText() {
        return mText;
    }

    /**
     * Sets the text for this label. Changes to the label will not be serialized by Blockly and
     * should not be caused by user input. For user editable text fields use
     * {@link FieldInput} instead.
     */
    public void setText(String text) {
        if (!TextUtils.equals(text, mText)) {
            String oldText = mText;
            mText = text;
            onTextChanged(oldText, text);
        }
    }

    @Override
    public boolean setFromString(String text) {
        throw new IllegalStateException("Label field text cannot be set after construction.");
    }

    @Override
    public String getSerializedValue() {
        return ""; // Label fields do not have value.
    }

    private void onTextChanged(String oldText, String newText) {
        for (int i = 0; i < mObservers.size(); i++) {
            mObservers.get(i).onTextChanged(this, oldText, newText);
        }
    }

    /**
     * Observer for listening to changes to an input field.
     */
    public interface Observer {
        /**
         * Called when the field's text changed.
         * <p>
         * Note: Label fields are not expected to be user editable and are not serialized by
         * Blockly's core library.
         *
         * @param field The field that changed.
         * @param oldText The field's previous text.
         * @param newText The field's new text.
         */
        void onTextChanged(FieldLabel field, String oldText, String newText);
    }
}
