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

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests for {@link FieldAngle}.
 */
public class FieldAngleTest {
    @Test
    public void testConstructor() {
        FieldAngle field = new FieldAngle("name", 0);
        assertThat(field.getType()).isEqualTo(Field.TYPE_ANGLE);
        assertThat(field.getName()).isEqualTo("name");
        assertThat(field.getAngle()).isEqualTo(0f);
    }

    @Test
    public void testWrapAround() {
        FieldAngle field = new FieldAngle("name", 360);
        assertThat(field.getAngle()).isEqualTo(0f);

        field.setAngle(720f);
        assertThat(field.getAngle()).isEqualTo(0f);

        field.setAngle(-180f);
        assertThat(field.getAngle()).isEqualTo(180f);

        field.setAngle(10000f);
        assertThat(field.getAngle()).isEqualTo(280f);

        field.setAngle(-10000f);
        assertThat(field.getAngle()).isEqualTo(80f);

        field.setAngle(27f);
        assertThat(field.getAngle()).isEqualTo(27f);

        field.setAngle(-10001f);
        assertThat(field.getAngle()).isEqualTo(79f);
    }

    @Test
    public void testParseValue() {
        FieldAngle field = new FieldAngle("name", 0);

        // xml parsing
        assertThat(field.setFromString("-180")).isTrue();
        assertThat(field.getAngle()).isEqualTo(180f);
        assertThat(field.setFromString("27")).isTrue();
        assertThat(field.getAngle()).isEqualTo(27f);
        assertThat(field.setFromString("this is not a number")).isFalse();
    }

    @Test
    public void testClone() {
        FieldAngle field = new FieldAngle("name", 5);
        FieldAngle clone = field.clone();

        assertThat(field).isNotSameAs(field.clone());
        assertThat(clone.getName()).isEqualTo(field.getName());
        assertThat(clone.getAngle()).isEqualTo(field.getAngle());
    }

    @Test
    public void testObserverEvent() {
        FieldTestHelper.testObserverEvent(new FieldAngle("ANGLE", 0),
                /* New value to assign */ "15",
                /* oldValue */ "0",
                /* newValue */ "15");
        FieldTestHelper.testObserverNoEvent(new FieldAngle("ANGLE", 0));
        FieldTestHelper.testObserverNoEvent(new FieldAngle("ANGLE", 0), "360");
    }
}
