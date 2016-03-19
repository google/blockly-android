/*
 *  Copyright 2015 Google Inc. All Rights Reserved.
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

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;

/**
 * Renders the workspace coordinate grid.
 */
// TODO(#134): Make this a drawable. Assign as a background.
class WorkspaceGridRenderer {
    // Constants for drawing the coordinate grid.
    private static final int GRID_SPACING = 48;
    private static final int GRID_COLOR = 0xffa0a0a0;
    private static final int GRID_RADIUS = 2;

    // Fields for grid drawing.
    private final Paint mGridPaint = new Paint();
    private final Paint mCirclePaint = new Paint();
    private final Rect mTempRect = new Rect();
    private Bitmap mGridBitmap;

    WorkspaceGridRenderer() {
        mCirclePaint.setColor(GRID_COLOR);
    }

    /** @return Current grid spacing in pixels. */
    int getGridSpacing() {
        return GRID_SPACING;
    }

    /**
     * Using the current view scale, create a bitmap tiling shader to render the workspace grid.
     */
    void updateGridBitmap(float viewScale) {
        int gridSpacing = (int) (GRID_SPACING * viewScale);

        // For some reason, reusing the same Bitmap via Bitmap.reconfigure() leads to bad rendering,
        // so recycle existing Bitmap and create a new one instead.
        if (mGridBitmap != null) {
            mGridBitmap.recycle();
        }
        mGridBitmap = Bitmap.createBitmap(gridSpacing, gridSpacing, Bitmap.Config.ARGB_8888);

        Canvas bitmapCanvas = new Canvas(mGridBitmap);
        bitmapCanvas.drawCircle(GRID_RADIUS, GRID_RADIUS, GRID_RADIUS, mCirclePaint);

        mGridPaint.setShader(
                new BitmapShader(mGridBitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT));
    }

    /**
     * Draw grid.
     *
     * @param canvas The canvas to draw to.
     * @param width Total width of the grid to draw; this is the view width.
     * @param height Total height of the grid to draw; this is the view height.
     * @param offsetX Horizontal grid offset; this is the horizontal view scroll offset.
     * @param offsetY Vertical grid offset; this is the vertical view scroll offset.
     */
    void drawGrid(Canvas canvas, int width, int height, int offsetX, int offsetY) {
        mTempRect.set(offsetX - GRID_RADIUS, offsetY - GRID_RADIUS,
                width + offsetX, height + offsetY);
        canvas.drawRect(mTempRect, mGridPaint);
    }
}
