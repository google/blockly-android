/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

package com.google.blockly.util;

/**
 * Helper functions for dealing with Javascript.
 */
public class JavascriptUtil {
    /**
     * Creates a double quoted Javascript string, escaping backslashes, single quotes, double
     * quotes, and newlines.
     */
    public static String makeJsString(String str) {
        // TODO(#17): More complete character escaping.
        String escapedStr = str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\'", "\\\'")
                .replace("\n", "\\n");
        return "\"" + escapedStr + "\"";
    }

    private JavascriptUtil() {} // Do not instantiate.
}
