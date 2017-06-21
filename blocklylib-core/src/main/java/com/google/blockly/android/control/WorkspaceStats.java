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

import android.support.annotation.Nullable;
import android.support.v4.util.ArraySet;

import com.google.blockly.model.Block;
import com.google.blockly.model.Connection;
import com.google.blockly.model.Field;
import com.google.blockly.model.FieldVariable;
import com.google.blockly.model.Input;
import com.google.blockly.model.ProcedureInfo;
import com.google.blockly.model.VariableInfo;
import com.google.blockly.utils.BlockLoadingException;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Tracks information about the Workspace that we want fast access to.
 */
public class WorkspaceStats {
    // Maps from variable/procedure names to the blocks/fields where they are referenced.
    private final VariableNameManager mVariableNameManager;
    private final ProcedureManager mProcedureManager;
    private final ConnectionManager mConnectionManager;

    private final Field.Observer mVariableObserver = new Field.Observer() {
        @Override
        public void onValueChanged(Field field, String oldVar, String newVar) {
            if (oldVar != null) {
                VariableInfoImpl usages = getVarInfoImpl(oldVar, /* create */ false);
                usages.removeField((FieldVariable) field);
            }
            if (newVar != null) {
                VariableInfoImpl usages = getVarInfoImpl(newVar, /* create */ true);
                usages.addField((FieldVariable) field);
            }
        }
    };

    private final ProcedureManager.Observer mProcedureObserver = new ProcedureManager.Observer() {
        @Override
        public void onProcedureBlockAdded(String procedureName, Block block) {
            List<String> args = mProcedureManager.getProcedureArguments(block);
            if (args != null) {
                for (String arg : args) {
                    VariableInfoImpl info = getVarInfoImpl(arg, true);
                    info.addProcedure(procedureName);
                }
            }
        }

        @Override
        public void onProcedureBlocksRemoved(String procedureName, Set<Block> blocks) {
            for (Block block : blocks) {
                List<String> args = mProcedureManager.getProcedureArguments(block);
                if (args != null) {
                    for (String arg : args) {
                        VariableInfoImpl info = getVarInfoImpl(procedureName, false);
                        if (info != null) {
                            info.removeProcedure(procedureName);
                        }
                    }
                }
            }
        }

        @Override
        public void onProcedureMutated(ProcedureInfo oldInfo, ProcedureInfo newInfo) {
            // TODO: This clearly doesn't do what it was intended to do with respect to
            //       argument-only changes (when names don't change).  Need to rewrite and test.

            String oldName = oldInfo.getProcedureName();
            String newName = newInfo.getProcedureName();
            if (!newName.equals(oldName)) {
                int varCount = mVariableNameManager.size();
                for (int i = 0; i < varCount; ++i) {
                    VariableInfoImpl varInfo =
                            (VariableInfoImpl) mVariableNameManager.entryAt(i).mValue;
                    if (varInfo.removeProcedure(oldName)) {
                        varInfo.addProcedure(newName);
                    }
                }
            }
        }

        @Override
        public void onClear() {
            int varCount = mVariableNameManager.size();
            for (int i = 0; i < varCount; ++i) {
                ((VariableInfoImpl) mVariableNameManager.entryAt(i).mValue).mProcedures = null;
            }
        }
    };

    private final List<Connection> mTempConnecitons = new ArrayList<>();

    public WorkspaceStats(ProcedureManager procedureManager,
                          ConnectionManager connectionManager) {
        mVariableNameManager = new VariableNameManagerImpl();
        mProcedureManager = procedureManager;
        mConnectionManager = connectionManager;

        // TODO: Register arguments of existing procedures. Currently assuming none.
        mProcedureManager.registerObserver(mProcedureObserver);
    }

    public VariableNameManager getVariableNameManager() {
        return mVariableNameManager;
    }

    @Nullable
    public VariableInfo getVariableInfo(String varName) {
        return getVarInfoImpl(varName, false);
    }

    /**
     * Walks through a block and records all Connections, variable references, procedure
     * definitions and procedure calls.
     *
     * @param block The block to inspect.
     * @param recursive Whether to recursively collect stats for all descendants of the current
     *                  block.
     */
    public void collectStats(Block block, boolean recursive) throws BlockLoadingException {
        collectStats(Collections.singletonList(block), recursive);
    }

