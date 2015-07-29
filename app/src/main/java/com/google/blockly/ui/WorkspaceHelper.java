package com.google.blockly.ui;

import android.content.Context;
import android.graphics.Point;

/**
 * Provides helper methods for converting coordinates between the workspace and the views.
 */
public class WorkspaceHelper {
    private float mScale = 1;
    private float mDensity;
    private Point mOffset;

    /**
     * Create a helper for doing workspace to view conversions.
     *
     * @param context The current context to get display metrics from.
     * @param leftOffset The left offset into the workspace in workspace units.
     * @param topOffset The top offset into the workspace in workspace units.
     */
    public WorkspaceHelper(Context context, int leftOffset, int topOffset) {
        mDensity = context.getResources().getDisplayMetrics().density;
        mOffset = new Point(leftOffset, topOffset);
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
        mScale = scale;
    }

    /**
     * Set the {@link WorkspaceView}'s offset into the workspace, in workspace units. This value
     * should only change due to translation, not scaling, and therefore is not a pixel value.
     *
     * @param leftOffset The left offset in workspace units.
     * @param topOffset The top offset in workspace units.
     */
    public void setOffset(int leftOffset, int topOffset) {
        mOffset.x = leftOffset;
        mOffset.y = topOffset;
    }

    /**
     * @return The current scale of the workspace view.
     */
    public float getScale() {
        return mScale;
    }

    /**
     * Get the current view's offset in workspace coordinates. If you want the pixel offset of the
     * view you should use {@link #getViewOffset()} instead.
     *
     * @return The top left corner the viewport in workspace coordinates.
     */
    public Point getOffset() {
        return mOffset;
    }

    /**
     * Get the current offset of the view in pixels. If you want the offset in workspace coordinates
     * {@link #getOffset()} should be used instead.
     *
     * @return The top left corner of the viewport in pixels.
     */
    public Point getViewOffset() {
        return new Point(workspaceToViewUnits(mOffset.x), workspaceToViewUnits(mOffset.y));
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
     * Scales a value in view pixels to workspace units. This does account for offsets into the
     * view's space, it only uses scaling and screen density to calculate the result.
     *
     * @param viewValue The value in pixels in the view.
     * @return The value in workspace units.
     */
    public int viewToWorkspaceUnits(int viewValue) {
        return (int) (viewValue / (mScale * mDensity));
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
    private void workspaceToViewCoordinates(Point workspacePosition, Point viewPosition) {
        viewPosition.x = workspaceToViewUnits(workspacePosition.x - mOffset.x);
        viewPosition.y = workspaceToViewUnits(workspacePosition.y - mOffset.y);
    }

    /**
     * Converts a point in view coordinates to workspace coordinates, storing the result in the
     * second parameter. The view position should be in the {@link WorkspaceView WorkspaceView's}
     * coordinates in pixels.
     *
     * @param viewPosition The position to convert to workspace coordinates.
     * @param workspacePosition The Point to store the results in.
     */
    private void viewToWorkspaceCoordinates(Point viewPosition, Point workspacePosition) {
        workspacePosition.x = viewToWorkspaceUnits(viewPosition.x) + mOffset.x;
        workspacePosition.y = viewToWorkspaceUnits(viewPosition.y) + mOffset.y;
    }
}
