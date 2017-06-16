package com.google.blockly.android.control;

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

    public abstract boolean addVariable(String variableName);
    public abstract boolean addProcedureArg(String argName, String procedureName);
}