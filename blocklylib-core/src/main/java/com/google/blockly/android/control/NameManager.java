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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility functions for handling variable and procedure names.
 */
public class NameManager<T> extends DataSetObservable {

    public final class NameEntry<T> {
        final String mDisplayName;
        final T mValue;

        public NameEntry(String displayName, T value) {
            mDisplayName = displayName;
            mValue = value;
        }
    }

    // Regular expression with two groups.  The first lazily looks for any sequence of characters
    // and the second looks for one or more numbers.  So foo2 -> (foo, 2).  f222 -> (f, 222).
    private static final Pattern mRegEx = Pattern.compile("^(.*?)(\\d+)$");
    protected final SortedSet<String> mDisplayNamesSorted = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    protected final ArrayMap<String, NameEntry<T>> mCanonicalMap = new ArrayMap<>();

    /**
     * Generates a name that is unique within the scope of the current NameManager, based on the
     * input name.  If the base name was unique, returns it directly.
     *
     * @param name The name upon which to base the unique name.
     *
     * @return A unique name.
     */
    public @NonNull String generateUniqueName(@NonNull String name) {
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
        return name;
    }

    /**
     * @return The number of names that have been used.
     */
    public int size() {
        return mCanonicalMap.size();
    }

    /**
     * @param name The string to look up.
     *
     * @return True if {@code name} is a registered name.
     */
    public boolean hasName(String name) {
        return mCanonicalMap.containsKey(makeCanonical(name));
    }

    /**
     * @param name The name of the item desired.
     * @return The value associated with {@code name}, if any. Otherwise null.
     */
    @Nullable
    public T getValueOf(String name) {
        NameEntry<T> entry = mCanonicalMap.get(makeCanonical(name));
        return entry == null ? null : entry.mValue;
    }

    /**
     * @param index The desired index, must be between 0 and {@link #size()}-1.
     * @return The NameEntry at {@code index}.
     */
    public NameEntry<T> entryAt(int index) {
       return mCanonicalMap.valueAt(index);
    }

    /**
     * Adds the name to the list of used names.  Does not check if the name is already there.
     *
     * @param name The name to add.
     * @throws IllegalArgumentException If the name is not valid.
     */
    public void put(String name, T value) {
        if (!isValidName(name)) {
            throw new IllegalArgumentException("Invalid name \"" + name + "\".");
        }
        String canonical = makeCanonical(name);
        if (mCanonicalMap.put(canonical, new NameEntry<>(name, value)) == null) {
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
     * @return The removed value.
     */
    public T remove(String toRemove) {
        String canonical = makeCanonical(toRemove);
        NameEntry<T> removed = mCanonicalMap.remove(canonical);
        if (removed != null) {
            mDisplayNamesSorted.remove(toRemove);
            notifyChanged();
        }
        return removed.mValue;
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
     * Otherwise, returns null.
     * @param proposedName The proposed name.
     * @return A previous added display name that shares the same canonical form.
     *         Otherwise return null.
     */
    @Nullable
    public String getExisting(String proposedName) {
        NameEntry<T> existingEntry = mCanonicalMap.get(makeCanonical(proposedName));
        return existingEntry == null ? proposedName : existingEntry.mDisplayName;
    }

    /**
     * @param name The proposed name
     * @return The canonical string for the provided name, as used by {@link #mCanonicalMap}.
     */
    protected String makeCanonical(@NonNull String name) {
        return name.toLowerCase();
    }

}
