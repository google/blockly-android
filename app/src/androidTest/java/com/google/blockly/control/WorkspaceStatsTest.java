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
import com.google.blockly.model.Connection;
import com.google.blockly.model.Field;
import com.google.blockly.model.Input;
import com.google.blockly.model.NameManager;

/**
 * Tests for {@link WorkspaceStats}
 */
public class WorkspaceStatsTest extends AndroidTestCase {
    private WorkspaceStats stats;
    private Input fieldInput;
    private Input variableFieldsInput;

    @Override
    public void setUp() throws Exception {
         stats = new WorkspaceStats(
                 new NameManager.VariableNameManager(), new NameManager.ProcedureNameManager());

        fieldInput = new Input.InputDummy("name input", Input.ALIGN_LEFT);
        Field field = new Field.FieldInput("name", "nameid");
        field.setFromXmlText("new procedure");
        fieldInput.add(field);

        variableFieldsInput = new Input.InputDummy("name input", Input.ALIGN_LEFT);
        field = new Field.FieldVariable("field name", "nameid");
        field.setFromXmlText("variable name");
        variableFieldsInput.add(field);
        field = new Field.FieldVariable("field name 2", "nameid2");
        field.setFromXmlText("variable name");
        variableFieldsInput.add(field);
    }

    public void testCollectProcedureStats() {
        Block.Builder blockBuilder = new Block.Builder(
                WorkspaceStats.PROCEDURE_DEFINITION_PREFIX + "test", "testid");
        try {
            stats.collectStats(blockBuilder.build(), false);
            fail("Expected an exception when defining a procedure with no name field.");
        } catch (IllegalStateException expected) {
            // expected
        }

        assertFalse(stats.getProcedureNameManager().contains(
                WorkspaceStats.PROCEDURE_DEFINITION_PREFIX + "test"));
        stats.clear();
        blockBuilder.addInput(fieldInput);
        stats.collectStats(blockBuilder.build(), false);

        assertTrue(stats.getProcedureNameManager().contains("new procedure"));
        assertFalse(stats.getProcedureNameManager().contains(
                WorkspaceStats.PROCEDURE_DEFINITION_PREFIX + "test"));

        // Add another block referring to the last one.
        blockBuilder = new Block.Builder(WorkspaceStats.PROCEDURE_REFERENCE_PREFIX + "test", "ref");
        Block procedureReference = blockBuilder.build();
        stats.collectStats(procedureReference, false);
        assertEquals(1, stats.getProcedureReferences().size());

        // TODO(fenichel): Make mutations work so that this works.

        //assertEquals(procedureReference,
        //        stats.getProcedureReferences().get("new procedure").get(0));
    }

    public void testCollectVariableStats() {
        Block.Builder blockBuilder = new Block.Builder("test", "testid");

        blockBuilder.addInput(variableFieldsInput);
        Block variableReference = blockBuilder.build();
        stats.collectStats(variableReference, false);

        assertTrue(stats.getVariableNameManager().contains("variable name"));
        assertFalse(stats.getVariableNameManager().contains("field name"));

        assertEquals(1, stats.getVariableReferences().size());
        assertEquals(2, stats.getVariableReferences().get("variable name").size());
        assertEquals(variableReference.getFieldByName("field name"),
                stats.getVariableReferences().get("variable name").get(0));
    }

    public void testCollectConnectionStatsRecursive() {
        // Make sure we're only recursing on next and input connections, not output or previous.
        Block.Builder blockBuilder = new Block.Builder("first block", "testid");
        blockBuilder.addInput(variableFieldsInput);
        blockBuilder.setNext(new Connection(Connection.CONNECTION_TYPE_NEXT, null));
        Block firstBlock = blockBuilder.build();

        blockBuilder = new Block.Builder("second block", "testid");
        blockBuilder.addInput(fieldInput);
        blockBuilder.setPrevious(new Connection(Connection.CONNECTION_TYPE_PREVIOUS, null));
        blockBuilder.setNext(new Connection(Connection.CONNECTION_TYPE_NEXT, null));

        Block secondBlock = blockBuilder.build();
        secondBlock.getPreviousConnection().connect(firstBlock.getNextConnection());

        blockBuilder = new Block.Builder("third block", "testid");

        Input in = new Input.InputDummy("name input", Input.ALIGN_LEFT);
        Field field = new Field.FieldVariable("third block field name", "nameid");
        field.setFromXmlText("third block variable name");
        in.add(field);
        blockBuilder.addInput(in);
        blockBuilder.setPrevious(new Connection(Connection.CONNECTION_TYPE_PREVIOUS, null));

        Block thirdBlock = blockBuilder.build();
        thirdBlock.getPreviousConnection().connect(secondBlock.getNextConnection());

        stats.collectStats(secondBlock, true);
        assertTrue(stats.getVariableNameManager().contains("third block variable name"));
        assertFalse(stats.getVariableNameManager().contains("variable name"));
        assertTrue(stats.getInputConnections().isEmpty());
        assertTrue(stats.getOutputConnections().isEmpty());
        assertEquals(2, stats.getPreviousConnections().size());
        assertEquals(1, stats.getNextConnections().size());
    }

    public void testRemoveConnection() {
        // TODO(fenichel): Implement in next CL.
    }
}
