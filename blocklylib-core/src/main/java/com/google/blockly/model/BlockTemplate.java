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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Template of a block, describing the initial state of a new block.
 *
 * The API of this class is designed to be used in a literate manner, as an input to
 * {@link BlockFactory#obtain}. The static method {@link BlockFactory#block()} (which returns a
 * {@code BlockTemplate}) can be used as a via static import to read like English.
 *
 * <pre>
 * {@code factory.obtain(block().shadow().ofType("math_number").atPosition(25,56));}
 * </pre>
 */
public class BlockTemplate {
    protected static class FieldValue {
        /** The name of the field. */
        final String mName;
        /** The serialized value of the field. */
        final String mValue;

        FieldValue(String name, String value) {
            mName = name;
            mValue = value;
        }
    }

    protected static class InputValue {
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

    // Member fields are accessed directly from {@link Block#applyTemplate(BlockTemplate)}.

    // Only one of the following three may be set.
    protected String mDefinitionName = null;
    protected BlockDefinition mDefinition = null;
    protected Block mCopySource = null;

    protected String mId = null;
    protected boolean mAllowAlternateId = true;

    // Mutable state variables below.
    // Applied via Block.applyTemplate()
    // If not set, the block's value is unmodified.
    protected Boolean mIsShadow = null;
    protected WorkspacePoint mPosition = null;
    protected Boolean mIsCollapsed = null;
    protected Boolean mIsDeletable = null;
    protected Boolean mIsDisabled = null;
    protected Boolean mIsEditable = null;
    protected Boolean mInlineInputs = null;
    protected Boolean mIsMovable = null;
    protected String mCommentText = null;

    /** Ordered list of field names and string values, as loaded during XML deserialization. */
    protected List<FieldValue> mFieldValues;

    /** Ordered list of input names and blocks, as loaded during XML deserialization. */
    protected List<InputValue> mInputValues;

    protected Block mNextChild;
    protected Block mNextShadow;

    /**
     * Create a new block descriptor.
     *
     * Alternatively, consider using {@link BlockFactory#block()} via static import:
     * <pre>
     * {@code factory.obtain(block().ofType("math_number"));}
     * </pre>
     */
    public BlockTemplate() {}

    /**
     * Create a new block descriptor for the named block type.
     *
     * @param blockDefinitionName The name of the block definition to use.
     */
    public BlockTemplate(String blockDefinitionName) {
        ofType(blockDefinitionName);
    }

    /**
     * Copy constructor to create a new description based on a prior.
     */
    public BlockTemplate(BlockTemplate src) {
        if (src.mInputValues != null || src.mNextChild != null || src.mNextShadow != null) {
            throw new IllegalArgumentException(
                    "Cannot copy a template with child Block references.");
        }

        mDefinitionName = src.mDefinitionName;
        mDefinition = src.mDefinition;
        mId = src.mId;
        mAllowAlternateId = src.mAllowAlternateId;

        mIsShadow = src.mIsShadow;
        if (src.mPosition != null) {
            mPosition = new WorkspacePoint(src.mPosition);
        }

        mIsCollapsed = src.mIsCollapsed;
        mIsDeletable = src.mIsDeletable;
        mIsDisabled = src.mIsDisabled;
        mIsEditable = src.mIsEditable;
        mInlineInputs = src.mInlineInputs;
        mIsMovable = src.mIsMovable;
        mCommentText = src.mCommentText;

        if (src.mFieldValues != null) {
            mFieldValues = new ArrayList<>(src.mFieldValues);
        }
        if (src.mInputValues != null) {
            mInputValues = new ArrayList<>(src.mInputValues);
        }
    }

    /**
     * Sets the block definition by name.
     *
     * <pre>
     * {@code blockFactory.obtain(block().ofType("math_number"));}
     * </pre>
     *
     * @param definitionName The name of the definition, as registered with the block.
     * @return This block descriptor, for chaining.
     */
    public BlockTemplate ofType(String definitionName) {
        checkDefinitionAndCopySourceUnset();  // Throws if already set.
        mDefinitionName = definitionName;
        return this;
    }

    /**
     * Sets the block definition manually. Such definitions are might not be registered in the
     * BlockFactory, and so such blocks will not load from XML. Some operations may fail, including
     * duplication. That said, such definitions can be useful for one-off blocks, especially in test
     * code.
     *
     * <pre>
     * {@code blockFactory.obtain(block().fromDefinition(booleanBlockDefinition));}
     * </pre>
     *
     * @param definition The definition of the block.
     * @return This block descriptor, for chaining.
     */
    public BlockTemplate fromDefinition(BlockDefinition definition) {
        checkDefinitionAndCopySourceUnset();  // Throws if already set.
        mDefinition = definition;
        return this;
    }

