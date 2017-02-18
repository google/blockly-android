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

import android.text.TextUtils;
import android.util.Log;

import com.google.blockly.utils.BlockLoadingException;
import com.google.blockly.utils.ColorUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Definition of a type of block.
 */
public class BlockDefinition {
    private static final String TAG = "BlockDefinition";

    public static boolean isValidType(String typeName) {
        return !TextUtils.isEmpty(typeName);
    }

    private final JSONObject mJson;
    private final String mTypeName;
    private final int mColor;
    private final boolean mHasOutput;
    private final boolean mHasPrevious;
    private final boolean mHasNext;
    private final String[] mOutputChecks;
    private final String[] mPreviousChecks;
    private final String[] mNextChecks;
    private final boolean mInputsInlineDefault;

    public BlockDefinition(String jsonStr) throws JSONException {
        this(new JSONObject(jsonStr));
    }

    public BlockDefinition(JSONObject json) throws JSONException {
        mJson = json;

        // Validate or create type id.
        String tmpTypeName = mJson.optString("type");
        String warningPrefix = "";
        if (tmpTypeName == null) {
            // Generate definition name that will be consistent across runs
            int jsonHash = json.toString().hashCode();
            tmpTypeName = "auto-" + Integer.toHexString(jsonHash);
        } else if(isValidType(tmpTypeName)) {
            warningPrefix = "Type \"" + tmpTypeName + "\"";
        } else {
            String valueQuotedAndEscaped = new JSONStringer().value(tmpTypeName).toString();
            throw new IllegalArgumentException("Invalid type name: " + valueQuotedAndEscaped);
        }
        mTypeName = tmpTypeName;

        // A block can have either an output connection or previous connection, but it can always
        // have a next connection.
        mHasOutput = mJson.has("output");
        mHasPrevious = mJson.has("previousStatement");
        mHasNext = mJson.has("nextStatement");
        if (mHasOutput && mHasPrevious) {
            throw new IllegalArgumentException(
                    warningPrefix + "Block cannot have both \"output\" and \"previousStatement\".");
        }
        mOutputChecks = mHasOutput ? Input.getChecksFromJson(mJson, "output") : null;
        mPreviousChecks = mHasPrevious ? Input.getChecksFromJson(mJson, "previousStatement") : null;
        mNextChecks = mHasNext ? Input.getChecksFromJson(mJson, "nextStatement") : null;

        mColor = parseColour(warningPrefix, mJson);
        mInputsInlineDefault = parseInputsInline(warningPrefix, mJson);
    }

    public String getTypeName() {
        return mTypeName;
    }

    public int getColor() {
        return mColor;
    }

    public Connection createOutputConnection() {
        return new Connection(Connection.CONNECTION_TYPE_OUTPUT, mOutputChecks);
    }

    public Connection createPreviousConnection() {
        return new Connection(Connection.CONNECTION_TYPE_PREVIOUS, mPreviousChecks);
    }

    public Connection createNextConnection() {
        return new Connection(Connection.CONNECTION_TYPE_NEXT, mNextChecks);
    }

    public ArrayList<Input> createInputList(BlockFactory factory) throws BlockLoadingException {
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
            // the number of args without removing them.
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
                            Input input = Input.fromJson(element);
                            input.addAll(fields);
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
                Input input = new Input.InputDummy(null, align);
                input.addAll(fields);
                inputs.add(input);
                fields.clear();
            }
        }

        return  inputs;
    }

    public boolean isInputsInlineDefault() {
        return mInputsInlineDefault;
    }

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
}
