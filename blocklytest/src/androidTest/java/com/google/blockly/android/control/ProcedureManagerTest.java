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

import com.google.blockly.android.BlocklyTestCase;
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
    private Block mProcedureDefinition;
    private Block mProcedureReference;

    @Before
    public void setUp() throws Exception {
        this.configureForUIThread();

        mController = new BlocklyController.Builder(getContext()).build();
        mFactory = mController.getBlockFactory();
        TestUtils.loadProcedureBlocks(mController);
        mProcedureManager = mController.getWorkspace().getProcedureManager();

        mProcedureDefinition = buildNoReturnDefinition(PROCEDURE_NAME);
        mProcedureReference = buildCaller(PROCEDURE_NAME);

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

    private Block buildCaller(final String procName) throws BlockLoadingException {
        final Block[] result = {null};
        runAndSync(new Runnable() {
            @Override
            public void run() {
                try {
                    Block block = mFactory.obtainBlockFrom(
                            new BlockTemplate(ProcedureManager.CALL_NO_RETURN_BLOCK_TYPE)
                                    .withMutation("<mutation name=\"" + procName + "\"/>"));
                    result[0] = block;
                } catch (BlockLoadingException e) {
                    throw new IllegalStateException(e);
                }
            }
        });
        return result[0];
    }

    private Block buildNoReturnDefinition(final String procName) throws BlockLoadingException {
        final Block[] result = { null };
        runAndSync(new Runnable() {
            @Override
            public void run() {
                try {
                    Block block = mFactory.obtainBlockFrom(
                            new BlockTemplate(ProcedureManager.DEFINE_NO_RETURN_BLOCK_TYPE)
                                .withMutation("<mutation name=\"" + procName + "\"/>"));
                    result[0] = block;
                } catch (BlockLoadingException e) {
                    throw new IllegalStateException(e);
                }
            }
        });
        return result[0];
    }
}
