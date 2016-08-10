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

package com.google.blockly.android.ui.fieldview;

import android.view.View;


/**
 * Views that allow the user to request changes to the list of variables should implement this
 * interface. The {@link com.google.blockly.android.control.BlocklyController} maintains a
 * {@link VariableRequestCallback} for handling UI requests.
 */
public interface VariableChangeView {
    /**
     * Sets the callback for user generated variable change requests, such as deleting or renaming a
     * variable. The view takes no action on its own so the callback is expected to handle any
     * requests.
     *
     * @param requestCallback The callback to notify when the user has selected an action.
     */
    public void setVariableRequestCallback(VariableRequestCallback requestCallback);
}
