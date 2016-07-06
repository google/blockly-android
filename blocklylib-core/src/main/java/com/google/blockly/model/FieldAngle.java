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

import com.google.blockly.utils.BlockLoadingException;

import org.json.JSONObject;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;

/**
 * Adds an angle (0-360) picker to an Input.
 */
public final class FieldAngle extends Field<FieldAngle.Observer> {
    private int mAngle;

    public FieldAngle(String name, int angle) {
        super(name, TYPE_ANGLE);
        setAngle(angle);
    }

    public static FieldAngle fromJson(JSONObject json) throws BlockLoadingException {
        String name = json.optString("name");
        if (TextUtils.isEmpty(name)) {
            throw new BlockLoadingException("field_angle \"name\" attribute must not be empty.");
        }

        return new FieldAngle(name, json.optInt("angle", 90));
    }

    @Override
    public FieldAngle clone() {
        return new FieldAngle(getName(), mAngle);
    }

    @Override
    public boolean setFromString(String text) {
        try {
            setAngle(Integer.parseInt(text));
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    /**
     * @return The angle set by the user.
     */
    public int getAngle() {
        return mAngle;
    }

    /**
     * Set the current angle in this field. The angle will be wrapped to be in the range
     * 0-360.
     *
     * @param angle The angle to set this field to.
     */
    public void setAngle(int angle) {
        int newAngle;
        if (angle == 360) {
            newAngle = angle;
        } else {
            angle = angle % 360;
            if (angle < 0) {
                angle += 360;
            }
            newAngle = angle;
        }

        if (newAngle != mAngle) {
            int oldAngle = mAngle;
            mAngle = newAngle;
            onAngleChanged(oldAngle, newAngle);
        }
    }

    @Override
    public String getSerializedValue() {
        return Integer.toString(mAngle);
    }

    private void onAngleChanged(int oldAngle, int newAngle) {
        for (int i = 0; i < mObservers.size(); i++) {
            mObservers.get(i).onAngleChanged(this, oldAngle, newAngle);
        }
    }

    /**
     * Observer for listening to changes to an angle field.
     */
    public interface Observer {
        /**
         * Called when the field's angle changed.
         *
         * @param field The field that changed.
         * @param oldAngle The field's previous angle.
         * @param newAngle The field's new angle.
         */
        void onAngleChanged(Field field, int oldAngle, int newAngle);
    }
}
