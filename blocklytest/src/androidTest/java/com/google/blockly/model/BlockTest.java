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

import android.support.test.InstrumentationRegistry;

import com.google.blockly.android.BlocklyTestCase;
import com.google.blockly.android.test.R;
import com.google.blockly.utils.BlocklyXmlHelper;
import com.google.blockly.utils.StringOutputStream;

import org.junit.Before;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.test.MoreAsserts.assertNotEqual;
import static com.google.blockly.utils.MoreAsserts.assertStringNotEmpty;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Tests for {@link Block}.
 */
public class BlockTest extends BlocklyTestCase {
    private XmlPullParserFactory xmlPullParserFactory;
    private BlockFactory mBlockFactory;

    @Before
    public void setUp() throws Exception {
        xmlPullParserFactory = XmlPullParserFactory.newInstance();
        mBlockFactory = new BlockFactory(getContext(), new int[]{R.raw.test_blocks});
    }

    @Test
    public void testEmptyBlockHasId() {
        Block block = new Block.Builder("test_block").build();
        assertStringNotEmpty("Block id cannot be empty.", block.getId());
    }

    @Test
    public void testCopyBlockDoesNotCopyId() {
        Block original = new Block.Builder("test_block").build();
        Block copy = original.deepCopy();

        assertStringNotEmpty("Copies of blocks cannot be empty ids.", copy.getId());
        assertNotEqual("Copies of blocks must have different ids than their originals.",
                original.getId(), copy.getId());
    }

    @Test
    public void testCopyBlockCopiesChildren() {
        Block original = mBlockFactory.obtainBlock("simple_input_output", "1");
        Block original2 = mBlockFactory.obtainBlock("simple_input_output", "2");
        Block originalShadow = mBlockFactory.obtainBlock("simple_input_output", "3");
        originalShadow.setShadow(true);
        original.getOnlyValueInput().getConnection().connect(original2.getOutputConnection());
        original.getOnlyValueInput().getConnection()
                .setShadowConnection(originalShadow.getOutputConnection());

        Block copy = original.deepCopy();
        assertThat(original).isNotSameAs(copy);
        assertThat(original.getOnlyValueInput().getConnection().getTargetBlock())
                .isNotSameAs(copy.getOnlyValueInput().getConnection().getTargetBlock());
        assertThat(original.getOnlyValueInput().getConnection().getShadowBlock())
                .isNotSameAs(copy.getOnlyValueInput().getConnection().getShadowBlock());
    }

    @Test
    public void testMessageTokenizer() {
        String testMessage = "%%5 should have %1 %12 6 tokens %999 in the end";
        List<String> tokens = Block.tokenizeMessage(testMessage);
        List<String> expected = Arrays.asList(
                new String[]{"%%5 should have", "%1", "%12", "6 tokens", "%999", "in the end"});
        assertListsMatch(expected, tokens);

        testMessage = "This has no args %%5";
        tokens = Block.tokenizeMessage(testMessage);
        assertWithMessage("Should have 1 token: " + tokens.toString())
                .that(tokens.size()).isEqualTo(1);
        assertWithMessage("Only token should be the original string: " + tokens.toString())
                .that(tokens.get(0)).isEqualTo(testMessage);

        testMessage = "%1";
        tokens = Block.tokenizeMessage(testMessage);
        assertWithMessage("Should have 1 token: " + tokens.toString())
                .that(tokens.size()).isEqualTo(1);
        assertWithMessage("Only token should be the original string: " + tokens.toString())
                .that(tokens.get(0)).isEqualTo(testMessage);

        testMessage = "%Hello";
        tokens = Block.tokenizeMessage(testMessage);
        assertWithMessage("Should have 1 token: " + tokens.toString())
                .that(tokens.size()).isEqualTo(1);
        assertWithMessage("Only token should be the original string: " + tokens.toString())
                .that(tokens.get(0)).isEqualTo(testMessage);


        testMessage = "%Hello%1World%";
        tokens = Block.tokenizeMessage(testMessage);
        expected = Arrays.asList(new String[]{"%Hello", "%1", "World%"});
        assertListsMatch(expected, tokens);
    }

