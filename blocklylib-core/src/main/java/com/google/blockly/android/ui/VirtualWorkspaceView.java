/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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
import android.graphics.Canvas;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.inputmethod.InputMethodManager;

import com.google.blockly.android.R;
import com.google.blockly.android.ZoomBehavior;

/**
 * Virtual view of a {@link WorkspaceView}.
 * <p/>
 * This view class provides a viewport-sized window into a larger workspace and supports panning and
 * zoom.
 */
public class VirtualWorkspaceView extends NonPropagatingViewGroup {
    private static final String TAG = "VirtualWorkspaceView";
    private static final boolean DEBUG = false;

    // TODO(#87): Replace with configuration. Use dp.
    // Default desired width of the view in pixels.
    private static final int DESIRED_WIDTH = 2048;
    // Default desired height of the view in pixels.
    private static final int DESIRED_HEIGHT = 2048;

    private static final float MIN_SCALE_TO_DRAW_GRID = 0.5f;

    // Allowed zoom scales.
    private static final float[] ZOOM_SCALES = new float[]{0.25f, 0.5f, 1.0f, 2.0f};
    private static final int INIT_ZOOM_SCALES_INDEX = 2;

    protected boolean mScrollable = true;
    protected boolean mScalable = true;

    private final ViewPoint mPanningStart = new ViewPoint();

    private final WorkspaceGridRenderer mGridRenderer = new WorkspaceGridRenderer();

    // Fields for workspace panning.
    private int mPanningPointerId = MotionEvent.INVALID_POINTER_ID;
    // Coordinates at the beginning of scrolling the workspace.
    private int mOriginalScrollX;
    private int mOriginalScrollY;
    // Scale and zoom in/out factor.
    private int mCurrentZoomScaleIndex = INIT_ZOOM_SCALES_INDEX;
    private float mViewScale = ZOOM_SCALES[INIT_ZOOM_SCALES_INDEX];

    private boolean mDrawGrid = true;

    // The workspace view that backs this virtual view.
    private WorkspaceView mWorkspaceView;
    // Flag indicating whether view should be reset before redrawing. This is set upon construction
    // to force an initial reset in the first call to onLayout. Call postResetView() to set this.
    private boolean mResetViewPending = true;

    private ScaleGestureDetector mScaleGestureDetector;
    private GestureDetector mTapGestureDetector;
    private InputMethodManager mImeManager;

    private Rect mTempRect = new Rect();  // Used when calculating view-scaled block bounds.

    public VirtualWorkspaceView(Context context) {
        this(context, null);
    }

    public VirtualWorkspaceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VirtualWorkspaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();

        mWorkspaceView = (WorkspaceView) findViewById(R.id.workspace);
        // Setting the child view's pivot point to (0,0) means scaling leaves top-left corner in
        // place means there is no need to adjust view translation.
        mWorkspaceView.setPivotX(0);
        mWorkspaceView.setPivotY(0);

        setWillNotDraw(false);
        setHorizontalScrollBarEnabled(mScrollable);
        setVerticalScrollBarEnabled(mScrollable);

        mScaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleGestureListener());
        mTapGestureDetector = new GestureDetector(getContext(), new TapGestureListener());
        mGridRenderer.updateGridBitmap(mViewScale);
        mImeManager = (InputMethodManager) getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    /**
     * Post a view reset to occur upon next call to {@link #onLayout(boolean, int, int, int, int)}.
     * <p/>
     * This method can be called when workspace content has been completely replaced, to provide the
     * user with a good initial view of the new workspace content.
     */
    public void postResetView() {
        mResetViewPending = true;
    }

    /**
     * Reset the view to the top-left corner of the virtual workspace (with a small margin), and
     * reset zoom to unit scale.
     * <p/>
     * This is called when the "reset view" button is clicked, or when
     * {@link #mResetViewPending} is set.
     */
    public void resetView() {
        // Reset scrolling state.
        mPanningPointerId = MotionEvent.INVALID_POINTER_ID;
        mPanningStart.set(0,0);
        mOriginalScrollX = 0;
        mOriginalScrollY = 0;

        updateScaleStep(INIT_ZOOM_SCALES_INDEX);

        final Rect blocksBoundingBox = getViewScaledBlockBounds();
        final boolean useRtl = mWorkspaceView.getWorkspaceHelper().useRtl();
        if (mScrollable) {
            final int margin = mGridRenderer.getGridSpacing() / 2;
            final int scrollToY = blocksBoundingBox.top - margin;
            if (useRtl) {
                scrollTo(blocksBoundingBox.right - getMeasuredWidth() + margin, scrollToY);
            } else {
                scrollTo(blocksBoundingBox.left - margin, scrollToY);
            }
        } else {
            // Reset top leading corner to 0,0 when
            scrollTo(useRtl ? -getMeasuredWidth() : 0, 0);
        }
    }

    public boolean isScrollable() {
        return mScrollable;
    }

    public void setDrawGrid(boolean drawGrid){
        mDrawGrid = drawGrid;
    }

    public void setZoomBehavior(ZoomBehavior zoomBehavior){
        setScrollable(zoomBehavior.isScrollEnabled());
        setScalable(zoomBehavior.isPinchZoomEnabled());
    }

    /**
     * Configures whether the user can scroll the workspace by dragging.  If scrolling is disabled,
     * the workspace will reset to 0,0 in the top right hand corner.
     *
     * @param scrollable Allow scrolling if true. Otherwise, disable it.
     */
    protected void setScrollable(boolean scrollable) {
        if (scrollable == mScrollable) {
            return;
        }
        mScrollable = scrollable;
        setHorizontalScrollBarEnabled(mScrollable);
        setVerticalScrollBarEnabled(mScrollable);
        if (!mScrollable) {
            resetView();
        }
    }

    /**
     * Configures whether the user can scale the workspace by touch events.
     *
     * @param scalable Allow scalability if true. Otherwise, disable it.
     */
    protected void setScalable(boolean scalable){
        if(mScalable == scalable){
            return;
        }
        mScalable = scalable;
        if(!scalable) {
            resetView();
        }
    }

    /**
     * Zoom into (i.e., enlarge) the workspace.
     *
     * @return True if a zoom was changed, increased. Otherwise false.
     */
    public boolean zoomIn() {
        if (mCurrentZoomScaleIndex < ZOOM_SCALES.length - 1) {
            updateScaleStep(mCurrentZoomScaleIndex + 1);
            return true;
        }
        return false;
    }

    /**
     * Zoom out from (i.e.,shrink) the workspace.
     *
     * @return True if a zoom was changed, decreased. Otherwise false.
     */
    public boolean zoomOut() {
        if (mScrollable && mCurrentZoomScaleIndex > 0) {
            updateScaleStep(mCurrentZoomScaleIndex - 1);
            return true;
        }
        return false;
    }

    /**
     * @return The current view scale factor. Larger than 1.0 means zoomed in, smaller than 1.0
     * means zoomed out.
     */
    public float getViewScale() {
        return mViewScale;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        mTapGestureDetector.onTouchEvent(event);
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(mScalable && mScaleGestureDetector != null) {
            mScaleGestureDetector.onTouchEvent(event);
            if (mScaleGestureDetector.isInProgress()) {
                // If the scale gesture detector is handling a scale-and-pan gesture, then exit here
                // since otherwise we would also be generating dragging events below.
                return true;
            }
        }

        final int action = event.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                clearFocus();
                mImeManager.hideSoftInputFromWindow(getWindowToken(), 0);

                if (mScrollable) {
                    final int pointerIdx = event.getActionIndex();
                    mPanningPointerId = event.getPointerId(pointerIdx);
                    mPanningStart.set(
                            (int) event.getX(pointerIdx),
                            (int) event.getY(pointerIdx));
                    mOriginalScrollX = getScrollX();
                    mOriginalScrollY = getScrollY();
                }
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                if (mPanningPointerId != MotionEvent.INVALID_POINTER_ID) {
                    final int pointerIdx =
                            event.findPointerIndex(mPanningPointerId);
                    if (pointerIdx == -1) {
                        // TODO: (#319) remove when we clean up multi-touch handling.
                        Log.w(TAG, "Got an invalid pointer idx for the panning pointer.");
                        return false;
                    }
                    scrollTo(
                            mOriginalScrollX + mPanningStart.x -
                                    (int) event.getX(pointerIdx),
                            mOriginalScrollY + mPanningStart.y -
                                    (int) event.getY(pointerIdx));
                    return true;
                } else {
                    return false;
                }
            }
            case MotionEvent.ACTION_POINTER_UP: {
                // Some pointer went up - check whether it was the one used for panning.
                final int pointerIdx = event.getActionIndex();
                final int pointerId = event.getPointerId(pointerIdx);
                if (pointerId != mPanningPointerId) {
                    return false;
                }
                // Pointer that went up was used for panning - treat like ACTION_UP.
                // FALLTHROUGH INTENDED.
            }
            case MotionEvent.ACTION_UP: {
                if (mPanningPointerId != MotionEvent.INVALID_POINTER_ID) {
                    mPanningPointerId = MotionEvent.INVALID_POINTER_ID;
                    return true;
                } else {
                    return false;
                }
            }
            case MotionEvent.ACTION_CANCEL: {
                if (mPanningPointerId != MotionEvent.INVALID_POINTER_ID) {
                    // When cancelled, reset to original scroll position.
                    scrollTo(mOriginalScrollX, mOriginalScrollY);
                    mPanningPointerId = MotionEvent.INVALID_POINTER_ID;
                    return true;
                } else {
                    return false;
                }
            }
            default: {
                break;
            }
        }

        return false;
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(
                getMeasuredSize(widthMeasureSpec, DESIRED_WIDTH),
                getMeasuredSize(heightMeasureSpec, DESIRED_HEIGHT));

        mWorkspaceView.measure(
                MeasureSpec.makeMeasureSpec(
                        (int) (getMeasuredWidth() / mViewScale), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(
                        (int) (getMeasuredHeight() / mViewScale), MeasureSpec.EXACTLY));
    }

    @Override
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        if (mResetViewPending) {
            resetView();
            mResetViewPending = false;
        }

        // Shift the wrapped view's position to follow scrolling along. The scrolling of view
        // content is controlled by setTranslationX() and setTranslationY() in this.scrollTo()
        // below.
        final int offsetX = getScrollX();
        final int offsetY = getScrollY();
        mWorkspaceView.layout((offsetX), (offsetY),
                (int) ((getMeasuredWidth() / mViewScale) + offsetX),
                (int) ((getMeasuredHeight() / mViewScale) + offsetY));
    }

    @Override
    protected void onDraw(Canvas c) {
        if (shouldDrawGrid()) {
            mGridRenderer.drawGrid(c, getWidth(), getHeight(), getScrollX(), getScrollY());
        }
    }

    /**
     * Set scroll position for the {@link WorkspaceView} wrapped by this instance.
     * <p/>
     * The given scroll position specifies the absolute offset of the displayed area within the
     * virtual workspace. Inside this method, the given scroll coordinates are clamped to their
     * valid ranges determined from the bounding box of all blocks in the virtual workspace, and
     * allowing for overscroll of half the width (or height, respectively) of this view.
     *
     * @param x The horizontal scroll position in pixel units of this view.
     * @param y The vertical scroll position in pixel units of this view.
     */
    @Override
    public void scrollTo(int x, int y) {
        if (!mScrollable) {
            return;
        }

        // Clamp x and y to the scroll range that will allow for 1/2 the view (or more, for smaller
        // views) being outside the range use by blocks. This matches the computations in
        // computeHorizontalScrollOffset and computeVerticalScrollOffset, respectively.
        Rect blocksBounds = getViewScaledBlockBounds();
        int blocksWidth = blocksBounds.width();
        int blocksHeight = blocksBounds.height();

        int viewWidth = getMeasuredWidth();
        int halfViewWidth = viewWidth / 2;
        int viewHeight = getMeasuredHeight();
        int halfViewHeight = viewHeight / 2;

        int horzMargin = halfViewWidth; // Default margin is half the scrollable view width.
        if (blocksWidth < halfViewWidth) {
            horzMargin = viewWidth - blocksWidth;
        }

        int vertMargin = halfViewHeight;
        if (blocksHeight < halfViewHeight) {
            vertMargin = viewHeight - blocksHeight;
        }

        final int xMin = blocksBounds.left - horzMargin;
        final int xMax = blocksBounds.right + horzMargin - viewWidth;
        x = clampToRange(x, xMin, xMax);

        final int yMin = blocksBounds.top - vertMargin;
        final int yMax = blocksBounds.bottom + vertMargin - viewHeight;
        y = clampToRange(y, yMin, yMax);

        // Update and show scroll bars.
        super.scrollTo(x, y);

        // Set view offset in the virtual workspace and request layout of the WorkspaceView with the
        // new offset. The view offset is the location of the top-left pixel displayed in this view
        // in virtual workspace coordinates, regardless of RTL vs. LTR mode.
        mWorkspaceView.getWorkspaceHelper()
                .setVirtualWorkspaceViewOffset(
                        (int) (x / mViewScale), /* virtual coords. */
                        (int) (y / mViewScale));
        mWorkspaceView.requestLayout();
    }

    /**
     * Get size for one dimension (width or height) of the view based on measure spec and desired
     * size.
     *
     * @param measureSpec The measure spec provided to {@link #onMeasure(int, int)} by its caller.
     * @param desiredSize The intrinsic desired size for this view.
     *
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

    /**
     * @return Horizontal scroll range width, which is used to position the horizontal scroll bar.
     * <p/>
     * This value is the upper limit of the permitted horizontal scroll range, which corresponds to
     * the right boundary of the currently-used workspace area, plus has the view width to allow for
     * overscroll.
     * <p/>
     * Together with the return value of {@link #computeHorizontalScrollOffset()}, this determines
     * the relative position of the scroll bar.
     * <p/>
     * Note that the units of the value returned by this method are essentially arbitrary in the
     * sense that scroll bar position and width are invariant under scaling of range, offset, and
     * extent by the same factor (up to truncation of fractions to nearest integers).
     * <p/>
     * We compute this by taking the total width of the available workspace and converting it to
     * screen pixels. The available workspace is the size needed to fully enclose all blocks with
     * half the display's width for padding, or
     * {@code (workspace max - workspace min) in pixels + the visible width}.
     */
    @Override
    protected int computeHorizontalScrollRange() {
        final Rect viewScaledBlockBounds = getViewScaledBlockBounds();
        final int viewScaledWorkspaceRange =
                viewScaledBlockBounds.right - viewScaledBlockBounds.left;
        final int width = getMeasuredWidth();
        int totalMargin = width;  // By default, leave a half screen width on left and right.
        if (viewScaledWorkspaceRange < width / 2) {
            // Make sure blocks can touch each edge.
            totalMargin = 2 * (width - viewScaledWorkspaceRange);
        }
        return viewScaledWorkspaceRange + totalMargin;
    }

    /**
     * @return Width of the horizontal scroll bar within its range.
     * <p/>
     * This value determines how wide the horizontal scroll bar is, relative to its range as
     * returned by {@link #computeHorizontalScrollRange()}. For example, if this value is half the
     * current range, then the scroll bar will be draw half as wide as the view.
     * <p/>
     * Note that the units of the value returned by this method are essentially arbitrary in the
     * sense that scroll bar position and width are invariant under scaling of range, offset, and
     * extent by the same factor (up to truncation of fractions to nearest integers).
     * <p/>
     * We use screen pixels for the unit, so this is just the measured width of the view.
     */
    @Override
    protected int computeHorizontalScrollExtent() {
        return getMeasuredWidth();
    }

    /**
     * @return Offset of the current horizontal scroll position, which determines the relative
     * position of the horizontal scroll bar.
     * <p/>
     * This is a value between 0 and the return value of {@link #computeHorizontalScrollRange()}.
     * Zero corresponds to the left boundary of the currently-used workspace area, minus half the
     * width of this view to allow for overscroll.
     * <p/>
     * Note that the units of the value returned by this method are essentially arbitrary in the
     * sense that scroll bar position and width are invariant under scaling of range, offset, and
     * extent by the same factor (up to truncation of fractions to nearest integers).
     * <p/>
     * The offset is just a translation of {@code getScrollX()} of the left edge of the viewport on
     * the workspace in pixels relative to the farthest left position possible. In other words,
     * offset goes from {@code 0} to {@code (workspace max - workspace min)} in pixels.
     */
    @Override
    protected int computeHorizontalScrollOffset() {
        final Rect viewScaledBlockBounds = getViewScaledBlockBounds();
        return getScrollX() - (viewScaledBlockBounds.left - computeHorizontalScrollExtent()) / 2;
    }

    /**
     * @return Vertical scroll range width. See {@link #computeHorizontalScrollRange()} for details.
     */
    @Override
    protected int computeVerticalScrollRange() {
        final Rect viewScaledBlockBounds = getViewScaledBlockBounds();
        final int viewScaledWorkspaceRange =
                viewScaledBlockBounds.bottom - viewScaledBlockBounds.top;
        final int height = getMeasuredHeight();
        int totalMargin = height;  // By default, leave a half screen height on top and bottom.
        if (viewScaledWorkspaceRange < height / 2) {
            // Make sure blocks can touch each edge.
            totalMargin = 2 * (height - viewScaledWorkspaceRange);
        }
        return viewScaledWorkspaceRange + totalMargin;
    }

    /**
     * @return Width of the vertical scroll bar within its range.
     * See {@link #computeHorizontalScrollExtent()} for details.
     */
    @Override
    protected int computeVerticalScrollExtent() {
        return getMeasuredHeight();
    }

    /**
     * @return Offset of the current horizontal scroll position.
     * See {@link #computeHorizontalScrollOffset()} for details.
     */
    @Override
    protected int computeVerticalScrollOffset() {
        final Rect viewScaledBlockBounds = getViewScaledBlockBounds();
        return getScrollY() - (viewScaledBlockBounds.top - computeVerticalScrollExtent()) / 2;
    }

    private void updateScaleStep(int newScaleIndex) {
        if (newScaleIndex != mCurrentZoomScaleIndex) {
            final float oldViewScale = mViewScale;

            mCurrentZoomScaleIndex = newScaleIndex;
            mViewScale = ZOOM_SCALES[mCurrentZoomScaleIndex];

            // Add offset to current scroll coordinates so the center of the visible workspace area
            // remains in the same place.
            final float scaleDifference = mViewScale - oldViewScale;
            scrollBy((int) (scaleDifference * getMeasuredWidth() / 2),
                    (int) (scaleDifference * getMeasuredHeight() / 2));

            if (shouldDrawGrid()) {
                mGridRenderer.updateGridBitmap(mViewScale);
            }

            mWorkspaceView.setScaleX(mViewScale);
            mWorkspaceView.setScaleY(mViewScale);
            mWorkspaceView.requestLayout();
        }
    }

    private boolean shouldDrawGrid() {
        return mDrawGrid && mViewScale >= MIN_SCALE_TO_DRAW_GRID;
    }

    private static int clampToRange(int y, int min, int max) {
        return Math.min(max, Math.max(min, y));
    }

    public int getGridSpacing() {
        return mGridRenderer.getGridSpacing();
    }

    public void setGridSpacing(int gridSpacing) {
        mGridRenderer.setGridSpacing(gridSpacing);
    }

    public void setGridColor(int gridColor) {
        mGridRenderer.setGridColor(gridColor);
    }

    public void setGridDotRadius(int gridDotRadius) {
        mGridRenderer.setGridDotRadius(gridDotRadius);
    }

    private class TapGestureListener extends GestureDetector.SimpleOnGestureListener {
        public boolean onSingleTapUp(MotionEvent e) {
            return callOnClick();
        }
    }

    /** EventsCallback class for scaling and panning the view using pinch-to-zoom gestures. */
    private class ScaleGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        // Focus point at the start of the pinch gesture. This is used for computing proper scroll
        // offsets during scaling, as well as for simultaneous panning.
        private float mStartFocusX;
        private float mStartFocusY;
        // View scale at the beginning of the gesture. This is used for computing proper scroll
        // offsets during scaling.
        private float mStartScale;
        // View scroll offsets at the beginning of the gesture. These provide the reference point
        // for adjusting scroll in response to scaling and panning.
        private int mStartScrollX;
        private int mStartScrollY;

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            mStartFocusX = detector.getFocusX();
            mStartFocusY = detector.getFocusY();

            mStartScrollX = getScrollX();
            mStartScrollY = getScrollY();

            mStartScale = mViewScale;
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            final float oldViewScale = mViewScale;

            final float scaleFactor = detector.getScaleFactor();
            mViewScale *= scaleFactor;

            if (mViewScale < ZOOM_SCALES[0]) {
                mCurrentZoomScaleIndex = 0;
                mViewScale = ZOOM_SCALES[mCurrentZoomScaleIndex];
            } else if (mViewScale > ZOOM_SCALES[ZOOM_SCALES.length - 1]) {
                mCurrentZoomScaleIndex = ZOOM_SCALES.length - 1;
                mViewScale = ZOOM_SCALES[mCurrentZoomScaleIndex];
            } else {
                // find nearest zoom scale
                float minDist = Float.MAX_VALUE;
                // If we reach the end the last one was the closest
                int index = ZOOM_SCALES.length - 1;
                for (int i = 0; i < ZOOM_SCALES.length; i++) {
                    float dist = Math.abs(mViewScale - ZOOM_SCALES[i]);
                    if (dist < minDist) {
                        minDist = dist;
                    } else {
                        // When it starts increasing again we've found the closest
                        index = i - 1;
                        break;
                    }
                }
                mCurrentZoomScaleIndex = index;
            }

            if (shouldDrawGrid()) {
                mGridRenderer.updateGridBitmap(mViewScale);
            }

            mWorkspaceView.setScaleX(mViewScale);
            mWorkspaceView.setScaleY(mViewScale);

            // Compute scroll offsets based on difference between original and new scaling factor
            // and the focus point where the gesture started. This makes sure that the scroll offset
            // is adjusted to keep the focus point in place on the screen unless there is also a
            // focus point shift (see next scroll component below).
            final float scaleDifference = mViewScale - mStartScale;
            final int scrollScaleX = (int) (scaleDifference * mStartFocusX);
            final int scrollScaleY = (int) (scaleDifference * mStartFocusY);

            // Compute scroll offset based on shift of the focus point. This makes sure the view
            // pans along with the focus.
            final int scrollPanX = (int) (mStartFocusX - detector.getFocusX());
            final int scrollPanY = (int) (mStartFocusY - detector.getFocusY());

            // Apply the computed scroll components for scale and panning relative to the scroll
            // coordinates at the beginning of the gesture.
            scrollTo(mStartScrollX + scrollScaleX + scrollPanX,
                    mStartScrollY + scrollScaleY + scrollPanY);

            return true;
        }
    }

    @NonNull
    private Rect getViewScaledBlockBounds() {
        mWorkspaceView.getBlocksBoundingBox(mTempRect);
        mTempRect.left = (int) Math.floor(mTempRect.left * mViewScale);
        mTempRect.right = (int) Math.ceil(mTempRect.right * mViewScale);
        mTempRect.top = (int) Math.floor(mTempRect.top * mViewScale);
        mTempRect.bottom = (int) Math.ceil(mTempRect.bottom * mViewScale);
        return mTempRect;
    }
}
