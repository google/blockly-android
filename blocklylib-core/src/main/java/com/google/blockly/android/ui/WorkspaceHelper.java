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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.view.ViewParent;

import com.google.blockly.android.AbstractBlocklyActivity;
import com.google.blockly.android.ZoomBehavior;
import com.google.blockly.android.clipboard.BlockClipDataHelper;
import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.model.Block;
import com.google.blockly.model.WorkspacePoint;

/**
 * Provides helper methods for view traversal and coordinate conversions.
 * <p/>
 * There are two primary coordinate systems, workspace coordinates and virtual view coordinates.
 * <p/>
 * <em>Workspace coordinates</em> represent Block model positions in an infinite xy plane with no
 * pre-defined offset.
 * <p/>
 * <em>Virtual view coordinates</em> are workspace coordinates scaled to adjust for device display
 * density ({@link #mDensity}) and expressed relative to an offset that facilitates workspace
 * scrolling. In right-to-left (RTL) mode ({@link #mRtl}), the virtual view coordinates also flip
 * the x coordinates ({@code x *= -1}) relative to workspace coordinates.
 * <p/>
 * The conversion from workspace to virtual view coordinates is as follows:
 * <ul>
 * <li><pre>virtualViewX = workspaceX * density * rtl - virtualViewOffsetX</pre></li>
 * <li><pre>virtualViewY = workspaceY * density - virtualViewOffsetY</pre></li>
 * </ul>
 * where
 * <ul>
 * <li><pre>density</pre> is display density,</li>
 * <li><pre>rtl<pre> is -1 in RTL mode or +1 in LTR mode,</li>
 *    <li><pre>virtualViewOffsetX,Y</pre> is the offset of the workspace view expressed in virtual
 *    view coordinates.</li>
 * </ul>
 * The virtual view offset is updated during workspace panning as:
 * <p/>
 * <pre>virtualViewOffsetNewX,Y = virtualViewOffsetOldX,Y + (1  / viewScale) * dragVectorX,Y</pre>.
 */
public class WorkspaceHelper {
    private static final String TAG = "WorkspaceHelper";

    // Blocks "snap" toward each other at the end of drags if they have compatible connections
    // near each other.  This is the farthest they can snap at 1.0 zoom, in workspace units.
    private static final int DEFAULT_MAX_SNAP_DISTANCE = 24;

    private final ViewPoint mVirtualWorkspaceViewOffset = new ViewPoint();
    private final ViewPoint mTempViewPoint = new ViewPoint();
    private final WorkspacePoint mTempWorkspacePoint = new WorkspacePoint();
    private final int[] mTempIntArray2 = new int[2];
    private final Context mContext;
    private final ZoomBehavior mZoomBehavior;

    private WorkspaceView mWorkspaceView;
    private VirtualWorkspaceView mVirtualWorkspaceView;
    private BlockViewFactory mViewFactory;
    private float mDensity;
    private boolean mRtl;

    /**
     * Determine if {@code dragEvent} is a block drag.
     * @param viewContext The context of the view receiving the drag event.
     * @param dragEvent The drag event in question.
     * @return True if the drag represents a block drag. Otherwise false.
     */
    public static boolean isBlockDrag(Context viewContext, DragEvent dragEvent) {
        // Unwrap ContextWrappers until the Activity is found.
        while (viewContext instanceof ContextWrapper && !(viewContext instanceof Activity)) {
            viewContext = ((ContextWrapper) viewContext).getBaseContext();
        }
        BlocklyController controller = (viewContext instanceof AbstractBlocklyActivity) ?
                ((AbstractBlocklyActivity) viewContext).getController() : null;
        BlockClipDataHelper clipHelper =
                (controller == null) ? null : controller.getClipDataHelper();
        return (clipHelper != null && clipHelper.isBlockData(dragEvent.getClipDescription()));
    }

    /**
     * Create a helper for creating and doing calculations for views in the workspace.
     *
     * @param context The {@link Context} of the fragment or activity this lives in.
     */
    public WorkspaceHelper(Context context) {
        mContext = context;
        mZoomBehavior = ZoomBehavior.loadFromTheme(context);

        final Resources res = mContext.getResources();
        mDensity = res.getDisplayMetrics().density;
        if (mDensity == 0) {
            Log.e(TAG, "Density is not defined for this context. Defaulting to 1.");
            mDensity = 1f;
        }

        updateRtl(res);
    }

