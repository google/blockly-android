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

import android.database.DataSetObserver;

import com.google.blockly.android.BlocklyTestCase;
import com.google.blockly.android.TestUtils;
import com.google.blockly.android.control.ProcedureManager.ArgumentIndexUpdate;
import com.google.blockly.model.Block;
import com.google.blockly.model.BlockFactory;
import com.google.blockly.model.BlockTemplate;
import com.google.blockly.model.FieldInput;
import com.google.blockly.model.ProcedureInfo;
import com.google.blockly.model.VariableInfo;
import com.google.blockly.model.mutator.AbstractProcedureMutator;
import com.google.blockly.utils.BlockLoadingException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests for {@link ProcedureManager}.
 */
public class ProcedureManagerTest extends BlocklyTestCase {
    private static final String PROCEDURE_NAME = "procedure name";

    private BlocklyController mController;

    private BlockFactory mFactory;
    private ProcedureManager mProcedureManager;
    private VariableNameManager mVariableManager;
    private Block mProcedureDefinition;
    private Block mProcedureReference;

    @Before
    public void setUp() throws Exception {
        this.configureForUIThread();

        mController = new BlocklyController.Builder(getContext()).build();
        mFactory = mController.getBlockFactory();
        TestUtils.loadProcedureBlocks(mController);
        mProcedureManager = mController.getWorkspace().getProcedureManager();
        mVariableManager = mController.getWorkspace().getVariableNameManager();

        mProcedureDefinition = buildDefinitionWithoutReturn(PROCEDURE_NAME);
        mProcedureReference = buildCallerWithoutReturn(PROCEDURE_NAME);

        assertThat(mProcedureDefinition).isNotNull();
        assertThat(mProcedureReference).isNotNull();
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testAddProcedureDefinition() {
        mProcedureManager.addDefinition(mProcedureDefinition);
        assertThat(mProcedureManager.containsDefinition(mProcedureDefinition)).isTrue();
        assertThat(mProcedureManager.isDefinitionReferenced(mProcedureDefinition)).isFalse();
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
    public void testAddProcedureReference() throws BlockLoadingException {
        mProcedureManager.addDefinition(mProcedureDefinition);

        mProcedureManager.addReference(mProcedureReference);
        assertThat(mProcedureManager.isDefinitionReferenced(mProcedureDefinition)).isTrue();
    }

    @Test
    // Remove definition should also remove all references.
    public void testRemoveProcedureDefinition() throws BlockLoadingException {
        mProcedureManager.addDefinition(mProcedureDefinition);
        assertThat(mProcedureManager.containsDefinition(mProcedureDefinition)).isTrue();

        mProcedureManager.removeProcedure(mProcedureDefinition);
        assertThat(mProcedureManager.containsDefinition(mProcedureDefinition)).isFalse();
        assertThat(mProcedureManager.isDefinitionReferenced(mProcedureDefinition)).isFalse();

        mProcedureManager.addDefinition(mProcedureDefinition);
        mProcedureManager.addReference(mProcedureReference);
        Set<Block> removedBlocks =
                mProcedureManager.removeProcedure(mProcedureDefinition);
        assertThat(removedBlocks).isNotNull();
        assertThat(removedBlocks.size()).isEqualTo(2); // 1 definition and 1 caller
        assertThat(removedBlocks.contains(mProcedureReference)).isTrue();

        assertThat(mProcedureManager.containsDefinition(mProcedureDefinition)).isFalse();
        assertThat(mProcedureManager.isDefinitionReferenced(mProcedureDefinition)).isFalse();
    }

    @Test
    public void testRemoveProcedureReference() throws BlockLoadingException {
        mProcedureManager.addDefinition(mProcedureDefinition);
        mProcedureManager.addReference(mProcedureReference);

        mProcedureManager.removeReference(mProcedureReference);
        assertThat(mProcedureManager.isReferenceRegistered(mProcedureReference)).isFalse();
    }

    @Test
    public void testMissingNames() throws BlockLoadingException {
        mProcedureDefinition = mFactory.obtainBlockFrom(new BlockTemplate().fromJson(
                "{\"type\":\"no field named name\"}"));

        thrown.expect(IllegalArgumentException.class);
        mProcedureManager.addDefinition(mProcedureDefinition);
    }

    @Test
    public void testAddReferenceToUndefined() throws BlockLoadingException {
        thrown.expect(BlockLoadingException.class);
        mProcedureManager.addReference(mProcedureReference);
    }

    @Test
    public void testRemoveNoUndefined() {
        assertThat(mProcedureManager.removeProcedure(mProcedureDefinition).size()).isEqualTo(0);
    }

    @Test
    public void testNoReference() {
        assertThat(mProcedureManager.removeReference(mProcedureReference)).isFalse();
    }

    @Test
    public void testDefinitionWithArgs() throws BlockLoadingException {
        String procName = "procWithArgs";
        String[] args = { "abc", "DEF", "xYz"};
        final Block procDef = buildDefinitionWithReturn(procName, args);
        runAndSync(new Runnable() {
            @Override
            public void run() {
                mProcedureManager.addDefinition(procDef);
            }
        });

        assertThat(mProcedureManager.isProcedureDefined(procName)).isTrue();
        for (String arg : args) {
            assertThat(mVariableManager.hasName(arg)).isTrue();
            assertThat(mVariableManager.getDisplayName(arg)).isEqualTo(arg);
        }
    }

    @Test
    public void testProcedureMutationAddsArgs() throws BlockLoadingException {
        mProcedureManager.addDefinition(mProcedureDefinition);
        mProcedureManager.addReference(mProcedureReference);
        assertThat(mProcedureManager.isDefinitionReferenced(mProcedureDefinition)).isTrue();

        final String[] args = {"FiRsT", "sEcOnD"};
        for (String arg : args) {
            assertThat(mVariableManager.hasName(arg)).isFalse();
        }

        final int[] onChangeCallbackCallCount = { 0 };
        mVariableManager.registerObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                ++onChangeCallbackCallCount[0];
            }
        });

