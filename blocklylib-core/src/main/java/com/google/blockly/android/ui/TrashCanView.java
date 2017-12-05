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
package com.google.blockly.android.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.IntDef;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;

import com.google.blockly.android.R;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Drop target view for deleting blocks via a drag gesture, with animation.
 * <p/>
 * This view has two layout attributes, {@code closedIcon} and {@code openedIcon}. Each is a
 * reference to a drawable resource for one of the two trash states, closed (default/idle state) and
 * opened (pending drop during drag).  The TrashCanView assumes both drawables are the same size, so
 * the overall view will not change size when the state changes.
 */
public class TrashCanView extends AppCompatImageView {
    private static final String TAG = "TrashCanView";

    @Retention(SOURCE)
    @IntDef({STATE_DEFAULT, STATE_ON_HOVER})
    public @interface HoverState {}
    private static final int STATE_DEFAULT = 0;
    private static final int STATE_ON_HOVER = 1;

    protected int mState = STATE_DEFAULT;
    protected Drawable mDefaultDrawable;
    protected Drawable mOnHoverDrawable;

    public TrashCanView(Context context) {
        this(context, null, 0);
    }

    public TrashCanView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TrashCanView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.TrashCanView,
                0, 0);
        try {
            //noinspection ResourceType
            setDefaultIcon(a.getResourceId(
                    R.styleable.TrashCanView_defaultIcon, R.drawable.blockly_trash));
            setOnHoverIcon(a.getResourceId(
                    R.styleable.TrashCanView_onHoverIcon, R.drawable.blockly_trash_open));
        } finally {
            a.recycle();
        }
    }

    @Override
    public void setOnDragListener(final View.OnDragListener dragListener) {
        View.OnDragListener wrapper = new OnDragListener() {
            @Override
            public boolean onDrag(View view, DragEvent dragEvent) {
                int action = dragEvent.getAction();
                // Whether the dragged object can be handled by the trash.
                boolean result = dragListener.onDrag(view, dragEvent);
                if (action == DragEvent.ACTION_DRAG_ENDED) {
                    setState(STATE_DEFAULT);
                } else  if (result) {
                    switch (action) {
                        case DragEvent.ACTION_DRAG_ENTERED:
                            setState(STATE_ON_HOVER);
                            break;
                        case DragEvent.ACTION_DROP:
                        case DragEvent.ACTION_DRAG_EXITED:
                            setState(STATE_DEFAULT);
                            break;
                    }
                }
                return result;
            }
        };
        super.setOnDragListener(wrapper);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setState(STATE_DEFAULT);
    }

    /**
     * Assigns a drawable resource id for the default state, when no block is hovering with a
     * pending drop. Usually, this is a closed trashcan.
     *
     * @param drawableRes Default drawable resource id.
     */
    public void setDefaultIcon(int drawableRes) {
        setDefaultIcon(ContextCompat.getDrawable(getContext(), drawableRes));
    }

    /**
     * Assigns a drawable resource if for the default state, when no block is hovering with a
     * pending drop. Usually, this is a closed trashcan.
     *
     * @param drawable Default drawable.
     */
    public void setDefaultIcon(Drawable drawable) {
        mDefaultDrawable = drawable;
        if (mState == STATE_DEFAULT) {
            setImageDrawable(mDefaultDrawable);
        }
    }

    /**
     * Assigns a drawable resource id for the hovered state, when a block has been dragged above
     * this view. Usually, this is a open trashcan.
     *
     * @param drawableRes Drawable resource id for the hovered
     */
    public void setOnHoverIcon(int drawableRes) {
        setOnHoverIcon(ContextCompat.getDrawable(getContext(), drawableRes));
    }

    /**
     * Assigns a drawable for the hovered state, when a block has been dragged above this view.
     * Usually, this is a open trashcan.
     *
     * @param drawable
     */
    public void setOnHoverIcon(Drawable drawable) {
        mOnHoverDrawable = drawable;
        if (mState == STATE_ON_HOVER) {
            setImageDrawable(mOnHoverDrawable);
        }
    }

    public void setState(@HoverState int state) {
        mState = state;
        switch (state) {
            default:
                Log.w(TAG, "Invalid state: " + state);
                // continue to default
            case STATE_DEFAULT:
                setImageDrawable(mDefaultDrawable);
                break;
            case STATE_ON_HOVER:
                setImageDrawable(mOnHoverDrawable);
                break;
        }
    }
}