    /**
     * Pairs a {@code blockViewFactory} with this helper instance.
     *
     * @param blockViewFactory The factory used to construct block views and subviews for this app.
     */
    void setBlockViewFactory(BlockViewFactory blockViewFactory) {
        if (mViewFactory != null && mViewFactory != blockViewFactory) {
            throw new IllegalStateException("BlockViewFactory already set. Only one allowed.");
        }
        mViewFactory = blockViewFactory;
    }

    /**
     * @return The BlockViewFactory for this application.
     */
    public BlockViewFactory getBlockViewFactory() {
        return mViewFactory;
    }

    /**
     * @return The {@link WorkspaceView} for which this is a helper.
     */
    public WorkspaceView getWorkspaceView() {
        return mWorkspaceView;
    }

    /**
     * Sets the workspace view to use when converting between coordinate systems.
     */
    public void setWorkspaceView(WorkspaceView workspaceView) {
        mWorkspaceView = workspaceView;
        mVirtualWorkspaceView = (VirtualWorkspaceView) mWorkspaceView.getParent();
    }

    /**
     * Convenience method for {@link BlockViewFactory#getView(Block)}.
     *
     * @param block The Block to view.
     * @return The view that was constructed for a given Block object, if any.
     */
    @Nullable
    public BlockView getView(Block block) {
        return (mViewFactory == null) ? null : mViewFactory.getView(block);
    }

    /**
     * Gets the first block up the hierarchy that can be dragged by the user. If the starting
     * block can be manipulated it will be returned.
     *
     * @param startingView The original view that was touched.
     * @return The nearest parent block that the user can manipulate.
     */
    public BlockView getNearestActiveView(BlockView startingView) {
        Block block = startingView.getBlock();
        while (block != null) {
            if (block.isDraggable()) {
                return getView(block);
            }
            block = block.getParentBlock();
        }
        return null;
    }

    /**
     * Set the offset of the virtual workspace view.
     * <p/>
     * This is the coordinate of the top-left corner of the area of the workspace shown by a
     * {@link WorkspaceView} inside a {@link VirtualWorkspaceView}. The coordinate is represented
     * in virtual workspace view coordinates, i.e., workspace coordinates adjusted for display
     * density and reversed in RTL mode (this implies, that the coordinate provides here refers
     * to the top-left corner of the view area, even in RTL mode).
     */
    public void setVirtualWorkspaceViewOffset(int x, int y) {
        mVirtualWorkspaceViewOffset.set(x, y);
    }

    /**
     * @return The maximum distance a block can snap to match a connection, in workspace units.
     */
    // TODO(#477): Return floating point.
    public int getMaxSnapDistance() {
        // TODO(#62): Adapt to WorkspaceView zoom, if connected.
        return DEFAULT_MAX_SNAP_DISTANCE;
    }

    /**
     * Scales a value in workspace coordinate units to virtual view pixel units.
     * <p/>
     * This does not account for offsets into the view's space, nor any scale applied by
     * {@link VirtualWorkspaceView}, but only uses screen density to  calculate the result.
     *
     * @param workspaceValue The value in workspace units.
     *
     * @return The value in virtual view units.
     */
    public int workspaceToVirtualViewUnits(float workspaceValue) {
        return (int) (mDensity * workspaceValue);
    }

    /**
     * Scales a value in virtual view units to workspace units.
     * <p/>
     * This does not account for offsets into the view's space, nor any scale applied by
     * {@link VirtualWorkspaceView}, but only uses screen density to  calculate the result.
     *
     * @param viewValue The value in virtual view units.
     *
     * @return The value in workspace units.
     */
    public int virtualViewToWorkspaceUnits(float viewValue) {
        return (int) (viewValue / mDensity);
    }

    public float getWorkspaceViewScale() {
        return mWorkspaceView.getScaleX();
    }

    /**
     * Function that converts x and y components of a delta vector from virtual view to workspace
     * coordinates.
     * <p/>
     * This function respects right-to-left mode by reversing the direction of the x coordinate, but
     * does not apply the workspace offset.
     */
    public void virtualViewToWorkspaceDelta(ViewPoint viewPointIn,
                                            WorkspacePoint workspacePointOut) {
        workspacePointOut.set(
                (mRtl ? -1 : 1) * virtualViewToWorkspaceUnits(viewPointIn.x),
                virtualViewToWorkspaceUnits(viewPointIn.y));
    }

