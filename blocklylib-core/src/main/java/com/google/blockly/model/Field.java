/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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
import android.graphics.Color;
import android.support.annotation.IntDef;
import android.support.v4.util.SimpleArrayMap;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.google.blockly.android.ui.fieldview.FieldCheckboxView;
import com.google.blockly.android.ui.fieldview.FieldColourView;
import com.google.blockly.android.ui.fieldview.FieldDateView;
import com.google.blockly.android.ui.fieldview.FieldDropdownView;
import com.google.blockly.android.ui.fieldview.FieldLabelView;
import com.google.blockly.android.ui.fieldview.FieldVariableView;
import com.google.blockly.android.ui.fieldview.FieldView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * The base class for Fields in Blockly. A field is the smallest piece of a {@link Block} and is
 * wrapped by an {@link Input}.
 */
// TODO: (#97) Split fields out into their own files.
// TODO: (#98) Remove references to views and have views observe their fields instead.
public abstract class Field<T> extends Observable<T> implements Cloneable {
    private static final String TAG = "Field";
    public static final int TYPE_UNKNOWN = -1;
    public static final int TYPE_LABEL = 0;
    public static final int TYPE_INPUT = 1;
    public static final int TYPE_ANGLE = 2;
    public static final int TYPE_CHECKBOX = 3;
    public static final int TYPE_COLOUR = 4;
    public static final int TYPE_DATE = 5;
    public static final int TYPE_VARIABLE = 6;
    public static final int TYPE_DROPDOWN = 7;
    public static final int TYPE_IMAGE = 8;
    private static final String TYPE_LABEL_STRING = "field_label";
    private static final String TYPE_INPUT_STRING = "field_input";
    private static final String TYPE_ANGLE_STRING = "field_angle";
    private static final String TYPE_CHECKBOX_STRING = "field_checkbox";
    private static final String TYPE_COLOUR_STRING = "field_colour";
    private static final String TYPE_DATE_STRING = "field_date";
    private static final String TYPE_VARIABLE_STRING = "field_variable";
    private static final String TYPE_DROPDOWN_STRING = "field_dropdown";
    private static final String TYPE_IMAGE_STRING = "field_image";
    private final String mName;
    private final int mType;
    protected FieldView mView;

    public Field(String name, @FieldType int type) {
        mName = name;
        mType = type;
    }

    @Override
    public Field clone() throws CloneNotSupportedException {
        return (Field) super.clone();
    }

    /**
     * Writes information about the editable parts of the field as XML.
     *
     * @param serializer The XmlSerializer to write to.
     *
     * @throws IOException
     */
    public void serialize(XmlSerializer serializer) throws IOException {
        if (mType == TYPE_LABEL || mType == TYPE_IMAGE) {
            return;
        }
        serializer.startTag(null, "field").attribute(null, "name", mName);
        serializeInner(serializer);
        serializer.endTag(null, "field");
    }

    /**
     * Get the name of this field. Names, if they are not null, are expected to be unique within a
     * block but are not guaranteed to be.
     */
    public String getName() {
        return mName;
    }

    /**
     * Get the type of this field. There should be a concrete implementation for each field type.
     */
    @FieldType
    public int getType() {
        return mType;
    }

    /**
     * @return The view that renders this field.
     */
    public FieldView getView() {
        return mView;
    }

    /**
     * Sets the view that renders this field.
     */
    public void setView(FieldView view) {
        mView = view;
    }

    /**
     * Sets the values of the field from a string.
     * <p/>
     * This is used for setting values of all types of fields when loading a workspace from XML. It
     * is also used, however, as the primary means of setting text fields (e.g., inputs, labels,
     * dates).
     * <p/>
     * There should be a concrete implementation for each field type.
     *
     * @param text The text value for this field from the XML.
     *
     * @return True if the value was set, false otherwise.
     */
    public abstract boolean setFromString(String text);

    /**
     * Checks if the given type name is a known field type.
     *
     * @param typeString The type to check.
     *
     * @return true if this is a known field type, false otherwise.
     */
    public static boolean isFieldType(String typeString) {
        return stringToFieldType(typeString) != TYPE_UNKNOWN;
    }

