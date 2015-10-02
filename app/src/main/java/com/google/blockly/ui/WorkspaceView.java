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
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.google.blockly.model.Workspace;
import com.google.blockly.model.WorkspacePoint;

/**
 * Handles updating the viewport into the workspace and is the parent view for all blocks. This
 * view is responsible for handling drags. A drag on the workspace will move the viewport and a
 * drag on a block or stack of blocks will drag those within the workspace.
 */
public class WorkspaceView extends ViewGroup {
    private static final String TAG = "WorkspaceView";
    private static final boolean DEBUG = true;

    private static final int DEFAULT_GRID_SPACING = 48;
    private static final int GRID_COLOR = 0xffa0a0a0;
    private static final int GRID_RADIUS = 2;
    private static final float MIN_SCALE_TO_DRAW_GRID = 0.2f;

    // TODO: Replace with more intelligent defaults
    // Default desired width of the view in pixels.
    private static final int DESIRED_WIDTH = 2048;
    // Default desired height of the view in pixels.
    private static final int DESIRED_HEIGHT = 4096;

    private final WorkspaceHelper mHelper;
    private final Paint mGridPaint = new Paint();
    private final int mGridSpacing = DEFAULT_GRID_SPACING;
    private final boolean mDrawGrid = true;
    private final ViewPoint mTemp = new ViewPoint();
    private Workspace mWorkspace;

    // Fields for workspace dragging.
    private boolean mIsDragging;
    private final ViewPoint mDragStart = new ViewPoint();
    private WorkspacePoint mWorkspaceOffsetBeforeDraw = new WorkspacePoint();

    public WorkspaceView(Context context) {
        this(context, null);
    }

    public WorkspaceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WorkspaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mHelper = new WorkspaceHelper(context, attrs);
        mGridPaint.setColor(GRID_COLOR);
        setWillNotDraw(false);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // TODO(rohlfingt): limit panning range to "current size" of the workspace. Need to first
        // determine what that actually is.
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mIsDragging = true;
            mDragStart.set((int) event.getRawX(), (int) event.getRawY());
            mWorkspaceOffsetBeforeDraw.setFrom(mHelper.getOffset());
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (mIsDragging) {
                mHelper.getOffset().set(
                        mWorkspaceOffsetBeforeDraw.x +
                                mHelper.viewToWorkspaceUnits(mDragStart.x - (int) event.getRawX()),
                        mWorkspaceOffsetBeforeDraw.y +
                                mHelper.viewToWorkspaceUnits(mDragStart.y - (int) event.getRawY()));
                requestLayout();
                return true;
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            if (mIsDragging) {
                mIsDragging = false;
                return true;
            }
        }

        return false;
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            getChildAt(i).measure(0, 0);
        }

        int width = getMeasuredSize(widthMeasureSpec, DESIRED_WIDTH);
        int height = getMeasuredSize(heightMeasureSpec, DESIRED_HEIGHT);

        if (DEBUG) {
            Log.d(TAG, "Setting dimens to " + width + "x" + height);
        }
        setMeasuredDimension(width, height);
    }

    /**
     * Get size for one dimension (width or height) of the view based on measure spec and desired
     * size.
     * @param measureSpec The measure spec provided to {@link #onMeasure(int, int)} by its caller.
     * @param desiredSize The intrinsic desired size for this view.
     * @return The determined size, given measure spec and desired size.
     */
    private static int getMeasuredSize(int measureSpec, int desiredSize) {
        int mode = MeasureSpec.getMode(measureSpec);
        int size = MeasureSpec.getSize(measureSpec);

        if (mode == MeasureSpec.EXACTLY) {
            return size;
        } else if (mode == MeasureSpec.AT_MOST) {
            return Math.min(size, desiredSize);
        } else {
            return desiredSize;
        }

    }

    @Override
    public void onDraw(Canvas c) {
        if (shouldDrawGrid()) {
            int gridSpacing = mGridSpacing;
            // Figure out where we should start drawing the grid
            ViewPoint viewOffset = mHelper.getViewOffset();
            int gridX = gridSpacing - (viewOffset.x % gridSpacing);
            int gridY = gridSpacing - (viewOffset.y % gridSpacing);

            for (int x = gridX; x < getWidth(); x += gridSpacing) {
                for (int y = gridY; y < getHeight(); y += gridSpacing) {
                    c.drawCircle(x, y, GRID_RADIUS, mGridPaint);
                }
            }
        }
        super.onDraw(c);
    }

    /**
     * Sets the workspace this view should display.
     *
     * @param workspace The workspace to load views for.
     */
    public void setWorkspace(Workspace workspace) {
        mWorkspace = workspace;
        mWorkspace.setWorkspaceHelper(mHelper);
    }

    /**
     * @return The helper for doing unit conversions and generating views in this workspace.
     */
    public WorkspaceHelper getWorkspaceHelper() {
        return mHelper;
    }

    private boolean shouldDrawGrid() {
        return mDrawGrid && mHelper.getScale() > MIN_SCALE_TO_DRAW_GRID && mGridSpacing > 0;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        mTemp.x = r - l;
        mTemp.y = b - t;
        mHelper.setViewSize(mTemp);
        boolean rtl = mHelper.useRtL();
        int childCount = getChildCount();

        if (DEBUG) {
            Log.d(TAG, "Laying out " + childCount + " children");
        }
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }
            if (child instanceof BlockGroup) {
                BlockGroup bg = (BlockGroup) child;
                WorkspacePoint wksPos = bg.getTopBlockPosition();
                mHelper.workspaceToViewCoordinates(wksPos, mTemp);

                int cl = rtl ? mTemp.x - bg.getMeasuredWidth() : mTemp.x;
                int cr = rtl ? mTemp.x : mTemp.x + bg.getMeasuredWidth();
                int ct = mTemp.y;
                int cb = mTemp.y + bg.getMeasuredHeight();
                child.layout(cl, ct, cr, cb);
                Log.d(TAG, "Laid out block group at " + cl + ", " + ct + ", " + cr + ", " + cb);
            }
        }
    }
}
