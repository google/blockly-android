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

import android.graphics.Rect;
import android.view.View;

import com.google.blockly.model.Field;
import com.google.blockly.model.WorkspacePoint;


/**
 * Stores layout parameters for fields in the workspace. Keeps track of the bounding box and
 * position in workspace coordinates for easy translation to the view space.
 */
public class FieldWorkspaceParams {
    private final Field mField;
    private final WorkspaceHelper mWorkspaceHelper;

    private final WorkspacePoint mWorkspacePosition = new WorkspacePoint();
    private final Rect mBounds = new Rect();
    // Helper object for setting mWorkspacePosition in updateFromView(); instantiated at class
    // level to avoid repeated object creation.
    private final ViewPoint mGlobalViewPosition = new ViewPoint();
    private int mWidth;
    private int mHeight;

    public FieldWorkspaceParams(Field field, WorkspaceHelper workspaceHelper) {
        if (field == null) {
            throw new IllegalArgumentException("Field may not be null.");
        }
        if (workspaceHelper == null) {
            throw new IllegalArgumentException("WorkspaceHelper may not be null.");
        }
        mField = field;
        mWorkspaceHelper = workspaceHelper;
        field.setLayoutParameters(this);
    }

    /**
     * Set the measured dimensions of the block's view in pixels. They will be converted to
     * workspace units and the bounding box will be updated.
     *
     * @param width The width of the block in pixels.
     * @param height The height of the block in pixels.
     */
    public void setMeasuredDimensions(int width, int height) {
        mWidth = mWorkspaceHelper.viewToWorkspaceUnits(width);
        mHeight = mWorkspaceHelper.viewToWorkspaceUnits(height);
        mBounds.set(
                mWorkspacePosition.x, mWorkspacePosition.y + mHeight,
                mWorkspacePosition.x + mWidth, mWorkspacePosition.y);
    }

    /**
     * Set the position of the block's view in pixels. The position will be converted to workspace
     * units and the bounding box will be updated.
     *
     * @param viewPosition The x, y position fo the block's view in pixels.
     */
    public void setPosition(ViewPoint viewPosition) {
        mWorkspaceHelper.viewToWorkspaceCoordinates(viewPosition, mWorkspacePosition);
        mBounds.set(
                mWorkspacePosition.x, mWorkspacePosition.y + mHeight,
                mWorkspacePosition.x + mWidth, mWorkspacePosition.y);
    }

    /**
     * @return The current bounds of this block in workspace coordinates.
     */
    public Rect getWorkspaceBounds() {
        return mBounds;
    }

    /**
     * @return The top left (top right in RtL) corner of this block in workspace coordinates.
     */
    public WorkspacePoint getWorkspacePosition() {
        return mWorkspacePosition;
    }

    /**
     * Update workspace coordinates based on the new view coordinates of the {@link View}.
     *
     * @param view The view associated with the field handled by this instance.
     */
    public void updateFromView(View view) {
        mWorkspaceHelper.getWorkspaceCoordinates(view, mWorkspacePosition);
    }
}