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

import android.content.ClipDescription;
import android.content.Context;
import android.graphics.Rect;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.util.AttributeSet;
import android.util.Log;
import android.view.DragEvent;
import android.widget.EditText;

import com.google.blockly.android.ui.WorkspaceView;
import com.google.blockly.model.Field;
import com.google.blockly.model.FieldNumber;

import java.text.DecimalFormat;

/**
 * A basic UI for {@link FieldNumber}.
 */
public class BasicFieldNumberView extends EditText implements FieldView {
    private static final String TAG = "BasicFieldNumberView";

    protected static final DecimalFormat DEFAULT_DISPLAY_FORMAT = new DecimalFormat("0");

    protected DecimalFormat mDisplayFormat = DEFAULT_DISPLAY_FORMAT;
    protected String mRecentValidText = "";

    private final TextWatcher mWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable newContent) {
            if (mNumberField != null && newContent.length() > 0) {  // Allow empty strings
                String s = newContent.toString();
                if (mNumberField.setFromString(s)) {
                    mRecentValidText = s;
                } else {
                    // Try to preserve the cursor location, even if we veto the input
                    int newLength = mRecentValidText.length();
                    int selectionStart = Math.min(getSelectionStart(), newLength);
                    int selectionFromEnd = newContent.length() - getSelectionStart();
                    int selectionEnd = Math.max(selectionStart, newLength - selectionFromEnd);

                    setText(mRecentValidText);
                    setSelection(selectionStart, selectionEnd);
                }
            }
        }
    };

    private final FieldNumber.Observer mFieldObserver = new FieldNumber.Observer() {
        @Override
        public void onValueChanged(FieldNumber field, double oldValue, double newValue) {
            if (field != mNumberField) {
                Log.w(TAG, "Received value change from unexpected field.");
                return;
            }
            try {
                CharSequence text = getText();
                if (TextUtils.isEmpty(text) || Double.parseDouble(text.toString()) != newValue) {
                    mRecentValidText = mDisplayFormat.format(mNumberField.getValue());
                    setText(mRecentValidText);
                }
            } catch (NumberFormatException e) {
                setText(mDisplayFormat.format(newValue));
            }
        }

        @Override
        public void onConstraintsChanged(FieldNumber field) {
            updateInputMethod();
        }
    };

    protected FieldNumber mNumberField;

    public BasicFieldNumberView(Context context) {
        super(context);
        onFinishInflate();
    }

    public BasicFieldNumberView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BasicFieldNumberView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        addTextChangedListener(mWatcher);
        updateInputMethod();
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        if (!focused) {
            CharSequence text = getText();
            if (text.length() == 0) {
                // Replace empty string with value closest to zero.
                mNumberField.setValue(0);
            } else {
                setText(mDisplayFormat.format(mNumberField.getValue()));
            }
        }
    }

    /**
     * Sets the {@link Field} model for this view, if not null. Otherwise, disconnects the prior
     * field model.
     *
     * @param number The number field to view.
     */
    public void setField(FieldNumber number) {
        if (mNumberField == number) {
            return;
        }

        if (mNumberField != null) {
            mNumberField.unregisterObserver(mFieldObserver);
        }
        mNumberField = number;
        if (mNumberField != null) {
            updateInputMethod();
            mRecentValidText = mDisplayFormat.format(mNumberField.getValue());
            setText(mRecentValidText);
            mNumberField.registerObserver(mFieldObserver);
        } else {
            setText("");
        }
    }

    @Override
    public Field getField() {
        return mNumberField;
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
    public void unlinkField() {
        setField(null);
    }

    protected void updateInputMethod() {
        boolean hasNumberField = mNumberField != null;
        setEnabled(hasNumberField);

        if (hasNumberField) {
            int imeOptions = InputType.TYPE_CLASS_NUMBER;
            StringBuilder allowedChars = new StringBuilder("0123456789");

            if (mNumberField.getMinimumValue() < 0) {
                imeOptions |= InputType.TYPE_NUMBER_FLAG_SIGNED;
                allowedChars.append("-");
            }
            if (!mNumberField.isInteger()) {
                imeOptions |= InputType.TYPE_NUMBER_FLAG_DECIMAL;
                allowedChars.append(".");
            }
            setImeOptions(imeOptions);
            setKeyListener(DigitsKeyListener.getInstance(allowedChars.toString()));
        }
    }
}
