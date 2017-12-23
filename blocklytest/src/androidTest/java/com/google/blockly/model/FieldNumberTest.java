/*
 *  Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.blockly.model;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.text.NumberFormat;
import java.util.Locale;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Tests for {@link FieldNumber}.
 */
public class FieldNumberTest {
    private static final String FIELD_NAME = "FIELD_NAME";
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    private FieldNumber mField;

    @Before
    public void setUp() throws Exception {
        mField = new FieldNumber(FIELD_NAME);
    }

    @Test
    public void testConstructor() {
        assertThat(mField.getName()).isEqualTo(FIELD_NAME);
        assertThat(mField.getType()).isEqualTo(Field.TYPE_NUMBER);
    }

    @Test
    public void testConstructorWithAllConstraints() {
        FieldNumber field = new FieldNumber(FIELD_NAME, -1.0, 1.0, 0.1);

        assertThat(field.getName()).isEqualTo(FIELD_NAME);
        assertThat(field.getMinimumValue()).isEqualTo(-1.0);
        assertThat(field.getMaximumValue()).isEqualTo(1.0);
        assertThat(field.getPrecision()).isEqualTo(0.1);

        assertThat(field.hasMinimum()).isTrue();
        assertThat(field.hasMaximum()).isTrue();
        assertThat(field.hasPrecision()).isTrue();
    }

    @Test
    public void testConstructorWithPartialConstraints() {
        FieldNumber field = new FieldNumber(FIELD_NAME, FieldNumber.NO_CONSTRAINT,
                FieldNumber.NO_CONSTRAINT, 0.1);

        assertThat(field.getName()).isEqualTo(FIELD_NAME);
        assertThat(field.getMinimumValue()).isNaN();
        assertThat(field.getMaximumValue()).isNaN();
        assertThat(field.getPrecision()).isEqualTo(0.1);

        assertThat(field.hasMinimum()).isFalse();
        assertThat(field.hasMaximum()).isFalse();
        assertThat(field.hasPrecision()).isTrue();
    }

    @Test
    public void testConstraintDefaults() {
        // Before assignment
        assertThat(mField.getMinimumValue()).isNaN();
        assertThat(mField.getMaximumValue()).isNaN();
        assertThat(mField.getPrecision()).isNaN();

        assertThat(mField.hasMaximum()).isFalse();
        assertThat(mField.hasMinimum()).isFalse();
        assertThat(mField.hasPrecision()).isFalse();
    }

    @Test
    public void testConstrainedRangeSuccess() {
        final double MIN = -100.0;
        final double MAX = 100.0;
        final double PRECISION = 0.1;

        mField.setConstraints(MIN, MAX, PRECISION);
        assertThat(mField.getMinimumValue()).isEqualTo(MIN);
        assertThat(mField.getMaximumValue()).isEqualTo(MAX);
        assertThat(mField.getPrecision()).isEqualTo(PRECISION);
        assertThat(mField.hasPrecision()).isTrue();

        // Normal assignments
        mField.setValue(3.0);
        assertThat(mField.getValue()).isEqualTo(3.0);
        mField.setValue(0.2);
        assertThat(mField.getValue()).isEqualTo(0.2);
        mField.setValue(0.09);
        assertWithMessage("Rounded 0.09 to precision.").that(mField.getValue()).isEqualTo(0.1);
        mField.setValue(0.0);
        assertThat(mField.getValue()).isEqualTo(0.0);
        mField.setValue(-0.1);
        assertThat(mField.getValue()).isEqualTo(-0.1);
        mField.setValue(-0.1);
        assertThat(mField.getValue()).isEqualTo(-0.1);
        mField.setValue(-0.29);
        assertThat(mField.getValue()).isEqualTo(-0.3);

        mField.setValue(MIN + PRECISION);
        assertThat(MIN < mField.getValue()).isTrue();
        mField.setValue(MAX - PRECISION);
        assertThat(mField.getValue() < MAX).isTrue();

        mField.setValue(MIN);
        assertThat(mField.getValue()).isEqualTo(MIN);
        mField.setValue(MAX);
        assertThat(mField.getValue()).isEqualTo(MAX);

        mField.setValue(MIN - PRECISION);
        assertThat(mField.getValue()).isEqualTo(MIN);
        mField.setValue(MAX + PRECISION);
        assertThat(mField.getValue()).isEqualTo(MAX);

        mField.setValue(MIN - 1e100);
        assertThat(mField.getValue()).isEqualTo(MIN);
        mField.setValue(MAX + 1e100);
        assertThat(mField.getValue()).isEqualTo(MAX);
    }

