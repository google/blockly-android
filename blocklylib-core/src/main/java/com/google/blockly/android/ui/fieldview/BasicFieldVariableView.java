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
import android.database.DataSetObserver;
import android.os.Handler;
import android.support.annotation.IntDef;
import android.support.annotation.LayoutRes;
import android.util.AttributeSet;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.google.blockly.android.R;
import com.google.blockly.android.control.NameManager;
import com.google.blockly.model.Field;
import com.google.blockly.model.FieldVariable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.SortedSet;

/**
 * Renders a dropdown field containing the workspace's variables as part of a Block.
 */
public class BasicFieldVariableView extends android.support.v7.widget.AppCompatSpinner
        implements FieldView, VariableChangeView {
    protected Field.Observer mFieldObserver = new Field.Observer() {
        @Override
        public void onValueChanged(Field field, String oldValue, String newValue) {
            refreshSelection();
        }
    };

    protected FieldVariable mVariableField;
    protected VariableViewAdapter mAdapter;
    protected VariableRequestCallback mCallback;

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
            refreshSelection();
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
        if (mVariableField == null) {
            return;
        }
        int type = ((VariableViewAdapter)getAdapter()).getVariableAction(position);

        switch (type) {
            case VariableViewAdapter.ACTION_SELECT_VARIABLE:
                super.setSelection(position);
                if (mVariableField != null) {
                    String varName = getAdapter().getItem(position).toString();
                    mVariableField.setVariable(varName);
                }
                break;
            case VariableViewAdapter.ACTION_RENAME_VARIABLE:
                if (mCallback != null) {
                    mCallback.onVariableRequest(VariableRequestCallback.REQUEST_RENAME,
                            mVariableField.getVariable());
                }
                break;
            case VariableViewAdapter.ACTION_DELETE_VARIABLE:
                if (mCallback != null) {
                    mCallback.onVariableRequest(VariableRequestCallback.REQUEST_DELETE,
                            mVariableField.getVariable());
                }
                break;
        }

    }

    @Override
    public void setAdapter(SpinnerAdapter adapter) {
        mAdapter = (VariableViewAdapter) adapter;
        super.setAdapter(adapter);

        if (mAdapter != null) {
            refreshSelection();
            mAdapter.registerDataSetObserver(new DataSetObserver() {
                @Override
                public void onChanged() {
                    refreshSelection();
                }
            });
        }
    }

    @Override
    public void unlinkField() {
        setField(null);
    }

    @Override
    public void setVariableRequestCallback(VariableRequestCallback requestCallback) {
        mCallback = requestCallback;
    }

    /**
     * Updates the selection from the field. This is used when the indices may have changed to
     * ensure the correct index is selected.
     */
    private void refreshSelection() {
        if (mAdapter != null) {
            // Because this may change the available indices, we want to make sure each assignment
            // is in its own event tick.
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mVariableField != null) {
                        setSelection(
                                mAdapter.getOrCreateVariableIndex(mVariableField.getVariable()));
                    }
                }
            });
        }
    }

    /**
     * An implementation of {@link ArrayAdapter} that wraps the
     * {@link NameManager.VariableNameManager} to create the variable item views.
     */
    public static class VariableViewAdapter extends ArrayAdapter<String> {
        @Retention(RetentionPolicy.SOURCE)
        @IntDef({ACTION_SELECT_VARIABLE, ACTION_RENAME_VARIABLE, ACTION_DELETE_VARIABLE})
        public @interface VariableAdapterType{}
        public static final int ACTION_SELECT_VARIABLE = 0;
        public static final int ACTION_RENAME_VARIABLE = 1;
        public static final int ACTION_DELETE_VARIABLE = 2;

        private final NameManager mVariableNameManager;
        private final SortedSet<String> mVars;
        private final String mRenameString;
        private final String mDeleteString;

        /**
         * @param variableNameManager The name manager containing the variables.
         * @param context A context for inflating layouts.
         * @param resource The {@link TextView} layout to use when inflating items.
         */
        public VariableViewAdapter(Context context, NameManager variableNameManager,
                                   @LayoutRes int resource) {
            super(context, resource);

            mVariableNameManager = variableNameManager;
            mVars = mVariableNameManager.getUsedNames();

            mRenameString = context.getString(R.string.rename_variable);
            mDeleteString = context.getString(R.string.delete_variable);
            refreshVariables();
            variableNameManager.registerObserver(new DataSetObserver() {
                @Override
                public void onChanged() {
                    refreshVariables();
                }
            });
        }

        /**
         * Retrieves the index for the given variable name, creating a new variable if it is not
         * found.
         *
         * @param variableName The name of the variable to retrieve.
         * @return The index of the variable.
         */
        public int getOrCreateVariableIndex(String variableName) {
            String existing = mVariableNameManager.getExisting(variableName);
            if (existing != null) {
                return getIndexForVarName(existing);
            } else {
                // No match found.  Create it.
                variableName = mVariableNameManager.makeValidName(
                        /* suggested */ variableName, /* fallback */ variableName);
                mVariableNameManager.addName(variableName);
                int insertionIndex = getIndexForVarName(variableName);

                notifyDataSetChanged();
                return insertionIndex;
            }
        }

        private int getIndexForVarName(String expected) {
            int i = 0;
            for (String varName : mVars) {
                if (varName.equals(expected)) {
                    return i;
                }
                ++i;
            }
            throw new IllegalArgumentException(
                    "Expected variable name \"" + expected + "\" not found.");
        }

        @Override
        public int getCount() {
            int count = super.getCount();
            return count == 0 ? 0 : count + 2;
        }

        @Override
        public String getItem(int index) {
            int count = super.getCount();
            if (index >= count + 2 || index < 0) {
                throw new IndexOutOfBoundsException("There is no item at index " + index
                        + ". Count is " + count);
            }
            if (index < count) {
                return super.getItem(index);
            }
            if (index == count) {
                return mRenameString;
            } else {
                return mDeleteString;
            }
        }

        public @VariableAdapterType
        int getVariableAction(int index) {
            int count = super.getCount();
            if (index >= count + 2 || index < 0) {
                throw new IndexOutOfBoundsException("There is no item at index " + index + "."
                        + " Count is " + count);
            }
            if (index < count) {
                return ACTION_SELECT_VARIABLE;
            } else if (index == count) {
                return ACTION_RENAME_VARIABLE;
            } else {
                return ACTION_DELETE_VARIABLE;
            }
        }

        /**
         * Updates the ArrayAdapter internal list with the latest list from the NameManager.
         */
        private void refreshVariables() {
            clear();
            for (String varName : mVars) {
                add(varName);
            }
            notifyDataSetChanged();
        }
    }
}
