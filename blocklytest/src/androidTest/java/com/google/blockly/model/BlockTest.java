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

import com.google.blockly.android.BlocklyTestCase;
import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.utils.BlockLoadingException;
import com.google.blockly.utils.BlocklyXmlHelper;
import com.google.blockly.utils.StringOutputStream;
import com.google.blockly.utils.TestBlockObserver;
import com.google.blockly.utils.TestEventsCallback;

import org.junit.Before;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.blockly.utils.MoreAsserts.assertStringNotEmpty;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Tests for {@link Block}.
 */
public class BlockTest extends BlocklyTestCase {
    private static final String FAKE_WORKSPACE_ID = "FAKE_WORKSPACE_ID";

    private XmlPullParserFactory xmlPullParserFactory;
    private BlockFactory mBlockFactory;
    private BlocklyController mController;

    private TestBlockObserver mBlockObserver = new TestBlockObserver();
    private TestEventsCallback mEventsCallback = new TestEventsCallback();

    @Before
    public void setUp() throws Exception {
        configureForUIThread();

        xmlPullParserFactory = XmlPullParserFactory.newInstance();
        mController = new BlocklyController.Builder(getContext())
                .addBlockDefinitionsFromAsset("default/test_blocks.json")
                .build();
        mBlockFactory = mController.getBlockFactory();
    }

    @Test
    public void testEmptyBlockHasId() throws BlockLoadingException {
        Block block = mBlockFactory.obtainBlockFrom(new BlockTemplate().ofType("text"));
        assertWithMessage("Block id cannot be empty.")
                .that(block.getId()).isNotEmpty();
    }

    @Test
    public void testCopyBlockDoesNotCopyId() throws BlockLoadingException {
        Block original = mBlockFactory.obtainBlockFrom(new BlockTemplate().ofType("text"));
        Block copy = original.deepCopy();

        assertStringNotEmpty("Copies of blocks cannot be empty ids.", copy.getId());
        assertWithMessage("Copies of blocks must have different ids than their originals.")
                .that(copy.getId()).isNotEqualTo(original.getId());
    }

