package com.google.blockly.android.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import com.google.blockly.android.R;

/**
 * Default dialog window shown when clearing the workspace.
 */
public class ClearWorkspaceDialog extends DialogFragment {
    public interface NoticeDialogListener {
        public void onDialogPositiveClick(DialogFragment dialog);
        public void onDialogNegativeClick(DialogFragment dialog);
    }

    NoticeDialogListener mListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (NoticeDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceBundle) {

        AlertDialog.Builder clear = new AlertDialog.Builder(getActivity());
        clear.setTitle(R.string.workspace_clear_title);
        clear.setMessage(R.string.workspace_clear_message);
        clear.setPositiveButton(R.string.workspace_clear_positive,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int i) {
                        mListener.onDialogPositiveClick(ClearWorkspaceDialog.this);
                    }
                });
        clear.setNegativeButton(R.string.workspace_clear_negative,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        mListener.onDialogNegativeClick(ClearWorkspaceDialog.this);
                    }
                });
        return clear.create();
    }
}