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
import android.util.AttributeSet;
import android.widget.ImageView;

import com.google.blockly.model.Field;
import com.google.blockly.model.FieldImage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Renders an image bitmap.
 */
public class BasicFieldImageView extends ImageView implements FieldView {
    protected final FieldImage.Observer mFieldObserver = new FieldImage.Observer() {
        @Override
        public void onImageChanged(FieldImage field, String newSource,
                                   int newWidth, int newHeight) {

            loadImageFromSource(newSource);
        }
    };

    protected FieldImage mImageField;

    /**
     * Constructs a new {@link BasicFieldImageView}.
     *
     * @param context The application's context.
     */
    public BasicFieldImageView(Context context) {
        super(context);
    }

    public BasicFieldImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BasicFieldImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setField(Field field) {
        FieldImage imageField = (FieldImage) field;
        if (mImageField == imageField) {
            return;
        }

        if (mImageField != null) {
            mImageField.unregisterObserver(mFieldObserver);
        }
        mImageField = imageField;
        if (mImageField != null) {
            loadImageFromSource(mImageField.getSource());
            mImageField.registerObserver(mFieldObserver);
        } else {
            // TODO(#44): Set image to default 'no image' default
        }
    }

    @Override
    public Field getField() {
        return mImageField;
    }

    @Override
    public void unlinkField() {
        setField(null);
    }

    /**
     * Asynchronously load and set image bitmap.
     * <p/>
     * If a bitmap cannot be read from the given source, a default bitmap is set instead.
     *
     * @param source The source URI of the image to load.
     */
    protected void loadImageFromSource(String source) {
        new AsyncTask<String, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(String... strings) {
                try {
                    final InputStream stream = (InputStream) new URL(strings[0]).getContent();
                    try {
                        return BitmapFactory.decodeStream(stream);
                    } finally {
                        stream.close();
                    }
                } catch (IOException ex) {
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                if (bitmap != null) {
                    setImageBitmap(bitmap);
                } else {
                    // TODO(#44): identify and bundle as a resource a suitable default
                    // "cannot load" bitmap.
                }
                requestLayout();
            }
        }.execute(source);
    }
}
