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

import android.graphics.Point;
import android.graphics.Rect;

import com.google.blockly.model.Field;


/**
 * Stores layout parameters for fields in the workspace. Keeps track of the bounding box and
 * position in workspace coordinates for easy translation to the view space.
 */
public class FieldWorkspaceParams {
    private final Field mField;
    private final WorkspaceHelper mWorkspaceHelper;

    private Point mPosition = new Point();
    private Rect mBounds = new Rect();
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
        mBounds.set(mPosition.x, mPosition.y + mHeight, mPosition.x + mWidth, mPosition.y);
    }

    /**
     * Set the position of the block's view in pixels. The position will be converted to workspace
     * units and the bounding box will be updated.
     *
     * @param x The x position fo the block's view in pixels.
     * @param y The y position of the block's view in pixels.
     */
    public void setPosition(int x, int y) {
        mPosition.x = x;
        mPosition.y = y;
        mWorkspaceHelper.viewToWorkspaceCoordinates(mPosition, mPosition);
        mBounds.set(mPosition.x, mPosition.y + mHeight, mPosition.x + mWidth, mPosition.y);
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
    public Point getWorkspacePosition() {
        return mPosition;
    }
}