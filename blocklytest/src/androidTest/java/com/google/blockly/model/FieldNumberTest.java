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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link FieldNumber}.
 */
public class FieldNumberTest {
    private static final String FIELD_NAME = "FIELD_NAME";

    private FieldNumber mField;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        mField = new FieldNumber(FIELD_NAME);
    }

    @Test
    public void testConstructor() {
        assertEquals(FIELD_NAME, mField.getName());
        assertEquals(Field.TYPE_NUMBER, mField.getType(), 0d);
    }

    @Test
    public void testConstraintDefaults() {
        // Before assignment
        assertTrue(Double.isNaN(mField.getMinimumValue()));
        assertTrue(Double.isNaN(mField.getMaximumValue()));
        assertTrue(Double.isNaN(mField.getPrecision()));

        assertFalse(mField.hasMaximum());
        assertFalse(mField.hasMinimum());
        assertFalse(mField.hasPrecision());
    }

    @Test
    public void testConstrainedRangeSuccess() {
        final double MIN = -100.0;
        final double MAX = 100.0;
        final double PRECISION = 0.1;

        mField.setConstraints(MIN, MAX, PRECISION);
        assertEquals(MIN, mField.getMinimumValue(), 0d);
        assertEquals(MAX, mField.getMaximumValue(), 0d);
        assertEquals(PRECISION, mField.getPrecision(), 0d);
        assertTrue(mField.hasPrecision());

        // Normal assignments
        mField.setValue(3.0);
        assertEquals(3.0, mField.getValue(), 0d);
        mField.setValue(0.2);
        assertEquals(0.2, mField.getValue(), 0d);
        mField.setValue(0.09);
        assertEquals("Rounded 0.09 to precision.", 0.1, mField.getValue(), 0d);
        mField.setValue(0.0);
        assertEquals(0.0, mField.getValue(), 0d);
        mField.setValue(-0.1);
        assertEquals(-0.1, mField.getValue(), 0d);
        mField.setValue(-0.1);
        assertEquals(-0.1, mField.getValue(), 0d);
        mField.setValue(-0.29);
        assertEquals(-0.3, mField.getValue(), 0d);

        mField.setValue(MIN + PRECISION);
        assertTrue(MIN < mField.getValue());
        mField.setValue(MAX - PRECISION);
        assertTrue(mField.getValue() < MAX);

        mField.setValue(MIN);
        assertEquals(MIN, mField.getValue(), 0d);
        mField.setValue(MAX);
        assertEquals(MAX, mField.getValue(), 0d);

        mField.setValue(MIN - PRECISION);
        assertEquals(MIN, mField.getValue(), 0d);
        mField.setValue(MAX + PRECISION);
        assertEquals(MAX, mField.getValue(), 0d);

        mField.setValue(MIN - 1e100);
        assertEquals(MIN, mField.getValue(), 0d);
        mField.setValue(MAX + 1e100);
        assertEquals(MAX, mField.getValue(), 0d);
    }

    @Test
    public void testSetConstraints_SpecialValues() {
        mField.setConstraints(Double.NaN, 1.0, 0.1);
        assertTrue(Double.isNaN(mField.getMinimumValue()));
        assertFalse(mField.hasMinimum());
        assertTrue(mField.hasMaximum());

        mField.setConstraints(Double.NEGATIVE_INFINITY, 1.0, 0.1);
        assertTrue(Double.isNaN(mField.getMinimumValue()));
        assertFalse(mField.hasMinimum());
        assertTrue(mField.hasMaximum());

        mField.setConstraints(-1.0, Double.NaN, 0.1);
        assertTrue(Double.isNaN(mField.getMaximumValue()));
        assertTrue(mField.hasMinimum());
        assertFalse(mField.hasMaximum());

        mField.setConstraints(-1.0, Double.POSITIVE_INFINITY, 0.1);
        assertTrue(Double.isNaN(mField.getMaximumValue()));
        assertTrue(mField.hasMinimum());
        assertFalse(mField.hasMaximum());

        mField.setConstraints(-1.0, 1.0, Double.NaN);
        assertTrue(Double.isNaN(mField.getPrecision()));
        assertTrue(mField.hasMinimum());
        assertTrue(mField.hasMaximum());
        assertFalse(mField.hasPrecision());
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

        assertFalse(mField.isInteger());

        // Exact values
        mField.setValue(0.0);
        assertEquals(0.0, mField.getValue(), 0d);
        assertEquals("0", mField.getFormattedValue());

        mField.setValue(0.25);
        assertEquals(0.25, mField.getValue(), 0d);
        assertEquals("0.25", mField.getFormattedValue());

        mField.setValue(1.0);
        assertEquals(1.0, mField.getValue(), 0d);
        assertEquals("1", mField.getFormattedValue());

        mField.setValue(1.25);
        assertEquals(1.25, mField.getValue(), 0d);
        assertEquals("1.25", mField.getFormattedValue());

        mField.setValue(2.50);
        assertEquals(2.5, mField.getValue(), 0d);
        assertEquals("2.5", mField.getFormattedValue());

        mField.setValue(25);
        assertEquals(25.0, mField.getValue(), 0d);
        assertEquals("25", mField.getFormattedValue());

        mField.setValue(-0.25);
        assertEquals(-0.25, mField.getValue(), 0d);
        assertEquals("-0.25", mField.getFormattedValue());

        mField.setValue(-1.0);
        assertEquals(-1.0, mField.getValue(), 0d);
        assertEquals("-1", mField.getFormattedValue());

        mField.setValue(-1.25);
        assertEquals(-1.25, mField.getValue(), 0d);
        assertEquals("-1.25", mField.getFormattedValue());

        mField.setValue(-2.50);
        assertEquals(-2.5, mField.getValue(), 0d);
        assertEquals("-2.5", mField.getFormattedValue());

        mField.setValue(-25);
        assertEquals(-25.0, mField.getValue(), 0d);
        assertEquals("-25", mField.getFormattedValue());

        // Rounded Values
        mField.setValue(0.2);
        assertEquals(0.25, mField.getValue(), 0d);

        mField.setValue(0.9);
        assertEquals(1.0, mField.getValue(), 0d);

        mField.setValue(1.1);
        assertEquals(1.0, mField.getValue(), 0d);

        mField.setValue(1.2);
        assertEquals(1.25, mField.getValue(), 0d);

        mField.setValue(1.3);
        assertEquals(1.25, mField.getValue(), 0d);
    }

    @Test
    public void testIntegerPrecisionOne() {
        final double MIN = FieldNumber.NO_CONSTRAINT;
        final double MAX = FieldNumber.NO_CONSTRAINT;
        final double PRECISION = 1;
        mField.setConstraints(MIN, MAX, PRECISION);

        assertTrue(mField.isInteger());

        // Exact values
        mField.setValue(0.0);

        // Exact values
        mField.setValue(0.0);
        assertEquals(0.0, mField.getValue(), 0d);
        assertEquals("0", mField.getFormattedValue());

        mField.setValue(1.0);
        assertEquals(1.0, mField.getValue(), 0d);
        assertEquals("1", mField.getFormattedValue());

        mField.setValue(2.0);
        assertEquals(2.0, mField.getValue(), 0d);
        assertEquals("2", mField.getFormattedValue());

        mField.setValue(7.0);
        assertEquals(7.0, mField.getValue(), 0d);
        assertEquals("7", mField.getFormattedValue());

        mField.setValue(10.0);
        assertEquals(10.0, mField.getValue(), 0d);
        assertEquals("10", mField.getFormattedValue());

        mField.setValue(100.0);
        assertEquals(100.0, mField.getValue(), 0d);
        assertEquals("100", mField.getFormattedValue());

        mField.setValue(1000000.0);
        assertEquals(1000000.0, mField.getValue(), 0d);
        assertEquals("1000000", mField.getFormattedValue());

        mField.setValue(-1.0);
        assertEquals(-1.0, mField.getValue(), 0d);
        assertEquals("-1", mField.getFormattedValue());

        mField.setValue(-2.0);
        assertEquals(-2.0, mField.getValue(), 0d);
        assertEquals("-2", mField.getFormattedValue());

        mField.setValue(-7.0);
        assertEquals(-7.0, mField.getValue(), 0d);
        assertEquals("-7", mField.getFormattedValue());

        mField.setValue(-10.0);
        assertEquals(-10.0, mField.getValue(), 0d);
        assertEquals("-10", mField.getFormattedValue());

        mField.setValue(-100.0);
        assertEquals(-100.0, mField.getValue(), 0d);
        assertEquals("-100", mField.getFormattedValue());

        mField.setValue(-1000000.0);
        assertEquals(-1000000.0, mField.getValue(), 0d);
        assertEquals("-1000000", mField.getFormattedValue());


        // Rounded Values
        mField.setValue(0.2);
        assertEquals(0.0, mField.getValue(), 0d);

        mField.setValue(0.499999);
        assertEquals(0.0, mField.getValue(), 0d);

        mField.setValue(0.5);
        assertEquals(1.0, mField.getValue(), 0d);

        mField.setValue(1.1);
        assertEquals(1.0, mField.getValue(), 0d);

        mField.setValue(99.9999);
        assertEquals(100.0, mField.getValue(), 0d);

        mField.setValue(-0.2);
        assertEquals(0.0, mField.getValue(), 0d);

        mField.setValue(-0.5);
        assertEquals(0.0, mField.getValue(), 0d);

        mField.setValue(-0.501);
        assertEquals(-1.0, mField.getValue(), 0d);

        mField.setValue(-1.1);
        assertEquals(-1.0, mField.getValue(), 0d);

        mField.setValue(-99.9999);
        assertEquals(-100.0, mField.getValue(), 0d);
    }

    @Test
    public void testIntegerPrecisionTwo() {
        final double MIN = FieldNumber.NO_CONSTRAINT;
        final double MAX = FieldNumber.NO_CONSTRAINT;
        final double PRECISION = 2;
        mField.setConstraints(MIN, MAX, PRECISION);

        assertTrue(mField.isInteger());

        // Exact values
        mField.setValue(0.0);
        assertEquals(0.0, mField.getValue(), 0d);
        assertEquals("0", mField.getFormattedValue());

        mField.setValue(2.0);
        assertEquals(2.0, mField.getValue(), 0d);
        assertEquals("2", mField.getFormattedValue());

        mField.setValue(8.0);
        assertEquals(8.0, mField.getValue(), 0d);
        assertEquals("8", mField.getFormattedValue());

        mField.setValue(10.0);
        assertEquals(10.0, mField.getValue(), 0d);
        assertEquals("10", mField.getFormattedValue());

        mField.setValue(-2.0);
        assertEquals(-2.0, mField.getValue(), 0d);
        assertEquals("-2", mField.getFormattedValue());

        mField.setValue(-8.0);
        assertEquals(-8.0, mField.getValue(), 0d);
        assertEquals("-8", mField.getFormattedValue());

        // Rounded Values
        mField.setValue(0.2);
        assertEquals(0.0, mField.getValue(), 0d);

        mField.setValue(1.9);
        assertEquals(2.0, mField.getValue(), 0d);

        mField.setValue(0.999);
        assertEquals(0.0, mField.getValue(), 0d);

        mField.setValue(1.0);
        assertEquals(2.0, mField.getValue(), 0d);

        mField.setValue(3.0);
        assertEquals(4.0, mField.getValue(), 0d);

        mField.setValue(-0.2);
        assertEquals(0.0, mField.getValue(), 0d);

        mField.setValue(-1.9);
        assertEquals(-2.0, mField.getValue(), 0d);

        mField.setValue(-1.0);
        assertEquals(0.0, mField.getValue(), 0d);

        mField.setValue(-1.001);
        assertEquals(-2.0, mField.getValue(), 0d);
    }

    @Test
    public void testSetFromString_ExponentNotation() {
        final double MIN = FieldNumber.NO_CONSTRAINT;
        final double MAX = FieldNumber.NO_CONSTRAINT;
        final double PRECISION = FieldNumber.NO_CONSTRAINT;
        mField.setConstraints(MIN, MAX, PRECISION);

        mField.setFromString("123e4");
        assertEquals(1230000d, mField.getValue(), 0d);

        mField.setFromString("1.23e4");
        assertEquals(12300d, mField.getValue(), 0d);

        mField.setFromString("-1.23e4");
        assertEquals(-12300d, mField.getValue(), 0d);

        mField.setFromString("123e-4");
        assertEquals(0.0123, mField.getValue(), 0d);

        mField.setFromString("1.23e-4");
        assertEquals(0.000123, mField.getValue(), 0d);
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