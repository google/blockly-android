/*
 *  Copyright 2017 Google Inc. All Rights Reserved.
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
package com.google.blockly.model;

import com.google.blockly.utils.BlockLoadingException;

/**
 * Interface for custom {@link BlocklyCategory}s, such as
 * <code>{@link VariableCustomCategory VARIABLES}</code> and
 * <code>{@link ProcedureCustomCategory PROCEDURE}</code> toolbox categories. Referenced by the
 * {@code custom} XML attribute and the {@link BlocklyCategory#CUSTOM_CATEGORIES}.
 * <p/>
 * {@link #initializeCategory(BlocklyCategory)} will be called once during load time, where the
 * implementation can configure the category as needed. This is different from the web
 * implementation of custom category generator functions that are called each time the toolbox
 * category is opened. The Android implementation allows better integration with RecycleViews
 * (notifying the Adapter of changes with individual add and remove calls) and always-open
 * toolboxes.
 */
public interface CustomCategory {
    /**
     * Called to initialize a BlocklyCategory, usually at the time the toolbox XML is loaded. This
     * method is called once in the lifetime of the category. If the category is dynamic, a
     * reference to the category should be saved for future updates.
     * @param category The category to initialize.
     * @throws BlockLoadingException
     */
    void initializeCategory(BlocklyCategory category) throws BlockLoadingException;
}
