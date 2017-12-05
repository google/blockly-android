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

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Template of a block, describing the initial state of a new block. Most methods return the
 * template, for use in chaining.
 */
public class BlockTemplate {
    private static final String TAG = "BlockTemplate";

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

    // Member fields are accessed directly from {@link Block#applyTemplate(BlockTemplate)}.

    // Only one of the following three may be set.
    protected String mTypeName = null;
    protected BlockDefinition mDefinition = null;
    protected Block mCopySource = null;

    protected String mId = null;

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
    protected String mMutation = null;

    /** Ordered list of field names and string values, as loaded during XML deserialization. */
    protected List<FieldValue> mFieldValues;

    protected Block mNextChild;
    protected Block mNextShadow;

    /**
     * Create a new block template.
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
        mTypeName = src.mTypeName;
        mDefinition = src.mDefinition;
        mId = src.mId;

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
    }

    /**
     * Applies the mutable state (block object values that can be updated after construction)
     * described by a template. Called after extensions are applied to the block (and thus event
     * handles and mutators have been registered).
     * @param block The block to update.
     * @throws BlockLoadingException
     */
    public void applyMutableState(Block block) throws BlockLoadingException {
        // Must apply mutation first, to ensure the block inputs, fields, and connectors, etc. are
        // updated and ready for the latter mutable state.
        if (mMutation != null) {
            if (block.getMutator() != null) {
                block.setMutation(mMutation);
            } else {
                Log.w(TAG, toString() + ": Ignoring <mutation> on " + this + " without mutator.");
            }
        }

        if (mFieldValues != null) {
            for (BlockTemplate.FieldValue fieldValue : mFieldValues) {
                Field field = block.getFieldByName(fieldValue.mName);
                if (field == null) {
                    Log.w(TAG, "Ignoring non-existent field \"" + field + "\" in " + this);
                } else {
                    if (!field.setFromString(fieldValue.mValue)) {
                        throw new BlockLoadingException(
                                "Failed to set a field's value from XML.");
                    }
                }
            }
        }

        if (mIsShadow != null) {
            block.setShadow(mIsShadow);
        }
        if (mPosition != null) {
            block.setPosition(mPosition.x, mPosition.y);
        }
        if (mIsCollapsed != null) {
            block.setCollapsed(mIsCollapsed);
        }
        if (mCommentText != null) {
            block.setComment(mCommentText);
        }
        if (mIsDeletable != null) {
            block.setDeletable(mIsDeletable);
        }
        if (mIsDisabled != null) {
            block.setDisabled(mIsDisabled);
        }
        if (mIsEditable != null) {
            block.setEditable(mIsEditable);
        }
        if (mInlineInputs != null) {
            block.setInputsInline(mInlineInputs);
        }
        if (mIsMovable != null) {
            block.setMovable(mIsMovable);
        }
    }

    /**
     * Sets the block definition by name.
     *
     * <pre>
     * {@code blockFactory.obtainBlockFrom(new BlockTemplate().ofType("math_number"));}
     * </pre>
     *
     * @param definitionName The name of the definition, as registered with the block.
     * @return This block template, for chaining.
     */
    public BlockTemplate ofType(String definitionName) {
        checkDefinitionAndCopySourceNotSet();  // Throws if already set.
        mTypeName = definitionName;
        return this;
    }

    /**
     * Sets the block definition manually. Such definitions are might not be registered in the
     * BlockFactory, and so such blocks will not load from XML. Some operations may fail, including
     * duplication. That said, such definitions can be useful for one-off blocks, especially in test
     * code.
     *
     * <pre>{@code
     * blockFactory.obtainBlockFrom(new BlockTemplate().fromDefinition(booleanBlockDefinition));
     * }</pre>
     *
     * @param definition The definition of the block.
     * @return This block template, for chaining.
     */
    public BlockTemplate fromDefinition(BlockDefinition definition) {
        checkDefinitionAndCopySourceNotSet();  // Throws if already set.
        mDefinition = definition;
        return this;
    }

    /**
     * Sets the block definition manually from a JSON string. Such definitions are not registered in
     * the BlockFactory, and so such blocks will not load from XML. Some operations may fail,
     * including duplication. That said, such definitions can be useful for one-off blocks,
     * especially in test code.
     *
     * <pre>{@code
     * blockFactory.obtainBlockFrom(new BlockTemplate().fromJson("{\"output\": \"Number\"}"));
     * }</pre>
     *
     * @param json The JSON definition of the block.
     * @return This block template, for chaining.
     */
    public BlockTemplate fromJson(String json) throws BlockLoadingException {
        checkDefinitionAndCopySourceNotSet();  // Throws if already set.
        mDefinition = new BlockDefinition(json);
        return this;
    }

    /**
     * Sets the block definition manually from a JSON object. Such definitions are not registered in
     * the BlockFactory, and so such blocks will not load from XML. Some operations may fail,
     * including duplication. That said, such definitions can be useful for one-off blocks,
     * especially in test code.
     *
     * <pre>
     * {@code blockFactory.obtainBlockFrom(new BlockTemplate().fromJson(myJsonDefinition));}
     * </pre>
     *
     * @param json The JSON definition of the block.
     * @return This block template, for chaining.
     */
    public BlockTemplate fromJson(JSONObject json) throws BlockLoadingException {
        checkDefinitionAndCopySourceNotSet();  // Throws if already set.
        mDefinition = new BlockDefinition(json);
        return this;
    }