    /**
     * Function that converts x and y components of a delta vector from workspace to virtual view
     * units.
     * <p/>
     * This function respects right-to-left mode by reversing the direction of the x coordinate, but
     * does not apply the workspace offset.
     */
    public void workspaceToVirtualViewDelta(WorkspacePoint workspacePointIn,
                                            ViewPoint viewPointOut) {
        viewPointOut.set(
                workspaceToVirtualViewUnits(mRtl ? -workspacePointIn.x : workspacePointIn.x),
                workspaceToVirtualViewUnits(workspacePointIn.y));
    }

    /**
     * @return True if using Right to Left layout, false otherwise.
     */
    public boolean useRtl() {
        return mRtl;
    }

    /**
     * Get workspace coordinates of a given {@link View}, relative to the nearest WorkspaceView or
     * RecyclerView.
     * <p/>
     * This function always returns the coordinate of the corner of the view that corresponds to the
     * block coordinate in workspace coordinates. In left-to-right (LTR) mode, this is the
     * <em>top-left</em> corner of the view, in right-to-left (RTL) mode, it is the
     * <em>top-right</em> corner of the view.
     *
     * @param view The view to find the position of.
     * @param outCoordinate The Point to store the results in.
     */
    public void getWorkspaceCoordinates(@NonNull View view, WorkspacePoint outCoordinate) {
        getVirtualViewCoordinates(view, mTempViewPoint);
        if (mRtl) {
            // In right-to-left mode, the Block's position is that of its top-RIGHT corner, but
            // Android still refers to the BlockView's layout coordinate by its top-LEFT corner.
            // Adding the view's width to the lhs view coordinate gives us the rhs coordinate.
            mTempViewPoint.x += view.getMeasuredWidth();
        }
        virtualViewToWorkspaceCoordinates(mTempViewPoint, outCoordinate);
    }

    /**
     * Get virtual view coordinates of a given {@link View}.
     * <p/>
     * This function always returns the coordinate of the top-left corner of the given view,
     * regardless of left-to-right (LTR) vs. right-to-left (RTL) mode. Note that in RTL mode, this
     * is not the corner that corresponds to the block's workspace coordinates. Use
     * {@link #getWorkspaceCoordinates(View, WorkspacePoint)} to obtain  the workspace coordinates
     * of a block from its view, adjusted for RTL mode if necessary.
     *
     * @param view The view to find the position of.
     * @param viewPosition The Point to store the results in.
     */
    public void getVirtualViewCoordinates(View view, ViewPoint viewPosition) {
        int leftRelativeToWorkspace = view.getLeft();
        int topRelativeToWorkspace = view.getTop();

        // Move up the parent hierarchy and add parent-relative view coordinates.
        ViewParent viewParent = view.getParent();
        while (viewParent != null) {
            if (viewParent instanceof WorkspaceView || viewParent instanceof RecyclerView) {
                break;
            }

            leftRelativeToWorkspace += ((View) viewParent).getLeft();
            topRelativeToWorkspace += ((View) viewParent).getTop();

            viewParent = viewParent.getParent();
        }

        if (viewParent == null) {
            throw new IllegalStateException(
                    "No WorkspaceView or RecyclerView found among view's parents.");
        }

        viewPosition.x = leftRelativeToWorkspace;
        viewPosition.y = topRelativeToWorkspace;
    }

    /**
     * Find the highest {@link BlockGroup} in the hierarchy that this {@link Block} descends from.
     *
     * @param block The block to start searching from.
     *
     * @return The highest {@link BlockGroup} found.
     */
    @Nullable
    public BlockGroup getRootBlockGroup(Block block) {
        BlockView bv = getView(block.getRootBlock());
        return (bv == null) ? null : (BlockGroup) bv.getParent();
    }


    /**
     * Find the highest {@link BlockGroup} in the hierarchy that contains this {@link BlockView}.
     *
     * @param blockView The BlockView to start searching from.
     *
     * @return The highest {@link BlockGroup} found.
     */
    @Nullable
    public BlockGroup getRootBlockGroup(BlockView blockView) {
        return getRootBlockGroup(blockView.getBlock());
    }

