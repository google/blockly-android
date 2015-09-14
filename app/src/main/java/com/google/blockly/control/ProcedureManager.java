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

package com.google.blockly.control;

import android.support.v4.util.SimpleArrayMap;

import com.google.blockly.model.Block;
import com.google.blockly.model.Field;
import com.google.blockly.model.NameManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages procedure definitions, references and names.
 */
public class ProcedureManager {
    public static final String PROCEDURE_DEFINITION_PREFIX = "procedure_def";
    public static final String PROCEDURE_REFERENCE_PREFIX = "procedure_call";

    private final SimpleArrayMap<String, List<Block>> mProcedureReferences = new SimpleArrayMap<>();
    // No procedure will be defined more than once, so just a list.
    private final List<Block> mProcedureDefinitions = new ArrayList<>();
    private final NameManager mProcedureNameManager = new NameManager.ProcedureNameManager();

    public SimpleArrayMap<String, List<Block>> getProcedureReferences() {
        return mProcedureReferences;
    }

    public NameManager getProcedureNameManager() {
        return mProcedureNameManager;
    }

    public List<Block> getProcedureDefinitions() {
        return mProcedureDefinitions;
    }

    public void clear() {
        mProcedureDefinitions.clear();
        mProcedureReferences.clear();
        mProcedureNameManager.clearUsedNames();
    }

    // Procedure references
    public boolean isProcedureReference(Block block) {
        return block.getName().startsWith(PROCEDURE_REFERENCE_PREFIX);
    }

    public void addProcedureReference(Block block) {
        if (mProcedureReferences.containsKey(block.getName())) {
            mProcedureReferences.get(block.getName()).add(block);
        } else {
            throw new IllegalStateException(
                    "Tried to add a reference to a procedure that has not been defined.");
        }
    }

    public void removeProcedureReference(Block block) {
        if (mProcedureReferences.containsKey(block.getName())) {
            mProcedureReferences.get(block.getName()).remove(block);
        } else {
            throw new IllegalStateException(
                    "Tried to remove a procedure reference that was not in the list of references");
        }
    }

    // Procedure definitions
    public boolean isProcedureDefinition(Block block) {
        return block.getName().startsWith(PROCEDURE_DEFINITION_PREFIX);
    }

    public void addProcedureDefinition(Block block) {
        // TODO (fenichel): Do I need to check for the name being null in other places as well?
        if (block.getFieldByName("name") != null) {
            mProcedureDefinitions.add(block);
            mProcedureReferences.put(block.getName(), new ArrayList<Block>());
            mProcedureNameManager.addName(
                    ((Field.FieldInput) block.getFieldByName("name")).getText());
        } else {
            throw new IllegalStateException(
                    "Procedure definition block with no procedure name.");
        }
    }

    public List<Block>  removeProcedureDefinition(Block block) {
        if (mProcedureDefinitions.contains(block)) {
            //mProcedureReferences.remove(block.getName());
            // TODO(fenichel): Remove name from procedure name manager.
            // TODO(fenichel): Decide when to remove the list of procedure references.
            mProcedureDefinitions.remove(block);
            return mProcedureReferences.get(block.getName());
        } else {
            throw new IllegalStateException(
                    "Tried to remove an unknown procedure definition");
        }
    }
}
