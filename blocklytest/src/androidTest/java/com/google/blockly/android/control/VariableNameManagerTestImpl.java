/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.blockly.android.control;

import android.support.v4.util.ArraySet;

import com.google.blockly.model.FieldVariable;
import com.google.blockly.model.VariableInfo;


/**
 * Basic implementation of a VariableNameManager.
 */
public class VariableNameManagerTestImpl
        extends VariableNameManager<VariableNameManagerTestImpl.VariableInfoImpl> {
    @Override
    protected VariableInfoImpl newVariableInfo(String name) {
        return new VariableInfoImpl(name);
    }

    public static class VariableInfoImpl implements VariableInfo {
        String mDisplayName;
        ArraySet<String> mProcedureNames = new ArraySet<>();

        private VariableInfoImpl(String displayName) {
            mDisplayName = displayName;
        }

        @Override
        public String getDisplayName() {
            return mDisplayName;
        }

        @Override
        public boolean isProcedureArgument() {
            return !mProcedureNames.isEmpty();
        }

        @Override
        public int getUsageCount() {
            return 1;
        }

        @Override
        public ArraySet<String> getProcedureNames() {
            return mProcedureNames;
        }

        @Override
        public ArraySet<FieldVariable> getFields() {
            return new ArraySet<>();
        }

        @Override
        public void setUseAsProcedureArgument(String procedureName) {
            mProcedureNames.add(procedureName);
        }

        @Override
        public void removeUseAsProcedureArgument(String procedureName) {
            mProcedureNames.remove(procedureName);
        }
    }
}
