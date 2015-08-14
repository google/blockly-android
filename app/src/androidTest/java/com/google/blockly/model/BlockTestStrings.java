package com.google.blockly.model;

/**
 * Strings for testing loading blocks from JSON and XML.
 */
public class BlockTestStrings {
    public static final String TEST_JSON_STRING = "{"
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

    public static final String BLOCK_START =  "<block type=\"frankenblock\" id=\"364\" x=\"37\" y=\"13\">";
    public static final String BLOCK_END = "</block>";
    public static final String SIMPLE_BLOCK =
            "<block type=\"frankenblock\" id=\"364\" x=\"37\" y=\"13\">"
            + "<field name=\"text_input\">item</field>"
            + "</block>";

    public static final String NO_BLOCK_TYPE =
            "<block id=\"364\" x=\"37\" y=\"13\">"
            + "<field name=\"text_input\">item</field>"
            + "</block>";

    public static final String NO_BLOCK_ID =
            "<block type=\"frankenblock\" x=\"37\" y=\"13\">"
            + "<field name=\"text_input\">item</field>"
            + "</block>";

    public static final String NO_BLOCK_POSITION =
            "<block type=\"frankenblock\" id=\"364\">"
            + "<field name=\"text_input\">item</field>"
            + "</block>";

    public static final String FIELD_HAS_NAME = "<field name=\"text_input\">item</field>";
    public static final String FIELD_MISSING_NAME = "<field>item</field>";
    public static final String FIELD_UNKNOWN_NAME = "<field name=\"not_a_field\">item</field>";
    public static final String FIELD_MISSING_TEXT = "<field name=\"text_input\"></field>";

    public static final String VALUE_GOOD = "<value name=\"value_input\">" +
            "      <block type=\"output_foo\" id=\"126\">" +
            "      </block>" +
            "    </value>";
    public static final String VALUE_BAD_NAME = "<value name=\"not_a_name\">" +
            "      <block type=\"output_foo\" id=\"126\">" +
            "      </block>" +
            "    </value>";
    public static final String VALUE_NO_CHILD = "<value name=\"value_input\">" +
            "    </value>";
    public static final String VALUE_NO_OUTPUT = "<value name=\"value_input\">" +
            "      <block type=\"no_output\" id=\"126\">" +
            "      </block>" +
            "    </value>";
    public static final String VALUE_REPEATED = "<value name=\"value_input\">" +
            "      <block type=\"output_foo\" id=\"126\">" +
            "      </block>" +
            "    </value>" +
            "    <value name=\"value_input\">" +
            "      <block type=\"output_foo\" id=\"126\">" +
            "      </block>" +
            "    </value>";

    public static final String STATEMENT_GOOD = "<statement name=\"NAME\">" +
            "      <block type=\"frankenblock\" id=\"3\">" +
            "      </block>" +
            "    </statement>";
    public static final String STATEMENT_NO_CHILD = "<statement name=\"NAME\">" +
            "    </statement>";
    public static final String STATEMENT_BAD_NAME = "<statement name=\"not_a_name\">" +
            "      <block type=\"frankenblock\" id=\"3\">" +
            "      </block>" +
            "    </statement>";
    public static final String STATEMENT_BAD_CHILD = "<statement name=\"NAME\">" +
            "      <block type=\"no_output\" id=\"3\">" +
            "      </block>" +
            "    </statement>";

    public static final String COMMENT_GOOD = "<comment pinned=\"true\" h=\"80\" w=\"160\">" +
            "    Calm</comment>";
    public static final String COMMENT_NO_TEXT= "<comment pinned=\"true\" h=\"80\" w=\"160\">" +
            "    </comment>";

    public static final String DEEPLY_NESTED_BLOCKS = "<xml xmlns=\"http://www.w3.org/1999/xhtml\">"
            + "<block type=\"variables_set\" id=\"1231\" x=\"163\" y=\"37\">"
            + "<field name=\"VAR\">item</field>"
            + "<value name=\"VALUE\">"
            + "<block type=\"text\" id=\"1385\">"
            + "<field name=\"TEXT\">blockly</field>"
            + "</block>"
            + "</value>"
            + "<next>"
            + "<block type=\"controls_if\" id=\"1079\">"
            + "<value name=\"IF0\">"
            + "<block type=\"logic_compare\" id=\"1080\">"
            + "<field name=\"OP\">EQ</field>"
            + "<value name=\"A\">"
            + "<block type=\"variables_get\" id=\"1081\">"
            + "<field name=\"VAR\">item</field>"
            + "</block>"
            + "</value>"
            + "<value name=\"B\">"
            + "<block type=\"math_constant\" id=\"1082\">"
            + "<field name=\"CONSTANT\">PI</field>"
            + "</block>"
            + "</value>"
            + "</block>"
            + "</value>"
            + "<statement name=\"DO0\">"
            + "<block type=\"variables_set\" id=\"1083\">"
            + "<field name=\"VAR\">item</field>"
            + "<value name=\"VALUE\">"
            + "<block type=\"colour_blend\" id=\"1084\">"
            + "<value name=\"COLOUR1\">"
            + "<block type=\"colour_picker\" id=\"1085\">"
            + "<field name=\"COLOUR\">#ff0000</field>"
            + "</block>"
            + "</value>"
            + "<value name=\"COLOUR2\">"
            + "<block type=\"colour_picker\" id=\"1086\">"
            + "<field name=\"COLOUR\">#3333ff</field>"
            + "</block>"
            + "</value>"
            + "<value name=\"RATIO\">"
            + "<block type=\"math_number\" id=\"1087\">"
            + "<field name=\"NUM\">0.5</field>"
            + "</block>"
            + "</value>"
            + "</block>"
            + "</value>"
            + "<next>"
            + "<block type=\"controls_flow_statements\" id=\"1169\">"
            + "<field name=\"FLOW\">BREAK</field>"
            + "</block>"
            + "</next>"
            + "</block>"
            + "</statement>"
            + "<next>"
            + "<block type=\"text_append\" id=\"1206\">"
            + "<field name=\"VAR\">item</field>"
            + "<value name=\"TEXT\">"
            + "<block type=\"text\" id=\"1207\">"
            + "<field name=\"TEXT\"></field>"
            + "</block>"
            + "</value>"
            + "<next>"
            + "<block type=\"text_print\" id=\"1403\">"
            + "<value name=\"TEXT\">"
            + "<block type=\"variables_get\" id=\"1461\">"
            + "<field name=\"VAR\">item</field>"
            + "</block>"
            + "</value>"
            + "</block>"
            + "</next>"
            + "</block>"
            + "</next>"
            + "</block>"
            + "</next>"
            + "</block>"
            + "</xml>";

    public static String assembleBlock(String interior) {
        return BLOCK_START + interior + BLOCK_END;
    }
}