    /**
     * Create a new {@link Field} instance from JSON.  If the type is not recognized
     * null will be returned. If the JSON is invalid or there is an error reading the data a
     * {@link RuntimeException} will be thrown.
     *
     * @param json The JSON to generate the Field from.
     *
     * @return A Field of the appropriate type.
     *
     * @throws RuntimeException
     */
    public static Field fromJson(JSONObject json) {
        String type = null;
        try {
            type = json.getString("type");
        } catch (JSONException e) {
            throw new RuntimeException("Error getting the field type.", e);
        }

        // If new fields are added here FIELD_TYPES should also be updated.
        Field field = null;
        switch (type) {
            case TYPE_LABEL_STRING:
                field = new FieldLabel(json);
                break;
            case TYPE_INPUT_STRING:
                field = new FieldInput(json);
                break;
            case TYPE_ANGLE_STRING:
                field = new FieldAngle(json);
                break;
            case TYPE_CHECKBOX_STRING:
                field = new FieldCheckbox(json);
                break;
            case TYPE_COLOUR_STRING:
                field = new FieldColour(json);
                break;
            case TYPE_DATE_STRING:
                field = new FieldDate(json);
                break;
            case TYPE_VARIABLE_STRING:
                field = new FieldVariable(json);
                break;
            case TYPE_DROPDOWN_STRING:
                field = new FieldDropdown(json);
                break;
            case TYPE_IMAGE_STRING:
                field = new FieldImage(json);
                break;
            default:
                Log.w(TAG, "Unknown field type.");
                break;
        }
        return field;
    }

    /**
     * Convert string representation of field type to internal integer Id.
     *
     * @param typeString The field type string, e.g., TYPE_LABEL_STRING ("field_label").
     *
     * @return The integer Id representing the given field type string, or {@code #TYPE_UNKNOWN} if
     * the string does not represent a valid type.
     *
     * @throws IllegalArgumentException If the string is null.
     */
    @FieldType
    private static int stringToFieldType(String typeString) {
        if (typeString == null) {
            throw new IllegalArgumentException("type may not be null");
        }

        switch (typeString) {
            case TYPE_LABEL_STRING:
                return TYPE_LABEL;
            case TYPE_INPUT_STRING:
                return TYPE_INPUT;
            case TYPE_ANGLE_STRING:
                return TYPE_ANGLE;
            case TYPE_CHECKBOX_STRING:
                return TYPE_CHECKBOX;
            case TYPE_COLOUR_STRING:
                return TYPE_COLOUR;
            case TYPE_DATE_STRING:
                return TYPE_DATE;
            case TYPE_VARIABLE_STRING:
                return TYPE_VARIABLE;
            case TYPE_DROPDOWN_STRING:
                return TYPE_DROPDOWN;
            case TYPE_IMAGE_STRING:
                return TYPE_IMAGE;
            default:
                return TYPE_UNKNOWN;
        }
    }

    /**
     * Writes the value of the Field as a string.
     *
     * @param serializer The XmlSerializer to write to.
     *
     * @throws IOException
     */
    protected abstract void serializeInner(XmlSerializer serializer) throws IOException;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({TYPE_UNKNOWN, TYPE_LABEL, TYPE_INPUT, TYPE_ANGLE, TYPE_CHECKBOX, TYPE_COLOUR,
            TYPE_DATE, TYPE_VARIABLE, TYPE_DROPDOWN, TYPE_IMAGE})
    public @interface FieldType {
    }

    /**
     * Adds a text to an Input. This can be used to add text to the block or label
     * another field. The text is not modifiable by the user.
     */
    public static final class FieldLabel extends Field<FieldLabel.Observer> {
        private String mText;

        public FieldLabel(String name, String text) {
            super(name, TYPE_LABEL);
            mText = text == null ? "" : text;
        }

        private FieldLabel(JSONObject json) {
            this(json.optString("name", null), json.optString("text", ""));
        }

        @Override
        public FieldLabel clone() throws CloneNotSupportedException {
            return (FieldLabel) super.clone();
        }

        /**
         * @return The text for this label.
         */
        public String getText() {
            return mText;
        }

        /**
         * Sets the text for this label. Changes to the label will not be serialized by Blockly and
         * should not be caused by user input. For user editable text fields use
         * {@link com.google.blockly.model.Field.FieldInput} instead.
         */
        public void setText(String text) {
            if (!TextUtils.equals(text, mText)) {
                String oldText = mText;
                mText = text;
                if (mView != null) {
                    ((FieldLabelView)mView).setText(text);
                }
                onTextChanged(oldText, text);
            }
        }