    @Test
    public void testSetConstraints_SpecialValues() {
        mField.setConstraints(Double.NaN, 1.0, 0.1);
        assertThat(mField.getMinimumValue()).isNaN();
        assertThat(mField.hasMinimum()).isFalse();
        assertThat(mField.hasMaximum()).isTrue();

        mField.setConstraints(Double.NEGATIVE_INFINITY, 1.0, 0.1);
        assertThat(mField.getMinimumValue()).isNaN();
        assertThat(mField.hasMinimum()).isFalse();
        assertThat(mField.hasMaximum()).isTrue();

        mField.setConstraints(-1.0, Double.NaN, 0.1);
        assertThat(mField.getMaximumValue()).isNaN();
        assertThat(mField.hasMinimum()).isTrue();
        assertThat(mField.hasMaximum()).isFalse();

        mField.setConstraints(-1.0, Double.POSITIVE_INFINITY, 0.1);
        assertThat(mField.getMaximumValue()).isNaN();
        assertThat(mField.hasMinimum()).isTrue();
        assertThat(mField.hasMaximum()).isFalse();

        mField.setConstraints(-1.0, 1.0, Double.NaN);
        assertThat(mField.getPrecision()).isNaN();
        assertThat(mField.hasMinimum()).isTrue();
        assertThat(mField.hasMaximum()).isTrue();
        assertThat(mField.hasPrecision()).isFalse();
    }

    @Test
    public void testPositiveInfinityMinException() {
        thrown.expect(IllegalArgumentException.class);
        mField.setConstraints(Double.POSITIVE_INFINITY, 1.0, 0.1);
    }

    @Test
    public void testNegativeInfinityMaxException() {
        thrown.expect(IllegalArgumentException.class);
        mField.setConstraints(-1.0, Double.NEGATIVE_INFINITY, 0.1);
    }

    @Test
    public void testPositiveInfinityPrecisionException() {
        thrown.expect(IllegalArgumentException.class);
        mField.setConstraints(-1.0, 1.0, Double.POSITIVE_INFINITY);
    }

    @Test
    public void testNegativeInfinityPrecisionException() {
        thrown.expect(IllegalArgumentException.class);
        mField.setConstraints(-1.0, 1.0, Double.NEGATIVE_INFINITY);
    }

    @Test
    public void testSetConstraintsInvalidPrecision() {
        thrown.expect(IllegalArgumentException.class);
        mField.setConstraints(1.0, -1.0, FieldNumber.NO_CONSTRAINT);
    }

    @Test
    public void testSetConstraintsInvalidPairs() {
        thrown.expect(IllegalArgumentException.class);
        mField.setConstraints(1.0, 4.0, 5.0);
    }

