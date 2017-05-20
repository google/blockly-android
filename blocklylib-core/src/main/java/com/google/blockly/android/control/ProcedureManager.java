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

package com.google.blockly.android.control;

import android.database.DataSetObserver;
import android.database.Observable;
import android.support.v4.util.ArrayMap;

import com.google.blockly.model.Block;
import com.google.blockly.model.Input;
import com.google.blockly.model.Mutator;
import com.google.blockly.model.Workspace;
import com.google.blockly.model.mutator.AbstractProcedureMutator;
import com.google.blockly.model.mutator.ProcedureCallMutator;
import com.google.blockly.model.mutator.ProcedureDefinitionMutator;
import com.google.blockly.utils.BlockLoadingException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Manages procedure definitions, references and names.
 */
public class ProcedureManager extends Observable<DataSetObserver> {
    private static final String TAG = "ProcedureManager";

    /**
     * Describes an update to a procedure argument, with reference to its original ordering.
     */
    public static class ArgumentUpdate {
        /** Value for {@code originalIndex} when the argument is new. */
        public static final int NEW_ARGUMENT = -1;

        private final int mOriginalIndex;
        private String mName;

        public ArgumentUpdate(String name, int originalIndex) {
            mName = name;
            mOriginalIndex = originalIndex;
        }

        public int getOriginalIndex() {
            return mOriginalIndex;
        }

        public String getName() {
            return mName;
        }

        public void setName(String name) {
            // TODO: Validate or cannonicalize
            mName = name;
        }
    }

    public static final String PROCEDURE_NAME_FIELD = "NAME";

    public static final String DEFINE_NO_RETURN_BLOCK_TYPE = "procedures_defnoreturn";
    public static final String DEFINE_WITH_RETURN_BLOCK_TYPE = "procedures_defreturn";
    public static final String CALL_NO_RETURN_BLOCK_TYPE = "procedures_callnoreturn";
    public static final String CALL_WITH_RETURN_BLOCK_TYPE = "procedures_callreturn";

    private final BlocklyController mController;
    private final NameManager mVariableNameManager;

    private final ArrayMap<String, List<Block>> mProcedureReferences = new ArrayMap<>();
    private final ArrayMap<String, Block> mProcedureDefinitions = new ArrayMap<>();
    private final NameManager mProcedureNameManager = new NameManager.ProcedureNameManager();

    private int mCountOfReferencesWithReturn = 0;

    public ProcedureManager(BlocklyController controller, Workspace workspace) {
        mController = controller;
        mVariableNameManager = workspace.getVariableNameManager();
    }

    /**
     * Determines if a block is procedure call.
     * @param block The block in question.
     * @return True, if the block type is a recognized type of procedure call.
     */
    public static boolean isReference(Block block) {
        Mutator mutator = block.getMutator();
        return mutator != null && mutator instanceof ProcedureCallMutator;
    }

    /**
     * Determines if a block is procedure definition.
     * @param block The block in question.
     * @return True, if the block type is a recognized type of procedure definition.
     */
    public static boolean isDefinition(Block block) {
        Mutator mutator = block.getMutator();
        return mutator != null && mutator instanceof ProcedureDefinitionMutator;
    }

    public Map<String, Block> getDefinitionBlocks() {
        return Collections.unmodifiableMap(mProcedureDefinitions);
    }

    public List<Block> getReferences(String functionName) {
        return mProcedureReferences.get(functionName);
    }

    public boolean containsDefinition(Block block) {
        return mProcedureDefinitions.containsKey(getProcedureName(block));
    }

    /**
     * @param block The block being referenced.
     *
     * @return True if the block is referenced one or more times.
     */
    public boolean hasReferences(Block block) {
        return (mProcedureReferences.get(getProcedureName(block)) != null
                && !mProcedureReferences.get(getProcedureName(block)).isEmpty());
    }

    public void clear() {
        mProcedureDefinitions.clear();
        mProcedureReferences.clear();
        mProcedureNameManager.clearUsedNames();
        mCountOfReferencesWithReturn = 0;

        notifyObservers();
    }

    /**
     * Adds a reference to a procedure.
     *
     * @param block The reference to add.
     *
     * @throws IllegalStateException if the referenced procedure has not been defined.
     */
    public void addReference(Block block) {
        String procedureName = getProcedureName(block);
        if (mProcedureReferences.containsKey(procedureName)) {
            mProcedureReferences.get(procedureName).add(block);
            if (block.getType().equals(CALL_WITH_RETURN_BLOCK_TYPE)) {
                ++mCountOfReferencesWithReturn;
            }

            notifyObservers();
        } else {
            throw new IllegalStateException(
                    "Tried to add a reference to a procedure that has not been defined.");
        }
    }

    /**
     * Removes a reference to a procedure.
     *
     * @param block The reference to remove.
     *
     * @throws IllegalStateException if the referenced procedure has not been defined..
     */
    public void removeReference(Block block) {
        String procedureName = getProcedureName(block);
        if (mProcedureReferences.containsKey(procedureName)) {
            mProcedureReferences.get(procedureName).remove(block);
            if (block.getType().equals(CALL_WITH_RETURN_BLOCK_TYPE)) {
                --mCountOfReferencesWithReturn;
                assert (mCountOfReferencesWithReturn >= 0);
            }

            notifyObservers();
        } else {
            throw new IllegalStateException(
                    "Tried to remove a procedure reference that was not in the list of references");
        }
    }

