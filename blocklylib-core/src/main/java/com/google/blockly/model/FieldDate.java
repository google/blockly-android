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
import android.util.Log;

import org.json.JSONObject;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Adds a date picker to an Input. Dates must be in the format "YYYY-MM-DD"
 */
public final class FieldDate extends Field<FieldDate.Observer> {
    private static final String TAG = "FieldDate";

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private final Date mDate;

    public FieldDate(String name, String dateString) {
        super(name, TYPE_DATE);
        Date date = null;
        if (!TextUtils.isEmpty(dateString)) {
            try {
                date = DATE_FORMAT.parse(dateString);
            } catch (ParseException e) {
                Log.e(TAG, "Unable to parse date " + dateString, e);
            }
        }
        if (date == null) {
            date = new Date();
        }
        mDate = date;
    }

    public FieldDate(FieldDate other) {
        super(other.getName(), TYPE_DATE);
        mDate = (Date) other.mDate.clone();
    }


    public static FieldDate fromJson(JSONObject json) {
        return new FieldDate(
                json.optString("name", "NAME"),
                json.optString("date"));
    }

    @Override
    public FieldDate clone() {
        return new FieldDate(this);
    }

    @Override
    public boolean setFromString(String text) {
        Date date = null;
        try {
            date = DATE_FORMAT.parse(text);
            setDate(date);
            return true;
        } catch (ParseException e) {
            Log.e(TAG, "Unable to parse date " + text, e);
            return false;
        }
    }

    /**
     * @return The date in this field.
     */
    public Date getDate() {
        return mDate;
    }

    /**
     * Sets this field to the specified {@link Date}.
     */
    public void setDate(Date date) {
        if (date == null) {
            throw new IllegalArgumentException("Date may not be null.");
        }
        setTime(date.getTime());
    }

    /**
     * @return The string format for the date in this field.
     */
    public String getDateString() {
        return DATE_FORMAT.format(mDate);
    }

    /**
     * Sets this field to a specific time.
     *
     * @param millis The time in millis since UNIX Epoch.
     */
    public void setTime(long millis) {
        long oldTime = mDate.getTime();
        if (millis != oldTime) {
            mDate.setTime(millis);
            onDateChanged(oldTime, millis);
        }
    }

    @Override
    protected void serializeInner(XmlSerializer serializer) throws IOException {
        serializer.text(DATE_FORMAT.format(mDate));
    }

    private void onDateChanged(long oldMillis, long newMillis) {
        for (int i = 0; i < mObservers.size(); i++) {
            mObservers.get(i).onDateChanged(this, oldMillis, newMillis);
        }
    }

    /**
     * Observer for listening to changes to a date field.
     */
    public interface Observer {
        /**
         * Called when the field's date changed.
         *
         * @param field The field that changed.
         * @param oldMillis The field's previous time in UTC millis since epoch.
         * @param newMillis The field's new time in UTC millis since epoch.
         */
        void onDateChanged(Field field, long oldMillis, long newMillis);
    }
}
