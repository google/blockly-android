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
package com.google.blockly.demo;

import android.support.annotation.NonNull;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

import com.google.blockly.AbstractBlocklyActivity;
import com.google.blockly.LoggingCodeGeneratorCallback;
import com.google.blockly.utils.CodeGenerationRequest;

/**
 * Simplest implementation of AbstractBlocklyActivity.
 */
public class SimpleActivity extends AbstractBlocklyActivity {
    private static final String TAG = "SimpleActivity";

    CodeGenerationRequest.CodeGeneratorCallback mCodeGeneratorCallback =
            new LoggingCodeGeneratorCallback(this, TAG);

    @Override
    @NonNull
    protected String getWorkspaceBlocksPath() {
        return "turtle/definitions.json";
    }

    @Override
    @NonNull
    protected String getWorkspaceToolboxPath() {
        return "turtle/level_1/toolbox.xml";
    }

    @Override
    @NonNull
    protected String getGeneratorJsFilename() {
        return "turtle/generators.js";
    }

    @Override
    @NonNull
    protected String getBlockDefinitionsFilename() {
        return "turtle/definitions.json";
    }

    @NonNull
    @Override
    protected CodeGenerationRequest.CodeGeneratorCallback getCreateCodeGenerationCallback() {
        // Uses the same callback for every generation call.
        return mCodeGeneratorCallback;
    }
}