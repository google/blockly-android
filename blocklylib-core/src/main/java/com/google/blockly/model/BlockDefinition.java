/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.google.blockly.utils.BlockLoadingException;
import com.google.blockly.utils.ColorUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Definition of a type of block, usually defined in JSON.  See
 * <a href="https://developers.google.com/blockly/guides/create-custom-blocks/define-blocks">the
 * guide on block definitions</a>for details.
 */
public final class BlockDefinition {
    private static final String TAG = "BlockDefinition";

    public static boolean isValidType(String typeName) {
        return !TextUtils.isEmpty(typeName);
    }

    // TODO(#542): Parse into List<InputDefinition> and discard JSON. Include FieldDropdown.Options.
    private final @NonNull JSONObject mJson;  // Saved to parse inputs and field at creation time.
    private final @Nullable JSONObject mStyle; // Ideally, this would be immutable.
    private final @NonNull String mTypeName;
    private final int mColor;
    private final boolean mHasOutput;
    private final boolean mHasPrevious;
    private final boolean mHasNext;
    private final @Nullable String[] mOutputChecks;
    private final @Nullable String[] mPreviousChecks;
    private final @Nullable String[] mNextChecks;
    private final boolean mInputsInlineDefault;

    private final @Nullable String mMutatorName;
    private final @NonNull List<String> mExtensionNames;

    /**
     * Initializes the definition from a string of JSON.
     * @param jsonStr The JSON definition as a string.
     * @throws JSONException If JSON is malformed or does not include expected attributes.
     */
    public BlockDefinition(String jsonStr) throws BlockLoadingException {
        this(parseJsonOrThrow(jsonStr));
    }

    /**
     * Initializes the definition from a JSON object.
     * @param json The JSON object with the definition.
     * @throws BlockLoadingException If JSON does not include expected attributes.
     */
    public BlockDefinition(JSONObject json) throws BlockLoadingException {
        mJson = json;

        // Validate or create type id.
        String tmpName = mJson.optString("type");
        String logPrefix = "";
        if (tmpName == null) {
            // Generate definition name that will be consistent across runs
            int jsonHash = json.toString().hashCode();
            tmpName = "auto-" + Integer.toHexString(jsonHash);
        } else if (isValidType(tmpName)) {
            logPrefix = "Type \"" + tmpName + "\": ";
        } else {
            String valueQuotedAndEscaped = JSONObject.quote(tmpName);
            throw new BlockLoadingException("Invalid block type name: " + valueQuotedAndEscaped);
        }
        mTypeName = tmpName;

        try {
            // A block can have either an output connection or previous connection, but it can always
            // have a next connection.
            mHasOutput = mJson.has("output");
            mHasPrevious = mJson.has("previousStatement");
            mHasNext = mJson.has("nextStatement");
            if (mHasOutput && mHasPrevious) {
                throw new BlockLoadingException(
                        logPrefix + "Block cannot have both \"output\" and \"previousStatement\".");
            }
            // Each connection may have a list of allow connection checks / types.
            mOutputChecks = mHasOutput ? Input.getChecksFromJson(mJson, "output") : null;
            mPreviousChecks = mHasPrevious ? Input.getChecksFromJson(mJson, "previousStatement") : null;
            mNextChecks = mHasNext ? Input.getChecksFromJson(mJson, "nextStatement") : null;

            mColor = parseColour(logPrefix, mJson);
            mInputsInlineDefault = parseInputsInline(logPrefix, mJson);

            mMutatorName = json.has("mutator") ? json.getString("mutator") : null;
            mExtensionNames = parseExtensions(json);
        } catch (JSONException e) {
            throw new BlockLoadingException(
                    "Cannot load BlockDefinition \"" + mTypeName + "\" from JSON.", e);
        }

        mStyle = mJson.optJSONObject("style");
    }

    /**
     * This is the identifying string used in the "type" attributes within JSON and XML.
     * @return The identifying name of the block definition, referenced by XML and
     *         {@link BlockTemplate}s.
     */
    public String getTypeName() {
        return mTypeName;
    }

    /**
     * @return The opaque {@link android.graphics.Color} int of block.
     */
    public int getColor() {
        return mColor;
    }

