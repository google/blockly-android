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
 * Tests for {@link FieldCheckbox}.
 */
public class FieldCheckboxTest {
    @Test
    public void testFieldCheckbox() {
        FieldCheckbox field = new FieldCheckbox("checkbox", true);
        assertThat(field.getType()).isEqualTo(Field.TYPE_CHECKBOX);
        assertThat(field.getName()).isEqualTo("checkbox");
        assertThat(field.isChecked()).isEqualTo(true);
        field.setChecked(false);
        assertThat(field.isChecked()).isEqualTo(false);

        field = new FieldCheckbox("fname", false);
        assertThat(field.isChecked()).isEqualTo(false);
        field.setChecked(true);
        assertThat(field.isChecked()).isEqualTo(true);

        assertThat(field.setFromString("false")).isTrue();
        assertThat(field.isChecked()).isFalse();

        assertThat(field.setFromString("true")).isTrue();
        assertThat(field.setFromString("TRUE")).isTrue();
        assertThat(field.setFromString("True")).isTrue();
        assertThat(field.isChecked()).isTrue();

        // xml parsing
        // Boolean.parseBoolean checks the lowercased value against "true" and returns false
        // otherwise.
        assertThat(field.setFromString("This is not a boolean")).isTrue();
        assertThat(field.isChecked()).isFalse();
        field.setChecked(true);
        assertThat(field.setFromString("t")).isTrue();
        assertThat(field.isChecked()).isFalse();
    }

    @Test
    public void testClone() {
        FieldCheckbox field = new FieldCheckbox("checkbox", true);
        FieldCheckbox clone = field.clone();

        assertThat(field).isNotSameAs(clone);
        assertThat(field.getName()).isEqualTo(field.getName());
        assertThat(clone.isChecked()).isEqualTo(field.isChecked());

        // Test with false
        field = new FieldCheckbox("checkbox", false);
        clone = field.clone();
        assertThat(clone.isChecked()).isEqualTo(field.isChecked());
    }

    @Test
    public void testObserverEvent() {
        FieldTestHelper.testObserverEvent(new FieldCheckbox("CHECKBOX", true),
                /* New value to assign */ "FALSE",
                /* oldValue */ "TRUE",
                /* newValue */ "FALSE");
        FieldTestHelper.testObserverEvent(new FieldCheckbox("CHECKBOX", false),
                /* New value to assign */ "TRUE",
                /* oldValue */ "FALSE",
                /* newValue */ "TRUE");

        // No change assignments
        FieldTestHelper.testObserverNoEvent(new FieldCheckbox("CHECKBOX", true));
        FieldTestHelper.testObserverNoEvent(new FieldCheckbox("CHECKBOX", false));
        FieldTestHelper.testObserverNoEvent(new FieldCheckbox("CHECKBOX", true), "TrUe");
        FieldTestHelper.testObserverNoEvent(new FieldCheckbox("CHECKBOX", false), "ugly false");
    }
}
