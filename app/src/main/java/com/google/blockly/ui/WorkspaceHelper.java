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
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;

import com.google.blockly.R;
import com.google.blockly.control.ConnectionManager;
import com.google.blockly.model.Block;
import com.google.blockly.model.WorkspacePoint;

/**
 * Provides helper methods for converting coordinates between the workspace and the views.
 * <p/>
 * There are two primary coordinate systems, workspace coordinates and virtual view coordinates.
 * <p/>
 * <em>Workspace coordinates</em> represent Block model positions in an infinite xy plane with no
 * pre-defined offset.
 * <p/>
 * <em>Virtual view coordinates</em> are workspace coordinates scaled to adjust for device display
 * density ({@link #mDensity}) and expressed relative to an offset that facilitates workspace
 * scrolling. In right-to-left (RtL) mode ({@link #mRtL}), the virtual view coordinates also flip
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
 * <li><pre>rtl<pre> is -1 in RtL mode or +1 in LtR mode,</li>
 *    <li><pre>virtualViewOffsetX,Y</pre> is the offset of the workspace view expressed in virtual
 *    view coordinates.</li>
 * </ul>
 * The virtual view offset is updated during workspace panning as:
 * <p/>
 * <pre>virtualViewOffsetNewX,Y = virtualViewOffsetOldX,Y + (1  / viewScale) * dragVectorX,Y</pre>.
 */
public class WorkspaceHelper {
    private static final String TAG = "WorkspaceHelper";
    private static final boolean DEBUG = false;

    private final ViewPoint mVirtualWorkspaceViewOffset = new ViewPoint();
    private final ViewPoint mTempViewPoint = new ViewPoint();
    private final int[] mTempIntArray2 = new int[2];
    private WorkspaceView mWorkspaceView;
    private float mDensity;
    private boolean mRtL;
    private int mBlockStyle;
    private int mFieldLabelStyle;

    private BlockTouchHandler mBlockTouchHandler;

    /**
     * Create a helper for creating and doing calculations for views in the workspace using the
     * workspace's style.
     *
     * @param workspaceView The {@link WorkspaceView} for which this is a helper.
     * @param attrs The workspace attributes to load the style from.
     */
    public WorkspaceHelper(WorkspaceView workspaceView, AttributeSet attrs) {
        this(workspaceView.getContext(), attrs, 0);
        mWorkspaceView = workspaceView;
    }

    /**
     * Create a helper for creating and doing calculations for views in the workspace using the
     * workspace's style.
     *
     * @param context The {@link Context} of the fragment or activity this lives in.
     * @param attrs The workspace attributes to load the style from.
     */
    public WorkspaceHelper(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        mWorkspaceView = null;
    }

    /**
     * Create a helper for creating and doing calculations for views in the workspace, with a
     * specific style. The style must be a resource id for a style that extends
     * {@link R.style#BlocklyTheme}.
     * <p/>
     * The config and styles are loaded from one of three sources with the following priority.
     * <ol>
     * <li>The specified style id if it is not 0.</li>
     * <li>The attribute's style if it is not null.</li>
     * <li>The context's theme.</li>
     * </ol>
     *
     * @param context The {@link Context} of the fragment or activity this lives in.
     * @param attrs The {@link WorkspaceView} attributes or null.
     * @param workspaceStyle The style to use for views.
     */
    public WorkspaceHelper(Context context, AttributeSet attrs, int workspaceStyle) {
        Resources res = context.getResources();
        mDensity = res.getDisplayMetrics().density;
        if (mDensity == 0) {
            Log.e(TAG, "Density is not defined for this context. Defaulting to 1.");
            mDensity = 1f;
        }

        initConfig(context, attrs, workspaceStyle);
        updateRtL(context);
    }

    /**
     * @return The {@link WorkspaceView} for which this is a helper.
     */
    public WorkspaceView getWorkspaceView() {
        return mWorkspaceView;
    }

