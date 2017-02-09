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

package com.google.blockly.android.ui.fieldview;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public abstract class VariableRequestCallback {
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({REQUEST_DELETE, REQUEST_RENAME, REQUEST_CREATE})
    public @interface VariableRequestType {
    }

    public static final int REQUEST_DELETE = 1;
    public static final int REQUEST_RENAME = 2;
    public static final int REQUEST_CREATE = 3;

    public abstract void onVariableRequest(int request, String variable);
}
