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
 * Adds a colour picker to an Input.
 */
public final class FieldColour extends Field<FieldColour.Observer> {
    public static final int DEFAULT_COLOUR = 0xff0000;  // Red

    private int mColour;

    public FieldColour(String name) {
        this(name, DEFAULT_COLOUR);
    }

    public FieldColour(String name, int colour) {
        super(name, TYPE_COLOUR);
        setColour(colour);
    }

    public static FieldColour fromJson(JSONObject json) throws BlockLoadingException {
        String name = json.optString("name");
        if (TextUtils.isEmpty(name)) {
            throw new BlockLoadingException("field_colour \"name\" attribute must not be empty.");
        }
        int colour = DEFAULT_COLOUR;

        String colourString = json.optString("colour");
        if (!TextUtils.isEmpty(colourString)) {
            colour = Color.parseColor(colourString);
        }
        return new FieldColour(name, colour);
    }

    @Override
    public FieldColour clone() {
        return new FieldColour(getName(), mColour);
    }

    @Override
    public boolean setFromString(String text) {
        try {
            setColour(Color.parseColor(text));
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }

    /**
     * @return The current colour in this field.
     */
    public int getColour() {
        return mColour;
    }

    /**
     * Sets the colour stored in this field.
     *
     * @param colour A colour in the form 0xRRGGBB
     */
    public void setColour(int colour) {
        final int newColour = 0xFFFFFF & colour;
        if (mColour != newColour) {
            int oldColour = mColour;
            mColour = newColour;
            onColourChanged(oldColour, newColour);
        }
    }

    @Override
    protected void serializeInner(XmlSerializer serializer) throws IOException {
        serializer.text(String.format("#%02x%02x%02x",
                Color.red(mColour), Color.green(mColour), Color.blue(mColour)));
    }

    private void onColourChanged(int oldColour, int newColour) {
        for (int i = 0; i < mObservers.size(); i++) {
            mObservers.get(i).onColourChanged(this, oldColour, newColour);
        }
    }

    /**
     * Observer for listening to changes to a colour field.
     */
    public interface Observer {
        /**
         * Called when the field's colour changed.
         *
         * @param field The field that changed.
         * @param oldColour The field's previous colour.
         * @param newColour The field's new colour.
         */
        void onColourChanged(Field field, int oldColour, int newColour);
    }
}
