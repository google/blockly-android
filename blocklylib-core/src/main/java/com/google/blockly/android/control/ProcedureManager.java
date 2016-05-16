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
import com.google.blockly.model.Field;
import com.google.blockly.model.FieldInput;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages procedure definitions, references and names.
 */
public class ProcedureManager {
    public static final String PROCEDURE_DEFINITION_PREFIX = "procedure_def";
    public static final String PROCEDURE_REFERENCE_PREFIX = "procedure_call";

    private final SimpleArrayMap<String, List<Block>> mProcedureReferences = new SimpleArrayMap<>();
    private final SimpleArrayMap<String, Block> mProcedureDefinitions = new SimpleArrayMap<>();
    private final NameManager mProcedureNameManager = new NameManager.ProcedureNameManager();


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
            return retval;
        } else {
            throw new IllegalStateException(
                    "Tried to remove an unknown procedure definition");
        }
    }

    public static boolean isReference(Block block) {
        return block.getType().startsWith(PROCEDURE_REFERENCE_PREFIX);
    }

    public static boolean isDefinition(Block block) {
        return block.getType().startsWith(PROCEDURE_DEFINITION_PREFIX);
    }

    private static String getProcedureName(Block block) {
        Field nameField = block.getFieldByName("name");
        if (nameField != null) {
            return ((FieldInput) nameField).getText();
        } else {
            throw new IllegalArgumentException(
                    "Procedure definition block with no procedure name.");
        }
    }

    private static void setProcedureName(Block block, String newName) {
        Field nameField = block.getFieldByName("name");
        if (nameField != null) {
            ((FieldInput) nameField).setText(newName);
        } else {
            throw new IllegalArgumentException(
                    "Procedure definition block with no procedure name.");
        }
    }
}