    /**
     * Find the closest {@link BlockGroup} in the hierarchy that this {@link Block} descends from.
     *
     * @param block The block to start searching from.
     *
     * @return The {@link BlockGroup} parent of the block's view, otherwise null.
     * @throws IllegalStateException when a BlockView is found without a parent BlockGroup.
     */
    @Nullable
    public BlockGroup getParentBlockGroup(Block block) {
        BlockView blockView = getView(block);
        if (blockView != null) {
            BlockGroup bg = blockView.getParentBlockGroup();
            if (bg == null) {
                throw new IllegalStateException("Block has a BlockView, but no parent BlockGroup.");
            }
            return bg;
        }
        return null; // Block does not have a view, and thus no parent BlockGroup.
    }

    /**
     * Finds the closest BlockView that contains the {@code descendantView} as a child or further
     * descendant.
     *
     * @param descendantView The contained {@code view}.
     * @return The closest BlockView that contains the {@code descendantView}.
     */
    @Nullable
    public BlockView getClosestAncestorBlockView(View descendantView) {
        ViewParent parent = descendantView.getParent();
        while (parent != null) {
            if (parent instanceof BlockView) {
                return (BlockView) parent;
            }
            parent = parent.getParent();
        }
        return null; // Not found.
    }

    /**
     * Converts a point in virtual view coordinates to screen coordinates.
     *
     * @param viewPositionIn Input coordinates of a location in {@link WorkspaceView}, expressed
     * with respect to the virtual view coordinate system.
     * @param screenPositionOut Output coordinates of the same location in absolute coordinates on
     * the screen.
     */
    public void virtualViewToScreenCoordinates(ViewPoint viewPositionIn, Point screenPositionOut) {
        mWorkspaceView.getLocationOnScreen(mTempIntArray2);
        screenPositionOut.x = mTempIntArray2[0] +
                (int) (viewPositionIn.x * mWorkspaceView.getScaleX());
        screenPositionOut.y = mTempIntArray2[1] +
                (int) (viewPositionIn.y * mWorkspaceView.getScaleY());
    }

    /**
     * Converts a point in screen coordinates to virtual view coordinates.
     *
     * @param screenPositionIn Input coordinates of a location in absolute coordinates on the
     * screen.
     * @param viewPositionOut Output coordinates of the same location in {@link WorkspaceView},
     * expressed with respect to the virtual view coordinate system.
     */
    public void screenToVirtualViewCoordinates(@Size(2) int[] screenPositionIn,
                                               ViewPoint viewPositionOut) {
        mWorkspaceView.getLocationOnScreen(mTempIntArray2);
        viewPositionOut.x =
                (int) ((screenPositionIn[0] - mTempIntArray2[0]) / mWorkspaceView.getScaleX());
        viewPositionOut.y =
                (int) ((screenPositionIn[1] - mTempIntArray2[1]) / mWorkspaceView.getScaleY());
    }

    /**
     * Convenience method for direct mapping of screen to workspace coordinates.
     * <p/>
     * This method applies {@link #screenToVirtualViewCoordinates} followed by
     * {@link #virtualViewToWorkspaceCoordinates(ViewPoint, WorkspacePoint)} using an existing
     * temporary {@link ViewPoint} instance as intermediate.
     *
     * @param screenPositionIn Input coordinates of a location in absolute coordinates on the
     * screen.
     * @param workspacePositionOut Output coordinates of the same location in workspace coordinates.
     */
    // TODO(#248): Support floating point queries.
    public void screenToWorkspaceCoordinates(@Size(2) int[] screenPositionIn,
                                             WorkspacePoint workspacePositionOut) {
        screenToVirtualViewCoordinates(screenPositionIn, mTempViewPoint);
        virtualViewToWorkspaceCoordinates(mTempViewPoint, workspacePositionOut);
    }

    /**
     * Converts a point in workspace coordinates to virtual view coordinates, storing the result in
     * the second parameter. The resulting coordinates will be in the
     * {@link WorkspaceView WorkspaceView's} coordinates, relative to the current
     * workspace view offset.
     *
     * @param workspacePosition The position to convert to view coordinates.
     * @param viewPosition The Point to store the results in.
     */
    public void workspaceToVirtualViewCoordinates(WorkspacePoint workspacePosition,
                                                  ViewPoint viewPosition) {
        float workspaceX = workspacePosition.x;
        if (mRtl) {
            workspaceX *= -1;
        }
        viewPosition.x = workspaceToVirtualViewUnits(workspaceX) - mVirtualWorkspaceViewOffset.x;
        viewPosition.y = workspaceToVirtualViewUnits(workspacePosition.y) -
                mVirtualWorkspaceViewOffset.y;
    }

