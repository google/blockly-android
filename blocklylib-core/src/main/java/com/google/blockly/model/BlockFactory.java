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

package com.google.blockly.model;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.google.blockly.android.ui.PendingDrag;
import com.google.blockly.utils.BlockLoadingException;
import com.google.blockly.utils.ColorUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Helper class for building a set of master blocks and then obtaining copies of them for use in
 * a workspace or toolbar.
 */
public class BlockFactory {
    private static final String TAG = "BlockFactory";

    /** Array used for by {@link ColorUtils#parseColor(String, float[], int)} during I/O. **/
    private static final float[] TEMP_IO_THREAD_FLOAT_ARRAY = new float[3];

    private Resources mResources;
    private final HashMap<String, Block> mBlockTemplates = new HashMap<>();
    private final HashMap<String, WeakReference<Block>> mBlockRefs = new HashMap<>();

    /**
     * The global list of dropdown options available to each field matching the
     * {@link BlockTypeFieldName} key.
     */
    protected final HashMap<BlockTypeFieldName, WeakReference<FieldDropdown.Options>>
            mDropDownOptions = new HashMap<>();


    /**
     * Create a factory with an initial set of blocks from json resources.
     *
     * @param context The context for loading resources.
     * @param blockSourceIds A list of JSON resources containing blocks.
     * @throws IllegalStateException if any block definitions fail to load.
     */
    public BlockFactory(Context context, int[] blockSourceIds) {
        this(context);
        if (blockSourceIds != null) {
            for (int i = 0; i < blockSourceIds.length; i++) {
                addBlocks(blockSourceIds[i]);
            }
        }
    }

    /**
     * Create a factory.
     *
     * @param context The context for loading resources.
     */
    public BlockFactory(Context context) {
        mResources = context.getResources();
    }

    public BlockFactory(final InputStream source) throws IOException {
        loadBlocks(source);
    }

    /**
     * Adds a block to the set of blocks that can be created.
     *
     * @param block The master block to add.
     */
    public void addBlockTemplate(Block block) {
        if (mBlockTemplates.containsKey(block.getType())) {
            Log.i(TAG, "Replacing block: " + block.getType());
        }
        mBlockTemplates.put(block.getType(), new Block.Builder(block).build());
    }

    /**
     * Removes a block type from the factory. If the Block is still in use by the workspace this
     * could cause a crash if the user tries to load a new block of this type.
     *
     * @param prototypeName The name of the block to remove.
     *
     * @return The master block that was removed or null if it wasn't found.
     */
    public Block removeBlockTemplate(String prototypeName) {
        return mBlockTemplates.remove(prototypeName);
    }

    /**
     * Creates a block of the specified type using one of the master blocks known to this factory.
     * If the prototypeName is not one of the known block types null will be returned instead.
     *
     * @param prototypeName The name of the block type to create.
     * @param uuid The id of the block if loaded from XML; null otherwise.
     *
     * @return A new block of that type or null.
     */
    public Block obtainBlock(String prototypeName, @Nullable String uuid) {
        // First search for any existing instance
        Block block;
        if (uuid != null) {
            WeakReference<Block> ref = mBlockRefs.get(uuid);
            if (ref != null) {
                block = ref.get();
                if (block != null) {
                    throw new IllegalArgumentException("Block with given UUID \"" + uuid
                            + "\" already exists. Duplicate UUIDs not allowed.");
                }
            }
        }

        // Existing instance not found.  Constructing a new one.
        if (!mBlockTemplates.containsKey(prototypeName)) {
            Log.w(TAG, "Block " + prototypeName + " not found.");
            return null;
        }
        Block.Builder builder = new Block.Builder(mBlockTemplates.get(prototypeName));
        if (uuid != null) {
            builder.setUuid(uuid);
        }
        block = builder.build();
        mBlockRefs.put(block.getId(), new WeakReference<Block>(block));
        return block;
    }

    /**
     * @return The list of known blocks that can be created.
     */
    public List<Block> getAllBlocks() {
        return new ArrayList<>(mBlockTemplates.values());
    }

