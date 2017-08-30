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
package com.google.blockly.android;

import android.support.annotation.NonNull;
import android.view.ContextThemeWrapper;

import com.google.blockly.android.codegen.CodeGenerationRequest;
import com.google.blockly.android.codegen.LanguageDefinition;
import com.google.blockly.android.codegen.LoggingCodeGeneratorCallback;
import com.google.blockly.android.ui.BlockViewFactory;
import com.google.blockly.android.ui.WorkspaceHelper;
import com.google.blockly.android.ui.vertical.VerticalBlockViewFactory;
import com.google.blockly.android.ui.vertical.R;
import com.google.blockly.model.DefaultBlocks;

import java.util.Arrays;
import java.util.List;

/**
 * Simplest implementation of AbstractBlocklyActivity. Does not load a workspace at start.
 */
public class BlocklyTestActivity extends AbstractBlocklyActivity {
    private static final String TAG = "BlocklyTestActivity";

    private static final List<String> BLOCK_DEFINITIONS = DefaultBlocks.getAllBlockDefinitions();

    private static final List<String> BLOCK_GENERATORS = Arrays.asList(new String[] {
            "fake/generator.js"
    });

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
    protected LanguageDefinition getBlockGeneratorLanguage() {
        return DefaultBlocks.LANGUAGE_DEFINITION;
    }

    @NonNull
    @Override
    protected List<String> getGeneratorsJsPaths() {
        return BLOCK_GENERATORS; // Never executed in tests.
    }

    @Override
    public BlocklyActivityHelper onCreateActivityHelper() {
        return new BlocklyActivityHelper(this, this.getSupportFragmentManager()) {
            @Override
            public BlockViewFactory onCreateBlockViewFactory(WorkspaceHelper workspaceHelper) {
                return new VerticalBlockViewFactory(
                        new ContextThemeWrapper(
                                BlocklyTestActivity.this,
                                R.style.BlocklyVerticalTheme),
                        workspaceHelper);
            }
        };
    }

    @NonNull
    @Override
    protected CodeGenerationRequest.CodeGeneratorCallback getCodeGenerationCallback() {
        // Uses the same callback for every generation call.
        return mCodeGeneratorCallback;
    }
}
