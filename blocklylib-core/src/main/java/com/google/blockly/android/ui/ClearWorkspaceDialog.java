package com.google.blockly.android.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import com.google.blockly.android.R;
import com.google.blockly.android.control.BlocklyController;

/**
 * Default dialog window shown when clearing the workspace.
 */
public class ClearWorkspaceDialog extends DialogFragment {
    public boolean choice;
    public interface NoticeDialogListener {
        public void onDialogPositiveClick(DialogFragment dialog);
        public void onDialogNegativeClick(DialogFragment dialog);
    }
    NoticeDialogListener mListener;

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceBundle) {

        AlertDialog.Builder clear = new AlertDialog.Builder(getActivity());
        clear.setTitle(R.string.workspace_clear_title);
        clear.setMessage(R.string.workspace_clear_message);
        clear.setPositiveButton(R.string.workspace_clear_positive,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int i) {
                        choice = true;
                    }
                });
        clear.setNegativeButton(R.string.workspace_clear_negative,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        choice = false;
                    }
                });
        return clear.create();
    }

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
}