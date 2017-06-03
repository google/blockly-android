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
import android.support.v4.util.SimpleArrayMap;

import com.google.blockly.model.Block;
import com.google.blockly.model.Connection;
import com.google.blockly.model.Field;
import com.google.blockly.model.FieldVariable;
import com.google.blockly.model.Input;
import com.google.blockly.model.ProcedureInfo;
import com.google.blockly.model.VariableInfo;
import com.google.blockly.utils.SimpleArraySet;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Tracks information about the Workspace that we want fast access to.
 */
public class WorkspaceStats {
    // Maps from variable/procedure names to the blocks/fields where they are referenced.
    private final SimpleArrayMap<String, VariableInfoImpl> mVariableInfoMap =
            new SimpleArrayMap<>();
    private final NameManager mVariableNameManager;
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
            VariableInfoImpl info = getVarInfoImpl(procedureName, true);
            info.addProcedure(procedureName);
        }

        @Override
        public void onProcedureBlocksRemoved(String procedureName, List<Block> blocks) {
            VariableInfoImpl info = getVarInfoImpl(procedureName, false);
            if (info != null) {
                info.removeProcedure(procedureName);
            }
        }

        @Override
        public void onProcedureMutated(ProcedureInfo oldInfo, ProcedureInfo newInfo) {
            String oldName = oldInfo.getProcedureName();
            String newName = newInfo.getProcedureName();
            if (!newName.equals(oldName)) {
                int varCount = mVariableInfoMap.size();
                for (int i = 0; i < varCount; ++i) {
                    VariableInfoImpl varInfo = mVariableInfoMap.get(i);
                    varInfo.removeProcedure(oldName);
                    varInfo.addProcedure(newName);
                }
            }
        }

        @Override
        public void onClear() {
            int varCount = mVariableInfoMap.size();
            for (int i = 0; i < varCount; ++i) {
                VariableInfoImpl varInfo = mVariableInfoMap.get(i);
                varInfo.mProcedures = null;
            }
        }
    };

    private final List<Connection> mTempConnecitons = new ArrayList<>();

    public WorkspaceStats(NameManager variableManager, ProcedureManager procedureManager,
                          ConnectionManager connectionManager) {
        mVariableNameManager = variableManager;
        mProcedureManager = procedureManager;
        mConnectionManager = connectionManager;

        // TODO: Register arguments of existing procedures. Currently assuming none.
        mProcedureManager.registerObserver(mProcedureObserver);
    }

    public NameManager getVariableNameManager() {
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
    public void collectStats(Block block, boolean recursive) {
        String procedureName = ProcedureManager.getProcedureName(block);
        if (procedureName != null) {
            List<String> procedureArgs = ProcedureManager.getProcedureArguments(block);
            if (procedureArgs != null) {
                for (String arg : procedureArgs) {
                    getVarInfoImpl(arg, true).addProcedure(procedureName);
                }
            }

            if (ProcedureManager.isDefinition(block)) {
                mProcedureManager.addDefinition(block);
            } else {
                mProcedureManager.addDefinition(block);
            }
        }

        for (int i = 0; i < block.getInputs().size(); i++) {
            Input input = block.getInputs().get(i);
            List<Field> fields = input.getFields();
            int fieldCount = fields.size();
            for (int j = 0; j < fieldCount; ++j) {
                Field field = fields.get(j);
                if (field instanceof FieldVariable) {
                    FieldVariable varField = (FieldVariable) field;
                    String varName = varField.getVariable();
                    getVarInfoImpl(varName, true).addField(varField);
                    varField.registerObserver(mVariableObserver);
                }
            }
            addConnection(input.getConnection(), recursive);
        }

        addConnection(block.getNextConnection(), recursive);
        // Don't recurse on outputs or previous connections--that's effectively walking back up the
        // tree.
        addConnection(block.getPreviousConnection(), false);
        addConnection(block.getOutputConnection(), false);
    }

    /**
     * Clear state about variables, procedures, and connections.
     * These changes will be reflected in the externally owned connection and procedure manager.
     */
    public void clear() {
        mProcedureManager.clear();
        mVariableNameManager.clearUsedNames();
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

    private void addConnection(Connection conn, boolean recursive) {
        if (conn != null) {
            mConnectionManager.addConnection(conn);
            if (recursive) {
                Block recursiveTarget = conn.getTargetBlock();
                if (recursiveTarget != null) {
                    collectStats(recursiveTarget, true);
                }
            }
        }
    }

    private VariableInfoImpl getVarInfoImpl(String varName, boolean create) {
        String canonical = mVariableNameManager.makeCanonical(varName);
        VariableInfoImpl usages = mVariableInfoMap.get(varName);
        if (usages == null && create) {
            usages = new VariableInfoImpl(canonical);
            mVariableInfoMap.put(canonical, usages);
        }
        return usages;
    }

    public class VariableInfoImpl implements VariableInfo {
        /** Canonical variable name. */
        final String mKey;
        /** FieldVariables that are set to the variable. */
        ArrayList<WeakReference<FieldVariable>> mFields = null;
        /** Procedures that use the variable as an argument. */
        SimpleArraySet<String> mProcedures = null;

        private VariableInfoImpl(String canonicalProcedureName) {
            mKey = canonicalProcedureName;
        }

        @Override
        public int getUsageCount() {
            return (mFields == null ? 0 : mFields.size()) + getCountOfProceduresUsages();
        }

        @Override
        public int getCountOfProceduresUsages() {
            return mProcedures == null ? 0 : mProcedures.size();
        }

        @Override
        public String getProcedureName(int i) {
            return mProcedures.getAt(i);
        }

        void addField(FieldVariable newField) {
            if (mFields == null) {
                mFields = new ArrayList<>();
                mFields.add(new WeakReference<>(newField));
                return;
            }

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
        }

        boolean removeField(FieldVariable newField) {
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
                if (field == newField) {
                    mFields.remove(i);
                    --count;
                    return true;
                }
                ++i;
            }
            if (mFields.isEmpty()) {
                mFields = null;
                if (mProcedures == null) {
                    mVariableInfoMap.remove(mKey);
                }
            }
            return false;
        }

        void addProcedure(String procedureName) {
            if (mProcedures == null) {
                mProcedures = new SimpleArraySet<>();
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
                if (mFields == null) {
                    mVariableInfoMap.remove(mKey);
                }
            }
            return foundAndRemoved;
        }

        public boolean isProcedureArgument() {
            return mProcedures != null && !mProcedures.isEmpty();
        }

        @Override
        public List<FieldVariable> getFields() {
            int count = mFields.size();
            ArrayList<FieldVariable> fields = new ArrayList<>(mFields.size());
            int i = 0;
            while (i < count) {
                FieldVariable field = mFields.get(i).get();
                if (field == null) {
                    mFields.remove(i);
                    --count;
                    continue;  // Don't increment i
                }
                fields.add(field);
                ++i;
            }
            return fields;
        }
    }
}
