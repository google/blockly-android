/*
 *  Copyright 2015 Google Inc. All Rights Reserved.
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

package com.google.blockly.android.control;

import org.junit.Before;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests for {@link NameManager}.
 */
public class NameManagerTest {
    private NameManager mNameManager;

    @Before
     public void setUp() throws Exception {
        mNameManager = new NameManager.ProcedureNameManager();
    }

    @Test
    public void testGenerateUniqueName() throws Exception {
        String name1 = mNameManager.generateUniqueName("string", true /* addName */);
        assertThat(mNameManager.generateUniqueName("string", true /* addName */))
                .isNotEqualTo(name1);

        assertThat(mNameManager.generateUniqueName("foo", true /* addName */)).isEqualTo("foo");
        assertThat(mNameManager.generateUniqueName("foo", true /* addName */)).isEqualTo("foo2");
        assertThat(mNameManager.generateUniqueName("foo2", true /* addName */)).isEqualTo("foo3");
        assertThat(mNameManager.generateUniqueName("222", true /* addName */)).isEqualTo("222");
        assertThat(mNameManager.generateUniqueName("222", true /* addName */)).isEqualTo("223");
    }

    @Test
    public void testGenerateUniqueNameCaseInsensitive() {
        assertThat(mNameManager.generateUniqueName("FOO", true /* addName */)).isEqualTo("FOO");
    }

    @Test
    public void testCaseInsensitive() {
        String name1 = mNameManager.generateUniqueName("string", true /* addName */);
        String name2 = mNameManager.generateUniqueName("String", true /* addName */);
        assertThat(name2).isNotEqualTo(name1);
        assertThat(name2.toLowerCase()).isNotEqualTo(name1.toLowerCase());
    }

    @Test
    public void testListFunctions() {
        mNameManager.addName("foo");
        assertThat(mNameManager.getUsedNames().size()).isEqualTo(1);
        mNameManager.generateUniqueName("bar", true /* addName */);
        assertThat(mNameManager.getUsedNames().size()).isEqualTo(2);

        mNameManager.generateUniqueName("bar", false /* addName */);
        assertThat(mNameManager.getUsedNames().size()).isEqualTo(2);

        mNameManager.clear();
        assertThat(mNameManager.getUsedNames().isEmpty()).isTrue();
    }

    @Test
    public void testGenerateVariableName() {
        NameManager.VariableNameManager nameManager = new NameManager.VariableNameManager();
        assertThat(nameManager.generateVariableName(false /* addName */)).isEqualTo("i");
        assertThat(nameManager.generateVariableName(true /* addName */)).isEqualTo("i");
        assertThat(nameManager.generateVariableName(true /* addName */)).isEqualTo("j");
        assertThat(nameManager.generateVariableName(true /* addName */)).isEqualTo("k");
        assertThat(nameManager.generateVariableName(true /* addName */)).isEqualTo("m");

        for (int i = 0; i < 21; i++) {
            nameManager.generateVariableName(true /* addName */);
        }

        assertThat(nameManager.generateVariableName(true /* addName */)).isEqualTo("i2");

        nameManager.addName("j2");
        assertThat(nameManager.generateVariableName(true /* addName */)).isEqualTo("k2");
    }

    @Test
    public void testRemove() {
        mNameManager.addName("foo");
        assertThat(mNameManager.contains("FOO")).isTrue();
        mNameManager.remove("Foo");
        assertThat(mNameManager.contains("foo")).isFalse();
        // Remove something that wasn't there; expect no problems.
        mNameManager.remove("foo");
    }
}
