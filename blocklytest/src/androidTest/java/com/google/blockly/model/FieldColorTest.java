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
    private static final int INITIAL_COLOR = 0xaabbcc;
    private static final String INITIAL_COLOR_STRING = "#aabbcc";

    FieldColor mField;

    public void setUp() {
        mField = new FieldColor("fname", INITIAL_COLOR);
    }

    public void testConstructors() {
        assertEquals(Field.TYPE_COLOR, mField.getType());
        assertEquals("fname", mField.getName());
        assertEquals(INITIAL_COLOR, mField.getColor());

        mField = new FieldColor("fname");
        assertEquals("fname", mField.getName());
        assertEquals(FieldColor.DEFAULT_COLOR, mField.getColor());
    }

    public void testSetColor() {
        mField.setColor(0xb0bb1e);
        assertEquals(0xb0bb1e, mField.getColor());
    }

    public void testSetFromString() {
        assertTrue(mField.setFromString("#ffcc66"));
        assertEquals(0xffcc66, mField.getColor());
        assertTrue(mField.setFromString("#00cc66"));
        assertEquals(0x00cc66, mField.getColor());

        // Ignore alpha channel
        assertTrue(mField.setFromString("#1000cc66"));
        assertEquals(0x00cc66, mField.getColor());

        // Invalid color strings. Value should not change.
        assertFalse(mField.setFromString("This is not a color"));
        assertEquals(0x00cc66, mField.getColor());
        assertFalse(mField.setFromString("#fc6"));
        assertEquals(0x00cc66, mField.getColor());
    }

    public void testClone() {
        FieldColor clone = mField.clone();
        assertNotSame(mField, clone);
        assertEquals(mField.getName(), clone.getName());
        assertEquals(mField.getColor(), clone.getColor());
    }

    public void testObserverEvents() {
        FieldTestHelper.testObserverEvent(new FieldColor("normal", INITIAL_COLOR),
                /* New value */ "#ffcc66",
                /* Expected old value */ INITIAL_COLOR_STRING,
                /* Expected new value */ "#ffcc66");

        // No Change
        FieldTestHelper.testObserverNoEvent(
                new FieldColor("normal", INITIAL_COLOR),
                /* New value */ INITIAL_COLOR_STRING);
        FieldTestHelper.testObserverNoEvent(
                new FieldColor("normal", INITIAL_COLOR),
                /* New value */ INITIAL_COLOR_STRING.toUpperCase());
    }
}
