/*
 *  Copyright 2016 Google Inc. All Rights Reserved.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.google.blockly.android.ui.fieldview;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.Adapter;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import com.google.blockly.android.ui.VariableViewAdapter;
import com.google.blockly.model.Field;
import com.google.blockly.model.FieldVariable;

/**
 * Renders a dropdown field containing the workspace's variables as part of a Block.
 */
public class BasicFieldVariableView extends Spinner implements FieldView {
    protected FieldVariable.Observer mFieldObserver = new FieldVariable.Observer() {
        @Override
        public void onVariableChanged(FieldVariable field, String oldVar, String newVar) {
            setSelection(mVariableField.getVariable());
        }
    };

    protected FieldVariable mVariableField;
    protected VariableViewAdapter mAdapter;

    private final Handler mMainHandler;

    /**
     * Constructs a new {@link BasicFieldVariableView}.
     *
     * @param context The application's context.
     */
    public BasicFieldVariableView(Context context) {
        super(context);
        mMainHandler = new Handler(context.getMainLooper());
    }

    public BasicFieldVariableView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mMainHandler = new Handler(context.getMainLooper());
    }

    public BasicFieldVariableView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mMainHandler = new Handler(context.getMainLooper());
    }

    @Override
    public void setField(Field field) {
        FieldVariable variableField = (FieldVariable) field;

        if (mVariableField == variableField) {
            return;
        }

        if (mVariableField != null) {
            mVariableField.unregisterObserver(mFieldObserver);
        }
        mVariableField = variableField;
        if (mVariableField != null) {
            // Update immediately.
            setSelection(mVariableField.getVariable());
            mVariableField.registerObserver(mFieldObserver);
        } else {
            setSelection(0);
        }
    }

    @Override
    public Field getField() {
        return mVariableField;
    }

    @Override
    public void setSelection(int position) {
        if (position == getSelectedItemPosition()) {
            return;
        }
        super.setSelection(position);
        if (mVariableField != null) {
            String varName = getAdapter().getItem(position).toString();
            mVariableField.setVariable(varName);
        }
    }

    @Override
    public void setAdapter(SpinnerAdapter adapter) {
        mAdapter = (VariableViewAdapter) adapter;
        super.setAdapter(adapter);

        if (adapter != null && mVariableField != null) {
            setSelection(mVariableField.getVariable());
        }
    }

    @Override
    public void unlinkField() {
        setField(null);
    }

    /**
     * Set the selection from a variable name, ignoring case. The variable must be a variable in
     * the workspace.
     *
     * @param variableName The name of the variable, ignoring case.
     */
    private void setSelection(final String variableName) {
        if (TextUtils.isEmpty(variableName)) {
            throw new IllegalArgumentException("Cannot set an empty variable name.");
        }
        if (mAdapter != null) {
            // Because this may change the available indices, we want to make sure each assignment
            // is in its own event tick.
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    setSelection(mAdapter.getOrCreateVariableIndex(variableName));
                }
            });
        }
    }
}
