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

import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.android.control.NameManager;
import com.google.blockly.android.control.VariableCategory;

/**
 * Class for returning a FlyoutCategory of a specific type.
 */
public abstract class CategoryFactory {
    public abstract FlyoutCategory obtainFlyout(String customType);

    public static final class VariableFlyoutFactory extends CategoryFactory {
        private Context mContext;
        private BlocklyController mController;
        private NameManager mVariableNameManager;

        public VariableFlyoutFactory(Context context, BlocklyController controller) {
            mContext = context;
            mController = controller;
            mVariableNameManager = mController.getWorkspace().getVariableNameManager();
        }

        @Override
        public FlyoutCategory obtainFlyout(String customType) {
            return new VariableCategory(mContext, mController, mVariableNameManager);
        }
    }
}
