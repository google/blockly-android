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
import android.support.v4.util.ArraySet;
import android.support.v4.util.SimpleArrayMap;
import android.util.Log;

import com.google.blockly.android.BuildConfig;
import com.google.blockly.model.Block;
import com.google.blockly.model.Input;
import com.google.blockly.model.Mutator;
import com.google.blockly.model.ProcedureInfo;
import com.google.blockly.model.Workspace;
import com.google.blockly.model.mutator.AbstractProcedureMutator;
import com.google.blockly.model.mutator.ProcedureCallMutator;
import com.google.blockly.model.mutator.ProcedureDefinitionMutator;
import com.google.blockly.utils.BlockLoadingException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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
        void onProcedureBlocksRemoved(String procedureName, Set<Block> blocks);

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

    private class ProcedureBlocks {
        String mName;
        Block mDefinition;
        final ArraySet<Block> mReferences = new ArraySet<>();

        /**
         * Constructs a ProcedureBlocks from the provided definition.
         * @param definitionBlock The block defining the procedure.
         */
        ProcedureBlocks(Block definitionBlock) {
            mName = getProcedureName(definitionBlock);
            mDefinition = definitionBlock;
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

    // TODO: Make NameManager a map-like interface for ProcedureBlocks and VariableInfo
    private final ArrayMap<String, ProcedureBlocks> mProcedureBlocks = new ArrayMap<>();
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
     * If the block is a procedure block, this {@code getProcedureArguments} returns the argument
     * list. Otherwise, it returns null.
     *
     * @param block The block queried.
     * @return The list of argument for defined or referenced by this block, if it is a procedure
     *         block. Otherwise null;
     */
    public static @Nullable List<String> getProcedureArguments(Block block) {
        ProcedureInfo info = getProcedureInfo(block);
        return info == null ? null : info.getArgumentNames();
    }

    /**
     * If the block is a procedure block, this {@code getProcedureInfo} returns the
     * {@link ProcedureInfo}. Otherwise, it returns null.
     *
     * @param block The block queried.
     * @return The list of argument for defined or referenced by this block, if it is a procedure
     *         block. Otherwise null;
     */
    public static @Nullable ProcedureInfo getProcedureInfo(Block block) {
        Mutator mutator = block.getMutator();
        if (mutator instanceof AbstractProcedureMutator) {
            return ((AbstractProcedureMutator) mutator).getProcedureInfo();
        } else {
            return null;
        }
    }

    /**
     * Queries whether this ProcedureManager has a registered definition for the given procedure
     * name.
     * @param procedureName The name of the procedure in question.
     * @return True if a definition block for a procedure of the same name is registered. Otherwise
     *         false.
     */
    public final boolean isProcedureDefined(String procedureName) {
        return getDefinitionBlock(procedureName) != null;
    }

    /**
     * Captures the set of all registered procedure definition blocks, keyed by procedure name.
     * This map is not updated dynamically (hence the lightweight {@link SimpleArrayMap}).
     * @return All the registered procedure definition blocks, keyed by procedure name.
     */
    public SimpleArrayMap<String, Block> getDefinitionBlocks() {
        int count = mProcedureBlocks.size();
        SimpleArrayMap<String, Block> map = new SimpleArrayMap<>(count);
        for (int i = 0; i < count; ++i) {
            ProcedureBlocks procBlocks = mProcedureBlocks.valueAt(i);
            map.put(procBlocks.mName, procBlocks.mDefinition);
        }
        return map;
    }
    /**
     * @param procedureName The procedure name of the desired definition block.
     * @return The definition block for {@code procedureName}.
     */
    public @Nullable Block getDefinitionBlock(String procedureName) {
        String canonical = mProcedureNameManager.makeCanonical(procedureName);
        ProcedureBlocks procBlocks = mProcedureBlocks.get(canonical);
        return procBlocks == null ? null : procBlocks.mDefinition;
    }

    /**
     * Queries whether a procedure block contains a matching registered definition block. Similiar
     * to {@link #isProcedureDefined(String)}, except it takes in a procedure block. If the block is
     * a procedure definition, if ensure the block is the same registered definition block.
     *
     * @param procedureBlock A block for the procedure in question, either definition or reference.
     * @return True if the block is the registered definition block for the procedure, or it is a
     *         non-definition procedure block (i.e., reference) to a procedure of the same name.
     *         Otherwise false.
     * @throws IllegalArgumentException If {@code procedureBlock} does not contain a procedure
     *         definition or reference mutator.
     */
    public boolean containsDefinition(Block procedureBlock) {
        String procedureName = getProcedureNameOrFail(procedureBlock);
        String canonical = mProcedureNameManager.makeCanonical(procedureName);
        ProcedureBlocks procBlocks = mProcedureBlocks.get(canonical);
        if (procBlocks == null) {
            return false;
        }
        if (isDefinition(procedureBlock)) {
            return procBlocks.mDefinition == procedureBlock;
        } else {
            return checkProcedureBlocksMatch(procBlocks.mDefinition, procedureBlock, false);
        }
    }

    /**
     * @param procedureDefBlock The queried procedure definition block.
     *
     * @return True if the block is referenced one or more times.
     * @throws IllegalArgumentException If {@code procedureBlock} does not contain a procedure
     *         definition or reference mutator.
     */
    public boolean isDefinitionReferenced(Block procedureDefBlock) {
        String procedureName = getProcedureNameOrFail(procedureDefBlock);
        String canonical = mProcedureNameManager.makeCanonical(procedureName);
        ProcedureBlocks procBlocks = mProcedureBlocks.get(canonical);
        return procBlocks != null
                && procBlocks.mDefinition == procedureDefBlock
                && !procBlocks.mReferences.isEmpty();
    }

    /**
     * @param procedureRefBlock The queried procedure reference block.
     *
     * @return True if the block is referenced one or more times.
     * @throws IllegalArgumentException If {@code procedureBlock} does not contain a procedure
     *         definition or reference mutator.
     */
    public boolean isReferenceRegistered(Block procedureRefBlock) {
        String procedureName = getProcedureNameOrFail(procedureRefBlock);
        String canonical = mProcedureNameManager.makeCanonical(procedureName);
        ProcedureBlocks procBlocks = mProcedureBlocks.get(canonical);
        return procBlocks != null && procBlocks.mReferences.contains(procedureRefBlock);
    }

    /**
     * Removes all tracked procedures from this manager, and notifies all
     * {@link Observer Observers}.
     */
    public void clear() {
        mProcedureBlocks.clear();
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
     * @param procedureReferenceBlock The reference block to add.
     *
     * @throws BlockLoadingException If {@code procedureBlock} does not contain a procedure
     *         reference mutator or does not have a defined procedure name.
     * @throws BlockLoadingException If the referenced procedure has not been defined.
     */
    public void addReference(Block procedureReferenceBlock) throws BlockLoadingException {
        if (!isReference(procedureReferenceBlock)) {
            throw new BlockLoadingException(
                    "Block is not a procedure reference: " + procedureReferenceBlock);
        }
        String procedureName = getProcedureNameOrFail(procedureReferenceBlock);
        String canonical = mProcedureNameManager.makeCanonical(procedureName);
        ProcedureBlocks procBlocks = mProcedureBlocks.get(canonical);
        if (procBlocks == null) {
            throw new BlockLoadingException(
                    "Tried to add a reference to procedure \"" + procedureName
                            + "\" that has not been defined."
                            + "\n\tReference block: " + procedureReferenceBlock);
        }
        validateProcedureBlocksMatch(procBlocks.mDefinition, procedureReferenceBlock, false);
        // TODO: validate definition & reference ProcedureInfo match
        procBlocks.mReferences.add(procedureReferenceBlock);
    }

    protected boolean checkProcedureBlocksMatch(
            Block definition, Block reference, boolean checkName) {
        try {
            // Testing with try/throw/catch, instead of testing with conditionals,
            // so that the stack trace to validateProcedureBlocksMatch(..) is maintained when
            // that method is called directly.
            validateProcedureBlocksMatch(definition, reference, checkName);
            return true;
        } catch (BlockLoadingException e) {
            Log.d(TAG, "checkProcedureBlocksMatch() failed: " + e.getMessage());
            return false;
        }
    }

    protected void validateProcedureBlocksMatch(
            Block definition, Block reference, boolean checkName)
            throws BlockLoadingException
    {
        String definitionName = getProcedureName(definition);
        if (checkName) {
            if (definitionName == null) {
                throw new BlockLoadingException(
                        "Definition is missing procedure name: " + reference);
            }
            String canonicalDefName = mProcedureNameManager.makeCanonical(definitionName);
            String referenceName = getProcedureName(reference);
            if (referenceName == null) {
                throw new BlockLoadingException(
                        "Reference is missing procedure name: " + reference);
            }
            String canonicalRefName = mProcedureNameManager.makeCanonical(referenceName);
            if (!canonicalDefName.equals(canonicalRefName)) {
                throw new BlockLoadingException(
                        "Definition name \"" + definitionName + "\" does not match "
                        + "reference name \"" + referenceName + "\"");
            }
        }
        ProcedureInfo defInfo = getProcedureInfo(definition);
        ProcedureInfo refInfo = getProcedureInfo(reference);
        if (defInfo != refInfo) {
            // Validate arguments
            List<String> defArgs = defInfo.getArgumentNames();
            List<String> refArgs = refInfo.getArgumentNames();
            int defArgCount = defArgs == null ? 0 : defArgs.size();
            int refArgCount = refArgs == null ? 0 : refArgs.size();
            if (refArgCount > defArgCount) {  // Allow ref args to be less than the definition args
                throw new BlockLoadingException(
                        "Definition args "
                        + (defArgs == null ? "(null)" : Arrays.toString(defArgs.toArray()))
                        + " does not match reference args " + Arrays.toString(refArgs.toArray())
                );
            }
            for (int i = 0; i < refArgCount; ++i) {
                if (!refArgs.get(i).equals(defArgs.get(i))) {
                    throw new BlockLoadingException(
                            "Definition args " + Arrays.toString(defArgs.toArray()) + " does not "
                            + "match reference args " + Arrays.toString(refArgs.toArray()) + "."
                    );
                }
            }
        }
        String defType = definition.getType();
        boolean defHasReturn = defType.equals(DEFINE_WITH_RETURN_BLOCK_TYPE);
        String refType = reference.getType();
        boolean refHasReturn = refType.equals(CALL_WITH_RETURN_BLOCK_TYPE);
        if (defHasReturn != refHasReturn) {
            throw new BlockLoadingException(
                    "Definition block type \"" + defType + "\" does not match "
                    + "reference block type \"" + refType + "\"."
            );
        }
    }

    /**
     * Removes a reference to a procedure.
     *
     * @param referenceBlock The reference to remove.
     * @return Whether the reference was found.
     */
    public boolean removeReference(Block referenceBlock) {
        if (!isReference(referenceBlock)) {
            throw new IllegalArgumentException(
                    "Block is not a procedure reference: " + referenceBlock);
        }
        String procedureName = getProcedureNameOrFail(referenceBlock);
        String canonical = mProcedureNameManager.makeCanonical(procedureName);
        ProcedureBlocks procBlocks = mProcedureBlocks.get(canonical);
        boolean found = procBlocks != null && procBlocks.mReferences.remove(referenceBlock);
        if (found) {
            if (referenceBlock.getType().equals(CALL_WITH_RETURN_BLOCK_TYPE)) {
                --mCountOfDefinitionsWithReturn;
                assert (mCountOfDefinitionsWithReturn >= 0);
            }

            if (!mObservers.isEmpty()) {
                Set<Block> references = Collections.singleton(referenceBlock);
                int obsCount = mObservers.size();
                for (int i = 0; i < obsCount; ++i) {
                    mObservers.get(i).onProcedureBlocksRemoved(procedureName, references);
                }
            }
        }
        return found;
    }

    /**
     * Adds a block containing a procedure definition to the managed list.  If a procedure
     * by that name is already defined, creates a new unique name for the procedure and renames the
     * block.
     *
     * @param definitionBlock A block containing the definition of the procedure to add.
     * @throws IllegalArgumentException If the block is not a procedure definition.
     */
    public void addDefinition(Block definitionBlock) {
        Log.d(TAG, "addDefinition " + getProcedureName(definitionBlock) + " block "
                + Integer.toHexString(definitionBlock.hashCode()));

        if (!isDefinition(definitionBlock)) {
            throw new IllegalArgumentException("Block is not a procedure definition: " + definitionBlock);
        }
        String procedureName = getProcedureNameOrFail(definitionBlock);
        String canonicalProcName = mProcedureNameManager.makeCanonical(procedureName);
        ProcedureBlocks procBlocks = mProcedureBlocks.get(canonicalProcName);
        if (procBlocks != null && procBlocks.mDefinition == definitionBlock) {
            return;  // Nothing to do.
        }
        if (procBlocks != null || mProcedureNameManager.contains(procedureName)) {
            procedureName = mProcedureNameManager.generateUniqueName(procedureName, false);
            setProcedureName(definitionBlock, procedureName);
            canonicalProcName = mProcedureNameManager.makeCanonical(procedureName);
        }
        mProcedureNameManager.addName(canonicalProcName);
        mProcedureBlocks.put(canonicalProcName, new ProcedureBlocks(definitionBlock));

        int obsCount = mObservers.size();
        for (int i = 0; i < obsCount; ++i) {
            mObservers.get(i).onProcedureBlockAdded(procedureName, definitionBlock);
        }
    }

    /**
     * Removes the block containing the procedure definition from the manager, and removes all
     * references as well.  Returns a list of Blocks to recursively delete.
     *
     * @param procedureName The name of the procedure to remove.
     * @return A set of Blocks that referred to the procedure, including the procedure definition.
     *         Possibly empty, if no such procedure was found.
     */
    public Set<Block> removeProcedure(String procedureName) {
        String canonical = mProcedureNameManager.makeCanonical(procedureName);
        ProcedureBlocks procBlocks = mProcedureBlocks.get(canonical);
        if (procBlocks == null) {
            return Collections.emptySet();
        }
        return removeProcedureImpl(canonical, procBlocks);
    }

    /**
     * Removes the block containing the procedure definition from the manager, and removes all
     * references as well.  Returns a list of Blocks to recursively delete.
     *
     * @param procedureBlock A block referencing or defining the procedure to remove.
     * @return A set of Blocks that referred to the procedure defined by block, including the
     *         procedure definition block. May be empty if the procedure is unregistered.
     */
    public Set<Block> removeProcedure(Block procedureBlock) {
        String procedureName = getProcedureNameOrFail(procedureBlock);
        String canonical = mProcedureNameManager.makeCanonical(procedureName);
        ProcedureBlocks procBlocks = mProcedureBlocks.get(canonical);
        if (procBlocks == null) {
            // Unknown procedure
            return Collections.emptySet();
        }
        if (isDefinition(procedureBlock) && procBlocks.mDefinition != procedureBlock) {
            throw new IllegalArgumentException(
                    "Procedure definition block " + procedureBlock + " is not the registered "
                            +" definition block for procedure \"" + procBlocks.mName + "\".");
        }
        return removeProcedureImpl(canonical, procBlocks);
    }

    private Set<Block> removeProcedureImpl(String canonicalName, ProcedureBlocks procBlocks) {
        boolean removedMetadata = mProcedureBlocks.remove(canonicalName) == procBlocks;
        boolean removedName = mProcedureNameManager.remove(canonicalName);
        if(BuildConfig.DEBUG && !removedMetadata) {
            throw new AssertionError("Failed to find & remove ProcedureBlocks.");
        }
        if(BuildConfig.DEBUG && !removedName) {
            throw new AssertionError("Failed to find & remove canonical procedure name.");
        }

        ArraySet<Block> blocks = procBlocks.mReferences;
        blocks.add(procBlocks.mDefinition);
        Set<Block> resultBlocks = Collections.unmodifiableSet(blocks);
        if (!mObservers.isEmpty()) {
            int obsCount = mObservers.size();
            for (int i = 0; i < obsCount; ++i) {
                mObservers.get(i).onProcedureBlocksRemoved(procBlocks.mName, resultBlocks);
            }
        }
        return resultBlocks;
    }

    /**
     * Updates all blocks related to a specific procedure with respect to name, arguments, and
     * whether the definition can contain a statement sequence. If any of the optional arguments are
     * null, the existing values from the blocks are used.
     *
     * @param originalProcedureName The name of the procedure, before this method.
     * @param updatedProcedureInfo The info with which to update procedure mutators.
     * @param argIndexUpdates A list of mappings from original argument index to new index.
     * @return The final ProcedureInfo, which may have a different name than requested if there was
     *         a name collision with a previously defined name.
     * @throws IllegalArgumentException If any {@code originalProcedureName} is not found,
     *                                  if {@code optUpdatedProcedureName} is not a valid procedure
     *                                  name, or if argument name is invalid.
     * @throws IllegalStateException If updatedProcedureInfo fails to serialize or deserialize.
     */
    public ProcedureInfo mutateProcedure(final @NonNull String originalProcedureName,
                                         @NonNull ProcedureInfo updatedProcedureInfo,
                                         final @Nullable List<ArgumentIndexUpdate> argIndexUpdates)
    {
        final String originalCanonical = mProcedureNameManager.makeCanonical(originalProcedureName);
        final ProcedureBlocks procBlocks = mProcedureBlocks.get(originalCanonical);
        if (procBlocks == null) {
            throw new IllegalArgumentException(
                    "Unknown procedure \"" + originalProcedureName + "\"");
        }
        final Block definition = procBlocks.mDefinition;
        final AbstractProcedureMutator definitionMutator =
                ((AbstractProcedureMutator<? extends ProcedureInfo>) definition.getMutator());
        final ProcedureInfo oldProcInfo = definitionMutator.getProcedureInfo();
        String newProcedureNameRequested = updatedProcedureInfo.getProcedureName();
        Log.d(TAG, "Definition "+Integer.toHexString(definition.hashCode())+"\n\tnewProcedureNameRequested = " + newProcedureNameRequested);
        final boolean isFuncRename = !originalProcedureName.equals(newProcedureNameRequested);
        Log.d(TAG, "\tisFuncRename = " + isFuncRename);
        final String newProcedureName = !isFuncRename ? originalProcedureName
                : mProcedureNameManager.generateUniqueName(newProcedureNameRequested, true);
        Log.d(TAG, "\tActual newProcedureName = " + newProcedureName);
        final String newCanonicalName = !isFuncRename ? originalCanonical
                : mProcedureNameManager.makeCanonical(newProcedureName);
        final ProcedureInfo updatedProcedureInfoFinal =
                !isFuncRename || newProcedureName.equals(newProcedureNameRequested)
                        ? updatedProcedureInfo
                        : updatedProcedureInfo.cloneWithName(newProcedureName);

        final List<String> newArgs = updatedProcedureInfoFinal.getArgumentNames();
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

                definitionMutator.mutate(updatedProcedureInfoFinal);
                if (isFuncRename) {
                    mProcedureNameManager.remove(originalCanonical);
                    mProcedureBlocks.remove(originalCanonical);

                    procBlocks.mName = newProcedureName;

                    mProcedureNameManager.addName(newCanonicalName);
                    mProcedureBlocks.put(newCanonicalName, procBlocks);
                }

                // Mutate each procedure call
                Set<Block> procedureCalls = procBlocks.mReferences;
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

                    callMutator.mutate(updatedProcedureInfoFinal);

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

        return updatedProcedureInfoFinal;
    }

    /**
     * Convenience form of {@link #mutateProcedure(String, ProcedureInfo, List)} that assumes the
     * arguments maintain their same index.
     * @param procedureBlock A procedure block
     * @param updatedProcedureInfo The info with which to update procedure mutators.
     * @return The final ProcedureInfo, which may have a different name than requested if there was
     *         a name collision with a previously defined name.
     * @throws IllegalArgumentException If the old and new argument counts do not match.
     */
    public ProcedureInfo mutateProcedure(final @NonNull Block procedureBlock,
                                         final @NonNull ProcedureInfo updatedProcedureInfo) {
        Mutator mutator = procedureBlock.getMutator();
        if (!(mutator instanceof  AbstractProcedureMutator)) {
            throw new IllegalArgumentException("procedureBlock does not have a procedure mutator.");
        }
        ProcedureInfo procInfo = ((AbstractProcedureMutator)mutator).getProcedureInfo();
        if (procInfo == null) {
            throw new IllegalStateException("No ProcedureInfo for " + mutator);
        }

        final String originalProcedureName = getProcedureName(procedureBlock);
        final String originalCanonicalName = originalProcedureName == null ? null :
                mProcedureNameManager.makeCanonical(originalProcedureName);
        final ProcedureBlocks procBlocks = originalCanonicalName == null ? null
                : mProcedureBlocks.get(originalCanonicalName);
        if (procBlocks == null) {
            // This procedure is not (yet?) managed by the ProcedureManager.
            ((AbstractProcedureMutator) mutator).mutate(updatedProcedureInfo);
            return updatedProcedureInfo;
        }
        final Block definition = procBlocks.mDefinition;
        if (definition == null) {
            // Unregistered procedure name. Just change this one block.
            // Probably because the procedure hasn't been connected to the workspace, yet.
            ((AbstractProcedureMutator) mutator).mutate(updatedProcedureInfo);
            return updatedProcedureInfo;
        } else {
            int oldArgCount = ((ProcedureDefinitionMutator) definition.getMutator())
                    .getArgumentNameList().size();
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
            return mutateProcedure(
                    originalProcedureName, updatedProcedureInfo, SAME_INDICES.subList(0, i));
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
