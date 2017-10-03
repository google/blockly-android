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
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Build;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.util.AttributeSet;
import android.util.Log;
import android.view.DragEvent;

import com.google.blockly.android.ui.WorkspaceHelper;
import com.google.blockly.model.Field;
import com.google.blockly.model.FieldNumber;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

/**
 * A basic UI for {@link FieldNumber}.
 */
public class BasicFieldNumberView extends AppCompatEditText implements FieldView {
    private static final String TAG = "BasicFieldNumberView";

    protected DecimalFormatSymbols mLocalizedDecimalSymbols;
    protected String mLocalizedGroupingSeparator;
    protected NumberFormat mLocalizedNumberParser;

    protected boolean mAllowExponent = true;
    protected boolean mTextIsValid = false;

    private boolean mIsUpdatingField = false;

    private double mLatestMin = FieldNumber.NO_CONSTRAINT;
    private double mLatestMax = FieldNumber.NO_CONSTRAINT;
    private double mLatestPrecision = FieldNumber.NO_CONSTRAINT;
    private NumberFormat mLocalizedNumberFormat;

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
                        // Attempt to parse numbers using context's locale,
                        // ignoring the locale's grouping marker (b/c is causes parse errors).
                        String textWithoutGrouping =
                                text.toString().replace(mLocalizedGroupingSeparator, "");
                        try {
                            double newValue =
                                    mLocalizedNumberParser.parse(textWithoutGrouping).doubleValue();
                            mNumberField.setValue(newValue);
                            setTextValid(true);
                        } catch (ParseException e) {
                            // Failed to parse intermediate
                            setTextValid(false);
                        }
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

    private final Field.Observer mFieldObserver = new Field.Observer() {
        @Override
        public void onValueChanged(Field field, String oldStrValue, String newStrValue) {
            if (mIsUpdatingField) {
                return;
            }
            updateLocalizedNumberFormatIfConstraintsChanged();
            if (field != mNumberField) {  // Potential race condition if view's field changes.
                Log.w(TAG, "Received value change from unexpected field.");
                return;
            }
            try {
                CharSequence text = getText();
                Double value = mNumberField.getValue();

                // Because numbers can have different string representations,
                // only overwrite if the parsed value differs.
                if (TextUtils.isEmpty(text)
                        || mLocalizedNumberParser.parse(text.toString()).doubleValue() != value) {
                    setText(mLocalizedNumberFormat.format(mNumberField.getValue()));
                }
            } catch (ParseException e) {
                setText(mLocalizedNumberFormat.format(mNumberField.getValue()));
            }
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

        // Get a localized, but otherwise permissive
        mLocalizedDecimalSymbols = new DecimalFormatSymbols(getPrimaryLocale());
        mLocalizedGroupingSeparator =
                Character.toString(mLocalizedDecimalSymbols.getGroupingSeparator());
        mLocalizedNumberParser =
                new DecimalFormat("#.#", mLocalizedDecimalSymbols);

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
            updateLocalizedNumberFormatIfConstraintsChanged();
            setText(mLocalizedNumberFormat.format(mNumberField.getValue()));
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
            updateLocalizedNumberFormat();
            setText(mLocalizedNumberFormat.format(mNumberField.getValue()));
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
        if (WorkspaceHelper.isBlockDrag(getContext(), event)) {
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

            if (!mNumberField.hasMinimum() || mNumberField.getMinimumValue() < 0) {
                imeOptions |= InputType.TYPE_NUMBER_FLAG_SIGNED;
                allowedChars.append("-");
            }
            if (!mNumberField.isInteger()) {
                imeOptions |= InputType.TYPE_NUMBER_FLAG_DECIMAL;
                allowedChars.append(mLocalizedDecimalSymbols.getDecimalSeparator());
            }
            allowedChars.append(mLocalizedDecimalSymbols.getGroupingSeparator());

            setImeOptions(imeOptions);
            setKeyListener(DigitsKeyListener.getInstance(allowedChars.toString()));
        }
    }

    protected void updateLocalizedNumberFormat() {
        mLatestMin = mNumberField.getMinimumValue();
        mLatestMax = mNumberField.getMaximumValue();
        mLatestPrecision = mNumberField.getPrecision();
        mLocalizedNumberFormat = mNumberField.getNumberFormatForLocale(getPrimaryLocale());
    }

    protected void updateLocalizedNumberFormatIfConstraintsChanged() {
        if (mNumberField.getMinimumValue() != mLatestMin
                || mNumberField.getMaximumValue() != mLatestMax
                || mNumberField.getPrecision() != mLatestPrecision) {
            updateLocalizedNumberFormat();
        }
    }

    /**
     * @return The primary locale for the view's context.
     */
    private Locale getPrimaryLocale() {
        Configuration configuration = getContext().getResources().getConfiguration();
        if (Build.VERSION.SDK_INT >= 24) {
            return configuration.getLocales().get(0);
        } else {
            return configuration.locale;
        }
    }
}
