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

import com.google.blockly.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

/**
 * Tests for {@link Block}.
 */
public class BlockTest extends AndroidTestCase {

    private static final String TEST_JSON_STRING = "{"
            + "  \"id\": \"test_block\","
            + "  \"message0\": \"%1 %2 %3 %4  %5 for each %6 %7 in %8 do %9\","
            + "  \"args0\": ["
            + "    {"
            + "      \"type\": \"field_image\","
            + "      \"src\": \"https://www.gstatic.com/codesite/ph/images/star_on.gif\","
            + "      \"width\": 15,"
            + "      \"height\": 20,"
            + "      \"alt\": \"*\""
            + "    },"
            + "    {"
            + "      \"type\": \"field_variable\","
            + "      \"name\": \"NAME\","
            + "      \"variable\": \"item\""
            + "    },"
            + "    {"
            + "      \"type\": \"field_colour\","
            + "      \"name\": \"NAME\","
            + "      \"colour\": \"#ff0000\""
            + "    },"
            + "    {"
            + "      \"type\": \"field_angle\","
            + "      \"name\": \"NAME\","
            + "      \"angle\": 90"
            + "    },"
            + "    {"
            + "      \"type\": \"field_input\","
            + "      \"name\": \"NAME\","
            + "      \"text\": \"default\""
            + "    },"
            + "    {"
            + "      \"type\": \"field_variable\","
            + "      \"name\": \"NAME\","
            + "      \"variable\": \"item\""
            + "    },"
            + "    {"
            + "      \"type\": \"field_checkbox\","
            + "      \"name\": \"NAME\","
            + "      \"checked\": true"
            + "    },"
            + "    {"
            + "      \"type\": \"input_value\","
            + "      \"name\": \"NAME\","
            + "      \"check\": \"Array\","
            + "      \"align\": \"CENTRE\""
            + "    },"
            + "    {"
            + "      \"type\": \"input_statement\","
            + "      \"name\": \"NAME\""
            + "    }"
            + "  ],"
            + "  \"tooltip\": \"\","
            + "  \"helpUrl\": \"http://www.example.com/\""
            + "}";

    public void testJson() {
        JSONObject blockJson;
        try {
            blockJson = new JSONObject(TEST_JSON_STRING);
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
        assertEquals("BlockFactory failed to load all blocks.", 2, blocks.size());
        Block emptyBlock = bf.obtainBlock("empty_block");
        assertNotNull("Failed to create the empty block.", emptyBlock);
        assertEquals("Empty block has the wrong name", "empty_block", emptyBlock.getName());

        Block frankenblock = bf.obtainBlock("frankenblock");
        assertNotNull("Failed to create the frankenblock.", frankenblock);

        List<Input> inputs = frankenblock.getInputs();
        assertEquals("Frankenblock has the wrong number of inputs", 3, inputs.size());
        assertTrue("First input should be a value input.",
                inputs.get(0) instanceof Input.InputValue);
        assertTrue("Second input should be a statement input.",
                inputs.get(1) instanceof Input.InputStatement);
        assertTrue("Third input should be a dummy input.",
                inputs.get(2) instanceof Input.InputDummy);
    }

    private void assertListsMatch(List<String> expected, List<String> actual) {
        assertEquals("Wrong number of items in the list.", expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals("Item " + i + " does not match.", expected.get(i), actual.get(i));
        }
    }
}
