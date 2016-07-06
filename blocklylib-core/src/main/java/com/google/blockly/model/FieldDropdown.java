/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.blockly.model;

import android.text.TextUtils;

import com.google.blockly.utils.BlockLoadingException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds a dropdown list to an Input.
 */
public final class FieldDropdown extends Field<FieldDropdown.Observer> {
    private static final String TAG = "FieldDropdown";

    private List<Option> mOptions;
    private int mCurrentSelection = 0;

    public FieldDropdown(String name) {
        super(name, TYPE_DROPDOWN);
        mOptions = new ArrayList<>(0);
    }

    public FieldDropdown(String name, List<Option> options) {
        super(name, TYPE_DROPDOWN);
        mOptions = (options != null) ? options : new ArrayList<Option>(0);
    }

    public static FieldDropdown fromJson(JSONObject json) throws BlockLoadingException {
        String name = json.optString("name");
        if (TextUtils.isEmpty(name)) {
            throw new BlockLoadingException("field_dropdown \"name\" attribute must not be empty.");
        }

        JSONArray jsonOptions = json.optJSONArray("options");
        ArrayList<Option> options = null;
        if (jsonOptions != null) {
            int count = jsonOptions == null ? 0 :jsonOptions.length();
            options = new ArrayList<>(count);

            for (int i = 0; i < count; i++) {
                JSONArray option = null;
                try {
                    option = jsonOptions.getJSONArray(i);
                } catch (JSONException e) {
                    throw new BlockLoadingException("Error reading dropdown options.", e);
                }
                if (option != null && option.length() == 2) {
                    try {
                        String displayName = option.getString(0);
                        String value = option.getString(1);
                        if (TextUtils.isEmpty(value)) {
                            throw new BlockLoadingException("Option values may not be empty");
                        }
                        options.add(new Option(value, displayName));
                    } catch (JSONException e) {
                        throw new BlockLoadingException("Error reading option values.", e);
                    }
                }
            }
        }
        return new FieldDropdown(name, options);
    }

    @Override
    public FieldDropdown clone() {
        FieldDropdown copy = new FieldDropdown(getName(), new ArrayList<>(mOptions));
        copy.setSelectedIndex(mCurrentSelection);
        return copy;
    }

    @Override
    public boolean setFromString(String text) {
        setSelectedValue(text);
        return true;
    }

    /**
     * Sets the list of options. Each Pair in the list must have a display name as the first
     * parameter and the value as the second parameter.
     *
     * @param options A list of options consisting of pairs of displayName/value.
     */
    public void setOptions(List<Option> options) {
        String previousValue = getSerializedValue();
        mOptions = options;
        setSelectedValue(previousValue);
    }


    /**
     * Set the list of options this field displays. The parameters must be two arrays of equal
     * length.
     *
     * @param values The values for the options.
     * @param displayNames The names to display for the options.
     */
    public void setOptions(List<String> values, List<String> displayNames) {
        if (values == null && displayNames == null) {
            mOptions.clear();
            return;
        }
        if ((values == null && displayNames != null) || (displayNames == null && values != null)) {
            throw new IllegalArgumentException("displayNames and values must both be non-null");
        }

        final int count = values.size();
        if (displayNames.size() != count) {
            throw new IllegalArgumentException("displayNames and values must be the same length.");
        }
        ArrayList<Option> options = new ArrayList<>(count);
        for (int i = 0; i < count; ++i) {
            options.add(new Option(values.get(i), displayNames.get(i)));
        }
        setOptions(options);
    }

    /**
     * @return The value of the currently selected option.
     */
    public String getSelectedValue() {
        return mOptions.size() == 0 ? null : mOptions.get(mCurrentSelection).value;
    }

    /**
     * Update the selection index to the first available option that has the given value. If
     * there are no options the index will be set to -1. If the value given is empty or does
     * not exist the index will be set to 0.
     *
     * @param value The value of the option to select.
     */
    public void setSelectedValue(String value) {
        if (mOptions.size() == 0) {
            mCurrentSelection = -1;
        } else if (TextUtils.isEmpty(value)) {
            setSelectedIndex(0);
        } else {
            boolean found = false;
            for (int i = 0; i < mOptions.size(); i++) {
                if (TextUtils.equals(value, mOptions.get(i).value)) {
                    setSelectedIndex(i);
                    found = true;
                    break;
                }
            }
            if (!found) {
                setSelectedIndex(0);
            }
        }
    }

    /**
     * @return The display name of the currently selected option.
     */
    public String getSelectedDisplayName() {
        return mOptions.size() == 0 ? null : mOptions.get(mCurrentSelection).displayName;
    }

    /**
     * @return The index of the currently selected option.
     */
    public int getSelectedIndex() {
        return mCurrentSelection;
    }

    /**
     * Sets the current selected option.
     *
     * @param index The index to select.
     */
    public void setSelectedIndex(int index) {
        if (index < 0 || index >= mOptions.size()) {
            throw new IllegalArgumentException(
                    "Index must be between 0 and the number of options - 1");
        }

        // If value selected index has changed, update current selection and (if it exists) let
        // the observers know.
        if (mCurrentSelection != index) {
            int oldIndex = mCurrentSelection;
            mCurrentSelection = index;
            onSelectionChanged(this, oldIndex, index);
        }
    }

    /**
     * @return A list of all of the display names in order.
     */
    public List<String> getDisplayNames() {
        List<String> list = new ArrayList<>(mOptions.size());
        for (int i = 0; i < mOptions.size(); i++) {
            list.add(mOptions.get(i).displayName);
        }
        return list;
    }

    @Override
    public String getSerializedValue() {
        return getSelectedValue();
    }

    private void onSelectionChanged(FieldDropdown field, int oldIndex, int newIndex) {
        for (int i = 0; i < mObservers.size(); i++) {
            mObservers.get(i).onSelectionChanged(field, oldIndex, newIndex);
        }
    }

    public int getOptionCount() {
        return mOptions == null ? 0 : mOptions.size();
    }

    /**
     * Observer for listening to changes to a variable field.
     */
    public interface Observer {
        /**
         * Called when the field's selected index changed.
         *
         * @param field The field that changed.
         * @param oldIndex The field's previously selected index.
         * @param newIndex The field's new selected index.
         */
        void onSelectionChanged(FieldDropdown field, int oldIndex, int newIndex);
    }

    public static class Option {
        public final String value;
        public final String displayName;

        public Option(String value, String displayName) {
            if (TextUtils.isEmpty(value)) {
                throw new IllegalArgumentException("Dropdown option value cannot be null or empty");
            }
            this.value = value;
            this.displayName = TextUtils.isEmpty(displayName) ? value : displayName;
        }
    }
}
