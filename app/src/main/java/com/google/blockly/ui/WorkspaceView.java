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
import android.graphics.Rect;
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
    private static final int DESIRED_HEIGHT = 2048;

    private final WorkspaceHelper mHelper;
    private final Paint mGridPaint = new Paint();
    private final int mGridSpacing = DEFAULT_GRID_SPACING;
    private final boolean mDrawGrid = true;
    private final ViewPoint mTemp = new ViewPoint();
    private final ViewPoint mDragStart = new ViewPoint();
    private Workspace mWorkspace;
    // Fields for workspace dragging.
    private boolean mIsDragging;

    // Viewport bounds. These define the bounding box of all blocks, in view coordinates, and
    // are used to determine ranges and offsets for scrolling.
    private final Rect mViewportBounds = new Rect();

    // Scroll coordinates at the beginning of dragging the workspace. During dragging, the workspace
    // is scrolled relative to these.
    private int mPreDragScrollX;
    private int mPreDragScrollY;

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
        setHorizontalScrollBarEnabled(true);
        setVerticalScrollBarEnabled(true);
    }

    /**
     * Scroll to the viewport origin.
     * <p/>
     * This is intended to be called, for example, when the "reset view" button is clicked.
     */
    public void resetViewport() {
        scrollTo(0, 0);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mIsDragging = true;
            mDragStart.set((int) event.getRawX(), (int) event.getRawY());
            mPreDragScrollX = getScrollX();
            mPreDragScrollY = getScrollY();
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (mIsDragging) {
                scrollTo(
                        mPreDragScrollX + mDragStart.x - (int) event.getRawX(),
                        mPreDragScrollY + mDragStart.y - (int) event.getRawY());
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
    public void scrollTo(int x, int y) {
        int halfWidth = getWidth() / 2;
        int halfHeight = getHeight() / 2;

        // Clamp x and y to the scroll range that will allow for 1/2 view being outside the range
        // use by blocks. This matches the computations in computeHorizontalScrollOffset and
        // computeVerticalScrollOffset, respectively.
        x = Math.max(mViewportBounds.left - halfWidth,
                Math.min(mViewportBounds.right - halfWidth, x));
        y = Math.max(mViewportBounds.top - halfHeight,
                Math.min(mViewportBounds.bottom - halfHeight, y));

        mHelper.getOffset().set(mHelper.viewToWorkspaceUnits(x), mHelper.viewToWorkspaceUnits(y));
        super.scrollTo(x, y);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mViewportBounds.setEmpty();
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            BlockGroup blockGroup = (BlockGroup) getChildAt(i);
            blockGroup.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);

            // Determine this BlockGroup's bounds and extend viewport boundaries accordingly.
            final WorkspacePoint position = blockGroup.getTopBlockPosition();
            int childViewLeft = mHelper.workspaceToViewUnits(position.x);
            int childViewTop = mHelper.workspaceToViewUnits(position.y);

            mViewportBounds.union(childViewLeft, childViewTop,
                    childViewLeft + blockGroup.getMeasuredWidth(),
                    childViewTop + blockGroup.getMeasuredHeight());
        }

        int width = getMeasuredSize(widthMeasureSpec, DESIRED_WIDTH);
        int height = getMeasuredSize(heightMeasureSpec, DESIRED_HEIGHT);

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
            int beginX = getScrollX() + gridSpacing - (getScrollX() % gridSpacing);
            int beginY = getScrollY() + gridSpacing - (getScrollY() % gridSpacing);

            int endX = getWidth() + getScrollX();
            int endY = getHeight() + getScrollY();

            for (int x = beginX; x < endX; x += gridSpacing) {
                for (int y = beginY; y < endY; y += gridSpacing) {
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

    @Override
    protected int computeHorizontalScrollRange() {
        return mViewportBounds.width() + getWidth();
    }

    @Override
    protected int computeHorizontalScrollExtent() {
        return getWidth();
    }

    @Override
    protected int computeHorizontalScrollOffset() {
        return getScrollX() - (mViewportBounds.left - getWidth() / 2);
    }

    @Override
    protected int computeVerticalScrollRange() {
        return mViewportBounds.height() + getHeight();
    }

    @Override
    protected int computeVerticalScrollExtent() {
        return getHeight();
    }

    @Override
    protected int computeVerticalScrollOffset() {
        return getScrollY() - (mViewportBounds.top - getHeight() / 2);
    }

    private boolean shouldDrawGrid() {
        return mDrawGrid && mHelper.getScale() > MIN_SCALE_TO_DRAW_GRID && mGridSpacing > 0;
    }

    /**
     * Get size for one dimension (width or height) of the view based on measure spec and desired
     * size.
     *
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
                mTemp.x = mHelper.workspaceToViewUnits(wksPos.x);
                mTemp.y = mHelper.workspaceToViewUnits(wksPos.y);

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
