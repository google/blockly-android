/*
 *  Copyright 2017 Google Inc. All Rights Reserved.
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

import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.utils.BlockLoadingException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;

/**
 * Tests for {@link BlockExtension} and related classes.
 */
public class BlockExtensionTest {
    private static final String BLOCK_TYPE = "block type";
    private static final String EXTENSION_ID = "mock extension";

    BlocklyController mMockController;
    BlockFactory mBlockFactory;
    BlockExtension mMockExtension;

    @Before
    public void setUp() throws BlockLoadingException, IOException {
        mMockController = Mockito.mock(BlocklyController.class);

        mBlockFactory = new BlockFactory();
        mBlockFactory.setController(mMockController);
        mBlockFactory.addJsonDefinitions(
                "[{\"type\": \"" + BLOCK_TYPE + "\","
                + "\"extensions\": [\"" + EXTENSION_ID + "\"]}]"
        );

        mMockExtension = Mockito.mock(BlockExtension.class);
        mBlockFactory.registerExtension(EXTENSION_ID, mMockExtension);
    }

    @Test
    public void testExtensionApplied() throws BlockLoadingException {
        Block block = mBlockFactory.obtainBlockFrom(new BlockTemplate().ofType(BLOCK_TYPE));
        Mockito.verify(mMockExtension).applyTo(block);

        Block blockCopy = mBlockFactory.obtainBlockFrom(new BlockTemplate().copyOf(block));
        Mockito.verify(mMockExtension).applyTo(blockCopy);
    }
}
