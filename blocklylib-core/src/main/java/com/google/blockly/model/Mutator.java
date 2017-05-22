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

import android.support.annotation.Nullable;

import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.utils.BlockLoadingException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;

/**
 * Interface for mutators, platform specific hooks into blocks that manage the {@code <mutation>}
 * serialized state and any related block updates.
 * <p/>
 * Mutators can be added to blocks by setting the {@code "mutator"} attribute in JSON block
 * definition. With this, the block constructor will call
 * {@link BlockFactory#applyMutator(String, Block)}.
 * <p/>
 * See <a href="https://developers.google.com/blockly/guides/create-custom-blocks/mutators">guide on
 * extensions and mutators</a>.
 */
public abstract class Mutator {
    /** The tag name for the XML element when serializing and updating Mutators. */
    public static final String TAG_MUTATION = "mutation";

    /**
     * The factory class for this type of mutator.
     * @param <T> The type of Mutator constructed.
     */
    public interface Factory<T extends Mutator> {
        /**
         * @return The new Mutator instance.
         */
        T newMutator(BlocklyController controller);

        /**
         * @return The id used to register this mutator.
         */
        String getMutatorId();
    }

    protected Block mBlock;

    private final String mMutatorId;

    /**
     * Mutators are required to be initialized with the factory used to create them.
     */
    protected Mutator(Factory factory) {
        mMutatorId = factory.getMutatorId();
    }

    /**
     * This is called when a mutator is attached to a block. Developers wishing to perform
     * setup on the block should override {@link #onAttached(Block)}.
     *
     * @param block The block this mutator is attached to.
     */
    public final void attachToBlock(Block block) {
        mBlock = block;
        onAttached(block);
    }

    /**
     * This is called when a mutator is detached from a block. Developers wishing to perform
     * teardown on the block should override {@link #onDetached(Block)}.
     */
    public final void detachFromBlock() {
        final Block block = mBlock;
        onDetached(block);
        mBlock = null;
    }

    /**
     * @return The id that was used to register this mutator's factory.
     */
    public final String getMutatorId() {
        return mMutatorId;
    }

    /**
     * @return The block this mutator is currently attached to.
     */
    @Nullable
    public Block getBlock() {
        return mBlock;
    }

    /**
     * Called immediately after the mutator is attached to the block. Can be used to perform
     * additional block initialization related to this mutator.
     */
    protected void onAttached(Block block) {
        // Do nothing by default.
    }

    /**
     * Called immediately after the mutator is detached from a block, usually as a result of
     * destroying the block.
     */
    protected void onDetached(Block block) {
        // Do nothing by default
    }

    // TODO: onAttachToWorkspace(Block, Workspace) and onDetachFromWorkspace(Block, Workspace)

    /**
     * Serializes the Mutator's state to an XML {@code <mutation>} element.
     *
     * Compare with {@code block.mutationToDom()} on web Blockly (added by extensions or mixins),
     * or {@code Mutator.toXMLElement()} on blockly-ios.
     *
     * @param serializer
     * @throws IOException
     */
    public abstract void serialize(XmlSerializer serializer) throws IOException;

    /**
     * Updates the mutator state from the provided {@code <mutation>} XML. The parser state is such
     * that {@code parser.next()} will return {@code START_TAG}, the beginning of the
     * {@code <mutation>} element.
     *
     * Compare with {@code block.domToMutation()} on web Blockly (added by extensions or mixins),
     * or {@code Mutator.update()} on blockly-ios.
     * @param parser The parser with the {@code <mutation>} element.
     * @throws IOException If the parser cannot read its source.
     * @throws XmlPullParserException If the parser cannot parse into XML.
     * @throws BlockLoadingException If the XML is not what the mutator expected.
     */
    public abstract void update(XmlPullParser parser)
            throws BlockLoadingException, IOException, XmlPullParserException;
}
