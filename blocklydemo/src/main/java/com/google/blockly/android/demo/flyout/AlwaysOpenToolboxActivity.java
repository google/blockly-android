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
package com.google.blockly.android.demo.flyout;

import android.support.annotation.NonNull;
import android.view.View;

import com.google.blockly.android.AbstractBlocklyActivity;
import com.google.blockly.android.codegen.CodeGenerationRequest;
import com.google.blockly.android.codegen.LoggingCodeGeneratorCallback;
import com.google.blockly.android.demo.R;

import java.util.Arrays;
import java.util.List;

/**
 * Demonstrates how the layout for the flyouts can be dramatically changed by overriding
 * {@link #onCreateContentView(int)} with a custom layout file.
 *
 * TODO: Add menu to switch between different layouts.
 */
public class AlwaysOpenToolboxActivity extends AbstractBlocklyActivity {
    private static final String TAG = "AlwaysOpenToolbox";

    private static final String SAVE_FILENAME = "always_open_workspace.xml";
    private static final String AUTOSAVE_FILENAME = "always_open_workspace_temp.xml";

    private static final List<String> BLOCK_DEFINITIONS = Arrays.asList(
            "default/logic_blocks.json",
            "default/loop_blocks.json",
            "default/math_blocks.json",
            "default/text_blocks.json",
            "default/list_blocks.json",
            "default/colour_blocks.json",
            "default/variable_blocks.json"
    );
    private static final List<String> JAVASCRIPT_GENERATORS = Arrays.asList(
        // Custom block generators go here. Default blocks are already included.
        // TODO(#99): Include Javascript defaults when other languages are supported.
    );

    CodeGenerationRequest.CodeGeneratorCallback mCodeGeneratorCallback =
            new LoggingCodeGeneratorCallback(this, TAG);

    @NonNull
    @Override
    protected List<String> getBlockDefinitionsJsonPaths() {
        return BLOCK_DEFINITIONS;
    }

    @NonNull
    @Override
    protected String getToolboxContentsXmlPath() {
        return "default/toolbox.xml";
    }

    @NonNull
    @Override
    protected List<String> getGeneratorsJsPaths() {
        return JAVASCRIPT_GENERATORS;
    }

    @NonNull
    @Override
    protected CodeGenerationRequest.CodeGeneratorCallback getCodeGenerationCallback() {
        // Uses the same callback for every generation call.
        return mCodeGeneratorCallback;
    }

    @Override
    protected void onInitBlankWorkspace() {
        // Initialize variable names.
        // TODO: (#22) Remove this override when variables are supported properly
        getController().addVariable("item");
        getController().addVariable("leo");
        getController().addVariable("don");
        getController().addVariable("mike");
        getController().addVariable("raf");
    }


    @Override
    protected View onCreateContentView(int parentId) {
        View root = getLayoutInflater().inflate(R.layout.styled_content, null);
        return root;
    }

    @Override
    @NonNull
    protected String getWorkspaceSavePath() {
        return SAVE_FILENAME;
    }

    @Override
    @NonNull
    protected String getWorkspaceAutosavePath() {
        return AUTOSAVE_FILENAME;
    }
}
