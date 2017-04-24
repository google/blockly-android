package com.google.blockly.android.ui.mutator;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.google.blockly.android.R;
import com.google.blockly.android.ui.MutatorFragment;
import com.google.blockly.model.Block;
import com.google.blockly.model.mutator.IfElseMutator;

public class IfElseMutatorFragment extends MutatorFragment {
    private IfElseMutator mMutator;
    private Block mBlock;

    public void init(IfElseMutator mutator) {
        mMutator = mutator;
        mBlock = mutator.getBlock();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater helium = LayoutInflater.from(getContext());
        View contentView = helium.inflate(R.layout.if_else_mutator_dialog, null, false);
        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.mutator_if_else_title)
                .setPositiveButton(R.string.mutator_done, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finishMutation();
                    }
                })
                .setView(contentView)
                .create();
        return dialog;
    }

    private void finishMutation() {
        dismiss();
    }
}
