/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.blockly.android.ui.fieldview;

import android.content.Context;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.DragEvent;

import com.google.blockly.android.ui.WorkspaceHelper;
import com.google.blockly.model.Field;
import com.google.blockly.model.FieldInput;

/**
 * Renders editable text as part of a {@link com.google.blockly.android.ui.InputView}.
 */
public class BasicFieldInputView extends AppCompatEditText implements FieldView {
    private static final String TAG = "BasicFieldInputView";

    private final TextWatcher mWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            if (mInputField != null) {
                mInputField.setText(s.toString());
            }
        }
    };

    private final Field.Observer mFieldObserver = new Field.Observer() {
        @Override
        public void onValueChanged(Field field, String oldValue, String newValue) {
            if (field != mInputField) {
                Log.w(TAG, "Received text change from unexpected field.");
                return;
            }
            if (!TextUtils.equals(newValue, getText())) {
                setText(newValue);
            }
        }
    };

    protected FieldInput mInputField;

    public BasicFieldInputView(Context context) {
        super(context);
        onFinishInflate();
    }

    public BasicFieldInputView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BasicFieldInputView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        addTextChangedListener(mWatcher);
    }

    @Override
    public void setField(Field field) {
        FieldInput input = (FieldInput) field;
        if (mInputField == input) {
            return;
        }

        if (mInputField != null) {
            mInputField.unregisterObserver(mFieldObserver);
        }
        mInputField = input;
        if (mInputField != null) {
            setText(mInputField.getText());
            mInputField.registerObserver(mFieldObserver);
        } else {
            setText("");
        }
    }

    @Override
    public Field getField() {
        return mInputField;
    }

    /**
     * Override onDragEvent to stop blocks from being dropped into text fields.  If the dragged
     * information is anything but a block, let the standard EditText drag interface take care of
     * it.
     *
     * @param event The {@link DragEvent} to respond to.
     * @return False if the dragged data is a block, whatever a normal EditText would return
     * otherwise.
     */
    @Override
    public boolean onDragEvent(DragEvent event) {
        // Don't let block groups be dropped into text fields.
        if (WorkspaceHelper.isBlockDrag(getContext(), event)) {
            return false;
        }
        return super.onDragEvent(event);
    }

    @Override
    public void unlinkField() {
        setField(null);
    }

    @Override
    public boolean callOnClick() {
        Log.d(TAG, "callOnClick()");
        return super.callOnClick();
    }

    @Override
    public boolean performClick() {
        Log.d(TAG, "performClick()");
        return super.performClick();
    }
}
