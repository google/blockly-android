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
 * Tests for {@link FieldImage}.
 */
public class FieldImageTest extends AndroidTestCase {
    public void testFieldImage() {
        String url = "https://www.gstatic.com/codesite/ph/images/star_on.gif";
        FieldImage field = new FieldImage("fname", url, 15, 21, "altText");
        assertEquals(Field.TYPE_IMAGE, field.getType());
        assertEquals("fname", field.getName());
        assertEquals(url, field.getSource());
        assertEquals(15, field.getWidth());
        assertEquals(21, field.getHeight());
        assertEquals("altText", field.getAltText());

        assertNotSame(field, field.clone());
    }
}
