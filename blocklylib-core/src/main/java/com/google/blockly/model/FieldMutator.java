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

/**
 * Adds the mutator edit button to an Input. This should only be added to blocks that have a mutator
 * that {@link Mutator#hasUI() has UI}.
 */
public final class FieldMutator extends Field {
    private int mResId;
    private int mWidth;
    private int mHeight;
    private String mAltText;

    public FieldMutator(String name, int resId, int width, int height, String altText) {
        super(name, TYPE_ICON);
        mResId = resId;
        mWidth = width;
        mHeight = height;
        mAltText = altText;
    }

    @Override
    public FieldMutator clone() {
        return new FieldMutator(getName(), mResId, mWidth, mHeight, mAltText);
    }

    /**
     * @return The resource id for the edit icon.
     */
    public int getResId() {
        return mResId;
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
     * Sets a new image to be shown for the mutator edit icon.
     * <p/>
     * Changes will induce a {@link Observer#onValueChanged}, even though FieldMutators do not
     * store a value.  This trigger updates to the matching Fieldview, but in might also generate a
     * no-op {@link BlocklyEvent.ChangeEvent}.
     *
     * @param resId The resource id for the edit icon.
     * @param width The display width of the icon in dips.
     * @param height The display height of the icon in dips.
     */
    public void setImage(int resId, int width, int height) {
        if (resId != mResId || mWidth != width || mHeight != height) {
            mResId = resId;
            mWidth = width;
            mHeight = height;

            fireValueChanged("", "");
        }
    }

    @Override
    public boolean setFromString(String text) {
        throw new IllegalStateException("Mutator field cannot be set from string.");
    }

    @Override
    public String getSerializedValue() {
        return ""; // Mutator fields do not have value.
    }
}
