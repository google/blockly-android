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
    public void testFieldLabel() {
        FieldLabel field = new FieldLabel("field name", "some text");
        assertEquals(Field.TYPE_LABEL, field.getType());
        assertEquals("field name", field.getName());
        assertEquals("some text", field.getText());

        field = new FieldLabel("name", null);
        assertEquals("name", field.getName());
        assertEquals("", field.getText());

        assertNotSame(field, field.clone());
    }
}
