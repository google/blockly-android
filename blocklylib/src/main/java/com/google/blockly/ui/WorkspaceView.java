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

import android.content.ClipData;
import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.IntDef;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import com.google.blockly.control.Dragger;
import com.google.blockly.model.Workspace;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Handles updating the viewport into the workspace and is the parent view for all blocks. This
 * view is responsible for handling drags. A drag on the workspace will move the viewport and a
 * drag on a block or stack of blocks will drag those within the workspace.
 */
public class WorkspaceView extends ViewGroup {
    private static final String TAG = "WorkspaceView";
    private static final boolean DEBUG = true;
    public static final String BLOCK_GROUP_CLIP_DATA_LABEL = "BlockGroupClipData";

    // No current touch interaction.
    private static final int TOUCH_STATE_NONE = 0;
    // Block in this view has received "Down" event; waiting for further interactions to decide
    // whether to drag, show context menu, etc.
    private static final int TOUCH_STATE_DOWN = 1;
    // Block in this view is being dragged.
    private static final int TOUCH_STATE_DRAGGING = 2;
    // Block in this view received a long press.
    private static final int TOUCH_STATE_LONGPRESS = 3;
    private final ViewPoint mTemp = new ViewPoint();
    // Distance threshold for detecting drag gestures.
    private final float mTouchSlop;
    private final ViewPoint mDraggingStart = new ViewPoint();
    // Viewport bounds. These define the bounding box of all blocks, in view coordinates, and
    // are used to determine ranges and offsets for scrolling.
    private final Rect mBlocksBoundingBox = new Rect();
    private Workspace mWorkspace;
    private WorkspaceHelper mHelper;
    // Current state of touch interaction with blocks in this workspace view.
    @TouchState
    private int mTouchState = TOUCH_STATE_NONE;
    // Fields for dragging blocks in the workspace.
    private int mDraggingPointerId = MotionEvent.INVALID_POINTER_ID;
    private BlockView mDraggingBlockView = null;
    private Dragger mDragger;
    private View mTrashView;

    public WorkspaceView(Context context) {
        this(context, null);
    }

