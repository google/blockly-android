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
 * Tests for {@link FieldLabel}.
 */
public class FieldLabelTest extends AndroidTestCase {
    public static final String FIELD_NAME = "mField name";
    public static final String INITIAL_VALUE = "some text";
    FieldLabel mField;

    public void setUp() {
        mField = new FieldLabel(FIELD_NAME, INITIAL_VALUE);
    }

    public void testConstructor() {
        assertEquals(Field.TYPE_LABEL, mField.getType());
        assertEquals(FIELD_NAME, mField.getName());
        assertEquals(INITIAL_VALUE, mField.getText());
    }

    public void testClone() {
        FieldLabel clone = mField.clone();
        assertNotSame(mField, clone);
        assertEquals(mField.getName(), clone.getName());
        assertEquals(mField.getText(), clone.getText());
    }
}
