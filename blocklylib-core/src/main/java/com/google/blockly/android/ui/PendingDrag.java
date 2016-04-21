package com.google.blockly.android.ui;

import android.support.annotation.NonNull;
import android.support.annotation.Size;
import android.support.v4.view.MotionEventCompat;
import android.view.MotionEvent;
import android.view.View;

import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.model.Block;
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

    private final Dragger.DragHandler mDragHandler;
    private final BlockView mTouchedView;
    private final int mPointerId;

    // The screen location of the first touch, in device pixel units.
    private final @Size(2) int[] mTouchDownScreen = new int[2];

    /**
     * The workspace location of the first touch, even if the touch occured outside the
     * {@link VirtualWorkspaceView}.
     */
    private final WorkspacePoint mTouchDownWorkspace = new WorkspacePoint();

    // The coordinates within the BlockView fo the first touch,
    // in local pixel units (possibly scaled, if within the workspace).
    private final int mTouchDownBlockX;
    private final int mTouchDownBlockY;

    private BlockGroup mDragGroup;
    private BlockView mRootBlockView;
    private WorkspacePoint mOriginalBlockPosition = new WorkspacePoint();

    PendingDrag(@NonNull BlocklyController controller,
                @NonNull Dragger.DragHandler dragHandler,
                @NonNull BlockView touchedView, @NonNull MotionEvent actionDown) {
        this.mController = controller;
        this.mHelper = controller.getWorkspaceHelper();

        assert (actionDown.getAction() == MotionEvent.ACTION_DOWN);

        mDragHandler = dragHandler;
        mTouchedView = touchedView;

        mPointerId = MotionEventCompat.getPointerId(
                actionDown, MotionEventCompat.getActionIndex(actionDown));
        int pointerIdx = MotionEventCompat.findPointerIndex(actionDown, mPointerId);
        mTouchDownBlockX = (int) MotionEventCompat.getX(actionDown, pointerIdx);
        mTouchDownBlockY = (int) MotionEventCompat.getY(actionDown, pointerIdx);

        touchedView.getLocationOnScreen(mTouchDownScreen);

        // Get local screen coordinates.
        float screenOffsetX = mTouchDownBlockX;
        float screenOffsetY = mTouchDownBlockY;
        if (mHelper.isInWorkspaceView((View) touchedView)) {
            float scale = mHelper.getWorkspaceZoomScale();
            screenOffsetX = mTouchDownBlockX * scale;
            screenOffsetY = mTouchDownBlockY * scale;
        }
        mTouchDownScreen[0] += screenOffsetX;
        mTouchDownScreen[1] += screenOffsetY;
        mHelper.screenToWorkspaceCoordinates(mTouchDownScreen, mTouchDownWorkspace);
    }

    public Dragger.DragHandler getDragHandler() {
        return mDragHandler;
    }

    public BlockView getTouchedBlockView() {
        return mTouchedView;
    }

    public int getPointerId() {
        return mPointerId;
    }

    public void getTouchDownScreen(@Size(2) int[] screenCoordOut) {
        screenCoordOut[0] = mTouchDownScreen[0];
        screenCoordOut[1] = mTouchDownScreen[1];
    }

    public float getTouchDownViewOffsetX() {
        return mTouchDownBlockX;
    }

    public float getTouchDownViewOffsetY() {
        return mTouchDownBlockY;
    }

    public WorkspacePoint getTouchDownWorkspace() {
        return mTouchDownWorkspace;
    }

    /**
     * @param dragGroup The {@link BlockGroup} containing all the dragged blocks.
     */
    public void setDragGroup(BlockGroup dragGroup) {
        if (!mController.getWorkspace().isRootBlock(dragGroup.getFirstBlock())) {
            throw new IllegalArgumentException("dragGroup must be root block in workspace");
        }

        mDragGroup = dragGroup;

        // Save reference to root block, so we know which block if dropped into another group
        mRootBlockView = dragGroup.getFirstBlockView();
        if (mRootBlockView == null) {
            throw new IllegalStateException();
        }

        mOriginalBlockPosition.setFrom(dragGroup.getFirstBlock().getPosition());
    }

    public BlockGroup getDragGroup() {
        return mDragGroup;
    }

    public Block getRootBlock() {
        return mRootBlockView.getBlock();
    }

    public BlockView getRootBlockView() {
        return mRootBlockView;
    }

    public WorkspacePoint getOriginalBlockPosition() {
        return mOriginalBlockPosition;
    }
}
