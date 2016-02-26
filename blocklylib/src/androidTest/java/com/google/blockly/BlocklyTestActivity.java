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
package com.google.blockly;

import android.support.annotation.NonNull;

import com.google.blockly.utils.CodeGenerationRequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Simplest implementation of AbstractBlocklyActivity. Does not load a workspace at start.
 */
public class BlocklyTestActivity extends AbstractBlocklyActivity {
    private static final String TAG = "BlocklyTestActivity";

    private static final List<String> BLOCK_DEFINITIONS = Collections.unmodifiableList(
            Arrays.asList(new String[]{
                    "default/loop_blocks.json",
                    "default/math_blocks.json",
                    "default/variable_blocks.json",
                    "default/test_blocks.json"
            }));

    CodeGenerationRequest.CodeGeneratorCallback mCodeGeneratorCallback =
            new LoggingCodeGeneratorCallback(this, TAG);

    @NonNull
    @Override
    protected String getToolboxContentsXmlPath() {
        return "default/toolbox.xml";
    }

    @NonNull
    @Override
    protected List<String> getBlockDefinitionsJsonPaths() {
        return BLOCK_DEFINITIONS;
    }

    @NonNull
    @Override
    protected String getGeneratorJsPath() {
        return "fake/generator.js";  // Never executed in tests.
    }

    @NonNull
    @Override
    protected CodeGenerationRequest.CodeGeneratorCallback getCodeGenerationCallback() {
        // Uses the same callback for every generation call.
        return mCodeGeneratorCallback;
    }
}