    @Test
    public void testCopyBlockCopiesChildren() throws BlockLoadingException {
        Block original = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("simple_input_output").withId("1"));
        Block original2 = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("simple_input_output").withId("2"));
        Block originalShadow = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().shadow().ofType("simple_input_output").withId("3"));
        
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
    public void testSerializeBlock() throws IOException, BlockLoadingException {
        Block block = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("empty_block").withId(BlockTestStrings.EMPTY_BLOCK_ID));
        block.setPosition(37, 13);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XmlSerializer serializer = getXmlSerializer(os);

        block.serialize(serializer, /* root block */ true, IOOptions.WRITE_ALL_DATA);
        serializer.flush();
        assertThat(os.toString()).isEqualTo(BlockTestStrings.EMPTY_BLOCK_WITH_POSITION);

        os = new ByteArrayOutputStream();
        serializer = getXmlSerializer(os);

        block.serialize(serializer, /* root block */ false, IOOptions.WRITE_ALL_DATA);
        serializer.flush();
        assertThat(os.toString()).isEqualTo(BlockTestStrings.EMPTY_BLOCK_NO_POSITION);

        block = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("frankenblock").withId("frankenblock1"));
        os = new ByteArrayOutputStream();
        serializer = getXmlSerializer(os);

        block.serialize(serializer, /* root block */ false, IOOptions.WRITE_ALL_DATA);
        serializer.flush();
        assertThat(os.toString()).isEqualTo(
            BlockTestStrings.blockStart("block", "frankenblock", "frankenblock1", null)
                + BlockTestStrings.FRANKENBLOCK_DEFAULT_VALUES
                + BlockTestStrings.BLOCK_END);
    }

    @Test
    public void testSerializeInputsInline() throws IOException, BlockLoadingException {
        final Block block = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("empty_block").withId(BlockTestStrings.EMPTY_BLOCK_ID));

        runAndSync(new Runnable() {
            @Override
            public void run() {
                try {
                    block.setPosition(37, 13);
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    XmlSerializer serializer = getXmlSerializer(os);

                    block.serialize(serializer, /* root block */ true, IOOptions.WRITE_ALL_DATA);
                    serializer.flush();
                    assertThat(os.toString()).isEqualTo(BlockTestStrings.EMPTY_BLOCK_WITH_POSITION);

                    block.setInputsInline(false);
                    os = new ByteArrayOutputStream();
                    serializer = getXmlSerializer(os);
                    block.serialize(serializer, /* root block */ true, IOOptions.WRITE_ALL_DATA);
                    serializer.flush();
                    assertThat(os.toString()).isEqualTo(BlockTestStrings.EMPTY_BLOCK_INLINE_FALSE);

                    block.setInputsInline(true);
                    os = new ByteArrayOutputStream();
                    serializer = getXmlSerializer(os);
                    block.serialize(serializer, /* root block */ true, IOOptions.WRITE_ALL_DATA);
                    serializer.flush();
                    assertThat(os.toString()).isEqualTo(BlockTestStrings.EMPTY_BLOCK_INLINE_TRUE);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
        });
    }

    @Test
    public void testSerializeShadowBlock() throws IOException, BlockLoadingException {
        Block block = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().shadow().ofType("empty_block")
                .withId(BlockTestStrings.EMPTY_BLOCK_ID)
                .atPosition(37, 13));
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XmlSerializer serializer = getXmlSerializer(os);

        block.serialize(serializer, /* root block */ true, IOOptions.WRITE_ALL_DATA);
        serializer.flush();
        assertThat(os.toString()).isEqualTo(BlockTestStrings.EMPTY_SHADOW_WITH_POSITION);
    }

    @Test
    public void testSerializeValue() throws IOException, BlockLoadingException {
        Block block = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("frankenblock").withId("364"));
        block.setPosition(37, 13);

        Input input = block.getInputByName("value_input");
        Block inputBlock = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("output_foo").withId("VALUE_GOOD"));
        input.getConnection().connect(inputBlock.getOutputConnection());

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XmlSerializer serializer = getXmlSerializer(os);
        block.serialize(serializer, /* root block */ true, IOOptions.WRITE_ALL_DATA);
        serializer.flush();

        String expected = BlockTestStrings.frankenBlockStart("block", "364")
                + BlockTestStrings.VALUE_GOOD
                + BlockTestStrings.FRANKENBLOCK_DEFAULT_VALUES
                + BlockTestStrings.BLOCK_END;
        assertThat(os.toString()).isEqualTo(expected);
    }

    @Test
    public void testSerializeShadowValue() throws IOException, BlockLoadingException {
        Block block = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("frankenblock").withId("364").atPosition(37, 13));

        Input input = block.getInputByName("value_input");
        Block inputBlock = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("output_foo").withId("VALUE_REAL"));
        input.getConnection().connect(inputBlock.getOutputConnection());
        inputBlock = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().shadow().ofType("output_foo").withId("VALUE_SHADOW"));
        input.getConnection().setShadowConnection(inputBlock.getOutputConnection());

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XmlSerializer serializer = getXmlSerializer(os);
        block.serialize(serializer, /* root block */ true, IOOptions.WRITE_ALL_DATA);
        serializer.flush();

        String expected = BlockTestStrings.frankenBlockStart("block", "364")
                + BlockTestStrings.VALUE_SHADOW_GOOD
                + BlockTestStrings.FRANKENBLOCK_DEFAULT_VALUES
                + BlockTestStrings.BLOCK_END;
        assertThat(os.toString()).isEqualTo(expected);

        block = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("frankenblock").withId("777"));
        block.setPosition(37, 13);
        input = block.getInputByName("value_input");
        inputBlock = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().shadow().ofType("simple_input_output").withId("SHADOW1"));
        input.getConnection().connect(inputBlock.getOutputConnection());
        input = inputBlock.getOnlyValueInput();
        inputBlock = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().shadow().ofType("simple_input_output").withId("SHADOW2"));
        input.getConnection().connect(inputBlock.getOutputConnection());

        os.reset();
        block.serialize(serializer, /* root block */ true, IOOptions.WRITE_ALL_DATA);
        serializer.flush();

        expected = BlockTestStrings.frankenBlockStart("block", "777")
                + BlockTestStrings.VALUE_NESTED_SHADOW
                + BlockTestStrings.FRANKENBLOCK_DEFAULT_VALUES
                + BlockTestStrings.BLOCK_END;
        assertThat(os.toString()).isEqualTo(expected);
    }

    @Test
    public void testSerializeStatement() throws IOException, BlockLoadingException {
        Block block = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("frankenblock").withId("364"));
        block.setPosition(37, 13);

        Input input = block.getInputByName("NAME");
        Block inputBlock = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("frankenblock").withId("3"));
        input.getConnection().connect(inputBlock.getPreviousConnection());

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XmlSerializer serializer = getXmlSerializer(os);
        block.serialize(serializer, /* root block */ true, IOOptions.WRITE_ALL_DATA);
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
    public void testGetAllConnections() throws BlockLoadingException {
        Block block = mBlockFactory.obtainBlockFrom(new BlockTemplate().ofType("frankenblock"));
        List<Connection> allConnections = new ArrayList<>();
        block.getAllConnections(allConnections);
        assertThat(allConnections.size()).isEqualTo(4);

        block = mBlockFactory.obtainBlockFrom(new BlockTemplate().ofType("frankenblock"));
        allConnections.clear();
        block.getAllConnections(allConnections);
        assertThat(allConnections.size()).isEqualTo(4);

        Block ivb = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("simple_input_output"));
        block.getInputs().get(0).getConnection().connect(ivb.getOutputConnection());
        Block svb = mBlockFactory.obtainBlockFrom(new BlockTemplate().ofType("output_no_input"));
        ivb.getInputs().get(0).getConnection().connect(svb.getOutputConnection());
        Block smb = mBlockFactory.obtainBlockFrom(new BlockTemplate().ofType("statement_no_input"));
        block.getInputs().get(1).getConnection().connect(smb.getPreviousConnection());

        allConnections.clear();
        block.getAllConnectionsRecursive(allConnections);
        assertThat(allConnections.size()).isEqualTo(9);
    }

    @Test
    public void testGetOnlyValueInput() throws BlockLoadingException {
        // No inputs.
        assertThat(mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("statement_no_input")
        ).getOnlyValueInput()).isNull();

        // One value input.
        Block underTest = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("statement_value_input"));
        assertThat(underTest.getInputByName("value")).isSameAs(underTest.getOnlyValueInput());

        // Statement input, no value inputs.
        assertThat(mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("statement_statement_input")
        ).getOnlyValueInput()).isNull();

        // Multiple value inputs.
        assertThat(mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("statement_multiple_value_input")
        ).getOnlyValueInput());

        // Statement input, dummy input and value input.
        underTest = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("controls_repeat_ext"));
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
    public void testGetLastUnconnectedInputConnectionOneInputAtEnd() throws BlockLoadingException {
        // Two simple input blocks
        ArrayList<Block> blocks = new ArrayList<>();
        Block first = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("simple_input_output").withId("first block"));
        Block second = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("simple_input_output").withId("second block"));
        first.getOnlyValueInput().getConnection().connect(second.getOutputConnection());
        blocks.add(first);

        assertThat(second.getLastUnconnectedInputConnection())
                .isSameAs(second.getOnlyValueInput().getConnection());
        assertThat(first.getLastUnconnectedInputConnection())
                .isSameAs(second.getOnlyValueInput().getConnection());
    }

    @Test
    public void testGetLastUnconnectedInputConnectionMultipleInputs() throws BlockLoadingException {
        ArrayList<Block> blocks = new ArrayList<>();
        Block first = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("simple_input_output").withId("first block"));
        Block second = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("simple_input_output").withId("second block"));
        Block third = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("multiple_input_output").withId("third block"));
        first.getOnlyValueInput().getConnection().connect(second.getOutputConnection());
        second.getOnlyValueInput().getConnection().connect(third.getOutputConnection());
        blocks.add(first);

        assertThat(third.getLastUnconnectedInputConnection()).isNull();
        assertThat(second.getLastUnconnectedInputConnection()).isNull();
        assertThat(first.getLastUnconnectedInputConnection()).isNull();
    }

    @Test
    public void testGetLastUnconnectedInputConnectionNoInput() throws BlockLoadingException {
        ArrayList<Block> blocks = new ArrayList<>();
        Block first = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("simple_input_output").withId("first block"));
        Block second = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("simple_input_output").withId("second block"));
        Block third = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("output_no_input").withId("third block"));
        first.getOnlyValueInput().getConnection().connect(second.getOutputConnection());
        second.getOnlyValueInput().getConnection().connect(third.getOutputConnection());
        blocks.add(first);

        assertThat(third.getLastUnconnectedInputConnection()).isNull();
        assertThat(second.getLastUnconnectedInputConnection()).isNull();
        assertThat(first.getLastUnconnectedInputConnection()).isNull();
    }

    @Test
    public void testGetLastUnconnectedInputConnectionShadowAtEnd() throws BlockLoadingException {
        // Two simple input blocks
        ArrayList<Block> blocks = new ArrayList<>();
        Block first = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("simple_input_output").withId("first block"));
        Block second = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("simple_input_output").withId("second block"));
        Block shadow = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().shadow().copyOf(second).withId("shadow block"));

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
    public void testLastBlockInSequence_blockLacksNext() throws BlockLoadingException {
        Block block = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("statement_input_no_next").withId("block"));

        // This value block should not returned; it is not connected to the next connection.
        Block value = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("output_no_input").withId("value"));
        block.getInputByName("value").getConnection().connect(value.getOutputConnection());

        assertThat(block).isSameAs(block.getLastBlockInSequence());
    }

    @Test
    public void testLastBlockInSequence_noBlockConnected() throws BlockLoadingException {
        Block block = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("statement_value_input").withId("block"));

        // This value block should not returned; it is not connected to the next connection.
        Block value = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("output_no_input").withId("value"));
        block.getInputByName("value").getConnection().connect(value.getOutputConnection());

        assertThat(block).isSameAs(block.getLastBlockInSequence());
    }

    @Test
    public void testLastBlockInSequence_lastBlockUnconnected() throws BlockLoadingException {
        Block first = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("statement_no_input").withId("first block"));
        Block second = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("statement_no_input").withId("second block"));
        Block third = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("statement_value_input").withId("third block"));

        // This value block should not returned; it is not connected to the next connection.
        Block value = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("output_no_input").withId("value"));

        first.getNextConnection().connect(second.getPreviousConnection());
        second.getNextConnection().connect(third.getPreviousConnection());
        third.getInputByName("value").getConnection().connect(value.getOutputConnection());

        assertThat(third).isSameAs(first.getLastBlockInSequence());
    }

    @Test
    public void testLastBlockInSequence_lastBlockNoNext() throws BlockLoadingException {
        Block first = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("statement_no_input").withId("first block"));
        Block second = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("statement_no_input").withId("second block"));
        Block third = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("statement_input_no_next").withId("third block"));

        // This value block should not returned; it is not connected to the next connection.
        Block value = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("output_no_input").withId("value"));

        first.getNextConnection().connect(second.getPreviousConnection());
        second.getNextConnection().connect(third.getPreviousConnection());
        third.getInputByName("value").getConnection().connect(value.getOutputConnection());

        assertThat(third).isSameAs(first.getLastBlockInSequence());
    }

    @Test
    public void testLastBlockInSequence_lastBlockShadow() throws BlockLoadingException {
        Block first = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("statement_no_input").withId("first block"));
        Block second = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("statement_no_input").withId("second block"));
        Block shadow = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().shadow().copyOf(second).withId("shadow block"));

        first.getNextConnection().connect(second.getPreviousConnection());
        second.getNextConnection().setShadowConnection(shadow.getPreviousConnection());
        second.getNextConnection().connect(shadow.getPreviousConnection());

        assertThat(second).isSameAs(first.getLastBlockInSequence());
    }

    @Test
    public void testBlockIdSerializedDeserialized() throws BlockLoadingException {
        Block block = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("statement_no_input").withId("123"));
    }

    @Test
    public void testCollapsed() throws BlockLoadingException {
        mController.addCallback(mEventsCallback);

        final Block block = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("statement_no_input"));

        runAndSync(new Runnable() {
            @Override
            public void run() {
                block.setEventWorkspaceId(FAKE_WORKSPACE_ID);

                // Check default
                assertWithMessage("By default, blocks are not collapsed.")
                        .that(block.isCollapsed()).isFalse();

                // No attribute when saved with default value
                String blockXml = toXml(block);
                assertWithMessage("Default state is not stored in XML")
                        .that(blockXml).doesNotContain("collapsed");

                // Check default for blocks loaded from XML
                Block blockFromXml = fromXmlWithoutId(blockXml);
                assertWithMessage("By default, blocks loaded from XML are not collapsed.")
                        .that(blockFromXml.isCollapsed()).isFalse();

                // false -> false
                mEventsCallback.mEventsReceived.clear();
                block.setCollapsed(false);
                assertThat(mEventsCallback.mEventsReceived).hasSize(0);

                // false -> true
                mEventsCallback.mEventsReceived.clear();
                block.setCollapsed(true);
                assertWithMessage("Collapsed state can change.").that(block.isCollapsed()).isTrue();
                assertThat(mEventsCallback.mEventsReceived).hasSize(1);         // One event group
                assertThat(mEventsCallback.mEventsReceived.get(0)).hasSize(1);  // a single message
                BlocklyEvent.ChangeEvent changeEvent =
                        (BlocklyEvent.ChangeEvent) mEventsCallback.mEventsReceived.get(0).get(0);
                assertThat(changeEvent.getElement()).isSameAs(BlocklyEvent.ELEMENT_COLLAPSED);
                assertThat(changeEvent.getFieldName()).isNull();
                assertThat(changeEvent.getOldValue()).isEqualTo("false");
                assertThat(changeEvent.getNewValue()).isEqualTo("true");

                // collapsed attribute in XML when value is true
                blockXml = toXml(block);
                assertWithMessage("Collapsed state is stored in XML.")
                        .that(blockXml.contains("collapsed=\"true\"")).isTrue();

                // loads correctly from XML
                blockFromXml = fromXmlWithoutId(blockXml);
                assertWithMessage("Collapsed state set from XML.")
                        .that(blockFromXml.isCollapsed()).isTrue();
            }
        });
    }

    @Test
    public void testDeletable() throws BlockLoadingException {
        Block block = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("statement_no_input"));
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
    public void testDisabled() throws BlockLoadingException {
        mController.addCallback(mEventsCallback);

        final Block block = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("statement_no_input"));

        runAndSync(new Runnable() {
            @Override
            public void run() {
                block.setEventWorkspaceId(FAKE_WORKSPACE_ID);

                assertWithMessage("By default, blocks are not disabled.")
                        .that(block.isDisabled()).isFalse();

                // No disabled attribute if disabled is false (the default)
                String blockXml = toXml(block);
                assertWithMessage("Disabled state is stored in XML.")
                        .that(blockXml).doesNotContain("disabled");

                // false -> false
                mEventsCallback.mEventsReceived.clear();
                block.setDisabled(false);
                assertWithMessage("Overwriting the disabled state with the same value " +
                                  "(false -> false) does not generate events.")
                        .that(mEventsCallback.mEventsReceived).hasSize(0);

                blockXml = toXml(block);
                assertWithMessage("Default state is not stored in XML")
                        .that(blockXml.contains("disabled")).isFalse();

                Block blockFromXml = fromXmlWithoutId(blockXml);
                assertWithMessage("By default, blocks loaded from XML are not disabled.")
                        .that(blockFromXml.isDisabled()).isFalse();

                // false -> true
                mEventsCallback.mEventsReceived.clear();
                block.setDisabled(true);
                assertWithMessage("Disabled state can change.")
                        .that(block.isDisabled()).isTrue();
                assertThat(mEventsCallback.mEventsReceived).hasSize(1);
                assertThat(mEventsCallback.mEventsReceived.get(0)).hasSize(1);
                BlocklyEvent.ChangeEvent changeEvent =
                        (BlocklyEvent.ChangeEvent) mEventsCallback.mEventsReceived.get(0).get(0);
                assertThat(changeEvent.getElement()).isSameAs(BlocklyEvent.ELEMENT_DISABLED);
                assertThat(changeEvent.getFieldName()).isNull();
                assertThat(changeEvent.getOldValue()).isEqualTo("false");
                assertThat(changeEvent.getNewValue()).isEqualTo("true");

                // true -> true
                mEventsCallback.mEventsReceived.clear();
                block.setDisabled(true);
                assertWithMessage("Overwriting the disabled state with the same value " +
                                  "(true -> true)does not generate events.")
                        .that(mEventsCallback.mEventsReceived).hasSize(0);

                // True value saved to XML
                blockXml = toXml(block);
                assertWithMessage("Disabled state is stored in XML.")
                        .that(blockXml).contains("disabled=\"true\"");

                // Restored from XML
                blockFromXml = fromXmlWithoutId(blockXml);
                assertWithMessage("Disabled state set from XML.")
                        .that(blockFromXml.isDisabled()).isTrue();

                // true -> false
                mEventsCallback.mEventsReceived.clear();
                block.setDisabled(false);
                assertThat(block.isDisabled()).isFalse();
                assertThat(mEventsCallback.mEventsReceived).hasSize(1);
                assertThat(mEventsCallback.mEventsReceived.get(0)).hasSize(1);
                changeEvent = (BlocklyEvent.ChangeEvent) mEventsCallback.mEventsReceived.get(0).get(0);
                assertThat(changeEvent.getElement()).isSameAs(BlocklyEvent.ELEMENT_DISABLED);
                assertThat(changeEvent.getFieldName()).isNull();
                assertThat(changeEvent.getOldValue()).isEqualTo("true");
                assertThat(changeEvent.getNewValue()).isEqualTo("false");
            }
        });
    }

    @Test
    public void testEditable() throws BlockLoadingException {
        Block block = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("statement_no_input"));
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
    public void testSetInputsInline() throws BlockLoadingException {
        final Block blockDefaultInline = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("math_arithmetic"));
        final Block blockDefaultExternal = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("logic_ternary"));

        mController.addCallback(mEventsCallback);
        blockDefaultExternal.registerObserver(mBlockObserver);
        blockDefaultInline.registerObserver(mBlockObserver);

        runAndSync(new Runnable() {
            @Override
            public void run() {
                blockDefaultInline.setEventWorkspaceId(FAKE_WORKSPACE_ID);
                blockDefaultExternal.setEventWorkspaceId(FAKE_WORKSPACE_ID);

                assertWithMessage("By default, arithmetic block has inline inputs.")
                        .that(blockDefaultInline.getInputsInline()).isTrue();
                assertWithMessage("By default, ternary block has external inputs.")
                        .that(blockDefaultExternal.getInputsInline()).isFalse();

                // No inline attribute if value is the default
                String blockXml = toXml(blockDefaultInline);
                assertWithMessage("Default inline state (true) is stored in XML.")
                        .that(blockXml).doesNotContain("inline");
                Block blockFromXml = fromXmlWithoutId(blockXml);
                assertWithMessage("By default, arithmetic blocks loaded from XML are inline.")
                        .that(blockFromXml.getInputsInline()).isTrue();

                blockXml = toXml(blockDefaultExternal);
                assertWithMessage("Default inline state (false) is stored in XML.")
                        .that(blockXml).doesNotContain("inline");
                blockFromXml = fromXmlWithoutId(blockXml);
                assertWithMessage("By default, ternary blocks loaded from XML are not inline.")
                        .that(blockFromXml.getInputsInline()).isFalse();

                // true -> true
                mEventsCallback.mEventsReceived.clear();
                mBlockObserver.mObservations.clear();
                blockDefaultInline.setInputsInline(true);
                assertWithMessage("Rewriting the true state with the same value " +
                                  "does not generate observer.")
                        .that(mBlockObserver.mObservations).hasSize(0);
                assertWithMessage("Rewriting the true state with the same value " +
                                  "does not generate blockly events.")
                        .that(mEventsCallback.mEventsReceived).hasSize(0);

                // false -> false
                mEventsCallback.mEventsReceived.clear();
                mBlockObserver.mObservations.clear();
                blockDefaultExternal.setInputsInline(false);
                assertWithMessage("Rewriting the false state with the same value " +
                        "does not generate observer.")
                        .that(mBlockObserver.mObservations).hasSize(0);
                assertWithMessage("Rewriting the false state with the same value " +
                        "does not generate blockly events.")
                        .that(mEventsCallback.mEventsReceived).hasSize(0);

                // true -> false
                mEventsCallback.mEventsReceived.clear();
                mBlockObserver.mObservations.clear();
                blockDefaultInline.setInputsInline(false);
                assertWithMessage("Inline inputs state can change.")
                        .that(blockDefaultInline.getInputsInline()).isFalse();
                assertThat(mBlockObserver.mObservations).hasSize(1);
                assertThat(mBlockObserver.mObservations.get(0).mBlock).isSameAs(blockDefaultInline);
                assertThat(mBlockObserver.mObservations.get(0).mUpdateStateMask)
                        .isEqualTo(Block.UPDATE_INPUTS_INLINE);
                assertThat(mEventsCallback.mEventsReceived).hasSize(1);
                assertThat(mEventsCallback.mEventsReceived.get(0)).hasSize(1);
                BlocklyEvent.ChangeEvent changeEvent =
                        (BlocklyEvent.ChangeEvent) mEventsCallback.mEventsReceived.get(0).get(0);
                assertThat(changeEvent.getElement()).isSameAs(BlocklyEvent.ELEMENT_INLINE);
                assertThat(changeEvent.getFieldName()).isNull();
                assertThat(changeEvent.getOldValue()).isEqualTo("true");
                assertThat(changeEvent.getNewValue()).isEqualTo("false");

                // false -> true
                mEventsCallback.mEventsReceived.clear();
                mBlockObserver.mObservations.clear();
                blockDefaultExternal.setInputsInline(true);
                assertWithMessage("Inline inputs state can change.")
                        .that(blockDefaultExternal.getInputsInline()).isTrue();
                assertThat(mBlockObserver.mObservations).hasSize(1);
                assertThat(mBlockObserver.mObservations.get(0).mBlock)
                        .isSameAs(blockDefaultExternal);
                assertThat(mBlockObserver.mObservations.get(0).mUpdateStateMask)
                        .isEqualTo(Block.UPDATE_INPUTS_INLINE);
                assertThat(mEventsCallback.mEventsReceived).hasSize(1);
                assertThat(mEventsCallback.mEventsReceived.get(0)).hasSize(1);
                changeEvent = (BlocklyEvent.ChangeEvent) mEventsCallback.mEventsReceived.get(0).get(0);
                assertThat(changeEvent.getElement()).isSameAs(BlocklyEvent.ELEMENT_INLINE);
                assertThat(changeEvent.getFieldName()).isNull();
                assertThat(changeEvent.getOldValue()).isEqualTo("false");
                assertThat(changeEvent.getNewValue()).isEqualTo("true");

                // A changed-to-true value saved to XML
                blockXml = toXml(blockDefaultExternal);
                assertWithMessage("Inline true state is stored in XML if the default is false")
                        .that(blockXml).contains("inline=\"true\"");
                blockFromXml = fromXmlWithoutId(blockXml);
                assertWithMessage("Inline state set from XML.")
                        .that(blockFromXml.getInputsInline()).isTrue();

                // A changed-to-false value saved to XML
                blockXml = toXml(blockDefaultInline);
                assertWithMessage("Inline false state is stored in XML if the default is true")
                        .that(blockXml).contains("inline=\"false\"");
                blockFromXml = fromXmlWithoutId(blockXml);
                assertWithMessage("Inline state set from XML.")
                        .that(blockFromXml.getInputsInline()).isFalse();
            }
        });
    }

    @Test
    public void testMovable() throws BlockLoadingException {
        Block block = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("statement_no_input"));
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
    public void testEscapeFieldData() throws BlockLoadingException {
        Block block = mBlockFactory.obtainBlockFrom(new BlockTemplate().ofType("text"));
        block.getFieldByName("TEXT").setFromString(
                "less than < greater than > ampersand & quote \" apostrophe ' end");

        String xml = toXml(block);
        Matcher matcher = Pattern.compile("less.*end").matcher(xml);
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group()).isEqualTo(
                "less than &lt; greater than &gt; ampersand &amp; quote \" apostrophe ' end");
    }

    @Test
    public void testSetComment() throws BlockLoadingException {
        mController.addCallback(mEventsCallback);

        final Block block = mBlockFactory.obtainBlockFrom(new BlockTemplate().ofType("text"));
        block.registerObserver(mBlockObserver);

        final String newComment = "New comment.";
        final String updatedComment = "Updated comment.";

        runAndSync(new Runnable() {
            @Override
            public void run() {
                block.setEventWorkspaceId(FAKE_WORKSPACE_ID);

                // Preconditions
                assertThat(block.getComment()).isNull();
                assertThat(newComment).isNotEqualTo(updatedComment);

                // Test overwrite null with null
                mEventsCallback.mEventsReceived.clear();
                mBlockObserver.mObservations.clear();
                block.setComment(null);
                assertThat(block.getComment()).isNull();
                assertWithMessage("Observer is not called with null comment (same as before)")
                        .that(mBlockObserver.mObservations).isEmpty();
                assertThat(mEventsCallback.mEventsReceived).isEmpty();

                // Test overwrite null with new value
                mEventsCallback.mEventsReceived.clear();
                mBlockObserver.mObservations.clear();
                block.setComment(newComment);
                assertThat(block.getComment()).isEqualTo(newComment);
                assertWithMessage("Observer is called once during .setComment(newComment)")
                        .that(mBlockObserver.mObservations).hasSize(1);
                assertThat(mBlockObserver.mObservations.get(0).mBlock).isSameAs(block);
                assertThat(mBlockObserver.mObservations.get(0).mUpdateStateMask)
                        .isEqualTo(Block.UPDATE_COMMENT);
                assertWithMessage("EventsCallback is called once during .setComment(newComment)")
                        .that(mEventsCallback.mEventsReceived).hasSize(1);      // One group
                assertThat(mEventsCallback.mEventsReceived.get(0)).hasSize(1);  // with one event
                BlocklyEvent.ChangeEvent changeEvent =
                        (BlocklyEvent.ChangeEvent) mEventsCallback.mEventsReceived.get(0).get(0);
                assertThat(changeEvent.getElement()).isSameAs(BlocklyEvent.ELEMENT_COMMENT);
                assertThat(changeEvent.getFieldName()).isNull();
                assertThat(changeEvent.getOldValue()).isNull();
                assertThat(changeEvent.getNewValue()).isSameAs(newComment);

                // Test overwrite comment value with equal value
                mEventsCallback.mEventsReceived.clear();
                mBlockObserver.mObservations.clear();
                block.setComment(new String(newComment));
                assertThat(block.getComment()).isEqualTo(newComment);
                assertWithMessage("Observer is not called when .setComment() called with same comment")
                        .that(mBlockObserver.mObservations).isEmpty();
                assertWithMessage("EventsCallback is not called when .setComment() called with same comment")
                        .that(mEventsCallback.mEventsReceived).isEmpty();

                // Test overwrite comment with different value
                mEventsCallback.mEventsReceived.clear();
                mBlockObserver.mObservations.clear();
                block.setComment(updatedComment);
                assertThat(block.getComment()).isEqualTo(updatedComment);
                assertWithMessage("Observer is called once during .setComment(updatedComment)")
                        .that(mBlockObserver.mObservations).hasSize(1);
                assertThat(mBlockObserver.mObservations.get(0).mBlock).isSameAs(block);
                assertThat(mBlockObserver.mObservations.get(0).mUpdateStateMask)
                        .isEqualTo(Block.UPDATE_COMMENT);
                assertWithMessage("EventsCallback is called once during .setComment(updatedComment)")
                        .that(mEventsCallback.mEventsReceived).hasSize(1);      // One call / event group
                assertThat(mEventsCallback.mEventsReceived.get(0)).hasSize(1);  // with one event
                changeEvent = (BlocklyEvent.ChangeEvent) mEventsCallback.mEventsReceived.get(0).get(0);
                assertThat(changeEvent.getElement()).isSameAs(BlocklyEvent.ELEMENT_COMMENT);
                assertThat(changeEvent.getFieldName()).isNull();
                assertThat(changeEvent.getOldValue()).isEqualTo(newComment);
                assertThat(changeEvent.getNewValue()).isSameAs(updatedComment);

                // Test delete value (overwrite with null)
                mEventsCallback.mEventsReceived.clear();
                mBlockObserver.mObservations.clear();
                block.setComment(null);
                assertThat(block.getComment()).isNull();
                assertWithMessage("Observer is called once during .setComment(null) deletion")
                        .that(mBlockObserver.mObservations).hasSize(1);
                assertThat(mBlockObserver.mObservations.get(0).mBlock).isSameAs(block);
                assertThat(mBlockObserver.mObservations.get(0).mUpdateStateMask)
                        .isEqualTo(Block.UPDATE_COMMENT);
                assertWithMessage("EventsCallback is called once during .setComment(null)")
                        .that(mEventsCallback.mEventsReceived).hasSize(1);      // One call / event group
                assertThat(mEventsCallback.mEventsReceived.get(0)).hasSize(1);  // with one event
                changeEvent = (BlocklyEvent.ChangeEvent) mEventsCallback.mEventsReceived.get(0).get(0);
                assertThat(changeEvent.getElement()).isSameAs(BlocklyEvent.ELEMENT_COMMENT);
                assertThat(changeEvent.getFieldName()).isNull();
                assertThat(changeEvent.getOldValue()).isEqualTo(updatedComment);
                assertThat(changeEvent.getNewValue()).isNull();
            }
        });
    }

    @Test
    public void testSetShadow_withObserver() throws BlockLoadingException {
        Block block = mBlockFactory.obtainBlockFrom(new BlockTemplate().ofType("text"));
        block.registerObserver(mBlockObserver);

        // Preconditions
        assertThat(block.isShadow()).isFalse();

        // Test false -> false
        mBlockObserver.mObservations.clear();
        block.setShadow(false);
        assertThat(block.isShadow()).isFalse();
        assertWithMessage("Observer is not called when non-shadow .setShadow(false)")
                .that(mBlockObserver.mObservations).isEmpty();

        // Test false -> true
        mBlockObserver.mObservations.clear();
        block.setShadow(true);
        assertThat(block.isShadow()).isTrue();
        assertWithMessage("Observer is called when non-shadow .setShadow(true)")
                .that(mBlockObserver.mObservations).hasSize(1);
        assertThat(mBlockObserver.mObservations.get(0).mBlock).isSameAs(block);
        assertThat(mBlockObserver.mObservations.get(0).mUpdateStateMask)
                .isEqualTo(Block.UPDATE_IS_SHADOW);

        // Test true -> true
        mBlockObserver.mObservations.clear();
        block.setShadow(true);
        assertThat(block.isShadow()).isTrue();
        assertWithMessage("Observer is not called when shadow .setShadow(true)")
                .that(mBlockObserver.mObservations).isEmpty();

        // Test true -> false
        mBlockObserver.mObservations.clear();
        block.setShadow(false);
        assertThat(block.isShadow()).isFalse();
        assertWithMessage("Observer is called when shadow .setShadow(false)")
                .that(mBlockObserver.mObservations).hasSize(1);
        assertThat(mBlockObserver.mObservations.get(0).mBlock).isSameAs(block);
        assertThat(mBlockObserver.mObservations.get(0).mUpdateStateMask)
                .isEqualTo(Block.UPDATE_IS_SHADOW);
    }

    @Test
    public void testSetDisabled_withObserver() throws BlockLoadingException {
        final Block block = mBlockFactory.obtainBlockFrom(new BlockTemplate().ofType("text"));

        runAndSync(new Runnable() {
            @Override
            public void run() {
                block.registerObserver(mBlockObserver);

                // Preconditions
                assertThat(block.isDisabled()).isFalse();

                // Test false -> false
                mBlockObserver.mObservations.clear();
                block.setDisabled(false);
                assertThat(block.isDisabled()).isFalse();
                assertWithMessage(
                        "Observer is not called when non-disabled block .setDisabled(false)")
                        .that(mBlockObserver.mObservations).isEmpty();

                // Test false -> true
                mBlockObserver.mObservations.clear();
                block.setDisabled(true);
                assertThat(block.isDisabled()).isTrue();
                assertWithMessage("Observer is called when non-disabled block .setDisabled(false)")
                        .that(mBlockObserver.mObservations).hasSize(1);
                assertThat(mBlockObserver.mObservations.get(0).mBlock).isSameAs(block);
                assertThat(mBlockObserver.mObservations.get(0).mUpdateStateMask)
                        .isEqualTo(Block.UPDATE_IS_DISABLED);

                // Test true -> true
                mBlockObserver.mObservations.clear();
                block.setDisabled(true);
                assertThat(block.isDisabled()).isTrue();
                assertWithMessage("Observer is not called when disabled block .setDisabled(true)")
                        .that(mBlockObserver.mObservations).isEmpty();

                // Test true -> false
                mBlockObserver.mObservations.clear();
                block.setDisabled(false);
                assertThat(block.isDisabled()).isFalse();
                assertWithMessage("Observer is called when disabled block .setDisabled(false)")
                        .that(mBlockObserver.mObservations).hasSize(1);
                assertThat(mBlockObserver.mObservations.get(0).mBlock).isSameAs(block);
                assertThat(mBlockObserver.mObservations.get(0).mUpdateStateMask)
                        .isEqualTo(Block.UPDATE_IS_DISABLED);
            }
        });
    }

    @Test
    public void testSetCollapsed_withObserver() throws BlockLoadingException {
        mController.addCallback(mEventsCallback);

        final Block block = mBlockFactory.obtainBlockFrom(new BlockTemplate().ofType("text"));
        block.registerObserver(mBlockObserver);

        runAndSync(new Runnable() {
            @Override
            public void run() {
                // Preconditions
                assertThat(block.isCollapsed()).isFalse();

                // Test false -> false
                mBlockObserver.mObservations.clear();
                block.setCollapsed(false);
                assertThat(block.isCollapsed()).isFalse();
                assertWithMessage(
                        "Observer is not called when non-collapsed block .setCollapsed(false)")
                        .that(mBlockObserver.mObservations).isEmpty();

                // Test false -> true
                mBlockObserver.mObservations.clear();
                block.setCollapsed(true);
                assertThat(block.isCollapsed()).isTrue();
                assertWithMessage("Observer is called when non-collapsed block .setCollapsed(true)")
                        .that(mBlockObserver.mObservations).hasSize(1);
                assertThat(mBlockObserver.mObservations.get(0).mBlock).isSameAs(block);
                assertThat(mBlockObserver.mObservations.get(0).mUpdateStateMask)
                        .isEqualTo(Block.UPDATE_IS_COLLAPSED);

                // Test true -> true
                mBlockObserver.mObservations.clear();
                block.setCollapsed(true);
                assertThat(block.isCollapsed()).isTrue();
                assertWithMessage("Observer is not called when collapsed block .setCollapsed(true)")
                        .that(mBlockObserver.mObservations).isEmpty();

                // Test true -> false
                mBlockObserver.mObservations.clear();
                block.setCollapsed(false);
                assertThat(block.isCollapsed()).isFalse();
                assertWithMessage("Observer is called when collapsed block .setCollapsed(false)")
                        .that(mBlockObserver.mObservations).hasSize(1);
                assertThat(mBlockObserver.mObservations.get(0).mBlock).isSameAs(block);
                assertThat(mBlockObserver.mObservations.get(0).mUpdateStateMask)
                        .isEqualTo(Block.UPDATE_IS_COLLAPSED);
            }
        });
    }

    @Test
    public void testSetEditable_withObserver() throws BlockLoadingException {
        Block block = mBlockFactory.obtainBlockFrom(new BlockTemplate().ofType("text"));
        block.registerObserver(mBlockObserver);

        // Preconditions
        assertThat(block.isEditable()).isTrue();

        // Test true -> true
        mBlockObserver.mObservations.clear();
        block.setEditable(true);
        assertThat(block.isEditable()).isTrue();
        assertWithMessage("Observer is not called when editable block .setEditable(true)")
                .that(mBlockObserver.mObservations).isEmpty();

        // Test true -> false
        mBlockObserver.mObservations.clear();
        block.setEditable(false);
        assertThat(block.isEditable()).isFalse();
        assertWithMessage("Observer is called when editable block .setEditable(false)")
                .that(mBlockObserver.mObservations).hasSize(1);
        assertThat(mBlockObserver.mObservations.get(0).mBlock).isSameAs(block);
        assertThat(mBlockObserver.mObservations.get(0).mUpdateStateMask)
                .isEqualTo(Block.UPDATE_IS_EDITABLE);

        // Test false -> false
        mBlockObserver.mObservations.clear();
        block.setEditable(false);
        assertThat(block.isEditable()).isFalse();
        assertWithMessage("Observer is not called when non-editable block .setEditable(false)")
                .that(mBlockObserver.mObservations).isEmpty();

        // Test false -> true
        mBlockObserver.mObservations.clear();
        block.setEditable(true);
        assertThat(block.isEditable()).isTrue();
        assertWithMessage("Observer is called when non-editable block .setEditable(true)")
                .that(mBlockObserver.mObservations).hasSize(1);
        assertThat(mBlockObserver.mObservations.get(0).mBlock).isSameAs(block);
        assertThat(mBlockObserver.mObservations.get(0).mUpdateStateMask)
                .isEqualTo(Block.UPDATE_IS_EDITABLE);
    }

    @Test
    public void testSetDeletable_withObserver() throws BlockLoadingException {
        Block block = mBlockFactory.obtainBlockFrom(new BlockTemplate().ofType("text"));
        block.registerObserver(mBlockObserver);

        // Preconditions
        assertThat(block.isDeletable()).isTrue();

        // Test true -> true
        mBlockObserver.mObservations.clear();
        block.setDeletable(true);
        assertThat(block.isDeletable()).isTrue();
        assertWithMessage("Observer is not called when deletable block .setDeletable(true)")
                .that(mBlockObserver.mObservations).isEmpty();

        // Test true -> false
        mBlockObserver.mObservations.clear();
        block.setDeletable(false);
        assertThat(block.isDeletable()).isFalse();
        assertWithMessage("Observer is called when deletable block .setDeletable(false)")
                .that(mBlockObserver.mObservations).hasSize(1);
        assertThat(mBlockObserver.mObservations.get(0).mBlock).isSameAs(block);
        assertThat(mBlockObserver.mObservations.get(0).mUpdateStateMask).isEqualTo(Block.UPDATE_IS_DELETABLE);

        // Test false -> false
        mBlockObserver.mObservations.clear();
        block.setDeletable(false);
        assertThat(block.isDeletable()).isFalse();
        assertWithMessage("Observer is not called when non-deletable block .setDeletable(false)")
                .that(mBlockObserver.mObservations).isEmpty();

        // Test false -> true
        mBlockObserver.mObservations.clear();
        block.setDeletable(true);
        assertThat(block.isDeletable()).isTrue();
        assertWithMessage("Observer is called when non-deletable block .setDeletable(true)")
                .that(mBlockObserver.mObservations).hasSize(1);
        assertThat(mBlockObserver.mObservations.get(0).mBlock).isSameAs(block);
        assertThat(mBlockObserver.mObservations.get(0).mUpdateStateMask)
                .isEqualTo(Block.UPDATE_IS_DELETABLE);
    }

    @Test
    public void testSetEventCallback() throws BlockLoadingException {
        final Block block = mBlockFactory.obtainBlockFrom(new BlockTemplate().ofType("text"));
        final BlocklyEvent event = new BlocklyEvent.ChangeEvent(
                BlocklyEvent.ELEMENT_MUTATE, block, null, "", "");

        runAndSync(new Runnable() {
            @Override
            public void run() {
                assertThat(mController.getCallbackCount()).isEqualTo(0);
                block.setEventWorkspaceId("Fake Workspace Id");

                // Reference the event list directly, for later verification.
                List<List<BlocklyEvent>> eventsReceived = mEventsCallback.mEventsReceived;
                eventsReceived.clear();

                // Send event before setting block callback
                mController.addPendingEvent(event);
                assertThat(mEventsCallback.mEventsReceived).isEmpty();

                // Set callback and attempt to remove the direct reference to the callback.
                block.setEventCallback(mEventsCallback);
                assertThat(mController.getCallbackCount()).isEqualTo(1);
                mEventsCallback = null;
                System.gc();
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {} // Ignored.

                // Try another event.
                mController.addPendingEvent(event);
                assertThat(eventsReceived).hasSize(1);         // One event group
                assertThat(eventsReceived.get(0)).hasSize(1);  // One event
                assertThat(eventsReceived.get(0).get(0)).isSameAs(event);

                // Unset callback and try again
                eventsReceived.clear();
                block.setEventCallback(null);
                mController.addPendingEvent(event);
                assertThat(eventsReceived).isEmpty();

                assertThat(mController.getCallbackCount()).isEqualTo(0);
            }
        });
    }

    private String toXml(Block block) {
        StringOutputStream out = new StringOutputStream();
        try {
            BlocklyXmlHelper.writeBlockToXml(block, out, null);
        } catch (BlocklySerializerException e) {
            throw new IllegalArgumentException("Failed to serialize block.", e);
        }
        return out.toString();
    }

    // TODO: Replace with IOOption
    private Block fromXmlWithoutId(String xml) {
        xml = xml.replaceAll("id=\\\"[^\\\"]*\\\"", "");  // Remove id attributes.
        return fromXml(xml);
    }

    private Block fromXml(String xml) {
        try {
            return BlocklyXmlHelper.loadOneBlockFromXml(xml, mBlockFactory);
        } catch (BlockLoadingException e) {
            // Should not happen. Throw as RuntimeException
            throw new IllegalStateException(e);
        }
    }
}
