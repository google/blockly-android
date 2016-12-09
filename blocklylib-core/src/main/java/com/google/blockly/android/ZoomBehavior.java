package com.google.blockly.android;

import android.content.Context;
import android.content.res.TypedArray;

/**
 * Created by Surinder Singh (surinder83singh@gmail.com) on 09/12/16.
 */
public final class ZoomBehavior {

    //no scrollbar, no zoom, no buttons
    public static final int BEHAVIOR_FIXED = 1;
    //only scrollable, no buttons, no zoom
    public static final int BEHAVIOR_SCROLL_ONLY = 2;
    //scrollable, zoomable with buttons, zoom-in/out buttons
    public static final int BEHAVIOR_BUTTONS_ONLY = 3;
    //scrollable, zoomable, no buttons
    public static final int BEHAVIOR_ZOOM_ONLY = 4;
    //scrollable, zoomable, zoom-in/out buttons
    public static final int BEHAVIOR_ZOOM_AND_BUTTONS = 5;

    public static final int DEFAULT_BEHAVIOR = BEHAVIOR_ZOOM_AND_BUTTONS;

    private int mBehavior = DEFAULT_BEHAVIOR;


    public ZoomBehavior(Context context) {
        TypedArray a = context.getTheme().obtainStyledAttributes(
                null,
                R.styleable.BlocklyWorkspaceTheme,
                0, 0);
        try {
            mBehavior =
                    a.getInt(R.styleable.BlocklyWorkspaceTheme_zoomBehavior, DEFAULT_BEHAVIOR);
        } finally {
            a.recycle();
        }
    }

    /**
     * @return true if zoom-in/out buttons are enabled
     */
    public boolean isButtonEnabled(){
        return (mBehavior == BEHAVIOR_BUTTONS_ONLY || mBehavior == BEHAVIOR_ZOOM_AND_BUTTONS);
    }

    /**
     * @return true if workspace is scrollable
     */
    public boolean isScrollEnabled(){
        return mBehavior > BEHAVIOR_FIXED;
    }

    /**
     * @return true if workspace scalable using touch/pinch events
     */
    public boolean isPinchZoomEnabled(){
        return mBehavior > BEHAVIOR_BUTTONS_ONLY;
    }

}
