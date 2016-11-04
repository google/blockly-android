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

import android.database.Observable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.blockly.utils.BlockLoadingException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Adds a dropdown list to an Input.
 */
public final class FieldDropdown extends Field {
    private static final String TAG = "FieldDropdown";

    /**
     * An option for a block's dropdown field.
     */
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

    /**
     * The list of all options for a {@link FieldDropdown}.
     */
    public static class Options extends Observable<OptionsObserver> {
        public final List<Option> mOptionList = new ArrayList<>();

        /**
         * Constructs
         *
         * @param options The initial options for the new instance.
         */
        public Options(List<Option> options) {
            mOptionList.addAll(options);
        }

        /**
         * @return A clone of this {@code Options}, with the same list of {@link Option}s.
         */
        public Options clone() {
            return new Options(mOptionList); // Creates a shallow copy of the list contents.
        }

        /**
         * @return True if there are no {@link Option}s. Otherwise false.
         */
        public boolean isEmpty() {
            return mOptionList.isEmpty();
        }

        /**
         * @return The count of {@link Option}s in this list.
         */
        public int size() {
            return mOptionList.size();
        }

        /**
         * @param index The index of the {@link Option} to retrieve.
         * @return The
         */
        public Option get(int index) {
            if (index < 0 || mOptionList.size() <= index) {
                throw new IllegalArgumentException("Index " + index + " is out of bounds. "
                                                   + mOptionList.size() + "Options.");
            }
            return mOptionList.get(index);
        }

        /**
         * Searches options for the first option that matches {@code value}.
         *
         * @param value The value string to match
         * @return The index of the first matching value, or -1 if not found.
         */
        public int getIndexForValue(String value) {
            int count = mOptionList.size();
            for (int i = 0; i < count; ++i) {
                Option option = mOptionList.get(i);
                if (TextUtils.equals(value, option.value)) {
                    return i;
                }
            }
            return -1;
        }

        /**
         * Replaces the current {@link Option} list with {@code option}, and updates all observers.
         *
         * @param options The new of {@link Option}s to use.
         */
        public void updateOptions(List<Option> options) {
            mOptionList.clear();
            mOptionList.addAll(options);

            for (OptionsObserver observer : mObservers) {
                observer.onOptionsUpdated(this);
            }
        }

        /**
         * Updates the list of {@link Option}s from {@code source} into this instance, and updates
         * all observers.
         *
         * @param source The {@link Options} with new of {@link Option}s to use.
         */
        public void copyFrom(Options source) {
            updateOptions(source.mOptionList);
        }
    }

    /**
     * Interface to listen for changes that occur to an {@link Options} list.
     */
    public interface OptionsObserver {
        void onOptionsUpdated(Options options);
    }

    private Options mOptions;
    private Option mSelectedOption = null;
    private int mSelectedIndex = 0;

    private OptionsObserver mOptionsObserver = new OptionsObserver() {
        @Override
        public void onOptionsUpdated(Options options) {
            if (options != mOptions) {
                throw new IllegalStateException("Mismatched Options instance.");
            }
            if (mOptions.isEmpty()) {
                mSelectedOption = null;
                mSelectedIndex = -1;
            } else if (mSelectedOption != null) {
                setSelectedValue(mSelectedOption.value);
            } else {
                mSelectedIndex = 0;
                mSelectedOption = mOptions.get(0);
            }
        }
    };

    public FieldDropdown(String name) {
        super(name, TYPE_DROPDOWN);
        setOptions(new Options(Collections.<Option>emptyList()));
    }

    public FieldDropdown(String name, @Nullable Options options) {
        super(name, TYPE_DROPDOWN);
        setOptions((options != null) ? options : new Options(Collections.<Option>emptyList()));
        if (!options.isEmpty()) {
            mSelectedIndex = 0;
            mSelectedOption = mOptions.get(mSelectedIndex);
        }
    }

