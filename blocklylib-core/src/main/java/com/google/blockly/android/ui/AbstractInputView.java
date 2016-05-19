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

import com.google.blockly.android.ui.fieldview.FieldView;
import com.google.blockly.model.Block;
import com.google.blockly.model.Input;

import java.util.ArrayList;
import java.util.List;

/**
 * Optional base {@ ViewGroup} representation of an {@link Input} to a {@link Block}.
 * <p/>
 * Default implementation assumes all {@link FieldView}s are added directly to this view as their
 * parent, as the first children.  Subclasses can use an alternate behavior by overriding
 * {@link #addFieldViewsToViewHierarchy}.
 */
public abstract class AbstractInputView extends NonPropagatingViewGroup implements InputView {
    protected final Input mInput;
    protected final @Input.InputType int mInputType;

    protected final WorkspaceHelper mHelper;
    protected final ArrayList<FieldView> mFieldViews;

    // The view of the blocks connected to this input.
    protected BlockGroup mConnectedGroup = null;

    /**
     * Constructs a base implementation of an {@link InputView}.
     *
     * @param context The Android {@link Context} for the app.
     * @param helper The {@link WorkspaceHelper} for the activity.
     * @param input The {@link Input} the view represents.
     * @param fieldViews The {@link FieldView}s instantiated by the {@link BlockViewFactory}.
     */
    protected AbstractInputView(Context context, WorkspaceHelper helper, Input input,
                                List<FieldView> fieldViews) {
        super(context);

        mInput = input;
        mInputType = mInput.getType();
        mHelper = helper;

        mInput.setView(this);
        mFieldViews = new ArrayList<>(fieldViews);
        addFieldViewsToViewHierarchy();
    }

    /**
     * Adds the {@link FieldView}s in {@link #mFieldViews} to the view hierarchy. The default
     * implementation adds the views directly to this view, in order.
     */
    protected void addFieldViewsToViewHierarchy() {
        for (int i = 0; i < mFieldViews.size(); i++) {
            addView((View) mFieldViews.get(i));
        }
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
     * Sets the view of the blocks whose output/previous connector is connected to this input.
     * Setting it to null will remove any set block group.
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
        if (blockGroup == null) {
            if (mConnectedGroup != null) {
                removeView(mConnectedGroup);
                mConnectedGroup = null;
                requestLayout();
            }
            return;
        }
        if (mConnectedGroup != null) {
            throw new IllegalStateException("Input is already connected; must disconnect first.");
        }

        mConnectedGroup = blockGroup;

        addView(blockGroup);
        requestLayout();
    }

    /**
     * Recursively disconnects the view from the model, and removes all views.
     */
    @Override
    public void unlinkModel() {
        int max = mFieldViews.size();
        for (int i = 0; i < max; ++i) {
            FieldView fieldView = mFieldViews.get(i);
            fieldView.unlinkField();
        }
        if (mConnectedGroup != null) {
            mConnectedGroup.unlinkModel();
            mConnectedGroup = null;
        }
        removeAllViews();
        mInput.setView(null);
        // TODO(#45): Remove model from view. Set mInputField to null, and handle all null cases.
    }
}