        @Override
        public boolean setFromString(String text) {
            throw new IllegalStateException("Label field text cannot be set after construction.");
        }

        @Override
        protected void serializeInner(XmlSerializer serializer) throws IOException {
            // Nothing to do.
        }

        private void onTextChanged(String oldText, String newText) {
            for (int i = 0; i < mObservers.size(); i++) {
                mObservers.get(i).onTextChanged(this, oldText, newText);
            }
        }

        /**
         * Observer for listening to changes to an input field.
         */
        public interface Observer {
            /**
             * Called when the field's text changed.
             * <p>
             * Note: Label fields are not expected to be user editable and are not serialized by
             * Blockly's core library.
             *
             * @param field The field that changed.
             * @param oldText The field's previous text.
             * @param newText The field's new text.
             */
            void onTextChanged(FieldLabel field, String oldText, String newText);
        }
    }

    /**
     * Adds an editable text input to an Input.
     */
    public static final class FieldInput extends Field<FieldInput.Observer> {
        private String mText;

        public FieldInput(String name, String text) {
            super(name, TYPE_INPUT);
            mText = text;
        }

        private FieldInput(JSONObject json) {
            this(json.optString("name", "NAME"), json.optString("text", "default"));
            // TODO: consider replacing default text with string resource
        }

        @Override
        public FieldInput clone() throws CloneNotSupportedException {
            return (FieldInput) super.clone();
        }

        @Override
        public boolean setFromString(String text) {
            setText(text);
            return true;
        }

        /**
         * @return The text the user has entered.
         */
        public String getText() {
            return mText;
        }

        /**
         * Sets the current text in this Field.
         *
         * @param text The text to replace the field content with.
         */
        public void setText(String text) {
            if (!TextUtils.equals(text, mText)) {
                String oldText = mText;
                mText = text;
                onTextChanged(oldText, text);
            }
        }

        @Override
        protected void serializeInner(XmlSerializer serializer) throws IOException {
            serializer.text(mText == null ? "" : mText);
        }

        private void onTextChanged(String oldText, String newText) {
            for (int i = 0; i < mObservers.size(); i++) {
                mObservers.get(i).onTextChanged(this, oldText, newText);
            }
        }

        /**
         * Observer for listening to changes to an input field.
         */
        public interface Observer {
            /**
             * Called when the field's text changed.
             *
             * @param field The field that changed.
             * @param oldText The field's previous text.
             * @param newText The field's new text.
             */
            void onTextChanged(FieldInput field, String oldText, String newText);
        }
    }

    /**
     * Adds an angle (0-360) picker to an Input.
     */
    public static final class FieldAngle extends Field<FieldAngle.Observer> {
        private int mAngle;

        public FieldAngle(String name, int angle) {
            super(name, TYPE_ANGLE);
            setAngle(angle);
        }

        private FieldAngle(JSONObject json) {
            this(json.optString("name", "NAME"), json.optInt("angle", 90));
        }

        @Override
        public FieldAngle clone() throws CloneNotSupportedException {
            return (FieldAngle) super.clone();
        }

        @Override
        public boolean setFromString(String text) {
            try {
                setAngle(Integer.parseInt(text));
            } catch (NumberFormatException e) {
                return false;
            }
            return true;
        }

        /**
         * @return The angle set by the user.
         */
        public int getAngle() {
            return mAngle;
        }

        /**
         * Set the current angle in this field. The angle will be wrapped to be in the range
         * 0-360.
         *
         * @param angle The angle to set this field to.
         */
        public void setAngle(int angle) {
            int newAngle;
            if (angle == 360) {
                newAngle = angle;
            } else {
                angle = angle % 360;
                if (angle < 0) {
                    angle += 360;
                }
                newAngle = angle;
            }

            if (newAngle != mAngle) {
                int oldAngle = mAngle;
                mAngle = newAngle;
                onAngleChanged(oldAngle, newAngle);
            }
        }

        @Override
        protected void serializeInner(XmlSerializer serializer) throws IOException {
            serializer.text(Integer.toString(mAngle));
        }

