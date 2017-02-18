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

import android.support.test.InstrumentationRegistry;

import com.google.blockly.android.TestUtils;
import com.google.blockly.model.Block;
import com.google.blockly.model.BlockFactory;
import com.google.blockly.model.Connection;
import com.google.blockly.model.Field;
import com.google.blockly.model.FieldInput;
import com.google.blockly.model.FieldVariable;
import com.google.blockly.model.Input;

import org.junit.Before;
import org.junit.Test;

import static com.google.blockly.model.BlockFactory.block;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link WorkspaceStats}.
 */
public class WorkspaceStatsTest {
    private BlockFactory mFactory;
    private WorkspaceStats mStats;
    private Input mFieldInput;
    private Input mVariableFieldsInput;
    private ConnectionManager mConnectionManager;
    private ProcedureManager mMockProcedureManager;

    @Before
    public void setUp() {
        mFactory = new BlockFactory(InstrumentationRegistry.getTargetContext());

        System.setProperty(
                "dexmaker.dexcache",
                InstrumentationRegistry.getTargetContext().getCacheDir().getPath());
        mMockProcedureManager = mock(ProcedureManager.class);
        mFieldInput = new Input.InputDummy("name input", Input.ALIGN_LEFT);
        Field field = new FieldInput("name", "nameid");
        field.setFromString("new procedure");
        mFieldInput.add(field);

        mVariableFieldsInput = new Input.InputDummy("name input", Input.ALIGN_LEFT);
        field = new FieldVariable("field name", "nameid");
        field.setFromString("variable name");
        mVariableFieldsInput.add(field);
        field = new FieldVariable("field name 2", "nameid2");
        field.setFromString("variable name");
        mVariableFieldsInput.add(field);

        mConnectionManager = new ConnectionManager();
        mStats = new WorkspaceStats(
                new NameManager.VariableNameManager(), mMockProcedureManager, mConnectionManager);
    }

    @Test
    public void testCollectProcedureStats() {
        Block blockUnderTest = mFactory.obtain(
                block().fromDefinition(TestUtils.getProcedureDefinitionBlockDefinition("test")));

        mStats.collectStats(blockUnderTest, false);
        verify(mMockProcedureManager).addDefinition(blockUnderTest);

        // Add another block referring to the last one.
        Block procedureReference = mFactory.obtain(
                block().fromDefinition(TestUtils.getProcedureReferenceBlockDefinition("test")));

        mStats.collectStats(procedureReference, false);
        verify(mMockProcedureManager).addReference(procedureReference);
    }

    @Test
    public void testCollectVariableStats() {
        Block.Builder blockBuilder = new Block.Builder("test");

        blockBuilder.addInput(mVariableFieldsInput);
        Block variableReference = blockBuilder.build();
        mStats.collectStats(variableReference, false);

        assertThat(mStats.getVariableNameManager().contains("variable name")).isTrue();
        assertThat(mStats.getVariableNameManager().contains("field name")).isFalse();

        assertThat(mStats.getVariableReference("variable name").size()).isEqualTo(2);
        assertThat(mStats.getVariableReference("variable name").get(0))
            .isEqualTo(variableReference.getFieldByName("field name"));
    }

    @Test
    public void testVariableReferencesNeverNull() {
        assertThat(mStats.getVariableReference("not a reference")).isNotNull();
    }

    @Test
    public void testCollectConnectionStatsRecursive() {
        // Make sure we're only recursing on next and input connections, not output or previous.
        Block.Builder blockBuilder = new Block.Builder("first block");
        blockBuilder.addInput(mVariableFieldsInput);
        blockBuilder.setNext(new Connection(Connection.CONNECTION_TYPE_NEXT, null));
        Block firstBlock = blockBuilder.build();

        blockBuilder = new Block.Builder("second block");
        blockBuilder.addInput(mFieldInput);
        blockBuilder.setPrevious(new Connection(Connection.CONNECTION_TYPE_PREVIOUS, null));
        blockBuilder.setNext(new Connection(Connection.CONNECTION_TYPE_NEXT, null));

        Block secondBlock = blockBuilder.build();
        secondBlock.getPreviousConnection().connect(firstBlock.getNextConnection());

        blockBuilder = new Block.Builder("third block");

        Input in = new Input.InputDummy("name input", Input.ALIGN_LEFT);
        Field field = new FieldVariable( "nameid", "third block field name");
        field.setFromString("third block variable name");
        in.add(field);
        blockBuilder.addInput(in);
        blockBuilder.setPrevious(new Connection(Connection.CONNECTION_TYPE_PREVIOUS, null));

        Block thirdBlock = blockBuilder.build();
        thirdBlock.getPreviousConnection().connect(secondBlock.getNextConnection());

        mStats.collectStats(secondBlock, true);
        assertThat(mStats.getVariableNameManager().contains("third block variable name")).isTrue();
        assertThat(mStats.getVariableNameManager().contains("variable name")).isFalse();
        assertThat(mConnectionManager.getConnections(Connection.CONNECTION_TYPE_INPUT).isEmpty())
                .isTrue();
        assertThat(mConnectionManager.getConnections(Connection.CONNECTION_TYPE_OUTPUT).isEmpty())
                .isTrue();
        assertThat(mConnectionManager.getConnections(Connection.CONNECTION_TYPE_PREVIOUS).size())
                .isEqualTo(2);
        assertThat(mConnectionManager.getConnections(Connection.CONNECTION_TYPE_NEXT).size())
                .isEqualTo(1);
    }

    @Test
    public void testRemoveConnection() {
        // TODO(fenichel): Implement in next CL.
    }
}
