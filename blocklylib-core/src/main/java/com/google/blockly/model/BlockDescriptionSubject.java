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

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Literate API to describe a new block, used as an input to {@link BlockFactory#obtain}.
 * BlockDescriptionSubject class is not intended to be referenced directly by name. Instead use the
 * static method {@link BlockFactory#block} to construct a new instance.
 */
public class BlockDescriptionSubject {
    // Only one of the following three may be set.
    String mDefinitionName = null;
    BlockDefinition mDefinition = null;
    Block mCopySource = null;

    String mId = null;
    boolean mIsShadow = false;

    /**
     * Create a new block descriptor. Prefer using {@link BlockFactory#block()} via static import
     * for readability.
     */
    public BlockDescriptionSubject() {}

    /**
     * Copy constructor to create a new description based on a prior.
     */
    public BlockDescriptionSubject(BlockDescriptionSubject src) {
        mDefinitionName = src.mDefinitionName;
        mDefinition = src.mDefinition;
        mId = src.mId;
        mIsShadow = src.mIsShadow;
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
    public BlockDescriptionSubject ofType(String definitionName) {
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
    public BlockDescriptionSubject fromDefinition(BlockDefinition definition) {
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
    public BlockDescriptionSubject fromJson(String json) {
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
    public BlockDescriptionSubject fromJson(JSONObject json) {
        checkDefinitionAndCopySourceUnset();  // Throws if already set.
        try {
            mDefinition = new BlockDefinition(json);
            return this;
        } catch (JSONException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Sets a block as an example to copy.
     *
     * <pre>
     * {@code blockFactory.obtain(block().fromJson("{\"output\": \"Number\"}"));}
     * </pre>
     *
     * @param source The JSON definition of the block.
     * @return This block descriptor, for chaining.
     */
    public BlockDescriptionSubject copyOf(Block source) {
        checkDefinitionAndCopySourceUnset();  // Throws if already set.
        mCopySource = source;
        return this;
    }

    /**
     * Sets the id of the block to be created.
     *
     * <pre>
     * {@code blockFactory.obtain(block().withId("my-block"));}
     * </pre>
     *
     * @param id The id of the block to be created.
     * @return This block descriptor, for chaining.
     */
    public BlockDescriptionSubject withId(String id) {
        if (mId != null) {
            throw new IllegalStateException("Block ID already assigned.");
        }
        mId = id;
        return this;
    }

    /**
     * Declares the new block will be a shadow block. Shadow blocks are usually used as default
     * values for inputs, especially in ToolBox XMl.
     *
     * <pre>
     * {@code blockFactory.obtain(block().ofType("math_number").shadow());}
     * </pre>
     *
     * @return This block descriptor, for chaining.
     */
    public BlockDescriptionSubject shadow() {
        mIsShadow = true;
        return this;
    }

    private void checkDefinitionAndCopySourceUnset() {
        if (mDefinition != null || mDefinitionName != null || mCopySource != null) {
            throw new IllegalStateException("Definition or copy source already set.");
        }
    }
}