        private void onAngleChanged(int oldAngle, int newAngle) {
            for (int i = 0; i < mObservers.size(); i++) {
                mObservers.get(i).onAngleChanged(this, oldAngle, newAngle);
            }
        }

        /**
         * Observer for listening to changes to an angle field.
         */
        public interface Observer {
            /**
             * Called when the field's angle changed.
             *
             * @param field The field that changed.
             * @param oldAngle The field's previous angle.
             * @param newAngle The field's new angle.
             */
            void onAngleChanged(Field field, int oldAngle, int newAngle);
        }
    }

    /**
     * Adds a toggleable checkbox to an Input.
     */
    public static final class FieldCheckbox extends Field<FieldCheckbox.Observer> {
        private boolean mChecked;

        public FieldCheckbox(String name, boolean checked) {
            super(name, TYPE_CHECKBOX);
            mChecked = checked;
        }

        private FieldCheckbox(JSONObject json) {
            this(json.optString("name", "NAME"), json.optBoolean("checked", true));
        }

        @Override
        public FieldCheckbox clone() throws CloneNotSupportedException {
            return (FieldCheckbox) super.clone();
        }

        @Override
        public boolean setFromString(String text) {
            mChecked = Boolean.parseBoolean(text);
            return true;
        }

        /**
         * @return The current state of the checkbox.
         */
        public boolean isChecked() {
            return mChecked;
        }

        /**
         * Sets the state of the checkbox.
         */
        public void setChecked(boolean checked) {
            if (mChecked != checked) {
                mChecked = checked;
                if (mView != null) {
                    ((FieldCheckboxView) mView).setChecked(mChecked);
                }
                onCheckChanged(checked);
            }
        }

        @Override
        protected void serializeInner(XmlSerializer serializer) throws IOException {
            serializer.text(mChecked ? "true" : "false");
        }

        private void onCheckChanged(boolean newState) {
            for (int i = 0; i < mObservers.size(); i++) {
                mObservers.get(i).onCheckChanged(this, newState);
            }
        }

        /**
         * Observer for listening to changes to a checkbox field.
         */
        public interface Observer {
            /**
             * Called when the field's checked value changed.
             *
             * @param field The field that changed.
             * @param newState The new state of the checkbox.
             */
            void onCheckChanged(FieldCheckbox field, boolean newState);
        }
    }

    /**
     * Adds a colour picker to an Input.
     */
    public static final class FieldColour extends Field<FieldColour.Observer> {
        /*package*/ static final int DEFAULT_COLOUR = 0xff0000;

        private int mColour;

        public FieldColour(String name) {
            this(name, DEFAULT_COLOUR);
        }

        public FieldColour(String name, int colour) {
            super(name, TYPE_COLOUR);
            setColour(colour);
        }

        private FieldColour(JSONObject json) {
            this(json.optString("name", "NAME"), DEFAULT_COLOUR);
            String colourString = json.optString("colour");
            if (!TextUtils.isEmpty(colourString)) {
                setColour(Color.parseColor(colourString));
            }
        }

        @Override
        public FieldColour clone() throws CloneNotSupportedException {
            return (FieldColour) super.clone();
        }

        @Override
        public boolean setFromString(String text) {
            try {
                setColour(Color.parseColor(text));
            } catch (IllegalArgumentException e) {
                return false;
            }
            return true;
        }

        /**
         * @return The current colour in this field.
         */
        public int getColour() {
            return mColour;
        }

        /**
         * Sets the colour stored in this field.
         *
         * @param colour A colour in the form 0xRRGGBB
         */
        public void setColour(int colour) {
            final int newColour = 0xFFFFFF & colour;
            if (mColour != newColour) {
                int oldColour = mColour;
                mColour = newColour;
                if (mView != null) {
                    ((FieldColourView) mView).setColour(mColour);
                }
                onColourChanged(oldColour, newColour);
            }
        }

        @Override
        protected void serializeInner(XmlSerializer serializer) throws IOException {
            serializer.text(String.format("#%02x%02x%02x",
                    Color.red(mColour), Color.green(mColour), Color.blue(mColour)));
        }

        private void onColourChanged(int oldColour, int newColour) {
            for (int i = 0; i < mObservers.size(); i++) {
                mObservers.get(i).onColourChanged(this, oldColour, newColour);
            }
        }

