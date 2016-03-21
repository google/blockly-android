/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

import android.content.ClipDescription;
import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.DragEvent;
import android.widget.EditText;

import com.google.blockly.model.Field;
import com.google.blockly.android.ui.WorkspaceView;
import com.google.blockly.android.ui.fieldview.FieldView;

/**
 * Renders editable text as part of a {@link com.google.blockly.android.ui.InputView}.
 */
public class BasicFieldInputView extends EditText implements FieldView {
    private static final String TAG = "BasicFieldInputView";
    protected Field.FieldInput mInput;

    protected TextWatcher mWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            if (mInput != null) {
                mInput.setText(s.toString());
            }
        }
    };

    private Field.FieldInput.Observer mFieldObserver = new Field.FieldInput.Observer() {
        @Override
        public void onTextChanged(Field.FieldInput field, String oldText, String newText) {
            if (field != mInput) {
                Log.w(TAG, "Received text change from unexpected field.");
                return;
            }
            if (!TextUtils.equals(newText, getText())) {
                setText(newText);
            }
        }
    };

    public BasicFieldInputView(Context context) {
        super(context, null);
    }

    public BasicFieldInputView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BasicFieldInputView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr, 0);
    }

    public BasicFieldInputView(
            Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        addTextChangedListener(mWatcher);
    }

    public void setField(Field input) {
        if (mInput != null) {
            mInput.unregisterObserver(mFieldObserver);
            mInput.setView(null);
        }
        if (input != null) {
            mInput = (Field.FieldInput) input;
            setText(mInput.getText());
            mInput.setView(this);
            mInput.registerObserver(mFieldObserver);
        } else {
            mInput = null;
            setText("");
        }
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
        if (event.getClipDescription() != null
            && event.getClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
            && event.getClipDescription().getLabel().equals(
                WorkspaceView.BLOCK_GROUP_CLIP_DATA_LABEL)) {
            return false;
        }

        return super.onDragEvent(event);
    }

    @Override
    public void unlinkModel() {
        if (mInput != null) {
            mInput.setView(null);
            mInput.unregisterObserver(mFieldObserver);
            mInput = null;
        }
        // TODO(#45): Remove model from view. Set mInput to null, and handle null cases above.
    }
}
