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

import android.test.AndroidTestCase;

import static android.test.MoreAsserts.assertNotEqual;

/**
 * Tests for {@link NameManager}.
 */
public class NameManagerTest extends AndroidTestCase {
    private NameManager mNameManager;

    @Override
    public void setUp() throws Exception {
        mNameManager = new NameManager.ProcedureNameManager();
    }

    public void testGenerateUniqueName() throws Exception {
        String name1 = mNameManager.generateUniqueName("string", true /* addName */);
        assertNotEqual(name1, mNameManager.generateUniqueName("string", true /* addName */));

        assertEquals("foo", mNameManager.generateUniqueName("foo", true /* addName */));
        assertEquals("foo2", mNameManager.generateUniqueName("foo", true /* addName */));
        assertEquals("foo3", mNameManager.generateUniqueName("foo2", true /* addName */));
        assertEquals("222", mNameManager.generateUniqueName("222", true /* addName */));
        assertEquals("223", mNameManager.generateUniqueName("222", true /* addName */));
    }

    public void testCaseInsensitive() {
        String name1 = mNameManager.generateUniqueName("string", true /* addName */);
        String name2 = mNameManager.generateUniqueName("String", true /* addName */);
        assertNotEqual(name1, name2);
        assertNotEqual(name1.toLowerCase(), name2.toLowerCase());
    }

    public void testListFunctions() {
        mNameManager.addName("foo");
        assertEquals(1, mNameManager.getUsedNames().size());
        mNameManager.generateUniqueName("bar", true /* addName */);
        assertEquals(2, mNameManager.getUsedNames().size());

        mNameManager.generateUniqueName("bar", false /* addName */);
        assertEquals(2, mNameManager.getUsedNames().size());

        mNameManager.clearUsedNames();
        assertTrue(mNameManager.getUsedNames().isEmpty());
    }

    public void testGenerateVariableName() {
        NameManager.VariableNameManager nameManager = new NameManager.VariableNameManager();
        assertEquals("i", nameManager.generateVariableName(false /* addName */));
        assertEquals("i", nameManager.generateVariableName(true /* addName */));
        assertEquals("j", nameManager.generateVariableName(true /* addName */));
        assertEquals("k", nameManager.generateVariableName(true /* addName */));
        assertEquals("m", nameManager.generateVariableName(true /* addName */));

        for (int i = 0; i < 21; i++) {
            nameManager.generateVariableName(true /* addName */);
        }

        assertEquals("i2", nameManager.generateVariableName(true /* addName */));

        nameManager.addName("j2");
        assertEquals("k2", nameManager.generateVariableName(true /* addName */));
    }

    public void testRemove() {
        mNameManager.addName("foo");
        assertTrue(mNameManager.contains("FOO"));
        mNameManager.remove("Foo");
        assertFalse(mNameManager.contains("foo"));
        // Remove something that wasn't there; expect no problems.
        mNameManager.remove("foo");
    }
}
