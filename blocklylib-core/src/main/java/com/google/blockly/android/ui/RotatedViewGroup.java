/*
 *  Copyright 2016 Google Inc. All Rights Reserved.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.google.blockly.android.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

/**
 * ViewGroup that can rotate a child view by 90&ordm; either clockwise or counter-clockwise.
 * While not fully supported, if the view has multiple children, they will all be rotated in the
 * same direction, just within the {@code RotatedViewGroup's} padding.
 */
public class RotatedViewGroup extends ViewGroup {
    private @Rotation.Enum int mRotation = Rotation.NONE;

    private boolean mRotationChanged = true;
    private final Matrix mEventTransformMatrix = new Matrix();
    private final Matrix mDrawTransformMatrix = new Matrix();  // Inverse of Event transform.
    private final Rect mChildLayoutRect = new Rect();
    private final float[] mViewTouchPoint = new float[2];
    private final float[] mChildTouchPoint = new float[2];

    public RotatedViewGroup(Context context) {
        super(context);
    }

    public RotatedViewGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RotatedViewGroup(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        dispatchConfigurationChanged(context.getResources().getConfiguration());  // Necessary?
    }

    @Rotation.Enum
    public int getChildRotation() {
        return mRotation;
    }

    public void setChildRotation(@Rotation.Enum int rotation) {
        if (mRotation != rotation) {
            mRotation = rotation;
            mRotationChanged = true;
            requestLayout();
        }
    }

    public boolean isChildRotated() {
        return Rotation.isRotated(mRotation);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        mViewTouchPoint[0] = event.getX();
        mViewTouchPoint[1] = event.getY();

        mEventTransformMatrix.mapPoints(mChildTouchPoint, mViewTouchPoint);
        event.setLocation(mChildTouchPoint[0], mChildTouchPoint[1]);
        boolean result = super.dispatchTouchEvent(event);
        event.setLocation(mViewTouchPoint[0], mViewTouchPoint[1]);

        return result;
    }

    @Override
    public ViewParent invalidateChildInParent(int[] location, Rect dirty) {
        invalidate();
        return super.invalidateChildInParent(location, dirty);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int horzPadding = getPaddingLeft() + getPaddingRight();
        int vertPadding = getPaddingTop() + getPaddingBottom();

        int childWidthMSpec, childHeightMSpec;
        if (isChildRotated()) {
            // Swap both measure specs, and subtracted paddings.
            childWidthMSpec = MeasureSpec.makeMeasureSpec(
                    Math.max(MeasureSpec.getSize(heightMeasureSpec) - vertPadding, 0),
                    MeasureSpec.getMode(heightMeasureSpec));
            childHeightMSpec = MeasureSpec.makeMeasureSpec(
                    Math.max(MeasureSpec.getSize(widthMeasureSpec) - horzPadding, 0),
                    MeasureSpec.getMode(widthMeasureSpec));
        } else {
            // Subtract the paddings from measure specs.
            childWidthMSpec = MeasureSpec.makeMeasureSpec(
                    Math.max(MeasureSpec.getSize(widthMeasureSpec) - horzPadding, 0),
                    MeasureSpec.getMode(widthMeasureSpec));
            childHeightMSpec = MeasureSpec.makeMeasureSpec(
                    Math.max(MeasureSpec.getSize(heightMeasureSpec) - vertPadding, 0),
                    MeasureSpec.getMode(heightMeasureSpec));
        }

        int maxChildWidth = 0, maxChildHeight = 0;
        int childCount = getChildCount();
        for (int i = 0; i < childCount; ++i) {
            View child = getChildAt(i);
            child.measure(childWidthMSpec, childHeightMSpec);
            maxChildWidth = Math.max(maxChildWidth, child.getMeasuredWidth());
            maxChildHeight = Math.max(maxChildHeight, child.getMeasuredHeight());
        }

        if (isChildRotated()) {
            setMeasuredDimension(maxChildHeight + horzPadding, maxChildWidth + vertPadding);
        } else {
            setMeasuredDimension(maxChildWidth + horzPadding, maxChildHeight + vertPadding);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (changed || mRotationChanged) {
            int width = right - left;
            int height = bottom - top;

            switch (Rotation.normalize(mRotation, this)) {
                default:
                case Rotation.NONE:
                    mChildLayoutRect.set(getPaddingLeft(), getPaddingTop(),
                            width - getPaddingRight(), height - getPaddingBottom());
                    mEventTransformMatrix.reset();
                    mDrawTransformMatrix.reset();
                    break;

                case Rotation.CLOCKWISE:
                    mChildLayoutRect.set(getPaddingTop(), getPaddingRight(),
                            height - getPaddingBottom(), width - getPaddingLeft());
                    mEventTransformMatrix.setRotate(-90);
                    mEventTransformMatrix.postTranslate(0, width);
                    mEventTransformMatrix.invert(mDrawTransformMatrix);
                    break;

                case Rotation.COUNTER_CLOCKWISE:
                    mChildLayoutRect.set(getPaddingBottom(), getPaddingLeft(),
                            height - getPaddingTop(), width - getPaddingRight());
                    mEventTransformMatrix.setRotate(90);
                    mEventTransformMatrix.postTranslate(height, 0);
                    mEventTransformMatrix.invert(mDrawTransformMatrix);
                    break;
            }
            mRotationChanged = true;
        }

        int childCount = getChildCount();
        for (int i = 0; i < childCount; ++i) {
            View child = getChildAt(i);
            child.layout(mChildLayoutRect.left, mChildLayoutRect.top, mChildLayoutRect.right,
                    mChildLayoutRect.bottom);
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        canvas.save();
        canvas.concat(mDrawTransformMatrix);
        super.dispatchDraw(canvas);
        canvas.restore();
    }
}
