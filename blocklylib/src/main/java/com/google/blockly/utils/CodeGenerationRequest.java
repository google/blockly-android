/*
 *  Copyright  2015 Google Inc. All Rights Reserved.
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

package com.google.blockly.utils;

import com.google.blockly.CodeGeneratorService;

/**
 * Container for the information needed to generate code through the {@link CodeGeneratorService}.
 */
public class CodeGenerationRequest {
    private final CodeGeneratorCallback mCallback;
    private final String mBlocklyXml;
    private final String mBlockDefinitionsFilename;

    /**
     * Constructor for a code generation request.
     *
     * @param xml The xml of a full workspace for which code should be generated.
     * @param callback A callback specifying what to do with the generated code.
     * @param blockDefinitionsFilename The path of the js file containing block definitions and
     * generators, relative to file:///android_assets/background_compiler.html.
     */
    public CodeGenerationRequest(String xml, CodeGeneratorCallback callback,
            String blockDefinitionsFilename) {
        if (xml == null || xml.isEmpty()) {
            throw new IllegalArgumentException("The blockly workspace string must not be empty " +
                    "or null.");
        }
        mCallback = callback;
        mBlocklyXml = xml;
        mBlockDefinitionsFilename = blockDefinitionsFilename;
    }

    public CodeGeneratorCallback getCallback() {
        return mCallback;
    }

    public String getXml() {
        return mBlocklyXml;
    }

    public String getBlockDefinitionsFilename() {
        return mBlockDefinitionsFilename;
    }

    public interface CodeGeneratorCallback {
        /**
         * Called when finished generating code.
         *
         * @param generatedCode The string containing all of the generated code.
         */
        void onFinishCodeGeneration(String generatedCode);
    }

}
