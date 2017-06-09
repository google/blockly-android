/*
 *  Copyright 2017 Google Inc. All Rights Reserved.
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
package com.google.blockly.model;

import android.util.Log;

import com.google.blockly.android.R;
import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.android.control.ProcedureManager;
import com.google.blockly.model.mutator.AbstractProcedureMutator;
import com.google.blockly.model.mutator.ProcedureCallMutator;
import com.google.blockly.model.mutator.ProcedureDefinitionMutator;
import com.google.blockly.utils.BlockLoadingException;

import java.lang.ref.WeakReference;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Class for building {@link BlocklyCategory categories} for procedure blocks (user-defined
 * functions).
 */
public class ProcedureCustomCategory implements CustomCategory {
    private static final String TAG = "ProcedureCategoryFactor";  // 23 chars max

    private static final String NAME_FIELD = ProcedureDefinitionMutator.NAME_FIELD_NAME;

    private static final BlockTemplate DEFINE_NO_RETURN_BLOCK_TEMPLATE =
            new BlockTemplate(ProcedureManager.DEFINE_NO_RETURN_BLOCK_TYPE);
    private static final BlockTemplate DEFINE_WITH_RETURN_BLOCK_TEMPLATE =
            new BlockTemplate(ProcedureManager.DEFINE_WITH_RETURN_BLOCK_TYPE);
    private static final BlockTemplate CALL_NO_RETURN_BLOCK_TEMPLATE =
            new BlockTemplate(ProcedureManager.CALL_NO_RETURN_BLOCK_TYPE);
    private static final BlockTemplate CALL_WITH_RETURN_BLOCK_TEMPLATE =
            new BlockTemplate(ProcedureManager.CALL_WITH_RETURN_BLOCK_TYPE);
    private static final BlockTemplate IF_RETURN_TEMPLATE =
            new BlockTemplate("procedures_ifreturn");

    protected final BlocklyController mController;
    protected final BlockFactory mBlockFactory;
    protected final Workspace mWorkspace;
    protected final ProcedureManager mProcedureManager;

    protected final String mDefaultProcedureName;

    public ProcedureCustomCategory(BlocklyController controller) {
        mController = controller;
        mBlockFactory = mController.getBlockFactory();
        mWorkspace = mController.getWorkspace();
        mProcedureManager = mWorkspace.getProcedureManager();

        mDefaultProcedureName = getDefaultProcedureName();
    }

    public String getDefaultProcedureName() {
        // TODO: Get from translation manager.
        return mController.getContext().getString(R.string.blockly_default_function_name);
    }

    @Override
    public void initializeCategory(BlocklyCategory category) throws BlockLoadingException {
        checkRequiredBlocksAreDefined();
        rebuildItems(category);

        final WeakReference<BlocklyCategory> catRef = new WeakReference<>(category);
        mProcedureManager.registerObserver(new ProcedureManager.Observer() {
            @Override
            public void onProcedureBlockAdded(String procedureName, Block block) {
                BlocklyCategory category = catRef.get();
                if (checkCategory(category)) {
                    rebuildItemsSafely(category);
                }
            }

            @Override
            public void onProcedureBlocksRemoved(String procedureName, List<Block> blocks) {
                BlocklyCategory category = catRef.get();
                if (checkCategory(category)) {
                    rebuildItemsSafely(category);
                }
            }

            @Override
            public void onProcedureMutated(ProcedureInfo oldProcInfo, ProcedureInfo newProcInfo) {
                BlocklyCategory category = catRef.get();
                if (checkCategory(category)) {
                    rebuildItemsSafely(category);
                }
            }

            @Override
            public void onClear() {
                BlocklyCategory category = catRef.get();
                if (checkCategory(category)) {
                    rebuildItemsSafely(category);
                }
            }

            private void rebuildItemsSafely(BlocklyCategory category) {
                try {
                    rebuildItems(category);
                } catch (BlockLoadingException e) {
                    category.clear();
                    Log.w(TAG, "Failed to rebuild ProcedureCustomCategory");
                }
            }

            private boolean checkCategory(BlocklyCategory category) {
                if (category == null) {
                    mProcedureManager.unregisterObserver(this);
                    return false;
                }
                return true;
            }
        });
    }

