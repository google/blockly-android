package com.google.blockly.android.control;

import com.google.blockly.model.VariableInfo;

/**
 * NameManager for VariableInfo.
 */
// This abstract class is required because we really want to store VariableInfoImpl objects
// inside the WorkspaceStats (with things like field and procedure counts).
public abstract class VariableNameManager<VI extends VariableInfo> extends NameManager<VI> {
    public abstract void addProcedureArg(String argName, String newProcedureName);
}