    @Test
    public void testDecimalPrecisionLessThanOne() {
        final double MIN = FieldNumber.NO_CONSTRAINT;
        final double MAX = FieldNumber.NO_CONSTRAINT;
        final double PRECISION = 0.25;  // Two significant digits
        mField.setConstraints(MIN, MAX, PRECISION);
        NumberFormat periodDecimal = mField.getNumberFormatForLocale(new Locale("en", "us"));
        NumberFormat commaDecimal = mField.getNumberFormatForLocale(new Locale("es", "es"));

        assertThat(mField.isInteger()).isFalse();

        // Exact values
        mField.setValue(0.0);
        assertThat(mField.getValue()).isEqualTo(0.0);
        assertThat(periodDecimal.format(mField.getValue())).isEqualTo("0");
        assertThat(commaDecimal.format(mField.getValue())).isEqualTo("0");

        mField.setValue(0.25);
        assertThat(mField.getValue()).isEqualTo(0.25);
        assertThat(periodDecimal.format(mField.getValue())).isEqualTo("0.25");
        assertThat(commaDecimal.format(mField.getValue())).isEqualTo("0,25");

        mField.setValue(1.0);
        assertThat(mField.getValue()).isEqualTo(1.0);
        assertThat(periodDecimal.format(mField.getValue())).isEqualTo("1");
        assertThat(commaDecimal.format(mField.getValue())).isEqualTo("1");

        mField.setValue(1.25);
        assertThat(mField.getValue()).isEqualTo(1.25);
        assertThat(periodDecimal.format(mField.getValue())).isEqualTo("1.25");
        assertThat(commaDecimal.format(mField.getValue())).isEqualTo("1,25");

        mField.setValue(2.50);
        assertThat(mField.getValue()).isEqualTo(2.5);
        assertThat(periodDecimal.format(mField.getValue())).isEqualTo("2.5");
        assertThat(commaDecimal.format(mField.getValue())).isEqualTo("2,5");

        mField.setValue(25);
        assertThat(mField.getValue()).isEqualTo(25.0);
        assertThat(periodDecimal.format(mField.getValue())).isEqualTo("25");
        assertThat(commaDecimal.format(mField.getValue())).isEqualTo("25");

        mField.setValue(-0.25);
        assertThat(mField.getValue()).isEqualTo(-0.25);
        assertThat(periodDecimal.format(mField.getValue())).isEqualTo("-0.25");
        assertThat(commaDecimal.format(mField.getValue())).isEqualTo("-0,25");

        mField.setValue(-1.0);
        assertThat(mField.getValue()).isEqualTo(-1.0);
        assertThat(periodDecimal.format(mField.getValue())).isEqualTo("-1");
        assertThat(commaDecimal.format(mField.getValue())).isEqualTo("-1");

        mField.setValue(-1.25);
        assertThat(mField.getValue()).isEqualTo(-1.25);
        assertThat(periodDecimal.format(mField.getValue())).isEqualTo("-1.25");
        assertThat(commaDecimal.format(mField.getValue())).isEqualTo("-1,25");

        mField.setValue(-2.50);
        assertThat(mField.getValue()).isEqualTo(-2.5);
        assertThat(periodDecimal.format(mField.getValue())).isEqualTo("-2.5");
        assertThat(commaDecimal.format(mField.getValue())).isEqualTo("-2,5");

        mField.setValue(-25);
        assertThat(mField.getValue()).isEqualTo(-25.0);
        assertThat(periodDecimal.format(mField.getValue())).isEqualTo("-25");
        assertThat(commaDecimal.format(mField.getValue())).isEqualTo("-25");

        // Rounded Values
        mField.setValue(0.2);
        assertThat(mField.getValue()).isEqualTo(0.25);

        mField.setValue(0.9);
        assertThat(mField.getValue()).isEqualTo(1.0);

        mField.setValue(1.1);
        assertThat(mField.getValue()).isEqualTo(1.0);

        mField.setValue(1.2);
        assertThat(mField.getValue()).isEqualTo(1.25);

        mField.setValue(1.3);
        assertThat(mField.getValue()).isEqualTo(1.25);
    }