    /**
     * Assigns {@code rect} the given bounds, possibly flipping horizontal bounds in RTL mode.
     *
     * @param ltrStart The left coordinate in LTR mode.
     * @param top The top coordinate.
     * @param ltrEnd The right coordinate in LTR mode.
     * @param bottom The bottom coordinate.
     */
    public void setRtlAwareBounds(Rect rect, int parentWidth,
                                  int ltrStart, int top, int ltrEnd, int bottom) {
        boolean isRtl = useRtl();
        rect.left = isRtl ? parentWidth - ltrEnd : ltrStart;
        rect.top = top;
        rect.right = isRtl ? parentWidth - ltrStart : ltrEnd;
        rect.bottom = bottom;
    }

    /**
     * Set a {@link ViewPoint} and flip x coordinate in RTL mode.
     *
     * @param viewPoint The point in view coordinates to set.
     * @param x The new x coordinate in LTR mode.
     * @param y The  new y coordinate.
     */
    public void setPointMaybeFlip(ViewPoint viewPoint, int x, int y) {
        viewPoint.set(useRtl() ? -x : x, y);
    }

    /**
     * Converts a point in virtual view coordinates to workspace coordinates, storing the result in
     * the second parameter. The view position should be in the {@link WorkspaceView} coordinates in
     * pixels.
     *
     * @param viewPosition The position to convert to workspace coordinates.
     * @param outCoordinate The Point to store the results in.
     */
    public void virtualViewToWorkspaceCoordinates(ViewPoint viewPosition,
                                                  WorkspacePoint outCoordinate) {
        int workspaceX =
                virtualViewToWorkspaceUnits(viewPosition.x + mVirtualWorkspaceViewOffset.x);
        if (mRtl) {
            workspaceX *= -1;
        }
        outCoordinate.x = workspaceX;
        outCoordinate.y =
                virtualViewToWorkspaceUnits(viewPosition.y + mVirtualWorkspaceViewOffset.y);
    }

    /**
     * Returns true if the view is a child or deeper descendant of the {@link WorkspaceView}
     * associated with this WorkspaceHelper.
     *
     * @param view The potential child view in question.
     * @return True if {@code touchedView} is a descendant.
     */
    public boolean isInWorkspaceView(BlockView view) {
        return view.getWorkspaceView() == mWorkspaceView;
    }

    /**
     * @return The zoom scale of the workspace, where > 1.0 is enlarged ("zoomed in").
     */
    public float getWorkspaceZoomScale() {
        // Workspace scale is simply the scale of the WorkspaceView, equal in both directions.
        return (mWorkspaceView == null) ? 1.0f : mWorkspaceView.getScaleX();
    }

    /**
     * Gets the visible bounds of the workspace, in workspace units.
     *
     * @param outRect The {@link RectF} in which to store the bounds values.
     * @return {@code outRect}
     */
    public RectF getViewableWorkspaceBounds(RectF outRect) {
        mTempViewPoint.set(0, 0);
        virtualViewToWorkspaceCoordinates(mTempViewPoint, mTempWorkspacePoint);
        outRect.left = (int) mTempWorkspacePoint.x;
        outRect.top = (int) mTempWorkspacePoint.y;

        mTempViewPoint.set(mWorkspaceView.getWidth(), mWorkspaceView.getHeight());
        virtualViewToWorkspaceCoordinates(mTempViewPoint, mTempWorkspacePoint);
        outRect.right = (int) mTempWorkspacePoint.x;
        outRect.bottom = (int) mTempWorkspacePoint.y;
        return outRect;
    }

    /**
     * @return The ZoomBehavior for workspaces in this context.
     */
    public ZoomBehavior getZoomBehavior() {
        return mZoomBehavior;
    }

    /**
     * Updates the current RTL state for the app.
     *
     * Lint Suppressed as plan is to support down to API 16.
     *
     * @param resources The context resources to get the RTL setting from.
     */
    @SuppressLint("ObsoleteSdkInt")
    private void updateRtl(Resources resources) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            mRtl = resources.getConfiguration().getLayoutDirection()
                    == View.LAYOUT_DIRECTION_RTL;
        } else {
            mRtl = false;  // RTL not supported.
        }
    }
}
