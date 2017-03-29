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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.annotation.VisibleForTesting;
import android.util.AttributeSet;
import android.util.Base64;
import android.util.Log;
import android.view.View;

import com.google.blockly.model.Field;
import com.google.blockly.model.FieldMutator;
import com.google.blockly.model.Mutator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Renders an image bitmap.
 */
public class BasicFieldMutatorView extends android.support.v7.widget.AppCompatImageView implements
        FieldView {
    private static String TAG = "BasicFieldMutatorView";

    protected final Field.Observer mFieldObserver = new Field.Observer() {
        @Override
        public void onValueChanged(Field field, String newValue, String oldValue) {
            int source = mMutatorField.getResId();
            if (source == mImageSrc) {
                updateViewSize();
            } else {
                refreshDrawable();
            }
        }
    };

    protected FieldMutator mMutatorField;
    protected int mImageSrc = 0;

    /**
     * Constructs a new {@link BasicFieldMutatorView}.
     *
     * @param context The application's context.
     */
    public BasicFieldMutatorView(Context context) {
        super(context);
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMutatorField != null) {
                    Mutator mutator = mMutatorField.getBlock().getMutator();
                    if (mutator != null) {
                        mutator.toggleUI();
                    } else {
                        Log.e(TAG, "Clicked a mutator edit button, but there's no mutator for "
                                + mMutatorField.getBlock().getId());
                    }
                }
            }
        });
    }

    @Override
    public void setField(Field field) {
        FieldMutator mutatorField = (FieldMutator) field;
        if (mMutatorField == mutatorField) {
            return;
        }

        if (mMutatorField != null) {
            mMutatorField.unregisterObserver(mFieldObserver);
        }
        mMutatorField = mutatorField;
        if (mMutatorField != null) {
            refreshDrawable();
            mMutatorField.registerObserver(mFieldObserver);
        } else {
            // TODO(#44): Set image to default 'no image'
        }
    }

    @Override
    public Field getField() {
        return mMutatorField;
    }

    @Override
    public void unlinkField() {
        setField(null);
    }

    /**
     * Update the icon being shown for the mutator button.
     */
    protected void refreshDrawable() {
        setImageResource(mMutatorField.getResId());
    }

    protected void updateViewSize() {
        float density = getContext().getResources().getDisplayMetrics().density;
        setMinimumWidth((int) Math.ceil(mMutatorField.getWidth() * density));
        setMinimumHeight((int) Math.ceil(mMutatorField.getHeight() * density));
    }
}
