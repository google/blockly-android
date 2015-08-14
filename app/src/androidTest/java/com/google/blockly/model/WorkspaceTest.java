package com.google.blockly.model;

import android.test.AndroidTestCase;
import android.test.mock.MockContext;

import java.io.ByteArrayInputStream;

/**
 * Tests for {@link Workspace}.
 */
public class WorkspaceTest extends AndroidTestCase {

    public static final String EMPTY_WORKSPACE =
            "<xml xmlns=\"http://www.w3.org/1999/xhtml\"></xml>";

    public static final String SIMPLE_XML_STRING = "<xml xmlns=\"http://www.w3.org/1999/xhtml\">"
            + "<block type=\"variables_get\" id=\"364\" x=\"37\" y=\"13\">"
            + "<field name=\"VAR\">item</field>"
            + "</block>"
            + "</xml>";

    public static final String MANY_TOP_LEVEL_BLOCKS_STRING =
    "<xml xmlns=\"http://www.w3.org/1999/xhtml\">"
    + "<block type=\"text_join\" id=\"656\" x=\"13\" y=\"37\">"
    + "<mutation items=\"2\"></mutation>"
    + "</block>"
    + "<block type=\"math_arithmetic\" id=\"605\" x=\"263\" y=\"62\">"
    + "<field name=\"OP\">ADD</field>"
    + "</block>"
    + "<block type=\"math_arithmetic\" id=\"586\" x=\"413\" y=\"112\">"
    + "<field name=\"OP\">ADD</field>"
    + "</block>"
    + "<block type=\"colour_random\" id=\"550\" x=\"388\" y=\"162\"></block>"
    + "<block type=\"lists_create_with\" id=\"567\" x=\"188\" y=\"187\">"
    + "<mutation items=\"3\"></mutation>"
    + "</block>"
    + "<block type=\"procedures_ifreturn\" id=\"713\" x=\"388\" y=\"237\">"
    + "<mutation value=\"1\"></mutation>"
    + "</block>"
    + "<block type=\"colour_picker\" id=\"684\" x=\"338\" y=\"312\">"
    + "<field name=\"COLOUR\">#ff0000</field>"
    + "</block>"
    + "<block type=\"lists_create_with\" id=\"673\" x=\"113\" y=\"337\">"
    + "<mutation items=\"3\"></mutation>"
    + "</block>"
    + "<block type=\"math_arithmetic\" id=\"638\" x=\"613\" y=\"312\">"
    + "<field name=\"OP\">ADD</field>"
    + "</block>"
    + "<block type=\"lists_create_empty\" id=\"701\" x=\"413\" y=\"412\"></block>"
    + "<block type=\"logic_operation\" id=\"709\" x=\"63\" y=\"462\">"
    + "<field name=\"OP\">AND</field>"
    + "</block>"
    + "<block type=\"colour_rgb\" id=\"616\" x=\"213\" y=\"487\">"
    + "<value name=\"RED\">"
    + "<block type=\"math_number\" id=\"617\">"
    + "<field name=\"NUM\">100</field>"
    + "</block>"
    + "</value>"
    + "<value name=\"GREEN\">"
    + "<block type=\"math_number\" id=\"618\">"
    + "<field name=\"NUM\">50</field>"
    + "</block>"
    + "</value>"
    + "<value name=\"BLUE\">"
    + "<block type=\"math_number\" id=\"619\">"
    + "<field name=\"NUM\">0</field>"
    + "</block>"
    + "</value>"
    + "</block>"
    + "</xml>";

    public void testXmlParsing() {
        Workspace workspace = new Workspace(new MockContext());
        workspace.loadFromXml(new ByteArrayInputStream(EMPTY_WORKSPACE.getBytes()));
        workspace.loadFromXml(new ByteArrayInputStream(SIMPLE_XML_STRING.getBytes()));
        workspace.loadFromXml(new ByteArrayInputStream(MANY_TOP_LEVEL_BLOCKS_STRING.getBytes()));
    }
}