        final ProcedureInfo firstMutatedInfo =
                new ProcedureInfo(PROCEDURE_NAME, Arrays.asList(args[0]), true);
        runAndSync(new Runnable() {
            @Override
            public void run() {
                mProcedureManager.mutateProcedure(
                        PROCEDURE_NAME, firstMutatedInfo, Collections.EMPTY_LIST);
            }
        });

        ProcedureInfo defInfo =
                ((AbstractProcedureMutator) mProcedureDefinition.getMutator()).getProcedureInfo();
        ProcedureInfo refInfo =
                ((AbstractProcedureMutator) mProcedureReference.getMutator()).getProcedureInfo();
        assertThat(defInfo.getArgumentNames()).hasSize(1);
        assertThat(defInfo.getArgumentNames().get(0)).isEqualTo(args[0]);
        assertThat(refInfo.getArgumentNames()).hasSize(1);
        assertThat(refInfo.getArgumentNames().get(0)).isEqualTo(args[0]);
        assertThat(mVariableManager.hasName(args[0])).isTrue();
        assertThat(mVariableManager.getDisplayName(args[0])).isEqualTo(args[0]);
        assertThat(mVariableManager.hasName(args[1])).isFalse();
        assertThat(mVariableManager.getUsedNames()).contains(args[0]);
        assertThat(mVariableManager.getUsedNames()).doesNotContain(args[1]);
        assertThat(onChangeCallbackCallCount[0]).isEqualTo(1);

        // Mutate via the reference block, adding the second argument
        final ProcedureInfo secondMutatedInfo =
                new ProcedureInfo(PROCEDURE_NAME, Arrays.asList(args[0], args[1]), true);
        runAndSync(new Runnable() {
            @Override
            public void run() {
                mProcedureManager.mutateProcedure(
                        PROCEDURE_NAME, secondMutatedInfo, buildArgUpdates(new int[][] {{0,0}}));
            }
        });