    /**
     * @return A new output connection for a new block of this type, or null if the block should not
     *         have an output connection.
     */
    @Nullable
    protected Connection createOutputConnection() {
        return !mHasOutput ? null :
                new Connection(Connection.CONNECTION_TYPE_OUTPUT, mOutputChecks);
    }

    /**
     * @return A new previous connection for a new block of this type, or null if the block should
     *         not have an previous connection.
     */
    @Nullable
    protected Connection createPreviousConnection() {
        return !mHasPrevious ? null :
                new Connection(Connection.CONNECTION_TYPE_PREVIOUS, mPreviousChecks);
    }

    /**
     * @return A new next connection for a new block of this type, or null if the block should not
     *         have an next connection.
     */
    @Nullable
    protected Connection createNextConnection() {
        return !mHasNext ? null :
                new Connection(Connection.CONNECTION_TYPE_NEXT, mNextChecks);
    }

    /**
     * @return A new list of {@link Input} objects for a new block of this type, complete with
     *         fields.
     */
    protected ArrayList<Input> createInputList(BlockFactory factory) throws BlockLoadingException {
        ArrayList<Input> inputs = new ArrayList<>();
        ArrayList<Field> fields = new ArrayList<>();
        for (int i = 0; ; i++) {
            String messageKey = "message" + i;
            String argsKey = "args" + i;
            String lastDummyAlignKey = "lastDummyAlign" + i;
            if (!mJson.has(messageKey)) {
                break;
            }
            String message = mJson.optString(messageKey);
            JSONArray args = mJson.optJSONArray(argsKey);
            if (args == null) {
                // If there's no args for this message use an empty array.
                args = new JSONArray();
            }

            if (message.matches("^%[a-zA-Z][a-zA-Z_0-9]*$")) {
                // TODO(#83): load the message from resources.
            }
            // Split on all argument indices of the form "%N" where N is a number from 1 to
            // the number of args. Arguments indices are returned as "%N" strings.
            List<String> tokens = Block.tokenizeMessage(message);
            int indexCount = 0;
            // Indices start at 1, make the array 1 bigger so we don't have to offset things
            boolean[] seenIndices = new boolean[args.length() + 1];

            for (String token : tokens) {
                // Check if this token is an argument index of the form "%N"
                if (token.matches("^%\\d+$")) {
                    int index = Integer.parseInt(token.substring(1));
                    if (index < 1 || index > args.length()) {
                        throw new BlockLoadingException("Message index " + index
                                + " is out of range.");
                    }
                    if (seenIndices[index]) {
                        throw new BlockLoadingException(("Message index " + index
                                + " is duplicated"));
                    }
                    seenIndices[index] = true;

                    JSONObject element;
                    try {
                        element = args.getJSONObject(index - 1);
                    } catch (JSONException e) {
                        throw new BlockLoadingException("Error reading arg %" + index, e);
                    }
                    while (element != null) {
                        String elementType = element.optString("type");
                        if (TextUtils.isEmpty(elementType)) {
                            throw new BlockLoadingException("No type for arg %" + index);
                        }

                        if (Field.isFieldType(elementType)) {
                            fields.add(factory.loadFieldFromJson(mTypeName, element));
                            break;
                        } else if (Input.isInputType(elementType)) {
                            Input input = Input.fromJson(element, fields);
                            fields.clear();
                            inputs.add(input);
                            break;
                        } else {
                            // Try getting the fallback block if it exists
                            Log.w(TAG, "Unknown element type: " + elementType);
                            element = element.optJSONObject("alt");
                        }
                    }
                } else {
                    token = token.replace("%%", "%").trim();
                    if (!TextUtils.isEmpty(token)) {
                        fields.add(new FieldLabel(null, token));
                    }
                }
            }

            // Verify every argument was used
            for (int j = 1; j < seenIndices.length; j++) {
                if (!seenIndices[j]) {
                    throw new BlockLoadingException("Argument " + j + " was never used.");
                }
            }
            // If there were leftover fields we need to add a dummy input to hold them.
            if (fields.size() != 0) {
                String align = mJson.optString(lastDummyAlignKey, Input.ALIGN_LEFT_STRING);
                Input input = new Input.InputDummy(null, fields, align);
                inputs.add(input);
                fields.clear();
            }
        }

        return  inputs;
    }

