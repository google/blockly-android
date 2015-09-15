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

package com.google.blockly.control;

import android.test.AndroidTestCase;

import com.google.blockly.model.Block;
import com.google.blockly.model.Field;
import com.google.blockly.model.Input;

import java.util.List;

/**
 * Tests for {@link ProcedureManager}
 */
public class ProcedureManagerTest extends AndroidTestCase {
    private ProcedureManager procedureManager;
    private final String PROCEDURE_NAME = "procedure name";
    private final Field nameField = new Field.FieldInput("name", PROCEDURE_NAME);

    private Block procedureDefinition;
    private Block procedureReference;

    @Override
    public void setUp() throws Exception {
        procedureManager = new ProcedureManager();

        Input nameInput = new Input.InputDummy("dummyName", Input.ALIGN_CENTER);
        nameInput.add(nameField);
        procedureDefinition = new Block.Builder(
                ProcedureManager.PROCEDURE_DEFINITION_PREFIX + "test", "testid")
                .addInput(nameInput)
                .build();
        procedureReference = new Block.Builder(
                ProcedureManager.PROCEDURE_REFERENCE_PREFIX + "test", "testid")
                .addInput(nameInput)
                .build();
    }

    public void testAddProcedureDefinition() {
        procedureManager.addDefinition(procedureDefinition);
        assertTrue(procedureManager.containsDefinition(procedureDefinition));
        assertNotNull(procedureManager.getReferences(PROCEDURE_NAME));
        assertEquals(0, procedureManager.getReferences(PROCEDURE_NAME).size());
    }

    public void testAddProcedureDefinitionTwice() {
        procedureManager.addDefinition(procedureDefinition);
        procedureManager.addDefinition(procedureDefinition);

        assertFalse(PROCEDURE_NAME.equalsIgnoreCase(
                ((Field.FieldInput) procedureDefinition.getFieldByName("name")).getText()));
    }

    public void testAddProcedureReference() {
        procedureManager.addDefinition(procedureDefinition);

        procedureManager.addReference(procedureReference);
        assertTrue(procedureManager.containsReference(procedureDefinition));
    }

    // Remove definition should also remove all references.
    public void testRemoveProcedureDefinition() {
        procedureManager.addDefinition(procedureDefinition);
        assertTrue(procedureManager.containsDefinition(procedureDefinition));

        procedureManager.removeDefinition(procedureDefinition);
        assertFalse(procedureManager.containsDefinition(procedureDefinition));
        assertFalse(procedureManager.containsReference(procedureDefinition));


        procedureManager.addDefinition(procedureDefinition);
        procedureManager.addReference(procedureReference);
        List<Block> references =
                procedureManager.removeDefinition(procedureDefinition);
        assertNotNull(references);
        assertEquals(1, references.size());
        assertEquals(procedureReference, references.get(0));

        assertFalse(procedureManager.containsDefinition(procedureDefinition));
        assertFalse(procedureManager.containsReference(procedureDefinition));
    }

    public void testRemoveProcedureReference() {
        procedureManager.addDefinition(procedureDefinition);
        procedureManager.addReference(procedureReference);

        procedureManager.removeReference(procedureReference);
    }

    public void testMissingNames() {
        procedureDefinition = new Block.Builder(
                ProcedureManager.PROCEDURE_DEFINITION_PREFIX + "test", "testid")
                .build();
        procedureReference = new Block.Builder(
                ProcedureManager.PROCEDURE_REFERENCE_PREFIX + "test", "testid")
                .build();

        try {
            procedureManager.addDefinition(procedureDefinition);
            fail("Expected an exception when defining a procedure with no name field.");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    public void testNoDefinition() {
        try {
            procedureManager.addReference(procedureReference);
            fail("Expected an exception when referencing a procedure with no definition.");
        } catch (IllegalStateException expected) {
            // expected
        }

        try {
            procedureManager.removeDefinition(procedureDefinition);
            fail("Expected an exception when removing a block with no definition");
        } catch (IllegalStateException expected) {
            // expected
        }

    }

    public void testNoReference() {
        try {
            procedureManager.removeReference(procedureReference);
            fail("Expected an exception when removing a nonexistent procedure reference");
        } catch (IllegalStateException expected) {
            // expected
        }
    }
}
