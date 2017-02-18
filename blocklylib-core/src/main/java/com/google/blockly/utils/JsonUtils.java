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
package com.google.blockly.utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * Static utility methods for working with JSON.
 */
public class JsonUtils {
    /**
     * Copies a JSONObject with all key/value pairs copied in a shallow manner, such that the
     * returned copy shares values with the original.
     *
     * @param original The source of copy.
     * @return A new JSONObject with a shallow copy of the original key/value pairs.
     */
    public static JSONObject shallowCopy(JSONObject original) {
        String[] keys = new String[original.length()];
        Iterator<String> keyIter = original.keys();
        for(int i = 0; keyIter.hasNext(); ++i) {
            keys[i] = keyIter.next();
        }
        try {
            return new JSONObject(original, keys);
        } catch (JSONException e) {
            throw new IllegalStateException("Unexpected JSON copy error.", e);
        }
    }

    // Private constructor prevents instances of this class.
    private JsonUtils() {}
}
