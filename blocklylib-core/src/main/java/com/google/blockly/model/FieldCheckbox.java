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
 * Adds a toggleable checkbox to an Input.
 */
public final class FieldCheckbox extends Field<FieldCheckbox.Observer> {
    private boolean mChecked;

    public FieldCheckbox(String name, boolean checked) {
        super(name, TYPE_CHECKBOX);
        mChecked = checked;
    }

    public static FieldCheckbox fromJson(JSONObject json) throws BlockLoadingException {
        String name = json.optString("name");
        if (TextUtils.isEmpty(name)) {
            throw new BlockLoadingException("field_checkbox \"name\" attribute must not be empty.");
        }

        return new FieldCheckbox(name, json.optBoolean("checked", true));
    }

    @Override
    public FieldCheckbox clone() {
        return new FieldCheckbox(getName(), mChecked);
    }

    @Override
    public boolean setFromString(String text) {
        mChecked = Boolean.parseBoolean(text);
        return true;
    }

    /**
     * @return The current state of the checkbox.
     */
    public boolean isChecked() {
        return mChecked;
    }

    /**
     * Sets the state of the checkbox.
     */
    public void setChecked(boolean checked) {
        if (mChecked != checked) {
            mChecked = checked;
            onCheckChanged(checked);
        }
    }

    @Override
    public String getSerializedValue() {
        return mChecked ? "true" : "false";
    }

    private void onCheckChanged(boolean newState) {
        for (int i = 0; i < mObservers.size(); i++) {
            mObservers.get(i).onCheckChanged(this, newState);
        }
    }

    /**
     * Observer for listening to changes to a checkbox field.
     */
    public interface Observer {
        /**
         * Called when the field's checked value changed.
         *
         * @param field The field that changed.
         * @param newState The new state of the checkbox.
         */
        void onCheckChanged(FieldCheckbox field, boolean newState);
    }
}
