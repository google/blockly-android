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

import android.support.v4.util.SimpleArrayMap;

import com.google.blockly.model.Block;
import com.google.blockly.model.Connection;
import com.google.blockly.model.Field;
import com.google.blockly.model.FieldVariable;
import com.google.blockly.model.Input;
import com.google.blockly.utils.SimpleArraySet;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Tracks information about the Workspace that we want fast access to.
 */
public class WorkspaceStats {

    // Maps from variable/procedure names to the blocks/fields where they are referenced.
    private final SimpleArrayMap<String, List<WeakReference<Block>>> mVariableBlockRefs =
            new SimpleArrayMap<>();
    private final NameManager mVariableNameManager;
    private final ProcedureManager mProcedureManager;
    private final ConnectionManager mConnectionManager;

    private final SimpleArraySet<String> mTempVarNames = new SimpleArraySet<>();

    private final Field.Observer mVariableObserver = new Field.Observer() {
        @Override
        public void onValueChanged(Field field, String oldVar, String newVar) {
            Block block = field.getBlock();
            removeBlockRef(oldVar, block);
            if (newVar == null) {
                return;
            }
            addAllBlockRefs(block);  // May re-add the block to oldVar, if ref'ed in another field.
        }
    };
    private final List<Connection> mTempConnecitons = new ArrayList<>();

    public WorkspaceStats(NameManager variableManager, ProcedureManager procedureManager,
                          ConnectionManager connectionManager) {
        mVariableNameManager = variableManager;
        mProcedureManager = procedureManager;
        mConnectionManager = connectionManager;
    }

    public NameManager getVariableNameManager() {
        return mVariableNameManager;
    }

    /**
     * Populates {@code outputBlocks} with the blocks that reference the given variable.
     * @param varName The name of the variable in question.
     * @param outputBlocks The collection into which the blocks will be added.
     */
    public void getBlocksWithVariable(String varName, Collection<Block> outputBlocks) {
        String canonical = mVariableNameManager.makeCanonical(varName);
        List<WeakReference<Block>> blocks = mVariableBlockRefs.get(canonical);
        if (blocks == null) {
            return;
        }
        Iterator<WeakReference<Block>> it = blocks.iterator();
        while (it.hasNext()) {
            Block block = it.next().get();
            if (block == null) {
                it.remove();
                continue;
            } else {
                outputBlocks.add(block);
            }
        }
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
        addAllBlockRefs(block);
        for (int i = 0; i < block.getInputs().size(); i++) {
            Input in = block.getInputs().get(i);
            addConnection(in.getConnection(), recursive);
        }

        addConnection(block.getNextConnection(), recursive);
        // Don't recurse on outputs or previous connections--that's effectively walking back up the
        // tree.
        addConnection(block.getPreviousConnection(), false);
        addConnection(block.getOutputConnection(), false);

        // TODO (fenichel): Procedure calls will only work when mutations work.
        // The mutation will change the name of the block.  I believe that means name field,
        // not type.
        if (mProcedureManager.isReference(block)) {
            mProcedureManager.addReference(block);
        }
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
                    mVariableBlockRefs.get(((FieldVariable)field).getVariable()).remove(field);
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

    private SimpleArraySet<String> getCanonicalVariableNamesReferenced(Block block) {
        mTempVarNames.clear();

        if (ProcedureManager.isDefinition(block)) {
            List<String> procArgs = mProcedureManager.getProcedureArguments(block);
            int argCount = procArgs.size();
            for (int i = 0; i < argCount; ++i) {
                String arg = procArgs.get(i);
                String canonical = mVariableNameManager.makeCanonical(arg);
                if (!mTempVarNames.contains(canonical)) {
                    mTempVarNames.add(canonical);
                }
            }
        }

        List<Input> inputs = block.getInputs();
        for (int i = 0; i < inputs.size(); i++) {
            Input input = inputs.get(i);
            List<Field> fields = input.getFields();
            for (int j = 0; j < fields.size(); j++) {
                Field field = fields.get(j);
                if (field instanceof FieldVariable) {
                    String varName = ((FieldVariable)field).getVariable();
                    mTempVarNames.add(varName);
                }
            }
        }

        return mTempVarNames;
    }

    private void addAllBlockRefs(Block block) {
        if (ProcedureManager.isDefinition(block)) {
            mProcedureManager.addDefinition(block);
        }

        SimpleArraySet<String> vars = getCanonicalVariableNamesReferenced(block);
        int count = vars.size();
        for (int i = 0; i < count; ++i) {
            String varName = vars.getAt(i);
            List<WeakReference<Block>> list = mVariableBlockRefs.get(varName);
            if (list == null) {
                list = new ArrayList<>();
                mVariableBlockRefs.put(varName, list);
                mVariableNameManager.addName(varName);
            }
            addBlockRef(list, block);
        }
    }

    private void addBlockRef(List<WeakReference<Block>> list, Block newBlock) {
        Iterator<WeakReference<Block>> it = list.iterator();
        while (it.hasNext()) {
            Block existingBlock = it.next().get();
            if (existingBlock == null) {
                it.remove();
            } else if (existingBlock == newBlock) {
                return; // Already added.
            }
        }
        list.add(new WeakReference<>(newBlock));
    }

    private boolean removeBlockRef(String varName, Block block) {
        String canonical = mVariableNameManager.makeCanonical(varName);
        List<WeakReference<Block>> list = mVariableBlockRefs.get(canonical);
        Iterator<WeakReference<Block>> it = list.iterator();
        while (it.hasNext()) {
            Block existingBlock = it.next().get();
            if (existingBlock == block) {
                it.remove();
                return true;  // Found and removed.
            } else if (existingBlock == null) {
                it.remove();
            }
        }
        return false; // Not found.
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
}
