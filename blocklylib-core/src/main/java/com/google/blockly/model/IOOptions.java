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

/**
 * A set of options for reading or writing blocks.
 *
 * This class is subject to API changes, and so all members are marked package private for now.
 * Instead, use the static instances {@link #WRITE_ALL_DATA}, {@link #WRITE_ALL_BLOCKS_WITHOUT_ID},
 * or {@link #WRITE_ROOT_ONLY_WITHOUT_ID}.
 */
public final class IOOptions {
    public static final IOOptions WRITE_ALL_DATA = new IOOptions(true, true);
    public static final IOOptions WRITE_ALL_BLOCKS_WITHOUT_ID = new IOOptions(true, false);
    public static final IOOptions WRITE_ROOT_ONLY_WITHOUT_ID = new IOOptions(false, false);

    protected final boolean mIncludeChildren;
    protected final boolean mIncludeIds;

    // Package private because this class is subject to future changes.
    IOOptions(boolean includeChildren, boolean includeIds) {
        mIncludeChildren = includeChildren;
        mIncludeIds = includeIds;
    }

    boolean isBlockChildWritten() {
        return mIncludeChildren;
    }

    boolean isBlockIdWritten() {
        return mIncludeIds;
    }
}
