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
import android.support.v4.util.SimpleArrayMap;
import android.util.Log;

import com.google.blockly.android.R;
import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.android.control.NameManager;

import java.lang.ref.WeakReference;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Class for building {@link BlocklyCategory categories} for variable
 */
public final class VariableCategoryFactory extends CategoryFactory {
    public static final String ACTION_CREATE_VARIABLE = "CREATE_VARIABLE";

    private static final String GET_VAR_BLOCK = "variables_get";
    private static final String SET_VAR_BLOCK = "variables_set";
    private static final String CHANGE_VAR_BLOCK = "math_change";
    private static final String GET_VAR_FIELD = "VAR";

    private Context mContext;
    private BlocklyController mController;
    private NameManager mVariableNameManager;
    private BlockFactory mBlockFactory;

    public VariableCategoryFactory(Context context, BlocklyController controller) {
        mContext = context;
        mController = controller;
        mVariableNameManager = mController.getWorkspace().getVariableNameManager();
        mBlockFactory = mController.getBlockFactory();
    }

    @Override
    public BlocklyCategory obtainCategory(String customType) {
        BlocklyCategory category = new BlocklyCategory();
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
                    rebuildItems(category);
                }
            }
        });
        return category;
    }

    private void rebuildItems(BlocklyCategory category) {
        category.clear();
        category.addItem(new BlocklyCategory.ButtonItem(
                mContext.getString(R.string.create_variable), ACTION_CREATE_VARIABLE));
        SimpleArrayMap<String, String> variables = mVariableNameManager.getUsedNames();
        if (variables.size() == 0) {
            return;
        }
        Block setter = mBlockFactory.obtainBlock(SET_VAR_BLOCK, null);
        category.addItem(new BlocklyCategory.BlockItem(setter));
        Block changer = mBlockFactory.obtainBlock(CHANGE_VAR_BLOCK, null);
        category.addItem(new BlocklyCategory.BlockItem(changer));
        ArrayList<String> varNames = new ArrayList<>(variables.size());
        for (int i = 0; i < variables.size(); i++) {
            varNames.add(variables.keyAt(i));
        }
        Collections.sort(varNames);

        for (int i = 0; i < varNames.size(); i++) {
            Block varBlock = mBlockFactory.obtainBlock(GET_VAR_BLOCK, null);
            varBlock.getFieldByName(GET_VAR_FIELD).setFromString(varNames.get(i));
            category.addItem(new BlocklyCategory.BlockItem(varBlock));
        }
    }
}