    @Test
    public void testIntegerPrecisionOne() {
        final double MIN = FieldNumber.NO_CONSTRAINT;
        final double MAX = FieldNumber.NO_CONSTRAINT;
        final double PRECISION = 1;
        mField.setConstraints(MIN, MAX, PRECISION);
        NumberFormat commaMarker = mField.getNumberFormatForLocale(new Locale("en", "us"));
        NumberFormat periodMarker = mField.getNumberFormatForLocale(new Locale("es", "es"));

        assertThat(mField.isInteger()).isTrue();

        // Exact values
        mField.setValue(0.0);

        // Exact values
        mField.setValue(0.0);
        assertThat(mField.getValue()).isEqualTo(0.0);
        assertThat(commaMarker.format(mField.getValue())).isEqualTo("0");
        assertThat(periodMarker.format(mField.getValue())).isEqualTo("0");

        mField.setValue(1.0);
        assertThat(mField.getValue()).isEqualTo(1.0);
        assertThat(commaMarker.format(mField.getValue())).isEqualTo("1");
        assertThat(periodMarker.format(mField.getValue())).isEqualTo("1");

        mField.setValue(2.0);
        assertThat(mField.getValue()).isEqualTo(2.0);
        assertThat(commaMarker.format(mField.getValue())).isEqualTo("2");
        assertThat(periodMarker.format(mField.getValue())).isEqualTo("2");

        mField.setValue(7.0);
        assertThat(mField.getValue()).isEqualTo(7.0);
        assertThat(commaMarker.format(mField.getValue())).isEqualTo("7");
        assertThat(periodMarker.format(mField.getValue())).isEqualTo("7");

        mField.setValue(10.0);
        assertThat(mField.getValue()).isEqualTo(10.0);
        assertThat(commaMarker.format(mField.getValue())).isEqualTo("10");
        assertThat(periodMarker.format(mField.getValue())).isEqualTo("10");

        mField.setValue(100.0);
        assertThat(mField.getValue()).isEqualTo(100.0);
        assertThat(commaMarker.format(mField.getValue())).isEqualTo("100");
        assertThat(periodMarker.format(mField.getValue())).isEqualTo("100");

        // Large numbers render with grouping markers
        mField.setValue(1000000.0);
        assertThat(mField.getValue()).isEqualTo(1000000.0);
        assertThat(commaMarker.format(mField.getValue())).isEqualTo("1,000,000");
        assertThat(periodMarker.format(mField.getValue())).isEqualTo("1.000.000");

        mField.setValue(-1.0);
        assertThat(mField.getValue()).isEqualTo(-1.0);
        assertThat(commaMarker.format(mField.getValue())).isEqualTo("-1");
        assertThat(periodMarker.format(mField.getValue())).isEqualTo("-1");

        mField.setValue(-2.0);
        assertThat(mField.getValue()).isEqualTo(-2.0);
        assertThat(commaMarker.format(mField.getValue())).isEqualTo("-2");
        assertThat(periodMarker.format(mField.getValue())).isEqualTo("-2");

        mField.setValue(-7.0);
        assertThat(mField.getValue()).isEqualTo(-7.0);
        assertThat(commaMarker.format(mField.getValue())).isEqualTo("-7");
        assertThat(periodMarker.format(mField.getValue())).isEqualTo("-7");

        mField.setValue(-10.0);
        assertThat(mField.getValue()).isEqualTo(-10.0);
        assertThat(commaMarker.format(mField.getValue())).isEqualTo("-10");
        assertThat(periodMarker.format(mField.getValue())).isEqualTo("-10");

        mField.setValue(-100.0);
        assertThat(mField.getValue()).isEqualTo(-100.0);
        assertThat(commaMarker.format(mField.getValue())).isEqualTo("-100");
        assertThat(periodMarker.format(mField.getValue())).isEqualTo("-100");

        mField.setValue(-1000000.0);
        assertThat(mField.getValue()).isEqualTo(-1000000.0);
        assertThat(commaMarker.format(mField.getValue())).isEqualTo("-1,000,000");
        assertThat(periodMarker.format(mField.getValue())).isEqualTo("-1.000.000");


        // Rounded Values
        mField.setValue(0.2);
        assertThat(mField.getValue()).isEqualTo(0.0);

        mField.setValue(0.499999);
        assertThat(mField.getValue()).isEqualTo(0.0);

        mField.setValue(0.5);
        assertThat(mField.getValue()).isEqualTo(1.0);

        mField.setValue(1.1);
        assertThat(mField.getValue()).isEqualTo(1.0);

        mField.setValue(99.9999);
        assertThat(mField.getValue()).isEqualTo(100.0);

        mField.setValue(-0.2);
        assertThat(mField.getValue()).isEqualTo(0.0);

        mField.setValue(-0.5);
        assertThat(mField.getValue()).isEqualTo(0.0);

        mField.setValue(-0.501);
        assertThat(mField.getValue()).isEqualTo(-1.0);

        mField.setValue(-1.1);
        assertThat(mField.getValue()).isEqualTo(-1.0);

        mField.setValue(-99.9999);
        assertThat(mField.getValue()).isEqualTo(-100.0);
    }

