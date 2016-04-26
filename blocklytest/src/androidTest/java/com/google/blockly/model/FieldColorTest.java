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
 * Tests for {@link FieldColor}.
 */
public class FieldColorTest extends AndroidTestCase {
    public void testFieldColour() {
        FieldColor field = new FieldColor("fname", 0xaa00aa);
        assertEquals(Field.TYPE_COLOR, field.getType());
        assertEquals("fname", field.getName());
        assertEquals(0xaa00aa, field.getColor());

        field = new FieldColor("fname");
        assertEquals("fname", field.getName());
        assertEquals(FieldColor.DEFAULT_COLOR, field.getColor());

        field.setColor(0xb0bb1e);
        assertEquals(0xb0bb1e, field.getColor());

        // xml parsing
        assertTrue(field.setFromString("#ffcc66"));
        assertEquals(0xffcc66, field.getColor());
        assertTrue(field.setFromString("#00cc66"));
        assertEquals(0x00cc66, field.getColor());
        assertTrue(field.setFromString("#1000cc66"));
        assertEquals(0x00cc66, field.getColor());
        assertFalse(field.setFromString("This is not a color"));
        // Color does not change
        assertEquals(0x00cc66, field.getColor());
        assertFalse(field.setFromString("#fc6"));
        // Color does not change
        assertEquals(0x00cc66, field.getColor());

        assertNotSame(field, field.clone());
    }
}
