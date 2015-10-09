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
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewParent;

import com.google.blockly.R;
import com.google.blockly.control.ConnectionManager;
import com.google.blockly.model.Block;
import com.google.blockly.model.WorkspacePoint;

/**
 * Provides helper methods for converting coordinates between the workspace and the views.
 */
public class WorkspaceHelper {
    private static final String TAG = "WorkspaceHelper";
    private static final boolean DEBUG = false;

    private static final float SCALE_MIN = 0.1f;
    private static final float SCALE_MAX = 3f;

    private final WorkspaceView mWorkspaceView;

    private final WorkspacePoint mWorkspaceOffset = new WorkspacePoint();
    private final ViewPoint mViewSize = new ViewPoint();
    private final ViewPoint mTempViewPoint = new ViewPoint();

    private float mMinScale;
    private float mMaxScale;
    private float mDefaultScale;
    private float mScale = 1;
    private float mDensity;
    private boolean mRtL;
    private int mBlockStyle;
    private int mFieldLabelStyle;

    /**
     * Create a helper for creating and doing calculations for views in the workspace using the
     * workspace's style.
     *
     * @param workspaceView The {@link WorkspaceView} for which this is a helper.
     * @param attrs The workspace attributes to load the style from.
     */
    public WorkspaceHelper(WorkspaceView workspaceView, AttributeSet attrs) {
        this(workspaceView, attrs, 0);
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
     * @param workspaceView The {@link WorkspaceView} for which this is a helper.
     * @param attrs The {@link WorkspaceView} attributes or null.
     * @param workspaceStyle The style to use for views.
     */
    public WorkspaceHelper(WorkspaceView workspaceView, AttributeSet attrs, int workspaceStyle) {
        mWorkspaceView = workspaceView;

        Context context = mWorkspaceView.getContext();
        Resources res = context.getResources();
        mDensity = res.getDisplayMetrics().density;
        if (mDensity == 0) {
            Log.e(TAG, "Density is not defined for this context. Defaulting to 1.");
            mDensity = 1f;
        }

        initConfig(context, attrs, workspaceStyle);
    }

    /** @return The {@link WorkspaceView} for which this is a helper. */
    public WorkspaceView getWorkspaceView() {
        return mWorkspaceView;
    }

    /**
     * @return The current scale of the workspace view.
     */
    public float getScale() {
        return mScale;
    }

    /**
     * Sets the scaling of the view. This scaling should be used for all drawing within the
     * {@link WorkspaceView} and children. A value of 1 means default size with no scaling. Values
     * larger than 1 will increase the size of drawn blocks and the grid, while a size smaller than
     * 1 will decrease the size and allow more blocks to fit on the screen.
     *
     * @param scale The scale of the view.
     */
    public void setScale(float scale) {
        mScale = Math.min(mMaxScale, Math.max(mMinScale, scale));
    }

    /**
     * Get the current view's offset in workspace coordinates. If you want the pixel offset of the
     * view you should use {@link #getViewOffset()} instead.
     *
     * @return The top left corner the viewport in workspace coordinates.
     */
    public WorkspacePoint getOffset() {
        return mWorkspaceOffset;
    }

    /**
     * Set the {@link WorkspaceView}'s offset into the workspace, in workspace units. This value
     * should only change due to translation, not scaling, and therefore is not a pixel value.
     *
     * @param workspaceOffset The view's offset into the workspace in workspace coordinates.
     */
    public void setOffset(WorkspacePoint workspaceOffset) {
        mWorkspaceOffset.x = workspaceOffset.x;
        mWorkspaceOffset.y = workspaceOffset.y;
    }

    /**
     * Get the current offset of the view in pixels. If you want the offset in workspace coordinates
     * {@link #getOffset()} should be used instead.
     *
     * @return The top left corner of the viewport in pixels.
     */
    public ViewPoint getViewOffset() {
        return new ViewPoint(
                workspaceToViewUnits(mWorkspaceOffset.x), workspaceToViewUnits(mWorkspaceOffset.y));
    }

    /**
     * @return The current size of the view in pixels.
     */
    public ViewPoint getViewSize() {
        return mViewSize;
    }

    /**
     * Set the size of the view window. This is required for RtL languages to be laid out correctly.
     * This should be called by the workspace view in onLayout.
     *
     * @param viewDimens The width and height of the workspace view in pixels.
     */
    public void setViewSize(ViewPoint viewDimens) {
        mViewSize.x = viewDimens.x;
        mViewSize.y = viewDimens.y;
    }

    /**
     * Scales a value in workspace coordinates to a view pixel value. This does not account for
     * offsets into the view's space, it only uses scaling and screen density to calculate the
     * result.
     *
     * @param workspaceValue The value in workspace units.
     * @return The value in view pixels.
     */
    public int workspaceToViewUnits(int workspaceValue) {
        return (int) (mScale * mDensity * workspaceValue);
    }

    /**
     * Scales a value in view to workspace units. This does not account for offsets into the
     * view's space, it only uses scaling and screen density to calculate the result.
     *
     * @param viewValue The value in view units.
     * @return The value in workspace units.
     */
    public int viewToWorkspaceUnits(int viewValue) {
        return (int) (viewValue / (mScale * mDensity));
    }

    /**
     * Convenience function that converts x and y components from view to workspace units by
     * applying {@link #viewToWorkspaceUnits(int)} to each component separately.
     */
    public void viewToWorkspaceUnits(ViewPoint viewPointIn, WorkspacePoint workspacePointOut) {
        workspacePointOut.x = viewToWorkspaceUnits(viewPointIn.x);
        workspacePointOut.y = viewToWorkspaceUnits(viewPointIn.y);
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
     * @return A view for the block.
     */
    public BlockView obtainBlockView(
            Block block, BlockGroup parentGroup, ConnectionManager connectionManager) {
        return new BlockView(mWorkspaceView.getContext(),
                getBlockStyle(), block, this, parentGroup, connectionManager);
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
     * Update workspace coordinates based on the new view coordinates of the {@link View}.
     *
     * @param view The view to find the position of.
     * @param workspacePosition The Point to store the results in.
     */
    public void getWorkspaceCoordinates(View view, WorkspacePoint workspacePosition) {
        getWorkspaceViewCoordinates(view, mTempViewPoint);
        workspacePosition.x = viewToWorkspaceUnits(mTempViewPoint.x);
        workspacePosition.y = viewToWorkspaceUnits(mTempViewPoint.y);
    }

    /**
     * Update view coordinates based on the new view coordinates of the {@link View}.
     *
     * @param view The view to find the position of.
     * @param viewPosition The Point to store the results in.
     */
    public void getWorkspaceViewCoordinates(View view, ViewPoint viewPosition) {
        int leftRelativeToWorkspace = view.getLeft();
        int topRelativeToWorkspace = view.getTop();

        // Move up the parent hierarchy and add parent-relative view coordinates.
        ViewParent viewParent = view.getParent();
        while (viewParent != null) {
            if (viewParent instanceof WorkspaceView) {
                break;
            }

            leftRelativeToWorkspace += ((View) viewParent).getLeft();
            topRelativeToWorkspace += ((View) viewParent).getTop();

            viewParent = viewParent.getParent();
        }

        if (viewParent == null) {
            throw new IllegalStateException("No WorkspaceView found among view's parents.");
        }

        viewPosition.x = leftRelativeToWorkspace;
        viewPosition.y = topRelativeToWorkspace;
    }

    /**
     * Find the highest {@link BlockGroup} in the hierarchy that this {@link Block} descends from.
     *
     * @param block The block to start searching from.
     * @return The highest {@link BlockGroup} found.
     */
    public BlockGroup getRootBlockGroup(Block block) {
        // Go up and left as far as possible.
        while (true) {
            if (block.getOutputConnection() != null && block.getOutputConnection().getTargetBlock() != null) {
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
            mMinScale = styles.getFloat(R.styleable.BlocklyWorkspaceTheme_minScale, SCALE_MIN);
            mMaxScale = styles.getFloat(R.styleable.BlocklyWorkspaceTheme_maxScale, SCALE_MAX);
            mDefaultScale = styles.getFloat(R.styleable.BlocklyWorkspaceTheme_defaultScale, 1.0f);
            mScale = mDefaultScale;
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
     * Converts a point in workspace coordinates to view coordinates, storing the result in the
     * second parameter. The resulting coordinates will be in the
     * {@link WorkspaceView WorkspaceView's} coordinates in pixels and include the current offset
     * into the workspace.
     *
     * @param workspacePosition The position to convert to view coordinates.
     * @param viewPosition The Point to store the results in.
     */
    void workspaceToViewCoordinates(WorkspacePoint workspacePosition, ViewPoint viewPosition) {
        viewPosition.x = workspaceToViewUnits(workspacePosition.x - mWorkspaceOffset.x);
        viewPosition.y = workspaceToViewUnits(workspacePosition.y - mWorkspaceOffset.y);

        if (useRtL()) {
            viewPosition.x = mViewSize.x - viewPosition.x;
        }
    }

    /**
     * Converts a point in view coordinates to workspace coordinates, storing the result in the
     * second parameter. The view position should be in the {@link WorkspaceView WorkspaceView's}
     * coordinates in pixels.
     *
     * @param viewPosition The position to convert to workspace coordinates.
     * @param workspacePosition The Point to store the results in.
     */
    void viewToWorkspaceCoordinates(ViewPoint viewPosition, WorkspacePoint workspacePosition) {
        int viewX = viewPosition.x;
        if (useRtL()) {
            viewX = mViewSize.x - viewX;
        }
        workspacePosition.x = viewToWorkspaceUnits(viewX) + mWorkspaceOffset.x;
        workspacePosition.y = viewToWorkspaceUnits(viewPosition.y) + mWorkspaceOffset.y;
    }
}
