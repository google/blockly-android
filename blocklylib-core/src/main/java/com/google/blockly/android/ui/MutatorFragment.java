package com.google.blockly.android.ui;

import android.content.DialogInterface;
import android.support.v4.app.DialogFragment;


public abstract class MutatorFragment extends DialogFragment {
    protected DismissListener mDismissListener;

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

    /**
     * Listener interface for performing cleanup when a MutatorFragment is dismissed.
     */
    public interface DismissListener {
        /**
         * Called when the fragment's dialog is dismissed.
         */
        void onDismiss(MutatorFragment fragment);
    }
}
