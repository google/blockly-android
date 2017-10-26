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

import com.google.blockly.android.BlocklyTestCase;
import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.utils.BlockLoadingException;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.google.blockly.utils.MoreAsserts.assertStringNotEmpty;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;


/**
 * Tests for {@link BlockFactory}.
 */
public class BlockFactoryTest extends BlocklyTestCase {
    private XmlPullParserFactory xmlPullParserFactory;
    private BlocklyController mController;
    private BlockFactory mBlockFactory;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

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
    public void testJson() throws JSONException, BlockLoadingException {
        Block block = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().fromJson(BlockTestStrings.TEST_JSON_STRING));

        assertWithMessage("Block was null after initializing from JSON").that(block).isNotNull();
        assertWithMessage("Type not set correctly").that(block.getType()).isEqualTo("test_block");
        assertStringNotEmpty("Block id cannot be empty.", block.getId());
        assertWithMessage("Wrong number of inputs").that(block.getInputs().size()).isEqualTo(2);
        assertWithMessage("Wrong number of fields in first input")
                .that(block.getInputs().get(0).getFields().size()).isEqualTo(9);
    }

    @Test
    public void testLoadBlocks() {
        List<BlockDefinition> definitions = mBlockFactory.getAllBlockDefinitions();
        assertWithMessage("BlockFactory failed to load all blocks.")
                .that(definitions.size()).isEqualTo(26);
    }

    @Test
    public void testSuccessfulLoadFromXml() throws BlockLoadingException {
        Block loaded = parseBlockFromXml(BlockTestStrings.SIMPLE_BLOCK);
        assertThat(loaded.getType()).isEqualTo("frankenblock");

        // PointF.equals(other) did not exist before API 17. Compare components for 16.
        WorkspacePoint position = loaded.getPosition();
        assertThat(position.x).isEqualTo(37f);
        assertThat(position.y).isEqualTo(13f);
    }

    @Test
    public void testBlocksRequireType() throws BlockLoadingException {
        parseBlockFromXmlFailure(BlockTestStrings.NO_BLOCK_TYPE,
            "Block without a type must fail to load.");
    }

    @Test
    public void testIdsGeneratedByBlockFactory() throws BlockLoadingException {
        parseBlockFromXml(BlockTestStrings.NO_BLOCK_ID);
    }

    @Test
    public void testOnlyTopLevelBlocksNeedPosition() throws BlockLoadingException {
        parseBlockFromXml(BlockTestStrings.NO_BLOCK_POSITION);
    }

    @Test
    public void testBlockWithGoodInterior() throws BlockLoadingException {
        parseBlockFromXml(BlockTestStrings.assembleFrankenblock("block", "1",
            BlockTestStrings.VALUE_GOOD));
    }

    @Test
    public void testBlockWithInteriorWithNoChild() throws BlockLoadingException {
        parseBlockFromXml(BlockTestStrings.assembleFrankenblock("block", "3",
            BlockTestStrings.VALUE_NO_CHILD));
    }

    // TODO(fenichel): Value: no input connection
    @Test
    public void testBlockWithNoOutputInteriorFails() throws BlockLoadingException {
        parseBlockFromXmlFailure(BlockTestStrings.assembleFrankenblock("block", "2",
            BlockTestStrings.VALUE_NO_OUTPUT),
            "A <value> child block without an output must fail to load.");
    }

    @Test
    public void testBlockWithBadlyNamedInteriorFails() throws BlockLoadingException {
        parseBlockFromXmlFailure(BlockTestStrings.assembleFrankenblock("block", "4",
            BlockTestStrings.VALUE_BAD_NAME),
            "A block without a recognized type id must fail to load.");
    }

    @Test
    public void testBlockMultipleValuesForSameInputFails() throws BlockLoadingException {
        parseBlockFromXmlFailure(BlockTestStrings.assembleFrankenblock("block", "5",
            BlockTestStrings.VALUE_REPEATED),
            "An input <value> with multiple blocks must fail to load.");
    }

    @Test
    public void testBlockWithGoodComment() {
        runAndSync(new Runnable() {
            @Override
            public void run() {
                try {
                    parseBlockFromXml(BlockTestStrings.assembleFrankenblock("block", "6",
                            BlockTestStrings.COMMENT_GOOD));
                } catch (BlockLoadingException e) {
                    throw new IllegalStateException(e);
                }
            }
        });
    }

    @Test
    public void testBlockWithEmptyComment() {
        runAndSync(new Runnable() {
            @Override
            public void run() {
                try {
                    parseBlockFromXml(BlockTestStrings.assembleFrankenblock("block", "7",
                            BlockTestStrings.COMMENT_NO_TEXT));
                } catch (BlockLoadingException e) {
                    throw new IllegalStateException(e);
                }
            }
        });
    }

    @Test
    public void testBlockWithGoodFields() throws BlockLoadingException {
        Block loaded = parseBlockFromXml(BlockTestStrings.assembleFrankenblock(
                "block", "8", BlockTestStrings.FIELD_HAS_NAME));
        assertThat(((FieldInput) loaded.getFieldByName("text_input")).getText())
                .isEqualTo("item");
    }

    @Test
    public void testCreateBlockWithMissingFieldName() throws BlockLoadingException, IOException {
        // Log warning and ignore field.
        parseBlockFromXml(BlockTestStrings.assembleFrankenblock("block", "9",
            BlockTestStrings.FIELD_MISSING_NAME));

        // TODO(#684): Test for warning "Ignoring unnamed field"
    }

    @Test
    public void testCreateBlockWithUnknownFieldName() throws BlockLoadingException, IOException {
        // Log warning and ignore field.
        parseBlockFromXml(BlockTestStrings.assembleFrankenblock("block", "10",
            BlockTestStrings.FIELD_UNKNOWN_NAME));

        // TODO(#684): Test for warning "Ignoring non-existent field"
    }

    @Test
    public void testCreateBlockWithMissingFieldText() throws BlockLoadingException {
        parseBlockFromXml(BlockTestStrings.assembleFrankenblock("block", "11",
            BlockTestStrings.FIELD_MISSING_TEXT));
    }

    @Test
    public void testBlockWithNoConnectionOnChildBlockFails() throws BlockLoadingException {
        parseBlockFromXmlFailure(BlockTestStrings.assembleFrankenblock("block", "13",
            BlockTestStrings.STATEMENT_BAD_CHILD),
            "A statement <value> child without a previous connection must fail to load.");
    }

    @Test
    public void testCreateBlockWithBadStatmentNameFails() throws BlockLoadingException {
        parseBlockFromXmlFailure(BlockTestStrings.assembleFrankenblock("block", "14",
            BlockTestStrings.STATEMENT_BAD_NAME),
            "A block without a recognized type id must fail to load.");
    }

    @Test
    public void testCreateBlockWithValidStatement() throws BlockLoadingException {
        parseBlockFromXml(BlockTestStrings.assembleFrankenblock("block", "15",
            BlockTestStrings.STATEMENT_GOOD));
    }

    @Test
    public void testCreateBlockWithStatementWithNullChildBlock() throws BlockLoadingException {
        parseBlockFromXml(BlockTestStrings.assembleFrankenblock("block", "12",
                BlockTestStrings.STATEMENT_NO_CHILD));
    }

    @Test
    public void testLoadFromXmlInlineTagAtStart() {
        runAndSync(new Runnable() {
            @Override
            public void run() {
                try {
                    Block inlineAtStart = parseBlockFromXml(BlockTestStrings.SIMPLE_BLOCK_INLINE_BEGINNING);
                    assertThat(inlineAtStart.getInputsInline()).isTrue();
                    assertThat(inlineAtStart.getInputsInlineModified()).isTrue();
                } catch (BlockLoadingException e) {
                    throw new IllegalStateException(e);
                }
            }
        });
    }

    @Test
    public void testLoadFromXmlInlineTagAtEnd() {
        runAndSync(new Runnable() {
            @Override
            public void run() {
                try {
                    Block inlineAtEnd = parseBlockFromXml(BlockTestStrings.SIMPLE_BLOCK_INLINE_END);
                    assertThat(inlineAtEnd.getInputsInline()).isTrue();
                    assertThat(inlineAtEnd.getInputsInlineModified()).isTrue();
                } catch (BlockLoadingException e) {
                    throw new IllegalStateException(e);
                }
            }
        });
    }

    @Test
    public void testLoadFromXmlInlineTagFalse() throws BlockLoadingException {
        Block inlineFalseBlock = parseBlockFromXml(BlockTestStrings.SIMPLE_BLOCK_INLINE_FALSE);
        assertThat(inlineFalseBlock.getInputsInline()).isFalse();
        assertThat(inlineFalseBlock.getInputsInlineModified()).isTrue();
    }

    @Test
    public void testLoadXmlWithNoInlineTag() throws BlockLoadingException {
        Block blockNoInline = parseBlockFromXml(BlockTestStrings.SIMPLE_BLOCK);
        assertThat(blockNoInline.getInputsInline()).isFalse();
        assertThat(blockNoInline.getInputsInlineModified()).isFalse();
    }

    @Test
    public void testLoadFromXmlSimpleShadow() throws BlockLoadingException {
        Block loaded = parseShadowFromXml(BlockTestStrings.SIMPLE_SHADOW);
        assertThat(loaded.getType()).isEqualTo("math_number");
        assertThat(loaded.isShadow()).isTrue();

        // PointF.equals(other) did not exist before API 17. Compare components for API 16.
        WorkspacePoint position = loaded.getPosition();
        assertThat(position.x).isEqualTo(37f);
        assertThat(position.y).isEqualTo(13f);
    }

    @Test
    public void testLoadFromXmlShadowInValue() throws BlockLoadingException {
        Block loaded = parseBlockFromXml(BlockTestStrings.assembleFrankenblock("block", "1",
            BlockTestStrings.VALUE_SHADOW));
        Connection conn = loaded.getInputByName("value_input").getConnection();
        assertThat(conn.getShadowBlock()).isEqualTo(conn.getTargetBlock());
        assertThat(conn.getShadowBlock().isShadow()).isTrue();
    }

    @Test
    public void testLoadFromXmlShadowWithBlockInValue() throws BlockLoadingException {
        Block loaded = parseBlockFromXml(BlockTestStrings.assembleFrankenblock("block", "2",
            BlockTestStrings.VALUE_SHADOW_GOOD));
        Connection conn = loaded.getInputByName("value_input").getConnection();
        assertThat(conn.getTargetBlock().getId()).isEqualTo("VALUE_REAL");
        assertThat(conn.getTargetBlock().isShadow()).isFalse();
        assertThat(conn.getShadowBlock().getId()).isEqualTo("VALUE_SHADOW");
        assertThat(conn.getShadowBlock().isShadow()).isTrue();
    }

    @Test
    public void testLoadFromXmlShadowInStatement() throws BlockLoadingException {
        Block loaded = parseBlockFromXml(BlockTestStrings.assembleFrankenblock("block", "3",
            BlockTestStrings.STATEMENT_SHADOW));
        Connection conn = loaded.getInputByName("NAME").getConnection();
        assertThat(conn.getShadowBlock()).isEqualTo(conn.getTargetBlock());
        assertThat(conn.getShadowBlock().isShadow()).isTrue();
    }

    @Test
    public void testLoadFromXmlShadowWithBlockInStatement() throws BlockLoadingException {
        Block loaded = parseBlockFromXml(BlockTestStrings.assembleFrankenblock("block", "4",
            BlockTestStrings.STATEMENT_SHADOW_GOOD));
        Connection conn = loaded.getInputByName("NAME").getConnection();
        assertThat(conn.getTargetBlock().getId()).isEqualTo("STATEMENT_REAL");
        assertThat(conn.getTargetBlock().isShadow()).isFalse();
        assertThat(conn.getShadowBlock().getId()).isEqualTo("STATEMENT_SHADOW");
        assertThat(conn.getShadowBlock().isShadow()).isTrue();
    }

    @Test
    public void testLoadFromXmlNestedShadow() throws BlockLoadingException {
        Block loaded = parseBlockFromXml(BlockTestStrings.assembleFrankenblock("block", "5",
            BlockTestStrings.VALUE_NESTED_SHADOW));
        Connection conn = loaded.getInputByName("value_input").getConnection();
        assertThat(conn.getShadowBlock()).isEqualTo(conn.getTargetBlock());
        Block shadow1 = conn.getShadowBlock();
        assertThat(shadow1.getId()).isEqualTo("SHADOW1");
        conn = shadow1.getOnlyValueInput().getConnection();
        assertThat(conn.getShadowBlock()).isEqualTo(conn.getTargetBlock());
        assertThat(conn.getShadowBlock().getId()).isEqualTo("SHADOW2");
    }

    @Test
    public void testLoadFromXmlShadowBlockWithNormalChildFails() throws BlockLoadingException {
        parseBlockFromXmlFailure(BlockTestStrings.assembleFrankenblock("block", "6",
            BlockTestStrings.VALUE_SHADOW_BLOCK_WITH_NESTED_NORMAL_BLOCK),
            "A <shadow> containing a normal <block> must fail to load.");
    }

    @Test
    public void testLoadFromXmlBlockWithVariableCannotBeShadowBlock() throws BlockLoadingException {
        parseBlockFromXmlFailure(BlockTestStrings.assembleFrankenblock("block", "7",
                BlockTestStrings.VALUE_SHADOW_VARIABLE),
                "A <shadow> containing a variable field (at any depth) must fail to load.");
    }

    @SuppressWarnings("deprecation") // testing against older API
    @Test
    public void testObtainBlock() {
        Block emptyBlock = mBlockFactory.obtainBlock("empty_block", null);
        assertWithMessage("Failed to create the empty block.").that(emptyBlock).isNotNull();
        assertWithMessage("Empty block has the wrong type")
                .that(emptyBlock.getType()).isEqualTo("empty_block");

        Block frankenblock = mBlockFactory.obtainBlock("frankenblock", null);
        assertWithMessage("Failed to create the frankenblock.").that(frankenblock).isNotNull();

        List<Input> inputs = frankenblock.getInputs();
        assertWithMessage("Frankenblock has the wrong number of inputs")
                .that(inputs.size()).isEqualTo(3);
        assertWithMessage("First input should be a value input.")
                .that(inputs.get(0)).isInstanceOf(Input.InputValue.class);
        assertWithMessage("Second input should be a statement input.")
                .that(inputs.get(1)).isInstanceOf(Input.InputStatement.class);
        assertWithMessage("Third input should be a dummy input.")
                .that(inputs.get(2)).isInstanceOf(Input.InputDummy.class);

        assertThat(frankenblock.getFieldByName("angle")).isNotNull();
    }

    @Test
    public void testObtainBlock_repeatedWithoutUuid() throws BlockLoadingException {
        Block frankenblock = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("frankenblock"));
        assertWithMessage("Failed to create the frankenblock.").that(frankenblock).isNotNull();

        Block frankencopy = mBlockFactory.obtainBlockFrom(
                new BlockTemplate().ofType("frankenblock"));
        assertWithMessage("Obtained blocks should be distinct objects when uuid is null.")
                .that(frankencopy).isNotSameAs(frankenblock);

        assertWithMessage("Obtained blocks should not share connections.")
                .that(frankencopy.getNextConnection())
                .isNotSameAs(frankenblock.getNextConnection());
        assertWithMessage("Obtained blocks should not share connections.")
                .that(frankencopy.getPreviousConnection())
                .isNotSameAs(frankenblock.getPreviousConnection());
        assertThat(frankenblock.getOutputConnection()).isNull();
        assertWithMessage("Obtained blocks should not share inputs.")
                .that(frankencopy.getInputs().get(0)).isNotSameAs(frankenblock.getInputs().get(0));
    }

    @Test
    public void testObtainBlock_repeatedWithId() {
        Block frankenblock = mBlockFactory.obtainBlock("frankenblock", "123");
        assertWithMessage("Failed to create the frankenblock.").that(frankenblock).isNotNull();

        thrown.expect(IllegalArgumentException.class);
        thrown.reportMissingExceptionWithMessage("Cannot create two blocks with the same id");
        mBlockFactory.obtainBlock("frankenblock", "123");
    }

    @Test
    public void testObtainBlock_repeatedWithUuidMismatchingPrototype() {
        Block frankenblock = mBlockFactory.obtainBlock("frankenblock", "123");
        assertWithMessage("Failed to create the frankenblock.").that(frankenblock).isNotNull();

        thrown.expect(IllegalArgumentException.class);
        thrown.reportMissingExceptionWithMessage("Expected error when requesting a block with " +
            "matching UUID but different prototype");
        mBlockFactory.obtainBlock("empty_block", "123");
    }

    private Block parseBlockFromXml(String testString) throws BlockLoadingException {
        XmlPullParser parser = getXmlPullParser(testString, "block");
        Block loaded = mBlockFactory.fromXml(parser);
        assertThat(loaded).isNotNull();
        return loaded;
    }

    private Block parseShadowFromXml(String testString) throws BlockLoadingException {
        XmlPullParser parser = getXmlPullParser(testString, "shadow");
        Block loaded = mBlockFactory.fromXml(parser);
        assertThat(loaded).isNotNull();
        return loaded;
    }

    private void parseBlockFromXmlFailure(String testString, String messageIfDoesNotFail)
            throws BlockLoadingException {
        XmlPullParser parser = getXmlPullParser(testString, "block");

        thrown.expect(BlockLoadingException.class);
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
            throw new IllegalStateException(e);
        }
        return null;
    }
}
