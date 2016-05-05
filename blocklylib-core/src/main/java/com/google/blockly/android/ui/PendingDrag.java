package com.google.blockly.android.ui;

import android.support.annotation.NonNull;
import android.support.annotation.Size;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.model.Block;
import com.google.blockly.model.Workspace;
import com.google.blockly.model.WorkspacePoint;

/**
 * {@code PendingDrag} collects all the information related to an in-progress drag of a
 * {@link BlockView}.  It is initialized by {@link Dragger}, passed to a {@link Dragger.DragHandler}
 * which calls {@link #setDragGroup(BlockGroup)} to inform the dragger how to complete the rest of
 * the drag behavior.
 */
// TODO(#233): Rename to PendingGesture or similar
public final class PendingDrag {
    /**
     * This threshold is used to detect bad state from invalid MotionEvent streams.  There are cases
     * where an intercepting OnTouchListener never receives an appropriate ACTION_CANCEL or
     * ACTION_UP event.  For example, on API 23, when dragging away from a Spinner (i.e., drop-down
     * field), the MotionEvent stream will stop as soon as the Spinner popup opens.
     * </p>
     * We assume that during a drag, the system should continue to receive events on the touch
     * stream at least this often during the drag.  Further, by detecting and resetting the state,
     * it is possible to recover for new drags, rather than locking the drag state in under the
     * presumption the missing UP or CANCEL will eventually arrive.  The latter case prevents
     * the {@link Dragger} from detecting future drag gestures.
     */
    private long MAX_MOTION_EVENT_MILLISECONDS_DELTA = 500;

    private final BlocklyController mController;
    private final WorkspaceHelper mHelper;
    private final BlockView mTouchedView;
    private final int mPointerId;

    // The screen location of the first touch, in device pixel units.
    private final @Size(2) int[] mTouchDownScreen = new int[2];

    /**
     * The workspace location of the first touch, even if the touch occured outside the
     * {@link VirtualWorkspaceView}.
     */
    private final WorkspacePoint mTouchDownWorkspace = new WorkspacePoint();

    // The coordinates within the BlockView of the first touch, in local pixel units
    // (possibly scaled by zoom, if within a WorkspaceView).
    private final int mTouchDownBlockX;
    private final int mTouchDownBlockY;

    private BlockGroup mDragGroup;
    private BlockView mRootBlockView;
    private WorkspacePoint mOriginalBlockPosition = new WorkspacePoint();

    // One gesture detector per drag ensures bad state will not carry over due to event bugs.
    // The GestureDetector class is relatively lightweight (as of API 23), without any
    // instantiations at construction.
    private final GestureDetectorCompat mGestureDetector;

    private long mLatestEventTime;
    private boolean mAlive = true;
    private boolean mClicked;

    /**
     * Constructs a new PendingDrag that, if accepted by the DragHandler, begins with the
     * {@code actionDown} event.
     *
     * @param controller The activity's {@link BlocklyController}.
     * @param touchedView The initial touched {@link BlockView} of the drag.
     * @param actionDown The first {@link MotionEvent#ACTION_DOWN} event.
     */
    PendingDrag(@NonNull BlocklyController controller,
                @NonNull BlockView touchedView, @NonNull MotionEvent actionDown) {
        assert (actionDown.getAction() == MotionEvent.ACTION_DOWN);

        mController = controller;
        mHelper = controller.getWorkspaceHelper();

        mLatestEventTime = actionDown.getEventTime();

        mTouchedView = touchedView;

        mPointerId = MotionEventCompat.getPointerId(
                actionDown, MotionEventCompat.getActionIndex(actionDown));
        int pointerIdx = MotionEventCompat.findPointerIndex(actionDown, mPointerId);
        mTouchDownBlockX = (int) MotionEventCompat.getX(actionDown, pointerIdx);
        mTouchDownBlockY = (int) MotionEventCompat.getY(actionDown, pointerIdx);

        touchedView.getTouchLocationOnScreen(actionDown, mTouchDownScreen);
        mHelper.screenToWorkspaceCoordinates(mTouchDownScreen, mTouchDownWorkspace);

        mGestureDetector = new GestureDetectorCompat(mController.getContext(),
                new GestureListener());
        mGestureDetector.onTouchEvent(actionDown);
    }

    /**
     * @return True if this PendingDrag has received a continuous stream of events for its pointer.
     */
    public boolean isAlive() {
        return mAlive;
    }

    /**
     * @return The initial touched {@link BlockView} of the drag.
     */
    public BlockView getTouchedBlockView() {
        return mTouchedView;
    }

    /**
     * @return The pointer id for this drag.
     */
    public int getPointerId() {
        return mPointerId;
    }

    /**
     * @return The screen coordinates of the initial {@link MotionEvent#ACTION_DOWN} event.
     */
    public void getTouchDownScreen(@Size(2) int[] screenCoordOut) {
        screenCoordOut[0] = mTouchDownScreen[0];
        screenCoordOut[1] = mTouchDownScreen[1];
    }

