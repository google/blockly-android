package com.google.blockly.model;

import android.test.AndroidTestCase;

import com.google.blockly.R;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Tests for {@link Workspace}.
 */
public class WorkspaceTest extends AndroidTestCase {

    public static final String WORKSPACE_XML_START =
            "<xml xmlns=\"http://www.w3.org/1999/xhtml\">";

    public static final String WORKSPACE_XML_END = "</xml>";

    public static final String BAD_XML = "<type=\"xml_no_name\">";

    public static final String EMPTY_WORKSPACE =
            "<xml xmlns=\"http://www.w3.org/1999/xhtml\" />";

    private static ByteArrayInputStream assembleWorkspace(String interior) {
        return new ByteArrayInputStream(
                (WORKSPACE_XML_START + interior + WORKSPACE_XML_END).getBytes());
    }

    public void testXmlParsing() {
        // TODO: Move test_blocks.json to the testapp's resources once
        // https://code.google.com/p/android/issues/detail?id=64887 is fixed.
        BlockFactory bf = new BlockFactory(getContext(), new int[] {R.raw.test_blocks});
        Workspace workspace = new Workspace();
        workspace.loadFromXml(assembleWorkspace(""), bf);
        workspace.loadFromXml(assembleWorkspace(BlockTestStrings.SIMPLE_BLOCK), bf);
        workspace.loadFromXml(new ByteArrayInputStream(EMPTY_WORKSPACE.getBytes()), bf);

        try {
            workspace.loadFromXml(assembleWorkspace(BAD_XML), bf);
            fail("Should have thrown a BlocklyParseException.");
        } catch (BlocklyParserException expected) {
            // expected
        }
    }

    public void testSerialization() throws BlocklySerializerException {
        Workspace workspace = new Workspace();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        workspace.serializeToXml(os);
        assertEquals(EMPTY_WORKSPACE, os.toString());
    }
}
