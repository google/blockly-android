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
import org.junit.Test;

import java.util.Date;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests for {@link FieldDate}.
 */
public class FieldDateTest {
    private static final String INITIAL_VALUE = "2015-09-14";

    FieldDate mField;

    @Before
    public void setUp() {
        mField = new FieldDate("alphabet", INITIAL_VALUE);
    }

    @Test
    public void testConstructor() {
        assertThat(mField.getType()).isEqualTo(Field.TYPE_DATE);
        assertThat(mField.getName()).isEqualTo("alphabet");
        assertThat(mField.getSerializedValue()).isEqualTo(INITIAL_VALUE);
    }

    @Test
    public void testSetDate() {
        Date date = new Date();
        mField.setDate(date);
        assertThat(mField.getDate()).isEqualTo(date);
        date.setTime(date.getTime() + 86400000);
        mField.setTime(date.getTime());
        assertThat(mField.getDate()).isEqualTo(date);

        assertThat(mField.setFromString("2017-03-23")).isTrue();
        assertThat(mField.getLocalizedDateString()).isEqualTo("2017-03-23");
    }

    @Test
    public void testSetFromString() {
        assertThat(mField.setFromString("today")).isFalse();
        assertThat(mField.setFromString("2017/03/03")).isFalse();
        assertThat(mField.setFromString("")).isFalse();
    }

    @Test
    public void testClone() {
        FieldDate clone = mField.clone();
        assertThat(mField).isNotSameAs(clone);
        assertThat(clone.getName()).isEqualTo(mField.getName());
        assertThat(mField.getDate()).isNotSameAs(clone.getDate());
        assertThat(clone.getDate()).isEqualTo(mField.getDate());
    }

    @Test
    public void testObserverEvents() {
        FieldTestHelper.testObserverEvent(mField,
                /* New value */ "2017-03-23",
                /* Expected old value */ INITIAL_VALUE,
                /* Expected new value */ "2017-03-23");

        // No events if no change
        FieldTestHelper.testObserverNoEvent(mField);
    }
}
