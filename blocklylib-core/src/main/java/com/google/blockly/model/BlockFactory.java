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
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.google.blockly.utils.BlockLoadingException;
import com.google.blockly.utils.BlocklyXmlHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

/**
 * Helper class for building a set of master blocks and then obtaining copies of them for use in
 * a workspace or toolbar.
 */
public class BlockFactory {
    private static final String TAG = "BlockFactory";

    /**
     * Description to pass into {@link #obtain(BlockTemplate)}, a literate API to obtain a
     * new Block.
     *
     * <pre>
     * {@code factory.obtain(block().ofType("math_number"));}
     * </pre>
     */
    public static BlockTemplate block() {
        return new BlockTemplate();
    }

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
     * Block definitions from resources will never be passed to generators
     * (https://github.com/google/blockly-android/issues/525). Instead, use
     * {@link #addJsonDefinitionsAsset(AssetManager, String)} to load from assets.
     *
     * @param context The context for loading resources.
     * @param blockSourceIds A list of JSON resources containing blocks.
     * @throws IllegalStateException if any block definitions fail to load.
     */
    @Deprecated
    public BlockFactory(Context context, int[] blockSourceIds) {
        this(context);
        if (blockSourceIds != null) {
            for (int i = 0; i < blockSourceIds.length; i++) {
                addJsonDefinitions(blockSourceIds[i]);
            }
        }
    }

    /**
     * Create a factory with a handle to the Activity Resources.
     * Block definitions from resources will never be passed to generators
     * (https://github.com/google/blockly-android/issues/525). Instead, use
     * {@link #addJsonDefinitionsAsset(AssetManager, String)} to load from assets.
     *
     * @param context The context for loading resources.
     */
    @Deprecated
    public BlockFactory(Context context) {
        mResources = context.getResources();
    }

    public BlockFactory(final InputStream source) throws IOException {
        addJsonDefinitions(source);
    }

    /**
     * @param id The id to check.
     * @returns True if a block with the given id exists. Otherwise, false.
     */
    public boolean isBlockIdInUse(String id) {
        WeakReference<Block> priorBlockRef = mBlockRefs.get(id);
        Block priorBlock = priorBlockRef == null ? null : priorBlockRef.get();
        return priorBlock != null;
    }

    /**
     * Registers a new BlockDefinition with the factory.
     * @param definition The new definition.
     * @throws IllegalArgumentException If type name is already defined.
     */
    public void addDefinition(BlockDefinition definition) {
        String definitionName = definition.getTypeName();
        if (mDefinitions.containsKey(definitionName)) {
            throw new IllegalArgumentException("Definition already defined. Must remove first.");
        }
        mDefinitions.put(definitionName, definition);
    }

    /**
     * Loads and adds block definitions from a JSON array in an input stream.
     *
     * @param jsonStream The stream containing the JSON block definitions.
     *
     * @return A pair of a list of exceptions (null if none) and a count of definitions added.
     * @throws IOException If unrecoverable errors are encountered reading the stream.
     */
    public Pair<List<Exception>, Integer> addJsonDefinitions(InputStream jsonStream)
            throws IOException {
        // Read stream as single string.
        String inString = new Scanner( jsonStream ).useDelimiter("\\A").next();
        return addJsonDefinitions(inString);
    }

    /**
     * Loads and adds block definition from a JSON array string.
     *
     * @param jsonString The InputStream containing the JSON block definitions.
     *
     * @return A pair of a list of exceptions (null if none) and a count of definitions added.
     * @throws IOException If unrecoverable errors are encountered reading the string.
     */
    public Pair<List<Exception>, Integer> addJsonDefinitions(String jsonString) throws IOException {
        List<Exception> exceptions = null;

        // Parse JSON first, avoiding side effects (partially added file) in the case of an error.
        List<BlockDefinition> defs;
        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            defs = new ArrayList<>(jsonArray.length());
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonDef = jsonArray.getJSONObject(i);
                try {
                    defs.add(new BlockDefinition(jsonDef));
                } catch (JSONException e) {
                    String msg = "Block definition #" + i + ": " + e.getMessage();
                    Log.w(TAG, msg);
                    if (exceptions == null) {
                        exceptions = new ArrayList<>();
                    }
                    exceptions.add(new BlockLoadingException(msg, e));
                }
            }
        } catch (JSONException e) {
            throw new IOException(e);
        }

        // Attempt to add each definition, catching redefinition errors.
        int blockAddedCount = 0;
        for (int i = 0; i < defs.size(); ++i) {
            BlockDefinition def = defs.get(i);
            String typeName = def.getTypeName();
            if (!mDefinitions.containsKey(typeName)) {
                addDefinition(def);
                blockAddedCount++;
            } else {
                String msg = "Block type \"" + def.getTypeName() + "\" already defined.";
                Log.w(TAG, msg);
                if (exceptions == null) {
                    exceptions = new ArrayList<>();
                }
                exceptions.add(new BlockLoadingException(msg));
            }
        }

        return Pair.create(exceptions, blockAddedCount);
    }

