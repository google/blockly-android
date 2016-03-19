/*
 *  Copyright 2015 Google Inc. All Rights Reserved.
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

import android.database.DataSetObservable;
import android.support.v4.util.SimpleArrayMap;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility functions for handling variable and procedure names.
 */
public abstract class NameManager extends DataSetObservable {
    // Regular expression with two groups.  The first lazily looks for any sequence of characters
    // and the second looks for one or more numbers.  So foo2 -> (foo, 2).  f222 -> (f, 222).
    private static final Pattern mRegEx = Pattern.compile("^(.*?)(\\d+)$");
    protected final SimpleArrayMap<String, String> mUsedNames;

    public NameManager() {
        mUsedNames = new SimpleArrayMap<>();
    }

    /**
     * Generates a name that is unique within the scope of the current NameManager, based on the
     * input name.  If the base name was unique, returns it directly.
     *
     * @param name The name upon which to base the unique name.
     * @param addName Whether to add the generated name to the used names list.
     *
     * @return A unique name.
     */
    public String generateUniqueName(String name, boolean addName) {
        while (mUsedNames.containsKey(name.toLowerCase())) {
            Matcher matcher = mRegEx.matcher(name);
            if (matcher.matches()) {
                name = matcher.group(1) + (Integer.parseInt(matcher.group(2)) + 1);
            } else {
                name = name + "2";
            }
        }
        if (addName) {
            mUsedNames.put(name.toLowerCase(), "UNUSED");
            notifyChanged();
        }
        return name.toLowerCase();
    }

    /**
     * @param name The string to look up.
     *
     * @return True if name's lowercase equivalent is in the list.
     */
    public boolean contains(String name) {
        return mUsedNames.containsKey(name.toLowerCase());
    }

    /**
     * @return The number of names that have been used.
     */
    public int size() {
        return mUsedNames.size();
    }

    /**
     * @return The variable name at the index.
     */
    public String get(int index) {
        return mUsedNames.keyAt(index);
    }

    /**
     * Convert a Blockly entity name to a legal exportable entity name.
     * Ensure that this is a new name not overlapping any previously defined name.
     * Also check against list of reserved words for the current language and
     * ensure name doesn't collide.
     * The new name will conform to the [_A-Za-z][_A-Za-z0-9]* format that most languages consider
     * legal for variables.
     *
     * @param reservedWords Reserved words in the target language.
     * @param baseName The name to convert.
     *
     * @return A legal variable or procedure name in the target language.
     */
    public abstract String generateExternalName(Set<String> reservedWords, String baseName);

    /**
     * Adds the name to the list of used names.  Does not check if the name is already there.
     *
     * @param name The name to add.
     */
    public void addName(String name) {
        if (mUsedNames.put(name.toLowerCase(), "UNUSED") == null) {
            notifyChanged();
        }
    }

    /**
     * @return A list of all of the names that have already been used.
     */
    public SimpleArrayMap<String, String> getUsedNames() {
        return mUsedNames;
    }

    /**
     * Clear the list of used names.
     */
    public void clearUsedNames() {
        if (mUsedNames.size() != 0) {
            mUsedNames.clear();
            notifyChanged();
        }
    }

    /**
     * Remove a single name from the list of used names.
     *
     * @param toRemove The name to remove.
     */
    public boolean remove(String toRemove) {
        String result = mUsedNames.remove(toRemove.toLowerCase());
        if (result != null) {
            notifyChanged();
            return true;
        }
        return false;
    }

    public static final class ProcedureNameManager extends NameManager {

        @Override
        public String generateExternalName(Set<String> reservedWords, String baseName) {
            // TODO(fenichel): Implement.
            return "";
        }
    }

    public static final class VariableNameManager extends NameManager {
        private static final String LETTERS = "ijkmnopqrstuvwxyzabcdefgh"; // no 'l', start at i.
        private String mVariablePrefix;

        @Override
        public String generateExternalName(Set<String> reservedWords, String baseName) {
            // TODO(fenichel): Implement.
            return "";
        }

        /**
         * Sets the prefix that will be attached to external names during generation.
         * Some languages need a '$' or a namespace before all variable names.
         *
         * @param variablePrefix The prefix to attach.
         */
        public void setVariablePrefix(String variablePrefix) {
            mVariablePrefix = variablePrefix;
        }

        /**
         * Return a new variable name that is not yet being used. This will try to
         * generate single letter variable names in the range 'i' to 'z' to start with.
         * If no unique name is located it will try 'i' to 'z', 'a' to 'h',
         * then 'i2' to 'z2' etc.  Skip 'l'.
         *
         * @param addName Whether to add the new name to the list of variables.
         *
         * @return New variable name.
         */
        public String generateVariableName(boolean addName) {
            String newName;
            int suffix = 1;
            while (true) {
                for (int i = 0; i < LETTERS.length(); i++) {
                    newName = Character.toString(LETTERS.charAt(i));
                    if (suffix > 1) {
                        newName += suffix;
                    }
                    if (!mUsedNames.containsKey(newName)) {
                        if (addName) {
                            mUsedNames.put(newName, "UNUSED");
                            notifyChanged();
                        }
                        return newName;
                    }
                }
                suffix++;
            }
        }
    }
}
