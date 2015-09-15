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

import android.test.InstrumentationTestCase;

import com.google.blockly.model.Block;
import com.google.blockly.model.Connection;
import com.google.blockly.model.Field;
import com.google.blockly.model.Input;
import com.google.blockly.model.NameManager;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link WorkspaceStats}
 */
public class WorkspaceStatsTest extends InstrumentationTestCase {
    private WorkspaceStats stats;
    private Input fieldInput;
    private Input variableFieldsInput;
    private ConnectionManager connectionManager;
    private ProcedureManager mockProcedureManager;

    @Override
    public void setUp() throws Exception {
        System.setProperty(
                "dexmaker.dexcache",
                getInstrumentation().getTargetContext().getCacheDir().getPath());
        mockProcedureManager = mock(ProcedureManager.class);
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

        connectionManager = new ConnectionManager();
        stats = new WorkspaceStats(
                new NameManager.VariableNameManager(), mockProcedureManager, connectionManager);
    }

    public void testCollectProcedureStats() {
        Block.Builder blockBuilder = new Block.Builder(
                ProcedureManager.PROCEDURE_DEFINITION_PREFIX + "test", "testid");
        blockBuilder.addInput(fieldInput);
        Block blockUnderTest = blockBuilder.build();

        when(mockProcedureManager.isDefinition(any(Block.class))).thenReturn(true);
        stats.collectStats(blockUnderTest, false);

        verify(mockProcedureManager).addDefinition(blockUnderTest);

        // Add another block referring to the last one.
        blockBuilder = new Block.Builder(ProcedureManager.PROCEDURE_REFERENCE_PREFIX + "test", "ref");
        blockBuilder.addInput(fieldInput);
        Block procedureReference = blockBuilder.build();

        when(mockProcedureManager.isReference(any(Block.class))).thenReturn(true);
        when(mockProcedureManager.isDefinition(any(Block.class))).thenReturn(false);

        stats.collectStats(procedureReference, false);
        verify(mockProcedureManager).addReference(procedureReference);
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
        assertTrue(connectionManager.getConnections(Connection.CONNECTION_TYPE_INPUT).isEmpty());
        assertTrue(connectionManager.getConnections(Connection.CONNECTION_TYPE_OUTPUT).isEmpty());
        assertEquals(2,
                connectionManager.getConnections(Connection.CONNECTION_TYPE_PREVIOUS).size());
        assertEquals(1, connectionManager.getConnections(Connection.CONNECTION_TYPE_NEXT).size());
    }

    public void testRemoveConnection() {
        // TODO(fenichel): Implement in next CL.
    }
}
