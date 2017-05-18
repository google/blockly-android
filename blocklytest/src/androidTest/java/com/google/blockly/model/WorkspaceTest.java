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

import com.google.blockly.android.BlocklyTestCase;
import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.utils.BlockLoadingException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Tests for {@link Workspace}.
 */
public class WorkspaceTest extends BlocklyTestCase {

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
        configureForUIThread();
        Context context = InstrumentationRegistry.getContext();
        BlocklyController.Builder builder = new BlocklyController.Builder(context);
        builder.addBlockDefinitionsFromAsset("default/test_blocks.json");
        BlocklyController controller = builder.build();
        mWorkspace = controller.getWorkspace();
    }

    @Test
    public void testSimpleXmlParsing() throws BlockLoadingException {
        mWorkspace.loadWorkspaceContents(assembleWorkspace(BlockTestStrings.SIMPLE_BLOCK));
        assertWithMessage("Workspace should have one block")
                .that(mWorkspace.getRootBlocks()).hasSize(1);
    }

    @Test
    public void testEmptyXmlParsing() throws BlockLoadingException {
        // Normal end tag.
        mWorkspace.loadWorkspaceContents(assembleWorkspace(""));
        assertWithMessage("Workspace should be empty").that(mWorkspace.getRootBlocks()).hasSize(0);

        // Abbreviated end tag.
        mWorkspace.loadWorkspaceContents(new ByteArrayInputStream(EMPTY_WORKSPACE.getBytes()));
        assertWithMessage("Workspace should be empty").that(mWorkspace.getRootBlocks()).hasSize(0);
    }

    @Test
    public void testBadXmlParsing() throws BlockLoadingException {
        thrown.expect(BlockLoadingException.class);
        thrown.reportMissingExceptionWithMessage("Bad workspace XML will will fail to load.");
        mWorkspace.loadWorkspaceContents(assembleWorkspace(BAD_XML));
    }

    @Test
    public void testSerialization() throws BlocklySerializerException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        mWorkspace.serializeToXml(os);
        assertThat(os.toString()).isEqualTo(EMPTY_WORKSPACE);
    }

    private static ByteArrayInputStream assembleWorkspace(String interior) {
        return new ByteArrayInputStream(
                (WORKSPACE_XML_START + interior + WORKSPACE_XML_END).getBytes());
    }
}
