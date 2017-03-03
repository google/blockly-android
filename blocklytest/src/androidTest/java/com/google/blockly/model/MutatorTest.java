/*
 *  Copyright 2017 Google Inc. All Rights Reserved.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.google.blockly.model;

import com.google.blockly.utils.BlockLoadingException;
import com.google.blockly.utils.BlocklyXmlHelper;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests {@link Mutator} and related classes.
 */
public class MutatorTest {
    private static final String BLOCK_TYPE = "block type";
    private static final String MUTATOR_ID = "mutator_id";
    private static final String STARTING_VALUE = "starting value";
    private static final String UPDATED_ATTR = "updated attrib";
    private static final String UPDATED_TEXT = "updated text";

    BlockFactory mFactory;
    Mutator.Factory mMutatorFactory;

    @Before
    public void setUp() throws BlockLoadingException, IOException {
        assertThat(STARTING_VALUE).isNotEqualTo(UPDATED_ATTR);
        assertThat(STARTING_VALUE).isNotEqualTo(UPDATED_TEXT);

        mFactory = new BlockFactory();
        mFactory.addJsonDefinitions(
                "[{\"type\": \"" + BLOCK_TYPE + "\","
                + "\"mutator\": \"" + MUTATOR_ID + "\"}]"
        );

        mMutatorFactory = new ExampleMutator.Factory();
        mFactory.registerMutator(MUTATOR_ID, mMutatorFactory);
    }

    /**
     * Tests the full Mutator life cycle, from extension registration, block construction via JSON
     * definition, serialization, and deserialization.
     */
    @Test
    public void testMutatorLifecycle() throws BlockLoadingException, BlocklySerializerException {
        Block block = mFactory.obtainBlockFrom(new BlockTemplate().ofType(BLOCK_TYPE));

        ExampleMutator mutator = (ExampleMutator) block.getMutator();
        assertThat(mutator).isNotNull();
        assertThat(mutator.mAttrib).isEqualTo(STARTING_VALUE);
        assertThat(mutator.mText).isEqualTo(STARTING_VALUE);
        Mockito.verify(mutator).onAttached(block);

        mutator.mAttrib = UPDATED_ATTR;
        mutator.mText = UPDATED_TEXT;

        String xml = BlocklyXmlHelper.writeBlockToXml(block, IOOptions.WRITE_ALL_BLOCKS_WITHOUT_ID);
        assertThat(xml).contains(UPDATED_ATTR);
        assertThat(xml).contains(UPDATED_TEXT);

        Block blockCopy = BlocklyXmlHelper.loadOneBlockFromXml(xml, mFactory);
        ExampleMutator mutatorCopy = (ExampleMutator) blockCopy.getMutator();
        assertThat(mutatorCopy).isNotNull();
        assertThat(mutatorCopy.mAttrib).isEqualTo(UPDATED_ATTR);
        assertThat(mutatorCopy.mText).isEqualTo(UPDATED_TEXT);
        Mockito.verify(mutatorCopy).onAttached(blockCopy);
    }

    public static class ExampleMutator extends Mutator {
        static class Factory implements Mutator.Factory<ExampleMutator> {
            @Override
            public ExampleMutator newMutator() {
                return Mockito.spy(new ExampleMutator());
            }
        }

        String mAttrib = STARTING_VALUE;
        String mText = STARTING_VALUE;

        @Override
        public void serialize(XmlSerializer serializer) throws IOException {
            serializer.startTag("", "mutation");
            serializer.attribute("", "attr", mAttrib);
            serializer.text(mText);
            serializer.endTag("", "mutation");
        }

        @Override
        public void update(Block block, XmlPullParser parser)
                throws IOException, XmlPullParserException {
            // Crude parse
            assertThat(parser.next()).isEqualTo(XmlPullParser.START_TAG);
            mAttrib = parser.getAttributeValue(0);
            assertThat(parser.next()).isEqualTo(XmlPullParser.TEXT);
            mText = parser.getText();
            assertThat(parser.next()).isEqualTo(XmlPullParser.END_TAG);
        }
    }
}