    /**
     * Adds a block containing a procedure definition to the managed list.  If a procedure
     * by that name is already defined, creates a new unique name for the procedure and renames the
     * block.
     *
     * @param block A block containing the definition of the procedure to add.
     */
    public void addDefinition(Block block) {
        String procedureName = getProcedureName(block);
        if (mProcedureDefinitions.get(procedureName) == block) {
            throw new IllegalStateException("Tried to add the same block definition twice");
        }
        if (mProcedureNameManager.contains(procedureName)) {
            procedureName = mProcedureNameManager.generateUniqueName(procedureName, false);
            setProcedureName(block, procedureName);
        }
        mProcedureDefinitions.put(procedureName, block);
        mProcedureReferences.put(procedureName, new ArrayList<Block>());
        mProcedureNameManager.addName(procedureName);

        notifyObservers();
    }

    /**
     * Removes the block containing the procedure definition from the manager, and removes all
     * references as well.  Returns a list of Blocks to recursively delete.
     *
     * @param block A block containing the definition of the procedure to remove.
     *
     * @return A list of Blocks that referred to the procedure defined by block.
     */
    public List<Block> removeDefinition(Block block) {
        String procedureName = getProcedureName(block);
        if (mProcedureDefinitions.containsKey(procedureName)) {
            List<Block> retval = mProcedureReferences.get(procedureName);
            mProcedureReferences.remove(procedureName);
            mProcedureDefinitions.remove(procedureName);
            mProcedureNameManager.remove(procedureName);

            notifyObservers();

            return retval;
        } else {
            throw new IllegalStateException(
                    "Tried to remove an unknown procedure definition");
        }
    }

    /**
     * Updates all blocks
     * @param procedureName
     * @param argUpdates
     * @param definitionHasStatementInputs
     * @throws IllegalArgumentException If any argument name is invalid.
     */
    public void mutateProcedure(final String procedureName,
                                final List<ArgumentUpdate> argUpdates,
                                final boolean definitionHasStatementInputs)
    {
        final Block definition = mProcedureDefinitions.get(procedureName);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown procedure \"" + procedureName + "\"");
        }

        final int newArgCount = argUpdates.size();
        final List<String> newArgs = new ArrayList<>(newArgCount);
        for (int i = 0; i < newArgCount; ++i) {
            String argName = argUpdates.get(i).getName();
            if (!mVariableNameManager.contains(argName)) {
                mVariableNameManager.addName(argName);
            }

            if (!mVariableNameManager.isValidName(argName)) {
                throw new IllegalArgumentException(
                        "Invalid variable name \"" + argName + "\" " + "(argument #" + i +" )");
            }
            newArgs.add(argName);
        }

        mController.groupAndFireEvents(new Runnable() {
            @Override
            public void run() {
                for (String argName : newArgs) {
                    if (!mVariableNameManager.contains(argName)) {
                        mVariableNameManager.addName(argName);  // TODO: Trigger variable event
                    }
                    // TODO: What if it is a different representation of existing var? "x" vs "X"
                }

                ProcedureDefinitionMutator definitionMutator =
                        (ProcedureDefinitionMutator) definition.getMutator();
                assert definitionMutator != null;
                definitionMutator.mutate(newArgs, definitionHasStatementInputs);

                // Mutate each procedure call
                List<Block> disconnectedBlocks = new ArrayList<Block>();
                for (Block procRef : mProcedureReferences.get(procedureName)) {
                    ProcedureCallMutator callMutator = (ProcedureCallMutator) procRef.getMutator();
                    assert callMutator != null;
                    int oldArgCount = callMutator.getArgumentList().size();
                    Block[] oldValues = new Block[oldArgCount];
                    disconnectedBlocks.clear();

                    // Disconnect prior value blocks
                    for (int i = 0; i < oldArgCount; ++i) {
                        Input argInput = callMutator.getArgumentInput(i);
                        Block valueBlock = argInput.getConnectedBlock();
                        if (valueBlock != null) {
                            oldValues[i] = valueBlock;
                            mController.extractBlockAsRoot(valueBlock);
                            disconnectedBlocks.add(valueBlock);
                        }
                    }

                    callMutator.mutate(procedureName, newArgs);

                    // Reconnect any blocks to original inputs
                    for (int i = 0; i < newArgCount; ++i) {
                        int originalIndex = argUpdates.get(i).getOriginalIndex();
                        Block originalValue = (originalIndex == ArgumentUpdate.NEW_ARGUMENT) ?
                                null : oldValues[originalIndex];
                        if (originalValue != null) {
                            Input argInput = callMutator.getArgumentInput(i);
                            mController.connect(
                                    argInput.getConnection(),
                                    originalValue.getOutputConnection());
                            disconnectedBlocks.remove(originalValue);
                        }
                    }

                    // TODO: Bump disconnected blocks.
                    //       Why does mController.bumpBlock() need a second Connection parameter?
                }
            }
        });
    }

    public boolean hasReferenceWithReturn() {
        return mCountOfReferencesWithReturn > 0;
    }

    protected void notifyObservers() {
        for (DataSetObserver observer : mObservers) {
            observer.onChanged();
        }
    }

    private static String getProcedureName(Block block) {
        Mutator mutator = block.getMutator();
        if (!(mutator instanceof AbstractProcedureMutator)) {
            throw new IllegalArgumentException("Block does not contain procedure mutator.");
        }
        return ((AbstractProcedureMutator) mutator).getProcedureName();
    }

    private static void setProcedureName(Block block, String newName) {
        Mutator mutator = block.getMutator();
        if (!(mutator instanceof AbstractProcedureMutator)) {
            throw new IllegalArgumentException("Block does not contain procedure mutator.");
        }
        ((AbstractProcedureMutator) mutator).setProcedureName(newName);
    }
}
