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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tests for {@link FieldDropdown}.
 */
public class FieldDropdownTest extends AndroidTestCase {
    public void testFieldDropdown() {
        String[] values = new String[] {"1", "2", "3"};
        String[] displayNames = new String[] {"A", "B", "C"};

        // Test creating a dropdown from two String[]s
        FieldDropdown field = new FieldDropdown("fname");
        field.setOptions(Arrays.asList(values), Arrays.asList(displayNames));
        assertEquals(Field.TYPE_DROPDOWN, field.getType());
        assertEquals("fname", field.getName());
        assertEquals(0, field.getSelectedIndex());
        List<String> fieldDisplayNames = field.getDisplayNames();
        assertEquals(displayNames.length, field.getDisplayNames().size());
        for (int i = 0; i < values.length; i++) {
            field.setSelectedIndex(i);
            assertEquals(values[i], field.getSerializedValue());
            assertEquals(displayNames[i], field.getSelectedDisplayName());
            assertEquals(displayNames[i], fieldDisplayNames.get(i));
        }

        // Test creating it from a List<Option>
        List<FieldDropdown.Option> options = new ArrayList<>(values.length);
        for (int i = 0; i < values.length; i++) {
            options.add(new FieldDropdown.Option(values[i], displayNames[i]));
        }
        field = new FieldDropdown("fname", options);
        assertEquals(Field.TYPE_DROPDOWN, field.getType());
        assertEquals("fname", field.getName());
        assertEquals(0, field.getSelectedIndex());
        fieldDisplayNames = field.getDisplayNames();
        assertEquals(displayNames.length, field.getDisplayNames().size());
        for (int i = 0; i < values.length; i++) {
            field.setSelectedIndex(i);
            assertEquals(values[i], field.getSerializedValue());
            assertEquals(displayNames[i], field.getSelectedDisplayName());
            assertEquals(displayNames[i], fieldDisplayNames.get(i));
        }

        // test changing the index
        field.setSelectedIndex(1);
        assertEquals(1, field.getSelectedIndex());
        assertEquals(displayNames[1], field.getSelectedDisplayName());
        assertEquals(values[1], field.getSerializedValue());

        // test setting by value
        field.setSelectedValue(values[2]);
        assertEquals(2, field.getSelectedIndex());
        assertEquals(displayNames[2], field.getSelectedDisplayName());
        assertEquals(values[2], field.getSerializedValue());

        // xml parsing
        assertTrue(field.setFromString(values[1]));
        assertEquals(1, field.getSelectedIndex());
        assertEquals(displayNames[1], field.getSelectedDisplayName());
        assertEquals(values[1], field.getSerializedValue());

        // xml parsing; setting a non-existent value defaults to 0
        assertTrue(field.setFromString(""));
        assertEquals(0, field.getSelectedIndex());
        assertEquals(displayNames[0], field.getSelectedDisplayName());
        assertEquals(values[0], field.getSerializedValue());

        try {
            // test setting out of bounds
            field.setSelectedIndex(5);
            fail("Setting an index that doesn't exist should throw an exception.");
        } catch (IllegalArgumentException e) {
            //expected
        }

        // setting a non-existent value defaults to 0
        field.setSelectedValue("blah");
        assertEquals(0, field.getSelectedIndex());
        assertEquals(displayNames[0], field.getSelectedDisplayName());
        assertEquals(values[0], field.getSerializedValue());

        // swap the values/display names and verify it was updated.
        field.setOptions(Arrays.asList(displayNames), Arrays.asList(values));
        for (int i = 0; i < values.length; i++) {
            field.setSelectedIndex(i);
            assertEquals(displayNames[i], field.getSerializedValue());
            assertEquals(values[i], field.getSelectedDisplayName());
        }
    }
}
