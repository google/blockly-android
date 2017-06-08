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

import com.google.blockly.android.BlocklyTestCase;
import com.google.blockly.android.TestUtils;
import com.google.blockly.model.Block;
import com.google.blockly.model.BlockDefinition;
import com.google.blockly.model.BlockFactory;
import com.google.blockly.model.BlockTemplate;
import com.google.blockly.model.Connection;
import com.google.blockly.utils.BlockLoadingException;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link WorkspaceStats}.
 */
public class WorkspaceStatsTest extends BlocklyTestCase {
    private BlocklyController mController;
    private BlockFactory mFactory;
    private WorkspaceStats mStats;
    private ConnectionManager mConnectionManager;
    private ProcedureManager mProcedureManagerSpy;

    @Before
    public void setUp() {
        configureForUIThread();

        Context context = getContext();
        mController = new BlocklyController.Builder(context).build();
        mFactory = mController.getBlockFactory();
        TestUtils.loadProcedureBlocks(mController);
        mFactory.setController(mController);

        // TODO: Do we need this?
        //       http://stackoverflow.com/a/22402631/152543
        System.setProperty("dexmaker.dexcache", context.getCacheDir().getPath());

        mProcedureManagerSpy = spy(mController.getWorkspace().getProcedureManager());

        mConnectionManager = new ConnectionManager();
        mStats = new WorkspaceStats(
                new NameManager.VariableNameManager(), mProcedureManagerSpy, mConnectionManager);
    }

    @Test
    public void testCollectProcedureStats() throws BlockLoadingException {
        runAndSync(new Runnable() {
            @Override
            public void run() {
                try {
                    String mutation = "<mutation name=\"proc\"/>";
                    Block blockUnderTest = mFactory.obtainBlockFrom(
                            new BlockTemplate(ProcedureManager.DEFINE_NO_RETURN_BLOCK_TYPE)
                                    .withMutation(mutation));

                    mStats.collectStats(blockUnderTest, false);
                    verify(mProcedureManagerSpy).addDefinition(blockUnderTest);

                    // Add another block referring to the last one.
                    Block procedureReference = mFactory.obtainBlockFrom(
                            new BlockTemplate(ProcedureManager.CALL_NO_RETURN_BLOCK_TYPE)
                                    .withMutation(mutation));

                    mStats.collectStats(procedureReference, false);
                    verify(mProcedureManagerSpy).addReference(procedureReference);
                } catch (BlockLoadingException e) {
                    throw new IllegalStateException(e);
                }
            }
        });
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
        Block variableReference = mFactory.obtainBlockFrom(new BlockTemplate().fromDefinition(def));
        mStats.collectStats(variableReference, false);

        assertThat(mStats.getVariableNameManager().contains("variable name")).isTrue();
        assertThat(mStats.getVariableNameManager().contains("field name")).isFalse();

        assertThat(mStats.getVariableInfo("variable name").getFields().size()).isEqualTo(2);
        assertThat(mStats.getVariableInfo("variable name").getFields().get(0))
            .isEqualTo(variableReference.getFieldByName("field name"));
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
        Block parentBlock = mFactory.obtainBlockFrom(
                new BlockTemplate().fromDefinition(variableWithNext));

        Block middleTestBlock = mFactory.obtainBlockFrom(
                new BlockTemplate().fromDefinition(nextAndPrev));
        middleTestBlock.getPreviousConnection().connect(parentBlock.getNextConnection());

        Block childBlock = mFactory.obtainBlockFrom(
                new BlockTemplate().fromDefinition(variableWithPrev));
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
