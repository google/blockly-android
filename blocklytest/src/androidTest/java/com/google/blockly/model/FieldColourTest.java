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
 * Tests for {@link FieldColour}.
 */
public class FieldColourTest extends AndroidTestCase {
    public void testFieldColour() {
        FieldColour field = new FieldColour("fname", 0xaa00aa);
        assertEquals(Field.TYPE_COLOUR, field.getType());
        assertEquals("fname", field.getName());
        assertEquals(0xaa00aa, field.getColour());

        field = new FieldColour("fname");
        assertEquals("fname", field.getName());
        assertEquals(FieldColour.DEFAULT_COLOUR, field.getColour());

        field.setColour(0xb0bb1e);
        assertEquals(0xb0bb1e, field.getColour());

        // xml parsing
        assertTrue(field.setFromString("#ffcc66"));
        assertEquals(0xffcc66, field.getColour());
        assertTrue(field.setFromString("#00cc66"));
        assertEquals(0x00cc66, field.getColour());
        assertTrue(field.setFromString("#1000cc66"));
        assertEquals(0x00cc66, field.getColour());
        assertFalse(field.setFromString("This is not a color"));
        // Color does not change
        assertEquals(0x00cc66, field.getColour());
        assertFalse(field.setFromString("#fc6"));
        // Color does not change
        assertEquals(0x00cc66, field.getColour());

        assertNotSame(field, field.clone());
    }
}
