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
    public void testFieldDate() {
        FieldDate field = new FieldDate("alphabet", "2015-09-14");
        assertEquals(Field.TYPE_DATE, field.getType());
        assertEquals("alphabet", field.getName());
        assertEquals("2015-09-14", field.getDateString());

        Date date = new Date();
        field.setDate(date);
        assertEquals(date, field.getDate());
        date.setTime(date.getTime() + 86400000);
        field.setTime(date.getTime());
        assertEquals(date, field.getDate());

        assertTrue(field.setFromString("2017-03-23"));
        assertEquals("2017-03-23", field.getDateString());

        // xml parsing
        assertFalse(field.setFromString("today"));
        assertFalse(field.setFromString("2017/03/03"));
        assertFalse(field.setFromString(""));

        FieldDate clone = field.clone();
        assertNotSame(field, clone);
        assertNotSame(field.getDate(), clone.getDate());
    }
}