    @Test
    public void testSerializeBlock() throws BlocklySerializerException, IOException {
        BlockFactory bf = new BlockFactory(getContext(), new int[]{R.raw.test_blocks});
        Block block = bf.obtainBlock("empty_block", BlockTestStrings.EMPTY_BLOCK_ID);
        block.setPosition(37, 13);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XmlSerializer serializer = getXmlSerializer(os);

        block.serialize(serializer, true);
        serializer.flush();
        assertThat(os.toString()).isEqualTo(BlockTestStrings.EMPTY_BLOCK_WITH_POSITION);

        os = new ByteArrayOutputStream();
        serializer = getXmlSerializer(os);

        block.serialize(serializer, false);
        serializer.flush();
        assertThat(os.toString()).isEqualTo(BlockTestStrings.EMPTY_BLOCK_NO_POSITION);

        block = bf.obtainBlock("frankenblock", "frankenblock1");
        os = new ByteArrayOutputStream();
        serializer = getXmlSerializer(os);

        block.serialize(serializer, false);
        serializer.flush();
        assertThat(os.toString()).isEqualTo(
            BlockTestStrings.blockStart("block", "frankenblock", "frankenblock1", null)
                + BlockTestStrings.FRANKENBLOCK_DEFAULT_VALUES
                + BlockTestStrings.BLOCK_END);
    }

    @Test
    public void testSerializeInputsInline() throws BlocklySerializerException, IOException {
        BlockFactory bf = new BlockFactory(getContext(), new int[]{R.raw.test_blocks});
        Block block = bf.obtainBlock("empty_block", BlockTestStrings.EMPTY_BLOCK_ID);
        block.setPosition(37, 13);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XmlSerializer serializer = getXmlSerializer(os);

        block.serialize(serializer, true);
        serializer.flush();
        assertThat(os.toString()).isEqualTo(BlockTestStrings.EMPTY_BLOCK_WITH_POSITION);

        block.setInputsInline(false);
        os = new ByteArrayOutputStream();
        serializer = getXmlSerializer(os);
        block.serialize(serializer, true);
        serializer.flush();
        assertThat(os.toString()).isEqualTo(BlockTestStrings.EMPTY_BLOCK_INLINE_FALSE);

        block.setInputsInline(true);
        os = new ByteArrayOutputStream();
        serializer = getXmlSerializer(os);
        block.serialize(serializer, true);
        serializer.flush();
        assertThat(os.toString()).isEqualTo(BlockTestStrings.EMPTY_BLOCK_INLINE_TRUE);
    }

    @Test
    public void testSerializeShadowBlock() throws BlocklySerializerException, IOException {
        BlockFactory bf = new BlockFactory(getContext(), new int[]{R.raw.test_blocks});
        Block block = bf.obtainBlock("empty_block", BlockTestStrings.EMPTY_BLOCK_ID);
        block.setPosition(37, 13);
        block.setShadow(true);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XmlSerializer serializer = getXmlSerializer(os);

        block.serialize(serializer, true);
        serializer.flush();
        assertThat(os.toString()).isEqualTo(BlockTestStrings.EMPTY_SHADOW_WITH_POSITION);
    }

