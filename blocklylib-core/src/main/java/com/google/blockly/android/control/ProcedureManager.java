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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;

import com.google.blockly.model.Block;
import com.google.blockly.model.Input;
import com.google.blockly.model.Mutator;
import com.google.blockly.model.ProcedureInfo;
import com.google.blockly.model.Workspace;
import com.google.blockly.model.mutator.AbstractProcedureMutator;
import com.google.blockly.model.mutator.ProcedureCallMutator;
import com.google.blockly.model.mutator.ProcedureDefinitionMutator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Manages procedure definitions, references and names.
 */
public class ProcedureManager extends Observable<DataSetObserver> {
    private static final String TAG = "ProcedureManager";

    public static final String NAME_FIELD = "name";

    /**
     * Describes the re-indexing of argument order during a procedure mutation. Used to ensure
     * values of arguments that are present both before and after are reconnected to the right
     * input.
     */
    public static class ArgumentIndexUpdate {
        public final int before;
        public final int after;

        /**
         *
         * @param before
         * @param after
         */
        public ArgumentIndexUpdate(int before, int after) {
            this.before = before;
            this.after = after;
        }
    }

    public static final String PROCEDURE_NAME_FIELD = "NAME";

    public static final String DEFINE_NO_RETURN_BLOCK_TYPE = "procedures_defnoreturn";
    public static final String DEFINE_WITH_RETURN_BLOCK_TYPE = "procedures_defreturn";
    public static final String CALL_NO_RETURN_BLOCK_TYPE = "procedures_callnoreturn";
    public static final String CALL_WITH_RETURN_BLOCK_TYPE = "procedures_callreturn";

    private static final ArrayList<ArgumentIndexUpdate> SAME_INDICES = new ArrayList<>();

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
     * Updates all blocks related to a specific procedure with respect to name, arguments, and
     * whether the definition can contain a statement sequence. If any of the optional arguments are
     * null, the existing values from the blocks are used.
     *
     * @param originalProcedureName The name of the procedure, before this method.
     * @param updatedProcedureInfo The info with which to update procedure mutators.
     * @param argIndexUpdates A list of mappings from original argument index to new index.
     * @throws IllegalArgumentException If any {@code originalProcedureName} is not found,
     *                                  if {@code optUpdatedProcedureName} is not a valid procedure
     *                                  name, or if argument name is invalid.
     */
    public void mutateProcedure(final @NonNull String originalProcedureName,
                                final @NonNull ProcedureInfo updatedProcedureInfo,
                                final @Nullable List<ArgumentIndexUpdate> argIndexUpdates)
    {
        final Block definition = mProcedureDefinitions.get(originalProcedureName);
        final ProcedureDefinitionMutator definitionMutator =
                (ProcedureDefinitionMutator) definition.getMutator();
        assert definitionMutator != null;
        if (definition == null) {
            throw new IllegalArgumentException(
                    "Unknown procedure \"" + originalProcedureName + "\"");
        }
        final String newProcedureName = updatedProcedureInfo.getProcedureName();
        final boolean isRename = originalProcedureName.equals(newProcedureName);
        if (isRename && !mProcedureNameManager.isValidName(newProcedureName)) {
            throw new IllegalArgumentException(
                    "Invalid procedure name \"" + newProcedureName + "\"");
        }

        final List<String> newArgs = updatedProcedureInfo.getArguments();
        final int newArgCount = newArgs.size();
        for (int i = 0; i < newArgCount; ++i) {
            String argName = newArgs.get(i);
            if (!mVariableNameManager.isValidName(argName)) {
                throw new IllegalArgumentException("Invalid variable name \"" + argName + "\" "
                        + "(argument #" + i + " )");
            }
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

                definitionMutator.mutate(updatedProcedureInfo);
                if (isRename) {
                    mProcedureDefinitions.remove(originalProcedureName);
                    mProcedureDefinitions.put(newProcedureName, definition);
                }

                // Mutate each procedure call
                List<Block> disconnectedBlocks = new ArrayList<Block>(newArgCount);
                List<Block> procedureCalls = mProcedureReferences.get(originalProcedureName);
                for (Block procRef : procedureCalls) {
                    ProcedureCallMutator callMutator =
                            (ProcedureCallMutator) procRef.getMutator();
                    assert callMutator != null;
                    int oldArgCount = callMutator.getArgumentList().size();
                    Block[] oldValues = new Block[oldArgCount];
                    disconnectedBlocks.clear();

                    if (hasNewArgs) {
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
                    }

                    callMutator.mutate(finalProcedureName, finalArgs);

                    if (hasNewArgs) {
                        // Reconnect any blocks to original inputs
                        for (int i = 0; i < newArgCount; ++i) {
                            int originalIndex = optArgUpdates.get(i).getOriginalIndex();
                            Block originalValue =
                                    (originalIndex == ArgumentUpdate.NEW_ARGUMENT) ?
                                    null : oldValues[originalIndex];
                            if (originalValue != null) {
                                Input argInput = callMutator.getArgumentInput(i);
                                mController.connect(
                                        argInput.getConnection(),
                                        originalValue.getOutputConnection());
                                disconnectedBlocks.remove(originalValue);
                            }
                        }
                    }

                    // TODO: Bump disconnected blocks. Needs a single param bump method.
                }
            }
        });
    }

    /**
     * Convenience form of {@link #mutateProcedure(String, ProcedureInfo, List)} that assumes the
     * arguments maintain their same index.
     * @param originalProcedureName The name of the procedure, before this method.
     * @param updatedProcedureInfo The info with which to update procedure mutators.
     * @throws IllegalArgumentException If the old and new argument counts do not match.
     */
    public void mutateProcedure(final @NonNull String originalProcedureName,
                                final @NonNull ProcedureInfo updatedProcedureInfo) {
        final Block definition = mProcedureDefinitions.get(originalProcedureName);
        int oldArgCount =
                ((ProcedureDefinitionMutator) definition.getMutator()).getArgumentList().size();
        int newArgCount = updatedProcedureInfo.getArguments().size();
        if (newArgCount != oldArgCount) {
            throw new IllegalArgumentException(
                    "Cannot map argument index map with differing argument counts. "+
                    "(" + oldArgCount + " -> " + newArgCount + ")" );
        }
        int i = SAME_INDICES.size();
        if (i < newArgCount) {
            while (i < newArgCount) {
                SAME_INDICES.add(new ArgumentIndexUpdate(i, i));
                ++i;
            }
        }
        mutateProcedure(originalProcedureName, updatedProcedureInfo, SAME_INDICES.subList(0, i));
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
