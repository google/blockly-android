
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
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.google.blockly.model.Workspace;

/**
 * Handles updating the viewport into the workspace and is the parent view for all blocks. This
 * view is responsible for handling drags. A drag on the workspace will move the viewport and a
 * drag on a block or stack of blocks will drag those within the workspace.
 */
public class WorkspaceView extends ViewGroup {
    private static final String TAG = "WorkspaceView";
    private static final boolean DEBUG = false;

    private static final int DEFAULT_GRID_SPACING = 48;
    private static final int GRID_COLOR = 0xffa0a0a0;
    private static final int GRID_RADIUS = 2;
    private static final float MIN_SCALE_TO_DRAW_GRID = 0.2f;


    private Workspace mWorkspace;
    private WorkspaceHelper mHelper;

    // Set to true when something changes that requires a fresh layout pass, such as a block being
    // added or removed. Moving blocks within the workspace should not require a fresh layout pass.
    private boolean mNeedLayout = false;

    private Paint mPaint;
    private int mGridSpacing = DEFAULT_GRID_SPACING;
    private boolean mDrawGrid = true;

    public WorkspaceView(Context context) {
        this(context, null);
    }

    public WorkspaceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WorkspaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mPaint = new Paint();
        mPaint.setColor(GRID_COLOR);
        setWillNotDraw(false);
        mHelper = new WorkspaceHelper(context, 0, 0);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (!changed && !mNeedLayout) {
            return;
        }
        mNeedLayout = false;
        int childCount = getChildCount();
        int measureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);

        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }

            child.measure(measureSpec, measureSpec);
            int childL = (int) child.getX();
            int childT = (int) child.getY();
            int childR = childL + child.getMeasuredWidth();
            int childB = childT + child.getMeasuredHeight();

            // Layout child views where they want to be. We'll enforce positioning when children
            // are moved by the user.
            child.layout(childL, childT, childR, childB);
        }
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredWidth = 2048; // TODO: replace with configurable values or better defaults
        int desiredHeight = 4096;
        int width, height;

        int mode = MeasureSpec.getMode(widthMeasureSpec);
        int size = MeasureSpec.getSize(widthMeasureSpec);

        if (mode == MeasureSpec.EXACTLY) {
            width = size;
        } else if (mode == MeasureSpec.AT_MOST) {
            width = Math.min(size, desiredWidth);
        } else {
            width = desiredWidth;
        }

        mode = MeasureSpec.getMode(heightMeasureSpec);
        size = MeasureSpec.getSize(heightMeasureSpec);

        if (mode == MeasureSpec.EXACTLY) {
            height = size;
        } else if (mode == MeasureSpec.AT_MOST) {
            height = Math.min(size, desiredHeight);
        } else {
            height = desiredHeight;
        }

        if (DEBUG) {
            Log.d(TAG, "Setting dimens to " + width + "x" + height);
        }
        setMeasuredDimension(width, height);
    }

    @Override
    public void onDraw(Canvas c) {
        if (shouldDrawGrid()) {
            int gridSpacing = mGridSpacing;
            // Figure out where we should start drawing the grid
            Point viewOffset = mHelper.getViewOffset();
            int gridX = gridSpacing - (viewOffset.x % gridSpacing);
            int gridY = gridSpacing - (viewOffset.y % gridSpacing);

            for (int x = gridX; x < getWidth(); x += gridSpacing) {
                for (int y = gridY; y < getHeight(); y += gridSpacing) {
                    c.drawCircle(x, y, GRID_RADIUS, mPaint);
                }
            }
        }
    }

    public void setWorkspace(Workspace workspace) {
        mWorkspace = workspace;
    }

    private boolean shouldDrawGrid() {
        return mDrawGrid && mHelper.getScale() > MIN_SCALE_TO_DRAW_GRID;
    }
}
