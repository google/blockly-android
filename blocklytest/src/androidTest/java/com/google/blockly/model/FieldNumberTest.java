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

import com.google.blockly.android.MockitoAndroidTestCase;

/**
 * Tests for {@link FieldNumber}.
 */
public class FieldNumberTest extends MockitoAndroidTestCase {
    private static final String FIELD_NAME = "FIELD_NAME";

    private FieldNumber mField;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mField = new FieldNumber(FIELD_NAME);
    }

    public void testConstructor() {
        assertEquals(FIELD_NAME, mField.getName());
        assertEquals(Field.TYPE_NUMBER, mField.getType());
    }

    public void testConstraintDefaults() {
        // Before assignment
        assertEquals(FieldNumber.NO_MINIMUM, mField.getMinimumValue());
        assertEquals(FieldNumber.NO_MAXIMUM, mField.getMaximumValue());
        assertEquals(FieldNumber.NO_PRECISION, mField.getPrecision());

        assertTrue("Default minimum is very very negative",
                FieldNumber.NO_MINIMUM < -1.0e100);
        assertTrue("Default maximum is very very positive",
                FieldNumber.NO_MAXIMUM > 1.0e100);
        assertTrue("Default precision is positive",
                FieldNumber.NO_PRECISION > 0);
        assertTrue("Default precision is very very small",
                FieldNumber.NO_PRECISION < 1.0e-100);
    }

    public void testConstrainedRangeSuccess() {
        final double MIN = -100.0;
        final double MAX = 100.0;
        final double PRECISION = 0.1;

        mField.setConstraints(MIN, MAX, PRECISION);
        assertEquals(MIN, mField.getMinimumValue());
        assertEquals(MAX, mField.getMaximumValue());
        assertEquals(PRECISION, mField.getPrecision());

        // Normal assignments
        mField.setValue(3.0);
        assertEquals(3.0, mField.getValue());
        mField.setValue(0.2);
        assertEquals(0.2, mField.getValue());
        mField.setValue(0.09);
        assertEquals("Rounded 0.09 to precision.", 0.1, mField.getValue());
        mField.setValue(0.0);
        assertEquals(0.0, mField.getValue());
        mField.setValue(-0.1);
        assertEquals(-0.1, mField.getValue());
        mField.setValue(-0.1);
        assertEquals(-0.1, mField.getValue());
        mField.setValue(-0.29);
        assertEquals(-0.3, mField.getValue());

        mField.setValue(MIN + PRECISION);
        assertTrue(MIN < mField.getValue());
        mField.setValue(MAX - PRECISION);
        assertTrue(mField.getValue() < MAX);

        mField.setValue(MIN);
        assertEquals(MIN, mField.getValue());
        mField.setValue(MAX);
        assertEquals(MAX, mField.getValue());

        mField.setValue(MIN - PRECISION);
        assertEquals(MIN, mField.getValue());
        mField.setValue(MAX + PRECISION);
        assertEquals(MAX, mField.getValue());

        mField.setValue(MIN - 1e100);
        assertEquals(MIN, mField.getValue());
        mField.setValue(MAX + 1e100);
        assertEquals(MAX, mField.getValue());
    }

    public void testSetConstraints_IllegalArguments() {
        try {
            mField.setConstraints(Double.NaN, 1.0, 0.1);
            fail("NaN minimum is not allowed.");
        } catch (IllegalArgumentException e) {
            // Expected
        }

        try {
            mField.setConstraints(Double.POSITIVE_INFINITY, 1.0, 0.1);
            fail("POSITIVE_INFINITY minimum is not allowed.");
        } catch (IllegalArgumentException e) {
            // Expected
        }

        try {
            mField.setConstraints(Double.NEGATIVE_INFINITY, 1.0, 0.1);
            fail("NEGATIVE_INFINITY minimum is not allowed.");
        } catch (IllegalArgumentException e) {
            // Expected
        }

        try {
            mField.setConstraints(-1.0, Double.NaN, 0.1);
            fail("NaN maximum is not allowed.");
        } catch (IllegalArgumentException e) {
            // Expected
        }

        try {
            mField.setConstraints(-1.0, Double.POSITIVE_INFINITY, 0.1);
            fail("POSITIVE_INFINITY maximum is not allowed.");
        } catch (IllegalArgumentException e) {
            // Expected
        }

        try {
            mField.setConstraints(-1.0, Double.NEGATIVE_INFINITY, 0.1);
            fail("NEGATIVE_INFINITY maximum is not allowed.");
        } catch (IllegalArgumentException e) {
            // Expected
        }


        try {
            mField.setConstraints(-1.0, 1.0, Double.NaN);
            fail("NaN precision is not allowed.");
        } catch (IllegalArgumentException e) {
            // Expected
        }

        try {
            mField.setConstraints(-1.0, 1.0, Double.POSITIVE_INFINITY);
            fail("POSITIVE_INFINITY precision is not allowed.");
        } catch (IllegalArgumentException e) {
            // Expected
        }

        try {
            mField.setConstraints(-1.0, 1.0, Double.NEGATIVE_INFINITY);
            fail("NEGATIVE_INFINITY precision is not allowed.");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    public void testSetConstraints_InvalidConstraintPairs() {
        try {
            mField.setConstraints(1.0, -1.0, FieldNumber.NO_PRECISION);
            fail("min must be less than max.");
        } catch (IllegalArgumentException e) {
            // Expected
        }

        try {
            mField.setConstraints(1.0, 4.0, 5.0);
            fail("Check for no valid values with given constraints.");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    public void testDecimalPrecisionLessThanOne() {
        final double MIN = FieldNumber.NO_MINIMUM;
        final double MAX = FieldNumber.NO_MAXIMUM;
        final double PRECISION = 0.25;  // Two significant digits
        mField.setConstraints(MIN, MAX, PRECISION);

        assertFalse(mField.isInteger());

        // Exact values
        mField.setValue(0.0);
        assertEquals(0.0, mField.getValue());
        assertEquals("0", mField.getValueString());

        mField.setValue(0.25);
        assertEquals(0.25, mField.getValue());
        assertEquals("0.25", mField.getValueString());

        mField.setValue(1.0);
        assertEquals(1.0, mField.getValue());
        assertEquals("1", mField.getValueString());

        mField.setValue(1.25);
        assertEquals(1.25, mField.getValue());
        assertEquals("1.25", mField.getValueString());

        mField.setValue(2.50);
        assertEquals(2.5, mField.getValue());
        assertEquals("2.5", mField.getValueString());

        mField.setValue(25);
        assertEquals(25.0, mField.getValue());
        assertEquals("25", mField.getValueString());

        mField.setValue(-0.25);
        assertEquals(-0.25, mField.getValue());
        assertEquals("-0.25", mField.getValueString());

        mField.setValue(-1.0);
        assertEquals(-1.0, mField.getValue());
        assertEquals("-1", mField.getValueString());

        mField.setValue(-1.25);
        assertEquals(-1.25, mField.getValue());
        assertEquals("-1.25", mField.getValueString());

        mField.setValue(-2.50);
        assertEquals(-2.5, mField.getValue());
        assertEquals("-2.5", mField.getValueString());

        mField.setValue(-25);
        assertEquals(-25.0, mField.getValue());
        assertEquals("-25", mField.getValueString());

        // Rounded Values
        mField.setValue(0.2);
        assertEquals(0.25, mField.getValue());

        mField.setValue(0.9);
        assertEquals(1.0, mField.getValue());

        mField.setValue(1.1);
        assertEquals(1.0, mField.getValue());

        mField.setValue(1.2);
        assertEquals(1.25, mField.getValue());

        mField.setValue(1.3);
        assertEquals(1.25, mField.getValue());
    }

    public void testIntegerPrecisionOne() {
        final double MIN = FieldNumber.NO_MINIMUM;
        final double MAX = FieldNumber.NO_MAXIMUM;
        final double PRECISION = 1;
        mField.setConstraints(MIN, MAX, PRECISION);

        assertTrue(mField.isInteger());

        // Exact values
        mField.setValue(0.0);

        // Exact values
        mField.setValue(0.0);
        assertEquals(0.0, mField.getValue());
        assertEquals("0", mField.getValueString());

        mField.setValue(1.0);
        assertEquals(1.0, mField.getValue());
        assertEquals("1", mField.getValueString());

        mField.setValue(2.0);
        assertEquals(2.0, mField.getValue());
        assertEquals("2", mField.getValueString());

        mField.setValue(7.0);
        assertEquals(7.0, mField.getValue());
        assertEquals("7", mField.getValueString());

        mField.setValue(10.0);
        assertEquals(10.0, mField.getValue());
        assertEquals("10", mField.getValueString());

        mField.setValue(100.0);
        assertEquals(100.0, mField.getValue());
        assertEquals("100", mField.getValueString());

        mField.setValue(1000000.0);
        assertEquals(1000000.0, mField.getValue());
        assertEquals("1000000", mField.getValueString());

        mField.setValue(-1.0);
        assertEquals(-1.0, mField.getValue());
        assertEquals("-1", mField.getValueString());

        mField.setValue(-2.0);
        assertEquals(-2.0, mField.getValue());
        assertEquals("-2", mField.getValueString());

        mField.setValue(-7.0);
        assertEquals(-7.0, mField.getValue());
        assertEquals("-7", mField.getValueString());

        mField.setValue(-10.0);
        assertEquals(-10.0, mField.getValue());
        assertEquals("-10", mField.getValueString());

        mField.setValue(-100.0);
        assertEquals(-100.0, mField.getValue());
        assertEquals("-100", mField.getValueString());

        mField.setValue(-1000000.0);
        assertEquals(-1000000.0, mField.getValue());
        assertEquals("-1000000", mField.getValueString());


        // Rounded Values
        mField.setValue(0.2);
        assertEquals(0.0, mField.getValue());

        mField.setValue(0.499999);
        assertEquals(0.0, mField.getValue());

        mField.setValue(0.5);
        assertEquals(1.0, mField.getValue());

        mField.setValue(1.1);
        assertEquals(1.0, mField.getValue());

        mField.setValue(99.9999);
        assertEquals(100.0, mField.getValue());

        mField.setValue(-0.2);
        assertEquals(0.0, mField.getValue());

        mField.setValue(-0.5);
        assertEquals(0.0, mField.getValue());

        mField.setValue(-0.501);
        assertEquals(-1.0, mField.getValue());

        mField.setValue(-1.1);
        assertEquals(-1.0, mField.getValue());

        mField.setValue(-99.9999);
        assertEquals(-100.0, mField.getValue());
    }

    public void testIntegerPrecisionTwo() {
        final double MIN = FieldNumber.NO_MINIMUM;
        final double MAX = FieldNumber.NO_MAXIMUM;
        final double PRECISION = 2;
        mField.setConstraints(MIN, MAX, PRECISION);

        assertTrue(mField.isInteger());

        // Exact values
        mField.setValue(0.0);
        assertEquals(0.0, mField.getValue());
        assertEquals("0", mField.getValueString());

        mField.setValue(2.0);
        assertEquals(2.0, mField.getValue());
        assertEquals("2", mField.getValueString());

        mField.setValue(8.0);
        assertEquals(8.0, mField.getValue());
        assertEquals("8", mField.getValueString());

        mField.setValue(10.0);
        assertEquals(10.0, mField.getValue());
        assertEquals("10", mField.getValueString());

        mField.setValue(-2.0);
        assertEquals(-2.0, mField.getValue());
        assertEquals("-2", mField.getValueString());

        mField.setValue(-8.0);
        assertEquals(-8.0, mField.getValue());
        assertEquals("-8", mField.getValueString());

        // Rounded Values
        mField.setValue(0.2);
        assertEquals(0.0, mField.getValue());

        mField.setValue(1.9);
        assertEquals(2.0, mField.getValue());

        mField.setValue(0.999);
        assertEquals(0.0, mField.getValue());

        mField.setValue(1.0);
        assertEquals(2.0, mField.getValue());

        mField.setValue(3.0);
        assertEquals(4.0, mField.getValue());

        mField.setValue(-0.2);
        assertEquals(0.0, mField.getValue());

        mField.setValue(-1.9);
        assertEquals(-2.0, mField.getValue());

        mField.setValue(-1.0);
        assertEquals(0.0, mField.getValue());

        mField.setValue(-1.001);
        assertEquals(-2.0, mField.getValue());
    }

    public void testSetFromString_ExponentNotation() {
        final double MIN = FieldNumber.NO_MINIMUM;
        final double MAX = FieldNumber.NO_MAXIMUM;
        final double PRECISION = FieldNumber.NO_PRECISION;
        mField.setConstraints(MIN, MAX, PRECISION);

        mField.setFromString("123e4");
        assertEquals(1230000d, mField.getValue());

        mField.setFromString("1.23e4");
        assertEquals(12300d, mField.getValue());

        mField.setFromString("-1.23e4");
        assertEquals(-12300d, mField.getValue());

        mField.setFromString("123e-4");
        assertEquals(0.0123, mField.getValue());

        mField.setFromString("1.23e-4");
        assertEquals(0.000123, mField.getValue());
    }
}