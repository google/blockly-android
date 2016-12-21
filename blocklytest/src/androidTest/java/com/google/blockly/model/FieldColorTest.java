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
 * Tests for {@link FieldColor}.
 */
public class FieldColorTest {
    private static final int INITIAL_COLOR = 0xaabbcc;
    private static final String INITIAL_COLOR_STRING = "#aabbcc";

    FieldColor mField;

    @Before
    public void setUp() {
        mField = new FieldColor("fname", INITIAL_COLOR);
    }

    @Test
    public void testConstructors() {
        assertThat(mField.getType()).isEqualTo(Field.TYPE_COLOR);
        assertThat(mField.getName()).isEqualTo("fname");
        assertThat(mField.getColor()).isEqualTo(INITIAL_COLOR);

        mField = new FieldColor("fname");
        assertThat(mField.getName()).isEqualTo("fname");
        assertThat(mField.getColor()).isEqualTo(FieldColor.DEFAULT_COLOR);
    }

    @Test
    public void testSetColor() {
        mField.setColor(0xb0bb1e);
        assertThat(mField.getColor()).isEqualTo(0xb0bb1e);
    }

    @Test
    public void testSetFromString() {
        assertThat(mField.setFromString("#ffcc66")).isTrue();
        assertThat(mField.getColor()).isEqualTo(0xffcc66);
        assertThat(mField.setFromString("#00cc66")).isTrue();
        assertThat(mField.getColor()).isEqualTo(0x00cc66);

        // Ignore alpha channel
        assertThat(mField.setFromString("#1000cc66")).isTrue();
        assertThat(mField.getColor()).isEqualTo(0x00cc66);

        // Invalid color strings. Value should not change.
        assertThat(mField.setFromString("This is not a color")).isFalse();
        assertThat(mField.getColor()).isEqualTo(0x00cc66);
        assertThat(mField.setFromString("#fc6")).isFalse();
        assertThat(mField.getColor()).isEqualTo(0x00cc66);
    }

    @Test
    public void testClone() {
        FieldColor clone = mField.clone();
        assertThat(mField).isNotSameAs(clone);
        assertThat(clone.getName()).isEqualTo(mField.getName());
        assertThat(clone.getColor()).isEqualTo(mField.getColor());
    }

    @Test
    public void testObserverEvents() {
        FieldTestHelper.testObserverEvent(new FieldColor("normal", INITIAL_COLOR),
                /* New value */ "#ffcc66",
                /* Expected old value */ INITIAL_COLOR_STRING,
                /* Expected new value */ "#ffcc66");

        // No Change
        FieldTestHelper.testObserverNoEvent(
                new FieldColor("normal", INITIAL_COLOR),
                /* New value */ INITIAL_COLOR_STRING);
        FieldTestHelper.testObserverNoEvent(
                new FieldColor("normal", INITIAL_COLOR),
                /* New value */ INITIAL_COLOR_STRING.toUpperCase());
    }
}
