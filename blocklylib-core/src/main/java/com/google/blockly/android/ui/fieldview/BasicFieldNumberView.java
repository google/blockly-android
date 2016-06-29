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

/**
 * A basic UI for {@link FieldNumber}.
 */
public class BasicFieldNumberView extends EditText implements FieldView {
    private static final String TAG = "BasicFieldNumberView";

    protected boolean mAllowExponent = true;
    protected boolean mTextIsValid = false;

    private boolean mIsUpdatingField = false;

    private final TextWatcher mWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable text) {
            if (mNumberField != null) {
                try {
                    mIsUpdatingField = true;
                    if (text.length() > 0) {
                        setTextValid(mNumberField.setFromString(text.toString()));
                    } else {
                        // Empty string always overwrites value as if it was 0.
                        mNumberField.setValue(0);
                        setTextValid(false);
                    }
                } finally {
                    mIsUpdatingField = false;
                }
            }
        }
    };

    private final FieldNumber.Observer mFieldObserver = new FieldNumber.Observer() {
        @Override
        public void onValueChanged(FieldNumber field, double oldValue, double newValue) {
            if (mIsUpdatingField) {
                return;
            }
            if (field != mNumberField) {  // Potential race condition if view's field changes.
                Log.w(TAG, "Received value change from unexpected field.");
                return;
            }
            try {
                CharSequence text = getText();
                try {
                    // Because numbers can have different string representations,
                    // only overwrite if the parsed value differs.
                    if (TextUtils.isEmpty(text)
                            || Double.parseDouble(text.toString()) != newValue) {
                        setText(field.getValueString());
                    }
                } catch (NumberFormatException e) {
                    setText(field.getValueString());
                }
            } catch (NumberFormatException e) {
                setText(field.getValueString());
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
        if (!focused && mNumberField != null) {
            CharSequence text = getText();
            if (text.length() == 0) {
                // Replace empty string with value closest to zero.
                mNumberField.setValue(0);
            }
            setText(mNumberField.getSerializedValue());
            setTextValid(true);
        }
    }

    @Override
    public void setField(Field field) {
        FieldNumber number = (FieldNumber) field;
        if (mNumberField == number) {
            return;
        }

        if (mNumberField != null) {
            mNumberField.unregisterObserver(mFieldObserver);
        }
        mNumberField = number;
        if (mNumberField != null) {
            updateInputMethod();
            setText(mNumberField.getSerializedValue());
            mNumberField.registerObserver(mFieldObserver);
        } else {
            setText("");
        }
    }

    @Override
    public Field getField() {
        return mNumberField;
    }

    public boolean isTextValid() {
        return mTextIsValid;
    }

    protected void setTextValid(boolean textIsValid) {
        mTextIsValid = textIsValid;
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
            if (mAllowExponent) {
                allowedChars.append("e");
            }

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
