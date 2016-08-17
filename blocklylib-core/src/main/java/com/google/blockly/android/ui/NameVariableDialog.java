package com.google.blockly.android.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.google.blockly.android.R;
import com.google.blockly.android.control.BlocklyController;

/**
 * Default dialog window shown when creating or renaming a variable in the workspace.
 */
public class NameVariableDialog extends DialogFragment {
    private String mVariable;
    private DialogInterface.OnClickListener mListener;
    private EditText mNameEditText;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceBundle) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View nameView = inflater.inflate(R.layout.name_variable_view, null);
        mNameEditText = (EditText) nameView.findViewById(R.id.name);
        mNameEditText.setText(mVariable);

        AlertDialog.Builder bob = new AlertDialog.Builder(getActivity());
        bob.setTitle(R.string.name_variable_title);
        bob.setView(nameView);
        bob.setPositiveButton(R.string.name_variable_positive, mListener);
        bob.setNegativeButton(R.string.name_variable_negative, mListener);
        return bob.create();
    }

    public void setVariable(String variable, final Callback clickListener) {
        if (clickListener == null) {
            throw new IllegalArgumentException("Must have a listener to perform an action.");
        }
        mVariable = variable;
        mListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case AlertDialog.BUTTON_POSITIVE:
                        clickListener.onNameConfirmed(mVariable,
                                mNameEditText.getText().toString());
                        break;
                    case AlertDialog.BUTTON_NEGATIVE:
                        clickListener.onNameCanceled(mVariable);
                        break;
                }
            }
        };
    }

    public abstract static class Callback {
        public abstract void onNameConfirmed(String originalName, String newName);
        public void onNameCanceled(String originalName) {
            // Do nothing by default
        }
    }
}
