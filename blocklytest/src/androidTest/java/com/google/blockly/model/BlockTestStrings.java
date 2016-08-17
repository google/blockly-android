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

/**
 * Strings for testing loading blocks from JSON and XML.
 */
public class BlockTestStrings {
    public static final WorkspacePoint DEFAULT_POSITION = new WorkspacePoint(37, 13);

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

    public static final String EMPTY_BLOCK_ID = "EMPTY_BLOCK_ID";
    public static final String EMPTY_BLOCK_WITH_POSITION =
            "<block type=\"empty_block\" id=\"" + EMPTY_BLOCK_ID + "\" x=\"37\" y=\"13\" />";
    public static final String EMPTY_BLOCK_INLINE_FALSE =
            "<block type=\"empty_block\" id=\"" + EMPTY_BLOCK_ID + "\" "
                   + "x=\"37\" y=\"13\" inline=\"false\" />";
    public static final String EMPTY_BLOCK_INLINE_TRUE =
            "<block type=\"empty_block\" id=\"" + EMPTY_BLOCK_ID + "\" "
                    + "x=\"37\" y=\"13\" inline=\"true\" />";
    public static final String EMPTY_SHADOW_WITH_POSITION =
            "<shadow type=\"empty_block\" id=\"" + EMPTY_BLOCK_ID + "\" x=\"37\" y=\"13\" />";
    public static final String EMPTY_BLOCK_NO_POSITION =
            "<block type=\"empty_block\" id=\"" + EMPTY_BLOCK_ID + "\" />";

    public static final String BLOCK_END = "</block>";
    public static final String SIMPLE_BLOCK =
            "<block type=\"frankenblock\" id=\"SIMPLE_BLOCK\" x=\"37\" y=\"13\">"
            + "<field name=\"text_input\">item</field>"
            + "</block>";
    public static final String SIMPLE_SHADOW =
            "<shadow type=\"math_number\" id=\"SIMPLE_BLOCK\" x=\"37\" y=\"13\">"
            + "<field name=\"NUM\">42</field>"
            + "</shadow>";
    public static final String SIMPLE_BLOCK_INLINE_END =
            "<block type=\"frankenblock\" id=\"INLINE_END\" x=\"37\" y=\"13\" inline=\"true\">"
                    + "<field name=\"text_input\">item</field>"
                    + "</block>";
    public static final String SIMPLE_BLOCK_INLINE_BEGINNING =
            "<block type=\"frankenblock\" id=\"INLINE_START\" inline=\"true\" x=\"37\" y=\"13\">"
                    + "<field name=\"text_input\">item</field>"
                    + "</block>";
    public static final String SIMPLE_BLOCK_INLINE_FALSE =
            "<block type=\"frankenblock\" id=\"INLINE_FALSE\" x=\"37\" y=\"13\" inline=\"false\">"
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
            "<block type=\"frankenblock\" id=\"NO_BLOCK_POSITION\">"
            + "<field name=\"text_input\">item</field>"
            + "</block>";

    public static final String FIELD_HAS_NAME = "<field name=\"text_input\">item</field>";
    public static final String FIELD_MISSING_NAME = "<field>item</field>";
    public static final String FIELD_UNKNOWN_NAME = "<field name=\"not_a_field\">item</field>";
    public static final String FIELD_MISSING_TEXT = "<field name=\"text_input\"></field>";

    public static final String VALUE_GOOD = "<value name=\"value_input\">" +
            "<block type=\"output_foo\" id=\"VALUE_GOOD\" />" +
            "</value>";
    public static final String VALUE_BAD_NAME = "<value name=\"not_a_name\">" +
            "      <block type=\"output_foo\" id=\"VALUE_BAD_NAME\">" +
            "      </block>" +
            "    </value>";
    public static final String VALUE_NO_CHILD = "<value name=\"value_input\">" +
            "    </value>";
    public static final String VALUE_NO_OUTPUT = "<value name=\"value_input\">" +
            "      <block type=\"no_output\" id=\"VALUE_NO_OUTPUT\">" +
            "      </block>" +
            "    </value>";
    public static final String VALUE_REPEATED = "<value name=\"value_input\">" +
            "      <block type=\"output_foo\" id=\"VALUE_REPEATED1\">" +
            "      </block>" +
            "    </value>" +
            "    <value name=\"value_input\">" +
            "      <block type=\"output_foo\" id=\"VALUE_REPEATED2\">" +
            "      </block>" +
            "    </value>";

