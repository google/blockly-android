package com.google.blockly.model;

import com.google.blockly.utils.BlockLoadingException;
import com.google.blockly.utils.BlocklyXmlHelper;

import org.junit.Before;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests Mutator and related classes.
 */
public class MutatorTest {
    private static final String BLOCK_TYPE = "block type";
    private static final String MUTATOR_ID = "mutator_id";
    private static final String STARTING_VALUE = "starting value";
    private static final String UPDATED_ATTR = "updated attrib";
    private static final String UPDATED_TEXT = "updated text";

    BlockFactory mFactory;
    BlockExtension mMutatorExtension;
    BlockDefinition mDefinition;

    @Before
    public void setUp() throws BlockLoadingException {
        assertThat(STARTING_VALUE).isNotEqualTo(UPDATED_ATTR);
        assertThat(STARTING_VALUE).isNotEqualTo(UPDATED_TEXT);

        mFactory = new BlockFactory();
        mMutatorExtension = new ExampleMutatorExtension();
        mDefinition = new BlockDefinition(
                "{\"type\": \"" + BLOCK_TYPE + "\","
                + "\"mutator\": \"" + MUTATOR_ID + "\"}"
        );
    }

    @Test
    public void testMutatorLifecycle() throws BlockLoadingException, BlocklySerializerException {
        mFactory.registerExtension(MUTATOR_ID, mMutatorExtension);
        mFactory.addDefinition(mDefinition);
        Block block = mFactory.obtainBlockFrom(new BlockTemplate().ofType(BLOCK_TYPE));

        ExampleMutator mutator = (ExampleMutator) block.getMutator();
        assertThat(mutator).isNotNull();
        assertThat(mutator.mAttrib).isEqualTo(STARTING_VALUE);
        assertThat(mutator.mText).isEqualTo(STARTING_VALUE);

        mutator.mAttrib = UPDATED_ATTR;
        mutator.mText = UPDATED_TEXT;

        String xml = BlocklyXmlHelper.writeBlockToXml(block, IOOptions.WRITE_ALL_BLOCKS_WITHOUT_ID);
        assertThat(xml).contains(UPDATED_ATTR);
        assertThat(xml).contains(UPDATED_TEXT);

        Block copy = BlocklyXmlHelper.loadOneBlockFromXml(xml, mFactory);
        ExampleMutator copyMutator = (ExampleMutator) copy.getMutator();
        assertThat(copyMutator).isNotNull();
        assertThat(copyMutator.mAttrib).isEqualTo(UPDATED_ATTR);
        assertThat(copyMutator.mText).isEqualTo(UPDATED_TEXT);
    }

    public static class ExampleMutator extends Mutator {
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

    public static class ExampleMutatorExtension implements BlockExtension {
        List<ExampleMutator> mMutatorsCreated = new ArrayList<>();

        @Override
        public void applyTo(Block block) throws BlockLoadingException {
            ExampleMutator mutator = new ExampleMutator();
            mMutatorsCreated.add(mutator);
            block.setMutator(mutator);
        }
    }
}
