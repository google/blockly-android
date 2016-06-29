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
import android.util.Log;

import com.google.blockly.utils.BlockLoadingException;

import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Arrays;

/**
 * A 'field_number' type of field, for an editable number.
 */
public final class FieldNumber extends Field<FieldNumber.Observer> {
    private static final String TAG = "FieldNumber";

    public static final double NO_CONSTRAINT = Double.NaN;

    /**
     * This formatter is used by fields without precision, and to count precision's significant
     * digits past the decimal point.  Unlike {@link Double#toString}, it displays as many
     * fractional digits as possible.
     */
    private static final DecimalFormat NAIVE_DECIMAL_FORMAT;
    static {
        char[] sigDigts = new char[100];
        Arrays.fill(sigDigts, '#');
        NAIVE_DECIMAL_FORMAT
                = new DecimalFormat(new StringBuffer("0.").append(sigDigts).toString());
    }

    /**
     * This formatter is used when precision is a multiple of 1.
     */
    private static final DecimalFormat INTEGER_DECIMAL_FORMAT = new DecimalFormat("0");

    private double mValue;
    private double mMin = NO_CONSTRAINT;
    private double mMax = NO_CONSTRAINT;
    private double mPrecision = NO_CONSTRAINT;

    private DecimalFormat mFormatter;
    private boolean mIntegerPrecision;
    private double mEffectiveMin = -Double.MAX_VALUE;  // mMin as a multiple of mPrecision
    private double mEffectiveMax = Double.MAX_VALUE;  // mMax as a multiple of mPrecision

    public FieldNumber(String name) {
        super(name, TYPE_NUMBER);
    }

    public static FieldNumber fromJson(JSONObject json) throws BlockLoadingException {
        String name = json.optString("name", null);
        if (name == null) {
            throw new BlockLoadingException("Number fields must have name field.");
        }

        FieldNumber field = new FieldNumber(name);

        if (json.has("value")) {
            try {
                field.setValue(json.getDouble("value"));
            } catch (JSONException e) {
                throw new BlockLoadingException("Cannot parse field_number value: "
                        + json.optString("value", "[object or array]"));
            }
        }
        try {
            field.setConstraints(
                    json.optDouble("min", NO_CONSTRAINT),
                    json.optDouble("max", NO_CONSTRAINT),
                    json.optDouble("precision", NO_CONSTRAINT));
        } catch (IllegalArgumentException e) {
            throw new BlockLoadingException(e);
        }
        return field;
    }

    @Override
    public FieldNumber clone() {
        FieldNumber copy = new FieldNumber(getName());
        copy.setValue(mValue);
        copy.setConstraints(mMin, mMax, mPrecision);
        return copy;
    }

    public void setConstraints(double min, double max, double precision) {
        if (max == Double.POSITIVE_INFINITY || Double.isNaN(max)) {
            max = NO_CONSTRAINT;
        } else if (max == Double.NEGATIVE_INFINITY) {
            throw new IllegalArgumentException("Max cannot be -Inf. No valid values would exist.");
        }
        if (min == Double.NEGATIVE_INFINITY || Double.isNaN(min)) {
            min = NO_CONSTRAINT;
        } else if (min == Double.POSITIVE_INFINITY) {
            throw new IllegalArgumentException("Min cannot be Inf. No valid values would exist.");
        }
        if (precision == 0 || Double.isNaN(precision)) {
            precision = NO_CONSTRAINT;
        }
        if (Double.isInfinite(precision)) {
            throw new IllegalArgumentException("Precision cannot be infinite.");
        }
        if (!Double.isNaN(min) && !Double.isNaN(max) && min > max) {
            throw new IllegalArgumentException("Minimum value must be less than max. Found "
                    + min + " > " + max);
        }
        if (!Double.isNaN(precision) && precision <= 0) {
            throw new IllegalArgumentException("Precision must be positive. Found " + precision);
        }

        double effectiveMin = Double.isNaN(min) ? -Double.MAX_VALUE : min;
        double  effectiveMax = Double.isNaN(max) ? Double.MAX_VALUE : max;
        if (!Double.isNaN(precision)) {
            if (effectiveMin < 0) {
                double multiplier = Math.floor(-effectiveMin / precision);
                effectiveMin = precision * -multiplier;
            } else {
                double multiplier = Math.ceil(effectiveMin / precision);
                effectiveMin = precision * multiplier;
            }
            if (effectiveMax < 0) {
                double multiplier = Math.ceil(-effectiveMax / precision);
                effectiveMax = precision * -multiplier;
            } else {
                double multiplier = Math.floor(effectiveMax / precision);
                effectiveMax = precision * multiplier;

            }
            if (effectiveMin > effectiveMax) {
                throw new IllegalArgumentException("No valid value in range.");
            }
        }

        mMin = min;
        mMax = max;
        mPrecision = precision;
        mEffectiveMin = effectiveMin;
        mEffectiveMax = effectiveMax;
        mIntegerPrecision = (precision == Math.round(precision));
        if (!hasPrecision()) {
            mFormatter = NAIVE_DECIMAL_FORMAT;
        } else if (mIntegerPrecision) {
            mFormatter = INTEGER_DECIMAL_FORMAT;
        } else {
            String precisionStr = NAIVE_DECIMAL_FORMAT.format(precision);
            int decimalChar = precisionStr.indexOf('.');
            if (decimalChar == -1) {
                mFormatter = INTEGER_DECIMAL_FORMAT;
            } else {
                int significantDigits = precisionStr.length() - decimalChar;
                StringBuilder sb = new StringBuilder("0.");
                char[] sigDigitsFormat = new char[significantDigits];
                Arrays.fill(sigDigitsFormat, '#');
                sb.append(sigDigitsFormat);
                mFormatter = new DecimalFormat(sb.toString());
            }
        }

        setValueImpl(mValue, true);
    }

