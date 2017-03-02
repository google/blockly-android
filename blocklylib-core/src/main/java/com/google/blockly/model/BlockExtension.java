/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.blockly.model;

import com.google.blockly.utils.BlockLoadingException;

import java.util.Collections;
import java.util.Map;

/**
 * A BlockExtension allows programmatic configuration of blocks, extra initialization, or custom
 * behaviors to be added to blocks. They are also the preferred mechanism to create and set
 * {@link Mutator}s on blocks.
 * <p/>
 * BlockExtensions must be registered using {@link BlockFactory#registerExtension}. Block
 * definitions can refer to the extension using the {@code "extension"} JSON attribute, an array of
 * extension names. When a block is constructed, {@link #applyTo(Block)} will be called on the new
 * block.
 * <p/>
 * See <a href="https://developers.google.com/blockly/guides/create-custom-blocks/mutators">guide on
 * extensions and mutators</a>.
 */
public interface BlockExtension {
    Map<String, BlockExtension> STANDARD_EXTENSIONS = Collections.EMPTY_MAP;  // TODO

    /**
     * Applies the extension to the provided block.
     * @param block The block to update
     * @throws BlockLoadingException
     */
    void applyTo(Block block) throws BlockLoadingException;
}
