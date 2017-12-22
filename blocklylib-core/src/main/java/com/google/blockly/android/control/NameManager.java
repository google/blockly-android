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

import android.annotation.SuppressLint;
import android.database.DataSetObservable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;

import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility functions for handling variable and procedure names.
 */
public abstract class NameManager extends DataSetObservable {
    // Regular expression with two groups.  The first lazily looks for any sequence of characters
    // and the second looks for one or more numbers.  So foo2 -> (foo, 2).  f222 -> (f, 222).
    private static final Pattern mRegEx = Pattern.compile("^(.*?)(\\d+)$");
    protected final SortedSet<String> mDisplayNamesSorted = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    protected final ArrayMap<String, String> mCanonicalMap = new ArrayMap<>();

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
        while (mCanonicalMap.containsKey(makeCanonical(name))) {
            Matcher matcher = mRegEx.matcher(name);
            if (matcher.matches()) {
                // Increment digits suffix, preserving leading zeros. Ex., "var001" => "var002"
                String digits = matcher.group(2);
                int newValue = Integer.parseInt(digits) + 1;
                String newDigits = String.format("%0" + digits.length() +  "d", newValue);

                name = matcher.group(1) + newDigits;
            } else {
                name = name + "2";
            }
        }
        if (addName) {
            addName(name);
        }
        return name;
    }

    /**
     * @param name The string to look up.
     *
     * @return True if name's lowercase equivalent is in the list.
     */
    public boolean contains(String name) {
        return mCanonicalMap.containsKey(makeCanonical(name));
    }

    /**
     * @return The number of names that have been used.
     */
    public int size() {
        return mDisplayNamesSorted.size();
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
     * @throws IllegalArgumentException If the name is not valid.
     */
    public void addName(String name) {
        if (!isValidName(name)) {
            throw new IllegalArgumentException("Invalid name \"" + name + "\".");
        }
        if (mCanonicalMap.put(makeCanonical(name), name) == null) {
            mDisplayNamesSorted.add(name);
            notifyChanged();
        }
    }

    /**
     * @return An alphabetically sorted list of all of the names that have already been used.
     *         This list is not modifiable, but is backed by the real list and will stay updated.
     */
    public SortedSet<String> getUsedNames() {
        return Collections.unmodifiableSortedSet(mDisplayNamesSorted);
    }

    /**
     * Clear the list of used names.
     */
    public void clear() {
        if (mDisplayNamesSorted.size() != 0) {
            mDisplayNamesSorted.clear();
            mCanonicalMap.clear();
            notifyChanged();
        }
    }

    /**
     * Remove a single name from the list of used names.
     *
     * @param toRemove The name to remove.
     */
    public boolean remove(String toRemove) {
        String canonical = makeCanonical(toRemove);
        if (mCanonicalMap.remove(canonical) != null) {
            mDisplayNamesSorted.remove(toRemove);
            notifyChanged();
            return true;
        }
        return false;
    }

    public boolean isValidName(@NonNull String name) {
        if (name.isEmpty() || name.trim().length() != name.length()) {
            return false;
        }
        return true;
    }

    public String makeValidName(@NonNull String name, @Nullable String fallbackName) {
        name = name.trim();
        if (name.isEmpty()) {
            return fallbackName;
        } else {
            return name;
        }
    }

    /**
     * Returns the existing name, if the {@code name} will map to the same canonical form.
     * Otherwise, return the proposedName.
     * @param name The proposed name.
     * @return A previous added name that shares the same canonical form. Otherwise return null.
     */
    @Nullable
    public String getExisting(String name) {
        return mCanonicalMap.get(makeCanonical(name));
    }

    /**
     * @param name The proposed name
     * @return The canonical string for the provided name, as used by {@link #mCanonicalMap}.
     */
    @SuppressLint("DefaultLocale")
    protected String makeCanonical(@NonNull String name) {
        return name.toLowerCase();
    }

    /**
     * The NameManager for procedure names.
     */
    // TODO(602): Move to com.google.blockly.android.codegen.LanguageDefinition
    public static final class ProcedureNameManager extends NameManager {
        @Override
        public String generateExternalName(Set<String> reservedWords, String baseName) {
            // TODO: Implement.
            return baseName;
        }
    }

    /**
     * The NameManager for variable names.
     */
    public static final class VariableNameManager extends NameManager {
        private static final String LETTERS = "ijkmnopqrstuvwxyzabcdefgh"; // no 'l', start at i.
        private String mVariablePrefix;

        // TODO(602): Move to com.google.blockly.android.codegen.LanguageDefinition
        @Override
        public String generateExternalName(Set<String> reservedWords, String baseName) {
            // TODO: Implement.
            return mVariablePrefix + baseName;
        }

        /**
         * Sets the prefix that will be attached to external names during generation.
         * Some languages need a '$' or a namespace before all variable names.
         *
         * @param variablePrefix The prefix to attach.
         */
        // TODO: Move to com.google.blockly.android.codegen.LanguageDefinition
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
                    String canonical = makeCanonical(newName);  // In case override by subclass.
                    if (!mCanonicalMap.containsKey(canonical)) {
                        if (addName) {
                            mCanonicalMap.put(canonical, newName);
                            mDisplayNamesSorted.add(newName);
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