    /**
     * Sets the block definition manually from a JSON string. Such definitions are not registered in
     * the BlockFactory, and so such blocks will not load from XML. Some operations may fail,
     * including duplication. That said, such definitions can be useful for one-off blocks,
     * especially in test code.
     *
     * <pre>
     * {@code blockFactory.obtain(block().fromJson("{\"output\": \"Number\"}"));}
     * </pre>
     *
     * @param json The JSON definition of the block.
     * @return This block descriptor, for chaining.
     */
    public BlockTemplate fromJson(String json) {
        checkDefinitionAndCopySourceUnset();  // Throws if already set.
        try {
            mDefinition = new BlockDefinition(json);
            return this;
        } catch (JSONException e) {
            throw new IllegalArgumentException("Failed to load definition.", e);
        }
    }

    /**
     * Sets the block definition manually from a JSON object. Such definitions are not registered in
     * the BlockFactory, and so such blocks will not load from XML. Some operations may fail,
     * including duplication. That said, such definitions can be useful for one-off blocks,
     * especially in test code.
     *
     * <pre>
     * {@code blockFactory.obtain(block().fromJson(myJsonDefinition));}
     * </pre>
     *
     * @param json The JSON definition of the block.
     * @return This block descriptor, for chaining.
     */
    public BlockTemplate fromJson(JSONObject json) {
        checkDefinitionAndCopySourceUnset();  // Throws if already set.
        try {
            mDefinition = new BlockDefinition(json);
            return this;
        } catch (JSONException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Sets a block as an example to copy. The source will be serialized and deserialized in the
     * copy process. No additional data (e.g., block definition or mutable state) will be stored in
     * this template.
     *
     * <pre>
     * {@code blockFactory.obtain(block().copyOf(otherBlock));}
     * </pre>
     *
     * @param source The JSON definition of the block.
     * @return This block descriptor, for chaining.
     */
    public BlockTemplate copyOf(Block source) {
        checkDefinitionAndCopySourceUnset();  // Throws if already set.
        mCopySource = source;
        return this;
    }

    /**
     * Sets the id of the block to be created. If the id is already in use, a new id will be
     * generated for the new block.
     *
     * <pre>
     * {@code blockFactory.obtain(block().withId("my-block"));}
     * </pre>
     *
     * @param id The id of the block to be created.
     * @return This block descriptor, for chaining.
     */
    public BlockTemplate withId(String id) {
        mId = id;
        mAllowAlternateId = true;
        return this;
    }

    /**
     * Sets the id of the block to be created. In the case of a conflict with an existing block id,
     * if {@code required} is true, obtaining the block will fail. Otherwise, a new id will be
     * generated for the new block.
     *
     * <pre>
     * {@code blockFactory.obtain(block().withRequiredId("this-exact-id"));}
     * </pre>
     *
     * @param id The id of the block to be created.
     * @return This block descriptor, for chaining.
     */
    public BlockTemplate withRequiredId(String id, boolean required) {
        mId = id;
        mAllowAlternateId = !required;
        return this;
    }

    /**
     * Declares the new block will be a shadow block. This is a literate API shorthand for
     * {@code template.shadow(true);}.
     *
     * <pre>
     * {@code blockFactory.obtain(block().shadow().ofType("math_number"));}
     * </pre>
     *
     * @return This block descriptor, for chaining.
     */
    public BlockTemplate shadow() {
        mIsShadow = true;
        return this;
    }

    /**
     * Declares whether the new block will be a shadow block. Shadow blocks are usually used as
     * default values for inputs, especially in ToolBox XMl.
     *
     * <pre>
     * {@code blockFactory.obtain(block().copyOf(originalShadow).shadow(false));}
     * </pre>
     *
     * @param isShadow Whether the block will be a shadow.
     * @return This block descriptor, for chaining.
     */
    public BlockTemplate shadow(boolean isShadow) {
        mIsShadow = isShadow;
        return this;
    }

    /**
     * Sets the position of the resulting block.
     *
     * @param x The horizontal coordinate of the workspace position.
     * @param y The vertical coordinate of the workspace position.
     * @return This block descriptor, for chaining.
     */
    public BlockTemplate atPosition(float x, float y) {
        mPosition = new WorkspacePoint(x, y);
        return this;
    }

    /**
     * @param isCollapsed The collapsed state of the resulting block.
     * @return This block descriptor, for chaining.
     */
    public BlockTemplate collapsed(boolean isCollapsed) {
        mIsCollapsed = isCollapsed;
        return this;
    }

    /**
     * @param isDeletable Whether users will be able to delete the resulting block.
     * @return This block descriptor, for chaining.
     */
    public BlockTemplate deletable(boolean isDeletable) {
        mIsDeletable = isDeletable;
        return this;
    }

    /**
     * @param isDisabled Whether the result block will be disabled.
     * @return This block descriptor, for chaining.
     */
    public BlockTemplate disabled(boolean isDisabled) {
        mIsDisabled = isDisabled;
        return this;
    }

    /**
     * @param isEditable Whether fields on the resulting block will be editable.
     * @return This block descriptor, for chaining.
     */
    public BlockTemplate editable(boolean isEditable) {
        mIsEditable = isEditable;
        return this;
    }

    /**
     * @param isInline Whether inputs will be inlined on the resulting block.
     * @return This block descriptor, for chaining.
     */
    public BlockTemplate withInlineInputs(boolean isInline) {
        mInlineInputs = isInline;
        return this;
    }

    /**
     * @param isMovable Whether users will be able to move the resulting block.
     * @return This block descriptor, for chaining.
     */
    public BlockTemplate movable(boolean isMovable) {
        mIsMovable = isMovable;
        return this;
    }

    /**
     * @param commentText The comment text of the resulting block.
     * @return This block descriptor, for chaining.
     */
    public BlockTemplate withComment(String commentText) {
        mCommentText = commentText;
        return this;
    }

    /**
     * @return
     */
    public String toString() {
        return toString("BlockTemplate");
    }

    public String toString(String prefix) {
        String msg = prefix;
        if (mDefinitionName != null) {
            msg += " \"" + mDefinitionName + "\"";
        } else if (mDefinition != null && mDefinition.getName() != null) {
            msg += " \"" + mDefinition.getName() + "\"";
        }
        if (mId != null) {
            msg += " (id=\"" + mId + "\")";
        }
        return msg;
    }

    /**
     * Sets a field's value immediately after creation.
     *
     * This method is package private because the API of this method is subject to change. Do not
     * use it in application code.
     *
     * @param fieldName The name of the field.
     * @param value The serialized field value.
     * @return This block descriptor, for chaining.
     */
    BlockTemplate withFieldValue(String fieldName, String value) {
        assert(!TextUtils.isEmpty(fieldName));
        assert(!TextUtils.isEmpty(value));
        if (mFieldValues == null) {
            mFieldValues = new ArrayList<>();
        }
        mFieldValues.add(new FieldValue(fieldName, value));
        return this;
    }

    /**
     * Sets a field's value immediately after creation.
     * Generally only used during XML deserialization.
     *
     * This method is package private because the API of this method is subject to change. Do not
     * use it in application code.
     *
     * @param inputName The name of the field.
     * @param child The deserialized child block.
     * @param shadow The deserialized shadow block.
     * @return This block descriptor, for chaining.
     * @throws IllegalArgumentException If inputName is not a valid name; if child or shadow are not
     *                                  configured as such; if child or shadow overwrites a prior
     *                                  value.
     */
    BlockTemplate withInputValue(String inputName, Block child, Block shadow) {
        if (inputName == null
                || (inputName = inputName.trim()).length() == 0) {  // Trim and test name
            throw new IllegalArgumentException("Invalid input value name.");
        }
        // Validate child block shadow state and upward connection.
        if (child != null && (child.isShadow() || child.getUpwardsConnection() == null)) {
            throw new IllegalArgumentException("Invalid input value block.");
        }
        if (shadow != null && (!shadow.isShadow() || shadow.getUpwardsConnection() == null)) {
            throw new IllegalArgumentException("Invalid input shadow block.");
        }

        // Find duplicate values.
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
     * Sets a block next children immediately after creation.
     * Generally only used during XML deserialization.
     *
     * This method is package private because the API of this method is subject to change. Do not
     * use it in application code.
     *
     * @param child The deserialized child block.
     * @param shadow The deserialized shadow block.
     * @return This block descriptor, for chaining.
     */
    BlockTemplate withNextChild(Block child, Block shadow) {
        if ((child != null && (child.isShadow() || child.getPreviousConnection() == null))
                || ((shadow != null) && (!shadow.isShadow() || shadow.getPreviousConnection() == null))) {
            throw new IllegalArgumentException("Invalid next child Block(s).");
        }
        mNextChild = child;
        mNextShadow = shadow;
        return this;
    }

    private void checkDefinitionAndCopySourceUnset() {
        if (mDefinition != null || mDefinitionName != null || mCopySource != null) {
            throw new IllegalStateException("Definition or copy source already set.");
        }
    }
}
