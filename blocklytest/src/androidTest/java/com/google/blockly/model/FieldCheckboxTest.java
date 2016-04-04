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
 * Tests for {@link FieldCheckbox}.
 */
public class FieldCheckboxTest extends AndroidTestCase {
    public void testFieldCheckbox() {
        FieldCheckbox field = new FieldCheckbox("fname", true);
        assertEquals(Field.TYPE_CHECKBOX, field.getType());
        assertEquals("fname", field.getName());
        assertEquals(true, field.isChecked());
        field.setChecked(false);
        assertEquals(false, field.isChecked());

        field = new FieldCheckbox("fname", false);
        assertEquals(false, field.isChecked());
        field.setChecked(true);
        assertEquals(true, field.isChecked());

        assertTrue(field.setFromString("false"));
        assertFalse(field.isChecked());

        assertTrue(field.setFromString("true"));
        assertTrue(field.setFromString("TRUE"));
        assertTrue(field.setFromString("True"));
        assertTrue(field.isChecked());

        // xml parsing
        // Boolean.parseBoolean checks the lowercased value against "true" and returns false
        // otherwise.
        assertTrue(field.setFromString("This is not a boolean"));
        assertFalse(field.isChecked());
        field.setChecked(true);
        assertTrue(field.setFromString("t"));
        assertFalse(field.isChecked());

        assertNotSame(field, field.clone());
    }
}
