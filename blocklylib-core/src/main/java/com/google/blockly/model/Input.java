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

import android.support.annotation.IntDef;
import android.support.annotation.Nullable;

import com.google.blockly.android.ui.InputView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An input on a Blockly block. This generally wraps one or more {@link Field fields}. An input can
 * be created by calling {@link #fromJson(JSONObject) Input.fromJson} or by creating a new instance
 * of a concrete input class and adding fields to it.
 */
public abstract class Input implements Cloneable {
    private static final String TAG = "Input";

    /**
     * An input that takes a single value. Must have an
     * {@link Connection#CONNECTION_TYPE_INPUT input connection}.
     */
    public static final int TYPE_VALUE = 0;
    public static final String TYPE_VALUE_STRING = "input_value";
    /**
     * An input that takes a set of statement blocks. Must have a
     * {@link Connection#CONNECTION_TYPE_NEXT next connection}.
     */
    public static final int TYPE_STATEMENT = 1;
    public static final String TYPE_STATEMENT_STRING = "input_statement";
    /**
     * An input that just wraps fields. Has no connections.
     */
    public static final int TYPE_DUMMY = 2;
    public static final String TYPE_DUMMY_STRING = "input_dummy";
    /**
     * This input's fields should be aligned at the left of the block, or the right in a RTL
     * configuration.
     */
    public static final int ALIGN_LEFT = 0;
    public static final String ALIGN_LEFT_STRING = "LEFT";
    /**
     * This input's fields should be aligned at the right of the block, or the left in a RTL
     * configuration.
     */
    public static final int ALIGN_RIGHT = 1;
    public static final String ALIGN_RIGHT_STRING = "RIGHT";
    /**
     * This input's fields should be aligned in the center of the block.
     */
    public static final int ALIGN_CENTER = 2;
    public static final String ALIGN_CENTER_STRING = "CENTRE";
    /**
     * The list of known input types.
     */
    protected static final Set<String> INPUT_TYPES = new HashSet<>();

    static {
        INPUT_TYPES.add(TYPE_VALUE_STRING);
        INPUT_TYPES.add(TYPE_STATEMENT_STRING);
        INPUT_TYPES.add(TYPE_DUMMY_STRING);
    }

    private final ArrayList<Field> mFields = new ArrayList<>();
    private final String mName;
    private final Connection mConnection;
    @InputType
    private final int mType;
    @Alignment
    private int mAlign = ALIGN_LEFT;
    private Block mBlock;
    private InputView mView;

    /**
     * Creates a new input that can be added to a block.
     *
     * @param name The name of the input. Not for display.
     * @param type The type of the input (value, statement, or dummy).
     * @param align The alignment for fields in this input (left, right, center).
     * @param connection (Optional) The connection for this input, if any..
     */
    public Input(String name, @InputType int type, @Alignment int align, Connection connection) {
        mName = name;
        mType = type;
        mAlign = align;
        mConnection = connection;

        if (mConnection != null) {
            mConnection.setInput(this);
        }
    }

    /**
     * Creates a new input that can be added to a block.
     *
     * @param name The name of the input. Not for display.
     * @param type The type of the input (value, statement, or dummy).
     * @param alignString The alignment for fields in this input (left, right, center).
     * @param connection (Optional) The connection for this input, if any..
     */
    public Input(String name, @InputType int type, String alignString, Connection connection) {
        this(name, type, stringToAlignment(alignString), connection);
    }

    /**
     * Copies the given Input; leaves the new Input's Block null.
     *
     * @param in The Input to copy.
     */
    private Input(Input in) throws IllegalStateException {
        List<Field> inputFields = in.getFields();
        for (int i = 0; i < inputFields.size(); i++) {
            try {
                mFields.add(inputFields.get(i).clone());
            } catch (CloneNotSupportedException e) {
                throw new IllegalStateException("Error cloning field "
                        + inputFields.get(i).getName() + " in Input " + in.getName() + ".");
            }
        }

        mName = in.getName();
        mType = in.getType();
        // Private copy constructor rather than pure cloning makes it possible to set this final
        // variable properly.
        mConnection = Connection.cloneConnection(in.getConnection());
        if (mConnection != null) {
            mConnection.setInput(this);
        }

        mAlign = in.getAlign();
    }

    @Override
    public Input clone() {
        try {
            return (Input) super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    /**
     * Writes the value of the Input and all of its Fields as a string. By default only fields are
     * written. Subclasses should override this and call {@link #serialize(XmlSerializer, String)}
     * with the correct tag to also serialize any connected blocks.
     *
     * @param serializer The XmlSerializer to write to.
     *
     * @throws IOException
     */
    public void serialize(XmlSerializer serializer) throws IOException {
        serialize(serializer, null);
    }

    /**
     * Writes the value of the Input and all of its Fields as a string. If a tag is given, anything
     * attached to the input's connection will also be serialized.
     *
     * @param serializer The XmlSerializer to write to.
     * @param tag The xml tag to use for wrapping the block connected to the input or null.
     *
     * @throws IOException
     */
    public void serialize(XmlSerializer serializer, @Nullable String tag) throws IOException {
        if (tag != null && getConnection() != null && (getConnection().isConnected()
                || getConnection().getShadowBlock() != null)) {
            serializer.startTag(null, tag)
                    .attribute(null, "name", getName());

            // Serialize the connection's shadow if it has one
            Block block = getConnection().getShadowBlock();
            if (block != null) {
                block.serialize(serializer, false);
            }
            // Then serialize its non-shadow target if it has one
            if (block != getConnection().getTargetBlock()) {
                block = getConnection().getTargetBlock();
                if (block != null) {
                    block.serialize(serializer, false);
                }
            }

            serializer.endTag(null, tag);
        }

        for (int i = 0; i < getFields().size(); i++) {
            getFields().get(i).serialize(serializer);
        }
    }

    /**
     * @return The type of this input.
     */
    @InputType
    public int getType() {
        return mType;
    }

    /**
     * @return The alignment of this input.
     */
    @Alignment
    public int getAlign() {
        return mAlign;
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
     * @return The view that renders this input.
     */
    public InputView getView() {
        return mView;
    }

    /**
     * Sets the view that renders this input.
     */
    public void setView(InputView view) {
        mView = view;
    }

    /**
     * @return The block this input belongs to.
     */
    public Block getBlock() {
        return mBlock;
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
        for (int i = 0; i < mFields.size(); i++) {
            mFields.get(i).setBlock(block);
        }
    }

    /**
     * @return The name of this input.
     */
    public String getName() {
        return mName;
    }

    /**
     * @return The input's Connection, or null if it is a dummy input.
     */
    @Nullable
    public Connection getConnection() {
        return mConnection;
    }

    /**
     * Checks if a given type is a known input type.
     *
     * @param typeString The type to check.
     *
     * @return True if the type is known to be an input type, false otherwise.
     */
    public static boolean isInputType(String typeString) {
        return INPUT_TYPES.contains(typeString);
    }

    /**
     * Generate an {@link Input} instance from a JSON definition. The type must be a known input
     * type. If the type is not supported an alternate input type should be used instead.
     *
     * @param json The JSONObject that describes the input.
     *
     * @return An instance of {@link Input} generated from the json.
     */
    public static Input fromJson(JSONObject json) {
        String type = null;
        try {
            type = json.getString("type");
        } catch (JSONException e) {
            throw new RuntimeException("Error getting the field type.", e);
        }

        switch (type) {
            case TYPE_VALUE_STRING:
                return new InputValue(json);
            case TYPE_STATEMENT_STRING:
                return new InputStatement(json);
            case TYPE_DUMMY_STRING:
                return new InputDummy(json);
            default:
                throw new IllegalArgumentException("Unknown input type: " + type);
        }
    }

    /**
     * Gets a list of connection checks from JSON. If json does not contain a 'check' field
     * null will be returned instead.
     *
     * @param json The JSON to extract the connection checks from.
     * @param checksKey The key for the checks.
     *
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
            checks = new String[]{(String) checkObj};
        }
        return checks;
    }

    /**
     * Convert string representation of field alignment selection to internal integer Id.
     *
     * @param alignString The alignment string, e.g., ALIGN_LEFT_STRING ("LEFT").
     *
     * @return The integer Id representing the given alignment string. If the alignment string is
     * null or does not correspond to a valid alignment, then {@code ALIGN_LEFT} is returned as the
     * default alignment.
     */
    @Alignment
    private static int stringToAlignment(String alignString) {
        if (alignString != null) {
            switch (alignString) {
                default:
                case ALIGN_LEFT_STRING:
                    return ALIGN_LEFT;
                case ALIGN_RIGHT_STRING:
                    return ALIGN_RIGHT;
                case ALIGN_CENTER_STRING:
                    return ALIGN_CENTER;
            }
        }
        return ALIGN_LEFT;
    }

    @Nullable
    public Block getConnectedBlock() {
        return (mConnection == null) ? null : mConnection.getTargetBlock();
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({TYPE_VALUE, TYPE_STATEMENT, TYPE_DUMMY})
    public @interface InputType {
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ALIGN_LEFT, ALIGN_RIGHT, ALIGN_CENTER})
    public @interface Alignment {
    }

    /**
     * An Input that takes a value. This will add an input connection to a Block.
     */
    public static final class InputValue extends Input implements Cloneable {

        public InputValue(String name, String alignString, String[] checks) {
            super(name, TYPE_VALUE, alignString,
                    new Connection(Connection.CONNECTION_TYPE_INPUT, checks));
        }

        public InputValue(String name, @Alignment int align, String[] checks) {
            super(name, TYPE_VALUE, align,
                    new Connection(Connection.CONNECTION_TYPE_INPUT, checks));
        }

        private InputValue(InputValue inv) {
            super(inv);
        }

        private InputValue(JSONObject json) {
            this(json.optString("name", "NAME"), json.optString("align"),
                    getChecksFromJson(json, "check"));
        }

        @Override
        public InputValue clone() {
            return new InputValue(this);
        }

        @Override
        public void serialize(XmlSerializer serializer) throws IOException {
            serialize(serializer, "value");
        }
    }

    /**
     * An input that accepts one or more statement blocks. This will add a wrapped code connection
     * to a Block.
     */
    public static final class InputStatement extends Input implements Cloneable {

        public InputStatement(String name, String alignString, String[] checks) {
            super(name, TYPE_STATEMENT, alignString,
                    new Connection(Connection.CONNECTION_TYPE_NEXT, checks));
        }

        public InputStatement(String name, @Alignment int align, String[] checks) {
            super(name, TYPE_STATEMENT, align,
                    new Connection(Connection.CONNECTION_TYPE_NEXT, checks));
        }

        private InputStatement(InputStatement ins) {
            super(ins);
        }

        private InputStatement(JSONObject json) {
            this(json.optString("name", "NAME"), json.optString("align"),
                    getChecksFromJson(json, "check"));
        }

        @Override
        public InputStatement clone() {
            return new InputStatement(this);
        }

        @Override
        public void serialize(XmlSerializer serializer) throws IOException {
            serialize(serializer, "statement");
        }
    }

    /**
     * An input that only wraps fields and does not provide its own input connection.
     */
    public static final class InputDummy extends Input implements Cloneable {

        public InputDummy(String name, String alignString) {
            super(name, TYPE_DUMMY, alignString, null);
        }

        public InputDummy(String name, @Alignment int align) {
            super(name, TYPE_DUMMY, align, null);
        }

        private InputDummy(InputDummy ind) {
            super(ind);
        }

        private InputDummy(JSONObject json) {
            this(json.optString("name", "NAME"), json.optString("align"));
        }

        @Override
        public InputDummy clone() {
            return new InputDummy(this);
        }
    }
}
