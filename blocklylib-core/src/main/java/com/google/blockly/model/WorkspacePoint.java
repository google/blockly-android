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

package com.google.blockly.model;

import android.graphics.Point;
import android.graphics.PointF;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * A point in workspace coordinates.
 * <p/>
 * This class primarily serves as a typesafe means of enforcing the use of the correct type of
 * coordinate in a given context, i.e., it prevents accidental use of workspace coordinates instead
 * of view coordinates, and vice versa.
 */
public class WorkspacePoint extends PointF {
    /**
     * Delegate default constructor.
     */
    public WorkspacePoint() {
        super();
    }

    /**
     * Create from generic {@link Point}.
     */
    public WorkspacePoint(Point point) {
        super(point);
    }

    /**
     * Create from generic {@link PointF}.
     */
    public WorkspacePoint(PointF pointf) {
        super(pointf.x, pointf.y);
    }

    /**
     * Create from x, y int coordinates.
     */
    public WorkspacePoint(int x, int y) {
        super(x, y);
    }

    /**
     * Create from x, y float coordinates.
     */
    public WorkspacePoint(float x, float y) {
        super(x, y);
        if (Float.isNaN(x) || Float.isInfinite(x) || Float.isNaN(y) || Float.isInfinite(y)) {
            throw new IllegalArgumentException("Point must be a real, finite number.");
        }
    }

    /**
     * Set this point from an existing one.
     */
    public void setFrom(WorkspacePoint other) {
        x = other.x;
        y = other.y;
    }

    public static final Parcelable.Creator<WorkspacePoint> CREATOR =
            new Parcelable.Creator<WorkspacePoint>()
    {
        /**
         * Return a new point from the data in the specified parcel.
         */
        public WorkspacePoint createFromParcel(Parcel in) {
            WorkspacePoint r = new WorkspacePoint();
            r.readFromParcel(in);
            return r;
        }

        /**
         * Return an array of rectangles of the specified size.
         */
        public WorkspacePoint[] newArray(int size) {
            return new WorkspacePoint[size];
        }
    };
}
