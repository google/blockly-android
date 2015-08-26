/*
 * Copyright  2015 Google Inc. All Rights Reserved.
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

import android.graphics.Color;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.google.blockly.ui.FieldWorkspaceParams;
import com.google.blockly.ui.fieldview.FieldView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The base class for Fields in Blockly. A field is the smallest piece of a {@link Block} and is
 * wrapped by an {@link Input}.
 */
public abstract class Field implements Cloneable {
    private static final String TAG = "Field";

    public static final String TYPE_LABEL = "field_label";
    public static final String TYPE_INPUT = "field_input";
    public static final String TYPE_ANGLE = "field_angle";
    public static final String TYPE_CHECKBOX = "field_checkbox";
    public static final String TYPE_COLOUR = "field_colour";
    public static final String TYPE_DATE = "field_date";
    public static final String TYPE_VARIABLE = "field_variable";
    public static final String TYPE_DROPDOWN = "field_dropdown";
    public static final String TYPE_IMAGE = "field_image";

    /**
     * The list of known FIELD_TYPES. If this list changes {@link #fromJson(JSONObject)} should
     * also be updated to support the new fields.
     */
    protected static final Set<String> FIELD_TYPES = new HashSet<>();
    static {
        FIELD_TYPES.add(TYPE_LABEL);
        FIELD_TYPES.add(TYPE_INPUT);
        FIELD_TYPES.add(TYPE_ANGLE);
        FIELD_TYPES.add(TYPE_CHECKBOX);
        FIELD_TYPES.add(TYPE_COLOUR);
        FIELD_TYPES.add(TYPE_DATE);
        FIELD_TYPES.add(TYPE_VARIABLE);
        FIELD_TYPES.add(TYPE_DROPDOWN);
        FIELD_TYPES.add(TYPE_IMAGE);
    }

    private final String mName;
    private final String mType;

    private FieldView mView;
    private FieldWorkspaceParams mLayoutParams;

