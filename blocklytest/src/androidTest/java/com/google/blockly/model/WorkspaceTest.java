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

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import com.google.blockly.android.R;
import com.google.blockly.android.control.BlocklyController;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link Workspace}.
 */
public class WorkspaceTest {

    private static final String WORKSPACE_XML_START =
            "<xml xmlns=\"http://www.w3.org/1999/xhtml\">";

    private static final String WORKSPACE_XML_END = "</xml>";

    private static final String BAD_XML = "<type=\"xml_no_name\">";

    private static final String EMPTY_WORKSPACE =
            "\r\n<xml xmlns=\"http://www.w3.org/1999/xhtml\" />";
    private Workspace mWorkspace;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        Context context = InstrumentationRegistry.getContext();
        // TODO(#84): Move test_blocks.json to the test app's resources.
        BlocklyController.Builder builder = new BlocklyController.Builder(context);
        builder.addBlockDefinitions(R.raw.test_blocks);
        BlocklyController controller = builder.build();
        mWorkspace = controller.getWorkspace();
    }

    @Test
    public void testSimpleXmlParsing() {
        mWorkspace.loadWorkspaceContents(assembleWorkspace(BlockTestStrings.SIMPLE_BLOCK));
        assertEquals("Workspace should have one block", 1, mWorkspace.getRootBlocks().size());
    }

    @Test
    public void testEmptyXmlParsing() {
        // Normal end tag.
        mWorkspace.loadWorkspaceContents(assembleWorkspace(""));
        assertEquals("Workspace should be empty", 0, mWorkspace.getRootBlocks().size());

        // Abbreviated end tag.
        mWorkspace.loadWorkspaceContents(new ByteArrayInputStream(EMPTY_WORKSPACE.getBytes()));
        assertEquals("Workspace should be empty", 0, mWorkspace.getRootBlocks().size());
    }

    @Test
    public void testBadXmlParsing() {
        thrown.expect(BlocklyParserException.class);
        mWorkspace.loadWorkspaceContents(assembleWorkspace(BAD_XML));
    }

    @Test
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
