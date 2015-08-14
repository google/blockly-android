package com.google.blockly.model;

import android.test.AndroidTestCase;
import android.util.Pair;

import com.google.blockly.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Tests for {@link Field}.
 */
public class FieldTest extends AndroidTestCase {

    public void testFieldLabel() {
        Field.FieldLabel field = new Field.FieldLabel("field name", "some text");
        assertEquals(Field.TYPE_LABEL, field.getType());
        assertEquals("field name", field.getName());
        assertEquals("some text", field.getText());

        field = new Field.FieldLabel("name", null);
        assertEquals("name", field.getName());
        assertEquals("", field.getText());
    }

    public void testFieldInput() {
        Field.FieldInput field = new Field.FieldInput("field name", "start text");
        assertEquals(Field.TYPE_INPUT, field.getType());
        assertEquals("field name", field.getName());
        assertEquals("start text", field.getText());

        field.setText("new text");
        assertEquals("new text", field.getText());
    }

    public void testFieldAngle() {
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
    }

    public void testFieldCheckbox() {
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
    }

    public void testFieldColour() {
        Field.FieldColour field = new Field.FieldColour("fname", 0xaa00aa);
        assertEquals(Field.TYPE_COLOUR, field.getType());
        assertEquals("fname", field.getName());
        assertEquals(0xaa00aa, field.getColour());

        field = new Field.FieldColour("fname");
        assertEquals("fname", field.getName());
        assertEquals(Field.FieldColour.DEFAULT_COLOUR, field.getColour());

        field.setColour(0xb0bb1e);
        assertEquals(0xb0bb1e, field.getColour());
    }

    public void testFieldDate() {
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
    }

    public void testFieldVariable() {
        Field.FieldVariable field = new Field.FieldVariable("fname", "varName");
        assertEquals(Field.TYPE_VARIABLE, field.getType());
        assertEquals("fname", field.getName());
        assertEquals("varName", field.getVariable());

        field.setVariable("newVar");
        assertEquals("newVar", field.getVariable());
    }

    public void testFieldDropdown() {
        String[] displayNames = new String[] {"A", "B", "C"};
        String[] values = new String[] {"1", "2", "3"};
        // Test creating a dropdown from two String[]s
        Field.FieldDropdown field = new Field.FieldDropdown("fname", displayNames, values);
        assertEquals(Field.TYPE_DROPDOWN, field.getType());
        assertEquals("fname", field.getName());
        assertEquals(0, field.getSelectedIndex());
        assertEquals(displayNames[0], field.getSelectedDisplayName());
        assertEquals(values[0], field.getSelectedValue());

        List<Pair<String, String>> options = field.getOptions();
        assertEquals(displayNames.length, options.size());
        for (int i = 0; i < displayNames.length; i++) {
            Pair<String, String> option = options.get(i);
            assertEquals(displayNames[i], option.first);
            assertEquals(values[i], option.second);
        }

        // Test creating it from a List<Pair<String, String>>
        field = new Field.FieldDropdown("fname", options);;
        assertEquals(Field.TYPE_DROPDOWN, field.getType());
        assertEquals("fname", field.getName());
        assertEquals(0, field.getSelectedIndex());
        assertEquals(displayNames[0], field.getSelectedDisplayName());
        assertEquals(values[0], field.getSelectedValue());

        options = field.getOptions();
        assertEquals(displayNames.length, options.size());
        for (int i = 0; i < displayNames.length; i++) {
            Pair<String, String> option = options.get(i);
            assertEquals(displayNames[i], option.first);
            assertEquals(values[i], option.second);
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
            Pair<String, String> option = options.get(i);
            assertEquals(values[i], option.first);
            assertEquals(displayNames[i], option.second);
        }
    }

    public void testFieldImage() {
        String url = "https://www.gstatic.com/codesite/ph/images/star_on.gif";
        Field.FieldImage field = new Field.FieldImage("fname", url, 15, 21, "altText");
        assertEquals(Field.TYPE_IMAGE, field.getType());
        assertEquals("fname", field.getName());
        assertEquals(url, field.getSource());
        assertEquals(15, field.getWidth());
        assertEquals(21, field.getHeight());
        assertEquals("altText", field.getAltText());
    }
}
