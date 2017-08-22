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
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.blockly.android.R;
import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.android.control.NameManager;
import com.google.blockly.android.control.ProcedureManager;
import com.google.blockly.android.control.ProcedureManager.ArgumentIndexUpdate;
import com.google.blockly.android.ui.MutatorFragment;
import com.google.blockly.model.Mutator;
import com.google.blockly.model.ProcedureInfo;
import com.google.blockly.model.mutator.ProcedureDefinitionMutator;

import java.util.ArrayList;
import java.util.List;

/**
 * Standard UI fragment for editing the {@link ProcedureDefinitionMutator} attached to
 * {@code procedures_defnoreturn} and {@code procedures_defreturn} blocks.
 */
public class ProcedureDefinitionMutatorFragment extends MutatorFragment {
    public static final MutatorFragment.Factory FACTORY =
            new MutatorFragment.Factory<ProcedureDefinitionMutatorFragment>() {
                @Override
                public ProcedureDefinitionMutatorFragment newMutatorFragment(Mutator mutator) {
                    ProcedureDefinitionMutatorFragment fragment =
                            new ProcedureDefinitionMutatorFragment();
                    fragment.init((ProcedureDefinitionMutator) mutator);
                    return fragment;
                }
            };

    private static final int VH_TYPE_ARGUMENT = 1;
    private static final int VH_TYPE_ADD = 2;
    private static final int NO_ORIGINAL_INDEX = -1;

    private static final String NEW_ARGUMENT_NAME = "x";

    private BlocklyController mController;
    private NameManager mVariableNameManager;
    private ProcedureManager mProcedureManager;

    private String mProcedureName;
    private ArrayList<ArgInfo> mArgInfos = new ArrayList<>();  // Name, original index
    private boolean mHasStatementInput = true;

    private Adapter mAdapter;

    private EditText mActiveArgNameField = null;
    private RecyclerView mRecycler;

