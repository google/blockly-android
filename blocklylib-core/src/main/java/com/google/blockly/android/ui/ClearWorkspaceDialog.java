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
    private BlocklyController mController;
    public boolean choice;

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

    public void setController(BlocklyController controller) {
        mController = controller;
    }

}