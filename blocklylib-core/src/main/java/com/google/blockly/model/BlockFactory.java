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
import java.util.Iterator;
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
    private final HashMap<String, BlockDefinition> mDefinitions = new HashMap<>();
    private final HashMap<String, WeakReference<Block>> mBlockRefs = new HashMap<>();

    /**
     * The global list of dropdown options available to each field matching the
     * {@link BlockTypeFieldName} key.
     */
    protected final HashMap<BlockTypeFieldName, WeakReference<FieldDropdown.Options>>
            mDropdownOptions = new HashMap<>();

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
     * Removes a block type from the factory. If the Block is still in use by the workspace this
     * could cause a crash if the user tries to load a new block of this type.
     *
     * @param definitionName The name of the block to remove.
     *
     * @return True if the definition was found and removed. Otherwise, false.
     */
    public boolean removeBlockDefinition(String definitionName) {
        return mDefinitions.remove(definitionName) != null;
    }

    /**
     * Creates a block of the specified type using one of {@link BlockDefinition}s registered with
     * this factory. If the definitionName is not one of the known block types null will be returned
     * instead.
     *
     * @param definitionName The name of the block type to create.
     * @param uuid The id of the block if loaded from XML; null otherwise.
     *
     * @return A new block of that type or null.
     * @throws IllegalArgumentException If uuid is not null and already refers to a block.
     */
    public Block obtainBlock(String definitionName, @Nullable String uuid) {
        // Existing instance not found.  Constructing a new one.
        BlockDefinition definition = mDefinitions.get(definitionName);
        if (definition == null) {
            Log.w(TAG, "Block " + definitionName + " not found.");
            return null;
        }
        return obtainBlock(definition, uuid);
    }

    /**
     * Creates a block of the specified by the JSON string definition. This block will lack a
     * registered type, and will not be loadable from serialized workspace or toolbox XML. This is
     * more useful in testing than in real apps.
     *
     * @param json The JSON defining the block.
     * @param uuid The id of the block if loaded from XML; null otherwise.
     *
     * @return A new block of that type.
     * @throws IllegalArgumentException If uuid is not null and already refers to a block.
     */
    public Block obtainBlockFromJson(String json, @Nullable String uuid) throws JSONException {
        return obtainBlock(new BlockDefinition(json), uuid);
    }

    /**
     * Creates a block of the specified by the block definition. This block may lack a
     * registered type, and will not be loadable from serialized workspace or toolbox XML. This is
     * more useful in testing than in real apps.
     *
     * @param definition The name of the block type to create.
     * @param uuid The id of the block if loaded from XML; null otherwise.
     *
     * @return A new block of that type or null.
     * @throws IllegalArgumentException If uuid is not null and already refers to a block.
     */
    public Block obtainBlock(BlockDefinition definition, @Nullable String uuid) {
        Block block;
        // First search for any existing instance
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
        try {
            block = new Block(this, definition, uuid);
            mBlockRefs.put(block.getId(), new WeakReference<Block>(block));

            // TODO: Apply Extensions.
            return block;
        } catch (BlockLoadingException e) {
            Log.e(TAG, "Failed to load block of type \"" + definitionName + "\".", e);
            return null;
        }
    }

    /**
     * @return The list of known blocks that can be created.
     */
    public List<BlockDefinition> getAllBlockDefinitions() {
        return new ArrayList<>(mDefinitions.values());
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
        try{
            String typeInJson = json.optString("type");
            if (typeInJson == null || (!type.equals(typeInJson))) {
                // Copy object with all keys.
                Iterator<String> keyIter = json.keys();
                String [] keys = new String[json.length()];
                int i = 0;
                while (keyIter.hasNext()) {
                    keys[i++] = keyIter.next();
                }
                json = new JSONObject(json, keys);
                json.put("type", type);
            }
            BlockDefinition def = new BlockDefinition(json);
            return new Block(this, def, null);
        } catch (JSONException e) {
            throw new BlockLoadingException(e);
        }
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
                    // While block type names should be unique, if there is a collision, the latest
                    // block and its option type wins.
                    mDropdownOptions.put(
                            new BlockTypeFieldName(blockType, fieldName),
                            new WeakReference<>(((FieldDropdown) field).getOptions()));
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
        // If the id was empty the BlockFactory will just generate one.

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
            resultBlock.setPosition(Float.parseFloat(x), Float.parseFloat(y));
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

    /**
     * Updates the list of options used by dropdowns in select block types. These fields must be
     * derived from the prototype blocks loaded via JSON (via {@link #obtainBlock}), and where
     * {@link FieldDropdown#setOptions(FieldDropdown.Options)} has not been called. An instance of
     * the block must already exist, usually the prototype loaded via JSON.
     *
     * @param blockType The name for the type of block containing such fields
     * @param fieldName The name of the field within the block.
     * @param optionList The list of {@link FieldDropdown.Option}s used to to set for the
     *                   referenced dropdowns.
     */
    public void updateDropdownOptions(String blockType, String fieldName,
                                      List<FieldDropdown.Option> optionList) {
        BlockTypeFieldName key = new BlockTypeFieldName(blockType, fieldName);
        WeakReference<FieldDropdown.Options> sharedOptionsRef = mDropdownOptions.get(key);
        FieldDropdown.Options sharedOptions =
                sharedOptionsRef == null ? null : sharedOptionsRef.get();
        if (sharedOptions == null) {
            sharedOptions = new FieldDropdown.Options(optionList);
            mDropdownOptions.put(key, new WeakReference<>(sharedOptions));
        } else {
            sharedOptions.updateOptions(optionList);
        }
    }

    /**
     * Removes all blocks from the factory.
     */
    public void clear() {
        mDefinitions.clear();
        mDropdownOptions.clear();
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
                JSONObject jsonDef = blocks.getJSONObject(i);
                String type = jsonDef.optString("type");
                if (!TextUtils.isEmpty(type)) {
                    mDefinitions.put(type, new BlockDefinition(jsonDef));
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
