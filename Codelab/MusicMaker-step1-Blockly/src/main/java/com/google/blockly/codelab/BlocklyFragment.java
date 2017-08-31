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

package com.google.blockly.codelab;

import android.support.annotation.NonNull;

import com.google.blockly.android.AbstractBlocklyFragment;
import com.google.blockly.android.codegen.CodeGenerationRequest;
import com.google.blockly.android.codegen.LoggingCodeGeneratorCallback;
import com.google.blockly.model.DefaultBlocks;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A Blockly workspace and toolbox to edit a sound script.
 */
public class BlocklyFragment extends AbstractBlocklyFragment {
    private static final String TAG = "BlocklyFragment";

    private static final List<String> BLOCK_DEFINITIONS =
            Collections.unmodifiableList(Arrays.asList(
                    DefaultBlocks.LOOP_BLOCKS_PATH,  // For "controls_repeat_ext"
                    DefaultBlocks.MATH_BLOCKS_PATH,  // For "math_number"
                    "sound_blocks.json"
            ));

    private static final List<String> JAVASCRIPT_GENERATORS =
            Collections.singletonList("sound_block_generators.js");

    CodeGenerationRequest.CodeGeneratorCallback mCodeGeneratorCallback = null;

    @NonNull
    @Override
    protected String getToolboxContentsXmlPath() {
        return "toolbox.xml";
    }

    @NonNull
    @Override
    protected List<String> getBlockDefinitionsJsonPaths() {
        return BLOCK_DEFINITIONS;
    }

    @NonNull
    @Override
    protected List<String> getGeneratorsJsPaths() {
        return JAVASCRIPT_GENERATORS;
    }

    @NonNull
    @Override
    protected CodeGenerationRequest.CodeGeneratorCallback getCodeGenerationCallback() {
        if (mCodeGeneratorCallback == null) {
            // Late initialization since Context is not available at construction time.
            // TODO: Replace the logging generator with actual sound playback.
            mCodeGeneratorCallback = new LoggingCodeGeneratorCallback(getContext(), TAG);
        }
        return mCodeGeneratorCallback;
    }
}
