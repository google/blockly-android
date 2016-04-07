package com.google.blockly.android.ui;

import android.support.annotation.NonNull;
import android.support.annotation.Size;
import android.support.v4.view.MotionEventCompat;
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
public final class PendingDrag {
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
        this.mController = controller;
        this.mHelper = controller.getWorkspaceHelper();

        assert (actionDown.getAction() == MotionEvent.ACTION_DOWN);

        mTouchedView = touchedView;

        mPointerId = MotionEventCompat.getPointerId(
                actionDown, MotionEventCompat.getActionIndex(actionDown));
        int pointerIdx = MotionEventCompat.findPointerIndex(actionDown, mPointerId);
        mTouchDownBlockX = (int) MotionEventCompat.getX(actionDown, pointerIdx);
        mTouchDownBlockY = (int) MotionEventCompat.getY(actionDown, pointerIdx);

        touchedView.getTouchLocationOnScreen(actionDown, mTouchDownScreen);
        mHelper.screenToWorkspaceCoordinates(mTouchDownScreen, mTouchDownWorkspace);
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
    public Block getRootBlock() {
        return mRootBlockView.getBlock();
    }

    /**
     * @return The root {@link BlockView} of the drag group.
     */
    public BlockView getRootBlockView() {
        return mRootBlockView;
    }

    /**
     * @return The initial workspace location of the drag group / root block.
     */
    public WorkspacePoint getOriginalBlockPosition() {
        return mOriginalBlockPosition;
    }
}
