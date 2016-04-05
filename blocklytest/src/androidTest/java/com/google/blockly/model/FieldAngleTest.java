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

/**
 * Tests for {@link FieldAngle}.
 */
public class FieldAngleTest extends AndroidTestCase {
    public void testFieldAngle() {
        FieldAngle field = new FieldAngle("name", 0);
        assertEquals(Field.TYPE_ANGLE, field.getType());
        assertEquals("name", field.getName());
        assertEquals(0, field.getAngle());

        field = new FieldAngle("360", 360);
        assertEquals("360", field.getName());
        assertEquals(360, field.getAngle());

        field = new FieldAngle("name", 720);
        assertEquals(0, field.getAngle());

        field = new FieldAngle("name", -180);
        assertEquals(180, field.getAngle());

        field = new FieldAngle("name", 10000);
        assertEquals(280, field.getAngle());

        field = new FieldAngle("name", -10000);
        assertEquals(80, field.getAngle());

        field.setAngle(360);
        assertEquals(360, field.getAngle());
        field.setAngle(27);
        assertEquals(27, field.getAngle());
        field.setAngle(-10001);
        assertEquals(79, field.getAngle());

        // xml parsing
        assertTrue(field.setFromString("-180"));
        assertEquals(180, field.getAngle());
        assertTrue(field.setFromString("27"));
        assertEquals(27, field.getAngle());
        assertFalse(field.setFromString("this is not a number"));

        assertNotSame(field, field.clone());
    }
}
