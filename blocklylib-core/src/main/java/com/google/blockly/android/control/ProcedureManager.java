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

import android.database.Observable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;

import com.google.blockly.android.BuildConfig;
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
import java.util.Locale;
import java.util.Map;

/**
 * Manages procedure definitions, references and names.
 */
public class ProcedureManager extends Observable<ProcedureManager.Observer> {
    private static final String TAG = "ProcedureManager";

    public static final String NAME_FIELD = "name";

    /** Callback listener for changes regarding procedures on the workspace. */
    public interface Observer {
        /** Called when a procedure is added to the workspace. */
        void onProcedureBlockAdded(String procedureName, Block block);

        /** Called when a procedure is removed from the workspace. */
        void onProcedureBlocksRemoved(String procedureName, List<Block> blocks);

        /** Called when a procedure name or definition changes. */
        void onProcedureMutated(ProcedureInfo oldProcInfo, ProcedureInfo newProcInfo);

        /** Called when all workspace procedures are removed. */
        void onClear();
    }

    /**
     * Describes the re-indexing of argument order during a procedure mutation. Used to ensure
     * values of arguments that are present both before and after are reconnected to the right
     * input.
     */
    public static class ArgumentIndexUpdate {
        public final int before;
        public final int after;

        /**
         * Constructor for a new ArgumentIndexUpdate.
         * @param before The argument's index before the change.
         * @param after The argument's index after the change.
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

    // TODO: These maps use the same key. Merge into a single data structure. Better yet, turn the
    //       NameManager into a map-like structure, mapping to ProcedureInfo and VariableInfo.
    /** Lists of all procedure calling blocks, keyed by the canonized name string. */
    private final ArrayMap<String, List<Block>> mProcedureReferences = new ArrayMap<>(); // Set?
    /** Procedure definition blocks, keyed by the canonized name string. */
    private final ArrayMap<String, Block> mProcedureDefinitions = new ArrayMap<>();
    private final NameManager mProcedureNameManager = new NameManager.ProcedureNameManager();

    // Used to determine the visibility of procedures_ifreturn block in the ProcedureCustomCategory.
    private int mCountOfDefinitionsWithReturn = 0;

    /**
     * Constructor for a new ProcedureManager.
     * @param controller The controller managing the provided workspace.
     * @param workspace The workspace represented (passed in separately from the controller
     *                  because the workspace constructor probably hasn't finished yet).
     */
    public ProcedureManager(BlocklyController controller, Workspace workspace) {
        mController = controller;
        mVariableNameManager = workspace.getVariableNameManager();
    }

    /**
     * If the block is a procedure definition or procedure call/reference, it returns the name of
     * the procedure.
     * @param block The block in question.
     * @return The name of the procedure defined or referenced by the block, if it is a procedure
     *         block. Otherwise, null.
     */
    @Nullable
    public static String getProcedureName(Block block) {
        Mutator mutator = block.getMutator();
        boolean isProcedure = mutator instanceof AbstractProcedureMutator;
        return isProcedure ? ((AbstractProcedureMutator) mutator).getProcedureName() : null;
    }

    /**
     * Determines if a block is procedure call.
     * @param block The block in question.
     * @return True, if the block type is a recognized type of procedure call.
     */
    public static boolean isReference(Block block) {
        return block.getMutator() instanceof ProcedureCallMutator;
    }

    /**
     * Determines if a block is procedure definition.
     * @param block The block in question.
     * @return True, if the block type is a recognized type of procedure definition.
     */
    public static boolean isDefinition(Block block) {
        return block.getMutator() instanceof ProcedureDefinitionMutator;
    }

    /**
     * If the block is a procedure block, this getProcedureArguments{@code getProcedureArguments}
     * returns the argument list. Otherwise, it returns null.
     *
     * @param block The block queried.
     * @return The list of argument for defined or referenced by this block, if it is a procedure
     *         block. Otherwise null;
     */
    public static @Nullable List<String> getProcedureArguments(Block block) {
        Mutator mutator = block.getMutator();
        if (mutator instanceof AbstractProcedureMutator) {
            return ((AbstractProcedureMutator) mutator).getArgumentNameList();
        } else {
            return null;
        }
    }

