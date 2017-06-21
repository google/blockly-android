package com.google.blockly.android.control;

import android.support.v4.util.ArraySet;

import com.google.blockly.model.VariableInfo;

/**
 * NameManager for VariableInfo.
 */
// This abstract class is required because we really want to store VariableInfoImpl objects
// inside the WorkspaceStats (with things like field and procedure counts).
public abstract class VariableNameManager<VI extends VariableInfo> extends NameManager<VI> {
    private static final String LETTERS = "ijkmnopqrstuvwxyzabcdefgh"; // no 'l', start at i.

    /**
     * Return a new variable name that is not yet being used. This will try to
     * generate single letter variable names in the range 'i' to 'z' to start with.
     * If no unique name is located it will try 'i' to 'z', 'a' to 'h',
     * then 'i2' to 'z2' etc.  Skip 'l'.
     *
     * @return New variable name.
     */
    public String generateVariableName() {
        String newName;
        int suffix = 1;
        while (true) {
            for (int i = 0; i < LETTERS.length(); i++) {
                newName = Character.toString(LETTERS.charAt(i));
                if (suffix > 1) {
                    newName += suffix;
                }
                String canonical = makeCanonical(newName);  // In case override by subclass.

                // TODO: Compare against reserved words, as well
                if (!mCanonicalMap.containsKey(canonical)) {
                    return newName;
                }
            }
            suffix++;
        }
    }

    /**
     * Attempts to add a variable to the workspace.
     * @param requestedName The preferred variable name. Usually the user name.
     * @param allowRenaming Whether the variable name can be renamed.
     * @return The name that was added, if any. May be null if renaming is not allowed.
     */
    public String addVariable(String requestedName, boolean allowRenaming) {
        VI varInfo = getValueOf(requestedName);
        if (varInfo == null) {
            varInfo = newVariableInfo(requestedName);
            put(requestedName, varInfo);
            return requestedName;
        } else if (allowRenaming) {
            String altName = generateUniqueName(requestedName);
            put(altName, newVariableInfo(altName));
            return altName;
        } else {
            return null;
        }
    }

    /**
     * Associates the variable {@code argName} with {@code procedureName}, creating the variable if
     * necessary.
     * @param argName The name of the procedure argument.
     * @param procedureName The name of the procedure that uses it.
     */
    public void addProcedureArg(String argName, String procedureName) {
        VI varInfo = getValueOf(argName);
        if (varInfo == null) {
            varInfo = newVariableInfo(argName);
            put(argName, varInfo);
        }
        varInfo.setUseAsProcedureArgument(procedureName);
    }

    /**
     * Removed any association between the variable {@code argName} with {@code procedureName}. This
     * does not remove the variable, if its role as a procedure argument was the last know use.
     * @param varName The name of the variable that was once a procedure argument.
     * @param procedureName The name of the procedure that no longer uses it.
     */
    public void removeProcedureArg(String varName, String procedureName) {
        VI varInfo = getValueOf(varName);
        if (varInfo != null) {
            varInfo.removeUseAsProcedureArgument(procedureName);
        }
    }

    /**
     * Constructs a new VariableInfo object with the given name
     * @param varName The name fo the variable to create.
     * @return The VariableInfo object.
     */
    protected abstract VI newVariableInfo(String varName);
}