    /**
     * Loads and adds block templates from a resource.
     *
     * @param resId The id of the JSON resource to load blocks from.
     *
     * @return Number of blocks added to the factory.
     * @throws BlockLoadingException if error occurs when parsing JSON or block definitions.
     */
    public int addBlocks(int resId) {
        InputStream blockIs = mResources.openRawResource(resId);
        try {
            return loadBlocks(blockIs);
        } catch (IOException e) {
            // Compile time resources are expected to always be valid.
            throw new IllegalStateException("Failed to load block defintions from resource: "
                    + mResources.getResourceEntryName(resId));
        }
    }

    /**
     * Loads and adds block templates from a string.
     *
     * @param json_string The JSON string to load blocks from.
     *
     * @return Number of blocks added to the factory.
     * @throws BlockLoadingException if error occurs when parsing JSON or block definitions.
     */
    public int addBlocks(String json_string) throws IOException {
        final InputStream blockIs = new ByteArrayInputStream(json_string.getBytes());
        return loadBlocks(blockIs);
    }


    /**
     * Loads and adds block templates from an input stream.
     *
     * @param is The json stream to read blocks from.
     *
     * @return Number of blocks added to the factory.
     * @throws BlockLoadingException if error occurs when parsing JSON or block definitions.
     */
    public int addBlocks(InputStream is) throws IOException {
        return loadBlocks(is);
    }

    /**
     * Generate a {@link Block} from JSON, including all inputs and fields within the block.
     *
     * @param type The type id of the block.
     * @param json The JSON to generate the block from.
     *
     * @return The generated Block.
     * @throws BlockLoadingException if the json is malformed.
     */
    public Block fromJson(String type, JSONObject json) throws BlockLoadingException {
        if (TextUtils.isEmpty(type)) {
            throw new IllegalArgumentException("Block type may not be null or empty.");
        }
        if (json == null) {
            throw new IllegalArgumentException("Json may not be null.");
        }
        Block.Builder builder = new Block.Builder(type);

        if (json.has("output") && json.has("previousStatement")) {
            throw new BlockLoadingException(
                    "Block cannot have both an output and a previous statement.");
        }

        // Parse any connections that are present.
        if (json.has("output")) {
            String[] checks = Input.getChecksFromJson(json, "output");
            Connection output = new Connection(Connection.CONNECTION_TYPE_OUTPUT, checks);
            builder.setOutput(output);
        } else if (json.has("previousStatement")) {
            String[] checks = Input.getChecksFromJson(json, "previousStatement");
            Connection previous = new Connection(Connection.CONNECTION_TYPE_PREVIOUS, checks);
            builder.setPrevious(previous);
        }
        // A block can have either an output connection or previous connection, but it can always
        // have a next connection.
        if (json.has("nextStatement")) {
            String[] checks = Input.getChecksFromJson(json, "nextStatement");
            Connection next = new Connection(Connection.CONNECTION_TYPE_NEXT, checks);
            builder.setNext(next);
        }
        if (json.has("inputsInline")) {
            try {
                builder.setInputsInline(json.getBoolean("inputsInline"));
            } catch (JSONException e) {
                // Do nothing and it will remain false.
            }
        }

        int blockColor = ColorUtils.DEFAULT_BLOCK_COLOR;
        if (json.has("colour")) {
            try {
                String colourString = json.getString("colour");
                blockColor = ColorUtils.parseColor(colourString, TEMP_IO_THREAD_FLOAT_ARRAY,
                        ColorUtils.DEFAULT_BLOCK_COLOR);
            } catch (JSONException e) {
                // Won't get here. Checked above.
            }
        }
        builder.setColor(blockColor);

        ArrayList<Input> inputs = new ArrayList<>();
        ArrayList<Field> fields = new ArrayList<>();
        for (int i = 0; ; i++) {
            String messageKey = "message" + i;
            String argsKey = "args" + i;
            String lastDummyAlignKey = "lastDummyAlign" + i;
            if (!json.has(messageKey)) {
                break;
            }
            String message = json.optString(messageKey);
            JSONArray args = json.optJSONArray(argsKey);
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
                            fields.add(loadFieldFromJson(type, element));
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
                String align = json.optString(lastDummyAlignKey, Input.ALIGN_LEFT_STRING);
                Input input = new Input.InputDummy(null, align);
                input.addAll(fields);
                inputs.add(input);
                fields.clear();
            }
        }

