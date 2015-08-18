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

package com.google.blockly.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Build;
import android.util.Log;
import android.view.View;

/**
 * Provides helper methods for converting coordinates between the workspace and the views.
 */
public class WorkspaceHelper {
    private static final String TAG = "WorkspaceHelper";

    // TODO: Pull from config file
    private static final float SCALE_MIN = 0.1f;
    private static final float SCALE_MAX = 3f;

    private float mScale = 1;
    private float mDensity;
    private Point mOffset;
    private Point mViewSize;
    private boolean mRtL;

    /**
     * Create a helper for doing workspace to view conversions.
     *
     * @param context The current context to get display metrics from.
     * @param leftOffset The left offset into the workspace in workspace units.
     * @param topOffset The top offset into the workspace in workspace units.
     */
    public WorkspaceHelper(Context context, int leftOffset, int topOffset) {
        Resources res = context.getResources();
        mDensity = res.getDisplayMetrics().density;
        if (mDensity == 0) {
            Log.e(TAG, "Density is not defined for this context. Defaulting to 1.");
            mDensity = 1f;
        }
        mOffset = new Point(leftOffset, topOffset);
        mViewSize = new Point();
    }

    /**
     * Sets the scaling of the view. This scaling should be used for all drawing within the
     * {@link WorkspaceView} and children. A value of 1 means default size with no scaling. Values
     * larger than 1 will increase the size of drawn blocks and the grid, while a size smaller than
     * 1 will decrease the size and allow more blocks to fit on the screen.
     *
     * @param scale The scale of the view.
     */
    public void setScale(float scale) {
        mScale = Math.min(SCALE_MAX, Math.max(SCALE_MIN, scale));
    }

    /**
     * Set the {@link WorkspaceView}'s offset into the workspace, in workspace units. This value
     * should only change due to translation, not scaling, and therefore is not a pixel value.
     *
     * @param workspaceOffset The view's offset into the workspace in workspace coordinates.
     */
    public void setOffset(Point workspaceOffset) {
        mOffset.x = workspaceOffset.x;
        mOffset.y = workspaceOffset.y;
    }

    /**
     * Set the size of the view window. This is required for RtL languages to be laid out correctly.
     * This should be called by the workspace view in onLayout.
     *
     * @param viewDimens The width and height of the workspace view in pixels.
     */
    public void setViewSize(Point viewDimens) {
        mViewSize.x = viewDimens.x;
        mViewSize.y = viewDimens.y;
    }

    /**
     * @return The current scale of the workspace view.
     */
    public float getScale() {
        return mScale;
    }

    /**
     * Get the current view's offset in workspace coordinates. If you want the pixel offset of the
     * view you should use {@link #getViewOffset()} instead.
     *
     * @return The top left corner the viewport in workspace coordinates.
     */
    public Point getOffset() {
        return mOffset;
    }

    /**
     * Get the current offset of the view in pixels. If you want the offset in workspace coordinates
     * {@link #getOffset()} should be used instead.
     *
     * @return The top left corner of the viewport in pixels.
     */
    public Point getViewOffset() {
        return new Point(workspaceToViewUnits(mOffset.x), workspaceToViewUnits(mOffset.y));
    }

    /**
     * @return The current size of the view in pixels.
     */
    public Point getViewSize() {
        return mViewSize;
    }

    /**
     * Scales a value in workspace coordinates to a view pixel value. This does not account for
     * offsets into the view's space, it only uses scaling and screen density to calculate the
     * result.
     *
     * @param workspaceValue The value in workspace units.
     * @return The value in view pixels.
     */
    public int workspaceToViewUnits(int workspaceValue) {
        return (int) (mScale * mDensity * workspaceValue);
    }

    /**
     * Scales a value in view pixels to workspace units. This does not account for offsets into the
     * view's space, it only uses scaling and screen density to calculate the result.
     *
     * @param viewValue The value in pixels in the view.
     * @return The value in workspace units.
     */
    public int viewToWorkspaceUnits(int viewValue) {
        return (int) (viewValue / (mScale * mDensity));
    }

    /**
     * Converts a point in workspace coordinates to view coordinates, storing the result in the
     * second parameter. The resulting coordinates will be in the
     * {@link WorkspaceView WorkspaceView's} coordinates in pixels and include the current offset
     * into the workspace.
     *
     * @param workspacePosition The position to convert to view coordinates.
     * @param viewPosition The Point to store the results in.
     */
    public void workspaceToViewCoordinates(Point workspacePosition, Point viewPosition) {
        viewPosition.x = workspaceToViewUnits(workspacePosition.x - mOffset.x);
        viewPosition.y = workspaceToViewUnits(workspacePosition.y - mOffset.y);

        if (useRtL()) {
            viewPosition.x = mViewSize.x - viewPosition.x;
        }
    }

    /**
     * Converts a point in view coordinates to workspace coordinates, storing the result in the
     * second parameter. The view position should be in the {@link WorkspaceView WorkspaceView's}
     * coordinates in pixels.
     *
     * @param viewPosition The position to convert to workspace coordinates.
     * @param workspacePosition The Point to store the results in.
     */
    public void viewToWorkspaceCoordinates(Point viewPosition, Point workspacePosition) {
        int viewX = viewPosition.x;
        if (useRtL()) {
            viewX = mViewSize.x - viewX;
        }
        workspacePosition.x = viewToWorkspaceUnits(viewX) + mOffset.x;
        workspacePosition.y = viewToWorkspaceUnits(viewPosition.y) + mOffset.y;
    }

    /**
     * @return True if using Right to Left layout, false otherwise.
     */
    public boolean useRtL() {
        return mRtL;
    }

    /**
     * Updates the current RtL state for the app.
     *
     * @param context The context to get the RtL setting from.
     */
    private void updateRtL(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            mRtL = context.getResources().getConfiguration().getLayoutDirection()
                    == View.LAYOUT_DIRECTION_RTL;
        } else {
            // TODO: Handle pre 17 versions.
            mRtL = false;
        }
    }
}
