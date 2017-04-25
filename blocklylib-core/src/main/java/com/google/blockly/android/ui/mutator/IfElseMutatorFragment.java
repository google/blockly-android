package com.google.blockly.android.ui.mutator;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.blockly.android.R;
import com.google.blockly.android.ui.MutatorFragment;
import com.google.blockly.model.Block;
import com.google.blockly.model.mutator.IfElseMutator;

import org.xmlpull.v1.XmlPullParser;

public class IfElseMutatorFragment extends MutatorFragment {
    private IfElseMutator mMutator;
    private Block mBlock;
    private String mElseIfCountString;
    private String mElseCountString;
    private int mElseIfCount;
    private boolean mHasElse;

    private ImageView mRemoveElseIfButton;
    private TextView mElseIfCountView;

    public void init(IfElseMutator mutator) {
        mMutator = mutator;
        mBlock = mutator.getBlock();
        mElseIfCount = mutator.getElseIfCount();
        mHasElse = mutator.hasElse();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Resources res = context.getResources();
        mElseIfCountString = res.getString(R.string.mutator_if_else_ifelse_count);
        mElseCountString = res.getString(R.string.mutator_if_else_else_count);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater helium = LayoutInflater.from(getContext());
        View contentView = helium.inflate(R.layout.if_else_mutator_dialog, null, false);

        mElseIfCountView = (TextView) contentView.findViewById(R.id.if_else_count);
        updateCountString();

        mRemoveElseIfButton = (ImageView) contentView.findViewById(R.id.remove_else_if);
        mRemoveElseIfButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mElseIfCount--;
                if (mElseIfCount <= 0) {
                    mElseIfCount = 0;
                    mRemoveElseIfButton.setEnabled(false);
                }
                updateCountString();
            }
        });
        if (mElseIfCount == 0) {
            mRemoveElseIfButton.setEnabled(false);
        }

        ImageView iv = (ImageView) contentView.findViewById(R.id.add_else_if);
        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mElseIfCount++;
                if (!mRemoveElseIfButton.isEnabled()) {
                    mRemoveElseIfButton.setEnabled(true);
                }
                updateCountString();
            }
        });

        CheckBox elseCheckBox = (CheckBox) contentView.findViewById(R.id.else_checkbox);
        elseCheckBox.setChecked(mHasElse);
        elseCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mHasElse = isChecked;
            }
        });

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

    private void updateCountString() {
        String elseIfCount = String.format(mElseIfCountString, mElseIfCount);
        mElseIfCountView.setText(elseIfCount);
    }

    private void finishMutation() {
        mMutator.update(mElseIfCount, mHasElse);
        dismiss();
    }
}