    @Test
    public void testIntegerPrecisionTwo() {
        final double MIN = FieldNumber.NO_CONSTRAINT;
        final double MAX = FieldNumber.NO_CONSTRAINT;
        final double PRECISION = 2;
        mField.setConstraints(MIN, MAX, PRECISION);
        NumberFormat commaMarker = mField.getNumberFormatForLocale(new Locale("en", "us"));
        NumberFormat periodMarker = mField.getNumberFormatForLocale(new Locale("es", "es"));

        assertThat(mField.isInteger()).isTrue();

        // Exact values
        mField.setValue(0.0);
        assertThat(mField.getValue()).isEqualTo(0.0);
        assertThat(commaMarker.format(mField.getValue())).isEqualTo("0");
        assertThat(periodMarker.format(mField.getValue())).isEqualTo("0");

        mField.setValue(2.0);
        assertThat(mField.getValue()).isEqualTo(2.0);
        assertThat(commaMarker.format(mField.getValue())).isEqualTo("2");
        assertThat(periodMarker.format(mField.getValue())).isEqualTo("2");

        mField.setValue(8.0);
        assertThat(mField.getValue()).isEqualTo(8.0);
        assertThat(commaMarker.format(mField.getValue())).isEqualTo("8");
        assertThat(periodMarker.format(mField.getValue())).isEqualTo("8");

        mField.setValue(10.0);
        assertThat(mField.getValue()).isEqualTo(10.0);
        assertThat(commaMarker.format(mField.getValue())).isEqualTo("10");
        assertThat(periodMarker.format(mField.getValue())).isEqualTo("10");

        mField.setValue(-2.0);
        assertThat(mField.getValue()).isEqualTo(-2.0);
        assertThat(commaMarker.format(mField.getValue())).isEqualTo("-2");
        assertThat(periodMarker.format(mField.getValue())).isEqualTo("-2");

        mField.setValue(-8.0);
        assertThat(mField.getValue()).isEqualTo(-8.0);
        assertThat(commaMarker.format(mField.getValue())).isEqualTo("-8");
        assertThat(periodMarker.format(mField.getValue())).isEqualTo("-8");

        // Rounded Values
        mField.setValue(0.2);
        assertThat(mField.getValue()).isEqualTo(0.0);

        mField.setValue(1.9);
        assertThat(mField.getValue()).isEqualTo(2.0);

        mField.setValue(0.999);
        assertThat(mField.getValue()).isEqualTo(0.0);

        mField.setValue(1.0);
        assertThat(mField.getValue()).isEqualTo(2.0);

        mField.setValue(3.0);
        assertThat(mField.getValue()).isEqualTo(4.0);

        mField.setValue(-0.2);
        assertThat(mField.getValue()).isEqualTo(0.0);

        mField.setValue(-1.9);
        assertThat(mField.getValue()).isEqualTo(-2.0);

        mField.setValue(-1.0);
        assertThat(mField.getValue()).isEqualTo(0.0);

        mField.setValue(-1.001);
        assertThat(mField.getValue()).isEqualTo(-2.0);
    }

    @Test
    public void testSetFromString_ExponentNotation() {
        final double MIN = FieldNumber.NO_CONSTRAINT;
        final double MAX = FieldNumber.NO_CONSTRAINT;
        final double PRECISION = FieldNumber.NO_CONSTRAINT;
        mField.setConstraints(MIN, MAX, PRECISION);

        mField.setFromString("123e4");
        assertThat(mField.getValue()).isEqualTo(1230000d);

        mField.setFromString("1.23e4");
        assertThat(mField.getValue()).isEqualTo(12300d);

        mField.setFromString("-1.23e4");
        assertThat(mField.getValue()).isEqualTo(-12300d);

        mField.setFromString("123e-4");
        assertThat(mField.getValue()).isEqualTo(0.0123);

        mField.setFromString("1.23e-4");
        assertThat(mField.getValue()).isEqualTo(0.000123);
    }

    @Test
    public void testObserverEvent() {
        mField.setValue(789);
        FieldTestHelper.testObserverEvent(mField,
                /* New Value */ "42",
                /* Expected old value */ "789",
                /* Expected new value */ "42");

        // No events if the value doesn't change.
        FieldTestHelper.testObserverNoEvent(mField);
        FieldTestHelper.testObserverNoEvent(mField, "42.0000000");
    }
}