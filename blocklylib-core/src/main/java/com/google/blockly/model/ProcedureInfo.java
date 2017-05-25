package com.google.blockly.model;

import com.google.blockly.android.control.ProcedureManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Describes a procedure for the {@link ProcedureManager} and procedure mutators.
 */
public class ProcedureInfo {
    final String mName;
    final List<String> mArguments;
    final boolean mStatementInDefinition;

    public ProcedureInfo(String name,
                         List<String> arguments,
                         boolean definitionHasStatements) {
        mName = name;
        mArguments = Collections.unmodifiableList(new ArrayList<String>(arguments));
        mStatementInDefinition = definitionHasStatements;
    }

    public String getProcedureName() {
        return mName;
    }

    public List<String> getArguments() {
        return mArguments;
    }
    
    public boolean hasStatementInputInDefinition() {
        return mStatementInDefinition;
    }

    public static class Builder<Info extends ProcedureInfo> {
        String mName;
        List<String> mArguments;
        boolean mStatementInDefinition;

        public Builder() {
            // No initialization
        }

        public Builder(ProcedureInfo other) {
            mName = other.mName;
            mArguments = other.mArguments;
            mStatementInDefinition = other.mStatementInDefinition;
        }

        public Info build() {
            return (Info)(new ProcedureInfo(mName, mArguments, mStatementInDefinition));
        }

        public Builder<Info> setName(String name) {
            mName = name;
            return this;
        }

        public Builder<Info> setArguments(List<String> arguments) {
            mArguments = arguments;
            return this;
        }

        public Builder<Info> setStatementInDefinition(boolean statementInDefinition) {
            mStatementInDefinition = statementInDefinition;
            return this;
        }
    }
}
