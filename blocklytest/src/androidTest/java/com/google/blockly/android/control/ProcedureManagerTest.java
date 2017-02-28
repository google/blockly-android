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

package com.google.blockly.android.control;

import com.google.blockly.android.TestUtils;
import com.google.blockly.model.Block;
import com.google.blockly.model.BlockFactory;
import com.google.blockly.model.BlockTemplate;
import com.google.blockly.model.FieldInput;
import com.google.blockly.utils.BlockLoadingException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests for {@link ProcedureManager}.
 */
public class ProcedureManagerTest {
    private static final String PROCEDURE_NAME = "procedure name";

    private BlockFactory mFactory;
    private ProcedureManager mProcedureManager;
    private Block mProcedureDefinition;
    private Block mProcedureReference;

    @Before
    public void setUp() throws Exception {
        mFactory = new BlockFactory();
        mProcedureManager = new ProcedureManager();

        mProcedureDefinition = mFactory.obtainBlockFrom(
                new BlockTemplate().fromDefinition(
                        TestUtils.getProcedureDefinitionBlockDefinition(PROCEDURE_NAME)));
        mProcedureReference = mFactory.obtainBlockFrom(
                new BlockTemplate().fromDefinition(
                        TestUtils.getProcedureReferenceBlockDefinition(PROCEDURE_NAME)));
        assertThat(mProcedureDefinition).isNotNull();
        assertThat(mProcedureReference).isNotNull();
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testAddProcedureDefinition() {
        mProcedureManager.addDefinition(mProcedureDefinition);
        assertThat(mProcedureManager.containsDefinition(mProcedureDefinition)).isTrue();

        List<Block> references = mProcedureManager.getReferences(PROCEDURE_NAME);
        assertThat(references).isNotNull();
        assertThat(references.size()).isEqualTo(0);
    }

    @Test
    public void testAddProcedureDefinitionTwice() throws BlockLoadingException {
        mProcedureManager.addDefinition(mProcedureDefinition);

        thrown.expect(IllegalStateException.class);
        mProcedureManager.addDefinition(mProcedureDefinition);

        // Adding two block definitions with the same name should change the name of the new
        // block.
        Block secondProcedureDefinition =
                mFactory.obtainBlockFrom(new BlockTemplate().copyOf(mProcedureDefinition));

        mProcedureManager.addDefinition(secondProcedureDefinition);
        assertThat(PROCEDURE_NAME.equalsIgnoreCase(
                ((FieldInput) secondProcedureDefinition.getFieldByName("name"))
                        .getText())).isFalse();
    }

    @Test
    public void testAddProcedureReference() {
        mProcedureManager.addDefinition(mProcedureDefinition);

        mProcedureManager.addReference(mProcedureReference);
        assertThat(mProcedureManager.hasReferences(mProcedureDefinition)).isTrue();
    }

    @Test
    // Remove definition should also remove all references.
    public void testRemoveProcedureDefinition() {
        mProcedureManager.addDefinition(mProcedureDefinition);
        assertThat(mProcedureManager.containsDefinition(mProcedureDefinition)).isTrue();

        mProcedureManager.removeDefinition(mProcedureDefinition);
        assertThat(mProcedureManager.containsDefinition(mProcedureDefinition)).isFalse();
        assertThat(mProcedureManager.hasReferences(mProcedureDefinition)).isFalse();

        mProcedureManager.addDefinition(mProcedureDefinition);
        mProcedureManager.addReference(mProcedureReference);
        List<Block> references =
                mProcedureManager.removeDefinition(mProcedureDefinition);
        assertThat(references).isNotNull();
        assertThat(references.size()).isEqualTo(1);
        assertThat(references.get(0)).isEqualTo(mProcedureReference);

        assertThat(mProcedureManager.containsDefinition(mProcedureDefinition)).isFalse();
        assertThat(mProcedureManager.hasReferences(mProcedureDefinition)).isFalse();
    }

    @Test
    public void testRemoveProcedureReference() {
        mProcedureManager.addDefinition(mProcedureDefinition);
        mProcedureManager.addReference(mProcedureReference);

        mProcedureManager.removeReference(mProcedureReference);
        assertThat(mProcedureManager.hasReferences(mProcedureReference)).isFalse();
    }

    @Test
    public void testMissingNames() throws BlockLoadingException {
        mProcedureDefinition = mFactory.obtainBlockFrom(new BlockTemplate().fromJson(
                "{\"type\":\"no field named name\"}"));

        thrown.expect(IllegalArgumentException.class);
        mProcedureManager.addDefinition(mProcedureDefinition);
    }

    @Test
    public void testAddReferenceToUndefined() {
        thrown.expect(IllegalStateException.class);
        mProcedureManager.addReference(mProcedureReference);
    }

    @Test
    public void testRemoveNoUndefined() {
        thrown.expect(IllegalStateException.class);
        mProcedureManager.removeDefinition(mProcedureDefinition);
    }

    @Test
    public void testNoReference() {
        thrown.expect(IllegalStateException.class);
        mProcedureManager.removeReference(mProcedureReference);
    }
}
