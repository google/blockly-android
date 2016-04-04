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
 * Tests for {@link FieldInput}.
 */
public class FieldInputTest extends AndroidTestCase {
    public void testFieldInput() {
        FieldInput field = new FieldInput("field name", "start text");
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
}