    /**
     * @return The X offset of the initial {@link MotionEvent#ACTION_DOWN} event, from the left side
     *         of the view in local view pixels.
     */
    public float getTouchDownViewOffsetX() {
        return mTouchDownBlockX;
    }

    /**
     * @return The Y offset of the initial {@link MotionEvent#ACTION_DOWN} event, from the top of
     *         the view in local view pixels.
     */
    public float getTouchDownViewOffsetY() {
        return mTouchDownBlockY;
    }

    /**
     * @return The workspace coordinates of the initial {@link MotionEvent#ACTION_DOWN} event.
     */
    public WorkspacePoint getTouchDownWorkspaceCoordinates() {
        return mTouchDownWorkspace;
    }

    /**
     * This sets the draggable {@link BlockGroup}, containing all the dragged blocks.
     * {@code dragGroup} must be a root block added to the {@link WorkspaceView}, with it's first
     * {@link Block} added as a root block in the {@link Workspace}.  The touch offset will be
     * inferred from the delta between block's workspace location and the initial touch down
     * workspace location.
     *
     * @param dragGroup The draggable {@link BlockGroup}.
     */
    public void setDragGroup(@NonNull BlockGroup dragGroup) {
        if (dragGroup == null) {
            throw new IllegalArgumentException("DragGroup cannot be null");
        }
        if (mDragGroup != null) {
            throw new IllegalStateException("Drag group already assigned.");
        }
        if (!mController.getWorkspace().isRootBlock(dragGroup.getFirstBlock())) {
            throw new IllegalArgumentException("Drag group must be root block in workspace");
        }

        mDragGroup = dragGroup;

        // Save reference to root block, so we know which block if dropped into another group
        mRootBlockView = dragGroup.getFirstBlockView();
        if (mRootBlockView == null) {
            throw new IllegalStateException();
        }

        mOriginalBlockPosition.setFrom(dragGroup.getFirstBlock().getPosition());
    }

    /**
     * @return Whether the drag group has been assigned the this drag should be active.
     */
    public boolean isDragging() {
        return mDragGroup != null;
    }

    /**
     * @return The dragged {@link BlockGroup} for this drag.
     */
    public BlockGroup getDragGroup() {
        return mDragGroup;
    }

    /**
     * @return The root {@link Block} of the drag group.
     */
    public Block getRootDraggedBlock() {
        return mRootBlockView.getBlock();
    }

    /**
     * @return The root {@link BlockView} of the drag group.
     */
    public BlockView getRootDraggedBlockView() {
        return mRootBlockView;
    }

    /**
     * @return The initial workspace location of the drag group / root block.
     */
    public WorkspacePoint getOriginalBlockPosition() {
        return mOriginalBlockPosition;
    }

    public boolean isClick() {
        return mClicked;
    }

    /**
     * Compares if {@code event} on {@code touchedView} is a continuation of the event stream
     * tracked by this PendingDrag.  This includes whether the event stream has had sufficient
     * regular updates, at least more often than {@link #MAX_MOTION_EVENT_MILLISECONDS_DELTA}
     * (in an effort to disregard it from dropped previous streams with dropped
     * {@link MotionEvent#ACTION_UP} and {@link MotionEvent#ACTION_CANCEL}).  If that threshold
     * is exceeded (for matching view and pointer id), the PendingDrag will no longer be alive
     * ({@link #isAlive()}, and not match any future events.
     * <p/>
     * If the event is a match and alive, it will pass the event through a {@link GestureDetector}
     * to determine if the event triggers a click (or other interesting gestures in the future).
     * Check {@link #isClick()} to determine whether a click was detected.
     * <p/>
     * This method should only be called from {@link Dragger#onTouchBlockImpl}.
     *
     * @param event The event to compare to.
     * @param touchedView The view that received the touch event.
     * @return Whether the event was a match and the drag is still alive.
     */
    boolean isMatchAndProcessed(MotionEvent event, BlockView touchedView) {
        if (!mAlive) {
            return false;
        }

        final int pointerId = MotionEventCompat.getPointerId(
                event, MotionEventCompat.getActionIndex(event));
        long curEventTime = event.getEventTime();
        long deltaMs = curEventTime - mLatestEventTime;
        if (deltaMs < MAX_MOTION_EVENT_MILLISECONDS_DELTA) {
            if (pointerId == mPointerId && touchedView == mTouchedView) {
                mLatestEventTime = curEventTime;
                mGestureDetector.onTouchEvent(event);
                return true;
            }
        } else {
            mAlive = false; // Exceeded threshold and expired.
        }

        return false;  // Not a pointer & view match or died.
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            mClicked = true;
            return true; // Not actually consumed anywhere.
        }
    }
}
