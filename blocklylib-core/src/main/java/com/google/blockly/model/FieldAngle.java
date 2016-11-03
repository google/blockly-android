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

/**
 * Adds an angle (0-360) picker to an Input.
 */
public final class FieldAngle extends Field {
    private static final float WRAP_ANGLE = 360;

    private float mAngle;

    public FieldAngle(String name, float angle) {
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
            setAngle(Float.parseFloat(text));
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    /**
     * @return The angle set by the user.
     */
    public float getAngle() {
        return mAngle;
    }

    /**
     * Set the current angle in this field. The angle will be wrapped to be in the range
     * 0-360.
     *
     * @param angle The angle to set this field to.
     */
    public void setAngle(float angle) {
        if (Float.isNaN(angle)) {
            return;
        }
        angle = angle % 360;
        if (angle < 0) {
            angle += 360;
        }
        if (angle > WRAP_ANGLE) {
            angle -= 360;
        }

        if (angle != mAngle) {
            String oldValue = getSerializedValue();
            mAngle = angle;
            String newValue = getSerializedValue();
            fireValueChanged(oldValue, newValue);
        }
    }

    @Override
    public String getSerializedValue() {
        if (mAngle % 1 == 0.0) {
            // Don't print the decimal for integer values.
            return FieldNumber.INTEGER_DECIMAL_FORMAT.format(mAngle);
        } else {
            return Double.toString(mAngle);
        }
    }
}