    public WorkspaceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WorkspaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setOnDragListener(new WorkspaceDragEventListener());
    }

    public Workspace getWorkspace() {
        return mWorkspace;
    }

    /**
     * Sets the workspace this view should display.
     *
     * @param workspace The workspace to load views for.
     */
    public void setWorkspace(Workspace workspace) {
        mWorkspace = workspace;
        if (workspace != null) {
            mHelper = mWorkspace.getWorkspaceHelper();
        }
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

    /**
     * @return The bounding box in view coordinates of the workspace region occupied by blocks.
     */
    public Rect getBlocksBoundingBox() {
        return mBlocksBoundingBox;
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        mBlocksBoundingBox.setEmpty();
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            BlockGroup blockGroup = (BlockGroup) getChildAt(i);
            blockGroup.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);

            // Determine this BlockGroup's bounds in view coordinates and extend boundaries
            // accordingly. Do NOT use mHelper.workspaceToVirtualViewCoordinates below, since we want the
            // bounding box independent of scroll offset.
            mHelper.workspaceToVirtualViewDelta(blockGroup.getTopBlockPosition(), mTemp);
            if (mHelper.useRtL()) {
                mTemp.x -= blockGroup.getMeasuredWidth();
            }

            mBlocksBoundingBox.union(mTemp.x, mTemp.y,
                    mTemp.x + blockGroup.getMeasuredWidth(),
                    mTemp.y + blockGroup.getMeasuredHeight());
        }

        setMeasuredDimension(width, height);
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

    /**
     * Record information that will be useful during a drag, including which pointer to use (in
     * case of multitouch) and which block will be dragged.
     *
     * @param blockView The {@link BlockView} that will be dragged.
     * @param event The {@link MotionEvent} that starts the drag.
     */
    public void setDragFocus(BlockView blockView, MotionEvent event) {
        mTouchState = TOUCH_STATE_DOWN;
        mDraggingBlockView = blockView;
        mDraggingPointerId =
                MotionEventCompat.getPointerId(event, MotionEventCompat.getActionIndex(event));
    }

    /**
     * Set the start location of the drag, in virtual view units.
     *
     * @param x The x position of the initial down event.
     * @param y The y position of the initial down event.
     */
    public void setDraggingStart(int x, int y) {
        mDraggingStart.set(x, y);
    }

    /**
     * Start a drag event on the view recorded in setDragFocus.
     */
    public void startDrag() {
        mTouchState = TOUCH_STATE_DRAGGING;
        mDragger.startDragging(mDraggingBlockView,
                mDraggingStart.x, mDraggingStart.y);
        mDraggingBlockView.startDrag(
                ClipData.newPlainText(BLOCK_GROUP_CLIP_DATA_LABEL, ""),
                new DragShadowBuilder(), null, 0);
    }

    /**
     * Let this instance know that a block was touched.
     * This will be called when the block has been touched but a drag has not yet been started.
     *
     * @param blockView The {@link BlockView} that detected a touch event.
     * @param event The touch event.
     *
     * @return true if the WorkspaceView has started dragging the given {@link BlockView} or
     * recorded it as draggable.
     */
    public boolean onTouchBlock(BlockView blockView, MotionEvent event) {
        // Only initiate dragging of given view if in idle state - this prevents occluded blocks
        // from grabbing drag focus because they saw an unconsumed Down event before it propagated
        // back up to this WorkspaceView.
        if (mTouchState == TOUCH_STATE_NONE) {
            setDragFocus(blockView, event);
        }
        return (mTouchState == TOUCH_STATE_DOWN || mTouchState == TOUCH_STATE_DRAGGING)
                && maybeStartDrag(blockView, event);
    }

    /**
     * Handle motion events while starting to drag a block.  This keeps track of whether the block
     * has been dragged more than {@code mTouchSlop} and starts a drag if necessary.
     * Once the drag has been started, all following events will be handled through the
     * {@link com.google.blockly.ui.WorkspaceView.WorkspaceDragEventListener}.
     *
     * @return True if the event was ACTION_DOWN or ACTION_MOVE, false otherwise.
     */
    private boolean maybeStartDrag(BlockView blockView, MotionEvent event) {
        final int action = MotionEventCompat.getActionMasked(event);
        final int pointerIdx = MotionEventCompat.findPointerIndex(event, mDraggingPointerId);

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                // Save position of Down event for later use when (if) minimum dragging distance
                // threshold has been met. By not calling Dragger.startDragging() here already, we
                // prevent unnecessary block operations until we are sure that the user is dragging.

                // Adjust the event's coordinates from the {@link BlockView}'s coordinate system to
                // {@link WorkspaceView} coordinates.
                getWorkspaceHelper().getWorkspaceViewCoordinates(blockView, mTemp);
                int startX = (int) MotionEventCompat.getX(event, pointerIdx);
                int startY = (int) MotionEventCompat.getY(event, pointerIdx);
                mDraggingStart.set(mTemp.x + startX, mTemp.y + startY);
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
                        startDrag();
                    }
                }
                return true;
            }
            default: {
                return false;
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int childCount = getChildCount();

        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }
            if (child instanceof BlockGroup) {
                BlockGroup bg = (BlockGroup) child;

                // Get view coordinates of child from its workspace coordinates. Note that unlike
                // onMeasure() above, workspaceToViewCoordinates() must be used for conversion here,
                // so view scroll offset is properly applied for positioning.
                mHelper.workspaceToViewCoordinates(bg.getTopBlockPosition(), mTemp);
                if (mHelper.useRtL()) {
                    mTemp.x -= bg.getMeasuredWidth();
                }

                child.layout(mTemp.x, mTemp.y,
                        mTemp.x + bg.getMeasuredWidth(), mTemp.y + bg.getMeasuredHeight());
            }
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({TOUCH_STATE_NONE, TOUCH_STATE_DOWN, TOUCH_STATE_DRAGGING, TOUCH_STATE_LONGPRESS})
    public @interface TouchState {
    }

    private class WorkspaceDragEventListener implements View.OnDragListener {
        @Override
        public boolean onDrag(View v, DragEvent event) {
            final int action = event.getAction();
            switch (action) {
                case DragEvent.ACTION_DRAG_STARTED:
                    return true;
                case DragEvent.ACTION_DRAG_ENTERED:
                    return true;
                case DragEvent.ACTION_DRAG_LOCATION:
                    mDragger.continueDragging(event);
                    return true;
                case DragEvent.ACTION_DRAG_EXITED:
                case DragEvent.ACTION_DRAG_ENDED:
                    return false;
                case DragEvent.ACTION_DROP:
                    // Finalize dragging and reset dragging state flags.
                    // These state flags are still used in the initial phase of figuring out if a
                    // drag has started.
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
                default:
                    return false;

            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int childCount = getChildCount();

        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }
            if (child instanceof BlockGroup) {
                BlockGroup bg = (BlockGroup) child;

                // Get view coordinates of child from its workspace coordinates. Note that unlike
                // onMeasure() above, workspaceToVirtualViewCoordinates() must be used for
                // conversion here, so view scroll offset is properly applied for positioning.
                mHelper.workspaceToVirtualViewCoordinates(bg.getTopBlockPosition(), mTemp);
                if (mHelper.useRtL()) {
                    mTemp.x -= bg.getMeasuredWidth();
                }

                child.layout(mTemp.x, mTemp.y,
                        mTemp.x + bg.getMeasuredWidth(), mTemp.y + bg.getMeasuredHeight());
            }
        }
    }

    @IntDef({TOUCH_STATE_NONE, TOUCH_STATE_DOWN, TOUCH_STATE_DRAGGING, TOUCH_STATE_LONGPRESS})
    public @interface TouchState {
    }
}
