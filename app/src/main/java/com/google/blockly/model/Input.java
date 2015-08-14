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

package com.google.blockly.model;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.json.JSONArray;
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

    /**
     * An input that takes a single value. Must have an
     * {@link Connection#CONNECTION_TYPE_INPUT input connection}.
     */
    public static final String TYPE_VALUE = "input_value";
    /**
     * An input that takes a set of statement blocks. Must have a
     * {@link Connection#CONNECTION_TYPE_NEXT next connection}.
     */
    public static final String TYPE_STATEMENT = "input_statement";
    /**
     * An input that just wraps fields. Has no connections.
     */
    public static final String TYPE_DUMMY = "input_dummy";

    /**
     * This input's fields should be aligned at the left of the block, or the right in a RtL
     * configuration.
     */
    public static final String ALIGN_LEFT = "LEFT";
    /**
     * This input's fields should be aligned at the right of the block, or the left in a RtL
     * configuration.
     */
    public static final String ALIGN_RIGHT = "RIGHT";
    /**
     * This input's fields should be aligned in the center of the block.
     */
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
    private final Connection mConnection;

    private Block mBlock;
    private String mAlign = ALIGN_LEFT;

    /**
     * Creates a new input that can be added to a block.
     *
     * @param name The name of the input. Not for display.
     * @param type The type of the input (value, statement, or dummy).
     * @param align The alignment for fields in this input (left, right, center).
     * @param connection (Optional) The connection for this input, if any..
     */
    public Input(String name, String type, String align, Connection connection) {
        if (TextUtils.isEmpty(type)) {
            throw new IllegalArgumentException("Type may not be empty.");
        }
        mName = name;
        mType = type;
        mConnection = connection;
        if (align != null) {
            mAlign = align;
        }

        if (mConnection != null) {
            mConnection.setInput(this);
        }
    }

    /**
     * Checks if a given type is a known input type.
     *
     * @param type The type to check.
     * @return True if the type is known to be an input type, false otherwise.
     */
    public static boolean isInputType(String type) {
        return INPUT_TYPES.contains(type);
    }

    /**
     * Generate an {@link Input} instance from a JSON definition. The type must be a known input
     * type. If the type is not supported an alternate input type should be used instead.
     *
     * @param json The JSONObject that describes the input.
     * @return An instance of {@link Input} generated from the json.
     */
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
                input = new InputStatement(json);
                break;
            case TYPE_DUMMY:
                input = new InputDummy(json);
                break;
            default:
                throw new IllegalArgumentException("Unknown input type: " + type);
        }
        return input;
    }

    /**
     * Adds all of the given fields to this input.
     *
     * @param fields The fields to add.
     */
    public void addAll(List<Field> fields) {
        mFields.addAll(fields);
    }

    /**
     * Adds a single field to the end of this input.
     *
     * @param field The field to add.
     */
    public void add(Field field) {
        mFields.add(field);
    }

    /**
     * @return The list of fields in this input.
     */
    public List<Field> getFields() {
        return mFields;
    }

    /**
     * Sets the block that is the parent of this input.
     *
     * @param block The block that owns this input.
     */
    public void setBlock(Block block) {
        mBlock = block;
        if (mConnection != null) {
            mConnection.setBlock(block);
        }
    }

    /**
     * @return The block this input belongs to.
     */
    public Block getBlock() {
        return mBlock;
    }

    /**
     * @return The name of this input.
     */
    public String getName() { return mName; }

    /**
     * @return The input's Connection, or null if it is a dummy input.
     */
    public Connection getConnection() {
        return mConnection;
    }

    /**
     * Gets a list of connection checks from JSON. If json does not contain a 'check' field
     * null will be returned instead.
     *
     * @param json The JSON to extract the connection checks from.
     * @param checksKey The key for the checks.
     * @return The set of checks or null.
     */
    @Nullable
    public static String[] getChecksFromJson(JSONObject json, String checksKey) {
        Object checkObj = json.opt(checksKey);
        String[] checks = null;
        if (checkObj == null) {
            // Do nothing, ignore other checks
        } else if (checkObj instanceof JSONArray) {
            JSONArray jsonChecks = (JSONArray) checkObj;
            if (jsonChecks != null) {
                int count = jsonChecks.length();
                checks = new String[count];
                for (int i = 0; i < count; i++) {
                    checks[i] = jsonChecks.optString(i);
                    if (checks[i] == null) {
                        throw new IllegalArgumentException("Malformatted check array in Input.");
                    }
                }
            }
        } else if (checkObj instanceof String) {
            checks = new String[] {(String) checkObj};
        }
        return checks;
    }

    /**
     * An Input that takes a value. This will add an input connection to a Block.
     */
    public static final class InputValue extends Input {

        public InputValue(String name, String align, String[] checks) {
            super(name, TYPE_VALUE, align, new Connection(Connection.CONNECTION_TYPE_INPUT, checks));
        }

        private InputValue(JSONObject json) {
            this(json.optString("name", "NAME"), json.optString("align"),
                    getChecksFromJson(json, "check"));
        }
    }

    /**
     * An input that accepts one or more statement blocks. This will add a wrapped code connection
     * to a Block.
     */
    public static final class InputStatement extends Input {

        public InputStatement(String name, String align, String[] checks) {
            super(name, TYPE_STATEMENT, align,
                    new Connection(Connection.CONNECTION_TYPE_NEXT, checks));
        }

        private InputStatement(JSONObject json) {
            this(json.optString("name", "NAME"), json.optString("align"),
                    getChecksFromJson(json, "check"));
        }
    }

    /**
     * An input that only wraps fields and does not provide its own input connection.
     */
    public static final class InputDummy extends Input {

        public InputDummy(String name, String align) {
            super(name, TYPE_DUMMY, align, null);
        }

        private InputDummy(JSONObject json) {
            this(json.optString("name", "NAME"), json.optString("align"));
        }
    }
}

