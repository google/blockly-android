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
 * Tests for {@link FieldVariable}.
 */
public class FieldVariableTest {
    public static final String FIELD_NAME = "fname";
    public static final String VARIABLE_NAME = "var";

    FieldVariable mField = new FieldVariable(FIELD_NAME, VARIABLE_NAME);

    @Before
    public void setUp() {
        mField = new FieldVariable(FIELD_NAME, VARIABLE_NAME);
    }

    @Test
    public void testConstructor() {
        assertThat(mField.getType()).isEqualTo(Field.TYPE_VARIABLE);
        assertThat(mField.getName()).isEqualTo(FIELD_NAME);
        assertThat(mField.getVariable()).isEqualTo(VARIABLE_NAME);
    }

    @Test
    public void testSetVariable() {
        mField.setVariable("newVar");
        assertThat( mField.getVariable()).isEqualTo("newVar");
    }

    @Test
    public void testSetFromString() {
        assertThat(mField.setFromString("newestVar")).isTrue();
        assertThat(mField.getVariable()).isEqualTo("newestVar");
        assertThat(mField.setFromString("")).isFalse();
        assertThat(mField.getVariable()).isEqualTo("newestVar");
    }

    @Test
    public void testClone() {
        FieldVariable clone = mField.clone();
        assertThat(mField).isNotSameAs(clone);
        assertThat(clone.getName()).isEqualTo(mField.getName());
        assertThat(clone.getVariable()).isEqualTo(mField.getVariable());
    }

    @Test
    public void testObserverEvents() {
        FieldTestHelper.testObserverEvent(mField,
                /* New value */ "zip",
                /* Expected old value */ VARIABLE_NAME,
                /* Expected new value */ "zip");
    }
}
