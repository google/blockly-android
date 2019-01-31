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

import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.Log;

import com.google.blockly.android.control.BlocklyController;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

/**
 * The BlockFactory is responsible for managing the set of BlockDefinitions, and instantiating
 * {@link Block}s from those definitions.
 *
 * Add new definitions to the factory via {@link #addJsonDefinitions}. Create new Blocks by calling
 * {@link #obtainBlockFrom(BlockTemplate)}.  Using {@link BlockTemplate}'s chaining methods, this
 * can look like:
 *
 * <pre>{@code
 * // Add definition.
 * factory.addJsonDefinitions(getAssets().open("default/math_blocks.json"));
 * // Create blocks.
 * Block pi = factory.obtainBlockFrom(new BlockTemplate().ofType("math_number").withId("PI"));
 * factory.obtainBlockFrom(new BlockTemplate().copyOf(pi).shadow().withId("PI-shadow"));
 * }</pre>
 */
public class BlockFactory {
    private static final String TAG = "BlockFactory";

    private final Map<String, BlockDefinition> mDefinitions = new HashMap<>();
    private final Map<String, Mutator.Factory> mMutatorFactories = new HashMap<>();
    private final Map<String, BlockExtension> mExtensions = new HashMap<>();
    private final Map<String, WeakReference<Block>> mBlockRefs = new HashMap<>();

    protected BlocklyController mController;

    /**
     * The global list of dropdown options available to each field matching the
     * {@link BlockTypeFieldName} key.
     */
    protected final HashMap<BlockTypeFieldName, WeakReference<FieldDropdown.Options>>
            mDropdownOptions = new HashMap<>();

    /** Default constructor. */
    public BlockFactory() {
    }

    public void setController(BlocklyController controller) {
        if (mController != null && controller != mController) {
            throw new IllegalStateException(
                    "BlockFactory is already associated with another BlocklyController.");
        }
        mController = controller;
    }

    /**
     * @param id The id to check.
     * @returns True if a block with the given id exists. Otherwise, false.
     */
    public boolean isBlockIdInUse(String id) {
        WeakReference<Block> priorBlockRef = mBlockRefs.get(id);
        return priorBlockRef != null && priorBlockRef.get() != null;
    }

    /**
     * Registers a new BlockDefinition with the factory.
     * @param definition The new definition.
     * @throws IllegalArgumentException If type name is already defined.
     */
    public void addDefinition(BlockDefinition definition) {
        String typeName = definition.getTypeName();
        if (mDefinitions.containsKey(typeName)) {
            throw new IllegalArgumentException(
                    "Definition \"" + typeName + "\" already defined. Prior must remove first.");
        }
        mDefinitions.put(typeName, definition);
    }

    /**
     * Loads and adds block definitions from a JSON array in an input stream.
     *
     * @param jsonStream The stream containing the JSON block definitions.
     *
     * @return A count of definitions added.
     * @throws IOException If there is a fundamental problem with the input.
     * @throws BlockLoadingException If the definition is malformed.
     */
    public int addJsonDefinitions(InputStream jsonStream)
            throws IOException, BlockLoadingException {
        // Read stream as single string.
        String inString = new Scanner( jsonStream ).useDelimiter("\\A").next();
        return addJsonDefinitions(inString);
    }

    /**
     * Loads and adds block definition from a JSON array string. These definitions will replace
     * existing definitions, if the type names conflict.
     *
     * @param jsonString The InputStream containing the JSON block definitions.
     *
     * @return A count of definitions added.
     * @throws IOException If there is a fundamental problem with the input.
     * @throws BlockLoadingException If the definition is malformed.
     */
    public int addJsonDefinitions(String jsonString) throws IOException, BlockLoadingException {
        // Parse JSON first, avoiding side effects (partially added file) in the case of an error.
        List<BlockDefinition> defs;
        int arrayIndex = 0;  // definition index
        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            defs = new ArrayList<>(jsonArray.length());
            for (arrayIndex = 0; arrayIndex < jsonArray.length(); arrayIndex++) {
                JSONObject jsonDef = jsonArray.getJSONObject(arrayIndex);
                defs.add(new BlockDefinition(jsonDef));
            }
        } catch (JSONException e) {
            String msg = "Block definition #" + arrayIndex + ": " + e.getMessage();
            throw new BlockLoadingException(msg, e);
        }

