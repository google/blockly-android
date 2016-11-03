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

import android.test.AndroidTestCase;

import java.util.Date;

/**
 * Tests for {@link FieldDate}.
 */
public class FieldDateTest extends AndroidTestCase {
    private static final String INITIAL_VALUE = "2015-09-14";

    FieldDate mField;

    public void setUp() {
        mField = new FieldDate("alphabet", INITIAL_VALUE);
    }

    public void testConstructor() {
        assertEquals(Field.TYPE_DATE, mField.getType());
        assertEquals("alphabet", mField.getName());
        assertEquals(INITIAL_VALUE, mField.getSerializedValue());
    }

    public void testSetDate() {
        Date date = new Date();
        mField.setDate(date);
        assertEquals(date, mField.getDate());
        date.setTime(date.getTime() + 86400000);
        mField.setTime(date.getTime());
        assertEquals(date, mField.getDate());

        assertTrue(mField.setFromString("2017-03-23"));
        assertEquals("2017-03-23", mField.getLocalizedDateString());
    }

    public void testSetFromString() {
        assertFalse(mField.setFromString("today"));
        assertFalse(mField.setFromString("2017/03/03"));
        assertFalse(mField.setFromString(""));
    }

    public void testClone() {
        FieldDate clone = mField.clone();
        assertNotSame(mField, clone);
        assertEquals(mField.getName(), clone.getName());
        assertNotSame(mField.getDate(), clone.getDate());
        assertEquals(mField.getDate(), clone.getDate());
    }

    public void testObserverEvents() {
        FieldTestHelper.testObserverEvent(mField,
                /* New value */ "2017-03-23",
                /* Expected old value */ INITIAL_VALUE,
                /* Expected new value */ "2017-03-23");

        // No events if no change
        FieldTestHelper.testObserverNoEvent(mField);
    }
}