    @Test
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
        assertThat(os.toString()).isEqualTo(expected);
    }

    @Test
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
        assertThat(os.toString()).isEqualTo(expected);

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
        assertThat(os.toString()).isEqualTo(expected);
    }

    @Test
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
        assertThat(os.toString()).isEqualTo(expected);
    }

    @Test
    public void testGetAllConnections() {
        Block block = mBlockFactory.obtainBlock("frankenblock", null);
        List<Connection> allConnections = block.getAllConnections();
        assertThat(allConnections.size()).isEqualTo(4);

        block = mBlockFactory.obtainBlock("frankenblock", null);
        allConnections.clear();
        block.getAllConnections(allConnections);
        assertThat(allConnections.size()).isEqualTo(4);

        allConnections.clear();

        Block ivb = mBlockFactory.obtainBlock("simple_input_output", null);
        block.getInputs().get(0).getConnection().connect(ivb.getOutputConnection());
        Block svb = mBlockFactory.obtainBlock("output_no_input", null);
        ivb.getInputs().get(0).getConnection().connect(svb.getOutputConnection());
        Block smb = mBlockFactory.obtainBlock("statement_no_input", null);
        block.getInputs().get(1).getConnection().connect(smb.getPreviousConnection());

        block.getAllConnectionsRecursive(allConnections);
        assertThat(allConnections.size()).isEqualTo(9);
    }

    @Test
    public void testGetOnlyValueInput() {
        BlockFactory bf = new BlockFactory(getContext(), new int[]{R.raw.test_blocks});
        // No inputs.
        assertThat(bf.obtainBlock("statement_no_input", null).getOnlyValueInput()).isNull();

        // One value input.
        Block underTest = bf.obtainBlock("statement_value_input", null);
        assertThat(underTest.getInputByName("value")).isSameAs(underTest.getOnlyValueInput());

        // Statement input, no value inputs.
        assertThat(bf.obtainBlock("statement_statement_input", null).getOnlyValueInput()).isNull();

        // Multiple value inputs.
        assertThat(bf.obtainBlock("statement_multiple_value_input", null)
                .getOnlyValueInput());

        // Statement input, dummy input and value input.
        underTest = bf.obtainBlock("controls_repeat_ext", null);
        assertThat(underTest.getInputByName("TIMES")).isSameAs(underTest.getOnlyValueInput());
    }

    private XmlSerializer getXmlSerializer(ByteArrayOutputStream os)
            throws BlocklySerializerException
    {
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
        assertWithMessage("Wrong number of items in the list.")
                .that(actual.size()).isEqualTo(expected.size());
        for (int i = 0; i < expected.size(); i++) {
            assertWithMessage("Item " + i + " does not match.")
                    .that(actual.get(i)).isEqualTo(expected.get(i));
        }
    }

    @Test
    public void testGetLastUnconnectedInputConnectionOneInputAtEnd() {
        // Two simple input blocks
        ArrayList<Block> blocks = new ArrayList<>();
        Block first = mBlockFactory.obtainBlock("simple_input_output", "first block");
        Block second = mBlockFactory.obtainBlock("simple_input_output", "second block");
        first.getOnlyValueInput().getConnection().connect(second.getOutputConnection());
        blocks.add(first);

        assertThat(second.getLastUnconnectedInputConnection())
                .isSameAs(second.getOnlyValueInput().getConnection());
        assertThat(first.getLastUnconnectedInputConnection())
                .isSameAs(second.getOnlyValueInput().getConnection());
    }

    @Test
    public void testGetLastUnconnectedInputConnectionMultipleInputs() {
        ArrayList<Block> blocks = new ArrayList<>();
        Block first = mBlockFactory.obtainBlock("simple_input_output", "first block");
        Block second = mBlockFactory.obtainBlock("simple_input_output", "second block");
        Block third = mBlockFactory.obtainBlock("multiple_input_output", "third block");
        first.getOnlyValueInput().getConnection().connect(second.getOutputConnection());
        second.getOnlyValueInput().getConnection().connect(third.getOutputConnection());
        blocks.add(first);

        assertThat(third.getLastUnconnectedInputConnection()).isNull();
        assertThat(second.getLastUnconnectedInputConnection()).isNull();
        assertThat(first.getLastUnconnectedInputConnection()).isNull();
    }

    @Test
    public void testGetLastUnconnectedInputConnectionNoInput() {
        ArrayList<Block> blocks = new ArrayList<>();
        Block first = mBlockFactory.obtainBlock("simple_input_output", "first block");
        Block second = mBlockFactory.obtainBlock("simple_input_output", "second block");
        Block third = mBlockFactory.obtainBlock("output_no_input", "third block");
        first.getOnlyValueInput().getConnection().connect(second.getOutputConnection());
        second.getOnlyValueInput().getConnection().connect(third.getOutputConnection());
        blocks.add(first);

        assertThat(third.getLastUnconnectedInputConnection()).isNull();
        assertThat(second.getLastUnconnectedInputConnection()).isNull();
        assertThat(first.getLastUnconnectedInputConnection()).isNull();
    }

    @Test
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

        assertThat(second.getLastUnconnectedInputConnection())
                .isSameAs(second.getOnlyValueInput().getConnection());
        assertThat(first.getLastUnconnectedInputConnection())
                .isSameAs(second.getOnlyValueInput().getConnection());
    }

    @Test
    public void testLastBlockInSequence_blockLacksNext() {
        Block block = mBlockFactory.obtainBlock("statement_input_no_next", "block");

        // This value block should not returned; it is not connected to the next connection.
        Block value = mBlockFactory.obtainBlock("output_no_input", "value");
        block.getInputByName("value").getConnection().connect(value.getOutputConnection());

        assertThat(block).isSameAs(block.getLastBlockInSequence());
    }

    @Test
    public void testLastBlockInSequence_noBlockConnected() {
        Block block = mBlockFactory.obtainBlock("statement_value_input", "block");

        // This value block should not returned; it is not connected to the next connection.
        Block value = mBlockFactory.obtainBlock("output_no_input", "value");
        block.getInputByName("value").getConnection().connect(value.getOutputConnection());

        assertThat(block).isSameAs(block.getLastBlockInSequence());
    }

    @Test
    public void testLastBlockInSequence_lastBlockUnconnected() {
        Block first = mBlockFactory.obtainBlock("statement_no_input", "first block");
        Block second = mBlockFactory.obtainBlock("statement_no_input", "second block");
        Block third = mBlockFactory.obtainBlock("statement_value_input", "third block");

        // This value block should not returned; it is not connected to the next connection.
        Block value = mBlockFactory.obtainBlock("output_no_input", "value");

        first.getNextConnection().connect(second.getPreviousConnection());
        second.getNextConnection().connect(third.getPreviousConnection());
        third.getInputByName("value").getConnection().connect(value.getOutputConnection());

        assertThat(third).isSameAs(first.getLastBlockInSequence());
    }

    @Test
    public void testLastBlockInSequence_lastBlockNoNext() {
        Block first = mBlockFactory.obtainBlock("statement_no_input", "first block");
        Block second = mBlockFactory.obtainBlock("statement_no_input", "second block");
        Block third = mBlockFactory.obtainBlock("statement_input_no_next", "third block");

        // This value block should not returned; it is not connected to the next connection.
        Block value = mBlockFactory.obtainBlock("output_no_input", "value");

        first.getNextConnection().connect(second.getPreviousConnection());
        second.getNextConnection().connect(third.getPreviousConnection());
        third.getInputByName("value").getConnection().connect(value.getOutputConnection());

        assertThat(third).isSameAs(first.getLastBlockInSequence());
    }

    @Test
    public void testLastBlockInSequence_lastBlockShadow() {
        Block first = mBlockFactory.obtainBlock("statement_no_input", "first block");
        Block second = mBlockFactory.obtainBlock("statement_no_input", "second block");
        Block shadow = new Block.Builder(second).setUuid("shadow block").setShadow(true).build();

        first.getNextConnection().connect(second.getPreviousConnection());
        second.getNextConnection().setShadowConnection(shadow.getPreviousConnection());
        second.getNextConnection().connect(shadow.getPreviousConnection());

        assertThat(second).isSameAs(first.getLastBlockInSequence());
    }

    @Test
    public void testBlockIdSerializedDeserialized() {
        Block block = mBlockFactory.obtainBlock("statement_no_input", "123");
    }

    @Test
    public void testCollapsed() {
        Block block = new Block.Builder("statement_no_input").build();
        assertWithMessage("By default, blocks are not collapsed.")
                .that(block.isCollapsed()).isFalse();

        String blockXml = toXml(block);
        assertWithMessage("Default state is not stored in XML")
                .that(blockXml.contains("collapsed")).isFalse();

        Block blockFromXml = fromXmlWithoutId(blockXml);
        assertWithMessage("By default, blocks loaded from XML are not collapsed.")
                .that(blockFromXml.isCollapsed()).isFalse();

        block.setCollapsed(true);
        assertWithMessage("Collapsed state can change.").that(block.isCollapsed()).isTrue();

        blockXml = toXml(block);
        assertWithMessage("Collapsed state is stored in XML.")
                .that(blockXml.contains("collapsed=\"true\"")).isTrue();

        blockFromXml = fromXmlWithoutId(blockXml);
        assertWithMessage("Collapsed state set from XML.")
                .that(blockFromXml.isCollapsed()).isTrue();
    }

    @Test
    public void testDeletable() {
        Block block = new Block.Builder("statement_no_input").build();
        assertWithMessage("By default, blocks are deletable.")
                .that(block.isDeletable()).isTrue();

        String blockXml = toXml(block);
        assertWithMessage("Default state is not stored in XML")
                .that(blockXml.contains("deletable")).isFalse();

        Block blockFromXml = fromXmlWithoutId(blockXml);
        assertWithMessage("By default, blocks loaded from XML are deletable.")
                .that(blockFromXml.isDeletable()).isTrue();

        block.setDeletable(false);
        assertWithMessage("Deletable state can change.")
                .that(block.isDeletable()).isFalse();

        blockXml = toXml(block);
        assertWithMessage("Deletable state is stored in XML")
                .that(blockXml.contains("deletable=\"false\"")).isTrue();

        blockFromXml = fromXmlWithoutId(blockXml);
        assertWithMessage("Deletable state set from XML.")
                .that(blockFromXml.isDeletable()).isFalse();
    }

    @Test
    public void testDisabled() {
        Block block = new Block.Builder("statement_no_input").build();
        assertWithMessage("By default, blocks are not disabled.")
                .that(block.isDisabled()).isFalse();

        String blockXml = toXml(block);
        assertWithMessage("Default state is not stored in XML")
                .that(blockXml.contains("disabled")).isFalse();

        Block blockFromXml = fromXmlWithoutId(blockXml);
        assertWithMessage("By default, blocks loaded from XML are not disabled.")
                .that(blockFromXml.isDisabled()).isFalse();

        block.setDisabled(true);
        assertWithMessage("Disabled state can change.")
                .that(block.isDisabled()).isTrue();

        blockXml = toXml(block);
        assertWithMessage("Disabled state is stored in XML.")
                .that(blockXml.contains("disabled=\"true\"")).isTrue();

        blockFromXml = fromXmlWithoutId(blockXml);
        assertWithMessage("Disabled state set from XML.")
                .that(blockFromXml.isDisabled()).isTrue();
    }

    @Test
    public void testEditable() {
        Block block = new Block.Builder("statement_no_input").build();
        assertWithMessage("By default, blocks are editable.")
                .that(block.isEditable()).isTrue();

        String blockXml = toXml(block);
        assertWithMessage("Default state is not stored in XML")
                .that(blockXml.contains("editable")).isFalse();

        Block blockFromXml = fromXmlWithoutId(blockXml);
        assertWithMessage("By default, blocks loaded from XML are editable.")
                .that(blockFromXml.isEditable()).isTrue();

        block.setEditable(false);
        assertWithMessage("Editable state can change.")
                .that(block.isEditable()).isFalse();

        blockXml = toXml(block);
        assertWithMessage("Editable state is stored in XML")
                .that(blockXml.contains("editable=\"false\"")).isTrue();

        blockFromXml = fromXmlWithoutId(blockXml);
        assertWithMessage("Editable state set from XML.")
                .that(blockFromXml.isEditable()).isFalse();
    }

    @Test
    public void testMovable() {
        Block block = new Block.Builder("statement_no_input").build();
        assertWithMessage("By default, blocks are editable.")
                .that(block.isMovable()).isTrue();

        String blockXml = toXml(block);
        assertWithMessage("Default state is not stored in XML")
                .that(blockXml.contains("movable")).isFalse();

        Block blockFromXml = fromXmlWithoutId(blockXml);
        assertWithMessage("By default, blocks loaded from XML are movable.")
                .that(blockFromXml.isMovable()).isTrue();

        block.setMovable(false);
        assertWithMessage("Movable state can change.")
                .that(block.isMovable()).isFalse();

        blockXml = toXml(block);
        assertWithMessage("Movable state is stored in XML")
                .that(blockXml.contains("movable=\"false\"")).isTrue();

        blockFromXml = fromXmlWithoutId(blockXml);
        assertWithMessage("Movable state set from XML.")
                .that(blockFromXml.isMovable()).isFalse();
    }

    /**
     * XML characters < , > and & need escaping when serialized.  Quote, apostrophe, and greater
     * than do not need escaping in a XML text node.
     */
    @Test
    public void testEscapeFieldData() {
        Block block = mBlockFactory.obtainBlock("text", null);
        block.getFieldByName("TEXT").setFromString(
                "less than < greater than > ampersand & quote \" apostrophe ' end");

        String xml = toXml(block);
        Matcher matcher = Pattern.compile("less.*end").matcher(xml);
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group()).isEqualTo(
                "less than &lt; greater than &gt; ampersand &amp; quote \" apostrophe ' end");
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
}