    /**
     * @return True if new blocks begin with inputs inlined. Otherwise, false.
     */
    public boolean isInputsInlineDefault() {
        return mInputsInlineDefault;
    }

    /**
     * @return The name of the {@link BlockExtension} (if any) which will add a {@link Mutator}
     *         to all Block instances of this type. Otherwise, null.
     */
    @Nullable
    public String getMutatorId() {
        return mMutatorName;
    }

    /**
     * @return A list of extension names to apply to all instances of this BlockDefinition.
     */
    @NonNull
    public List<String> getExtensionNames() {
        return mExtensionNames;
    }

    /**
     * @return The style definition JSON for this block type.
     */
    @Nullable
    public JSONObject getStyleJson() {
        return mStyle;
    }

    /**
     * Attempts to parse a string as JSON.
     * @throws BlockLoadingException If the string is not valid JSON.
     */
    private static final JSONObject parseJsonOrThrow(String jsonStr) throws BlockLoadingException {
        try {
            return new JSONObject(jsonStr);
        } catch (JSONException e) {
            throw new BlockLoadingException(e);
        }
    }

    /**
     * Parses the JSON "colour" attribute.
     * @param warningPrefix A prefix for errors, including the block type if available.
     * @param json The JSON representation of the block definition.
     * @return The int representation of the opaque color.
     */
    private static int parseColour(String warningPrefix, JSONObject json) {
        int color = ColorUtils.DEFAULT_BLOCK_COLOR;
        if (json.has("colour")) {
            boolean validColor = false;
            try {
                String colourString = json.getString("colour");
                color = ColorUtils.parseColor(colourString, null, -1);
                validColor = (color != -1);
            } catch (JSONException e) {
                // Not a string.
            }

            if (!validColor) {
                Log.w(TAG, warningPrefix + "Invalid colour: " + json.opt("colour"));
                color = ColorUtils.DEFAULT_BLOCK_COLOR;
            }
        }
        return color;
    }

    /**
     * Parses the JSON "inputsInline" attribute.
     * @param warningPrefix A prefix for errors, including the block type if available.
     * @param json The JSON representation of the block definition.
     * @return True if blocks of this type should inlined. Otherwise, false.
     */
    private static boolean parseInputsInline(String warningPrefix, JSONObject json) {
        boolean inputsInline = false;
        if (json.has("inputsInline")) {
            try {
                inputsInline = json.getBoolean("inputsInline");
            } catch (JSONException e) {
                // Not a boolean.
                Log.w(TAG, warningPrefix + "Invalid inputsInline: " + json.opt("inputsInline"));
                // Leave value false.
            }
        }
        return inputsInline;
    }

    /**
     * @param json The JSON block definition.
     * @return A list of extension names if provided. Otherwise, null.
     * @throws BlockLoadingException If "extensions" attribute is not an array.
     * @throws JSONException If {@code json}'s {@code "extensions"} is not an array of strings.
     */
    private List<String> parseExtensions(JSONObject json)
            throws JSONException, BlockLoadingException {
        if (!json.has("extensions")) {
            return Collections.<String>emptyList();
        }

        // While we expect an array, permissively grab an Object in case it is just a string.
        Object extensionsObj = json.get("extensions");
        if (extensionsObj instanceof JSONArray) {
            JSONArray array = (JSONArray) extensionsObj;
            int count = array.length();
            List<String> extensionNames = new ArrayList<>(count);
            for (int i = 0; i < count; ++i) {
                extensionNames.add(array.getString(i).trim());
            }
            return Collections.unmodifiableList(extensionNames);
        } else if (extensionsObj instanceof String) {
            Log.w(TAG,
                    "Type \"" + mTypeName + "\": Extensions attribute in JSON should be an array.");
            return Collections.unmodifiableList(
                    Collections.singletonList((String) extensionsObj));
        } else {
            throw new BlockLoadingException(
                    "Type \"" + mTypeName + "\": Extensions attribute in JSON expected an array, "
                    + "and found " + extensionsObj.getClass().getName());
        }
    }
}
