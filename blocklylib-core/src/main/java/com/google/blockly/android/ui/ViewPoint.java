/*
 * Copyright  2015 Google Inc. All Rights Reserved.
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

package com.google.blockly.android.ui;

import android.graphics.Point;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * A point in view coordinates.
 * <p/>
 * This class primarily serves as a typesafe means of enforcing the use of the correct type of
 * coordinate in a given context, i.e., it prevents accidental use of view coordinates instead
 * of workspace coordinates, and vice versa.
 */
public class ViewPoint extends Point {
    /**
     * Delegate default constructor.
     */
    public ViewPoint() {
        super();
    }

    /**
     * Create from generic {@link Point}.
     */
    public ViewPoint(Point point) {
        super(point);
    }

    /**
     * Create from x, y coordinates.
     */
    public ViewPoint(int x, int y) {
        super(x, y);
    }

    /**
     * Set this point from an existing one.
     */
    public void setFrom(ViewPoint other) {
        x = other.x;
        y = other.y;
    }

    public static final Parcelable.Creator<ViewPoint> CREATOR = new Parcelable.Creator<ViewPoint>() {
        /**
         * Return a new point from the data in the specified parcel.
         */
        public ViewPoint createFromParcel(Parcel in) {
            ViewPoint r = new ViewPoint();
            r.readFromParcel(in);
            return r;
        }

        /**
         * Return an array of rectangles of the specified size.
         */
        public ViewPoint[] newArray(int size) {
            return new ViewPoint[size];
        }
    };
}
