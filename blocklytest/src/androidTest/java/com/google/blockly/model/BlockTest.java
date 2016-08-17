/*
 *  Copyright 2015 Google Inc. All Rights Reserved.
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

import android.test.AndroidTestCase;

import com.google.blockly.android.R;
import com.google.blockly.utils.BlockLoadingException;
import com.google.blockly.utils.BlocklyXmlHelper;
import com.google.blockly.utils.StringOutputStream;

import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.test.MoreAsserts.assertNotEqual;

/**
 * Tests for {@link Block}.
 */
public class BlockTest extends AndroidTestCase {
    private XmlPullParserFactory xmlPullParserFactory;
    private BlockFactory mBlockFactory;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        xmlPullParserFactory = XmlPullParserFactory.newInstance();
        mBlockFactory = new BlockFactory(getContext(), new int[]{R.raw.test_blocks});
    }

    public void testJson() throws JSONException, BlockLoadingException {
        JSONObject blockDefinitionJson = new JSONObject(BlockTestStrings.TEST_JSON_STRING);
        Block block = Block.fromJson("test_block", blockDefinitionJson);

        assertNotNull("Block was null after initializing from JSON", block);
        assertEquals("Type not set correctly", "test_block", block.getType());
        assertStringNotEmpty("Block id cannot be empty.", block.getId());
        assertEquals("Wrong number of inputs", 2, block.getInputs().size());
        assertEquals("Wrong number of fields in first input",
                9, block.getInputs().get(0).getFields().size());
    }

    public void testEmptyBlockHasId() {
        Block block = new Block.Builder("test_block").build();
        assertStringNotEmpty("Block id cannot be empty.", block.getId());
    }

    public void testCopyBlockDoesNotCopyId() {
        Block original = new Block.Builder("test_block").build();
        Block copy = original.deepCopy();

        assertStringNotEmpty("Copies of blocks cannot be empty ids.", copy.getId());
        assertNotEqual("Copies of blocks must have different ids than their originals.",
                original.getId(), copy.getId());
    }

    public void testCopyBlockCopiesChildren() {
        Block original = mBlockFactory.obtainBlock("simple_input_output", "1");
        Block original2 = mBlockFactory.obtainBlock("simple_input_output", "2");
        Block originalShadow = mBlockFactory.obtainBlock("simple_input_output", "3");
        originalShadow.setShadow(true);
        original.getOnlyValueInput().getConnection().connect(original2.getOutputConnection());
        original.getOnlyValueInput().getConnection()
                .setShadowConnection(originalShadow.getOutputConnection());

        Block copy = original.deepCopy();
        assertNotSame(original, copy);
        assertNotSame(original.getOnlyValueInput().getConnection().getTargetBlock(),
                copy.getOnlyValueInput().getConnection().getTargetBlock());
        assertNotSame(original.getOnlyValueInput().getConnection().getShadowBlock(),
                copy.getOnlyValueInput().getConnection().getShadowBlock());
    }

    public void testMessageTokenizer() {
        String testMessage = "%%5 should have %1 %12 6 tokens %999 in the end";
        List<String> tokens = Block.tokenizeMessage(testMessage);
        List<String> expected = Arrays.asList(
                new String[]{"%%5 should have", "%1", "%12", "6 tokens", "%999", "in the end"});
        assertListsMatch(expected, tokens);

        testMessage = "This has no args %%5";
        tokens = Block.tokenizeMessage(testMessage);
        assertEquals("Should have 1 token: " + tokens.toString(), 1, tokens.size());
        assertEquals("Only token should be the original string: " + tokens.toString(),
                testMessage, tokens.get(0));

        testMessage = "%1";
        tokens = Block.tokenizeMessage(testMessage);
        assertEquals("Should have 1 token: " + tokens.toString(), 1, tokens.size());
        assertEquals("Only token should be the original string: " + tokens.toString(),
                testMessage, tokens.get(0));

        testMessage = "%Hello";
        tokens = Block.tokenizeMessage(testMessage);
        assertEquals("Should have 1 token: " + tokens.toString(), 1, tokens.size());
        assertEquals("Only token should be the original string: " + tokens.toString(),
                testMessage, tokens.get(0));


        testMessage = "%Hello%1World%";
        tokens = Block.tokenizeMessage(testMessage);
        expected = Arrays.asList(new String[]{"%Hello", "%1", "World%"});
        assertListsMatch(expected, tokens);
    }

    public void testLoadFromXml() throws IOException, XmlPullParserException {
        // TODO(#84): Move test_blocks.json to the test app's resources
        BlockFactory bf = new BlockFactory(getContext(), new int[]{R.raw.test_blocks});

        Block loaded = parseBlockFromXml(BlockTestStrings.SIMPLE_BLOCK, bf);
        assertEquals("frankenblock", loaded.getType());
        assertEquals(new WorkspacePoint(37, 13), loaded.getPosition());

        // All blocks need a type. Ids can be generated by the BlockFactory.
        parseBlockFromXmlFailure(BlockTestStrings.NO_BLOCK_TYPE, bf);
        parseBlockFromXml(BlockTestStrings.NO_BLOCK_ID, bf);

        // Only top level blocks need a position.
        parseBlockFromXml(BlockTestStrings.NO_BLOCK_POSITION, bf);

        // Values.
        parseBlockFromXml(BlockTestStrings.assembleFrankenblock("block", "1",
                BlockTestStrings.VALUE_GOOD), bf);
        // Value: null child block
        parseBlockFromXml(BlockTestStrings.assembleFrankenblock("block", "3",
                BlockTestStrings.VALUE_NO_CHILD), bf);
        // TODO(fenichel): Value: no input connection
        // Value: no output connection on child
        parseBlockFromXmlFailure(BlockTestStrings.assembleFrankenblock("block", "2",
                BlockTestStrings.VALUE_NO_OUTPUT), bf);
        // Value: no input with that name
        parseBlockFromXmlFailure(BlockTestStrings.assembleFrankenblock("block", "4",
                BlockTestStrings.VALUE_BAD_NAME), bf);
        // Value: multiple values for the same input
        parseBlockFromXmlFailure(BlockTestStrings.assembleFrankenblock("block", "5",
                BlockTestStrings.VALUE_REPEATED), bf);

        // Comment: with text
        parseBlockFromXml(BlockTestStrings.assembleFrankenblock("block", "6",
                BlockTestStrings.COMMENT_GOOD), bf);
        // Comment: empty string
        parseBlockFromXml(BlockTestStrings.assembleFrankenblock("block", "7",
                BlockTestStrings.COMMENT_NO_TEXT), bf);

        // Fields
        loaded = parseBlockFromXml(BlockTestStrings.assembleFrankenblock("block", "8",
                BlockTestStrings.FIELD_HAS_NAME), bf);
        assertEquals("item", ((FieldInput) loaded.getFieldByName("text_input")).getText());
        // A missing or unknown field name isn't an error, it's just ignored.
        parseBlockFromXml(BlockTestStrings.assembleFrankenblock("block", "9",
                BlockTestStrings.FIELD_MISSING_NAME), bf);
        parseBlockFromXml(BlockTestStrings.assembleFrankenblock("block", "10",
                BlockTestStrings.FIELD_UNKNOWN_NAME), bf);
        parseBlockFromXml(BlockTestStrings.assembleFrankenblock("block", "11",
                BlockTestStrings.FIELD_MISSING_TEXT), bf);

        // Statement: no previous connection on child block
        parseBlockFromXmlFailure(BlockTestStrings.assembleFrankenblock("block", "13",
                BlockTestStrings.STATEMENT_BAD_CHILD), bf);
        // Statement: no input with that name
        parseBlockFromXmlFailure(BlockTestStrings.assembleFrankenblock("block", "14",
                BlockTestStrings.STATEMENT_BAD_NAME), bf);

        // Statement
        parseBlockFromXml(BlockTestStrings.assembleFrankenblock("block", "15",
                BlockTestStrings.STATEMENT_GOOD), bf);
        // Statement: null child block
        parseBlockFromXml(BlockTestStrings.assembleFrankenblock("block", "12",
                BlockTestStrings.STATEMENT_NO_CHILD), bf);
    }

    public void testLoadFromXmlInline() throws IOException, XmlPullParserException {
        // TODO(#84): Move test_blocks.json to the test app's resources
        BlockFactory bf = new BlockFactory(getContext(), new int[]{R.raw.test_blocks});

        Block loaded = parseBlockFromXml(BlockTestStrings.SIMPLE_BLOCK_INLINE_BEGINNING, bf);
        assertTrue(loaded.getInputsInline());
        assertTrue(loaded.getInputsInlineModified());

        loaded = parseBlockFromXml(BlockTestStrings.SIMPLE_BLOCK_INLINE_END, bf);
        assertTrue(loaded.getInputsInline());
        assertTrue(loaded.getInputsInlineModified());

        loaded = parseBlockFromXml(BlockTestStrings.SIMPLE_BLOCK_INLINE_FALSE, bf);
        assertFalse(loaded.getInputsInline());
        assertTrue(loaded.getInputsInlineModified());

        loaded = parseBlockFromXml(BlockTestStrings.SIMPLE_BLOCK, bf);
        assertFalse(loaded.getInputsInline());
        assertFalse(loaded.getInputsInlineModified());
    }

    public void testLoadFromXml_shadows() throws IOException, XmlPullParserException {
        BlockFactory bf = new BlockFactory(getContext(), new int[]{R.raw.test_blocks});

        Block loaded = parseShadowFromXml(BlockTestStrings.SIMPLE_SHADOW, bf);
        assertEquals("math_number", loaded.getType());
        assertEquals(new WorkspacePoint(37, 13), loaded.getPosition());
        assertTrue(loaded.isShadow());


        loaded = parseBlockFromXml(BlockTestStrings.assembleFrankenblock("block", "1",
                BlockTestStrings.VALUE_SHADOW), bf);
        Connection conn = loaded.getInputByName("value_input").getConnection();
        assertEquals(conn.getTargetBlock(), conn.getShadowBlock());
        assertTrue(conn.getShadowBlock().isShadow());

        loaded = parseBlockFromXml(BlockTestStrings.assembleFrankenblock("block", "2",
                BlockTestStrings.VALUE_SHADOW_GOOD), bf);
        conn = loaded.getInputByName("value_input").getConnection();
        assertEquals("VALUE_REAL", conn.getTargetBlock().getId());
        assertFalse(conn.getTargetBlock().isShadow());
        assertEquals("VALUE_SHADOW", conn.getShadowBlock().getId());
        assertTrue(conn.getShadowBlock().isShadow());

        loaded = parseBlockFromXml(BlockTestStrings.assembleFrankenblock("block", "3",
                BlockTestStrings.STATEMENT_SHADOW), bf);
        conn = loaded.getInputByName("NAME").getConnection();
        assertEquals(conn.getTargetBlock(), conn.getShadowBlock());
        assertTrue(conn.getShadowBlock().isShadow());

        // Clear refs so block names can be reused
        bf.clearPriorBlockReferences();
        loaded = parseBlockFromXml(BlockTestStrings.assembleFrankenblock("block", "4",
                BlockTestStrings.STATEMENT_SHADOW_GOOD), bf);
        conn = loaded.getInputByName("NAME").getConnection();
        assertEquals("STATEMENT_REAL", conn.getTargetBlock().getId());
        assertFalse(conn.getTargetBlock().isShadow());
        assertEquals("STATEMENT_SHADOW", conn.getShadowBlock().getId());
        assertTrue(conn.getShadowBlock().isShadow());

        loaded = parseBlockFromXml(BlockTestStrings.assembleFrankenblock("block", "5",
                BlockTestStrings.VALUE_NESTED_SHADOW), bf);
        conn = loaded.getInputByName("value_input").getConnection();
        assertEquals(conn.getTargetBlock(), conn.getShadowBlock());
        Block shadow1 = conn.getShadowBlock();
        assertEquals("SHADOW1", shadow1.getId());
        conn = shadow1.getOnlyValueInput().getConnection();
        assertEquals(conn.getTargetBlock(), conn.getShadowBlock());
        assertEquals("SHADOW2", conn.getShadowBlock().getId());

        // Clear refs so block names can be reused
        bf.clearPriorBlockReferences();
        loaded = parseBlockFromXml(BlockTestStrings.assembleFrankenblock("block", "6",
                BlockTestStrings.VALUE_NESTED_SHADOW_BLOCK), bf);
        conn = loaded.getInputByName("value_input").getConnection();
        shadow1 = conn.getShadowBlock();
        assertEquals("SHADOW1", shadow1.getId());
        conn = shadow1.getOnlyValueInput().getConnection();
        assertEquals("BLOCK_INNER", conn.getTargetBlock().getId());
        assertEquals("SHADOW2", conn.getShadowBlock().getId());

        // Verify a block with a variable cannot be turned into a shadow block
        try {
            loaded = parseBlockFromXml(BlockTestStrings.assembleFrankenblock("block", "7",
                    BlockTestStrings.VALUE_SHADOW_VARIABLE), bf);
            fail("Shadow blocks may not contain variables.");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    public void testSerializeBlock() throws BlocklySerializerException, IOException {
        BlockFactory bf = new BlockFactory(getContext(), new int[]{R.raw.test_blocks});
        Block block = bf.obtainBlock("empty_block", BlockTestStrings.EMPTY_BLOCK_ID);
        block.setPosition(37, 13);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XmlSerializer serializer = getXmlSerializer(os);

        block.serialize(serializer, true);
        serializer.flush();
        assertEquals(BlockTestStrings.EMPTY_BLOCK_WITH_POSITION, os.toString());

        os = new ByteArrayOutputStream();
        serializer = getXmlSerializer(os);

        block.serialize(serializer, false);
        serializer.flush();
        assertEquals(BlockTestStrings.EMPTY_BLOCK_NO_POSITION, os.toString());

        block = bf.obtainBlock("frankenblock", "frankenblock1");
        os = new ByteArrayOutputStream();
        serializer = getXmlSerializer(os);

        block.serialize(serializer, false);
        serializer.flush();
        assertEquals(BlockTestStrings.blockStart("block", "frankenblock", "frankenblock1", null)
                + BlockTestStrings.FRANKENBLOCK_DEFAULT_VALUES
                + BlockTestStrings.BLOCK_END, os.toString());
    }

    public void testSerializeInputsInline() throws BlocklySerializerException, IOException {
        BlockFactory bf = new BlockFactory(getContext(), new int[]{R.raw.test_blocks});
        Block block = bf.obtainBlock("empty_block", BlockTestStrings.EMPTY_BLOCK_ID);
        block.setPosition(37, 13);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XmlSerializer serializer = getXmlSerializer(os);

        block.serialize(serializer, true);
        serializer.flush();
        assertEquals(BlockTestStrings.EMPTY_BLOCK_WITH_POSITION, os.toString());

        block.setInputsInline(false);
        os = new ByteArrayOutputStream();
        serializer = getXmlSerializer(os);
        block.serialize(serializer, true);
        serializer.flush();
        assertEquals(BlockTestStrings.EMPTY_BLOCK_INLINE_FALSE, os.toString());

        block.setInputsInline(true);
        os = new ByteArrayOutputStream();
        serializer = getXmlSerializer(os);
        block.serialize(serializer, true);
        serializer.flush();
        assertEquals(BlockTestStrings.EMPTY_BLOCK_INLINE_TRUE, os.toString());
    }

    public void testSerializeShadowBlock() throws BlocklySerializerException, IOException {
        BlockFactory bf = new BlockFactory(getContext(), new int[]{R.raw.test_blocks});
        Block block = bf.obtainBlock("empty_block", BlockTestStrings.EMPTY_BLOCK_ID);
        block.setPosition(37, 13);
        block.setShadow(true);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XmlSerializer serializer = getXmlSerializer(os);

        block.serialize(serializer, true);
        serializer.flush();
        assertEquals(BlockTestStrings.EMPTY_SHADOW_WITH_POSITION, os.toString());
    }

    public void testSerializeValue() throws BlocklySerializerException, IOException {
        BlockFactory bf = new BlockFactory(getContext(), new int[]{R.raw.test_blocks});
        Block block = bf.obtainBlock("frankenblock", "364");
        block.setPosition(37, 13);

        Input input = block.getInputByName("value_input");
        Block inputBlock = bf.obtainBlock("output_foo", "VALUE_GOOD");
        input.getConnection().connect(inputBlock.getOutputConnection());

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XmlSerializer serializer = getXmlSerializer(os);
        block.serialize(serializer, true);
        serializer.flush();

        String expected = BlockTestStrings.frankenBlockStart("block", "364")
                + BlockTestStrings.VALUE_GOOD
                + BlockTestStrings.FRANKENBLOCK_DEFAULT_VALUES
                + BlockTestStrings.BLOCK_END;
        assertEquals(expected, os.toString());
    }

    public void testSerializeShadowValue() throws BlocklySerializerException, IOException {
        BlockFactory bf = new BlockFactory(getContext(), new int[]{R.raw.test_blocks});
        Block block = bf.obtainBlock("frankenblock", "364");
        block.setPosition(37, 13);

        Input input = block.getInputByName("value_input");
        Block inputBlock = bf.obtainBlock("output_foo", "VALUE_REAL");
        input.getConnection().connect(inputBlock.getOutputConnection());
        inputBlock = bf.obtainBlock("output_foo", "VALUE_SHADOW");
        inputBlock.setShadow(true);
        input.getConnection().setShadowConnection(inputBlock.getOutputConnection());

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XmlSerializer serializer = getXmlSerializer(os);
        block.serialize(serializer, true);
        serializer.flush();

        String expected = BlockTestStrings.frankenBlockStart("block", "364")
                + BlockTestStrings.VALUE_SHADOW_GOOD
                + BlockTestStrings.FRANKENBLOCK_DEFAULT_VALUES
                + BlockTestStrings.BLOCK_END;
        assertEquals(expected, os.toString());

        block = bf.obtainBlock("frankenblock", "777");
        block.setPosition(37, 13);
        input = block.getInputByName("value_input");
        inputBlock = bf.obtainBlock("simple_input_output", "SHADOW1");
        inputBlock.setShadow(true);
        input.getConnection().connect(inputBlock.getOutputConnection());
        input = inputBlock.getOnlyValueInput();
        inputBlock = bf.obtainBlock("simple_input_output", "SHADOW2");
        inputBlock.setShadow(true);
        input.getConnection().connect(inputBlock.getOutputConnection());

        os.reset();
        block.serialize(serializer, true);
        serializer.flush();

        expected = BlockTestStrings.frankenBlockStart("block", "777")
                + BlockTestStrings.VALUE_NESTED_SHADOW
                + BlockTestStrings.FRANKENBLOCK_DEFAULT_VALUES
                + BlockTestStrings.BLOCK_END;
        assertEquals(expected, os.toString());
    }

    public void testSerializeStatement() throws BlocklySerializerException, IOException {
        BlockFactory bf = new BlockFactory(getContext(), new int[]{R.raw.test_blocks});
        Block block = bf.obtainBlock("frankenblock", "364");
        block.setPosition(37, 13);

        Input input = block.getInputByName("NAME");
        Block inputBlock = bf.obtainBlock("frankenblock", "3");
        input.getConnection().connect(inputBlock.getPreviousConnection());

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XmlSerializer serializer = getXmlSerializer(os);
        block.serialize(serializer, true);
        serializer.flush();

        String expected = BlockTestStrings.frankenBlockStart("block", "364")
                + BlockTestStrings.FRANKENBLOCK_DEFAULT_VALUES_START
                + "<statement name=\"NAME\">"
                + "<block type=\"frankenblock\" id=\"3\">"
                + BlockTestStrings.FRANKENBLOCK_DEFAULT_VALUES
                + BlockTestStrings.BLOCK_END
                + "</statement>"
                + BlockTestStrings.FRANKENBLOCK_DEFAULT_VALUES_END
                + BlockTestStrings.BLOCK_END;
        assertEquals(expected, os.toString());
    }

    public void testGetAllConnections() {
        Block block = mBlockFactory.obtainBlock("frankenblock", null);
        List<Connection> allConnections = block.getAllConnections();
        assertEquals(4, allConnections.size());

        block = mBlockFactory.obtainBlock("frankenblock", null);
        allConnections.clear();
        block.getAllConnections(allConnections);
        assertEquals(4, allConnections.size());

        allConnections.clear();

        Block ivb = mBlockFactory.obtainBlock("simple_input_output", null);
        block.getInputs().get(0).getConnection().connect(ivb.getOutputConnection());
        Block svb = mBlockFactory.obtainBlock("output_no_input", null);
        ivb.getInputs().get(0).getConnection().connect(svb.getOutputConnection());
        Block smb = mBlockFactory.obtainBlock("statement_no_input", null);
        block.getInputs().get(1).getConnection().connect(smb.getPreviousConnection());

        block.getAllConnectionsRecursive(allConnections);
        assertEquals(9, allConnections.size());
    }

    public void testGetOnlyValueInput() {
        BlockFactory bf = new BlockFactory(getContext(), new int[]{R.raw.test_blocks});
        // No inputs.
        assertNull(bf.obtainBlock("statement_no_input", null).getOnlyValueInput());

        // One value input.
        Block underTest = bf.obtainBlock("statement_value_input", null);
        assertSame(underTest.getInputByName("value"), underTest.getOnlyValueInput());

        // Statement input, no value inputs.
        assertNull(bf.obtainBlock("statement_statement_input", null).getOnlyValueInput());

        // Multiple value inputs.
        assertNull(bf.obtainBlock("statement_multiple_value_input", null)
                .getOnlyValueInput());

        // Statement input, dummy input and value input.
        underTest = bf.obtainBlock("controls_repeat_ext", null);
        assertSame(underTest.getInputByName("TIMES"), underTest.getOnlyValueInput());
    }

    private Block parseBlockFromXml(String testString, BlockFactory bf)
            throws IOException, XmlPullParserException {
        XmlPullParser parser = getXmlPullParser(testString, "block");
        Block loaded = Block.fromXml(parser, bf);
        assertNotNull(loaded);
        return loaded;
    }

    private Block parseShadowFromXml(String testString, BlockFactory bf)
            throws IOException, XmlPullParserException {
        XmlPullParser parser = getXmlPullParser(testString, "shadow");
        Block loaded = Block.fromXml(parser, bf);
        assertNotNull(loaded);
        return loaded;
    }

    private void parseBlockFromXmlFailure(String testString, BlockFactory bf)
            throws IOException, XmlPullParserException {

        XmlPullParser parser = getXmlPullParser(testString, "block");
        try {
            Block.fromXml(parser, bf);
            fail("Should have thrown a BlocklyParseException.");
        } catch (BlocklyParserException expected) {
            // expected
        }
    }

    /* Creates a pull parser with the given input and gobbles up to the first start tag
     * that equals returnFirstInstanceOf.
     */
    private XmlPullParser getXmlPullParser(String input, String returnFirstInstanceOf) {
        XmlPullParser parser = null;
        try {
            xmlPullParserFactory.setNamespaceAware(true);
            parser = xmlPullParserFactory.newPullParser();

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

    private XmlSerializer getXmlSerializer(ByteArrayOutputStream os) throws BlocklySerializerException {
        XmlSerializer serializer;
        try {
            xmlPullParserFactory.setNamespaceAware(true);
            serializer = xmlPullParserFactory.newSerializer();
            serializer.setOutput(os, null);
            return serializer;
        } catch (XmlPullParserException | IOException e) {
            throw new BlocklySerializerException(e);
        }
    }

    private void assertListsMatch(List<String> expected, List<String> actual) {
        assertEquals("Wrong number of items in the list.", expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals("Item " + i + " does not match.", expected.get(i), actual.get(i));
        }
    }

    public void testGetLastUnconnectedInputConnectionOneInputAtEnd() {
        // Two simple input blocks
        ArrayList<Block> blocks = new ArrayList<>();
        Block first = mBlockFactory.obtainBlock("simple_input_output", "first block");
        Block second = mBlockFactory.obtainBlock("simple_input_output", "second block");
        first.getOnlyValueInput().getConnection().connect(second.getOutputConnection());
        blocks.add(first);

        assertSame(second.getLastUnconnectedInputConnection(),
                second.getOnlyValueInput().getConnection());
        assertSame(first.getLastUnconnectedInputConnection(),
                second.getOnlyValueInput().getConnection());
    }

    public void testGetLastUnconnectedInputConnectionMultipleInputs() {
        ArrayList<Block> blocks = new ArrayList<>();
        Block first = mBlockFactory.obtainBlock("simple_input_output", "first block");
        Block second = mBlockFactory.obtainBlock("simple_input_output", "second block");
        Block third = mBlockFactory.obtainBlock("multiple_input_output", "third block");
        first.getOnlyValueInput().getConnection().connect(second.getOutputConnection());
        second.getOnlyValueInput().getConnection().connect(third.getOutputConnection());
        blocks.add(first);

        assertNull(third.getLastUnconnectedInputConnection());
        assertNull(second.getLastUnconnectedInputConnection());
        assertNull(first.getLastUnconnectedInputConnection());
    }

    public void testGetLastUnconnectedInputConnectionNoInput() {
        ArrayList<Block> blocks = new ArrayList<>();
        Block first = mBlockFactory.obtainBlock("simple_input_output", "first block");
        Block second = mBlockFactory.obtainBlock("simple_input_output", "second block");
        Block third = mBlockFactory.obtainBlock("output_no_input", "third block");
        first.getOnlyValueInput().getConnection().connect(second.getOutputConnection());
        second.getOnlyValueInput().getConnection().connect(third.getOutputConnection());
        blocks.add(first);

        assertNull(third.getLastUnconnectedInputConnection());
        assertNull(second.getLastUnconnectedInputConnection());
        assertNull(first.getLastUnconnectedInputConnection());
    }

    public void testGetLastUnconnectedInputConnectionShadowAtEnd() {
        // Two simple input blocks
        ArrayList<Block> blocks = new ArrayList<>();
        Block first = mBlockFactory.obtainBlock("simple_input_output", "first block");
        Block second = mBlockFactory.obtainBlock("simple_input_output", "second block");
        Block shadow = new Block.Builder(second).setUuid("shadow block").setShadow(true).build();
        first.getOnlyValueInput().getConnection().connect(second.getOutputConnection());
        Connection secondConn = second.getOnlyValueInput().getConnection();
        secondConn.setShadowConnection(shadow.getOutputConnection());
        secondConn.connect(shadow.getOutputConnection());
        blocks.add(first);

        assertSame(second.getLastUnconnectedInputConnection(),
                second.getOnlyValueInput().getConnection());
        assertSame(first.getLastUnconnectedInputConnection(),
                second.getOnlyValueInput().getConnection());
    }

    public void testLastBlockInSequence_blockLacksNext() {
        Block block = mBlockFactory.obtainBlock("statement_input_no_next", "block");

        // This value block should not returned; it is not connected to the next connection.
        Block value = mBlockFactory.obtainBlock("output_no_input", "value");
        block.getInputByName("value").getConnection().connect(value.getOutputConnection());

        assertSame(block, block.getLastBlockInSequence());
    }

    public void testLastBlockInSequence_noBlockConnected() {
        Block block = mBlockFactory.obtainBlock("statement_value_input", "block");

        // This value block should not returned; it is not connected to the next connection.
        Block value = mBlockFactory.obtainBlock("output_no_input", "value");
        block.getInputByName("value").getConnection().connect(value.getOutputConnection());

        assertSame(block, block.getLastBlockInSequence());
    }

    public void testLastBlockInSequence_lastBlockUnconnected() {
        Block first = mBlockFactory.obtainBlock("statement_no_input", "first block");
        Block second = mBlockFactory.obtainBlock("statement_no_input", "second block");
        Block third = mBlockFactory.obtainBlock("statement_value_input", "third block");

        // This value block should not returned; it is not connected to the next connection.
        Block value = mBlockFactory.obtainBlock("output_no_input", "value");

        first.getNextConnection().connect(second.getPreviousConnection());
        second.getNextConnection().connect(third.getPreviousConnection());
        third.getInputByName("value").getConnection().connect(value.getOutputConnection());

        assertSame(third, first.getLastBlockInSequence());
    }

    public void testLastBlockInSequence_lastBlockNoNext() {
        Block first = mBlockFactory.obtainBlock("statement_no_input", "first block");
        Block second = mBlockFactory.obtainBlock("statement_no_input", "second block");
        Block third = mBlockFactory.obtainBlock("statement_input_no_next", "third block");

        // This value block should not returned; it is not connected to the next connection.
        Block value = mBlockFactory.obtainBlock("output_no_input", "value");

        first.getNextConnection().connect(second.getPreviousConnection());
        second.getNextConnection().connect(third.getPreviousConnection());
        third.getInputByName("value").getConnection().connect(value.getOutputConnection());

        assertSame(third, first.getLastBlockInSequence());
    }

    public void testLastBlockInSequence_lastBlockShadow() {
        Block first = mBlockFactory.obtainBlock("statement_no_input", "first block");
        Block second = mBlockFactory.obtainBlock("statement_no_input", "second block");
        Block shadow = new Block.Builder(second).setUuid("shadow block").setShadow(true).build();

        first.getNextConnection().connect(second.getPreviousConnection());
        second.getNextConnection().setShadowConnection(shadow.getPreviousConnection());
        second.getNextConnection().connect(shadow.getPreviousConnection());

        assertSame(second, first.getLastBlockInSequence());
    }

    public void testBlockIdSerializedDeserialized() {
        Block block = mBlockFactory.obtainBlock("statement_no_input", "123");
    }

    public void testCollapsed() {
        Block block = new Block.Builder("statement_no_input").build();
        assertFalse("By default, blocks are not collapsed.", block.isCollapsed());

        String blockXml = toXml(block);
        assertFalse("Default state is not stored in XML", blockXml.contains("collapsed"));

        Block blockFromXml = fromXmlWithoutId(blockXml);
        assertFalse("By default, blocks loaded from XML are not collapsed.",
                blockFromXml.isCollapsed());

        block.setCollapsed(true);
        assertTrue("Collapsed state can change.", block.isCollapsed());

        blockXml = toXml(block);
        assertTrue("Collapsed state is stored in XML.", blockXml.contains("collapsed=\"true\""));

        blockFromXml = fromXmlWithoutId(blockXml);
        assertTrue("Collapsed state set from XML.", blockFromXml.isCollapsed());
    }

    public void testDeletable() {
        Block block = new Block.Builder("statement_no_input").build();
        assertTrue("By default, blocks are deletable.", block.isDeletable());

        String blockXml = toXml(block);
        assertFalse("Default state is not stored in XML", blockXml.contains("deletable"));

        Block blockFromXml = fromXmlWithoutId(blockXml);
        assertTrue("By default, blocks loaded from XML are deletable.", blockFromXml.isDeletable());

        block.setDeletable(false);
        assertFalse("Deletable state can change.", block.isDeletable());

        blockXml = toXml(block);
        assertTrue("Deletable state is stored in XML", blockXml.contains("deletable=\"false\""));

        blockFromXml = fromXmlWithoutId(blockXml);
        assertFalse("Deletable state set from XML.", blockFromXml.isDeletable());
    }

    public void testDisabled() {
        Block block = new Block.Builder("statement_no_input").build();
        assertFalse("By default, blocks are not disabled.", block.isDisabled());

        String blockXml = toXml(block);
        assertFalse("Default state is not stored in XML", blockXml.contains("disabled"));

        Block blockFromXml = fromXmlWithoutId(blockXml);
        assertFalse("By default, blocks loaded from XML are not disabled.",
                blockFromXml.isDisabled());

        block.setDisabled(true);
        assertTrue("Disabled state can change.", block.isDisabled());

        blockXml = toXml(block);
        assertTrue("Disabled state is stored in XML.", blockXml.contains("disabled=\"true\""));

        blockFromXml = fromXmlWithoutId(blockXml);
        assertTrue("Disabled state set from XML.", blockFromXml.isDisabled());
    }

    public void testEditable() {
        Block block = new Block.Builder("statement_no_input").build();
        assertTrue("By default, blocks are editable.", block.isEditable());

        String blockXml = toXml(block);
        assertFalse("Default state is not stored in XML", blockXml.contains("editable"));

        Block blockFromXml = fromXmlWithoutId(blockXml);
        assertTrue("By default, blocks loaded from XML are editable.", blockFromXml.isEditable());

        block.setEditable(false);
        assertFalse("Editable state can change.", block.isEditable());

        blockXml = toXml(block);
        assertTrue("Editable state is stored in XML", blockXml.contains("editable=\"false\""));

        blockFromXml = fromXmlWithoutId(blockXml);
        assertFalse("Editable state set from XML.", blockFromXml.isEditable());
    }

    public void testMovable() {
        Block block = new Block.Builder("statement_no_input").build();
        assertTrue("By default, blocks are editable.", block.isMovable());

        String blockXml = toXml(block);
        assertFalse("Default state is not stored in XML", blockXml.contains("movable"));

        Block blockFromXml = fromXmlWithoutId(blockXml);
        assertTrue("By default, blocks loaded from XML are movable.", blockFromXml.isMovable());

        block.setMovable(false);
        assertFalse("Movable state can change.", block.isMovable());

        blockXml = toXml(block);
        assertTrue("Movable state is stored in XML", blockXml.contains("movable=\"false\""));

        blockFromXml = fromXmlWithoutId(blockXml);
        assertFalse("Movable state set from XML.", blockFromXml.isMovable());
    }

    private String toXml(Block block) {
        StringOutputStream out = new StringOutputStream();
        try {
            BlocklyXmlHelper.writeOneBlockToXml(block, out);
        } catch (BlocklySerializerException e) {
            throw new IllegalArgumentException("Failed to serialize block.", e);
        }
        return out.toString();
    }

    private Block fromXmlWithoutId(String xml) {
        xml = xml.replaceAll("id=\\\"[^\\\"]*\\\"", "");  // Remove id attributes.
        return fromXml(xml);
    }

    private Block fromXml(String xml) {
        try {
            InputStream stream = new ByteArrayInputStream(xml.getBytes("UTF-8"));
            return BlocklyXmlHelper.loadOneBlockFromXml(stream, mBlockFactory);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private void assertStringNotEmpty(String mesg, String str) {
        if (str == null) {
            fail(mesg + " Found null string.");
        } else if(str.length() == 0) {
            fail(mesg + " Found empty string.");
        }
    }
}