        /**
         * Observer for listening to changes to a colour field.
         */
        public interface Observer {
            /**
             * Called when the field's colour changed.
             *
             * @param field The field that changed.
             * @param oldColour The field's previous colour.
             * @param newColour The field's new colour.
             */
            void onColourChanged(Field field, int oldColour, int newColour);
        }
    }

    /**
     * Adds a date picker to an Input. Dates must be in the format "YYYY-MM-DD"
     */
    public static final class FieldDate extends Field<FieldDate.Observer> {
        private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
        private final Date mDate;

        public FieldDate(String name, String dateString) {
            super(name, TYPE_DATE);
            Date date = null;
            if (!TextUtils.isEmpty(dateString)) {
                try {
                    date = DATE_FORMAT.parse(dateString);
                } catch (ParseException e) {
                    Log.e(TAG, "Unable to parse date " + dateString, e);
                }
            }
            if (date == null) {
                date = new Date();
            }
            mDate = date;
        }

        public FieldDate(FieldDate input) {
            super(input.getName(), TYPE_DATE);
            mDate = (Date) input.mDate.clone();
        }

        private FieldDate(JSONObject json) {
            this(json.optString("name", "NAME"), json.optString("date"));
        }

        @Override
        public FieldDate clone() throws CloneNotSupportedException {
            return new FieldDate(this);
        }

        @Override
        public boolean setFromString(String text) {
            Date date = null;
            try {
                date = DATE_FORMAT.parse(text);
                setDate(date);
                return true;
            } catch (ParseException e) {
                Log.e(TAG, "Unable to parse date " + text, e);
                return false;
            }
        }

        /**
         * @return The date in this field.
         */
        public Date getDate() {
            return mDate;
        }

        /**
         * Sets this field to the specified {@link Date}.
         */
        public void setDate(Date date) {
            if (date == null) {
                throw new IllegalArgumentException("Date may not be null.");
            }
            setTime(date.getTime());
        }

        /**
         * @return The string format for the date in this field.
         */
        public String getDateString() {
            return DATE_FORMAT.format(mDate);
        }

        /**
         * Sets this field to a specific time.
         *
         * @param millis The time in millis since UNIX Epoch.
         */
        public void setTime(long millis) {
            long oldTime = mDate.getTime();
            if (millis != oldTime) {
                mDate.setTime(millis);
                if (mView != null) {
                    ((FieldDateView) mView).setText(getDateString());
                }
                onDateChanged(oldTime, millis);
            }
        }

        @Override
        protected void serializeInner(XmlSerializer serializer) throws IOException {
            serializer.text(DATE_FORMAT.format(mDate));
        }

        private void onDateChanged(long oldMillis, long newMillis) {
            for (int i = 0; i < mObservers.size(); i++) {
                mObservers.get(i).onDateChanged(this, oldMillis, newMillis);
            }
        }

        /**
         * Observer for listening to changes to a date field.
         */
        public interface Observer {
            /**
             * Called when the field's date changed.
             *
             * @param field The field that changed.
             * @param oldMillis The field's previous time in UTC millis since epoch.
             * @param newMillis The field's new time in UTC millis since epoch.
             */
            void onDateChanged(Field field, long oldMillis, long newMillis);
        }
    }

    /**
     * Adds a variable to an Input.
     */
    public static final class FieldVariable extends Field<FieldVariable.Observer> {
        private String mVariable;

        public FieldVariable(String name, String variable) {
            super(name, TYPE_VARIABLE);
            mVariable = variable;
        }

        private FieldVariable(JSONObject json) {
            this(json.optString("name", "NAME"), json.optString("variable", "item"));
        }

        @Override
        public FieldVariable clone() throws CloneNotSupportedException {
            return new FieldVariable(getName(), mVariable);
        }

        @Override
        public boolean setFromString(String text) {
            if (TextUtils.isEmpty(text)) {
                return false;
            }
            setVariable(text);
            return true;
        }

        /**
         * @return The name of the variable that is set.
         */
        public String getVariable() {
            return mVariable;
        }

        /**
         * Sets the variable in this field. All variables are considered global and must be unique.
         * Two variables with the same name will be considered the same variable at generation.
         */
        public void setVariable(String variable) {
            if ((mVariable == null && variable != null)
                    || (mVariable != null && !mVariable.equalsIgnoreCase(variable))) {
                String oldVar = mVariable;
                mVariable = variable;
                onVariableChanged(this, oldVar, variable);
                if (mView != null) {
                    ((FieldVariableView)mView).setSelection(mVariable);
                }
            }
        }

