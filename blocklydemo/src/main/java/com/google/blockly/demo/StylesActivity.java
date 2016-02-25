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

import com.google.blockly.AbstractBlocklyActivity;
import com.google.blockly.LoggingCodeGeneratorCallback;
import com.google.blockly.utils.CodeGenerationRequest;

/**
 * Basic implementation of AbstractBlocklyActivity that demonstrates applying styles. This demo
 * uses an Activity style defined in the manifest XML. Alternatively, Activities can override
 * {@link #getStyleResId()} to change the style used in {@link #onCreateController()}.
 */
public class StylesActivity extends AbstractBlocklyActivity {
    private static final String TAG = "StylesActivity";

    CodeGenerationRequest.CodeGeneratorCallback mCodeGeneratorCallback =
            new LoggingCodeGeneratorCallback(this, TAG);

    @NonNull
    @Override
    protected String getBlockDefinitionsJsonPath() {
        return "turtle/definitions.json";
    }

    @NonNull
    @Override
    protected String getToolboxContentsXmlPath() {
        return "turtle/toolbox_all.xml";
    }

    @NonNull
    @Override
    protected String getGeneratorJsPath() {
        return "turtle/generators.js";
    }

    @NonNull
    @Override
    protected CodeGenerationRequest.CodeGeneratorCallback getCodeGenerationCallback() {
        // Uses the same callback for every generation call.
        return mCodeGeneratorCallback;
    }
}