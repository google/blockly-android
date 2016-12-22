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
 * Tests for {@link FieldImage}.
 */
public class FieldImageTest {
    static final String FIELD_NAME = "whatever";
    static final String SOURCE = "https://www.gstatic.com/codesite/ph/images/star_on.gif";
    static final int WIDTH = 15;
    static final int HEIGHT = 21;
    static final String ALT_TEXT = "altText";

    FieldImage mField;

    @Before
    public void setUp() {
        mField = new FieldImage(FIELD_NAME, SOURCE, WIDTH, HEIGHT, ALT_TEXT);
    }

    @Test
    public void testFieldImage() {
        assertThat(mField.getType()).isEqualTo(Field.TYPE_IMAGE);
        assertThat(mField.getName()).isEqualTo(FIELD_NAME);
        assertThat(mField.getSource()).isEqualTo(SOURCE);
        assertThat(mField.getWidth()).isEqualTo(WIDTH);
        assertThat(mField.getHeight()).isEqualTo(HEIGHT);
        assertThat(mField.getAltText()).isEqualTo(ALT_TEXT);
    }

    @Test
    public void testClone() {
        FieldImage clone = mField.clone();
        assertThat(mField).isNotSameAs(clone);
        assertThat(clone.getName()).isEqualTo(mField.getName());
        assertThat(clone.getSource()).isEqualTo(mField.getSource());
        assertThat(clone.getWidth()).isEqualTo(mField.getWidth());
        assertThat(clone.getHeight()).isEqualTo(mField.getHeight());
        assertThat(clone.getAltText()).isEqualTo(mField.getAltText());
    }

    @Test
    public void testObserverEvents() {
        final int[] eventCount = {0};
        mField.registerObserver(new Field.Observer() {
            @Override
            public void onValueChanged(Field field, String oldValue, String newValue) {
                assertThat(mField).isSameAs(field);
                eventCount[0] += 1;
            }
        });

        // Same source, new metadata
        mField.setImage(SOURCE, 101, 202);
        assertThat(eventCount[0]).isEqualTo(1);

        // New source
        mField.setImage(SOURCE + "2", 101, 202);
        assertThat(eventCount[0]).isEqualTo(2);
    }
}
