package com.google.blockly.model;

import android.test.AndroidTestCase;

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

    private void assertListsMatch(List<String> expected, List<String> actual) {
        assertEquals("Wrong number of items in the list.", expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals("Item " + i + " does not match.", expected.get(i), actual.get(i));
        }
    }
}