    /**
     * Set the offset of the virtual workspace view.
     * <p/>
     * This is the coordinate of the top-left corner of the area of the workspace shown by a
     * {@link WorkspaceView} inside a {@link VirtualWorkspaceView}. The coordinate is represented
     * in virtual workspace view coordinates, i.e., workspace coordinates adjusted for display
     * density and reversed in RtL mode (this implies, that the coordinate provides here refers
     * to the top-left corner of the view area, even in RtL mode).
     */
    public void setVirtualWorkspaceViewOffset(int x, int y) {
        mVirtualWorkspaceViewOffset.set(x, y);
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
    public int workspaceToVirtualViewUnits(int workspaceValue) {
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
    public int virtualViewToWorkspaceUnits(int viewValue) {
        return (int) (viewValue / mDensity);
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
                virtualViewToWorkspaceUnits(mRtL ? -viewPointIn.x : viewPointIn.x),
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
                workspaceToVirtualViewUnits(mRtL ? -workspacePointIn.x : workspacePointIn.x),
                workspaceToVirtualViewUnits(workspacePointIn.y));
    }

    /**
     * @return True if using Right to Left layout, false otherwise.
     */
    public boolean useRtL() {
        return mRtL;
    }

    /**
     * Creates a {@link BlockView} for the given block using the workspace's default style.
     *
     * @param block The block to generate a view for.
     * @param parentGroup The group to set as the parent for this block's view.
     * @param connectionManager The {@link ConnectionManager} to update when moving connections.
     *
     * @return A view for the block.
     */
    public BlockView obtainBlockView(
            Block block, BlockGroup parentGroup, ConnectionManager connectionManager) {
        return new BlockView(mWorkspaceView.getContext(),
                getBlockStyle(), block, this, parentGroup, connectionManager);
    }

    /**
     * Creates a {@link BlockView} for the given block using the workspace's default style.
     *
     * @param context The context in which to generate the view.
     * @param block The block to generate a view for.
     * @param parentGroup The group to set as the parent for this block's view.
     * @param connectionManager The {@link ConnectionManager} to update when moving connections.
     *
     * @return A view for the block.
     */
    public BlockView obtainBlockView(Context context, Block block, BlockGroup parentGroup,
                                     ConnectionManager connectionManager) {
        return new BlockView(
                context, getBlockStyle(), block, this, parentGroup, connectionManager);
    }

    /**
     * @return The style resource id to use for drawing blocks.
     */
    public int getBlockStyle() {
        return mBlockStyle;
    }

    /**
     * @return The style resource id to use for drawing field labels.
     */
    public int getFieldLabelStyle() {
        return mFieldLabelStyle;
    }

    /**
     * Get workspace coordinates of a given {@link View}.
     * <p/>
     * This function always returns the coordinate of the corner of the view that corresponds to the
     * block coordinate in workspace coordinates. In left-to-right (LtR) mode, this is the
     * <em>top-left</em> corner of the view, in right-to-left (RtL) mode, it is the
     * <em>top-right</em> corner of the view.
     *
     * @param view The view to find the position of.
     * @param workspacePosition The Point to store the results in.
     */
    public void getWorkspaceCoordinates(View view, WorkspacePoint workspacePosition) {
        getVirtualViewCoordinates(view, mTempViewPoint);
        if (mRtL) {
            // In right-to-left mode, the Block's position is that of its top-RIGHT corner, but
            // Android still refers to the BlockView's layout coordinate by its top-LEFT corner.
            // Adding the view's width to the lhs view coordinate gives us the rhs coordinate.
            mTempViewPoint.x += view.getMeasuredWidth();
        }
        virtualViewToWorkspaceCoordinates(mTempViewPoint, workspacePosition);
    }

    /**
     * Get virtual view coordinates of a given {@link View}.
     * <p/>
     * This function always returns the coordinate of the top-left corner of the given view,
     * regardless of left-to-right (LtR) vs. right-to-left (RtL) mode. Note that in RtL mode, this
     * is not the corner that corresponds to the block's workspace coordinates. Use
     * {@link #getWorkspaceCoordinates(View, WorkspacePoint)} to obtain  the workspace coordinates
     * of a block from its view, adjusted for RtL mode if necessary.
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
    public BlockGroup getRootBlockGroup(Block block) {
        // Go up and left as far as possible.
        while (true) {
            if (block.getOutputConnection() != null &&
                    block.getOutputConnection().getTargetBlock() != null) {
                block = block.getOutputConnection().getTargetBlock();
            } else if (block.getPreviousBlock() != null) {
                block = block.getPreviousBlock();
            } else {
                break;
            }
        }

        BlockView bv = block.getView();
        return (BlockGroup) bv.getParent();
    }

    /**
     * Find the closest {@link BlockGroup} in the hierarchy that this {@link Block} descends from.
     *
     * @param block The block to start searching from.
     *
     * @return The closest {@link BlockGroup} found.
     */
    public BlockGroup getNearestParentBlockGroup(Block block) {
        ViewParent viewParent = block.getView().getParent();
        while (viewParent != null) {
            if (viewParent instanceof BlockGroup)
                return (BlockGroup) viewParent;
        }
        throw new IllegalStateException("No BlockGroup found among view's parents.");
    }

    public BlockTouchHandler getBlockTouchHandler() {
        return mBlockTouchHandler;
    }

    public void setBlockTouchHandler(BlockTouchHandler bth) {
        mBlockTouchHandler = bth;
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
    public void screenToVirtualViewCoordinates(Point screenPositionIn, ViewPoint viewPositionOut) {
        mWorkspaceView.getLocationOnScreen(mTempIntArray2);
        viewPositionOut.x =
                (int) ((screenPositionIn.x - mTempIntArray2[0]) / mWorkspaceView.getScaleX());
        viewPositionOut.y =
                (int) ((screenPositionIn.y - mTempIntArray2[1]) / mWorkspaceView.getScaleY());
    }

    /**
     * Convenience method for direct mapping of screen to workspace coordinates.
     * <p/>
     * This method applies {@link #screenToVirtualViewCoordinates(Point, ViewPoint)} followed by
     * {@link #virtualViewToWorkspaceCoordinates(ViewPoint, WorkspacePoint)} using an existing
     * temporary {@link ViewPoint} instance as intermediate.
     *
     * @param screenPositionIn Input coordinates of a location in absolute coordinates on the
     * screen.
     * @param workspacePositionOut Output coordinates of the same location in workspace coordinates.
     */
    public void screenToWorkspaceCoordinates(Point screenPositionIn,
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
        int workspaceX = workspacePosition.x;
        if (mRtL) {
            workspaceX *= -1;
        }
        viewPosition.x = workspaceToVirtualViewUnits(workspaceX) - mVirtualWorkspaceViewOffset.x;
        viewPosition.y = workspaceToVirtualViewUnits(workspacePosition.y) -
                mVirtualWorkspaceViewOffset.y;
    }

    /**
     * Loads the style configurations. The config and styles are loaded from one of three sources
     * with the following priority.
     * <ol>
     * <li>The specified style id.</li>
     * <li>The attribute's style.</li>
     * <li>The context's theme.</li>
     * </ol>
     */
    private void initConfig(Context context, AttributeSet attrs, int style) {
        TypedArray styles;

        if (style != 0) {
            styles = context.obtainStyledAttributes(style, R.styleable.BlocklyWorkspaceTheme);
        } else if (attrs != null) {
            int styleId = attrs.getStyleAttribute();
            styles = context.obtainStyledAttributes(styleId, R.styleable.BlocklyWorkspaceTheme);
        } else {
            styles = context.obtainStyledAttributes(R.styleable.BlocklyWorkspaceTheme);
        }
        try {
            mBlockStyle = styles.getResourceId(R.styleable.BlocklyWorkspaceTheme_blockViewStyle, 0);
            mFieldLabelStyle = styles.getResourceId(
                    R.styleable.BlocklyWorkspaceTheme_fieldLabelStyle, 0);
            if (DEBUG) {
                Log.d(TAG, "BlockStyle=" + mBlockStyle + ", FieldLabelStyle=" + mFieldLabelStyle);
            }
        } finally {
            styles.recycle();
        }
    }

    /**
     * Updates the current RtL state for the app.
     *
     * @param context The context to get the RtL setting from.
     */
    private void updateRtL(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            mRtL = context.getResources().getConfiguration().getLayoutDirection()
                    == View.LAYOUT_DIRECTION_RTL;
        } else {
            // TODO: Handle pre 17 versions.
            mRtL = false;
        }
    }

    /**
     * Converts a point in virtual view coordinates to workspace coordinates, storing the result in
     * the second parameter. The view position should be in the {@link WorkspaceView} coordinates in
     * pixels.
     *
     * @param viewPosition The position to convert to workspace coordinates.
     * @param workspacePosition The Point to store the results in.
     */
    void virtualViewToWorkspaceCoordinates(ViewPoint viewPosition,
                                           WorkspacePoint workspacePosition) {
        int workspaceX =
                virtualViewToWorkspaceUnits(viewPosition.x + mVirtualWorkspaceViewOffset.x);
        if (mRtL) {
            workspaceX *= -1;
        }
        workspacePosition.x = workspaceX;
        workspacePosition.y =
                virtualViewToWorkspaceUnits(viewPosition.y + mVirtualWorkspaceViewOffset.y);
    }

    public static abstract class BlockTouchHandler {
        /**
         * Called by the BlockView when the visible area of the block has been touched.
         *
         * @param blockView The touched {@link BlockView}.
         * @param motionEvent The event the blockView is responding to.
         *
         * @return whether the {@link WorkspaceView} has handled the touch event.
         */
        abstract public boolean onTouchBlock(BlockView blockView, MotionEvent motionEvent);
    }
}
