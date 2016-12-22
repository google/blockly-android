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

import static com.google.common.truth.Truth.assertThat;

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
        assertThat(mOptions.size()).isEqualTo(VALUES.size());
        for (int i = 0; i < mOptions.size(); ++i) {
            FieldDropdown.Option option = mOptions.get(i);
            assertThat(option.value).isEqualTo(VALUES.get(i));
            assertThat(option.displayName).isEqualTo(LABELS.get(i));
        }
    }

    @Test
    public void testDropdownConstructor() {
        assertThat(mDropDown.getType()).isEqualTo(Field.TYPE_DROPDOWN);
        assertThat(mDropDown.getName()).isEqualTo(FIELD_NAME);
        assertThat(mDropDown.getSelectedIndex()).isEqualTo(0);
        assertThat(mDropDown.getOptions().size()).isEqualTo(VALUES.size());

        // The options may be shared, so identity
        assertThat(mOptions).isSameAs(mDropDown.getOptions());
    }

    @Test
    public void testSelectedByIndex() {
        for (int i = 0; i < VALUES.size(); i++) {
            mDropDown.setSelectedIndex(i);
            assertThat(mDropDown.getSelectedIndex()).isEqualTo(i);
            assertThat(mDropDown.getSelectedValue()).isEqualTo(VALUES.get(i));
            assertThat(mDropDown.getSerializedValue()).isEqualTo(VALUES.get(i));
            assertThat(mDropDown.getSelectedDisplayName()).isEqualTo(LABELS.get(i));
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
            assertThat(mDropDown.getSelectedIndex()).isEqualTo(i);
            assertThat(mDropDown.getSelectedValue()).isEqualTo(VALUES.get(i));
            assertThat(mDropDown.getSerializedValue()).isEqualTo(VALUES.get(i));
            assertThat(mDropDown.getSelectedDisplayName()).isEqualTo(LABELS.get(i));
        }

        // setting a non-existent value defaults to 0
        mDropDown.setSelectedValue("bad value");
        assertThat(mDropDown.getSelectedIndex()).isEqualTo(0);
        assertThat(mDropDown.getSelectedDisplayName()).isEqualTo(LABELS.get(0));
        assertThat(mDropDown.getSerializedValue()).isEqualTo(VALUES.get(0));
    }

    @Test
    public void testSetFromString() {
        assertThat(mDropDown.setFromString(VALUES.get(1))).isTrue();
        assertThat(mDropDown.getSelectedIndex()).isEqualTo(1);
        assertThat(mDropDown.getSelectedDisplayName()).isEqualTo(LABELS.get(1));
        assertThat(mDropDown.getSerializedValue()).isEqualTo(VALUES.get(1));

        // Setting a non-existent value defaults to 0
        mDropDown.setSelectedValue("bad value");
        assertThat(mDropDown.getSelectedIndex()).isEqualTo(0);
        assertThat(mDropDown.getSelectedDisplayName()).isEqualTo(LABELS.get(0));
        assertThat(mDropDown.getSerializedValue()).isEqualTo(VALUES.get(0));
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

        assertThat(mDropDown.getSelectedIndex()).isEqualTo(oldSelectedIndex + 1);
        assertThat(mDropDown.getSelectedValue()).isEqualTo(oldSelectedValue);
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
        assertThat(mDropDown.getSelectedIndex()).isEqualTo(0);

        for (int i = 1; i < VALUES.size(); i++) {
            mDropDown.setSelectedIndex(i);
            assertThat(mDropDown.getSerializedValue()).isEqualTo(LABELS.get(i));
            assertThat(mDropDown.getSelectedDisplayName()).isEqualTo(VALUES.get(i));
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
