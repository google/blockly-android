/*
 *  Copyright 2015 Google Inc. All Rights Reserved.
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

import android.support.v4.util.SimpleArrayMap;
import android.test.AndroidTestCase;

import java.util.Date;

/**
 * Tests for {@link Field}.
 */
public class FieldTest extends AndroidTestCase {

    public void testFieldLabel() throws CloneNotSupportedException {
        Field.FieldLabel field = new Field.FieldLabel("field name", "some text");
        assertEquals(Field.TYPE_LABEL, field.getType());
        assertEquals("field name", field.getName());
        assertEquals("some text", field.getText());

        field = new Field.FieldLabel("name", null);
        assertEquals("name", field.getName());
        assertEquals("", field.getText());

        assertNotSame(field, field.clone());
    }

    public void testFieldInput() throws CloneNotSupportedException {
        Field.FieldInput field = new Field.FieldInput("field name", "start text");
        assertEquals(Field.TYPE_INPUT, field.getType());
        assertEquals("field name", field.getName());
        assertEquals("start text", field.getText());

        field.setText("new text");
        assertEquals("new text", field.getText());

        // xml parsing
        assertTrue(field.setFromString("newest text"));
        assertEquals("newest text", field.getText());

        assertNotSame(field, field.clone());
    }

    public void testFieldAngle() throws CloneNotSupportedException {
        Field.FieldAngle field = new Field.FieldAngle("name", 0);
        assertEquals(Field.TYPE_ANGLE, field.getType());
        assertEquals("name", field.getName());
        assertEquals(0, field.getAngle());

        field = new Field.FieldAngle("360", 360);
        assertEquals("360", field.getName());
        assertEquals(360, field.getAngle());

        field = new Field.FieldAngle("name", 720);
        assertEquals(0, field.getAngle());

        field = new Field.FieldAngle("name", -180);
        assertEquals(180, field.getAngle());

        field = new Field.FieldAngle("name", 10000);
        assertEquals(280, field.getAngle());

        field = new Field.FieldAngle("name", -10000);
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

    public void testFieldCheckbox() throws CloneNotSupportedException {
        Field.FieldCheckbox field = new Field.FieldCheckbox("fname", true);
        assertEquals(Field.TYPE_CHECKBOX, field.getType());
        assertEquals("fname", field.getName());
        assertEquals(true, field.isChecked());
        field.setChecked(false);
        assertEquals(false, field.isChecked());

        field = new Field.FieldCheckbox("fname", false);
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

    public void testFieldColour() throws CloneNotSupportedException {
        Field.FieldColour field = new Field.FieldColour("fname", 0xaa00aa);
        assertEquals(Field.TYPE_COLOUR, field.getType());
        assertEquals("fname", field.getName());
        assertEquals(0xaa00aa, field.getColour());

        field = new Field.FieldColour("fname");
        assertEquals("fname", field.getName());
        assertEquals(Field.FieldColour.DEFAULT_COLOUR, field.getColour());

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

    public void testFieldDate() throws CloneNotSupportedException {
        Field.FieldDate field = new Field.FieldDate("alphabet", "2015-09-10");
        assertEquals(Field.TYPE_DATE, field.getType());
        assertEquals("alphabet", field.getName());
        assertEquals("2015-09-10", field.getDateString());

        Date date = new Date();
        field.setDate(date);
        assertEquals(date, field.getDate());
        date.setTime(date.getTime() + 86400000);
        field.setTime(date.getTime());
        assertEquals(date, field.getDate());

        assertTrue(field.setFromString("2017-03-03"));
        assertEquals("2017-03-03", field.getDateString());

        // xml parsing
        assertFalse(field.setFromString("today"));
        assertFalse(field.setFromString("2017/03/03"));
        assertFalse(field.setFromString(""));

        Field.FieldDate clone = field.clone();
        assertNotSame(field, clone);
        assertNotSame(field.getDate(), clone.getDate());
    }

    public void testFieldVariable() throws CloneNotSupportedException {
        Field.FieldVariable field = new Field.FieldVariable("fname", "varName");
        assertEquals(Field.TYPE_VARIABLE, field.getType());
        assertEquals("fname", field.getName());
        assertEquals("varName", field.getVariable());

        field.setVariable("newVar");
        assertEquals("newVar", field.getVariable());

        // xml parsing
        assertTrue(field.setFromString("newestVar"));
        assertEquals("newestVar", field.getVariable());
        assertFalse(field.setFromString(""));

        assertNotSame(field, field.clone());
    }

    public void testFieldDropdown() throws CloneNotSupportedException {
        String[] displayNames = new String[] {"A", "B", "C"};
        String[] values = new String[] {"1", "2", "3"};
        // Test creating a dropdown from two String[]s
        Field.FieldDropdown field = new Field.FieldDropdown("fname", displayNames, values);
        assertEquals(Field.TYPE_DROPDOWN, field.getType());
        assertEquals("fname", field.getName());
        assertEquals(0, field.getSelectedIndex());
        assertEquals(displayNames[0], field.getSelectedDisplayName());
        assertEquals(values[0], field.getSelectedValue());

        SimpleArrayMap<String, String> options = field.getOptions();
        assertEquals(displayNames.length, options.size());
        for (int i = 0; i < displayNames.length; i++) {
            assertEquals(displayNames[i], options.keyAt(i));
            assertEquals(values[i], options.valueAt(i));
        }

        // Test creating it from a List<Pair<String, String>>
        field = new Field.FieldDropdown("fname", options);
        assertEquals(Field.TYPE_DROPDOWN, field.getType());
        assertEquals("fname", field.getName());
        assertEquals(0, field.getSelectedIndex());
        assertEquals(displayNames[0], field.getSelectedDisplayName());
        assertEquals(values[0], field.getSelectedValue());

        options = field.getOptions();
        assertEquals(displayNames.length, options.size());
        for (int i = 0; i < displayNames.length; i++) {
            assertEquals(displayNames[i], options.keyAt(i));
            assertEquals(values[i], options.valueAt(i));
        }

        // test changing the index
        field.setSelectedIndex(1);
        assertEquals(1, field.getSelectedIndex());
        assertEquals(displayNames[1], field.getSelectedDisplayName());
        assertEquals(values[1], field.getSelectedValue());

        // test setting by value
        field.setSelectedValue(values[2]);
        assertEquals(2, field.getSelectedIndex());
        assertEquals(displayNames[2], field.getSelectedDisplayName());
        assertEquals(values[2], field.getSelectedValue());

        // xml parsing
        assertTrue(field.setFromString(values[1]));
        assertEquals(1, field.getSelectedIndex());
        assertEquals(displayNames[1], field.getSelectedDisplayName());
        assertEquals(values[1], field.getSelectedValue());

        // xml parsing; setting a non-existent value defaults to 0
        assertTrue(field.setFromString(""));
        assertEquals(0, field.getSelectedIndex());
        assertEquals(displayNames[0], field.getSelectedDisplayName());
        assertEquals(values[0], field.getSelectedValue());

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
        assertEquals(values[0], field.getSelectedValue());

        // swap the values/display names and verify it was updated.
        field.setOptions(values, displayNames);
        options = field.getOptions();
        assertEquals(displayNames.length, options.size());
        for (int i = 0; i < displayNames.length; i++) {
            assertEquals(values[i], options.keyAt(i));
            assertEquals(displayNames[i], options.valueAt(i));
        }

        Field.FieldDropdown clone = field.clone();
        assertNotSame(field, clone);
        assertNotSame(field.getOptions(), clone.getOptions());
        assertEquals(field.getOptions().get(0), clone.getOptions().get(0));
    }

    public void testFieldImage() throws CloneNotSupportedException {
        String url = "https://www.gstatic.com/codesite/ph/images/star_on.gif";
        Field.FieldImage field = new Field.FieldImage("fname", url, 15, 21, "altText");
        assertEquals(Field.TYPE_IMAGE, field.getType());
        assertEquals("fname", field.getName());
        assertEquals(url, field.getSource());
        assertEquals(15, field.getWidth());
        assertEquals(21, field.getHeight());
        assertEquals("altText", field.getAltText());

        assertNotSame(field, field.clone());
    }
}