    /**
     * Sets the value from a string.  As long as the text can be parsed as a number, the value will
     * be accepted.  The actual value assigned might be modified to fit the min, max, and precision
     * constraints.
     *
     * @param text The text value for this field.
     *
     * @return
     */
    @Override
    public boolean setFromString(String text) {
        if (TextUtils.isEmpty(text)) {
            Log.e(TAG, "text was empty" + (text == null ? "(null)" : ""));
            return false;
        }
        try {
            double value = Double.parseDouble(text);
            if (Double.isNaN(value)) {
                Log.e(TAG, "Value cannot be NaN");
                return false;
            }
            setValue(value);
            return true;
        } catch (NumberFormatException e) {
            Log.e(TAG, "Not a number: \"" + text + "\"");
            return false;
        }
    }

    /**
     * @return The number the user has entered.
     */
    public double getValue() {
        return mValue;
    }

    /**
     * @return The formatted (human readable) string version of the input.
     */
    public String getValueString() {
        return mFormatter.format(mValue);
    }

    /**
     * Sets the number in this Field.  The resulting value of this field may differ from
     * {@code newValue} to adapt to the assigned min, max, and precision constraints.
     *
     * @param newValue The number to replace the field content with.
     */
    public void setValue(double newValue) {
        setValueImpl(newValue, false);
    }

    private void setValueImpl(double newValue, boolean onConstraintsChanged) {
        if (hasPrecision()) {
            newValue = mPrecision * Math.round(newValue / mPrecision);
            // Run the value through formatter to limit significant digits.
            String formattedValue = mFormatter.format(newValue);
            newValue = Double.parseDouble(formattedValue);
        }
        if (hasMinimum() && newValue < mEffectiveMin) {
            newValue = mEffectiveMin;
        } else if (hasMaximum() && newValue > mEffectiveMax) {
            newValue = mEffectiveMax;
        }
        if (newValue != mValue) {
            double oldValue = mValue;
            mValue = newValue;
            if (onConstraintsChanged) {
                // Notify constraints change before notifying value change.
                onConstraintChanged();
            }
            onValueChanged(oldValue, newValue);
        } else if (onConstraintsChanged) {
            onConstraintChanged();
        }
    }

    @Override
    public String getSerializedValue() {
        return Double.toString(mValue);
    }

    private void onValueChanged(double oldValue, double newValue) {
        for (int i = 0; i < mObservers.size(); i++) {
            mObservers.get(i).onValueChanged(this, oldValue, newValue);
        }
    }

    private void onConstraintChanged() {
        for (int i = 0; i < mObservers.size(); i++) {
            mObservers.get(i).onConstraintsChanged(this);
        }
    }

    /**
     * @return True if there's a minimum constraint, false if the minimum is unbounded.
     */
    public boolean hasMinimum() {
        return !Double.isNaN(mMin);
    }

    /** @return The minimum allowed value for this field. */
    public double getMinimumValue() {
        return mMin;
    }

    /**
     * @return True if there's a maximum constraint, false if the maximum is unbounded.
     */
    public boolean hasMaximum() {
        return !Double.isNaN(mMax);
    }

    /** @return The maximum allowed value for this field. */
    public double getMaximumValue() {
        return mMax;
    }

    /**
     * @return True if there's a precision applied to the value, false otherwise.
     */
    public boolean hasPrecision() {
        return !Double.isNaN(mPrecision);
    }

    /**
     * This returns the precision of the value allowed by this field.  The value must be a multiple
     * of precision.  Precision is usually expressed as a power of 10 (e.g., 1, 100, 0.01), though
     * other useful examples might be 5, 20, or 25.
     *
     * @return The precision allowed for the value.
     */
    public double getPrecision() {
        return mPrecision;
    }

    /** @return Whether the precision (and thus the value) is an integer. */
    public boolean isInteger() {
        return mIntegerPrecision;
    }

    /**
     * Observer for listening to changes to a {@link FieldNumber}.
     */
    public interface Observer {
        /**
         * Called when the field's value changed.
         *
         * @param field The field that changed.
         * @param oldValue The field's previous value.
         * @param newValue The field's new value.
         */
        void onValueChanged(FieldNumber field, double oldValue, double newValue);

        /**
         * Called when the field's constraints changed.
         *
         * @param field The field that changed.
         */
        void onConstraintsChanged(FieldNumber field);
    }
}
