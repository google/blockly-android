/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.blockly.android.ui;

import android.support.annotation.Nullable;
import android.view.ViewGroup;

import com.google.blockly.model.Input;

/**
 * Abstract methods required by {@link Input} views.
 * <p/>
 * Implementations of {@link InputView} must extend {@link ViewGroup} or one of its subclasses. The
 * class should also disable activated/focused/pressed/selected state propegation, as implemented in
 * {@link NonPropagatingViewGroup}.
 */
public interface InputView {
    /**
     * @return The {@link Input} represented by this view.
     */
    Input getInput();

    /**
     * Sets the {@link BlockGroup} containing the block connected to this input and updates the view
     * hierarchy. Calling this with null will disconnect the current group.
     *
     * @param group The {@link BlockGroup} to add to this input view.
     */
    void setConnectedBlockGroup(@Nullable BlockGroup group);

    /**
     * @return The {@link BlockGroup} connected to this input connection.
     */
    @Nullable
    BlockGroup getConnectedBlockGroup();

    /**
     * Recursively disconnects the view from the model, including all subviews/model subcomponents.
     * <p/>
     * This method should only be called by {@link BlockView#unlinkModel()}.
     */
    void unlinkModel();
}