        // Attempt to add each definition, catching redefinition errors.
        int blockAddedCount = 0;
        for (arrayIndex = 0; arrayIndex < defs.size(); ++arrayIndex) {
            BlockDefinition def = defs.get(arrayIndex);
            String typeName = def.getTypeName();

            // Replace prior definition with warning, mimicking web behavior.
            if (removeDefinition(typeName)) {
                Log.w(TAG, "Block definition #" + arrayIndex +
                        " in JSON array replaces prior definition of \"" + typeName + "\".");
            }
            addDefinition(def);
            blockAddedCount++;
        }

        return blockAddedCount;
    }

    /**
     * Registers a {@link Mutator.Factory} for the named mutator type.
     * @param mutatorId The name / id of this mutator type.
     * @param mutatorFactory The factory for this mutator type.
     */
    public void registerMutator(String mutatorId, Mutator.Factory mutatorFactory) {
        Mutator.Factory old = mMutatorFactories.get(mutatorId);
        if (mutatorFactory == old) {
            return;
        }
        if (mutatorId != null && old != null) {
            if (mBlockRefs.isEmpty()) {
                Log.w(TAG, "Replacing reference to mutator \"" + mutatorId + "\".");
            } else {
                // Make this explicit
                throw new IllegalStateException(
                        "Mutator \"" + mutatorId + "\" already registered, and may be "
                        + "referenced by a block. Must call removeMutator() first .");
            }
        }
        mMutatorFactories.put(mutatorId, mutatorFactory);
    }

    /**
     * Registers a {@link BlockExtension} for Blocks.
     * @param extensionId The identifier used in BlockDefinition and JSON references.
     * @param extension The new extension.
     */
    public void registerExtension(String extensionId, BlockExtension extension) {
        BlockExtension old = mExtensions.get(extensionId);
        if (extension == old) {
            return;
        }
        if (extension != null && old != null) {
            if (mBlockRefs.isEmpty()) {
                Log.w(TAG, "Replacing reference to BlockExtension \"" + extensionId + "\".");
            } else {
                // Make this explicit
                throw new IllegalStateException(
                        "BlockExtension \"" + extensionId + "\" already registered, and may be "
                        + "referenced by a block. Must call removeExtension() first .");
            }
        }
        mExtensions.put(extensionId, extension);
    }

    /**
     * Removes a block type definition from this factory. If any block of this type is still in use,
     * this may cause a crash if the user tries to load a new block of this type, including copies.
     *
     * @param definitionName The name of the block definition to remove.
     * @return True if the block definition was found and removed. Otherwise, false.
     */
    public boolean removeDefinition(String definitionName) {
        boolean result = (mDefinitions.remove(definitionName) != null);
        if (result && !mBlockRefs.isEmpty()) {
            Log.w(TAG, "Removing BlockDefinition \"" + definitionName + "\" while blocks active.");
        }
        return result;
    }

    /**
     * Removes a {@link BlockExtension} from this block factory. If any block of this type is still
     * in use, this may cause a crash if the user tries to load a new block of this type, including
     * copies.
     *
     * @param extensionName The name of the extension to remove.
     * @return True if the extension was found and removed. Otherwise, false.
     */
    public boolean removeExtension(String extensionName) {
        boolean result = (mExtensions.remove(extensionName) != null);
        if (result && !mBlockRefs.isEmpty()) {
            Log.w(TAG, "Removing BlockExtension \"" + extensionName + "\" while blocks active.");
        }
        return result;
    }

    /**
     * Creates a block of the specified type using a {@link BlockDefinition} registered with this
     * factory. If the {@code definitionName} is not one of the known block types null will be
     * returned instead.
     *
     * This version is used for testing.
     * {@code obtain(new BlockTemplate().ofType(definitionName).withId(id));} should be use instead.
     *
     * @param definitionName The name of the block type to create.
     * @param id The id of the block if loaded from XML; null otherwise.
     * @return A new block of that type or null.
     * @throws IllegalArgumentException If id is not null and already refers to a block.
     */
    @VisibleForTesting
    Block obtainBlock(String definitionName, @Nullable String id) {
        // Validate id is available.
        if (id != null && isBlockIdInUse(id)) {
            throw new IllegalArgumentException("Block id \"" + id + "\" already in use.");
        }

        // Verify definition is defined.
        if (!mDefinitions.containsKey(definitionName)) {
            Log.w(TAG, "Block " + definitionName + " not found.");
            return null;
        }
        try {
            return obtainBlockFrom(new BlockTemplate().ofType(definitionName).withId(id));
        } catch (BlockLoadingException e) {
            return null;
        }
    }

    /**
     * Creates the {@link Block} described by the template.
     *
     * <pre>{@code
     * blockFactory.obtainBlockFrom(new BlockTemplate().shadow().ofType("math_number"));
     * }</pre>
     *
     * @param template A template of the block to create.
     * @return A new block, or null if not able to construct it.
     */
    public Block obtainBlockFrom(BlockTemplate template) throws BlockLoadingException {
        if (mController == null) {
            throw new IllegalStateException("Must set BlockController before creating block.");
        }

        String id = getCheckedId(template.mId);

        // Existing instance not found. Constructing a new Block.
        BlockDefinition definition;
        boolean isShadow = (template.mIsShadow == null) ? false : template.mIsShadow;
        Block block;
        if (template.mCopySource != null) {
            try {
                // TODO: Improve copy overhead. Template from copy to avoid XML I/O?
                String xml = BlocklyXmlHelper.writeBlockToXml(template.mCopySource,
                        IOOptions.WRITE_ROOT_ONLY_WITHOUT_ID);
                String escapedId = BlocklyXmlHelper.escape(id);
                xml = xml.replace("<block", "<block id=\"" + escapedId + "\"");
                block = BlocklyXmlHelper.loadOneBlockFromXml(xml, this);
            } catch (BlocklySerializerException e) {
                throw new BlockLoadingException(
                        "Failed to serialize original " + template.mCopySource, e);
            }
        } else {
            // Start a new block from a block definition.
            if (template.mDefinition != null) {
                if (template.mTypeName != null
                        && !template.mTypeName.equals(template.mDefinition.getTypeName())) {
                    throw new BlockLoadingException("Conflicting block definitions referenced.");
                }
                definition = template.mDefinition;
            } else if (template.mTypeName != null) {
                definition = mDefinitions.get(template.mTypeName.trim());
                if (definition == null) {
                    throw new BlockLoadingException("Block definition named \""
                            + template.mTypeName + "\" not found.");
                }
            } else {
                throw new BlockLoadingException(template.toString() + " missing block definition.");
            }

            block = new Block(mController, this, definition, id, isShadow);
        }

        // Apply mutable state last.
        template.applyMutableState(block);
        mBlockRefs.put(block.getId(), new WeakReference<>(block));

        return block;
    }

    /**
     * @return The list of known blocks types.
     */
    public List<BlockDefinition> getAllBlockDefinitions() {
        return new ArrayList<>(mDefinitions.values());
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
     * Applies the named mutator to the provided block.
     */
    public void applyMutator(String mutatorId, Block block) throws BlockLoadingException {
        Mutator.Factory factory = mMutatorFactories.get(mutatorId);
        if (factory == null) {
            throw new BlockLoadingException("Unknown mutator \"" + mutatorId + "\".");
        }
        block.setMutator(factory.newMutator(mController));
    }

    /**
     * Applies the named extension to the provided block.
     * @param extensionId The name / id of the extension, as seen in the JSON extensions attribute.
     * @param block The block to apply it to.
     */
    public void applyExtension(String extensionId, Block block)
            throws BlockLoadingException {
        BlockExtension extension = mExtensions.get(extensionId);
        if (extension == null) {
            throw new BlockLoadingException("Unknown extension \"" + extensionId + "\".");
        }
        Mutator old = block.getMutator();
        extension.applyTo(block);
        if (block.getMutator() != old) {
            // Unlike web, Android mutators are not assigned by extensions.
            Log.w(TAG, "Extensions are not allowed to assign mutators. "
                    + "Use Mutator and Mutator.Factory class.");
        }
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
        int startLine = parser.getLineNumber();
        BlockLoadingException childException = null;
        final XmlBlockTemplate template = new XmlBlockTemplate();
        try {
            String type = parser.getAttributeValue(null, "type");   // prototype name
            if (type == null || (type = type.trim()).isEmpty()) {
                throw new BlockLoadingException("Block is missing a type.");
            }
            template.ofType(type);
            template.withId(parser.getAttributeValue(null, "id"));
            // If the id was empty the BlockFactory will just generate one.

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
                            try {
                                childBlock = fromXml(parser);
                            } catch (BlockLoadingException e) {
                                childException = e;  // Save reference to pass through outer catch
                                throw e;
                            }
                        } else if (tagname.equalsIgnoreCase("shadow")) {
                            try {
                                childShadow = fromXml(parser);
                            } catch (BlockLoadingException e) {
                                childException = e;  // Save reference to pass through outer catch
                                throw e;
                            }
                        } else if (tagname.equalsIgnoreCase("field")) {
                            fieldName = parser.getAttributeValue(null, "name");
                        } else if (tagname.equalsIgnoreCase("value")
                                || tagname.equalsIgnoreCase("statement")) {
                            inputName = parser.getAttributeValue(null, "name");
                            if (TextUtils.isEmpty(inputName)) {
                                throw new BlockLoadingException(
                                        "<" + tagname + "> must have a name attribute.");
                            }
                        } else if (tagname.equalsIgnoreCase("mutation")) {
                            String elementStr = BlocklyXmlHelper.captureElement(parser);
                            template.withMutation(elementStr);
                        }
                        break;

                    case XmlPullParser.TEXT:
                        text = parser.getText();
                        break;

                    case XmlPullParser.END_TAG:
                        if (tagname.equalsIgnoreCase("block")) {
                            return obtainBlockFrom(template);
                        } else if (tagname.equalsIgnoreCase("shadow")) {
                            template.shadow();
                            return obtainBlockFrom(template);
                        } else if (tagname.equalsIgnoreCase("field")) {
                            if (TextUtils.isEmpty(fieldName)) {
                                Log.w(TAG, "Ignoring unnamed field in " +
                                        template.toString("block"));
                            } else {
                                template.withFieldValue(fieldName, text);
                            }
                            fieldName = null;
                            text = "";
                        } else if (tagname.equalsIgnoreCase("comment")) {
                            template.withComment(text);
                            text = "";
                        } else if (tagname.equalsIgnoreCase("value") ||
                                tagname.equalsIgnoreCase("statement")) {
                            if (inputName == null) {
                                // Start tag missing input name. Should catch this above.
                                throw new BlockLoadingException("Missing inputName.");
                            }
                            try {
                                template.withInputValue(inputName, childBlock, childShadow);
                            } catch (IllegalArgumentException e) {
                                throw new BlockLoadingException(template.toString("Block")
                                        + " input \"" + inputName + "\": " + e.getMessage());
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
            throw new BlockLoadingException("Reached the END_DOCUMENT before end of block.");
        } catch (BlockLoadingException | XmlPullParserException | IOException e) {
            if (e == childException) {
                throw (BlockLoadingException) e; // Pass through unchanged.
            }

            String msg = "Error";
            int errorLine = parser.getLineNumber();
            if (errorLine > -1) {
                int errorCol = parser.getColumnNumber();
                msg += " at line " + errorLine + ", col " + errorCol;
            }
            msg += " loading " + template.toString("block");
            if (startLine > -1) {
                msg += " starting at line " + startLine;
            }
            throw new BlockLoadingException(msg + ": " + e.getMessage(), e);
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
        mBlockRefs.clear();  // What if these blocks exist on the workspace?
        mDefinitions.clear();
        mExtensions.clear();
        mDropdownOptions.clear();
    }

    /**
     * Removes references to previous blocks. This can be used when resetting a workspace to force
     * a cleanup of known block instances.
     */
    public void clearWorkspaceBlockReferences(String workspaceId) {
        List<String> idsToRemove = new ArrayList<>(mBlockRefs.size());
        for (String blockId : mBlockRefs.keySet()) {
            WeakReference<Block> ref = mBlockRefs.get(blockId);
            Block block = ref.get();
            if (block == null || workspaceId.equals(block.getEventWorkspaceId())) {
                idsToRemove.add(blockId);
            }
        }
        for (String id : idsToRemove) {
            mBlockRefs.remove(id);
        }
    }

    /**
     * Returns an id that is statistically unique.
     * @param requested The requested id.
     * @return The allowed id.
     * @throws BlockLoadingException If a collision occurs when the id is required.
     */
    private String getCheckedId(String requested) throws BlockLoadingException {
        if (requested != null) {
            if (!isBlockIdInUse(requested)) {
                return requested;
            }
        }
        String id = UUID.randomUUID().toString();
        while(mBlockRefs.containsKey(id)) {  // Exceptionally unlikely, but...
            id = UUID.randomUUID().toString();
        }
        return id;
    }

    public boolean isDefined(String definitionId) {
        return mDefinitions.containsKey(definitionId);
    }

    /** Child blocks for a named input. Used by {@link XmlBlockTemplate}. */
    private static class InputValue {
        /** The name of the input */
        final String mName;
        /** The child block. */
        final Block mChild;
        /** The connected shadow block. */
        final Block mShadow;

        InputValue(String name, Block child, Block shadow) {
            mName = name;
            mChild = child;
            mShadow = shadow;
        }
    }

    /**
     * Extension of BlockTemplate that includes child blocks. This class is private, because child
     * block references in templates are strictly limited to one use, and this class in not intended
     * for use outside XML deserialization.
     */
    private class XmlBlockTemplate extends BlockTemplate {
        /** Ordered list of input names and blocks, as loaded during XML deserialization. */
        protected List<InputValue> mInputValues;


        /**
         * Sets a block input's child block and shadow. Child blocks in templates are only good
         * once, as the second time the child already has a parent. Only used during XML
         * deserialization.
         *
         * @param inputName The name of the field.
         * @param child The deserialized child block.
         * @param shadow The deserialized shadow block.
         * @return This block descriptor, for chaining.
         * @throws BlockLoadingException If inputName is not a valid name; if child or shadow are
         *                               not configured as such; if child or shadow overwrites a
         *                               prior value.
         */
        private XmlBlockTemplate withInputValue(String inputName, Block child, Block shadow)
                throws BlockLoadingException {
            if (inputName == null
                    || (inputName = inputName.trim()).length() == 0) {  // Trim and test name
                throw new BlockLoadingException("Invalid input value name.");
            }
            // Validate child block shadow state and upward connection.
            if (child != null && (child.isShadow() || child.getUpwardsConnection() == null)) {
                throw new BlockLoadingException("Invalid input value block.");
            }
            if (shadow != null && (!shadow.isShadow() || shadow.getUpwardsConnection() == null)) {
                throw new BlockLoadingException("Invalid input shadow block.");
            }

            if (mInputValues == null) {
                mInputValues = new ArrayList<>();
            } else {
                // Check for prior assignments to the same input value.
                Iterator<InputValue> iter = mInputValues.iterator();
                while (iter.hasNext()) {
                    InputValue priorValue = iter.next();
                    if (priorValue.mName.equals(inputName)) {
                        boolean overwriteChild = child != null
                                && priorValue.mChild != null && child != priorValue.mChild;
                        boolean overwriteShadow = shadow != null
                                & priorValue.mShadow != null && shadow != priorValue.mShadow;

                        if (overwriteChild || overwriteShadow) {
                            throw new IllegalArgumentException(
                                    "Input \"" + inputName + "\" already assigned.");
                        }
                        child = (child == null ? priorValue.mChild : child);
                        shadow = (shadow == null ? priorValue.mShadow : shadow);
                        iter.remove();  // Replaced below
                    }
                }
            }
            mInputValues.add(new InputValue(inputName, child, shadow));
            return this;
        }

        /**
         * Sets a block's next children. Child blocks in templates are only good once, as the second
         * time the child already has a parent. Only used during XML deserialization.
         *
         * @param child The deserialized child block.
         * @param shadow The deserialized shadow block.
         * @return This block descriptor, for chaining.
         * @throws BlockLoadingException If inputName is not a valid name; if child or shadow are
         *                               not configured as such; if child or shadow overwrites a
         *                               prior value.
         */
        private XmlBlockTemplate withNextChild(Block child, Block shadow)
                throws BlockLoadingException {
            if (child != null && (child.isShadow() || child.getPreviousConnection() == null)) {
                throw new BlockLoadingException("Invalid next child block.");
            }
            if (shadow != null && (!shadow.isShadow() || shadow.getPreviousConnection() == null)) {
                throw new BlockLoadingException("Invalid next child shadow.");

            }
            mNextChild = child;
            mNextShadow = shadow;
            return this;
        }

        /** Appends child blocks after the rest of the template state. */
        @Override
        public void applyMutableState(Block block) throws BlockLoadingException {
            super.applyMutableState(block);

            // TODO: Use the controller for the following block connections, in order to fire
            //       events.
            if (mInputValues != null) {
                for (InputValue inputValue : mInputValues) {
                    Input input = block.getInputByName(inputValue.mName);
                    if (input == null) {
                        throw new BlockLoadingException(
                                toString() + ": No input with name \"" + inputValue.mName + "\"");
                    }
                    Connection connection = input.getConnection();
                    if (connection == null) {
                        throw new BlockLoadingException(
                                "Input \"" + inputValue.mName + "\" does not have a connection.");
                    }
                    block.connectOrThrow(
                            input.getType() == Input.TYPE_STATEMENT ? "statement" : "value",
                            connection, inputValue.mChild, inputValue.mShadow);
                }
            }

            if (mNextChild != null || mNextShadow != null) {
                Connection connection = block.getNextConnection();
                if (connection == null) {
                    throw new BlockLoadingException(
                            this + "does not have a connection for next child.");
                }
                block.connectOrThrow("next", connection, mNextChild, mNextShadow);
            }
        }
    }
}