    public Field(String name, String type) {
        if (TextUtils.isEmpty(type)) {
            throw new IllegalArgumentException("type may not be empty");
        }
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
     * @throws IOException
     */
    public void serialize(XmlSerializer serializer) throws IOException {
        if (mType.equals(TYPE_LABEL) || mType.equals(TYPE_IMAGE)) {
            return;
        }
        serializer.startTag(null, mName);
        serializeInner(serializer);
        serializer.endTag(null, mName);
    }

    /**
     * Writes the value of the Field as a string.
     *
     * @param serializer The XmlSerializer to write to.
     * @throws IOException
     */
    protected void serializeInner(XmlSerializer serializer) throws IOException {}

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
    public String getType() {
        return mType;
    }

    /**
     * Sets the view that renders this field.
     */
    public void setView(FieldView view) {
        mView = view;
    }

    /**
     * @return The view that renders this field.
     */
    public FieldView getView() {
        return mView;
    }

    /**
     * Sets the layout params used for rendering this field.
     */
    public void setLayoutParameters(FieldWorkspaceParams params) {
        mLayoutParams = params;
    }

    /**
     * @return The layout params used for rendering this field.
     */
    public FieldWorkspaceParams getLayoutParams() {
        return mLayoutParams;
    }

    /**
     * Sets the values of the field when loading a workspace from XML.
     * There should be a concrete implementation for each field type.
     *
     * @param text The text value for this field from the XML.
     * @return True if the value was set, false otherwise.
     */
    public boolean setFromXmlText(String text) { return false; }

    /**
     * Checks if the given type is a known field type.
     *
     * @param type The type to check.
     * @return true if this is a known field type, false otherwise.
     */
    public static boolean isFieldType(String type) {
        return FIELD_TYPES.contains(type);
    }

    /**
     * Create a new {@link Field} instance from JSON.  If the type is not recognized
     * null will be returned. If the JSON is invalid or there is an error reading the data a
     * {@link RuntimeException} will be thrown.
     *
     * @param json The JSON to generate the Field from.
     * @return A Field of the appropriate type.
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
            case TYPE_LABEL:
                field = new FieldLabel(json);
                break;
            case TYPE_INPUT:
                field = new FieldInput(json);
                break;
            case TYPE_ANGLE:
                field = new FieldAngle(json);
                break;
            case TYPE_CHECKBOX:
                field = new FieldCheckbox(json);
                break;
            case TYPE_COLOUR:
                field = new FieldColour(json);
                break;
            case TYPE_DATE:
                field = new FieldDate(json);
                break;
            case TYPE_VARIABLE:
                field = new FieldVariable(json);
                break;
            case TYPE_DROPDOWN:
                field = new FieldDropdown(json);
                break;
            case TYPE_IMAGE:
                field = new FieldImage(json);
                break;
            default:
                Log.w(TAG, "Unknown field type.");
                break;
        }
        return field;
    }

    /**
     * Adds a text to an Input. This can be used to add text to the block or label
     * another field. The text is not modifiable by the user.
     */
    public static final class FieldLabel extends Field {
        private final String mText;

        public FieldLabel(String name, String text) {
            super(name, TYPE_LABEL);
            mText = text == null ? "" : text;
        }

        @Override
        public FieldLabel clone() throws CloneNotSupportedException {
            return (FieldLabel) super.clone();
        }

        private FieldLabel(JSONObject json) {
            this(json.optString("name", null), json.optString("text", ""));
        }

        /**
         * @return The text for this label.
         */
        public String getText() {
            return mText;
        }
    }

    /**
     * Adds an editable text input to an Input.
     */
    public static final class FieldInput extends Field {
        private String mText;

        public FieldInput(String name, String text) {
            super(name, TYPE_INPUT);
            mText = text;
        }

        @Override
        public FieldInput clone() throws CloneNotSupportedException {
            return (FieldInput) super.clone();
        }

        @Override
        protected void serializeInner(XmlSerializer serializer) throws IOException {
            serializer.text(mText == null ? "" : mText);
        }

        @Override
        public boolean setFromXmlText(String text) {
            setText(text);
            return true;
        }

        private FieldInput(JSONObject json) {
            super(json.optString("name", "NAME"), json.optString("text", "default"));
            // TODO: consider replacing default text with string resource
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
         * @param text The text to replace the contents with.
         */
        public void setText(String text) {
            mText = text;
        }
    }

    /**
     * Adds an angle (0-360) picker to an Input.
     */
    public static final class FieldAngle extends Field {
        private int mAngle;

        public FieldAngle(String name, int angle) {
            super(name, TYPE_ANGLE);
            setAngle(angle);
        }

        @Override
        public FieldAngle clone() throws CloneNotSupportedException {
            return (FieldAngle) super.clone();
        }

        @Override
        protected void serializeInner(XmlSerializer serializer) throws IOException {
            serializer.text(Integer.toString(mAngle));
        }

        @Override
        public boolean setFromXmlText(String text) {
            try {
                setAngle(Integer.parseInt(text));
            } catch (NumberFormatException e) {
                return false;
            }
            return true;
        }

        private FieldAngle(JSONObject json) {
            this(json.optString("name", "NAME"), json.optInt("angle", 90));
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
            if (angle == 360) {
                mAngle = angle;
            } else {
                angle = angle % 360;
                if (angle < 0) {
                    angle += 360;
                }
                mAngle = angle;
            }
        }
    }

    /**
     * Adds a toggleable checkbox to an Input.
     */
    public static final class FieldCheckbox extends Field {
        private boolean mChecked;

        public FieldCheckbox(String name, boolean checked) {
            super(name, TYPE_CHECKBOX);
            mChecked = checked;
        }

        @Override
        public FieldCheckbox clone() throws CloneNotSupportedException {
            return (FieldCheckbox) super.clone();
        }

        @Override
        protected void serializeInner(XmlSerializer serializer) throws IOException {
            serializer.text(mChecked ? "true" : "false");
        }

        @Override
        public boolean setFromXmlText(String text) {
            mChecked = Boolean.parseBoolean(text);
            return true;
        }

        private FieldCheckbox(JSONObject json) {
            this(json.optString("name", "NAME"), json.optBoolean("checked", true));
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
            mChecked = checked;
        }
    }

    /**
     * Adds a colour picker to an Input.
     */
    public static final class FieldColour extends Field {
        /*package*/ static final int DEFAULT_COLOUR = 0xff0000;

        private int mColour;

        public FieldColour(String name) {
            this(name, DEFAULT_COLOUR);
        }
        public FieldColour(String name, int colour) {
            super(name, TYPE_COLOUR);
            setColour(colour);
        }

        @Override
        public FieldColour clone() throws CloneNotSupportedException {
            return (FieldColour) super.clone();
        }

        @Override
        protected void serializeInner(XmlSerializer serializer) throws IOException {
            serializer.text(String.format("#%02x%02x%02x",
                    Color.red(mColour), Color.green(mColour), Color.blue(mColour)));
        }

        @Override
        public boolean setFromXmlText(String text) {
            try {
                setColour(Color.parseColor(text));
            } catch (IllegalArgumentException e) {
                return false;
            }
            return true;
        }

        private FieldColour(JSONObject json) {
            this(json.optString("name", "NAME"), DEFAULT_COLOUR);
            String colourString = json.optString("colour");
            if (!TextUtils.isEmpty(colourString)) {
                setColour(Color.parseColor(colourString));
            }
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
            mColour = 0xFFFFFF & colour;
        }
    }

    /**
     * Adds a date picker to an Input. Dates must be in the format "YYYY-MM-DD"
     */
    public static final class FieldDate extends Field {
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

        @Override
        public FieldDate clone() throws CloneNotSupportedException {
            return new FieldDate(this);
        }

        @Override
        protected void serializeInner(XmlSerializer serializer) throws IOException {
            serializer.text(DATE_FORMAT.format(mDate));
        }

        @Override
        public boolean setFromXmlText(String text) {
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

        private FieldDate(JSONObject json) {
            this(json.optString("name", "NAME"), json.optString("date"));
        }

        /**
         * @return The date in this field.
         */
        public Date getDate() {
            return mDate;
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
            mDate.setTime(millis);
        }

        /**
         * Sets this field to the specified {@link Date}.
         */
        public void setDate(Date date) {
            if (date == null) {
                throw new IllegalArgumentException("Date may not be null.");
            }
            mDate.setTime(date.getTime());
        }
    }

    /**
     * Adds a variable to an Input.
     */
    public static final class FieldVariable extends Field {
        private String mVariable;

        public FieldVariable(String name, String variable) {
            super(name, TYPE_VARIABLE);
            mVariable = variable;
        }

        @Override
        public FieldVariable clone() throws CloneNotSupportedException {
            return (FieldVariable) super.clone();
        }

        @Override
        protected void serializeInner(XmlSerializer serializer) throws IOException {
            serializer.text(mVariable);
        }

        @Override
        public boolean setFromXmlText(String text) {
            if (TextUtils.isEmpty(text)) {
                return false;
            }
            setVariable(text);
            return true;
        }

        private FieldVariable(JSONObject json) {
            this(json.optString("name", "NAME"), json.optString("variable", "item"));
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
        public void setVariable(String variable) { mVariable = variable; }
    }

    /**
     * Adds a dropdown list to an Input.
     */
    public static final class FieldDropdown extends Field {
        // TODO: consider other data structures
        private ArrayList<Pair<String, String>> mOptions = new ArrayList<>();
        private int mCurrentSelection = 0;

        public FieldDropdown(String name, String[] displayNames, String[] values) {
            super(name, TYPE_DROPDOWN);
            setOptions(displayNames, values);
        }

        public FieldDropdown(String name, List<Pair<String, String>> options) {
            super(name, TYPE_DROPDOWN);
            setOptions(options);
        }

        @Override
        public FieldDropdown clone() throws CloneNotSupportedException {
            FieldDropdown field = (FieldDropdown) super.clone();

            field.mOptions = new ArrayList<>(mOptions.size());
            for (int i = 0; i < mOptions.size(); i++) {
                Pair<String, String> original = mOptions.get(i);
                field.mOptions.add(Pair.create(original.first, original.second));
            }

            return field;
        }

        @Override
        protected void serializeInner(XmlSerializer serializer) throws IOException {
            serializer.text(getSelectedValue());
        }

        @Override
        public boolean setFromXmlText(String text) {
            setSelectedValue(text);
            return true;
        }

        private FieldDropdown(JSONObject json) {
            this(json.optString("name", "NAME"), null, null);
            JSONArray jsonOptions = json.optJSONArray("options");
            if (jsonOptions != null) {
                ArrayList<Pair<String, String>> options = new ArrayList<>();
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
                            options.add(Pair.create(displayName, value));
                        } catch (JSONException e) {
                            throw new RuntimeException("Error reading option values.", e);
                        }
                    }
                }
                setOptions(options);
            }
        }

        /**
         * @return The list of options available in this dropdown.
         */
        public List<Pair<String, String>> getOptions() {
            return mOptions;
        }

        /**
         * @return The value of the currently selected option.
         */
        public String getSelectedValue() {
            return mOptions.size() == 0 ? null : mOptions.get(mCurrentSelection).second;
        }

        /**
         * @return The display name of the currently selected option.
         */
        public String getSelectedDisplayName() {
            return mOptions.size() == 0 ? null : mOptions.get(mCurrentSelection).first;
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
            mCurrentSelection = index;
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
                    mOptions.add(Pair.create(displayNames[i], values[i]));
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
                mCurrentSelection = 0;
            } else {
                boolean found = false;
                for (int i = 0; i < mOptions.size(); i++) {
                    if (TextUtils.equals(value, mOptions.get(i).second)) {
                        mCurrentSelection = i;
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    mCurrentSelection = 0;
                }
            }
        }

        /**
         * Sets the list of options. Each Pair in the list must have a display name as the first
         * parameter and the value as the second parameter.
         *
         * @param options A list of options consisting of pairs of displayName/value.
         */
        public void setOptions(List<Pair<String, String>> options) {
            String previousValue = getSelectedValue();
            mOptions.clear();
            if (options != null) {
                mOptions.addAll(options);
            }
            setSelectedValue(previousValue);
        }
    }

    /**
     * Adds an image to an Input.
     */
    public static final class FieldImage extends Field {
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

        @Override
        public FieldImage clone() throws CloneNotSupportedException {
            return (FieldImage) super.clone();
        }

        private FieldImage(JSONObject json) {
            this(json.optString("name"),
                    json.optString("src", "https://www.gstatic.com/codesite/ph/images/star_on.gif"),
                    json.optInt("width", 15), json.optInt("height", 15),
                    json.optString("alt", "*"));
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
         * Sets a new image to be shown.
         *
         * @param src A web address or Blockly reference to the image.
         * @param width The display width of the image in dips.
         * @param height The display height of the image in dips.
         */
        public void setImage(String src, int width, int height) {
            mSrc = src;
            mWidth = width;
            mHeight = height;
        }

        /**
         * Sets the alt-text for the image.
         */
        public void setAltText(String altText) {
            mAltText = altText;
        }
    }
}
