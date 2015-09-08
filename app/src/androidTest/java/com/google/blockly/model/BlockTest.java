/*
 *  Copyright  2015 Google Inc. All Rights Reserved.
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

import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Tests for {@link Block}.
 */
public class BlockTest extends AndroidTestCase {
    XmlPullParserFactory factory = null;

    public void testJson() {
        JSONObject blockJson;
        try {
            blockJson = new JSONObject(BlockTestStrings.TEST_JSON_STRING);
        } catch (JSONException e) {
            throw new RuntimeException("Failure parsing test JSON.", e);
        }
        Block block = Block.fromJson(blockJson.optString("id"), blockJson);

        assertNotNull("Block was null after initializing from JSON", block);
        assertEquals("name not set correctly", "test_block", block.getName());
        assertEquals("Wrong number of inputs", 2, block.getInputs().size());
        assertEquals("Wrong number of fields in first input",
                9, block.getInputs().get(0).getFields().size());
    }

    public void testMessageTokenizer() {
        String testMessage = "%%5 should have %1 %12 6 tokens %999 in the end";
        List<String> tokens = Block.tokenizeMessage(testMessage);
        List<String> expected = Arrays.asList(
                new String[] {"%%5 should have", "%1", "%12", "6 tokens", "%999", "in the end"});
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
        expected = Arrays.asList(new String[] {"%Hello", "%1", "World%"});
        assertListsMatch(expected, tokens);
    }

    public void testBlockFactory() {
        // TODO: Move rest_blocks.json to the testapp's resources once
        // https://code.google.com/p/android/issues/detail?id=64887 is fixed.
        BlockFactory bf = new BlockFactory(getContext(), new int[] {R.raw.test_blocks});
        List<Block> blocks = bf.getAllBlocks();
        assertEquals("BlockFactory failed to load all blocks.", 4, blocks.size());
        Block emptyBlock = bf.obtainBlock("empty_block", null);
        assertNotNull("Failed to create the empty block.", emptyBlock);
        assertEquals("Empty block has the wrong name", "empty_block", emptyBlock.getName());

        Block frankenblock = bf.obtainBlock("frankenblock", null);
        assertNotNull("Failed to create the frankenblock.", frankenblock);
        assertNotNull("Missing previous connection", frankenblock.getPreviousConnection());
        assertNotNull("Missing next connection", frankenblock.getNextConnection());


        Block frankenblock2 = bf.obtainBlock("frankenblock", null);
        assertNotSame(frankenblock, frankenblock2);

        List<Input> inputs = frankenblock.getInputs();
        assertEquals("Frankenblock has the wrong number of inputs", 3, inputs.size());
        assertTrue("First input should be a value input.",
                inputs.get(0) instanceof Input.InputValue);
        assertTrue("Second input should be a statement input.",
                inputs.get(1) instanceof Input.InputStatement);
        assertTrue("Third input should be a dummy input.",
                inputs.get(2) instanceof Input.InputDummy);

        // Check getting inputs and fields by name.
        assertEquals(inputs.get(1), frankenblock.getInputByName("NAME"));
        Field foundField = frankenblock.getFieldByName("checkbox");
        assertTrue(foundField.isFieldType(Field.TYPE_CHECKBOX));
        assertTrue(((Field.FieldCheckbox)foundField).isChecked());
    }