    /**
     * Loads and adds block definitions from a file in Assets.
     *
     * @param assets The context's AssetManager.
     * @param assetPath The asset path to the JSON definitions.
     * @return The count of definitions added.
     */
    public int addJsonDefinitionsAsset(AssetManager assets, String assetPath) {
        Pair<List<Exception>, Integer> result;
        InputStream jsonInput = null;
        try {
            jsonInput = assets.open(assetPath);
            result = addJsonDefinitions(jsonInput);
        } catch (IOException e) {
            // Compile time resources are expected to always be valid.
            throw new IllegalStateException(
                    "Failed to load block definitions from asset file: " + assetPath, e);
        } finally {
            if (jsonInput != null) {
                try {
                    jsonInput.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
        if (result.first != null) {
            // Compile time resources are expected to always be valid.
            throw new IllegalStateException(
                    "Failed to load all block definitions from asset file: " + assetPath
                    + "\nSee above.");
        }
        return result.second;
    }

    /**
     * Removes a block type from the factory. If the Block is still in use by the workspace this
     * could cause a crash if the user tries to load a new block of this type, including copies.
     *
     * @param definitionName The name of the block to remove.
     *
     * @return True if the definition was found and removed. Otherwise, false.
     */
    public boolean removeDefinition(String definitionName) {
        return mDefinitions.remove(definitionName) != null;
    }

    /**
     * Creates a block of the specified type using one of {@link BlockDefinition}s registered with
     * this factory. If the definitionName is not one of the known block types null will be returned
     * instead.
     *
     * <strong>Deprecated:</strong> Prefer using
     * {@code obtain(block().ofType(definitionName).withId(id));}
     *
     * @param definitionName The name of the block type to create.
     * @param id The id of the block if loaded from XML; null otherwise.
     *
     * @return A new block of that type or null.
     * @throws IllegalArgumentException If id is not null and already refers to a block.
     */
    @Deprecated
    public Block obtainBlock(String definitionName, @Nullable String id) {
        // Validate id is available.
        if (isBlockIdInUse(id)) {
            throw new IllegalArgumentException("Block id \"" + id + "\" already in use.");
        }

        // Verify definition is defined.
        if (!mDefinitions.containsKey(definitionName)) {
            Log.w(TAG, "Block " + definitionName + " not found.");
            return null;
        }
        try {
            return obtain(block().ofType(definitionName).withRequiredId(id, true));
        } catch (BlockLoadingException e) {
            return null;
        }
    }

    /**
     * Creates the {@link Block} described by the template.
     *
     * {@code blockFactory.obtain(block().ofType("math_number"));}
     *
     * @param template A template of the block to create.
     * @return A new block, or null if not able to construct it.
     */
    public Block obtain(BlockTemplate template) throws BlockLoadingException {
        // Validate id is available.
        if (isBlockIdInUse(template.mId)) {
            throw new IllegalArgumentException("Block id \"" + template.mId + "\" already in use.");
        }

        // Existing instance not found. Constructing a new Block.
        BlockDefinition definition;
        boolean isShadow = template.mIsShadow == null ? false : template.mIsShadow;
        Block block;
        if (template.mCopySource != null) {
            try {
                // TODO: Improve copy overhead. Template from copy to avoid XML I/O?
                String xml = BlocklyXmlHelper.writeBlockToXml(template.mCopySource,
                        IOOptions.WRITE_ROOT_ONLY_WITHOUT_ID);
                if (template.mId != null) {
                    String escapedId = BlocklyXmlHelper.escape(template.mId);
                    xml = xml.replace("<block", "<block id=\"" + escapedId + "\"");
                }
                block = BlocklyXmlHelper.loadOneBlockFromXml(xml, this);
            } catch (BlocklySerializerException e) {
                Log.e(TAG, template.mCopySource + ": Failed to copy block.", e);
                return null;

            }
        } else {
            // Start a new block from a block definition.
            if (template.mDefinition != null) {
                assert (template.mDefinitionName == null);
                definition = template.mDefinition;
            } else if (template.mDefinitionName != null) {
                definition = mDefinitions.get(template.mDefinitionName.trim());
                if (definition == null) {
                    Log.w(TAG,
                            "BlockDefinition named \"" + template.mDefinitionName + "\" not found.");
                    return null;
                }
            } else {
                Log.w(TAG, "No BlockDefinition declared.");
                return null;
            }

            try {
                block = new Block(this, definition, template.mId, isShadow);
                // TODO(#529): Apply Extensions.
            } catch (BlockLoadingException e) {
                // Prefer reporting registered typename over the definition's self-reported type name.
                String defName = template.mDefinitionName != null ?
                        template.mDefinitionName : definition.getTypeName();
                String ofTypeName = " of type \"" + defName + "\"";
                Log.e(TAG, "Failed to create block" + ofTypeName + ".", e);
                return null;
            }
        }

        // Apply mutable state last.
        block.applyTemplate(template);
        mBlockRefs.put(block.getId(), new WeakReference<>(block));

        return block;
    }

    /**
     * @return The list of known blocks that can be created.
     */
    public List<BlockDefinition> getAllBlockDefinitions() {
        return new ArrayList<>(mDefinitions.values());
    }

    /**
     * Loads and adds block templates from a raw file resource.
     *
     * Block definitions from resources will never be passed to generators
     * (https://github.com/google/blockly-android/issues/525). Instead, use
     * {@link #addJsonDefinitionsAsset(AssetManager, String)} to load from assets.
     *
     * @param resId The id of the JSON resource to load blocks from.
     *
     * @return Number of blocks added to the factory.
     * @throws BlockLoadingException if error occurs when parsing JSON or block definitions.
     */
    @Deprecated
    public int addJsonDefinitions(int resId) {
        InputStream jsonInput = mResources.openRawResource(resId);
        try {
            Pair<List<Exception>, Integer> results = addJsonDefinitions(jsonInput);
            return results.second;
        } catch (IOException e) {
            // Compile time resources are expected to always be valid.
            throw new IllegalStateException("Failed to load block defintions from resource: "
                    + mResources.getResourceEntryName(resId));
        } finally {
            try {
                jsonInput.close();
            } catch (IOException e) {
                // Ignore
            }
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
     * @return The loaded block.
     * @throws BlockLoadingException If unable to load the block or child. May contain a
     *                               XmlPullParserException or IOException as a root cause.
     */
    public Block fromXml(XmlPullParser parser) throws BlockLoadingException {
        try {
            String type = parser.getAttributeValue(null, "type");   // prototype name
            String id = parser.getAttributeValue(null, "id");
            if (type == null || type.isEmpty()) {
                throw new BlockLoadingException("Block was missing a type.");
            }
            // If the id was empty the BlockFactory will just generate one.

            final BlockTemplate template = new BlockTemplate().ofType(type).withId(id);

            String collapsedString = parser.getAttributeValue(null, "collapsed");
            if (collapsedString != null) {
                template.collapsed(Boolean.parseBoolean(collapsedString));
            }

            String deletableString = parser.getAttributeValue(null, "deletable");
            if (deletableString != null) {
                template.deletable(Boolean.parseBoolean(deletableString));
            }

            String disabledString = parser.getAttributeValue(null, "disabled");
            if (disabledString != null) {
                template.disabled(Boolean.parseBoolean(disabledString));
            }

            String editableString = parser.getAttributeValue(null, "editable");
            if (editableString != null) {
                template.editable(Boolean.parseBoolean(editableString));
            }

            String inputsInlineString = parser.getAttributeValue(null, "inline");
            if (inputsInlineString != null) {
                template.withInlineInputs(Boolean.parseBoolean(inputsInlineString));
            }

            String movableString = parser.getAttributeValue(null, "movable");
            if (movableString != null) {
                template.movable(Boolean.parseBoolean(movableString));
            }

            // Set position.  Only if this is a top level block.
            String x = parser.getAttributeValue(null, "x");
            String y = parser.getAttributeValue(null, "y");
            if (x != null && y != null) {
                template.atPosition(Float.parseFloat(x), Float.parseFloat(y));
            }

            int eventType = parser.next();
            String text = "";
            String fieldName = "";
            Block childBlock = null;
            Block childShadow = null;
            String inputName = null;

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
                            inputName = parser.getAttributeValue(null, "name");
                        } else if (tagname.equalsIgnoreCase("statement")) {
                            inputName = parser.getAttributeValue(null, "name");
                        } else if (tagname.equalsIgnoreCase("mutation")) {
                            // TODO(#530): Handle mutations.
                        }
                        break;

                    case XmlPullParser.TEXT:
                        text = parser.getText();
                        break;

                    case XmlPullParser.END_TAG:
                        if (tagname.equalsIgnoreCase("block")) {
                            return obtain(template);
                        } else if (tagname.equalsIgnoreCase("shadow")) {
                            try {
                                template.shadow();
                                return obtain(template);
                            } catch (IllegalStateException e) {
                                throw new BlockLoadingException(e);
                            }
                        } else if (tagname.equalsIgnoreCase("field")) {
                            template.withFieldValue(fieldName, text);
                            fieldName = null;
                            text = "";
                        } else if (tagname.equalsIgnoreCase("comment")) {
                            template.withComment(text);
                            text = "";
                        } else if (tagname.equalsIgnoreCase("value") ||
                                tagname.equalsIgnoreCase("statement")) {
                            if (inputName == null) {
                                // Start tag missing input name
                                throw new BlockLoadingException("Missing inputName.");
                            }
                            try {
                                template.withInputValue(inputName, childBlock, childShadow);
                            } catch (IllegalArgumentException e) {
                                throw new BlockLoadingException(e.getMessage());
                            }
                            childBlock = null;
                            childShadow = null;
                            inputName = null;
                        } else if (tagname.equalsIgnoreCase("next")) {
                            template.withNextChild(childBlock, childShadow);
                            childBlock = null;
                            childShadow = null;
                        }
                        break;

                    default:
                        break;
                }
                eventType = parser.next();
            }
            // Should never reach here, since this is called from a workspace fromXml function.
            throw new BlockLoadingException("Reached the END_DOCUMENT before end of block.");
        } catch (XmlPullParserException | IOException e) {
            throw new BlockLoadingException(e);
        }
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
}