    private void init(ProcedureDefinitionMutator mutator) {
        mController = mutator.getBlock().getController();
        mVariableNameManager = mController.getWorkspace().getVariableNameManager();
        mProcedureManager = mController.getWorkspace().getProcedureManager();

        mProcedureName = mutator.getProcedureName();
        mHasStatementInput = mutator.hasStatementInput();

        List<String> arguments = mutator.getArgumentNameList();
        int count = arguments.size();
        mArgInfos.clear();
        mArgInfos.ensureCapacity(count);
        for (int i = 0; i < count; ++i) {
            mArgInfos.add(new ArgInfo(arguments.get(i), i));
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View contentView = inflater.inflate(
                R.layout.procedure_definition_mutator_dialog, null, false);

        String headerFormat = getString(R.string.mutator_procedure_def_header_format);
        String headerText = String.format(headerFormat, mProcedureName);
        TextView header = (TextView)contentView.findViewById(R.id.mutator_procedure_def_header);
        header.setText(headerText);

        mRecycler = (RecyclerView) contentView.findViewById(R.id.mutator_procedure_def_recycler);
        mRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        mAdapter = new Adapter();
        mRecycler.setAdapter(mAdapter);

        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.mutator_procedure_def_title)
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

    private View.OnClickListener mOnAddClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int argPosition = mArgInfos.size();
            mArgInfos.add(new ArgInfo(NEW_ARGUMENT_NAME, NO_ORIGINAL_INDEX));
            mAdapter.notifyItemInserted(argPosition);
        }
    };

    private View.OnClickListener mOnDeleteClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int position = mArgInfos.indexOf(v.getTag()); // Set by onBindViewHolder(..)
            if (position >= 0) {
                mArgInfos.remove(position);
                mAdapter.notifyItemRemoved(position);
            }
        }
    };

    private View.OnFocusChangeListener mArgFocusChangeListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            EditText argNameField = (EditText) v;
            if (hasFocus) {
                mActiveArgNameField = argNameField;
            } else {
                validateAndApplyArgNameChange(argNameField);
                mActiveArgNameField = null;
            }
        }
    };

    private void validateAndApplyArgNameChange(EditText argNameField) {
        ArgInfo argInfo = (ArgInfo) argNameField.getTag();
        String newName = argNameField.getText().toString();
        newName = mVariableNameManager.makeValidName(newName, null);
        if (newName != null && mVariableNameManager.getExisting(newName) == null) {
            argInfo.name = newName;
        }
    }

    private void finishMutation() {
        if (mActiveArgNameField != null) {
            validateAndApplyArgNameChange(mActiveArgNameField);
        }

        int count = mArgInfos.size();
        List<String> argNames = new ArrayList<>(count);
        List<ProcedureManager.ArgumentIndexUpdate> indexUpdates = null;
        for (int i = 0; i < count; ++i) {
            ArgInfo argInfo = mArgInfos.get(i);
            argNames.add(argInfo.name);
            if (argInfo.originalIndex != NO_ORIGINAL_INDEX) {
                if (indexUpdates == null) {
                    indexUpdates = new ArrayList<>();
                }
                indexUpdates.add(new ArgumentIndexUpdate(argInfo.originalIndex, i));
            }
        }

        mProcedureManager.mutateProcedure(mProcedureName,
                new ProcedureInfo(mProcedureName, argNames, mHasStatementInput),
                indexUpdates);
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {
        Button mAddButton = null;
        EditText mArgName = null;
        ImageButton mDeleteButton = null;

        public ViewHolder(View view, int viewType) {
            super(view);

            if (viewType == VH_TYPE_ADD) {
                mAddButton = (Button) view.findViewById(R.id.procedure_argument_add);
            } else
            if (viewType == VH_TYPE_ARGUMENT) {
                mArgName = (EditText) view.findViewById(R.id.procedure_argument_name);
                mDeleteButton = (ImageButton) view.findViewById(R.id.procedure_argument_delete);
            }
        }
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        LayoutInflater mInflater;

        public Adapter() {
            mInflater = LayoutInflater.from(getContext());
        }

        @Override
        public int getItemViewType(int position) {
            if (position == mArgInfos.size()) {
                return VH_TYPE_ADD;
            } else {
                return VH_TYPE_ARGUMENT;
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            if (viewType == VH_TYPE_ADD) {
                view = mInflater.inflate(R.layout.procedure_definition_mutator_add, null);
            } else if (viewType == VH_TYPE_ARGUMENT) {
                view = mInflater.inflate(R.layout.procedure_definition_mutator_argument, null);
            } else {
                throw new IllegalStateException("Unrecognized view type " + viewType);
            }
            return new ViewHolder(view, viewType);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            if (holder.getItemViewType() == VH_TYPE_ADD) {
                holder.mAddButton.setOnClickListener(mOnAddClick);
            }
            if (holder.getItemViewType() == VH_TYPE_ARGUMENT) {
                ArgInfo arg = mArgInfos.get(position);
                holder.mArgName.setText(arg.name);
                holder.mArgName.setTag(arg);
                holder.mArgName.setOnFocusChangeListener(mArgFocusChangeListener);
                holder.mDeleteButton.setTag(arg);
                holder.mDeleteButton.setOnClickListener(mOnDeleteClick);
            }
        }

        @Override
        public void onViewRecycled(ViewHolder holder) {
            if (holder.mAddButton != null) {
                holder.mAddButton.setOnClickListener(null);
                holder.mAddButton.setTag(null);
            }
            if (holder.mDeleteButton != null) {
                holder.mDeleteButton.setOnClickListener(null);
                holder.mDeleteButton.setTag(null);
            }
            super.onViewRecycled(holder);
        }

        @Override
        public int getItemCount() {
            return mArgInfos.size() + 1;
        }
    }

    private static final class ArgInfo {
        ArgInfo(String name, int originalPosition) {
            this.name = name;
            this.originalIndex = originalPosition;
        }

        String name;
        int originalIndex;  // or NO_ORIGINAL_INDEX
    }
}