        @Override
        protected void serializeInner(XmlSerializer serializer) throws IOException {
            serializer.text(mVariable);
        }

        private void onVariableChanged(FieldVariable field, String oldVar, String newVar) {
            for (int i = 0; i < mObservers.size(); i++) {
                mObservers.get(i).onVariableChanged(field, oldVar, newVar);
            }
        }

        /**
         * Observer for listening to changes to a variable field.
         */
        public interface Observer {
            /**
             * Called when the field's variable name changed.
             *
             * @param field The field that changed.
             * @param oldVar The field's previous variable name.
             * @param newVar The field's new variable name.
             */
            void onVariableChanged(FieldVariable field, String oldVar, String newVar);
        }
    }

    /**
     * Adds a dropdown list to an Input.
     */
    public static final class FieldDropdown extends Field<FieldDropdown.Observer> {
        // TODO: consider other data structures
        private SimpleArrayMap<String, String> mOptions = new SimpleArrayMap<>();
        private int mCurrentSelection = 0;

        public FieldDropdown(String name, String[] displayNames, String[] values) {
            super(name, TYPE_DROPDOWN);
            setOptions(displayNames, values);
        }

        public FieldDropdown(String name, SimpleArrayMap<String, String> options) {
            super(name, TYPE_DROPDOWN);
            setOptions(options);
        }

        private FieldDropdown(JSONObject json) {
            this(json.optString("name", "NAME"), null, null);
            JSONArray jsonOptions = json.optJSONArray("options");
            if (jsonOptions != null) {
                SimpleArrayMap<String, String> options = new SimpleArrayMap<>();
                for (int i = 0; i < jsonOptions.length(); i++) {
                    JSONArray option = null;
                    try {
                        option = jsonOptions.getJSONArray(i);
                    } catch (JSONException e) {
                        throw new RuntimeException("Error reading dropdown options.", e);
                    }
                    if (option != null && option.length() == 2) {
                        try {
                            String displayName = option.getString(0);
                            String value = option.getString(1);
                            if (TextUtils.isEmpty(value)) {
                                throw new IllegalArgumentException("Option values may not be empty");
                            }
                            options.put(displayName, value);
                        } catch (JSONException e) {
                            throw new RuntimeException("Error reading option values.", e);
                        }
                    }
                }
                setOptions(options);
            }
        }

        @Override
        public FieldDropdown clone() throws CloneNotSupportedException {
            FieldDropdown field = (FieldDropdown) super.clone();
            field.mOptions = new SimpleArrayMap<>(mOptions);
            return field;
        }

        @Override
        public boolean setFromString(String text) {
            setSelectedValue(text);
            return true;
        }

        /**
         * @return The list of options available in this dropdown.
         */
        public SimpleArrayMap<String, String> getOptions() {
            return mOptions;
        }

        /**
         * Sets the list of options. Each Pair in the list must have a display name as the first
         * parameter and the value as the second parameter.
         *
         * @param options A list of options consisting of pairs of displayName/value.
         */
        public void setOptions(SimpleArrayMap<String, String> options) {
            String previousValue = getSelectedValue();
            mOptions = new SimpleArrayMap<>(options);
            setSelectedValue(previousValue);
        }

