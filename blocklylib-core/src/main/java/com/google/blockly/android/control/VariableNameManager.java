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
    public boolean addVariable(String requestedName, boolean allowRenaming) {
        String canonical = makeCanonical(requestedName);
        NameEntry<VI> entry = mCanonicalMap.get(canonical);
        if (entry == null) {
            entry = new NameEntry<>(requestedName, newVariableInfo(requestedName));
            mCanonicalMap. put(canonical, entry);
            mDisplayNamesSorted.add(requestedName);
            return true;
        } else if (allowRenaming) {
            String altName = generateUniqueName(requestedName);
            put(altName, newVariableInfo(altName));
            mDisplayNamesSorted.add(altName);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Associated the variable {@code argName} with {@code procedureName}, creating the variable if
     * necessary.
     * @param argName The name of the procedure argument.
     * @param procedureName The name of the procedure that uses it.
     */
    public void addProcedureArg(String argName, String procedureName) {
        String canonical = makeCanonical(argName);
        NameEntry<VI> entry = mCanonicalMap.get(canonical);
        if (entry == null) {
            entry = new NameEntry<>(argName, newVariableInfo(argName));
            mCanonicalMap.put(canonical, entry);
        }
        markVariableAsProcedureArg(entry.mValue, procedureName);
    }

    protected abstract VI newVariableInfo(String name);

    /**
     *
     * @param varInfo
     * @param procedureArg
     */
    protected abstract void markVariableAsProcedureArg(VI varInfo, String procedureArg);
}