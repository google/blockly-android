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
import android.support.v4.util.SimpleArrayMap;

import com.google.blockly.model.Block;
import com.google.blockly.model.Connection;
import com.google.blockly.model.Field;
import com.google.blockly.model.Input;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores layout parameters for blocks in the workspace. Keeps track of the bounding box,
 * positioning of parts of a block, and other layout properties in workspace coordinates for easy
 * translation to the view space.
 */
public class BlockWorkspaceParams {
    // Connections just have a position that is stored in the model
    final ArrayList<Connection> mConnections = new ArrayList<>();

    final Block mBlock;
    final WorkspaceHelper mWorkspaceHelper;

    // The position of the rendered block in workspace coordinates
    private Point mPosition = new Point();
    // The bounding box for the rendered block in workspace coordinates
    private Rect mBounds = new Rect();
    // The width of the rendered block in workspace coordinates
    private int mWidth;
    // The height of the rendered block in workspace coordinates
    private int mHeight;

    public BlockWorkspaceParams(Block block, WorkspaceHelper workspaceHelper) {
        if (block == null) {
            throw new IllegalArgumentException("Block may not be null.");
        }
        if (workspaceHelper == null) {
            throw new IllegalArgumentException("WorkspaceHelper may not be null");
        }
        mBlock = block;
        mWorkspaceHelper = workspaceHelper;
        mBlock.setLayoutParameters(this);

        List<Input> inputs = block.getInputs();
        for (int i = 0; i < inputs.size(); i++) {
            Input input = inputs.get(i);
            if (input.getConnection() != null) {
                mConnections.add(input.getConnection());
            }
            List<Field> fields = input.getFields();
            for (int j = 0; j < fields.size(); j++) {
                Field field = fields.get(j);
                FieldWorkspaceParams fieldParams
                        = new FieldWorkspaceParams(field, mWorkspaceHelper);
            }
        }
    }

    /**
     * Set the measured dimensions of the block's view in pixels. The position is converted to
     * workspace units and used to update the bounding box in the workspace.
     *
     * @param viewDimens The width and height of the view in pixels..
     */
    public void setMeasuredDimensions(Point viewDimens) {
        mWidth = mWorkspaceHelper.viewToWorkspaceUnits(viewDimens.x);
        mHeight = mWorkspaceHelper.viewToWorkspaceUnits(viewDimens.y);
        mBounds.set(mPosition.x, mPosition.y + mHeight, mPosition.x + mWidth, mPosition.y);
    }

    /**
     * Set the position of the block's view in pixels. The position is converted to workspace
     * units and used to update the position and bounding box in the workspace.
     *
     * @param viewPosition The position of the block in the workspace view's coordinates.
     */
    public void setPosition(Point viewPosition) {
        mPosition.x = viewPosition.x;
        mPosition.y = viewPosition.y;
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