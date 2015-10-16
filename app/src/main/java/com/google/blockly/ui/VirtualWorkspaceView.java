package com.google.blockly.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewGroup;

import com.google.blockly.R;
import com.google.blockly.model.WorkspacePoint;

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

    // Allowed zoom scales.
    private final float[] ZOOM_SCALES = new float[]{0.25f, 0.5f, 1.0f, 2.0f};
    private final int INIT_ZOOM_SCALES_INDEX = 2;

    // Constants for drawing the coordinate grid.
    private static final int GRID_SPACING = 48;
    private static final int GRID_COLOR = 0xffa0a0a0;
    private static final int GRID_RADIUS = 2;
    private static final float MIN_SCALE_TO_DRAW_GRID = 0.5f;

    // Fields for workspace panning.
    private int mPanningPointerId = MotionEvent.INVALID_POINTER_ID;
    private final ViewPoint mPanningStart = new ViewPoint();

    // Coordinates at the beginning of scrolling the workspace.
    private int mOriginalScrollX;
    private int mOriginalScrollY;

    // Scale and zoom in/out factor.
    private int mCurrentZoomScaleIndex = INIT_ZOOM_SCALES_INDEX;
    private float mScale = ZOOM_SCALES[INIT_ZOOM_SCALES_INDEX];

    // Fields for grid drawing.
    private final boolean mDrawGrid = true;
    private final Paint mGridPaint = new Paint();

    // The workspace view that backs this virtual view.
    private WorkspaceView mWorkspaceView;

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
    }

    /**
     * Reset the view to the center of the virtual workspace with unit scale.
     * <p/>
     * This is called when the "reset view" button is clicked.
     */
    public void resetView() {
        updateScale(INIT_ZOOM_SCALES_INDEX);
        scrollTo(0, 0);
    }

    /**
     * Zoom into (i.e., enlarge) the workspace.
     */
    public void zoomIn() {
        if (mCurrentZoomScaleIndex < ZOOM_SCALES.length - 1) {
            updateScale(mCurrentZoomScaleIndex + 1);
        }
    }

    /**
     * Zoom out from (i.e.,shrink) the workspace.
     */
    public void zoomOut() {
        if (mCurrentZoomScaleIndex > 0) {
            updateScale(mCurrentZoomScaleIndex - 1);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
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
                        (int) (getMeasuredWidth() / mScale), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(
                        (int) (getMeasuredHeight() / mScale), MeasureSpec.EXACTLY));
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
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        // Shift the wrapped view's position to follow scrolling along. The scrolling of view
        // content is controlled by setTranslationX() and setTranslationY() in this.scrollTo()
        // below.
        final int offsetX = getScrollX();
        final int offsetY = getScrollY();
        mWorkspaceView.layout(
                (int) ((l / mScale) + offsetX), (int) ((t / mScale) + offsetY),
                (int) ((r / mScale) + offsetX), (int) ((b / mScale) + offsetY));
    }

    @Override
    public void onDraw(Canvas c) {
        if (shouldDrawGrid()) {
            int gridSpacing = (int) (GRID_SPACING * mScale);
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

    @Override
    public void scrollTo(int x, int y) {
        int halfWidth = getWidth() / 2;
        int halfHeight = getHeight() / 2;

        // Clamp x and y to the scroll range that will allow for 1/2 view being outside the range
        // use by blocks. This matches the computations in computeHorizontalScrollOffset and
        // computeVerticalScrollOffset, respectively.
        final Rect workspaceViewportBounds = mWorkspaceView.getBlocksBoundingBox();
        x = Math.max((int) (workspaceViewportBounds.left * mScale) - halfWidth,
                Math.min((int) (workspaceViewportBounds.right * mScale) - halfWidth, x));
        y = Math.max((int) (workspaceViewportBounds.top * mScale) - halfHeight,
                Math.min((int) (workspaceViewportBounds.bottom * mScale) - halfHeight, y));

        // Update and show scroll bars.
        super.scrollTo(x, y);
        final WorkspaceHelper helper = mWorkspaceView.getWorkspaceHelper();
        helper.getOffset().set(
                helper.viewToWorkspaceUnits((int) (x / mScale)),
                helper.viewToWorkspaceUnits((int) (y / mScale)));
        mWorkspaceView.requestLayout();
    }

    @Override
    protected int computeHorizontalScrollRange() {
        final Rect workspaceViewportBounds = mWorkspaceView.getBlocksBoundingBox();
        return (int) ((workspaceViewportBounds.right - workspaceViewportBounds.left) * mScale) +
                getMeasuredWidth();
    }

    @Override
    protected int computeHorizontalScrollExtent() {
        return getMeasuredWidth();
    }

    @Override
    protected int computeHorizontalScrollOffset() {
        final Rect workspaceViewportBounds = mWorkspaceView.getBlocksBoundingBox();
        return getScrollX() -
                ((int) (workspaceViewportBounds.left * mScale) - getMeasuredWidth() / 2);
    }

    @Override
    protected int computeVerticalScrollRange() {
        final Rect workspaceViewportBounds = mWorkspaceView.getBlocksBoundingBox();
        return (int) ((workspaceViewportBounds.bottom - workspaceViewportBounds.top) * mScale) +
                getMeasuredHeight();
    }

    @Override
    protected int computeVerticalScrollExtent() {
        return getMeasuredHeight();
    }

    @Override
    protected int computeVerticalScrollOffset() {
        final Rect workspaceViewportBounds = mWorkspaceView.getBlocksBoundingBox();
        return getScrollY() -
                ((int) (workspaceViewportBounds.top * mScale) - getMeasuredHeight() / 2);
    }

    private void updateScale(int newScaleIndex) {
        if (newScaleIndex != mCurrentZoomScaleIndex) {
            mCurrentZoomScaleIndex = newScaleIndex;
            mScale = ZOOM_SCALES[mCurrentZoomScaleIndex];
            mWorkspaceView.setScaleX(mScale);
            mWorkspaceView.setScaleY(mScale);
            mWorkspaceView.requestLayout();
        }
    }

    private boolean shouldDrawGrid() {
        return mDrawGrid && mScale >= MIN_SCALE_TO_DRAW_GRID;
    }
}
