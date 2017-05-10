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

import android.support.v4.util.ArrayMap;
import android.support.v4.util.SimpleArrayMap;

import com.google.blockly.model.Block;
import com.google.blockly.model.BlockTemplate;
import com.google.blockly.model.Field;
import com.google.blockly.model.FieldInput;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Manages procedure definitions, references and names.
 */
public class ProcedureManager {
    public static final String PROCEDURE_DEFINITION_PREFIX = "procedure_def";
    public static final String PROCEDURE_REFERENCE_PREFIX = "procedure_call";

    public static final String DEFINE_NO_RETURN_BLOCK_TYPE = "procedures_defnoreturn";
    public static final String DEFINE_WITH_RETURN_BLOCK_TEMPLATE = "procedures_defreturn";
    public static final String CALL_NO_RETURN_BLOCK_TEMPLATE = "procedures_callnoreturn";
    public static final String CALL_WITH_RETURN_BLOCK_TEMPLATE = "procedures_callreturn";

    private final ArrayMap<String, List<Block>> mProcedureReferences = new ArrayMap<>();
    private final ArrayMap<String, Block> mProcedureDefinitions = new ArrayMap<>();
    private final NameManager mProcedureNameManager = new NameManager.ProcedureNameManager();

    private int mCountOfReferencesWithReturn = 0;

    /**
     * Determines if a block is procedure call.
     * @param block The block in question.
     * @return True, if the block type is a recognized type of procedure call.
     */
    public static boolean isReference(Block block) {
        String type = block.getType();
        return type.equals(CALL_NO_RETURN_BLOCK_TEMPLATE)
                || type.equals(CALL_WITH_RETURN_BLOCK_TEMPLATE);
    }

    /**
     * Determines if a block is procedure definition.
     * @param block The block in question.
     * @return True, if the block type is a recognized type of procedure definition.
     */
    public static boolean isDefinition(Block block) {
        String type = block.getType();
        return type.equals(DEFINE_NO_RETURN_BLOCK_TYPE)
                || type.equals(DEFINE_WITH_RETURN_BLOCK_TEMPLATE);
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
            if (block.getType().equals(CALL_WITH_RETURN_BLOCK_TEMPLATE)) {
                ++mCountOfReferencesWithReturn;
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
        String procedureName = getProcedureName(block);
        if (mProcedureReferences.containsKey(procedureName)) {
            mProcedureReferences.get(procedureName).remove(block);
            if (block.getType().equals(CALL_WITH_RETURN_BLOCK_TEMPLATE)) {
                --mCountOfReferencesWithReturn;
                assert (mCountOfReferencesWithReturn >= 0);
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

    public boolean hasReferenceWithReturn() {
        return mCountOfReferencesWithReturn > 0;
    }

    // TODO: Better checking errors if the block is not a procedure definition,
    //       since this is called from so many public methods.
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