    /**
     * @return All the registered procedure definition blocks, keyed by canonical procedure name.
     */
    public Map<String, Block> getDefinitionBlocks() {
        return Collections.unmodifiableMap(mProcedureDefinitions);
    }

    public List<Block> getReferences(String procedureName) {
        String canonical = mProcedureNameManager.makeCanonical(procedureName);
        return mProcedureReferences.get(canonical);
    }

    public boolean containsDefinition(String procedureName) {
        String canonical = mProcedureNameManager.makeCanonical(procedureName);
        return mProcedureDefinitions.containsKey(canonical);
    }

    public boolean containsDefinition(Block block) {
        String procName = getProcedureNameOrFail(block);
        String canonicalProcName = mProcedureNameManager.makeCanonical(procName);
        return mProcedureDefinitions.containsKey(canonicalProcName);
    }

    /**
     * @param block The block being referenced.
     *
     * @return True if the block is referenced one or more times.
     */
    public boolean hasReferences(Block block) {
        String procName = getProcedureNameOrFail(block);
        String canonical = mVariableNameManager.makeCanonical(procName);
        List<Block> procedureCallingBlocks = mProcedureReferences.get(canonical);
        return (procedureCallingBlocks != null && !procedureCallingBlocks.isEmpty());
    }

    /**
     * Removes all tracked procedures from this manager, and notifies all
     * {@link Observer Observers}.
     */
    public void clear() {
        mProcedureDefinitions.clear();
        mProcedureReferences.clear();
        mProcedureNameManager.clear();
        mCountOfDefinitionsWithReturn = 0;

        int obsCount = mObservers.size();
        for (int i = 0; i < obsCount; ++i) {
            mObservers.get(i).onClear();
        }
    }

