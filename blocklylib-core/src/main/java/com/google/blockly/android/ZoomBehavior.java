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

package com.google.blockly.android;

import android.content.Context;
import android.content.res.TypedArray;

/**
 * ZoomBehavior captures the behavior of pan, zoom, and the presence of zoom buttons.
 */
public final class ZoomBehavior {
    private static final int BEHAVIOR_MIN = 1;
    private static final int BEHAVIOR_MAX = 5;

    //no scrollbar, no zoom, no buttons
    public static final int BEHAVIOR_FIXED = 1;
    //only scrollable, no buttons, no zoom
    public static final int BEHAVIOR_SCROLL_ONLY = 2;
    //scrollable, zoomable with buttons, zoom-in/out buttons
    public static final int BEHAVIOR_ZOOM_BUTTONS_ONLY = 3;
    //scrollable, zoomable, no buttons
    public static final int BEHAVIOR_ZOOM_PINCH_ONLY = 4;
    //scrollable, zoomable, zoom-in/out buttons
    public static final int BEHAVIOR_ZOOM_PINCH_AND_BUTTONS = 5;

    public static final int DEFAULT_BEHAVIOR = BEHAVIOR_ZOOM_PINCH_AND_BUTTONS;

    private int mBehavior = DEFAULT_BEHAVIOR;

    public static ZoomBehavior loadFromTheme(Context context) {

        TypedArray a = context.getTheme().obtainStyledAttributes(
                null,
                R.styleable.BlocklyWorkspaceTheme,
                0, 0);
        try {
            int attrValue =
                    a.getInt(R.styleable.BlocklyWorkspaceTheme_zoomBehavior, DEFAULT_BEHAVIOR);
            return new ZoomBehavior(attrValue);
        } finally {
            a.recycle();
        }
    }

    public ZoomBehavior(int value) {
        if (value < BEHAVIOR_MIN || value > BEHAVIOR_MAX) {
            throw new IllegalArgumentException("Illegal zoom value");
        }
        mBehavior = value;
    }

    /**
     * @return True if zoom-in/out buttons are enabled. Otherwise false.
     */
    public boolean isButtonEnabled(){
        return (mBehavior == BEHAVIOR_ZOOM_BUTTONS_ONLY
                || mBehavior == BEHAVIOR_ZOOM_PINCH_AND_BUTTONS);
    }

    /**
     * @return True if workspace is scrollable. Otherwise false.
     */
    public boolean isScrollEnabled(){
        return mBehavior >= BEHAVIOR_SCROLL_ONLY;
    }

    /**
     * @return True if workspace scalable using touch/pinch events. Otherwise false.
     */
    public boolean isPinchZoomEnabled(){
        return mBehavior == BEHAVIOR_ZOOM_PINCH_ONLY
                || mBehavior == BEHAVIOR_ZOOM_PINCH_AND_BUTTONS;
    }

    /**
     * @return True if the workspace is fixed (neither scalable nor scrollable). Otherwise false.
     */
    public boolean isFixed() {
        return mBehavior == BEHAVIOR_FIXED;
    }
}
