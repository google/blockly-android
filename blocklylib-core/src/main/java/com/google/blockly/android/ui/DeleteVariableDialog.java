package com.google.blockly.android.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import com.google.blockly.android.R;
import com.google.blockly.android.control.BlocklyController;

/**
 * Default dialog window shown when deleting a variable in the workspace.
 */
public class DeleteVariableDialog extends DialogFragment {
    private String mVariable;
    private int mCount;
    private BlocklyController mController;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceBundle) {
        AlertDialog.Builder bob = new AlertDialog.Builder(getActivity());
        bob.setTitle(R.string.delete_variable_title);
        bob.setMessage(String.format(getResources().getString(R.string.delete_variable_confirm),
                mVariable, mCount));
        bob.setPositiveButton(R.string.delete_variable_positive,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mController.deleteVariable(mVariable);
                    }
                });
        return bob.create();
    }

    public void setController(BlocklyController controller) {
        mController = controller;
    }

    public void setVariable(String variable, int count) {
        mVariable = variable;
        mCount = count;
    }
}
