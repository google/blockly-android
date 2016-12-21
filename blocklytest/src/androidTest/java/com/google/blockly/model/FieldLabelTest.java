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

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests for {@link FieldLabel}.
 */
public class FieldLabelTest {
    public static final String FIELD_NAME = "mField name";
    public static final String INITIAL_VALUE = "some text";
    FieldLabel mField;

    @Before
    public void setUp() {
        mField = new FieldLabel(FIELD_NAME, INITIAL_VALUE);
    }

    @Test
    public void testConstructor() {
        assertThat(mField.getType()).isEqualTo(Field.TYPE_LABEL);
        assertThat(mField.getName()).isEqualTo(FIELD_NAME);
        assertThat(mField.getText()).isEqualTo(INITIAL_VALUE);
    }

    @Test
    public void testClone() {
        FieldLabel clone = mField.clone();
        assertThat(mField).isNotSameAs(clone);
        assertThat(clone.getName()).isEqualTo(mField.getName());
        assertThat(clone.getText()).isEqualTo(mField.getText());
    }
}