    /**
     * Walks through a list of block and records all Connections, variable references, procedure
     * definitions and procedure calls.
     *
     * @param blocks The list of blocks to inspect.
     * @param recursive Whether to recursively collect stats for all descendants of the current
     *                  block.
     */
    public void collectStats(List<Block> blocks, boolean recursive) throws BlockLoadingException {
        int count = blocks.size();

        // Assuming procedure definitions can only occur as root blocks.
        // Register all definitions before potentially processing any calls to those procedures.
        for (int i = 0; i < count; ++i) {
            Block block = blocks.get(i);
            if (ProcedureManager.isDefinition(block)) {
                List<String> procedureArgs = ProcedureManager.getProcedureArguments(block);
                if (procedureArgs != null) {
                    String procedureName = ProcedureManager.getProcedureName(block);
                    for (String arg : procedureArgs) {
                        getVarInfoImpl(arg, true).addProcedure(procedureName);
                    }
                }

                mProcedureManager.addDefinition(block);
            }
        }


        for (int i = 0; i < count; ++i) {
            Block block = blocks.get(i);
            collectConnectionStatsAndProcedureReferences(block, recursive);
        }
    }

    /**
     * Clear state about variables, procedures, and connections.
     * These changes will be reflected in the externally owned connection and procedure manager.
     */
    public void clear() {
        mProcedureManager.clear();
        mVariableNameManager.clear();
        mConnectionManager.clear();
    }

    /**
     * Remove all the stats associated with this block and its descendents. This will remove all
     * connections from the ConnectionManager and dereference any variables in the tree.
     *
     * @param block The starting block to cleanup stats for.
     */
    public void cleanupStats(Block block) {
        block.getAllConnections(mTempConnecitons);
        for (int i = 0; i < mTempConnecitons.size(); i++) {
            mConnectionManager.removeConnection(mTempConnecitons.get(i));
        }
        mTempConnecitons.clear();
        List<Input> inputs = block.getInputs();
        for (int i = 0; i < inputs.size(); i++) {
            Input input = inputs.get(i);
            List<Field> fields = input.getFields();
            for (int j = 0; j < fields.size(); j++) {
                Field field = fields.get(j);
                if (field instanceof FieldVariable) {
                    removeVarField((FieldVariable) field);
                }
            }
            if (input.getConnection() != null && input.getConnection().getTargetBlock() != null) {
                cleanupStats(input.getConnection().getTargetBlock());
            }
        }
        if (block.getNextBlock() != null) {
            cleanupStats(block.getNextBlock());
        }
    }

    private void removeVarField(FieldVariable field) {
        VariableInfoImpl info = getVarInfoImpl(field.getVariable(), false);
        if (info != null) {
            info.removeField(field);
        }
        field.unregisterObserver(mVariableObserver);
    }

    private void collectConnectionStatsAndProcedureReferences(Block block, boolean recursive)
            throws BlockLoadingException
    {
        if (ProcedureManager.isReference(block)) {
            String procName = ProcedureManager.getProcedureName(block);
            if (mProcedureManager.containsDefinition(block)) {
                mProcedureManager.addReference(block);
            } else {
                throw new BlockLoadingException("Undefined procedure name \"" + procName + "\" "
                        + "in reference block " + block);
            }
        }

        List<Input> inputs = block.getInputs();
        int inputCount = inputs.size();
        for (int j = 0; j < inputCount; j++) {
            Input input = block.getInputs().get(j);
            List<Field> fields = input.getFields();
            int fieldCount = fields.size();
            for (int k = 0; k < fieldCount; ++k) {
                Field field = fields.get(k);
                if (field instanceof FieldVariable) {
                    FieldVariable varField = (FieldVariable) field;
                    String varName = varField.getVariable();
                    getVarInfoImpl(varName, true).addField(varField);
                    varField.registerObserver(mVariableObserver);
                }
            }
            collectConnectionStats(input.getConnection(), recursive);
        }

        collectConnectionStats(block.getNextConnection(), recursive);
        // Don't recurse on outputs or previous connections--that's effectively walking back up
        // the tree.
        collectConnectionStats(block.getPreviousConnection(), false);
        collectConnectionStats(block.getOutputConnection(), false);
    }

    private void collectConnectionStats(Connection conn, boolean recursive)
            throws BlockLoadingException {
        if (conn != null) {
            mConnectionManager.addConnection(conn);
            if (recursive) {
                Block recursiveTarget = conn.getTargetBlock();
                if (recursiveTarget != null) {
                    collectConnectionStatsAndProcedureReferences(recursiveTarget, true);
                }
            }
        }
    }

    private VariableInfoImpl getVarInfoImpl(String varName, boolean create) {
        VariableInfoImpl varInfo = (VariableInfoImpl) mVariableNameManager.getValueOf(varName);
        if (varInfo == null && create) {
            varInfo = new VariableInfoImpl(varName);
            mVariableNameManager.put(varName, varInfo);
        }
        return varInfo;
    }

