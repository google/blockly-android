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
 * Adds a variable to an Input.
 */
public final class FieldVariable extends Field<FieldVariable.Observer> {
    private String mVariable;

    public FieldVariable(String name, String variable) {
        super(name, TYPE_VARIABLE);
        mVariable = variable;
    }

    public static FieldVariable fromJson(JSONObject json) throws BlockLoadingException {
        String name = json.optString("name");
        if (TextUtils.isEmpty(name)) {
            throw new BlockLoadingException("field_variable \"name\" attribute must not be empty.");
        }
        return new FieldVariable(name, json.optString("variable", "item"));
    }

    @Override
    public FieldVariable clone() {
        return new FieldVariable(getName(), mVariable);
    }

    @Override
    public boolean setFromString(String text) {
        if (TextUtils.isEmpty(text)) {
            return false;
        }
        setVariable(text);
        return true;
    }

    /**
     * @return The name of the variable that is set.
     */
    public String getVariable() {
        return mVariable;
    }

    /**
     * Sets the variable in this field. All variables are considered global and must be unique.
     * Two variables with the same name will be considered the same variable at generation.
     */
    public void setVariable(String variable) {
        if ((mVariable == null && variable != null)
                || (mVariable != null && !mVariable.equalsIgnoreCase(variable))) {
            String oldVar = mVariable;
            mVariable = variable;
            onVariableChanged(this, oldVar, variable);
        }
    }

    @Override
    public String getSerializedValue() {
        return mVariable;
    }

    private void onVariableChanged(com.google.blockly.model.FieldVariable field, String oldVar, String newVar) {
        for (int i = 0; i < mObservers.size(); i++) {
            mObservers.get(i).onVariableChanged(field, oldVar, newVar);
        }
    }

    /**
     * Observer for listening to changes to a variable field.
     */
    public interface Observer {
        /**
         * Called when the field's variable name changed.
         *
         * @param field The field that changed.
         * @param oldVar The field's previous variable name.
         * @param newVar The field's new variable name.
         */
        void onVariableChanged(com.google.blockly.model.FieldVariable field, String oldVar, String newVar);
    }
}
