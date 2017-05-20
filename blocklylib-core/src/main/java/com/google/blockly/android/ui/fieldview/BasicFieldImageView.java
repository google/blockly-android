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

import com.google.blockly.model.Field;
import com.google.blockly.model.FieldImage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.regex.Pattern;

/**
 * Renders an image bitmap.
 */
public class BasicFieldImageView extends android.support.v7.widget.AppCompatImageView implements
        FieldView {
    private static String TAG = "BasicFieldImageView";

    private static final Pattern HTTP_URL_PATTERN = Pattern.compile("https?://.*");
    private static final Pattern DATA_URL_PATTERN = Pattern.compile("data:(.*)");
    private static final String FILE_ASSET_URL_PREFIX = "file:///android_assets/";

    protected final Field.Observer mFieldObserver = new Field.Observer() {
        @Override
        public void onValueChanged(Field field, String newValue, String oldValue) {
            String source = mImageField.getSource();
            if (source.equals(mImageSrc)) {
                updateViewSize();
            } else {
                startLoadingImage();
            }
        }
    };

    protected FieldImage mImageField;
    protected String mImageSrc = null;

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
            startLoadingImage();
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
     */
    protected void startLoadingImage() {
        final String source = mImageField.getSource();

        // Do I/O in the background
        new AsyncTask<String, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(String... strings) {
                try {
                    InputStream stream = getStreamForSource(source);
                    if (stream != null) {
                        try {
                            return BitmapFactory.decodeStream(stream);
                        } finally {
                            stream.close();
                        }
                    }
                    Log.w(TAG, "Unable to load image \"" + source + "\"");
                } catch (IOException e) {
                    Log.w(TAG, "Unable to load image \"" + source + "\"", e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                if (bitmap != null) {
                    setImageBitmap(bitmap);
                    mImageSrc = source;
                    updateViewSize();
                } else {
                    // TODO(#44): identify and bundle as a resource a suitable default
                    // "cannot load" bitmap.
                }
                requestLayout();
            }
        }.execute(source);
    }

    @VisibleForTesting
    InputStream getStreamForSource(String source) throws IOException {
        if (HTTP_URL_PATTERN.matcher(source).matches()) {
            return (InputStream) new URL(source).getContent();
        } else if (DATA_URL_PATTERN.matcher(source).matches()) {
            String imageDataBytes = source.substring(source.indexOf(",")+1);
            return new ByteArrayInputStream(
                    Base64.decode(imageDataBytes.getBytes(), Base64.DEFAULT));
        } else {
            String assetPath;
            if (source.startsWith(FILE_ASSET_URL_PREFIX)) {
                assetPath = source.substring(FILE_ASSET_URL_PREFIX.length());
            } else if (source.startsWith("/")) {
                assetPath = source.substring(1);
            } else {
                assetPath = source;
            }
            return getContext().getAssets().open(assetPath);
        }
    }

    protected void updateViewSize() {
        float density = getContext().getResources().getDisplayMetrics().density;
        setMinimumWidth((int) Math.ceil(mImageField.getWidth() * density));
        setMinimumHeight((int) Math.ceil(mImageField.getHeight() * density));
    }
}
