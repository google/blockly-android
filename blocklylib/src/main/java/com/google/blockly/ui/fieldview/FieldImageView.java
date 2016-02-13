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

package com.google.blockly.ui.fieldview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.ImageView;

import com.google.blockly.model.Field;
import com.google.blockly.ui.FieldWorkspaceParams;
import com.google.blockly.ui.WorkspaceHelper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Renders an image bitmap.
 */
public class FieldImageView extends ImageView implements FieldView {
    private final Field.FieldImage mImage;
    private final WorkspaceHelper mWorkspaceHelper;
    private final FieldWorkspaceParams mWorkspaceParams;

    public FieldImageView(Context context, Field field, WorkspaceHelper helper) {
        super(context);

        mImage = (Field.FieldImage) field;
        mWorkspaceHelper = helper;
        mWorkspaceParams = new FieldWorkspaceParams(mImage, mWorkspaceHelper);

        setBackground(null);
        loadImageFromSource(mImage.getSource());
        mImage.setView(this);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mWorkspaceParams.setMeasuredDimensions(getMeasuredWidth(), getMeasuredHeight());
    }

    @Override
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            mWorkspaceParams.updateFromView(this);
        }
    }

    @Override
    public FieldWorkspaceParams getWorkspaceParams() {
        return mWorkspaceParams;
    }

    /**
     * Asynchronously load and set image bitmap.
     * <p/>
     * If a bitmap cannot be read from the given source, a default bitmap is set instead.
     *
     * @param source The source URI of the image to load.
     */

    private void loadImageFromSource(String source) {
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
                    // TODO(357): identify and bundle as a resource a suitable default
                    // "cannot load" bitmap.
                }
                requestLayout();
            }
        }.execute(source);
    }

    @Override
    public void unlinkModel() {
        mImage.setView(null);
        // TODO(#381): Remove model from view. Set mImage to null, and handle null cases above.
    }
}