        /**
         * @return The value of the currently selected option.
         */
        public String getSelectedValue() {
            return mOptions.size() == 0 ? null : mOptions.valueAt(mCurrentSelection);
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
                    if (TextUtils.equals(value, mOptions.valueAt(i))) {
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
            return mOptions.size() == 0 ? null : mOptions.keyAt(mCurrentSelection);
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
            // the FieldDropdownView know.
            if (mCurrentSelection != index) {
                int oldIndex = mCurrentSelection;
                mCurrentSelection = index;
                if (mView != null) {
                    ((FieldDropdownView) mView).setSelection(mCurrentSelection);
                }
                onSelectionChanged(this, oldIndex, index);
            }
        }

        /**
         * Set the list of options this field displays. The parameters must be two arrays of equal
         * length.
         *
         * @param displayNames The names to display for the options.
         * @param values The values for the options.
         */
        public void setOptions(String[] displayNames, String[] values) {
            String previousValue = getSelectedValue();
            if (displayNames != null) {
                if (values == null) {
                    throw new IllegalArgumentException(
                            "displayNames and values must both be non-null");
                }
                if (displayNames.length != values.length) {
                    throw new IllegalArgumentException(
                            "displayNames and values must be the same length.");
                }
                mOptions.clear();
                for (int i = 0; i < values.length; i++) {
                    mOptions.put(displayNames[i], values[i]);
                }
            } else if (values != null) {
                throw new IllegalArgumentException("displayNames and values must both be non-null");
            } else {
                Log.w(TAG, "No options set, may not be displayed.");
                mOptions.clear();
            }
            setSelectedValue(previousValue);
        }

        /**
         * @return A list of all of the display names in order.
         */
        public List<String> getDisplayNames() {
            List<String> list = new ArrayList<>(mOptions.size());
            for (int i = 0; i < mOptions.size(); i++) {
                list.add(mOptions.keyAt(i));
            }
            return list;
        }

        @Override
        protected void serializeInner(XmlSerializer serializer) throws IOException {
            serializer.text(getSelectedValue());
        }

        private void onSelectionChanged(FieldDropdown field, int oldIndex, int newIndex) {
            for (int i = 0; i < mObservers.size(); i++) {
                mObservers.get(i).onSelectionChanged(field, oldIndex, newIndex);
            }
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
    }

    /**
     * Adds an image to an Input.
     */
    public static final class FieldImage extends Field<FieldImage.Observer> {
        private String mSrc;
        private int mWidth;
        private int mHeight;
        private String mAltText;

        public FieldImage(String name, String src, int width, int height, String altText) {
            super(name, TYPE_IMAGE);
            mSrc = src;
            mWidth = width;
            mHeight = height;
            mAltText = altText;
        }

        private FieldImage(JSONObject json) {
            this(json.optString("name"),
                    json.optString("src", "https://www.gstatic.com/codesite/ph/images/star_on.gif"),
                    json.optInt("width", 15), json.optInt("height", 15),
                    json.optString("alt", "*"));
        }

        @Override
        public FieldImage clone() throws CloneNotSupportedException {
            return (FieldImage) super.clone();
        }

        /**
         * @return The source for the image.
         */
        public String getSource() {
            return mSrc;
        }

        /**
         * @return The display width of the image in dips.
         */
        public int getWidth() {
            return mWidth;
        }

        /**
         * @return The display height of the image in dips.
         */
        public int getHeight() {
            return mHeight;
        }

        /**
         * @return The alt-text for the image.
         */
        public String getAltText() {
            return mAltText;
        }

        /**
         * Sets the alt-text for the image.
         */
        public void setAltText(String altText) {
            mAltText = altText;
        }

        /**
         * Sets a new image to be shown.
         *
         * @param src A web address or Blockly reference to the image.
         * @param width The display width of the image in dips.
         * @param height The display height of the image in dips.
         */
        public void setImage(String src, int width, int height) {
            if (!TextUtils.equals(mSrc, src) || mWidth != width || mHeight != height) {
                mSrc = src;
                mWidth = width;
                mHeight = height;
                if (mView != null) {
                    ((View) mView).requestLayout();
                }
                onImageChanged(src, width, height);
            }
        }

        @Override
        public boolean setFromString(String text) {
            throw new IllegalStateException("Image field cannot be set from string.");
        }

        @Override
        protected void serializeInner(XmlSerializer xmlSerializer) {
            // Something to do here?
        }

        private void onImageChanged(String newSource, int newWidth, int newHeight) {
            for (int i = 0; i < mObservers.size(); i++) {
                mObservers.get(i).onImageChanged(this, newSource, newWidth, newHeight);
            }
        }

        /**
         * Observer for listening to changes to an image field.
         */
        public interface Observer {
            /**
             * Called when the field's image source, width, or height was changed.
             * <p>
             * Note: Image fields are not expected to be user editable and are not serialized by
             * Blockly's core library.
             *
             * @param field The field that changed.
             * @param newSource The new source for the image.
             * @param newWidth The new width of the image.
             * @param newHeight The new height of the image.
             */
            void onImageChanged(FieldImage field, String newSource, int newWidth, int newHeight);
        }
    }
}
