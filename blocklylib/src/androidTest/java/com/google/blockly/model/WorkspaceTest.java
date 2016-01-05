package com.google.blockly.model;

import android.test.AndroidTestCase;

import com.google.blockly.R;
import com.google.blockly.control.BlocklyController;

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
    private Workspace mWorkspace;

    @Override
    protected void setUp() throws Exception {
        // TODO: Move test_blocks.json to the testapp's resources once
        // https://code.google.com/p/android/issues/detail?id=64887 is fixed.
        BlocklyController.Builder builder = new BlocklyController.Builder(getContext());
        builder.addBlockDefinitions(R.raw.test_blocks);
        BlocklyController controller = builder.build();
        mWorkspace = controller.getWorkspace();
    }

    public void testSimpleXmlParsing() {
        mWorkspace.loadWorkspaceContents(assembleWorkspace(BlockTestStrings.SIMPLE_BLOCK));
        assertEquals("Workspace should have one block", 1, mWorkspace.getRootBlocks().size());
    }

    public void testEmptyXmlParsing() {
        // Normal end tag.
        mWorkspace.loadWorkspaceContents(assembleWorkspace(""));
        assertEquals("Workspace should be empty", 0, mWorkspace.getRootBlocks().size());

        // Abbreviated end tag.
        mWorkspace.loadWorkspaceContents(new ByteArrayInputStream(EMPTY_WORKSPACE.getBytes()));
        assertEquals("Workspace should be empty", 0, mWorkspace.getRootBlocks().size());
    }

    public void testBadXmlParsing() {
        try {
            mWorkspace.loadWorkspaceContents(assembleWorkspace(BAD_XML));
            fail("Should have thrown a BlocklyParseException.");
        } catch (BlocklyParserException expected) {
            // expected
        }
    }

    public void testSerialization() throws BlocklySerializerException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        mWorkspace.serializeToXml(os);
        assertEquals(EMPTY_WORKSPACE, os.toString());
    }

    private static ByteArrayInputStream assembleWorkspace(String interior) {
        return new ByteArrayInputStream(
                (WORKSPACE_XML_START + interior + WORKSPACE_XML_END).getBytes());
    }
}
