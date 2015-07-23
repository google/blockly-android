/*
 * Copyright  2015 Google Inc. All Rights Reserved.
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

package com.google.blockly.blocks;

import android.text.TextUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An input on a Blockly block. This generally wraps one or more {@link Field fields}.
 */
public abstract class Input {
    private static final String TAG = "Input";

    public static final String TYPE_VALUE = "input_value";
    public static final String TYPE_STATEMENT = "input_statement";
    public static final String TYPE_DUMMY = "input_dummy";

    public static final String ALIGN_LEFT = "LEFT";
    public static final String ALIGN_RIGHT = "RIGHT";
    public static final String ALIGN_CENTER = "CENTRE";

    /**
     * The list of known input types.
     */
    protected static final Set<String> INPUT_TYPES = new HashSet<>();
    static {
        INPUT_TYPES.add(TYPE_VALUE);
        INPUT_TYPES.add(TYPE_STATEMENT);
        INPUT_TYPES.add(TYPE_DUMMY);
    }

    private final ArrayList<Field> mFields = new ArrayList<>();
    private final String mName;
    private final String mType;
    private final String mCheck;

    private String mAlign = ALIGN_LEFT;

    public Input(String name, String type, String align, String check) {
        if (TextUtils.isEmpty(type)) {
            throw new IllegalArgumentException("Type may not be empty.");
        }
        mName = name;
        mType = type;
        mCheck = check;
        mAlign = align;
    }

    public static boolean isInputType(String type) {
        return INPUT_TYPES.contains(type);
    }

    public static Input fromJSON(JSONObject json) {
        String type = null;
        try {
            type = json.getString("type");
        } catch (JSONException e) {
            throw new RuntimeException("Error getting the field type.", e);
        }

        Input input = null;
        switch (type) {
            case TYPE_VALUE:
                input = new InputValue(json);
                break;
            case TYPE_STATEMENT:
                input = new InputValue(json);
                break;
            case TYPE_DUMMY:
                input = new InputValue(json);
                break;

        }
        return input;
    }

    public void addAll(List<Field> fields) {
        mFields.addAll(fields);
    }

    public void add(Field field) {
        mFields.add(field);
    }

    public List<Field> getFields() {
        return mFields;
    }

    public static final class InputValue extends Input {

        public InputValue(String name, String align, String check) {
            super(name, TYPE_VALUE, align, check);
        }

        private InputValue(JSONObject json) {
            super(json.optString("name", "NAME"), TYPE_VALUE, json.optString("align"),
                    json.optString("check"));
        }
    }

    public static final class InputStatement extends Input {

        public InputStatement(String name, String align, String check) {
            super(name, TYPE_STATEMENT, align, check);
        }

        private InputStatement(JSONObject json) {
            super(json.optString("name", "NAME"), TYPE_VALUE, json.optString("align"),
                    json.optString("check"));
        }
    }

    public static final class InputDummy extends Input {

        public InputDummy(String name, String align) {
            super(name, TYPE_STATEMENT, align, null);
        }

        private InputDummy(JSONObject json) {
            super(json.optString("name", "NAME"), TYPE_VALUE, json.optString("align"), null);
        }
    }
}

