package com.google.blockly.blocks;

import android.graphics.Color;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The base class for Fields in Blockly. A field is the smallest piece of a {@link Block} and is
 * wrapped by an {@link Input}.
 */
public abstract class Field {
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
     * The list of known FIELD_TYPES. If this list changes {@link #fromJSON(JSONObject)} should
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

    public Field(String name, String type) {
        if (TextUtils.isEmpty(type)) {
            throw new IllegalArgumentException("type may not be empty");
        }
        mName = name;
        mType = type;
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
    public String getType() {
        return mType;
    }

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
    public static Field fromJSON(JSONObject json) {
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

    public static final class FieldLabel extends Field {
        private final String mText;

        public FieldLabel(String name, String text) {
            super(name, TYPE_LABEL);
            mText = text;
        }

        private FieldLabel(JSONObject json) {
            this(json.optString("name", null), json.optString("text", ""));
        }

        public String getText() {
            return mText;
        }
    }

    public static final class FieldInput extends Field {
        private String mText;

        public FieldInput(String name, String text) {
            super(name, TYPE_INPUT);
            mText = text;
        }

        private FieldInput(JSONObject json) {
            super(json.optString("name", "NAME"), json.optString("text", "default"));
            // TODO: consider replacing default text with string resource
        }

        public String getText() {
            return mText;
        }

        public void setText(String text) {
            mText = text;
        }
    }

    public static final class FieldAngle extends Field {
        private int mAngle;

        public FieldAngle(String name, int angle) {
            super(name, TYPE_ANGLE);
            mAngle = angle;
        }

        private FieldAngle(JSONObject json) {
            this(json.optString("name", "NAME"), json.optInt("angle", 90));
        }

        public int getAngle() {
            return mAngle;
        }

        public void setAngle(int angle) {
            mAngle = angle % 360;
        }
    }

    public static final class FieldCheckbox extends Field {
        private boolean mChecked;

        public FieldCheckbox(String name, boolean checked) {
            super(name, TYPE_CHECKBOX);
            mChecked = checked;
        }

        private FieldCheckbox(JSONObject json) {
            this(json.optString("name", "NAME"), json.optBoolean("checked", true));
        }

        public boolean isChecked() {
            return mChecked;
        }

        public void setChecked(boolean checked) {
            mChecked = checked;
        }
    }

    public static final class FieldColour extends Field {
        private int mColour;

        public FieldColour(String name, int colour) {
            super(name, TYPE_COLOUR);
            mColour = colour;
        }

        private FieldColour(JSONObject json) {
            this(json.optString("name", "NAME"), 0xff0000);
            String colourString = json.optString("colour");
            if (!TextUtils.isEmpty(colourString)) {
                mColour = Color.parseColor(colourString);
            }
        }

        public int getColour() {
            return mColour;
        }

        public void setColour(int colour) {
            mColour = colour;
        }
    }

    public static final class FieldDate extends Field {
        private final Date mDate;

        public FieldDate(String name, String dateString) {
            super(name, TYPE_DATE);
            Date date = null;
            if (!TextUtils.isEmpty(dateString)) {
                try {
                    date = DateFormat.getDateInstance().parse(dateString);
                } catch (ParseException e) {
                    Log.e(TAG, "Unable to parse date " + dateString, e);
                }
            }
            if (date == null) {
                date = new Date();
            }
            mDate = date;

        }

        private FieldDate(JSONObject json) {
            this(json.optString("name", "NAME"), json.optString("date"));
        }

        public Date getDate() {
            return mDate;
        }

        public void setTime(long millis) {
            mDate.setTime(millis);
        }
    }

    public static final class FieldVariable extends Field {
        private String mVariable;

        public FieldVariable(String name, String variable) {
            super(name, TYPE_VARIABLE);
            mVariable = variable;
        }

        private FieldVariable(JSONObject json) {
            this(json.optString("name", "NAME"), json.optString("variable", "item"));
        }

        public String getVariable() {
            return mVariable;
        }

        public void setVariable(String variable) {
            mVariable = variable;
        }
    }

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

        public List<Pair<String, String>> getOptions() {
            return mOptions;
        }

        public String getSelectedValue() {
            return mOptions.size() == 0 ? null : mOptions.get(mCurrentSelection).second;
        }

        public String getSelectedDisplayName() {
            return mOptions.size() == 0 ? null : mOptions.get(mCurrentSelection).first;
        }

        public int getSelectedIndex() {
            return mCurrentSelection;
        }

        public void setSelectedIndex(int index) {
            if (index < 0 || index >= mOptions.size()) {
                throw new IllegalArgumentException(
                        "Index must be between 0 and the number of options - 1");
            }
            mCurrentSelection = index;
        }

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

        public void setOptions(List<Pair<String, String>> options) {
            String previousValue = getSelectedValue();
            mOptions.clear();
            if (options != null) {
                mOptions.addAll(options);
            }
            setSelectedValue(previousValue);
        }
    }

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

        private FieldImage(JSONObject json) {
            this(json.optString("name"),
                    json.optString("src", "https://www.gstatic.com/codesite/ph/images/star_on.gif"),
                    json.optInt("width", 15), json.optInt("height", 15),
                    json.optString("alt", "*"));
        }

        public String getSource() {
            return mSrc;
        }

        public int getWidth() {
            return mWidth;
        }

        public int getHeight() {
            return mHeight;
        }

        public String getAltText() {
            return mAltText;
        }

        public void setImage(String src, int width, int height) {
            mSrc = src;
            mWidth = width;
            mHeight = height;
        }

        public void setAltText(String altText) {
            mAltText = altText;
        }
    }
}
