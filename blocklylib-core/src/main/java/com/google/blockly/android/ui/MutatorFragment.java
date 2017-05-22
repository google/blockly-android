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

package com.google.blockly.android.ui;

import android.content.DialogInterface;
import android.support.v4.app.DialogFragment;

import com.google.blockly.model.Mutator;

/**
 * Base class for a dialog fragment that displays UI for mutating a block. The fragment will be
 * shown or hidden by the activity, which is responsible for managing the fragment's lifecycle.
 * <p/>
 * {@link Factory}s should be registered with
 * {@link BlockViewFactory#registerMutatorUi(String, Factory)} so that the controller can create
 * fragments for block mutator UIs as necessary.
 */
public abstract class MutatorFragment extends DialogFragment {
    protected DismissListener mDismissListener;

    /**
     * Each mutator which wants to provide a toggleable UI for the user to mutate the block should
     * have a factory. The factory can be registered with
     * {@link BlockViewFactory#registerMutatorUi(String, Factory)}.
     */
    public interface Factory<T extends MutatorFragment> {
        T newMutatorFragment(Mutator mutator);
    }

    /**
     * Listener interface for performing cleanup when a MutatorFragment is dismissed.
     */
    public interface DismissListener {
        /**
         * Called when the fragment's dialog is dismissed.
         */
        void onDismiss(MutatorFragment fragment);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (mDismissListener != null) {
            mDismissListener.onDismiss(this);
        }
    }

    /**
     * Sets a listener that will be called when the mutator's dialog is dismissed.
     *
     * @param listener The listener to call.
     */
    public void setDismissListener(DismissListener listener) {
        mDismissListener = listener;
    }
}
