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

package com.google.blockly.model;

import android.text.TextUtils;

import org.json.JSONObject;
import org.xmlpull.v1.XmlSerializer;

/**
 * Adds an image to an Input.
 */
public final class FieldImage extends Field<FieldImage.Observer> {
    private String mSrc;
    private int mWidth;
    private int mHeight;
    private String mAltText;

    public FieldImage(String name, String src, int width, int height, String altText) {
        super(name, TYPE_IMAGE);
        mSrc = src;
        mWidth = width;
        mHeight = height;
        mAltText = altText;
    }

    public static FieldImage fromJson(JSONObject json) {
        return new FieldImage(json.optString("name"),
                json.optString("src", "https://www.gstatic.com/codesite/ph/images/star_on.gif"),
                json.optInt("width", 15), json.optInt("height", 15),
                json.optString("alt", "*"));
    }

    @Override
    public FieldImage clone() {
        return new FieldImage(getName(), mSrc, mWidth, mHeight, mAltText);
    }

    /**
     * @return The source for the image.
     */
    public String getSource() {
        return mSrc;
    }

    /**
     * @return The display width of the image in dips.
     */
    public int getWidth() {
        return mWidth;
    }

    /**
     * @return The display height of the image in dips.
     */
    public int getHeight() {
        return mHeight;
    }

    /**
     * @return The alt-text for the image.
     */
    public String getAltText() {
        return mAltText;
    }

    /**
     * Sets the alt-text for the image.
     */
    public void setAltText(String altText) {
        mAltText = altText;
    }

    /**
     * Sets a new image to be shown.
     *
     * @param src A web address or Blockly reference to the image.
     * @param width The display width of the image in dips.
     * @param height The display height of the image in dips.
     */
    public void setImage(String src, int width, int height) {
        if (!TextUtils.equals(mSrc, src) || mWidth != width || mHeight != height) {
            mSrc = src;
            mWidth = width;
            mHeight = height;
            onImageChanged(src, width, height);
        }
    }

    @Override
    public boolean setFromString(String text) {
        throw new IllegalStateException("Image field cannot be set from string.");
    }

    @Override
    public String getSerializedValue() {
        return ""; // Image fields do not have value.
    }

    private void onImageChanged(String newSource, int newWidth, int newHeight) {
        for (int i = 0; i < mObservers.size(); i++) {
            mObservers.get(i).onImageChanged(this, newSource, newWidth, newHeight);
        }
    }

    /**
     * Observer for listening to changes to an image field.
     */
    public interface Observer {
        /**
         * Called when the field's image source, width, or height was changed.
         * <p>
         * Note: Image fields are not expected to be user editable and are not serialized by
         * Blockly's core library.
         *
         * @param field The field that changed.
         * @param newSource The new source for the image.
         * @param newWidth The new width of the image.
         * @param newHeight The new height of the image.
         */
        void onImageChanged(FieldImage field, String newSource, int newWidth, int newHeight);
    }
}
