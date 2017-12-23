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

import com.google.blockly.model.BlocklyEvent.ChangeEvent;
import com.google.blockly.utils.BlockLoadingException;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Locale;

/**
 * A 'field_number' type of field, for an editable number.
 */
public final class FieldNumber extends Field {
    private static final String TAG = "FieldNumber";

    public static final double NO_CONSTRAINT = Double.NaN;

    private static final DecimalFormatSymbols PERIOD_AS_DECIMAL =
            new DecimalFormatSymbols(new Locale("en", "us"));
    /**
     * This formatter is used by fields without precision or grouping punctuation. It is used to
     * count precision's significant digits past the decimal point.  Unlike {@link Double#toString},
     * it displays as many fractional digits as possible.
     */
    private static final DecimalFormat NAIVE_DECIMAL_FORMAT;
    static {
        // Force as many significant digits as possible in a naive decimal format, using the period
        // as the decimal.
        char[] sigDigts = new char[324];  // Double.MIN_VALUE approx. 4.9E-324
        Arrays.fill(sigDigts, '#');
        NAIVE_DECIMAL_FORMAT
                = new DecimalFormat(new StringBuffer("0.").append(sigDigts).toString(),
                        PERIOD_AS_DECIMAL);
    }

    /**
     * This formatter is used when precision is a multiple of 1.
     */
    protected static final DecimalFormat INTEGER_DECIMAL_FORMAT = new DecimalFormat("0");

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

    public FieldNumber(String name, double min, double max, double precision) {
        this(name);
        setConstraints(min, max, precision);
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

    /**
     * Sets the constraints on valid number values.
     * <p/>
     * Changing the constraints may trigger a {@link ChangeEvent}, even if the value does not
     * change.
     *
     * @param min The minimum allowed value, inclusive.
     * @param max The maximum allowed value, inclusive.
     * @param precision The precision of allowed values. Valid values are multiples of this number,
     *                  such as 1, 0.1, 100, or 0.125.
     */
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
                mFormatter = new DecimalFormat(sb.toString(), PERIOD_AS_DECIMAL);
            }
        }

        setValueImpl(mValue, true);
    }

    /**
     * Retrieves (possibly constructing) a NumberFormat configured for both the field constraints
     * and the provided Locale.
     * @param locale The locale to construct a number formatter for.
     * @return A NumberFormat configured for both the field constraints and the Locale.
     */
    public NumberFormat getNumberFormatForLocale(Locale locale) {
        if (!hasPrecision()) {
            NumberFormat localizedNumFormat = NumberFormat.getInstance(locale);
            localizedNumFormat.setMaximumFractionDigits(324);  // Double.MIN_VALUE approx. 4.9E-324
            return localizedNumFormat;
        }
        if (mIntegerPrecision) {
            return NumberFormat.getIntegerInstance(locale);
        }

        String precisionStr = NAIVE_DECIMAL_FORMAT.format(mPrecision);
        int decimalChar = precisionStr.indexOf('.');
        if (decimalChar == -1) {
            return NumberFormat.getIntegerInstance(locale);
        }
        int significantDigits = precisionStr.length() - decimalChar;

        NumberFormat localizedNumFormat = NumberFormat.getInstance(locale);
        localizedNumFormat.setMaximumFractionDigits(significantDigits);
        return localizedNumFormat;
    }

    /**
     * Sets the value from a string.  As long as the text can be parsed as a number, the value will
     * be accepted.  The actual value assigned might be modified to fit the min, max, and precision
     * constraints.
     *
     * @param text The text value for this field.
     *
     * @return True if the value parsed without error and the value has been updated.
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
        if (newValue != mValue || onConstraintsChanged) {
            String oldStrValue = getSerializedValue();
            mValue = newValue;
            String newStrValue = getSerializedValue();

            fireValueChanged(oldStrValue, newStrValue);
        }
    }

    @Override
    public String getSerializedValue() {
        if (mValue % 1.0 == 0.0) {
            // Don't render the decimal point.
            return INTEGER_DECIMAL_FORMAT.format(mValue);
        } else {
            // Render as many decimal places as necessary. Don't abbreviate.
            return NAIVE_DECIMAL_FORMAT.format(mValue);
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
}
