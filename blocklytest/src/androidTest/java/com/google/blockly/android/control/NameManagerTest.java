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
    private NameManager<String> mNameManager;

    @Before
     public void setUp() throws Exception {
        mNameManager = new NameManager<>();
    }

    @Test
    public void testGenerateUniqueName() throws Exception {
        String name1 = mNameManager.putUniquely("string", "string value");
        assertThat(mNameManager.generateUniqueName("string"))
                .isNotEqualTo(name1);

        assertThat(mNameManager.generateUniqueName("foo")).isEqualTo("foo");
        assertThat(mNameManager.put("foo", "foo value")).isTrue();
        assertThat(mNameManager.generateUniqueName("foo")).isEqualTo("foo2");
        assertThat(mNameManager.put("foo2", "foo2 value")).isTrue();
        assertThat(mNameManager.generateUniqueName("foo2")).isEqualTo("foo3");
        assertThat(mNameManager.generateUniqueName("222")).isEqualTo("222");
        assertThat(mNameManager.put("222", "222 value")).isTrue();
        assertThat(mNameManager.generateUniqueName("222")).isEqualTo("223");
    }

    @Test
    public void testGenerateUniqueNamePreservesCase() {
        assertThat(mNameManager.generateUniqueName("FOO")).isEqualTo("FOO");
        assertThat(mNameManager.generateUniqueName("bar")).isEqualTo("bar");

        assertThat(mNameManager.put("FOO", "FOO value")).isTrue();
        assertThat(mNameManager.put("bar", "bar")).isTrue();

        String altFoo = mNameManager.generateUniqueName("FOO");
        assertThat(altFoo).isNotEqualTo("FOO");
        assertThat(altFoo).startsWith("FOO");
        String altBar = mNameManager.generateUniqueName("bar");
        assertThat(altBar).isNotEqualTo("bar");
        assertThat(altBar).startsWith("bar");
    }

    @Test
    public void testCaseInsensitive() {
        String name1 = mNameManager.putUniquely("string", "value1");
        String name2 = mNameManager.putUniquely("String", "value2");
        assertThat(name2).isNotEqualTo(name1);
        assertThat(name2.toLowerCase()).isNotEqualTo(name1.toLowerCase());
    }

    @Test
    public void testListFunctions() {
        assertThat(mNameManager.put("foo", "foo value")).isTrue();
        assertThat(mNameManager.getUsedNames().size()).isEqualTo(1);

        mNameManager.putUniquely("bar", "bar value");
        assertThat(mNameManager.getUsedNames().size()).isEqualTo(2);

        assertThat(mNameManager.put("bar", "another bar value")).isFalse();
        assertThat(mNameManager.getUsedNames().size()).isEqualTo(2);

        mNameManager.clear();
        assertThat(mNameManager.getUsedNames().isEmpty()).isTrue();
    }

    @Test
    public void testGenerateVariableName() {
        //  TODO: Replace this with the implementation inside WorkspaceStats.
        VariableNameManager nameManager = new VariableNameManagerTestImpl();

        assertThat(nameManager.generateVariableName()).isEqualTo("i");
        assertThat(nameManager.generateVariableName()).isEqualTo("i");  // Repeat b/c wasn't added
        assertThat(nameManager.addVariable("i", false)).isEqualTo("i");
        assertThat(nameManager.generateVariableName()).isEqualTo("j");
        assertThat(nameManager.addVariable("j", false)).isEqualTo("j");
        assertThat(nameManager.generateVariableName()).isEqualTo("k");
        assertThat(nameManager.addVariable("k", false)).isEqualTo("k");
        // Skip L
        assertThat(nameManager.generateVariableName()).isEqualTo("m");
        assertThat(nameManager.addVariable("m", false)).isEqualTo("m");

        for (int i = 0; i < 21; i++) {
            String var = nameManager.generateVariableName();
            assertThat(nameManager.addVariable(var, false)).isEqualTo(var);
        }

        assertThat(nameManager.generateVariableName()).isEqualTo("i2");
        assertThat(nameManager.addVariable("i2", false)).isEqualTo("i2");
        assertThat(nameManager.addVariable("j2", false)).isEqualTo("j2");
        assertThat(nameManager.generateVariableName()).isEqualTo("k2");
    }

    @Test
    public void testRemove() {
        mNameManager.put("foo", "foo value");
        assertThat(mNameManager.hasName("FOO")).isTrue();
        mNameManager.remove("Foo");
        assertThat(mNameManager.hasName("foo")).isFalse();
        // Remove something that wasn't there; expect no problems.
        mNameManager.remove("foo");
    }

}