        builder.setInputs(inputs);
        return builder.build();
    }

    /**
     * Create a new {@link Field} instance from JSON.  If the type is not recognized
     * null will be returned. If the JSON is invalid or there is an error reading the data a
     * {@link RuntimeException} will be thrown.
     *
     * @param blockType The type id of the block within this field will be contained.
     * @param json The JSON to generate the Field from.
     *
     * @return A Field of the appropriate type.
     *
     * @throws RuntimeException
     */
    public Field loadFieldFromJson(String blockType, JSONObject json) throws BlockLoadingException {
        String type = null;
        try {
            type = json.getString("type");
        } catch (JSONException e) {
            throw new BlockLoadingException("Error getting the field type.", e);
        }

        // If new fields are added here FIELD_TYPES should also be updated.
        Field field = null;
        switch (type) {
            case Field.TYPE_LABEL_STRING:
                field = FieldLabel.fromJson(json);
                break;
            case Field.TYPE_INPUT_STRING:
                field = FieldInput.fromJson(json);
                break;
            case Field.TYPE_ANGLE_STRING:
                field = FieldAngle.fromJson(json);
                break;
            case Field.TYPE_CHECKBOX_STRING:
                field = FieldCheckbox.fromJson(json);
                break;
            case Field.TYPE_COLOR_STRING:
                field = FieldColor.fromJson(json);
                break;
            case Field.TYPE_DATE_STRING:
                field = FieldDate.fromJson(json);
                break;
            case Field.TYPE_VARIABLE_STRING:
                field = FieldVariable.fromJson(json);
                break;
            case Field.TYPE_DROPDOWN_STRING:
                field = FieldDropdown.fromJson(json);
                String fieldName = field.getName();
                if (!TextUtils.isEmpty(blockType) && !TextUtils.isEmpty(fieldName)) {
                    updateDropDownOptions(blockType, fieldName, ((FieldDropdown) field).getOptions());
                }
                break;
            case Field.TYPE_IMAGE_STRING:
                field = FieldImage.fromJson(json);
                break;
            case Field.TYPE_NUMBER_STRING:
                field = FieldNumber.fromJson(json);
                break;
            default:
                Log.w(TAG, "Unknown field type.");
                break;
        }
        return field;
    }

    /**
     * Load a block and all of its children from XML.
     *
     * @param parser An XmlPullParser pointed at the start tag of this block.
     *
     * @return The loaded block.
     *
     * @throws XmlPullParserException
     * @throws IOException
     * @throws BlocklyParserException
     */
    public Block fromXml(XmlPullParser parser)
            throws XmlPullParserException, IOException, BlocklyParserException {
        String type = parser.getAttributeValue(null, "type");   // prototype name
        String id = parser.getAttributeValue(null, "id");
        if (type == null || type.isEmpty()) {
            throw new BlocklyParserException("Block was missing a type.");
        }
        // If the id was empty the blockfactory will just generate one.

        Block resultBlock = obtainBlock(type, id);
        if (resultBlock == null) {
            throw new BlocklyParserException("Tried to obtain a block of an unknown type " + type);
        }

        String collapsedString = parser.getAttributeValue(null, "collapsed");
        if (collapsedString != null) {
            resultBlock.setCollapsed(Boolean.parseBoolean(collapsedString));
        }

        String deletableString = parser.getAttributeValue(null, "deletable");
        if (deletableString != null) {
            resultBlock.setDeletable(Boolean.parseBoolean(deletableString));
        }

        String disabledString = parser.getAttributeValue(null, "disabled");
        if (disabledString != null) {
            resultBlock.setDisabled(Boolean.parseBoolean(disabledString));
        }

        String editableString = parser.getAttributeValue(null, "editable");
        if (editableString != null) {
            resultBlock.setEditable(Boolean.parseBoolean(editableString));
        }

        String inputsInlineString = parser.getAttributeValue(null, "inline");
        if (inputsInlineString != null) {
            resultBlock.setInputsInline(Boolean.parseBoolean(inputsInlineString));
        }

        String movableString = parser.getAttributeValue(null, "movable");
        if (movableString != null) {
            resultBlock.setMovable(Boolean.parseBoolean(movableString));
        }

        // Set position.  Only if this is a top level block.
        String x = parser.getAttributeValue(null, "x");
        String y = parser.getAttributeValue(null, "y");
        if (x != null && y != null) {
            resultBlock.setPosition(Integer.parseInt(x), Integer.parseInt(y));
        }

        int eventType = parser.next();
        String text = "";
        String fieldName = "";
        Block childBlock = null;
        Block childShadow = null;
        Input valueInput = null;
        Input statementInput = null;

        while (eventType != XmlPullParser.END_DOCUMENT) {
            String tagname = parser.getName();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    text = ""; // Ignore text from parent (or prior) block.
                    if (tagname.equalsIgnoreCase("block")) {
                        childBlock = fromXml(parser);
                    } else if (tagname.equalsIgnoreCase("shadow")) {
                        childShadow = fromXml(parser);
                    } else if (tagname.equalsIgnoreCase("field")) {
                        fieldName = parser.getAttributeValue(null, "name");
                    } else if (tagname.equalsIgnoreCase("value")) {
                        valueInput = resultBlock.getInputByName(
                                parser.getAttributeValue(null, "name"));
                        if (valueInput == null) {
                            throw new BlocklyParserException("The value input was null at line "
                                    + parser.getLineNumber() + "!");
                        }
                    } else if (tagname.equalsIgnoreCase("statement")) {
                        statementInput = resultBlock.getInputByName(
                                parser.getAttributeValue(null, "name"));
                    } else if (tagname.equalsIgnoreCase("mutation")) {
                        // TODO(fenichel): Handle mutations.
                    }
                    break;

                case XmlPullParser.TEXT:
                    text = parser.getText();
                    break;

                case XmlPullParser.END_TAG:
                    Connection parentConnection = null;

                    if (tagname.equalsIgnoreCase("block")) {
                        if (resultBlock == null) {
                            throw new BlocklyParserException(
                                    "Created a null block. This should never happen.");
                        }
                        return resultBlock;
                    } else if (tagname.equalsIgnoreCase("shadow")) {
                        if (resultBlock == null) {
                            throw new BlocklyParserException(
                                    "Created a null block. This should never happen.");
                        }
                        try {
                            resultBlock.setShadow(true);
                        } catch (IllegalStateException e) {
                            throw new BlocklyParserException(e);
                        }
                        return resultBlock;
                    }else if (tagname.equalsIgnoreCase("field")) {
                        Field toSet = resultBlock.getFieldByName(fieldName);
                        if (toSet != null) {
                            if (!toSet.setFromString(text)) {
                                throw new BlocklyParserException(
                                        "Failed to set a field's value from XML.");
                            }
                        }
                    } else if (tagname.equalsIgnoreCase("comment")) {
                        resultBlock.setComment(text);
                    } else if (tagname.equalsIgnoreCase("value")) {
                        if (valueInput != null) {
                            parentConnection = valueInput.getConnection();
                            if (parentConnection == null) {
                                throw new BlocklyParserException("The input connection was null.");
                            }
                        } else {
                            throw new BlocklyParserException(
                                    "A value input was null.");
                        }
                    } else if (tagname.equalsIgnoreCase("statement")) {
                        if (statementInput != null) {
                            parentConnection = statementInput.getConnection();
                            if (parentConnection == null) {
                                throw new BlocklyParserException(
                                        "The statement connection was null.");
                            }
                        } else {
                            throw new BlocklyParserException(
                                    "A statement input was null.");
                        }
                    } else if (tagname.equalsIgnoreCase("next")) {
                        parentConnection = resultBlock.getNextConnection();
                        if (parentConnection == null) {
                            throw new BlocklyParserException("A next connection was null");
                        }
                    }
                    // If we finished a parent connection (statement, value, or next)
                    if (parentConnection != null) {
                        // Connect its child if one exists
                        if (childBlock != null) {
                            Connection childConnection = childBlock.getPreviousConnection();
                            if (childConnection == null) {
                                childConnection = childBlock.getOutputConnection();
                            }
                            if (childConnection == null) {
                                throw new BlocklyParserException(
                                        "The child block's connection was null.");
                            }
                            if (parentConnection.isConnected()) {
                                throw new BlocklyParserException("Duplicated " + tagname
                                        + " in block.");
                            }
                            parentConnection.connect(childConnection);
                        }
                        // Then connect its shadow if one exists
                        if (childShadow != null) {
                            Connection shadowConnection = childShadow.getPreviousConnection();
                            if (shadowConnection == null) {
                                shadowConnection = childShadow.getOutputConnection();
                            }
                            if (shadowConnection == null) {
                                throw new BlocklyParserException(
                                        "The shadow block connection was null.");
                            }
                            if (parentConnection.getShadowConnection() != null) {
                                throw new BlocklyParserException("Duplicated " + tagname
                                        + " in block.");
                            }
                            parentConnection.setShadowConnection(shadowConnection);
                            if (!parentConnection.isConnected()) {
                                // If there was no standard block connect the shadow
                                parentConnection.connect(shadowConnection);
                            }
                        }
                        // And clear out all the references for this tag
                        childBlock = null;
                        childShadow = null;
                        valueInput = null;
                        statementInput = null;
                    }
                    break;

                default:
                    break;
            }
            eventType = parser.next();
        }
        // Should never reach here, since this is called from a workspace fromXml function.
        throw new BlocklyParserException(
                "Reached the end of Block.fromXml. This should never happen.");
    }

    public void updateDropDownOptions(String blockType, String fieldName,
                                      FieldDropdown.Options options) {
        BlockTypeFieldName key = new BlockTypeFieldName(blockType, fieldName);
        WeakReference<FieldDropdown.Options> sharedOptionsRef = mDropDownOptions.get(key);
        FieldDropdown.Options sharedOptions =
                sharedOptionsRef == null ? null : sharedOptionsRef.get();
        if (sharedOptions == null) {
            mDropDownOptions.put(key, new WeakReference<>(options));
        } else {
            sharedOptions.copyFrom(options);
        }
    }

    /**
     * Removes all blocks from the factory.
     */
    public void clear() {
        mBlockTemplates.clear();
        mBlockRefs.clear();
    }

    /**
     * Removes references to previous blocks. This can be used when resetting a workspace to force
     * a cleanup of known block instances.
     */
    public void clearPriorBlockReferences() {
        mBlockRefs.clear();
    }

    /** @return Number of blocks added to the factory. */
    private int loadBlocks(InputStream blockIs) throws IOException {
        int blockAddedCount = 0;
        try {
            int size = blockIs.available();
            byte[] buffer = new byte[size];
            blockIs.read(buffer);

            String json = new String(buffer, "UTF-8");
            JSONArray blocks = new JSONArray(json);
            for (int i = 0; i < blocks.length(); i++) {
                JSONObject block = blocks.getJSONObject(i);
                String type = block.optString("type");
                if (!TextUtils.isEmpty(type)) {
                    mBlockTemplates.put(type, fromJson(type, block));
                    ++blockAddedCount;
                } else {
                    throw new BlockLoadingException(
                            "Block " + i + " has no type and cannot be loaded.");
                }
            }
        } catch (JSONException e) {
            throw new BlockLoadingException(e);
        }

        return blockAddedCount;
    }
}
