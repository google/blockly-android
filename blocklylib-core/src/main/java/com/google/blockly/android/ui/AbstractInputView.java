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

package com.google.blockly.android.ui;

import android.content.Context;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;

import com.google.blockly.model.Input;
import com.google.blockly.android.ui.fieldview.FieldView;

import java.util.ArrayList;

/**
 * Optional base {@ ViewGroup} representation of an {@link Input} to a {@link com.google.blockly.model.Block}.
 */
public abstract class AbstractInputView extends NonPropagatingViewGroup implements InputView {
    protected final Input mInput;
    protected final @Input.InputType int mInputType;

    protected final WorkspaceHelper mHelper;
    protected final ArrayList<FieldView> mFieldViews = new ArrayList<>();

    // The view of the blocks connected to this input.
    protected BlockGroup mConnectedGroup = null;

    protected AbstractInputView(Context context, WorkspaceHelper helper, Input input) {
        super(context);

        mInput = input;
        mInputType = mInput.getType();
        mHelper = helper;

        mInput.setView(this);
    }

    /**
     * @return The block {@link Input} wrapped by this view.
     */
    public Input getInput() {
        return mInput;
    }

    /**
     * @return The {@link BlockGroup} containing the blocks connected to this input port, if any.
     */
    @Override
    @Nullable
    public BlockGroup getConnectedBlockGroup() {
        return mConnectedGroup;
    }

    /**
     * Set the view of the blocks whose output port is connected to this input.
     *
     * @param blockGroup The {@link BlockGroup} to attach to this input. The {@code childView} will
     *                  be added to the layout hierarchy for the current view via a call to
     *                  {@link ViewGroup#addView(View)}.
     *
     * @throws IllegalStateException if a child view is already set. The Blockly model requires
     *         disconnecting a block from an input before a new one can be connected.
     * @throws IllegalArgumentException if the method argument is {@code null}.
     */
    public void setConnectedBlockGroup(BlockGroup blockGroup) {
        if (mConnectedGroup != null) {
            throw new IllegalStateException("Input is already connected; must disconnect first.");
        }

        if (blockGroup == null) {
            throw new IllegalArgumentException("Cannot use setChildView with a null child. " +
                    "Use disconnectBlockGroup to remove a child view.");
        }
        mConnectedGroup = blockGroup;

        addView(blockGroup);
        requestLayout();
    }

    /**
     * Disconnect the currently-connected child view from this input.
     * <p/>
     * This method also removes the child view from the view hierarchy by calling
     * {@link ViewGroup#removeView(View)}.
     *
     * @return The removed child view, if any. Otherwise, null.
     */
    // Why is this a separate method from above? setConnectedBlockGroup(null)?
    public BlockGroup disconnectBlockGroup() {
        BlockGroup result = mConnectedGroup;
        if (mConnectedGroup != null) {
            removeView(mConnectedGroup);
            mConnectedGroup = null;
            requestLayout();
        }

        return result;
    }

    /**
     * Recursively disconnects the view from the model, and removes all views.
     */
    @Override
    public void unlinkModelAndSubViews() {
        int max = mFieldViews.size();
        for (int i = 0; i < max; ++i) {
            FieldView fieldView = mFieldViews.get(i);
            fieldView.unlinkModel();
        }
        if (mConnectedGroup != null) {
            mConnectedGroup.unlinkModelAndSubViews();
            disconnectBlockGroup();
        }
        removeAllViews();
        mInput.setView(null);
        // TODO(#45): Remove model from view. Set mInput to null, and handle all null cases.
    }
}
