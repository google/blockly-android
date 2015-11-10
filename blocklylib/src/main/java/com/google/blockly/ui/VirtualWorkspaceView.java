package com.google.blockly.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewGroup;

import com.google.blockly.R;

/**
 * Virtual view of a {@link WorkspaceView}.
 * <p/>
 * This view class provides a viewport-sized window into a larger workspace and supports panning and
 * zoom.
 */
public class VirtualWorkspaceView extends ViewGroup {
    private static final String TAG = "VirtualWorkspaceView";
    private static final boolean DEBUG = true;

    // TODO: Replace with more intelligent defaults
    // Default desired width of the view in pixels.
    private static final int DESIRED_WIDTH = 2048;
    // Default desired height of the view in pixels.
    private static final int DESIRED_HEIGHT = 2048;
    // Constants for drawing the coordinate grid.
    private static final int GRID_SPACING = 48;
    private static final int GRID_COLOR = 0xffa0a0a0;
    private static final int GRID_RADIUS = 2;
    private static final float MIN_SCALE_TO_DRAW_GRID = 0.5f;
    // Allowed zoom scales.
    private final float[] ZOOM_SCALES = new float[]{0.25f, 0.5f, 1.0f, 2.0f};
    private final int INIT_ZOOM_SCALES_INDEX = 2;
    private final ViewPoint mPanningStart = new ViewPoint();
    // Fields for grid drawing.
    private final boolean mDrawGrid = true;
    private final Paint mGridPaint = new Paint();
    // Fields for workspace panning.
    private int mPanningPointerId = MotionEvent.INVALID_POINTER_ID;
    // Coordinates at the beginning of scrolling the workspace.
    private int mOriginalScrollX;
    private int mOriginalScrollY;
    // Scale and zoom in/out factor.
    private int mCurrentZoomScaleIndex = INIT_ZOOM_SCALES_INDEX;
    private float mViewScale = ZOOM_SCALES[INIT_ZOOM_SCALES_INDEX];

    // The workspace view that backs this virtual view.
    private WorkspaceView mWorkspaceView;
    // Flag indicating whether view should be reset before redrawing. This is set upon construction
    // to force an initial reset in the first call to onLayout. Call postResetView() to set this.
    private boolean mResetViewPending = true;

    private ScaleGestureDetector mScaleGestureDetector;

    public VirtualWorkspaceView(Context context) {
        this(context, null);
    }

    public VirtualWorkspaceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VirtualWorkspaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mGridPaint.setColor(GRID_COLOR);
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
        setHorizontalScrollBarEnabled(true);
        setVerticalScrollBarEnabled(true);

        mScaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleGestureListener());
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
        updateScaleStep(INIT_ZOOM_SCALES_INDEX);

        final Rect blocksBoundingBox = mWorkspaceView.getBlocksBoundingBox();
        final int margin = GRID_SPACING / 2;
        final int scrollToY = (int) (blocksBoundingBox.top * mViewScale) - margin;
        if (mWorkspaceView.getWorkspaceHelper().useRtL()) {
            scrollTo((int) (blocksBoundingBox.right * mViewScale) - getMeasuredWidth() + margin,
                    scrollToY);
        } else {
            scrollTo((int) (blocksBoundingBox.left * mViewScale) - margin, scrollToY);
        }
    }

    /**
     * Zoom into (i.e., enlarge) the workspace.
     */
    public void zoomIn() {
        if (mCurrentZoomScaleIndex < ZOOM_SCALES.length - 1) {
            updateScaleStep(mCurrentZoomScaleIndex + 1);
        }
    }

    /**
     * Zoom out from (i.e.,shrink) the workspace.
     */
    public void zoomOut() {
        if (mCurrentZoomScaleIndex > 0) {
            updateScaleStep(mCurrentZoomScaleIndex - 1);
        }
    }

    /**
     * @return The current view scale factor. Larger than 1.0 means zoomed in, smaller than 1.0
     * means zoomed out.
     */
    public float getViewScale() {
        return mViewScale;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mScaleGestureDetector.onTouchEvent(event);

        final int action = MotionEventCompat.getActionMasked(event);

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                final int pointerIdx = MotionEventCompat.getActionIndex(event);
                mPanningPointerId = MotionEventCompat.getPointerId(event, pointerIdx);
                mPanningStart.set(
                        (int) MotionEventCompat.getX(event, pointerIdx),
                        (int) MotionEventCompat.getY(event, pointerIdx));
                mOriginalScrollX = getScrollX();
                mOriginalScrollY = getScrollY();
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                if (mPanningPointerId != MotionEvent.INVALID_POINTER_ID) {
                    final int pointerIdx =
                            MotionEventCompat.findPointerIndex(event, mPanningPointerId);
                    scrollTo(
                            mOriginalScrollX + mPanningStart.x -
                                    (int) MotionEventCompat.getX(event, pointerIdx),
                            mOriginalScrollY + mPanningStart.y -
                                    (int) MotionEventCompat.getY(event, pointerIdx));
                    return true;
                } else {
                    return false;
                }
            }
            case MotionEvent.ACTION_POINTER_UP: {
                // Some pointer went up - check whether it was the one used for panning.
                final int pointerIdx = MotionEventCompat.getActionIndex(event);
                final int pointerId = MotionEventCompat.getPointerId(event, pointerIdx);
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
        mWorkspaceView.layout(
                (int) ((l / mViewScale) + offsetX), (int) ((t / mViewScale) + offsetY),
                (int) ((r / mViewScale) + offsetX), (int) ((b / mViewScale) + offsetY));
    }

    @Override
    public void onDraw(Canvas c) {
        if (shouldDrawGrid()) {
            int gridSpacing = (int) (GRID_SPACING * mViewScale);
            // Figure out where we should start drawing the grid
            int scrollX = getScrollX();
            int scrollY = getScrollY();

            int beginX = scrollX + gridSpacing - (scrollX % gridSpacing);
            int beginY = scrollY + gridSpacing - (scrollY % gridSpacing);

            int endX = getWidth() + scrollX;
            int endY = getHeight() + scrollY;

            for (int x = beginX; x < endX; x += gridSpacing) {
                for (int y = beginY; y < endY; y += gridSpacing) {
                    c.drawCircle(x, y, GRID_RADIUS, mGridPaint);
                }
            }
        }
        super.onDraw(c);
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
        int halfViewWidth = getMeasuredWidth() / 2;
        int halfViewHeight = getMeasuredHeight() / 2;

        // Clamp x and y to the scroll range that will allow for 1/2 view being outside the range
        // use by blocks. This matches the computations in computeHorizontalScrollOffset and
        // computeVerticalScrollOffset, respectively.
        final Rect workspaceViewportBounds = mWorkspaceView.getBlocksBoundingBox();

        final int xMin =
                (int) (workspaceViewportBounds.left * mViewScale /* view-scaled virtual coords. */)
                        - halfViewWidth;
        final int xMax = (int) (workspaceViewportBounds.right * mViewScale) - halfViewWidth;
        x = clampToRange(x, xMin, xMax);

        final int yMin = (int) (workspaceViewportBounds.top * mViewScale) - halfViewHeight;
        final int yMax = (int) (workspaceViewportBounds.bottom * mViewScale) - halfViewHeight;
        y = clampToRange(y, yMin, yMax);

        // Update and show scroll bars.
        super.scrollTo(x, y);

        // Set view offset in the virtual workspace and request layout of the WorkspaceView with the
        // new offset. The view offset is the location of the top-left pixel displayed in this view
        // in virtual workspace coordinates, regardless of RtL vs. LtR mode.
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
        final Rect workspaceViewportBounds = mWorkspaceView.getBlocksBoundingBox();
        final int viewScaledWorkspaceRange =
                (int) ((workspaceViewportBounds.right - workspaceViewportBounds.left) * mViewScale);
        return  viewScaledWorkspaceRange + getMeasuredWidth();
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
        // Range and offset are in view-scaled units, so the extent of the displayed area is simply
        // the width of this view.
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
        final Rect workspaceViewportBounds = mWorkspaceView.getBlocksBoundingBox();
        final int viewScaledWorkspaceLeft = (int) (workspaceViewportBounds.left * mViewScale);
        return getScrollX() - (viewScaledWorkspaceLeft - getMeasuredWidth() / 2);
    }

    /**
     * @return Vertical scroll range width. See {@link #computeHorizontalScrollRange()} for details.
     */
    @Override
    protected int computeVerticalScrollRange() {
        final Rect workspaceViewportBounds = mWorkspaceView.getBlocksBoundingBox();
        final int viewScaledWorkspaceRange =
                (int) ((workspaceViewportBounds.bottom - workspaceViewportBounds.top) * mViewScale);
        return viewScaledWorkspaceRange + getMeasuredHeight();
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
        final Rect workspaceViewportBounds = mWorkspaceView.getBlocksBoundingBox();
        final int viewScaledWorkspaceTop = (int) (workspaceViewportBounds.top * mViewScale);
        return getScrollY() - (viewScaledWorkspaceTop - getMeasuredHeight() / 2);
    }

    private void updateScaleStep(int newScaleIndex) {
        if (newScaleIndex != mCurrentZoomScaleIndex) {
            final float oldViewScale = mViewScale;

            mCurrentZoomScaleIndex = newScaleIndex;
            mViewScale = ZOOM_SCALES[mCurrentZoomScaleIndex];
            mWorkspaceView.setScaleX(mViewScale);
            mWorkspaceView.setScaleY(mViewScale);

            // Add offset to current scroll coordinates so the center of the visible workspace area
            // remains in the same place.
            final float scaleDifference = mViewScale - oldViewScale;
            scrollBy((int) (scaleDifference * getMeasuredWidth() / 2),
                    (int) (scaleDifference * getMeasuredHeight() / 2));
        }
    }

    private boolean shouldDrawGrid() {
        return mDrawGrid && mViewScale >= MIN_SCALE_TO_DRAW_GRID;
    }

    private static int clampToRange(int y, int min, int max) {
        return Math.min(max, Math.max(min, y));
    }

    /** Listener class for scaling and panning the view using pinch-to-zoom gestures. */
    private class ScaleGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        // Focus point at the start of the pinch gesture. This is used for simultaneous panning.
        private float mStartFocusX;
        private float mStartFocusY;

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            mStartFocusX = detector.getFocusX();
            mStartFocusY = detector.getFocusY();
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
            }

            mWorkspaceView.setScaleX(mViewScale);
            mWorkspaceView.setScaleY(mViewScale);

            // Compute scroll offsets based on a) the shift of the gesture focus point, and b) the
            // shift necessary to keep the view area's center invariant under scaling.
            final float scaleDifference = mViewScale - oldViewScale;
            final int scrollX = (int) (detector.getFocusX() - mStartFocusX) +
                    (int) (scaleDifference * getMeasuredWidth() / 2);
            final int scrollY = (int) (detector.getFocusY() - mStartFocusY) +
                    (int) (scaleDifference * getMeasuredWidth() / 2);
            scrollBy(scrollX, scrollY);

            return true;
        }
    }
}
