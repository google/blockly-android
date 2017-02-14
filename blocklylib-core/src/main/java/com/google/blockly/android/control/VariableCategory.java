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
package com.google.blockly.android.control;

import android.content.Context;
import android.database.DataSetObserver;
import android.support.v4.util.SimpleArrayMap;

import com.google.blockly.android.R;
import com.google.blockly.model.Block;
import com.google.blockly.model.BlockFactory;
import com.google.blockly.model.BlocklyCategory;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Automatically adds/removes blocks from the category when the list of variables changes.
 */
public class VariableCategory extends BlocklyCategory {
    public static final String ACTION_CREATE_VARIABLE = "CREATE_VARIABLE";

    private static final String GET_VAR_BLOCK = "variables_get";
    private static final String SET_VAR_BLOCK = "variables_set";
    private static final String CHANGE_VAR_BLOCK = "math_change";
    private static final String GET_VAR_FIELD = "VAR";

    private final NameManager mNameManager;
    private final ButtonItem mCreateButton;
    private final BlockFactory mBlockFactory;

    public VariableCategory(Context context, final BlocklyController controller,
            final NameManager variableNameManager) {
        mNameManager = variableNameManager;
        mBlockFactory = controller.getBlockFactory();
        mCreateButton = new ButtonItem(context.getString(R.string.create_variable),
                ACTION_CREATE_VARIABLE);
        variableNameManager.registerObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                rebuildItems();
            }
        });
        rebuildItems();
    }

    private void rebuildItems() {
        mItems.clear();
        mItems.add(mCreateButton);
        SimpleArrayMap<String, String> variables = mNameManager.getUsedNames();
        if (variables.size() == 0) {
            return;
        }
        Block setter = mBlockFactory.obtainBlock(SET_VAR_BLOCK, null);
        mItems.add(new BlockItem(setter));
        Block changer = mBlockFactory.obtainBlock(CHANGE_VAR_BLOCK, null);
        mItems.add(new BlockItem(changer));
        ArrayList<String> varNames = new ArrayList<>(variables.size());
        for (int i = 0; i < variables.size(); i++) {
            varNames.add(variables.keyAt(i));
        }
        Collections.sort(varNames);

        for (int i = 0; i < varNames.size(); i++) {
            Block varBlock = mBlockFactory.obtainBlock(GET_VAR_BLOCK, null);
            varBlock.getFieldByName(GET_VAR_FIELD).setFromString(varNames.get(i));
            mItems.add(new BlockItem(varBlock));
        }
    }
}
