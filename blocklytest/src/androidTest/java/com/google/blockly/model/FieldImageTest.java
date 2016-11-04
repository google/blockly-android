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
    static final String FIELD_NAME = "whatever";
    static final String SOURCE = "https://www.gstatic.com/codesite/ph/images/star_on.gif";
    static final int WIDTH = 15;
    static final int HEIGHT = 21;
    static final String ALT_TEXT = "altText";

    FieldImage mField;

    public void setUp() {
        mField = new FieldImage(FIELD_NAME, SOURCE, WIDTH, HEIGHT, ALT_TEXT);
    }

    public void testFieldImage() {
        assertEquals(Field.TYPE_IMAGE, mField.getType());
        assertEquals(FIELD_NAME, mField.getName());
        assertEquals(SOURCE, mField.getSource());
        assertEquals(WIDTH, mField.getWidth());
        assertEquals(HEIGHT, mField.getHeight());
        assertEquals(ALT_TEXT, mField.getAltText());
    }

    public void testClone() {
        FieldImage clone = mField.clone();
        assertNotSame(mField, clone);
        assertEquals(mField.getName(), clone.getName());
        assertEquals(mField.getSource(), clone.getSource());
        assertEquals(mField.getWidth(), clone.getWidth());
        assertEquals(mField.getHeight(), clone.getHeight());
        assertEquals(mField.getAltText(), clone.getAltText());
    }

    public void testObserverEvents() {
        final int[] eventCount = {0};
        mField.registerObserver(new Field.Observer() {
            @Override
            public void onValueChanged(Field field, String oldValue, String newValue) {
                assertSame(mField, field);
                eventCount[0] += 1;
            }
        });

        // Same source, new metadata
        mField.setImage(SOURCE, 101, 202);
        assertEquals(1, eventCount[0]);

        // New source
        mField.setImage(SOURCE + "2", 101, 202);
        assertEquals(2, eventCount[0]);
    }
}