    /**
     * Attempts to add a variable to the workspace.
     * @param requestedName The preferred variable name. Usually the user name.
     * @param allowRename Whether the variable name can be renamed.
     * @return The name that was added, if any. May be null if renaming is not allowed.
     */
    @Nullable
    public String addVariable(String requestedName, boolean allowRename) {
        String finalName = mVariableNameManager.generateUniqueName(requestedName);
        if (requestedName.equals(finalName) || allowRename) {
            mVariableNameManager.put(finalName, new VariableInfoImpl(finalName));
            return finalName;
        }
        return null;  // Name already in use, and no renames allowed.
    }

    private class VariableInfoImpl implements VariableInfo {
        /** Canonical variable name. */
        final String mCanonicalName;
        /** Display name */
        final String mDisplayName;
        /** FieldVariables that are set to the variable. */
        ArrayList<WeakReference<FieldVariable>> mFields = null;
        /** Procedures that use the variable as an argument. */
        ArraySet<String> mProcedures = null;

        private VariableInfoImpl(String displayName) {
            mDisplayName = displayName;
            mCanonicalName = mVariableNameManager.makeCanonical(displayName);
        }

        @Override
        public String getDisplayName() {
            return mDisplayName;
        }

        @Override
        public int getUsageCount() {
            return (mFields == null ? 0 : mFields.size())
                    + (mProcedures == null ? 0 : mProcedures.size());
        }

        @Override
        public ArraySet<String> getProcedureNames() {
            return new ArraySet<>(mProcedures);
        }

        @Override
        public void setUseAsProcedureArgument(String procedureName) {
            if (mProcedures == null) {
                mProcedures = new ArraySet<>();
            }
            mProcedures.add(procedureName.toLowerCase());
        }

        @Override
        public void removeUseAsProcedureArgument(String procedureName) {
            mProcedures.remove(procedureName.toLowerCase());
            if (mProcedures.isEmpty()) {
                mProcedures = null;
            }
        }

        void addField(FieldVariable newField) {
            if (mFields == null) {
                mFields = new ArrayList<>();
                mFields.add(new WeakReference<>(newField));
                return;
            }

            // Search for existing reference to avoid duplicates
            int count = mFields.size();
            int i = 0;
            while (i < count) {
                FieldVariable field = mFields.get(i).get();
                if (field == newField) {
                    return;  // Already present.
                }
                if (field == null) {
                    mFields.remove(i);
                    --count;
                    continue;  // Don't increment i
                }
                ++i;
            }
            // Not found. Add the new field.
            mFields.add(new WeakReference(newField));
        }

        boolean removeField(FieldVariable fieldToRemove) {
            if (mFields == null) {
                return false;
            }
            int count = mFields.size();
            int i = 0;
            while (i < count) {
                FieldVariable field = mFields.get(i).get();
                if (field == null) {
                    mFields.remove(i);
                    continue;  // Don't increment i
                }
                if (field == fieldToRemove) {
                    mFields.remove(i);
                    return true;
                }
                ++i;
            }
            if (mFields.isEmpty()) {
                mFields = null;
            }
            return false;
        }

        void addProcedure(String procedureName) {
            if (mProcedures == null) {
                mProcedures = new ArraySet<>();
            }
            mProcedures.add(procedureName);
        }

        boolean removeProcedure(String procedureName) {
            if (mProcedures == null) {
                return false;
            }
            boolean foundAndRemoved = mProcedures.remove(procedureName);
            if (mProcedures.isEmpty()) {
                mProcedures = null;
            }
            return foundAndRemoved;
        }

        public boolean isProcedureArgument() {
            return mProcedures != null && !mProcedures.isEmpty();
        }

        @Override
        public ArraySet<FieldVariable> getFields() {
            if (mFields == null) {
                return new ArraySet<>(0);
            }
            int count = mFields.size();
            ArraySet<FieldVariable> fields = new ArraySet<>(mFields.size());

            int i = 0;
            while (i < count) {
                FieldVariable field = mFields.get(i).get();
                if (field == null) {
                    mFields.remove(i);
                    --count;
                    // Don't increment i
                } else {
                    fields.add(field);
                    ++i;
                }
            }
            return fields;
        }
    }

    /**
     * The NameManager for variable names.
     */
    public class VariableNameManagerImpl extends VariableNameManager<VariableInfoImpl> {
        @Override
        protected VariableInfoImpl newVariableInfo(String varName) {
            return new VariableInfoImpl(varName);
        }
    }
}