    /**
     * Loads a FieldDropdown from JSON. This is usually used for the {@link BlockFactory}'s
     * prototype instances.
     *
     * @param json The JSON representing the object.
     * @return A new FieldDropdown instance.
     * @throws BlockLoadingException
     */
    public static FieldDropdown fromJson(JSONObject json) throws BlockLoadingException {
        String name = json.optString("name");
        if (TextUtils.isEmpty(name)) {
            throw new BlockLoadingException("field_dropdown \"name\" attribute must not be empty.");
        }

        JSONArray jsonOptions = json.optJSONArray("options");
        ArrayList<Option> optionList = null;
        if (jsonOptions != null) {
            int count = jsonOptions == null ? 0 :jsonOptions.length();
            optionList = new ArrayList<>(count);

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
                        optionList.add(new Option(value, displayName));
                    } catch (JSONException e) {
                        throw new BlockLoadingException("Error reading option values.", e);
                    }
                }
            }
        }
        return new FieldDropdown(name, new Options(optionList));
    }

    /**
     * @return A clone of this field, which shares the same options and is initialized to the same
     *         selected value.
     */
    @Override
    public FieldDropdown clone() {
        FieldDropdown copy = new FieldDropdown(getName(), mOptions);
        copy.setSelectedIndex(mSelectedIndex);
        return copy;
    }

    @Override
    public boolean setFromString(String text) {
        setSelectedValue(text);
        return true;
    }

    /**
     * @return The list of options in this dropdown field.
     */
    public Options getOptions() {
        return mOptions;
    }

    /**
     * Sets the {@link FieldDropdown.Options} instance for this field.
     *
     * @param options An {@link FieldDropdown.Options} list, replacing the current options.
     */
    public void setOptions(@NonNull Options options) {
        if (mOptions == options) {
            return;
        }
        if (mOptions != null) {
            mOptions.unregisterObserver(mOptionsObserver);
        }

        mOptions = options;
        mOptions.registerObserver(mOptionsObserver);

        if (mOptions.isEmpty()) {
            mSelectedIndex = -1;
            mSelectedOption = null;
        } else if (mSelectedOption == null) {
            setSelectedIndex(0);
        } else {
            setSelectedValue(mSelectedOption.value);
        }
    }

    /**
     * Sets the available options for this field.
     *
     * @param optionList A list of {@link FieldDropdown.Option}s, to initialize a new
     *                   {@link FieldDropdown.Options} list.
     */
    public void setOptions(@NonNull List<FieldDropdown.Option> optionList) {
        setOptions(new FieldDropdown.Options(optionList));
    }

    /**
     * @return The value of the currently selected option.
     */
    public String getSelectedValue() {
        return mSelectedOption == null ? null : mSelectedOption.value;
    }

    /**
     * Update the selection index to the first available option that has the given value. If
     * there are no options the index will be set to -1. If the value given is empty or does
     * not exist the index will be set to 0.
     *
     * @param newValue The value of the option to select.
     */
    public void setSelectedValue(String newValue) {
        if (mOptions.isEmpty()) {
            String oldValue = getSerializedValue();
            mSelectedIndex = -1;
            mSelectedOption = null;
            fireValueChanged(oldValue, null);
        } else {
            int index = mOptions.getIndexForValue(newValue);
            if (index == -1) {
                index = 0;
            }
            setSelectedIndex(index);
        }
    }

    /**
     * @return The display name of the currently selected option.
     */
    public String getSelectedDisplayName() {
        return mSelectedOption == null ? null : mSelectedOption.displayName;
    }

    /**
     * @return The index of the currently selected option.
     */
    public int getSelectedIndex() {
        return mSelectedIndex;
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
        if (mSelectedIndex != index) {
            String oldValue = getSerializedValue();
            mSelectedIndex = index;
            mSelectedOption = mOptions.get(mSelectedIndex);
            String newValue = getSerializedValue();
            fireValueChanged(oldValue, newValue);
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

    /**
     * @return The value string used by XML serialization, same as {@link #getSelectedValue()}.
     */
    @Override
    public String getSerializedValue() {
        return getSelectedValue();
    }
}
