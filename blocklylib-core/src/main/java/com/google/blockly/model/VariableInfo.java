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

package com.google.blockly.model;

import android.support.v4.util.ArraySet;

import java.util.Set;

/**
 * Metadata describing a variable in the Blockly Workspace.
 */
public interface VariableInfo {
    String getDisplayName();

    /**
     * @return Whether the variable is used as an argument to a procedure.
     */
    boolean isProcedureArgument();

    /**
     * @return The count of number of blocks that are using the variable.
     */
    int getUsageCount();

    /**
     * @return An immutable list of procedure names that use this variable as an argument.
     */
    ArraySet<String> getProcedureNames();

    /**
     * @return An immutable list of fields that refer to this variable.
     */
    ArraySet<FieldVariable> getFields();
}
