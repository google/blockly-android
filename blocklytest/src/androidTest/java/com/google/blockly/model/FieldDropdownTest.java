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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link FieldDropdown}.
 */
public class FieldDropdownTest {
    private final String FIELD_NAME = "FieldDropdown";
    private final List<String> VALUES = Collections.unmodifiableList(
            Arrays.asList("Value1", "Value2", "Value3"));
    private final List<String> LABELS = Collections.unmodifiableList(
            Arrays.asList("Label1", "Label2", "Label3"));

    private FieldDropdown.Options mOptions;
    private FieldDropdown mDropDown;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        List<FieldDropdown.Option> optionList = new ArrayList<>(VALUES.size());
        for (int i = 0; i < VALUES.size(); ++i) {
            optionList.add(new FieldDropdown.Option(VALUES.get(i), LABELS.get(i)));
        }

        mOptions = new FieldDropdown.Options(optionList);
        mDropDown = new FieldDropdown(FIELD_NAME, mOptions);
    }

    @Test
    public void testOptionsConstructorFromStrings() {
        // Created above.
        assertEquals(VALUES.size(), mOptions.size());
        for (int i = 0; i < mOptions.size(); ++i) {
            FieldDropdown.Option option = mOptions.get(i);
            assertEquals(VALUES.get(i), option.value);
            assertEquals(LABELS.get(i), option.displayName);
        }
    }

    @Test
    public void testDropdownConstructor() {
        assertEquals(Field.TYPE_DROPDOWN, mDropDown.getType());
        assertEquals(FIELD_NAME, mDropDown.getName());
        assertEquals(0, mDropDown.getSelectedIndex());
        assertEquals(VALUES.size(), mDropDown.getOptions().size());

        // The options may be shared, so identity
        assertSame(mOptions, mDropDown.getOptions());
    }

    @Test
    public void testSelectedByIndex() {
        for (int i = 0; i < VALUES.size(); i++) {
            mDropDown.setSelectedIndex(i);
            assertEquals(i, mDropDown.getSelectedIndex());
            assertEquals(VALUES.get(i), mDropDown.getSelectedValue());
            assertEquals(VALUES.get(i), mDropDown.getSerializedValue());
            assertEquals(LABELS.get(i), mDropDown.getSelectedDisplayName());
        }

        thrown.expect(IllegalArgumentException.class);
        mDropDown.setSelectedIndex(VALUES.size() + 1);
    }

    @Test
    public void testSetSelectedByValue() {
        // Change initial value
        mDropDown.setSelectedIndex(VALUES.size() - 1);

        for (int i = 0; i < VALUES.size(); ++i) {
            mDropDown.setSelectedValue(VALUES.get(i));
            assertEquals(i, mDropDown.getSelectedIndex());
            assertEquals(VALUES.get(i), mDropDown.getSelectedValue());
            assertEquals(VALUES.get(i), mDropDown.getSerializedValue());
            assertEquals(LABELS.get(i), mDropDown.getSelectedDisplayName());
        }

        // setting a non-existent value defaults to 0
        mDropDown.setSelectedValue("bad value");
        assertEquals(0, mDropDown.getSelectedIndex());
        assertEquals(LABELS.get(0), mDropDown.getSelectedDisplayName());
        assertEquals(VALUES.get(0), mDropDown.getSerializedValue());
    }

    @Test
    public void testSetFromString() {
        assertTrue(mDropDown.setFromString(VALUES.get(1)));
        assertEquals(1, mDropDown.getSelectedIndex());
        assertEquals(LABELS.get(1), mDropDown.getSelectedDisplayName());
        assertEquals(VALUES.get(1), mDropDown.getSerializedValue());

        // Setting a non-existent value defaults to 0
        mDropDown.setSelectedValue("bad value");
        assertEquals(0, mDropDown.getSelectedIndex());
        assertEquals(LABELS.get(0), mDropDown.getSelectedDisplayName());
        assertEquals(VALUES.get(0), mDropDown.getSerializedValue());
    }

    @Test
    public void testUpdateOptionsWithMatch() {
        // Initialize to something other than 0;
        mDropDown.setSelectedIndex(VALUES.size() / 2);
        String oldSelectedValue = mDropDown.getSelectedValue();
        int oldSelectedIndex = mDropDown.getSelectedIndex();

        // Adding new options, such that selection index changes
        List<FieldDropdown.Option> newOptions = Arrays.asList(
                new FieldDropdown.Option("BEFORE", "Before"),
                new FieldDropdown.Option(VALUES.get(0), LABELS.get(0)),
                new FieldDropdown.Option(VALUES.get(1), LABELS.get(1)),
                new FieldDropdown.Option(VALUES.get(2), LABELS.get(2)),
                new FieldDropdown.Option("AFTER", "After")
        );
        mDropDown.getOptions().updateOptions(newOptions);

        assertEquals(oldSelectedIndex + 1, mDropDown.getSelectedIndex());
        assertEquals(oldSelectedValue, mDropDown.getSelectedValue());
    }

    @Test
    public void testUpdateOptionsWithoutMatch() {
        // Initialize to something other than 0;
        mDropDown.setSelectedIndex(VALUES.size() - 1);

        // swap the values/display names and verify it was updated.
        List<FieldDropdown.Option> newOptions = Arrays.asList(
            new FieldDropdown.Option(LABELS.get(0), VALUES.get(0)),
            new FieldDropdown.Option(LABELS.get(1), VALUES.get(1)),
            new FieldDropdown.Option(LABELS.get(2), VALUES.get(2))
        );
        mDropDown.getOptions().updateOptions(newOptions);

        // No matching value, so index should be 0;
        assertEquals(0, mDropDown.getSelectedIndex());

        for (int i = 1; i < VALUES.size(); i++) {
            mDropDown.setSelectedIndex(i);
            assertEquals(LABELS.get(i), mDropDown.getSerializedValue());
            assertEquals(VALUES.get(i), mDropDown.getSelectedDisplayName());
        }
    }

    @Test
    public void testObserverEvents() {
        FieldTestHelper.testObserverEvent(mDropDown,
                /* New value */ VALUES.get(2),
                /* Expected old value */ VALUES.get(0),
                /* Expected new value */ VALUES.get(2));

        FieldTestHelper.testObserverNoEvent(mDropDown);
    }
}