    private void checkRequiredBlocksAreDefined() throws BlockLoadingException {
        BlockTemplate[] required = {
                DEFINE_NO_RETURN_BLOCK_TEMPLATE,
                DEFINE_WITH_RETURN_BLOCK_TEMPLATE,
                CALL_NO_RETURN_BLOCK_TEMPLATE,
                CALL_WITH_RETURN_BLOCK_TEMPLATE,
                IF_RETURN_TEMPLATE
        };

        StringBuilder sb = null;
        for (BlockTemplate template : required) {
            if (!mBlockFactory.isDefined(template.mTypeName)) {
                if (sb == null) {
                    sb = new StringBuilder();
                } else {
                    sb.append(", ");
                }
                sb.append(template.mTypeName);
            }
        }
        if (sb != null) {
            throw new BlockLoadingException("Missing block definitions: " + sb.toString());
        }
    }

    private void rebuildItems(BlocklyCategory category) throws BlockLoadingException {
        category.clear();

        Block block = mBlockFactory.obtainBlockFrom(DEFINE_NO_RETURN_BLOCK_TEMPLATE);
        ((FieldInput)block.getFieldByName(NAME_FIELD)).setText(mDefaultProcedureName);
        category.addItem(new BlocklyCategory.BlockItem(block));

        block = mBlockFactory.obtainBlockFrom(DEFINE_WITH_RETURN_BLOCK_TEMPLATE);
        ((FieldInput)block.getFieldByName(NAME_FIELD)).setText(mDefaultProcedureName);
        category.addItem(new BlocklyCategory.BlockItem(block));

        if (!mProcedureManager.hasProcedureDefinitionWithReturn()) {
            block = mBlockFactory.obtainBlockFrom(IF_RETURN_TEMPLATE);
            category.addItem(new BlocklyCategory.BlockItem(block));
        }

        // Create a call block for each definition.
        final Map<String, Block> definitions = mProcedureManager.getDefinitionBlocks();
        SortedSet<String> sortedProcNames = new TreeSet<>(new Comparator<String>() {
            @Override
            public int compare(String procName1, String procName2) {
                Block def1 = definitions.get(procName1);
                Block def2 = definitions.get(procName2);
                String type1 = def1.getType();
                String type2 = def2.getType();

                // procedures_defnoreturn < procedures_defreturn
                int typeComp = type1.compareTo(type2);
                if (typeComp != 0) {
                    return typeComp;
                }
                // Otherwise sort by procedure name, alphabetically
                int nameComp = procName1.compareToIgnoreCase(procName2);
                if (nameComp != 0) {
                    return nameComp;
                }
                return def1.getId().compareTo(def2.getId()); // Last resort, by block id
            }
        });
        sortedProcNames.addAll(definitions.keySet());
        for (String procName : sortedProcNames) {
            Block defBlock = definitions.get(procName);
            ProcedureInfo procedureInfo = ((AbstractProcedureMutator) defBlock.getMutator())
                    .getProcedureInfo();
            BlockTemplate callBlockTemplate;
            if (defBlock.getType().equals(ProcedureManager.DEFINE_NO_RETURN_BLOCK_TYPE)) {
                callBlockTemplate = CALL_NO_RETURN_BLOCK_TEMPLATE;  // without return value
            } else {
                callBlockTemplate = CALL_WITH_RETURN_BLOCK_TEMPLATE;  // with return value
            }
            Block callBlock = mBlockFactory.obtainBlockFrom(callBlockTemplate);
            ((ProcedureCallMutator) callBlock.getMutator()).mutate(procedureInfo);
            category.addItem(new BlocklyCategory.BlockItem(callBlock));
        }
    }
}
