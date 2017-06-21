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
