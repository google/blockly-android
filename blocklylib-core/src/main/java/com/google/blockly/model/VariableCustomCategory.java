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

import android.content.Context;
import android.database.DataSetObserver;

import com.google.blockly.android.R;
import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.android.control.NameManager;
import com.google.blockly.utils.BlockLoadingException;

import java.lang.ref.WeakReference;
import java.util.SortedSet;

/**
 * Class for building {@link BlocklyCategory categories} for variables blocks.
 */
public final class VariableCustomCategory implements CustomCategory {
    private static final String TAG = "VariableCategoryFactory";

    public static final String ACTION_CREATE_VARIABLE = "CREATE_VARIABLE";

    private static final BlockTemplate GET_VAR_TEMPLATE = new BlockTemplate("variables_get");
    private static final BlockTemplate SET_VAR_TEMPLATE = new BlockTemplate("variables_set");
    private static final BlockTemplate CHANGE_VAR_TEMPLATE = new BlockTemplate("math_change");
    private static final String GET_VAR_FIELD = "VAR";

    private Context mContext;
    private BlocklyController mController;
    private NameManager mVariableNameManager;
    private BlockFactory mBlockFactory;

    public VariableCustomCategory(BlocklyController controller) {
        mController = controller;
        mContext = controller.getContext();
        mVariableNameManager = mController.getWorkspace().getVariableNameManager();
        mBlockFactory = mController.getBlockFactory();
    }

    @Override
    public void initializeCategory(BlocklyCategory category) throws BlockLoadingException {
        checkRequiredBlocksAreDefined();
        rebuildItems(category);

        final WeakReference<BlocklyCategory> catRef = new WeakReference<>(category);
        mVariableNameManager.registerObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                BlocklyCategory category = catRef.get();
                if (category == null) {
                    // If the category isn't being used anymore clean up this observer.
                    mVariableNameManager.unregisterObserver(this);
                } else {
                    // Otherwise, update the category's list.
                    try {
                        rebuildItems(category);
                    } catch (BlockLoadingException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
        });
    }

    private void checkRequiredBlocksAreDefined() throws BlockLoadingException {
        BlockTemplate[] required = {SET_VAR_TEMPLATE, GET_VAR_TEMPLATE, CHANGE_VAR_TEMPLATE};

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
        for (BlocklyCategory.CategoryItem item : category.getItems()) {
            if (item.getType() == BlocklyCategory.CategoryItem.TYPE_BLOCK) {
                // Clean up the old views
                BlocklyCategory.BlockItem blockItem = (BlocklyCategory.BlockItem) item;
                mController.unlinkViews(blockItem.getBlock());
            }
        }
        category.clear();

        category.addItem(new BlocklyCategory.ButtonItem(
                mContext.getString(R.string.create_variable), ACTION_CREATE_VARIABLE));
        SortedSet<String> varNames = mVariableNameManager.getUsedNames();
        if (varNames.size() == 0) {
            return;
        }

        Block setter = mBlockFactory.obtainBlockFrom(SET_VAR_TEMPLATE);
        setter.getFieldByName(GET_VAR_FIELD).setFromString(varNames.first());
        category.addItem(new BlocklyCategory.BlockItem(setter));

        Block changer = mBlockFactory.obtainBlockFrom(CHANGE_VAR_TEMPLATE);
        changer.getFieldByName(GET_VAR_FIELD).setFromString(varNames.first());
        category.addItem(new BlocklyCategory.BlockItem(changer));

        for (String name : varNames) {
            Block varBlock = mBlockFactory.obtainBlockFrom(GET_VAR_TEMPLATE);
            varBlock.getFieldByName(GET_VAR_FIELD).setFromString(name);
            category.addItem(new BlocklyCategory.BlockItem(varBlock));
        }
    }
}
