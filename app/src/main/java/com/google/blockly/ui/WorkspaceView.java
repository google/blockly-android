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
import android.graphics.Rect;
import android.support.annotation.IntDef;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import com.google.blockly.control.Dragger;
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

    private final WorkspaceHelper mHelper;
    private final ViewPoint mTemp = new ViewPoint();
    private Workspace mWorkspace;

    // Distance threshold for detecting drag gestures.
    private final float mTouchSlop;

    @IntDef({TOUCH_STATE_NONE, TOUCH_STATE_DOWN, TOUCH_STATE_DRAGGING, TOUCH_STATE_LONGPRESS})
    public @interface TouchState {
    }

    // No current touch interaction.
    private static final int TOUCH_STATE_NONE = 0;
    // Block in this view has received "Down" event; waiting for further interactions to decide
    // whether to drag, show context menu, etc.
    private static final int TOUCH_STATE_DOWN = 1;
    // Block in this view is being dragged.
    private static final int TOUCH_STATE_DRAGGING = 2;
    // Block in this view received a long press.
    private static final int TOUCH_STATE_LONGPRESS = 3;

    // Current state of touch interaction with blocks in this workspace view.
    @TouchState
    private int mTouchState = TOUCH_STATE_NONE;

    // Fields for dragging blocks in the workspace.
    private int mDraggingPointerId = MotionEvent.INVALID_POINTER_ID;
    private final ViewPoint mDraggingStart = new ViewPoint();
    private BlockView mDraggingBlockView = null;
    private Dragger mDragger;
    private View mTrashView;

    // Viewport bounds. These define the bounding box of all blocks, in view coordinates, and
    // are used to determine ranges and offsets for scrolling.
    private final Rect mBlocksBoundingBox = new Rect();

    public WorkspaceView(Context context) {
        this(context, null);
    }

    public WorkspaceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WorkspaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mHelper = new WorkspaceHelper(this, attrs);

        // Tell the workspace helper to pass onTouchBlock events straight through to the WSView.
        mHelper.setBlockTouchHandler(new WorkspaceHelper.BlockTouchHandler() {
            @Override
            public void onTouchBlock(BlockView blockView, MotionEvent motionEvent) {
                WorkspaceView.this.onTouchBlock(blockView, motionEvent);
            }
        });

        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
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

    public Workspace getWorkspace() {
        return mWorkspace;
    }

    public WorkspaceHelper getWorkspaceHelper() {
        return mHelper;
    }

    /**
     * Updates the dragger for this workspace view and passes through the view for the trash can.
     *
     * @param dragger The {@link Dragger} to use in this workspace.
     */
    public void setDragger(Dragger dragger) {
        mDragger = dragger;
        if (mTrashView != null) {
            mDragger.setTrashView(mTrashView);
        }
    }

    /** @return The bounding box in view coordinates of the workspace region occupied by blocks. */
    public Rect getBlocksBoundingBox() {
        return mBlocksBoundingBox;
    }

    /**
     * Intercept touch events while dragging blocks.
     * <p/>
     * Note that the current event does not need to be handled here, because
     * {@link BlockView#onTouchEvent(MotionEvent)} always returns {@code false}. Therefore, the
     * event that triggered a {@link BlockView} instance to call
     * {@link #onTouchBlock(BlockView, MotionEvent)} will not be consumed by the block view, but
     * be propagated all the way back up the view hierarchy and ultimately be handled by this the
     * {@link #onTouchEvent(MotionEvent)} method of this instance.
     * <p/>
     * One important benefit of this procedure is that all events for the dragging interaction have
     * locations in {@link WorkspaceView} coordinates, rather than in {@link BlockView} coordinates,
     * as would be the case for the event handed up from {@link BlockView}.
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return mTouchState == TOUCH_STATE_DOWN;
    }

    /** Handle touch events for block dragging. */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mTouchState == TOUCH_STATE_DOWN || mTouchState == TOUCH_STATE_DRAGGING) {
            return dragBlock(event);
        }

        return false;
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mBlocksBoundingBox.setEmpty();
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            BlockGroup blockGroup = (BlockGroup) getChildAt(i);
            blockGroup.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);

            // Determine this BlockGroup's bounds and extend viewport boundaries accordingly.
            final WorkspacePoint position = blockGroup.getTopBlockPosition();
            int childViewLeft = mHelper.workspaceToViewUnits(position.x);
            int childViewTop = mHelper.workspaceToViewUnits(position.y);

            mBlocksBoundingBox.union(childViewLeft, childViewTop,
                    childViewLeft + blockGroup.getMeasuredWidth(),
                    childViewTop + blockGroup.getMeasuredHeight());
        }

        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
    }

    /**
     * Update the {@link View} for the trash can, which will be passed to the dragger that moves
     * blocks in this view.
     *
     * @param trashView The {@link View} of the trash can icon.
     */
    public void setTrashView(View trashView) {
        mTrashView = trashView;
        if (mDragger != null) {
            mDragger.setTrashView(trashView);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        mTemp.x = r - l;
        mTemp.y = b - t;
        mHelper.setViewSize(mTemp);
        boolean rtl = mHelper.useRtL();
        int childCount = getChildCount();

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
            }
        }
    }

    /**
     * Let this instance know that a block was touched.
     *
     * @param blockView The {@link BlockView} that detected a touch event.
     * @param event The touch event.
     */
    private void onTouchBlock(BlockView blockView, MotionEvent event) {
        // Only initiate dragging of given view if in idle state - this prevents occluded blocks
        // from grabbing drag focus because they saw an unconsumed Down event before it propagated
        // back up to this WorkspaceView.
        if (mTouchState == TOUCH_STATE_NONE) {
            mTouchState = TOUCH_STATE_DOWN;
            mDraggingBlockView = blockView;
            mDraggingPointerId =
                    MotionEventCompat.getPointerId(event, MotionEventCompat.getActionIndex(event));
        }
    }

    /**
     * Handle motion events while dragging a block.
     */
    private boolean dragBlock(MotionEvent event) {
        final int action = MotionEventCompat.getActionMasked(event);
        final int pointerIdx = MotionEventCompat.findPointerIndex(event, mDraggingPointerId);

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                // Save position of Down event for later use when (if) minimum dragging distance
                // threshold has been met. By not calling Dragger.startDragging() here already, we
                // prevent unnecessary block operations until we are sure that the user is dragging.
                mDraggingStart.set(
                        (int) MotionEventCompat.getX(event, pointerIdx),
                        (int) MotionEventCompat.getY(event, pointerIdx));
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                if (mTouchState != TOUCH_STATE_DRAGGING) {
                    // Not dragging yet - compute distance from Down event and start dragging if
                    // far enough.
                    final float deltaX = mDraggingStart.x -
                            MotionEventCompat.getX(event, pointerIdx);
                    final float deltaY = mDraggingStart.y -
                            MotionEventCompat.getY(event, pointerIdx);

                    if (Math.sqrt(deltaX * deltaX + deltaY * deltaY) >= mTouchSlop) {
                        mTouchState = TOUCH_STATE_DRAGGING;
                        mDragger.startDragging(mDraggingBlockView,
                                mDraggingStart.x, mDraggingStart.y);
                    }
                }
                if (mTouchState == TOUCH_STATE_DRAGGING) {
                    mDragger.continueDragging(event);
                }
                return true;
            }
            case MotionEvent.ACTION_POINTER_UP: {
                // Some pointer went up - check whether it was the one used for dragging.
                final int pointerId = MotionEventCompat.getPointerId(
                        event, MotionEventCompat.getActionIndex(event));
                if (pointerId != mDraggingPointerId) {
                    return false;
                }
                // Pointer that went up was used for dragging - treat like ACTION_UP.
                // FALLTHROUGH INTENDED.
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                // Finalize dragging and reset dragging state flags.
                if (mTouchState == TOUCH_STATE_DRAGGING) {
                    if (mDragger.touchingTrashView(event)) {
                        mWorkspace.removeRootBlock(mDragger.getDragRootBlock());
                        mDragger.dropInTrash();
                    } else {
                        mDragger.finishDragging();
                    }
                }
                mTouchState = TOUCH_STATE_NONE;
                mDraggingPointerId = MotionEvent.INVALID_POINTER_ID;
                mDraggingBlockView = null;
                return true;
            }
            default: {
                return false;
            }
        }
    }
}