    public void testLoadFromXml() throws IOException, XmlPullParserException {
        // TODO: Move rest_blocks.json to the testapp's resources once
        // https://code.google.com/p/android/issues/detail?id=64887 is fixed.
        BlockFactory bf = new BlockFactory(getContext(), new int[] {R.raw.test_blocks});

        Block loaded = parseBlockFromXml(BlockTestStrings.SIMPLE_BLOCK, bf);
        assertEquals("frankenblock", loaded.getName());
        assertEquals(new Point(37, 13), loaded.getPosition());

        // All blocks need a type. Ids can be generated by the BlockFactory.
        parseBlockFromXmlFailure(BlockTestStrings.NO_BLOCK_TYPE, bf);
        parseBlockFromXml(BlockTestStrings.NO_BLOCK_ID, bf);

        // Only top level blocks need a position.
        parseBlockFromXml(BlockTestStrings.NO_BLOCK_POSITION, bf);

        // Values.
        parseBlockFromXml(BlockTestStrings.assembleBlock(
                BlockTestStrings.VALUE_GOOD), bf);
        // TODO(fenichel): Value: no input connection
        // Value: no output connection on child
        parseBlockFromXmlFailure(BlockTestStrings.assembleBlock(
                BlockTestStrings.VALUE_NO_OUTPUT), bf);
        // value: null child block
        parseBlockFromXmlFailure(BlockTestStrings.assembleBlock(
                BlockTestStrings.VALUE_NO_CHILD), bf);
        // Value: no input with that name
        parseBlockFromXmlFailure(BlockTestStrings.assembleBlock(
                BlockTestStrings.VALUE_BAD_NAME), bf);
        // Value: multiple values in one block
        parseBlockFromXml(BlockTestStrings.assembleBlock(
                BlockTestStrings.VALUE_REPEATED), bf);

        // Comment: with text
        parseBlockFromXml(BlockTestStrings.assembleBlock(
                BlockTestStrings.COMMENT_GOOD), bf);
        // Comment: empty string
        parseBlockFromXml(BlockTestStrings.assembleBlock(
                BlockTestStrings.COMMENT_NO_TEXT), bf);

        // Fields
        loaded = parseBlockFromXml(BlockTestStrings.assembleBlock(
                BlockTestStrings.FIELD_HAS_NAME), bf);
        assertEquals("item", ((Field.FieldInput) loaded.getFieldByName("text_input")).getText());
        // A missing or unknown field name isn't an error, it's just ignored.
        parseBlockFromXml(BlockTestStrings.assembleBlock(
                BlockTestStrings.FIELD_MISSING_NAME), bf);
        parseBlockFromXml(BlockTestStrings.assembleBlock(
                BlockTestStrings.FIELD_UNKNOWN_NAME), bf);
        parseBlockFromXml(BlockTestStrings.assembleBlock(
                BlockTestStrings.FIELD_MISSING_TEXT), bf);

        // Statement: null child block
        parseBlockFromXmlFailure(BlockTestStrings.assembleBlock(
                BlockTestStrings.STATEMENT_NO_CHILD), bf);
        // Statement: no previous connection on child block
        parseBlockFromXmlFailure(BlockTestStrings.assembleBlock(
                BlockTestStrings.STATEMENT_BAD_CHILD), bf);
        // Statement: no input with that name
        parseBlockFromXmlFailure(BlockTestStrings.assembleBlock(
                BlockTestStrings.STATEMENT_BAD_NAME), bf);

        //TODO(fenichel): Enable this test when the BlockFactory properly does deep
        // copies.
        // Statement
        //parseBlockFromXml(BlockTestStrings.assembleBlock(
        //        BlockTestStrings.STATEMENT_GOOD), bf);
    }

    public void testSerializeBlock() throws BlocklySerializerException, IOException {
        BlockFactory bf = new BlockFactory(getContext(), new int[]{R.raw.test_blocks});
        Block block = bf.obtainBlock("empty_block", "364");
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

        block = bf.obtainBlock("frankenblock", "364");
        os = new ByteArrayOutputStream();
        serializer = getXmlSerializer(os);

        block.serialize(serializer, false);
        serializer.flush();
        assertEquals(BlockTestStrings.BLOCK_START_NO_POSITION
                + BlockTestStrings.FRANKENBLOCK_DEFAULT_VALUES
                + BlockTestStrings.BLOCK_END, os.toString());
    }

    public void testSerializeValue() throws BlocklySerializerException, IOException {
        BlockFactory bf = new BlockFactory(getContext(), new int[]{R.raw.test_blocks});
        Block block = bf.obtainBlock("frankenblock", "364");
        block.setPosition(37, 13);

        Input input = block.getInputByName("value_input");
        Block inputBlock = bf.obtainBlock("output_foo", "126");
        input.getConnection().connect(inputBlock.getOutputConnection());

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XmlSerializer serializer = getXmlSerializer(os);
        block.serialize(serializer, true);
        serializer.flush();

        String expected = BlockTestStrings.BLOCK_START
                + BlockTestStrings.VALUE_GOOD
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

        String expected = BlockTestStrings.BLOCK_START
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

    private Block parseBlockFromXml(String testString, BlockFactory bf)
            throws IOException, XmlPullParserException {
        XmlPullParser parser = getXmlPullParser(testString, "block");
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
            if (factory == null) {
                factory = XmlPullParserFactory.newInstance();
            }
            factory.setNamespaceAware(true);
            parser = factory.newPullParser();

            parser.setInput(new ByteArrayInputStream(input.getBytes()), null);

            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG
                        && parser.getName().equalsIgnoreCase(returnFirstInstanceOf)) {
                            return parser;
                }
                eventType = parser.next();
            }
        } catch (XmlPullParserException e) {
            throw new BlocklyParserException(e);
        } catch (IOException e) {
            throw new BlocklyParserException(e);
        }
        return null;
    }

    private XmlSerializer getXmlSerializer(ByteArrayOutputStream os) throws BlocklySerializerException {
        XmlSerializer serializer;
        try {
            if (factory == null) {
                factory = XmlPullParserFactory.newInstance();
            }
            factory.setNamespaceAware(true);
            serializer = factory.newSerializer();
            serializer.setOutput(os, null);
            return serializer;
        } catch (XmlPullParserException e) {
            throw new BlocklySerializerException(e);
        } catch (IOException e) {
            throw new BlocklySerializerException(e);
        }
    }

    private void assertListsMatch(List<String> expected, List<String> actual) {
        assertEquals("Wrong number of items in the list.", expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals("Item " + i + " does not match.", expected.get(i), actual.get(i));
        }
    }
}