    /**
     * Adds a reference to a procedure.
     *
     * @param block The reference to add.
     *
     * @throws IllegalStateException if the referenced procedure has not been defined.
     */
    public void addReference(Block block) {
        String procedureName = getProcedureNameOrFail(block);
        String canonical = mProcedureNameManager.makeCanonical(procedureName);
        List<Block> callingBlocks = mProcedureReferences.get(canonical);
        if (callingBlocks != null) {
            callingBlocks.add(block);
            if (block.getType().equals(CALL_WITH_RETURN_BLOCK_TYPE)) {
                ++mCountOfDefinitionsWithReturn;
            }

            int obsCount = mObservers.size();
            for (int i = 0; i < obsCount; ++i) {
                mObservers.get(i).onProcedureBlockAdded(procedureName, block);
            }
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
        String procedureName = getProcedureNameOrFail(block);
        if (mProcedureReferences.containsKey(procedureName)) {
            mProcedureReferences.get(procedureName).remove(block);
            if (block.getType().equals(CALL_WITH_RETURN_BLOCK_TYPE)) {
                --mCountOfDefinitionsWithReturn;

                if (BuildConfig.DEBUG && mCountOfDefinitionsWithReturn < 0) {
                    throw new AssertionError(String.format(Locale.ROOT,
                            "mCountOfDefinitionsWithReturn (%d) must not be negative",
                            mCountOfDefinitionsWithReturn));
                }
            }

            if (!mObservers.isEmpty()) {
                List<Block> references = Collections.singletonList(block);
                int obsCount = mObservers.size();
                for (int i = 0; i < obsCount; ++i) {
                    mObservers.get(i).onProcedureBlocksRemoved(procedureName, references);
                }
            }
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
        String procedureName = getProcedureNameOrFail(block);
        String canonicalProcName = mProcedureNameManager.makeCanonical(procedureName);
        if (mProcedureDefinitions.get(canonicalProcName) == block) {
            throw new IllegalStateException("Tried to add the same block definition twice");
        }
        if (mProcedureNameManager.contains(procedureName)) {
            procedureName = mProcedureNameManager.generateUniqueName(procedureName, false);
            setProcedureName(block, procedureName);
            canonicalProcName = mProcedureNameManager.makeCanonical(procedureName);
        }
        mProcedureNameManager.addName(procedureName);
        mProcedureDefinitions.put(canonicalProcName, block);
        mProcedureReferences.put(canonicalProcName, new ArrayList<Block>());

        int obsCount = mObservers.size();
        for (int i = 0; i < obsCount; ++i) {
            mObservers.get(i).onProcedureBlockAdded(procedureName, block);
        }
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
        String procedureName = getProcedureNameOrFail(block);
        String canonical = mProcedureNameManager.makeCanonical(procedureName);
        if (mProcedureDefinitions.containsKey(canonical)) {
            List<Block> references = mProcedureReferences.get(canonical);
            mProcedureReferences.remove(canonical);
            mProcedureDefinitions.remove(canonical);
            mProcedureNameManager.remove(canonical);

            if (!mObservers.isEmpty()) {
                List<Block> blocks = new ArrayList<>(references.size() + 1);
                blocks.add(block);
                blocks.addAll(references);
                int obsCount = mObservers.size();
                for (int i = 0; i < obsCount; ++i) {
                    mObservers.get(i).onProcedureBlocksRemoved(procedureName, blocks);
                }
            }

            return references;
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
     * @throws IllegalStateException If updatedProcedureInfo fails to serialize or deserialize.
     */
    public void mutateProcedure(final @NonNull String originalProcedureName,
                                final @NonNull ProcedureInfo updatedProcedureInfo,
                                final @Nullable List<ArgumentIndexUpdate> argIndexUpdates)
    {
        final String originalCanonical = mProcedureNameManager.makeCanonical(originalProcedureName);
        final Block definition = mProcedureDefinitions.get(originalCanonical);
        if (definition == null) {
            throw new IllegalArgumentException(
                    "Unknown procedure \"" + originalProcedureName + "\"");
        }
        final ProcedureDefinitionMutator definitionMutator =
                (ProcedureDefinitionMutator) definition.getMutator();
        final ProcedureInfo oldProcInfo = definitionMutator.getProcedureInfo();
        final String newProcedureName = updatedProcedureInfo.getProcedureName();
        final String newCanonicalName = mProcedureNameManager.makeCanonical(newProcedureName);
        final boolean isFuncRename = !originalProcedureName.equals(newProcedureName);

        final List<String> newArgs = updatedProcedureInfo.getArgumentNames();
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
                if (isFuncRename) {
                    mProcedureNameManager.remove(originalCanonical);
                    mProcedureDefinitions.remove(originalCanonical);

                    mProcedureNameManager.addName(newProcedureName);
                    mProcedureDefinitions.put(newCanonicalName, definition);
                }

                // Mutate each procedure call
                List<Block> procedureCalls = mProcedureReferences.get(originalCanonical);
                if (isFuncRename) {
                    mProcedureReferences.remove(originalCanonical);
                    mProcedureReferences.put(newCanonicalName, procedureCalls);
                }
                for (Block procRef : procedureCalls) {
                    ProcedureCallMutator callMutator =
                            (ProcedureCallMutator) procRef.getMutator();
                    assert callMutator != null;
                    int oldArgCount = callMutator.getArgumentNameList().size();
                    Block[] oldValues = new Block[oldArgCount];  // Initially all null

                    // Disconnect prior value blocks
                    for (int i = 0; i < oldArgCount; ++i) {
                        Input argInput = callMutator.getArgumentInput(i);
                        Block valueBlock = argInput.getConnectedBlock();
                        if (valueBlock != null) {
                            oldValues[i] = valueBlock;
                            mController.extractBlockAsRoot(valueBlock);
                        }
                    }

                    callMutator.mutate(updatedProcedureInfo);

                    // Reconnect any blocks to original inputs
                    if (argIndexUpdates != null) {
                        for (int i = 0; i < argIndexUpdates.size(); ++i) {
                            ArgumentIndexUpdate argumentIndexUpdate = argIndexUpdates.get(i);
                            Block originalValue = oldValues[argumentIndexUpdate.before];
                            if (originalValue != null) {
                                Input argInput =
                                        callMutator.getArgumentInput(argumentIndexUpdate.after);
                                mController.connect(
                                        argInput.getConnection(),
                                        originalValue.getOutputConnection());
                            }
                        }
                    }

                    // TODO: Bump disconnected blocks. Needs a single param bump method.
                }
            }
        });

        int obsCount = mObservers.size();
        for (int i = 0; i < obsCount; ++i) {
            mObservers.get(i).onProcedureMutated(oldProcInfo, updatedProcedureInfo);
        }
    }

    /**
     * Convenience form of {@link #mutateProcedure(String, ProcedureInfo, List)} that assumes the
     * arguments maintain their same index.
     * @param procedureBlock A procedure block
     * @param updatedProcedureInfo The info with which to update procedure mutators.
     * @throws IllegalArgumentException If the old and new argument counts do not match.
     */
    public void mutateProcedure(final @NonNull Block procedureBlock,
                                final @NonNull ProcedureInfo updatedProcedureInfo) {
        Mutator mutator = procedureBlock.getMutator();
        if (!(mutator instanceof  AbstractProcedureMutator)) {
            throw new IllegalArgumentException("procedureBlock does not have a procedure mutator.");
        }
        ProcedureInfo procInfo = ((AbstractProcedureMutator)mutator).getProcedureInfo();
        if (procInfo == null) {
            throw new IllegalStateException("No ProcedureInfo for " + mutator);
        }

        final String originalProcedureName = procInfo.getProcedureName();
        final String originalCanonicalProcName =
                mProcedureNameManager.makeCanonical(originalProcedureName);
        final Block definition = mProcedureDefinitions.get(originalCanonicalProcName);
        if (definition == null) {
            // Unregistered procedure name. Just change this one block.
            // Probably because the procedure hasn't been connected to the workspace, yet.
            ((AbstractProcedureMutator) mutator).mutate(updatedProcedureInfo);
        } else {
            int oldArgCount =
                    ((ProcedureDefinitionMutator) definition.getMutator()).getArgumentNameList().size();
            int newArgCount = updatedProcedureInfo.getArgumentNames().size();
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
            mutateProcedure(originalProcedureName,
                    updatedProcedureInfo,
                    SAME_INDICES.subList(0, i));
        }
    }

    /**
     * Returns whether the workspace includes a procedures_defreturn block. Used determine the
     * visibility of procedures_ifreturn block in the ProcedureCustomCategory.
     * @return Whether the workspace includes a procedures_defreturn block.
     */
    public boolean hasProcedureDefinitionWithReturn() {
        return mCountOfDefinitionsWithReturn > 0;
    }

    /**
     * Helper method to retrieve a required procedure name.
     * @param block The block queried.
     * @return The block's procedure name.
     * @throws IllegalArgumentException If the block does not have a procedure name.
     */
    private static @NonNull String getProcedureNameOrFail(Block block) {
        String procName = getProcedureName(block);
        if (procName == null) {
            throw new IllegalArgumentException("Block does not contain procedure name.");
        }
        return procName;
    }

    /**
     * Helper method to name or rename a procedure for a single block.
     * @param block The block to name or rename.
     * @param newName The new procedure name.
     * @throws IllegalArgumentException If the block does not have a procedure mutator.
     */
    private static void setProcedureName(Block block, String newName) {
        Mutator mutator = block.getMutator();
        if (mutator instanceof AbstractProcedureMutator) {
            ((AbstractProcedureMutator) mutator).setProcedureName(newName);
        } else {
            throw new IllegalArgumentException("Block does not contain a procedure mutator.");
        }
    }
}