    public static final String VALUE_SHADOW = "<value name=\"value_input\">" +
            "<shadow type=\"output_foo\" id=\"VALUE_GOOD\" />" +
            "</value>";
    public static final String VALUE_SHADOW_VARIABLE = "<value name=\"value_input\">" +
            "<shadow type=\"get_variable\" id=\"VALUE_VARIABLE\" />" +
            "</value>";
    public static final String VALUE_SHADOW_GOOD = "<value name=\"value_input\">" +
            "<shadow type=\"output_foo\" id=\"VALUE_SHADOW\" />" +
            "<block type=\"output_foo\" id=\"VALUE_REAL\" />" +
            "</value>";

    public static final String VALUE_NESTED_SHADOW = "<value name=\"value_input\">" +
            "<shadow type=\"simple_input_output\" id=\"SHADOW1\">" +
            "<value name=\"value\">" +
            "<shadow type=\"simple_input_output\" id=\"SHADOW2\" />"  +
            "</value>" +
            "</shadow>" +
            "</value>";

    public static final String VALUE_NESTED_SHADOW_BLOCK = "<value name=\"value_input\">" +
            "      <shadow type=\"simple_input_output\" id=\"SHADOW1\">" +
            "        <value name=\"value\">" +
            "          <shadow type=\"simple_input_output\" id=\"SHADOW2\"/>"  +
            "          <block type=\"simple_input_output\" id=\"BLOCK_INNER\"/>"  +
            "        </value>" +
            "      </shadow>" +
            "    </value>";

    public static final String STATEMENT_GOOD = "<statement name=\"NAME\">" +
            "<block type=\"frankenblock\" id=\"STATEMENT_GOOD\">" +
            "</block>" +
            "</statement>";
    public static final String STATEMENT_NO_CHILD = "<statement name=\"NAME\">" +
            "    </statement>";
    public static final String STATEMENT_BAD_NAME = "<statement name=\"not_a_name\">" +
            "      <block type=\"frankenblock\" id=\"STATEMENT_BAD_NAME\">" +
            "      </block>" +
            "    </statement>";
    public static final String STATEMENT_BAD_CHILD = "<statement name=\"NAME\">" +
            "      <block type=\"no_output\" id=\"STATEMENT_BAD_CHILD\">" +
            "      </block>" +
            "    </statement>";
    public static final String STATEMENT_SHADOW = "<statement name=\"NAME\">" +
            "<shadow type=\"statement_value_input\" id=\"STATEMENT_SHADOW\">" +
            "</shadow>" +
            "</statement>";
    public static final String STATEMENT_SHADOW_GOOD = "<statement name=\"NAME\">" +
            "<shadow type=\"statement_value_input\" id=\"STATEMENT_SHADOW\">" +
            "</shadow>" +
            "<block type=\"frankenblock\" id=\"STATEMENT_REAL\">" +
            "</block>" +
            "</statement>";

    public static final String COMMENT_GOOD = "<comment pinned=\"true\" h=\"80\" w=\"160\">" +
            "    Calm</comment>";
    public static final String COMMENT_NO_TEXT= "<comment pinned=\"true\" h=\"80\" w=\"160\">" +
            "    </comment>";

    public static final String FRANKENBLOCK_DEFAULT_VALUES_START =
            "<field name=\"text_input\">something</field>"
            + "<field name=\"checkbox\">true</field>";
    public static final String FRANKENBLOCK_DEFAULT_VALUES_END =
            "<field name=\"dropdown\">OPTIONNAME1</field>"
            + "<field name=\"variable\">item</field>"
            + "<field name=\"angle\">90</field>"
            + "<field name=\"colour\">#ff0000</field>";
    public static final String FRANKENBLOCK_DEFAULT_VALUES = FRANKENBLOCK_DEFAULT_VALUES_START
            + FRANKENBLOCK_DEFAULT_VALUES_END;

    public static String blockStart(String tag, String type, String id, WorkspacePoint position) {
        StringBuilder sb = new StringBuilder("<" + tag + " type=\"" + type + '"');
        if (id != null) {
            sb.append(" id=\"" + id + '\"');
        }
        if (position != null) {
            sb.append(" x=\"" + position.x + "\" y=\"" + position.y + '\"');
        }
        sb.append('>');
        return sb.toString();
    }

    public static String frankenBlockStart(String tag, String id) {
        return blockStart(tag, "frankenblock", id, DEFAULT_POSITION);
    }

    public static String assembleFrankenblock(String tag, String id, String interior) {
        return frankenBlockStart(tag, id) + interior + "</" + tag + ">";
    }
}
