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

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import com.google.blockly.android.TestUtils;
import com.google.blockly.model.Block;
import com.google.blockly.model.BlockDefinition;
import com.google.blockly.model.BlockFactory;
import com.google.blockly.model.Connection;
import com.google.blockly.utils.BlockLoadingException;

import org.json.JSONException;
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
    private ConnectionManager mConnectionManager;
    private ProcedureManager mMockProcedureManager;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getTargetContext();
        mFactory = new BlockFactory(context);

        System.setProperty(
                "dexmaker.dexcache",
                context.getCacheDir().getPath());
        mMockProcedureManager = mock(ProcedureManager.class);
        mConnectionManager = new ConnectionManager();
        mStats = new WorkspaceStats(
                new NameManager.VariableNameManager(), mMockProcedureManager, mConnectionManager);
    }

    @Test
    public void testCollectProcedureStats() throws BlockLoadingException {
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
    public void testCollectVariableStats() throws JSONException, BlockLoadingException {
        BlockDefinition def = new BlockDefinition(
                "{" +
                    "\"type\":\"two variable references\"," +
                    "\"message0\":\"%1 %2\"," +
                    "\"args0\":[{" +
                        "\"type\":\"field_variable\"," +
                        "\"name\":\"field name\"," +
                        "\"variable\":\"variable name\"" +
                    "},{" +
                        "\"type\":\"field_variable\"," +
                        "\"name\":\"field name 2\"," +
                        "\"variable\":\"variable name\"" +
                    "}]" +
                "}"
        );
        Block variableReference = mFactory.obtain(block().fromDefinition(def));
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
    public void testCollectConnectionStatsNextPrevStatementRecursion()
            throws JSONException, BlockLoadingException {

        BlockDefinition variableWithNext = new BlockDefinition(
            "{" +
                "\"type\":\"parent variableWithNext\"," +
                "\"message0\":\"%1\"," +
                "\"args0\":[{" +
                    "\"type\":\"field_variable\"," +
                    "\"name\":\"nameid\"," +
                    "\"variable\":\"variable on parent\"" +
                "}]," +
                "\"nextStatement\":null" +
            "}"
        );
        BlockDefinition nextAndPrev = new BlockDefinition(
            "{" +
                "\"type\":\"test target nextAndPrev\"," +
                "\"message0\":\"label on dummy input\"," +
                "\"previousStatement\":null," +
                "\"nextStatement\":null" +
            "}"
        );
        BlockDefinition variableWithPrev = new BlockDefinition(
            "{" +
                "\"type\":\"child variableWithPrev\"," +
                "\"message0\":\"%1\"," +
                "\"args0\":[{" +
                    "\"type\":\"field_variable\"," +
                    "\"name\":\"nameid\"," +
                    "\"variable\":\"variable on child\"" +
                "}]," +
                "\"previousStatement\":null" +
            "}"
        );

        // Make sure we're only recursing on next, not previous.
        Block parentBlock = mFactory.obtain(block().fromDefinition(variableWithNext));

        Block middleTestBlock = mFactory.obtain(block().fromDefinition(nextAndPrev));
        middleTestBlock.getPreviousConnection().connect(parentBlock.getNextConnection());

        Block childBlock = mFactory.obtain(block().fromDefinition(variableWithPrev));
        childBlock.getPreviousConnection().connect(middleTestBlock.getNextConnection());

        mStats.collectStats(middleTestBlock, true);
        assertThat(mStats.getVariableNameManager().contains("variable on child")).isTrue();
        assertThat(mStats.getVariableNameManager().contains("variable on parent")).isFalse();
        assertThat(mConnectionManager.getConnections(Connection.CONNECTION_TYPE_INPUT).isEmpty())
                .isTrue();
        assertThat(mConnectionManager.getConnections(Connection.CONNECTION_TYPE_OUTPUT).isEmpty())
                .isTrue();
        assertThat(mConnectionManager.getConnections(Connection.CONNECTION_TYPE_PREVIOUS).size())
                .isEqualTo(2);
        assertThat(mConnectionManager.getConnections(Connection.CONNECTION_TYPE_NEXT).size())
                .isEqualTo(1);
    }

    // TODO: testCollectConnectionStatsValueInputRecursion()

    // TODO: testCollectConnectionStatsStatementInputRecursion()


    @Test
    public void testRemoveConnection() {
        // TODO(fenichel): Implement in next CL.
    }
}
