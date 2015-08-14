package com.google.blockly.model;

import android.test.AndroidTestCase;
import android.test.mock.MockContext;

import com.google.blockly.R;

import java.io.ByteArrayInputStream;

/**
 * Tests for {@link Workspace}.
 */
public class WorkspaceTest extends AndroidTestCase {

    public static final String WORKSPACE_XML_START =
            "<xml xmlns=\"http://www.w3.org/1999/xhtml\">";

    public static final String WORKSPACE_XML_END = "</xml>";

    private static ByteArrayInputStream assembleWorkspace(String interior) {
        return new ByteArrayInputStream(
                (WORKSPACE_XML_START + interior + WORKSPACE_XML_END).getBytes());
    }
    public void testXmlParsing() {
        // TODO: Move test_blocks.json to the testapp's resources once
        // https://code.google.com/p/android/issues/detail?id=64887 is fixed.
        BlockFactory bf = new BlockFactory(getContext(), new int[] {R.raw.test_blocks});
        Workspace workspace = new Workspace(new MockContext());
        workspace.loadFromXml(assembleWorkspace(""), bf);
        workspace.loadFromXml(assembleWorkspace(BlockTestStrings.SIMPLE_BLOCK), bf);
    }
}
