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

import com.google.blockly.utils.BlockLoadingException;

import org.json.JSONObject;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;

/**
 * Adds an editable text input to an Input.
 */
public final class FieldInput extends Field<FieldInput.Observer> {
    private String mText;

    public FieldInput(String name, String text) {
        super(name, TYPE_INPUT);
        mText = text;
    }

    public static FieldInput fromJson(JSONObject json) throws BlockLoadingException {
        String name = json.optString("name");
        if (TextUtils.isEmpty(name)) {
            throw new BlockLoadingException("field_input \"name\" attribute must not be empty.");
        }
        // TODO: consider replacing default text with string resource
        return new FieldInput(name, json.optString("text", "default"));
    }

    @Override
    public FieldInput clone() {
        return new FieldInput(getName(), mText);
    }

    @Override
    public boolean setFromString(String text) {
        setText(text);
        return true;
    }

    /**
     * @return The text the user has entered.
     */
    public String getText() {
        return mText;
    }

    /**
     * Sets the current text in this Field.
     *
     * @param text The text to replace the field content with.
     */
    public void setText(String text) {
        if (!TextUtils.equals(text, mText)) {
            String oldText = mText;
            mText = text;
            onTextChanged(oldText, text);
        }
    }

    @Override
    public String getSerializedValue() {
        return mText == null ? "" : mText;
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
         *
         * @param field The field that changed.
         * @param oldText The field's previous text.
         * @param newText The field's new text.
         */
        void onTextChanged(FieldInput field, String oldText, String newText);
    }
}