    /**
     * Sets a block as an example to copy. The source will be serialized and deserialized in the
     * copy process. No additional data (e.g., block definition or mutable state) will be stored in
     * this template.
     *
     * <pre>
     * {@code blockFactory.obtainBlockFrom(new BlockTemplate().copyOf(otherBlock));}
     * </pre>
     *
     * @param source The JSON definition of the block.
     * @return This block template, for chaining.
     */
    public BlockTemplate copyOf(Block source) {
        checkDefinitionAndCopySourceNotSet();  // Throws if already set.
        mCopySource = source;
        return this;
    }

    /**
     * Sets the id of the block to be created. If the id is already in use, a new id will be
     * generated for the new block.
     *
     * <pre>
     * {@code blockFactory.obtainBlockFrom(new BlockTemplate().withId("my-block"));}
     * </pre>
     *
     * @param id The id of the block to be created.
     * @return This block template, for chaining.
     */
    public BlockTemplate withId(String id) {
        mId = id;
        return this;
    }

    /**
     * Declares the new block will be a shadow block. This is true, even if the template copies
     * a non-shadow block via {@link #copyOf(Block)}, independent of call order.
     *
     * This is a fluent API shorthand for {@code template.shadow(true);}.
     *
     * <pre>
     * {@code blockFactory.obtainBlockFrom(new BlockTemplate().shadow().ofType("math_number"));}
     * </pre>
     *
     * @return This block template, for chaining.
     */
    public BlockTemplate shadow() {
        mIsShadow = true;
        return this;
    }

    /**
     * Declares whether the new block will be a shadow block. Shadow blocks are usually used as
     * default values for inputs, especially in ToolBox XMl.
     *
     * <pre>{@code
     * blockFactory.obtainBlockFrom(new BlockTemplate().copyOf(originalShadow).shadow(false));
     * }</pre>
     *
     * @param isShadow Whether the block will be a shadow.
     * @return This block template, for chaining.
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
     * @return This block template, for chaining.
     */
    public BlockTemplate atPosition(float x, float y) {
        mPosition = new WorkspacePoint(x, y);
        return this;
    }

    /**
     * @param isCollapsed The collapsed state of the resulting block.
     * @return This block template, for chaining.
     */
    public BlockTemplate collapsed(boolean isCollapsed) {
        mIsCollapsed = isCollapsed;
        return this;
    }

    /**
     * @param isDeletable Whether users will be able to delete the resulting block.
     * @return This block template, for chaining.
     */
    public BlockTemplate deletable(boolean isDeletable) {
        mIsDeletable = isDeletable;
        return this;
    }

    /**
     * @param isDisabled Whether the result block will be disabled.
     * @return This block template, for chaining.
     */
    public BlockTemplate disabled(boolean isDisabled) {
        mIsDisabled = isDisabled;
        return this;
    }

    /**
     * @param isEditable Whether fields on the resulting block will be editable.
     * @return This block template, for chaining.
     */
    public BlockTemplate editable(boolean isEditable) {
        mIsEditable = isEditable;
        return this;
    }

    /**
     * @param isInline Whether inputs will be inlined on the resulting block.
     * @return This block template, for chaining.
     */
    public BlockTemplate withInlineInputs(boolean isInline) {
        mInlineInputs = isInline;
        return this;
    }

    /**
     * @param isMovable Whether users will be able to move the resulting block.
     * @return This block template, for chaining.
     */
    public BlockTemplate movable(boolean isMovable) {
        mIsMovable = isMovable;
        return this;
    }

    /**
     * @param commentText The comment text of the resulting block.
     * @return This block template, for chaining.
     */
    public BlockTemplate withComment(String commentText) {
        mCommentText = commentText;
        return this;
    }

    /**
     * @param mutationString The mutator's serialized state. I.e, the {@code <mutation>} element.
     * @return This block template, for chaining.
     */
    public BlockTemplate withMutation(String mutationString) {
        mMutation = mutationString;
        return this;
    }

    /**
     * @return a string describing this template.
     */
    public String toString() {
        return toString("BlockTemplate");
    }

    /**
     * Constructs a string that describes this template, using the provided descriptive noun.
     * @param nounPrefix A generic noun, usually "BlockTemplate" or "Block".
     * @return a string describing this template, using the noun as a prefix.
     */
    public String toString(String nounPrefix) {
        String msg = nounPrefix;
        if (mTypeName != null) {
            msg += " \"" + mTypeName + "\"";
        } else if (mDefinition != null && mDefinition.getTypeName() != null) {
            msg += " \"" + mDefinition.getTypeName() + "\"";
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
     * @return This block template, for chaining.
     */
    BlockTemplate withFieldValue(String fieldName, String value) {
        if (TextUtils.isEmpty(fieldName)) {
            throw new IllegalArgumentException("fieldName must be defined, non-empty.");
        }
        if (value == null) {
            throw new IllegalArgumentException("fieldName must be defined.");
        }
        if (mFieldValues == null) {
            mFieldValues = new ArrayList<>();
        }
        mFieldValues.add(new FieldValue(fieldName, value));
        return this;
    }

    /**
     * Checks that this template has yet been configured with a BlockDefinition, whether directly,
     * via name, or via block to copy.
     * @throws IllegalStateException If the BlockDefinition has already been assigned.
     */
    private void checkDefinitionAndCopySourceNotSet() {
        if (mDefinition != null || mTypeName != null || mCopySource != null) {
            throw new IllegalStateException("Definition or copy source already set.");
        }
    }
}
