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
import com.google.blockly.model.Mutator;
import com.google.blockly.model.mutator.IfElseMutator;

/**
 * Standard fragment UI for editing an if-else block. This fragment does not support restoring
 * across activity restarts and should not be included in the back stack.
 */
public class IfElseMutatorFragment extends MutatorFragment {
    public static final MutatorFragment.Factory FACTORY =
            new MutatorFragment.Factory<IfElseMutatorFragment>() {
                @Override
                public IfElseMutatorFragment newMutatorFragment(Mutator mutator) {
                    IfElseMutatorFragment fragment = new IfElseMutatorFragment();
                    fragment.init((IfElseMutator) mutator);
                    return fragment;
                }
            };

    private IfElseMutator mMutator;
    private String mElseIfCountString;
    private int mElseIfCount;
    private boolean mHasElse;

    private ImageView mRemoveElseIfButton;
    private TextView mElseIfCountView;

    /**
     * This must be called after the fragment is created with the mutator that it should show UI
     * for. Because of this extra initialization this fragment should not be restored across
     * activity recreations and should not be added to the back stack.
     *
     * @param mutator The mutator to show UI for.
     */
    public void init(IfElseMutator mutator) {
        mMutator = mutator;
        mElseIfCount = mutator.getElseIfCount();
        mHasElse = mutator.hasElse();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Resources res = context.getResources();
        mElseIfCountString = res.getString(R.string.mutator_ifelse_edit_ifelse_count);
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
                .setTitle(R.string.mutator_ifelse_edit_title)
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
        // Because the user always sees at least one "if", start the count at one.
        String elseIfCount = String.format(mElseIfCountString, mElseIfCount + 1);
        mElseIfCountView.setText(elseIfCount);
    }

    private void finishMutation() {
        mMutator.mutate(mElseIfCount, mHasElse);
        dismiss();
    }
}
