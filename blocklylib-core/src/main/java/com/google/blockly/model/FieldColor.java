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

import android.graphics.Color;
import android.text.TextUtils;

import com.google.blockly.utils.BlockLoadingException;

import org.json.JSONObject;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;

/**
 * Adds a color picker to an Input.
 */
public final class FieldColor extends Field<FieldColor.Observer> {
    public static final int DEFAULT_COLOR = 0xff0000;  // Red

    private int mColor;

    public FieldColor(String name) {
        this(name, DEFAULT_COLOR);
    }

    public FieldColor(String name, int color) {
        super(name, TYPE_COLOR);
        setColor(color);
    }

    public static FieldColor fromJson(JSONObject json) throws BlockLoadingException {
        String name = json.optString("name");
        if (TextUtils.isEmpty(name)) {
            throw new BlockLoadingException("field_colour \"name\" attribute must not be empty.");
        }
        int color = DEFAULT_COLOR;

        String colourString = json.optString("colour");
        if (!TextUtils.isEmpty(colourString)) {
            color = Color.parseColor(colourString);
        }
        return new FieldColor(name, color);
    }

    @Override
    public FieldColor clone() {
        return new FieldColor(getName(), mColor);
    }

    @Override
    public boolean setFromString(String text) {
        try {
            setColor(Color.parseColor(text));
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }

    /**
     * @return The current color in this field.
     */
    public int getColor() {
        return mColor;
    }

    /**
     * Sets the color stored in this field.
     *
     * @param color A color in the form 0xRRGGBB
     */
    public void setColor(int color) {
        final int newColor = 0xFFFFFF & color;
        if (mColor != newColor) {
            int oldColor = mColor;
            mColor = newColor;
            onColorChanged(oldColor, newColor);
        }
    }

    @Override
    public String getSerializedValue() {
        return String.format("#%02x%02x%02x",
                Color.red(mColor), Color.green(mColor), Color.blue(mColor));
    }

    private void onColorChanged(int oldColor, int newColor) {
        for (int i = 0; i < mObservers.size(); i++) {
            mObservers.get(i).onColorChanged(this, oldColor, newColor);
        }
    }

    /**
     * Observer for listening to changes to a color field.
     */
    public interface Observer {
        /**
         * Called when the field's color changed.
         *
         * @param field The field that changed.
         * @param oldColor The field's previous color.
         * @param newColor The field's new color.
         */
        void onColorChanged(Field field, int oldColor, int newColor);
    }
}