        defInfo = ((AbstractProcedureMutator) mProcedureDefinition.getMutator()).getProcedureInfo();
        refInfo = ((AbstractProcedureMutator) mProcedureReference.getMutator()).getProcedureInfo();
        assertThat(defInfo.getArgumentNames()).hasSize(2);
        assertThat(defInfo.getArgumentNames().get(1)).isEqualTo(args[1]);
        assertThat(refInfo.getArgumentNames()).hasSize(2);
        assertThat(refInfo.getArgumentNames().get(1)).isEqualTo(args[1]);
        assertThat(mVariableManager.hasName(args[0])).isTrue();
        assertThat(mVariableManager.hasName(args[1])).isTrue();
        assertThat(mVariableManager.getDisplayName(args[1])).isEqualTo(args[1]);
        assertThat(mVariableManager.getUsedNames()).contains(args[0]);
        assertThat(mVariableManager.getUsedNames()).contains(args[1]);
        assertThat(onChangeCallbackCallCount[0]).isEqualTo(2);
    }

    @Test
    public void testProcedureMutationRemovesArgs() throws BlockLoadingException {
        mProcedureManager.addDefinition(mProcedureDefinition);
        mProcedureManager.addReference(mProcedureReference);
        assertThat(mProcedureManager.isDefinitionReferenced(mProcedureDefinition)).isTrue();

        // Setup and preconditions
        final String[] args = {"FiRsT", "sEcOnD"};
        final ProcedureInfo addArgs =
                new ProcedureInfo(PROCEDURE_NAME, Arrays.asList(args), true);
        runAndSync(new Runnable() {
            @Override
            public void run() {
                mProcedureManager.mutateProcedure(
                        PROCEDURE_NAME, addArgs, Collections.EMPTY_LIST);

                // Confirm initial setup
                ProcedureInfo defInfo =
                        ((AbstractProcedureMutator) mProcedureDefinition.getMutator()).getProcedureInfo();
                ProcedureInfo refInfo =
                        ((AbstractProcedureMutator) mProcedureReference.getMutator()).getProcedureInfo();
                assertThat(defInfo.getArgumentNames()).hasSize(2);
                assertThat(defInfo.getArgumentNames()).contains(args[0]);
                assertThat(defInfo.getArgumentNames()).contains(args[1]);
                assertThat(refInfo.getArgumentNames()).hasSize(2);
                assertThat(refInfo.getArgumentNames()).contains(args[0]);
                assertThat(refInfo.getArgumentNames()).contains(args[1]);
                assertThat(mVariableManager.hasName(args[0])).isTrue();
                assertThat(mVariableManager.hasName(args[1])).isTrue();
                assertThat(mVariableManager.getUsedNames()).contains(args[0]);
                assertThat(mVariableManager.getUsedNames()).contains(args[1]);
                assertThat(((VariableInfo) mVariableManager.getValueOf(args[0])).getProcedureNames())
                        .isNotNull();
                assertThat(((VariableInfo) mVariableManager.getValueOf(args[0])).getProcedureNames())
                        .contains(PROCEDURE_NAME);
                assertThat(((VariableInfo) mVariableManager.getValueOf(args[1])).getProcedureNames())
                        .isNotNull();
                assertThat(((VariableInfo) mVariableManager.getValueOf(args[1])).getProcedureNames())
                        .contains(PROCEDURE_NAME);

                // Mutate to zero arguments
                final ProcedureInfo noArgInfo =
                        new ProcedureInfo(PROCEDURE_NAME, Collections.<String>emptyList(), true);
                mProcedureManager.mutateProcedure(
                        PROCEDURE_NAME, noArgInfo, Collections.EMPTY_LIST);

                // No arguments on the definition or in the reference
                defInfo = ((AbstractProcedureMutator) mProcedureDefinition.getMutator()).getProcedureInfo();
                refInfo = ((AbstractProcedureMutator) mProcedureReference.getMutator()).getProcedureInfo();
                assertThat(defInfo.getArgumentNames()).hasSize(0);
                assertThat(refInfo.getArgumentNames()).hasSize(0);

                // But the variables still exist.
                assertThat(mVariableManager.hasName(args[0])).isTrue();
                assertThat(mVariableManager.hasName(args[1])).isTrue();
                assertThat(mVariableManager.getUsedNames()).contains(args[0]);
                assertThat(mVariableManager.getUsedNames()).contains(args[1]);
                assertThat(((VariableInfo) mVariableManager.getValueOf(args[0])).getProcedureNames())
                        .isEmpty();
                assertThat(((VariableInfo) mVariableManager.getValueOf(args[1])).getProcedureNames())
                        .isEmpty();
            }
        });
    }

    @Test
    public void testDeletesVariablesThatAreProcedureArguments() throws BlockLoadingException {
        mProcedureManager.addDefinition(mProcedureDefinition);
        mProcedureManager.addReference(mProcedureReference);
        assertThat(mProcedureManager.isDefinitionReferenced(mProcedureDefinition)).isTrue();

        // Setup and preconditions
        final String[] args = {"FiRsT", "sEcOnD"};
        final ProcedureInfo addArgs =
                new ProcedureInfo(PROCEDURE_NAME, Arrays.asList(args), true);
        runAndSync(new Runnable() {
            @Override
            public void run() {
                mProcedureManager.mutateProcedure(
                        PROCEDURE_NAME, addArgs, Collections.EMPTY_LIST);

                // Can't delete
                assertThat(mController.requestDeleteVariable(args[0])).isFalse();
                assertThat(mController.requestDeleteVariable(args[1])).isFalse();

                assertThat(mVariableManager.hasName(args[0])).isTrue();
                assertThat(mVariableManager.hasName(args[1])).isTrue();

                // Mutate to zero arguments
                final ProcedureInfo noArgInfo =
                        new ProcedureInfo(PROCEDURE_NAME, Collections.<String>emptyList(), true);
                mProcedureManager.mutateProcedure(
                        PROCEDURE_NAME, noArgInfo, Collections.EMPTY_LIST);

                // Still exists
                assertThat(mVariableManager.hasName(args[0])).isTrue();
                assertThat(mVariableManager.hasName(args[1])).isTrue();

                // And now we can delete them
                assertThat(mController.requestDeleteVariable(args[0])).isTrue();
                assertThat(mController.requestDeleteVariable(args[1])).isTrue();
                assertThat(mVariableManager.hasName(args[0])).isFalse();
                assertThat(mVariableManager.hasName(args[1])).isFalse();
            }
        });
    }

    @Test
    public void testProcedureMutationRenamesArguments() throws BlockLoadingException {
        mProcedureManager.addDefinition(mProcedureDefinition);
        mProcedureManager.addReference(mProcedureReference);
        assertThat(mProcedureManager.isDefinitionReferenced(mProcedureDefinition)).isTrue();

        runAndSync(new Runnable() {
            @Override
            public void run() {
                // Setup and preconditions
                final String[] initialArgNames = {"FiRsT", "sEcOnD"};
                final ProcedureInfo addArgs =
                        new ProcedureInfo(PROCEDURE_NAME, Arrays.asList(initialArgNames), true);
                mProcedureManager.mutateProcedure(
                        PROCEDURE_NAME, addArgs, Collections.EMPTY_LIST);

                // TODO: Add blocks to the workspace and connect arguments to the caller block

                ProcedureInfo defInfo =
                        ((AbstractProcedureMutator) mProcedureDefinition.getMutator())
                                .getProcedureInfo();
                ProcedureInfo refInfo =
                        ((AbstractProcedureMutator) mProcedureReference.getMutator())
                                .getProcedureInfo();
                assertThat(mVariableManager.getDisplayName(initialArgNames[0]))
                        .isEqualTo(initialArgNames[0]);
                assertThat(mVariableManager.getDisplayName(initialArgNames[1]))
                        .isEqualTo(initialArgNames[1]);
                assertThat(defInfo.getArgumentNames().get(0)).isEqualTo(initialArgNames[0]);
                assertThat(defInfo.getArgumentNames().get(1)).isEqualTo(initialArgNames[1]);
                assertThat(refInfo.getArgumentNames().get(0)).isEqualTo(initialArgNames[0]);
                assertThat(refInfo.getArgumentNames().get(1)).isEqualTo(initialArgNames[1]);

                // Mutate to new names arguments
                final String[] renamedArgs = {"pRemIère", "seCOndE"};
                assertThat(renamedArgs[0]).isNotEqualTo(initialArgNames[0]);
                assertThat(renamedArgs[1]).isNotEqualTo(initialArgNames[1]);
                final ProcedureInfo renamedArgsInfo =
                        new ProcedureInfo(PROCEDURE_NAME, Arrays.asList(renamedArgs), true);
                mProcedureManager.mutateProcedure(
                        PROCEDURE_NAME, renamedArgsInfo,
                        buildArgUpdates(new int[][] {{0, 0}, {1, 1}}));  // Same place

                // Still exists
                assertThat(mVariableManager.hasName(initialArgNames[0])).isTrue();
                assertThat(mVariableManager.hasName(initialArgNames[1])).isTrue();

                // But the new names are in use
                defInfo = ((AbstractProcedureMutator) mProcedureDefinition.getMutator())
                        .getProcedureInfo();
                refInfo = ((AbstractProcedureMutator) mProcedureReference.getMutator())
                        .getProcedureInfo();
                assertThat(mVariableManager.getDisplayName(renamedArgs[0]))
                        .isEqualTo(renamedArgs[0]);
                assertThat(mVariableManager.getDisplayName(renamedArgs[1]))
                        .isEqualTo(renamedArgs[1]);
                assertThat(defInfo.getArgumentNames().get(0)).isEqualTo(renamedArgs[0]);
                assertThat(defInfo.getArgumentNames().get(1)).isEqualTo(renamedArgs[1]);
                assertThat(refInfo.getArgumentNames().get(0)).isEqualTo(renamedArgs[0]);
                assertThat(refInfo.getArgumentNames().get(1)).isEqualTo(renamedArgs[1]);

                // TODO: Test that caller arg values remain connected
            }
        });
    }

    @Test
    public void testProcedureMutationReordersArguments() throws BlockLoadingException {
        mProcedureManager.addDefinition(mProcedureDefinition);
        mProcedureManager.addReference(mProcedureReference);
        assertThat(mProcedureManager.isDefinitionReferenced(mProcedureDefinition)).isTrue();

        runAndSync(new Runnable() {
            @Override
            public void run() {
                // Setup and preconditions
                final String[] initialArgNames = {"FiRsT", "sEcOnD"};
                final ProcedureInfo addArgs =
                        new ProcedureInfo(PROCEDURE_NAME, Arrays.asList(initialArgNames), true);
                mProcedureManager.mutateProcedure(
                        PROCEDURE_NAME, addArgs, Collections.EMPTY_LIST);

                // TODO: Add blocks to the workspace and connect arguments to the caller block

                ProcedureInfo defInfo =
                        ((AbstractProcedureMutator) mProcedureDefinition.getMutator())
                                .getProcedureInfo();
                ProcedureInfo refInfo =
                        ((AbstractProcedureMutator) mProcedureReference.getMutator())
                                .getProcedureInfo();
                assertThat(mVariableManager.getDisplayName(initialArgNames[0]))
                        .isEqualTo(initialArgNames[0]);
                assertThat(mVariableManager.getDisplayName(initialArgNames[1]))
                        .isEqualTo(initialArgNames[1]);
                assertThat(defInfo.getArgumentNames().get(0)).isEqualTo(initialArgNames[0]);
                assertThat(defInfo.getArgumentNames().get(1)).isEqualTo(initialArgNames[1]);
                assertThat(refInfo.getArgumentNames().get(0)).isEqualTo(initialArgNames[0]);
                assertThat(refInfo.getArgumentNames().get(1)).isEqualTo(initialArgNames[1]);

                // Mutate to new names arguments
                final String[] reorderedArgs = {initialArgNames[1], initialArgNames[0]};
                assertThat(reorderedArgs[0]).isNotEqualTo(initialArgNames[0]);
                assertThat(reorderedArgs[1]).isNotEqualTo(initialArgNames[1]);
                final ProcedureInfo renamedArgsInfo =
                        new ProcedureInfo(PROCEDURE_NAME, Arrays.asList(reorderedArgs), true);
                mProcedureManager.mutateProcedure(
                        PROCEDURE_NAME, renamedArgsInfo,
                        buildArgUpdates(new int[][] {{0, 1}, {1, 0}}));  // Reordering!!

                // But the reordered names are in use
                defInfo = ((AbstractProcedureMutator) mProcedureDefinition.getMutator())
                        .getProcedureInfo();
                refInfo = ((AbstractProcedureMutator) mProcedureReference.getMutator())
                        .getProcedureInfo();
                assertThat(defInfo.getArgumentNames().get(0)).isEqualTo(reorderedArgs[0]);
                assertThat(defInfo.getArgumentNames().get(1)).isEqualTo(reorderedArgs[1]);
                assertThat(refInfo.getArgumentNames().get(0)).isEqualTo(reorderedArgs[0]);
                assertThat(refInfo.getArgumentNames().get(1)).isEqualTo(reorderedArgs[1]);

                // TODO: Test that caller arg values are connected in the new order
            }
        });
    }

    private Block buildDefinitionWithoutReturn(final String procName, String... args)
            throws BlockLoadingException
    {
        return buildBlockWithMutation(ProcedureManager.DEFINE_NO_RETURN_BLOCK_TYPE,
                buildMutationXml(procName, args));
    }

    private Block buildCallerWithoutReturn(final String procName, String... args)
            throws BlockLoadingException
    {
        return buildBlockWithMutation(ProcedureManager.CALL_NO_RETURN_BLOCK_TYPE,
                buildMutationXml(procName, args));
    }

    private Block buildDefinitionWithReturn(final String procName, String... args)
            throws BlockLoadingException
    {
        return buildBlockWithMutation(ProcedureManager.DEFINE_WITH_RETURN_BLOCK_TYPE,
                buildMutationXml(procName, args));
    }

    private Block buildCallerWithReturn(final String procName, String... args)
            throws BlockLoadingException
    {
        return buildBlockWithMutation(ProcedureManager.CALL_WITH_RETURN_BLOCK_TYPE,
                buildMutationXml(procName, args));
    }

    private String buildMutationXml(final String procName, String... args) {
        final StringBuilder sb = new StringBuilder("<mutation name=\"" + procName + "\">");
        for (String arg : args) {
            sb.append("<arg name=\"").append(arg).append("\"/>");
        }
        sb.append("</mutation>");
        return sb.toString();
    }

    private Block buildBlockWithMutation(final String blockType, final String mutationXml)
            throws BlockLoadingException
    {
        final Block[] result = { null };

        runAndSync(new Runnable() {
            @Override
            public void run() {
                try {
                    Block block = mFactory.obtainBlockFrom(
                            new BlockTemplate(blockType)
                                    .withMutation(mutationXml));
                    result[0] = block;
                } catch (BlockLoadingException e) {
                    throw new IllegalStateException(e);
                }
            }
        });
        return result[0];
    }

    private List<ArgumentIndexUpdate> buildArgUpdates(int[][] argNameMapping) {
        List<ArgumentIndexUpdate> updates = new ArrayList<>(argNameMapping.length);
        for (int i = 0; i < argNameMapping.length; ++i) {
            updates.add(new ArgumentIndexUpdate(
                    /* original */ argNameMapping[i][0],
                    /* mutated */ argNameMapping[i][1]));
        }
        return updates;
    }
}
