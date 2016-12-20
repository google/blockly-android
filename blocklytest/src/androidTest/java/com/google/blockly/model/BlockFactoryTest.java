/*
 *  Copyright 2016 Google Inc. All Rights Reserved.
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

import android.support.test.InstrumentationRegistry;

import com.google.blockly.android.R;
import com.google.blockly.utils.BlockLoadingException;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import static com.google.blockly.utils.MoreAsserts.assertStringNotEmpty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


/**
 * Tests for {@link BlockFactory}.
 */
public class BlockFactoryTest {
    private XmlPullParserFactory xmlPullParserFactory;
    private BlockFactory mBlockFactory;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        xmlPullParserFactory = XmlPullParserFactory.newInstance();
        // TODO(#84): Move test_blocks.json to the testapp's resources.
        mBlockFactory = new BlockFactory(InstrumentationRegistry.getContext(),
            new int[]{R.raw.test_blocks});
    }

    @Test
    public void testJson() throws JSONException, BlockLoadingException {
        JSONObject blockDefinitionJson = new JSONObject(BlockTestStrings.TEST_JSON_STRING);
        Block block = mBlockFactory.fromJson("test_block", blockDefinitionJson);

        assertNotNull("Block was null after initializing from JSON", block);
        assertEquals("Type not set correctly", "test_block", block.getType());
        assertStringNotEmpty("Block id cannot be empty.", block.getId());
        assertEquals("Wrong number of inputs", 2, block.getInputs().size());
        assertEquals("Wrong number of fields in first input",
                9, block.getInputs().get(0).getFields().size());
    }

    @Test
    public void testLoadBlocks() {
        List<Block> blocks = mBlockFactory.getAllBlocks();
        assertEquals("BlockFactory failed to load all blocks.", 21, blocks.size());
    }

    @Test
    public void testSuccessfulLoadFromXml() throws IOException, XmlPullParserException {
        Block loaded = parseBlockFromXml(BlockTestStrings.SIMPLE_BLOCK);
        assertEquals("frankenblock", loaded.getType());
        assertEquals(new WorkspacePoint(37, 13), loaded.getPosition());
    }

    @Test
    public void testBlocksRequireType() throws IOException, XmlPullParserException {
        parseBlockFromXmlFailure(BlockTestStrings.NO_BLOCK_TYPE,
            "Block without a type must fail to load.");
    }

    @Test
    public void testIdsGeneratedByBlockFactory() throws IOException, XmlPullParserException {
        parseBlockFromXml(BlockTestStrings.NO_BLOCK_ID);
    }

    @Test
    public void testOnlyTopLevelBlocksNeedPosition() throws IOException, XmlPullParserException {
        parseBlockFromXml(BlockTestStrings.NO_BLOCK_POSITION);
    }

    @Test
    public void testBlockWithGoodInterior() throws IOException, XmlPullParserException {
        parseBlockFromXml(BlockTestStrings.assembleFrankenblock("block", "1",
            BlockTestStrings.VALUE_GOOD));
    }

    @Test
    public void testBlockWithInteriorWithNoChild() throws IOException, XmlPullParserException {
        parseBlockFromXml(BlockTestStrings.assembleFrankenblock("block", "3",
            BlockTestStrings.VALUE_NO_CHILD));
    }

    // TODO(fenichel): Value: no input connection
    @Test
    public void testBlockWithNoOutputInteriorFails() throws IOException, XmlPullParserException {
        parseBlockFromXmlFailure(BlockTestStrings.assembleFrankenblock("block", "2",
            BlockTestStrings.VALUE_NO_OUTPUT),
            "A <value> child block without an output must fail to load.");
    }

    @Test
    public void testBlockWithBadlyNamedInteriorFails() throws IOException, XmlPullParserException {
        parseBlockFromXmlFailure(BlockTestStrings.assembleFrankenblock("block", "4",
            BlockTestStrings.VALUE_BAD_NAME),
            "A block without a recognized type id must fail to load.");
    }

    @Test
    public void testBlockMultipleValuesForSameInputFails() throws IOException, XmlPullParserException {
        parseBlockFromXmlFailure(BlockTestStrings.assembleFrankenblock("block", "5",
            BlockTestStrings.VALUE_REPEATED),
            "An input <value> with multiple blocks must fail to load.");
    }

    @Test
    public void testBlockWithGoodComment() throws IOException, XmlPullParserException {
        parseBlockFromXml(BlockTestStrings.assembleFrankenblock("block", "6",
            BlockTestStrings.COMMENT_GOOD));
    }

    @Test
    public void testBlockWithEmptyComment() throws IOException, XmlPullParserException {
        parseBlockFromXml(BlockTestStrings.assembleFrankenblock("block", "7",
            BlockTestStrings.COMMENT_NO_TEXT));
    }

    @Test
    public void testBlockWithGoodFields() throws IOException, XmlPullParserException {
        Block loaded = parseBlockFromXml(BlockTestStrings.assembleFrankenblock("block", "8",
            BlockTestStrings.FIELD_HAS_NAME));
        assertEquals("item", ((FieldInput) loaded.getFieldByName("text_input")).getText());
    }

    @Test
    public void testCreateBlockWithMissingFieldName() throws IOException, XmlPullParserException {
        parseBlockFromXml(BlockTestStrings.assembleFrankenblock("block", "9",
            BlockTestStrings.FIELD_MISSING_NAME));
    }

    @Test
    public void testCreateBlockWithUnknownFieldName() throws IOException, XmlPullParserException {
        parseBlockFromXml(BlockTestStrings.assembleFrankenblock("block", "10",
            BlockTestStrings.FIELD_UNKNOWN_NAME));
    }

    @Test
    public void testCreateBlockWithMissingFieldText() throws IOException, XmlPullParserException {
        parseBlockFromXml(BlockTestStrings.assembleFrankenblock("block", "11",
            BlockTestStrings.FIELD_MISSING_TEXT));
    }

    @Test
    public void testBlockWithNoConnectionOnChildBlockFails() throws IOException, XmlPullParserException {
        parseBlockFromXmlFailure(BlockTestStrings.assembleFrankenblock("block", "13",
            BlockTestStrings.STATEMENT_BAD_CHILD),
            "A statement <value> child without a previous connection must fail to load.");
    }

    @Test
    public void testCreateBlockWithBadStatmentNameFails() throws IOException, XmlPullParserException {
        parseBlockFromXmlFailure(BlockTestStrings.assembleFrankenblock("block", "14",
            BlockTestStrings.STATEMENT_BAD_NAME),
            "A block without a recognized type id must fail to load.");
    }

    @Test
    public void testCreateBlockWithValidStatement() throws IOException, XmlPullParserException {
        parseBlockFromXml(BlockTestStrings.assembleFrankenblock("block", "15",
            BlockTestStrings.STATEMENT_GOOD));
    }

    @Test
    public void testCreateBlockWithStatementWithNullChildBlock() throws IOException, XmlPullParserException {
        parseBlockFromXml(BlockTestStrings.assembleFrankenblock("block", "12",
                BlockTestStrings.STATEMENT_NO_CHILD));
    }

    @Test
    public void testLoadFromXmlInlineTagAtStart() throws IOException, XmlPullParserException {
        Block inlineAtStart = parseBlockFromXml(BlockTestStrings.SIMPLE_BLOCK_INLINE_BEGINNING);
        assertTrue(inlineAtStart.getInputsInline());
        assertTrue(inlineAtStart.getInputsInlineModified());
    }

    @Test
    public void testLoadFromXmlInlineTagAtEnd() throws IOException, XmlPullParserException {
        Block inlineAtEnd = parseBlockFromXml(BlockTestStrings.SIMPLE_BLOCK_INLINE_END);
        assertTrue(inlineAtEnd.getInputsInline());
        assertTrue(inlineAtEnd.getInputsInlineModified());
    }

    @Test
    public void testLoadFromXmlInlineTagFalse() throws IOException, XmlPullParserException {
        Block inlineFalseBlock = parseBlockFromXml(BlockTestStrings.SIMPLE_BLOCK_INLINE_FALSE);
        assertFalse(inlineFalseBlock.getInputsInline());
        assertTrue(inlineFalseBlock.getInputsInlineModified());
    }

    @Test
    public void testLoadXmlWithNoInlineTag() throws IOException, XmlPullParserException {
        Block blockNoInline = parseBlockFromXml(BlockTestStrings.SIMPLE_BLOCK);
        assertFalse(blockNoInline.getInputsInline());
        assertFalse(blockNoInline.getInputsInlineModified());
    }

    @Test
    public void testLoadFromXmlSimpleShadow() throws IOException, XmlPullParserException {
        Block loaded = parseShadowFromXml(BlockTestStrings.SIMPLE_SHADOW);
        assertEquals("math_number", loaded.getType());
        assertEquals(new WorkspacePoint(37, 13), loaded.getPosition());
        assertTrue(loaded.isShadow());
    }

    @Test
    public void testLoadFromXmlShadowInValue() throws IOException, XmlPullParserException {
        Block loaded = parseBlockFromXml(BlockTestStrings.assembleFrankenblock("block", "1",
            BlockTestStrings.VALUE_SHADOW));
        Connection conn = loaded.getInputByName("value_input").getConnection();
        assertEquals(conn.getTargetBlock(), conn.getShadowBlock());
        assertTrue(conn.getShadowBlock().isShadow());
    }

    @Test
    public void testLoadFromXmlShadowWithBlockInValue() throws IOException, XmlPullParserException {
        Block loaded = parseBlockFromXml(BlockTestStrings.assembleFrankenblock("block", "2",
            BlockTestStrings.VALUE_SHADOW_GOOD));
        Connection conn = loaded.getInputByName("value_input").getConnection();
        assertEquals("VALUE_REAL", conn.getTargetBlock().getId());
        assertFalse(conn.getTargetBlock().isShadow());
        assertEquals("VALUE_SHADOW", conn.getShadowBlock().getId());
        assertTrue(conn.getShadowBlock().isShadow());
    }

    @Test
    public void testLoadFromXmlShadowInStatement() throws IOException, XmlPullParserException {
        Block loaded = parseBlockFromXml(BlockTestStrings.assembleFrankenblock("block", "3",
            BlockTestStrings.STATEMENT_SHADOW));
        Connection conn = loaded.getInputByName("NAME").getConnection();
        assertEquals(conn.getTargetBlock(), conn.getShadowBlock());
        assertTrue(conn.getShadowBlock().isShadow());
    }

    @Test
    public void testLoadFromXmlShadowWithBlockInStatement() throws IOException, XmlPullParserException {
        Block loaded = parseBlockFromXml(BlockTestStrings.assembleFrankenblock("block", "4",
            BlockTestStrings.STATEMENT_SHADOW_GOOD));
        Connection conn = loaded.getInputByName("NAME").getConnection();
        assertEquals("STATEMENT_REAL", conn.getTargetBlock().getId());
        assertFalse(conn.getTargetBlock().isShadow());
        assertEquals("STATEMENT_SHADOW", conn.getShadowBlock().getId());
        assertTrue(conn.getShadowBlock().isShadow());
    }

    @Test
    public void testLoadFromXmlNestedShadow() throws IOException, XmlPullParserException {
        Block loaded = parseBlockFromXml(BlockTestStrings.assembleFrankenblock("block", "5",
            BlockTestStrings.VALUE_NESTED_SHADOW));
        Connection conn = loaded.getInputByName("value_input").getConnection();
        assertEquals(conn.getTargetBlock(), conn.getShadowBlock());
        Block shadow1 = conn.getShadowBlock();
        assertEquals("SHADOW1", shadow1.getId());
        conn = shadow1.getOnlyValueInput().getConnection();
        assertEquals(conn.getTargetBlock(), conn.getShadowBlock());
        assertEquals("SHADOW2", conn.getShadowBlock().getId());
    }

    @Test
    public void testLoadFromXmlShadowBlockWithShadowChildFails() throws IOException, XmlPullParserException {
        parseBlockFromXmlFailure(BlockTestStrings.assembleFrankenblock("block", "6",
            BlockTestStrings.VALUE_NESTED_SHADOW_BLOCK),
            "A <shadow> containing a normal <block> must fail to load.");
    }

    @Test
    public void testLoadFromXmlBlockWithVariableCannotBeShadowBlock() throws IOException, XmlPullParserException {
        parseBlockFromXmlFailure(BlockTestStrings.assembleFrankenblock("block", "7",
                BlockTestStrings.VALUE_SHADOW_VARIABLE),
                "A <shadow> containing a variable field (at any depth) must fail to load.");
    }

    @Test
    public void testObtainBlock() {
        Block emptyBlock = mBlockFactory.obtainBlock("empty_block", null);
        assertNotNull("Failed to create the empty block.", emptyBlock);
        assertEquals("Empty block has the wrong type", "empty_block", emptyBlock.getType());

        Block frankenblock = mBlockFactory.obtainBlock("frankenblock", null);
        assertNotNull("Failed to create the frankenblock.", frankenblock);

        List<Input> inputs = frankenblock.getInputs();
        assertEquals("Frankenblock has the wrong number of inputs", 3, inputs.size());
        assertTrue("First input should be a value input.",
                inputs.get(0) instanceof Input.InputValue);
        assertTrue("Second input should be a statement input.",
                inputs.get(1) instanceof Input.InputStatement);
        assertTrue("Third input should be a dummy input.",
                inputs.get(2) instanceof Input.InputDummy);

        assertNotNull(frankenblock.getFieldByName("angle"));
    }

    @Test
    public void testObtainBlock_repeatedWithoutUuid() {
        Block frankenblock = mBlockFactory.obtainBlock("frankenblock", null);
        assertNotNull("Failed to create the frankenblock.", frankenblock);

        Block frankencopy = mBlockFactory.obtainBlock("frankenblock", null);
        assertNotSame("Obtained blocks should be distinct objects when uuid is null.",
                frankenblock, frankencopy);

        assertNotSame("Obtained blocks should not share connections.",
                frankenblock.getNextConnection(), frankencopy.getNextConnection());
        assertNotSame("Obtained blocks should not share connections.",
                frankenblock.getPreviousConnection(), frankencopy.getPreviousConnection());
        assertNull(frankenblock.getOutputConnection());
        assertNotSame("Obtained blocks should not share inputs.",
                frankenblock.getInputs().get(0), frankencopy.getInputs().get(0));
    }

    @Test
    public void testObtainBlock_repeatedWithUuid() {
        Block frankenblock = mBlockFactory.obtainBlock("frankenblock", "123");
        assertNotNull("Failed to create the frankenblock.", frankenblock);

        thrown.expect(IllegalArgumentException.class);
        thrown.reportMissingExceptionWithMessage("Cannot create two bblocks with the same id");
        mBlockFactory.obtainBlock("frankenblock", "123");
    }

    @Test
    public void testObtainBlock_repeatedWithUuidMismatchingPrototype() {
        Block frankenblock = mBlockFactory.obtainBlock("frankenblock", "123");
        assertNotNull("Failed to create the frankenblock.", frankenblock);

        thrown.expect(IllegalArgumentException.class);
        thrown.reportMissingExceptionWithMessage("Expected error when requesting a block with " +
            "matching UUID but different prototype");
        mBlockFactory.obtainBlock("empty_block", "123");
    }

    private Block parseBlockFromXml(String testString)
            throws IOException, XmlPullParserException {
        XmlPullParser parser = getXmlPullParser(testString, "block");
        Block loaded = mBlockFactory.fromXml(parser);
        assertNotNull(loaded);
        return loaded;
    }

    private Block parseShadowFromXml(String testString)
            throws IOException, XmlPullParserException {
        XmlPullParser parser = getXmlPullParser(testString, "shadow");
        Block loaded = mBlockFactory.fromXml(parser);
        assertNotNull(loaded);
        return loaded;
    }

    private void parseBlockFromXmlFailure(String testString, String messageIfDoesNotFail)
            throws IOException, XmlPullParserException {

        XmlPullParser parser = getXmlPullParser(testString, "block");

        thrown.expect(BlocklyParserException.class);
        thrown.reportMissingExceptionWithMessage(messageIfDoesNotFail);
        mBlockFactory.fromXml(parser);
    }

    /**
     * Creates a pull parser with the given input and gobbles up to the first start tag that equals
     * {@code returnFirstInstanceOf}.
     */
    private XmlPullParser getXmlPullParser(String input, String returnFirstInstanceOf) {
        try {
            xmlPullParserFactory.setNamespaceAware(true);
            XmlPullParser parser = xmlPullParserFactory.newPullParser();

            parser.setInput(new ByteArrayInputStream(input.getBytes()), null);

            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG
                        && parser.getName().equalsIgnoreCase(returnFirstInstanceOf)) {
                    return parser;
                }
                eventType = parser.next();
            }
        } catch (XmlPullParserException | IOException e) {
            throw new BlocklyParserException(e);
        }
        return null;
    }
}
