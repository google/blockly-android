/*
* Copyright 2016 Google Inc. All Rights Reserved.
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
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.support.annotation.Size;
import android.support.v4.view.MotionEventCompat;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.google.blockly.android.control.ConnectionManager;
import com.google.blockly.model.Block;
import com.google.blockly.model.Connection;
import com.google.blockly.model.Input;
import com.google.blockly.model.WorkspacePoint;

import java.util.ArrayList;
import java.util.List;

/**
 * An optional base class for {@link BlockView}. {@link AbstractBlockView} assumes
 * {@link InputView}s are direct children, and handles most UI events and view hierarchy
 * coordination. The measurement, placement and drawing are left to the subclass to implement.
 */
@SuppressLint("ViewConstructor")
public abstract class AbstractBlockView<InputView extends com.google.blockly.android.ui.InputView>
        extends NonPropagatingViewGroup implements BlockView {
    protected final WorkspaceHelper mHelper;
    protected final BlockViewFactory mFactory;
    protected final Block mBlock;
    protected final ConnectionManager mConnectionManager;
    protected BlockTouchHandler mTouchHandler;

    // Child views for the block inputs and their children.
    protected final ArrayList<InputView> mInputViews;

    protected WorkspaceView mWorkspaceView;

    // Reference points for connectors relative to this view (needed for selective highlighting).
    protected final ViewPoint mOutputConnectorOffset = new ViewPoint();
    protected final ViewPoint mPreviousConnectorOffset = new ViewPoint();
    protected final ViewPoint mNextConnectorOffset = new ViewPoint();

    // Flag is set to true if this block has at least one "Value" input.
    protected boolean mHasValueInput = false;
    protected int mInputCount;
    protected final ArrayList<ViewPoint> mInputConnectorOffsets;
    // Layout coordinates for inputs in this Block, so they don't have to be computed repeatedly.
    protected final ArrayList<ViewPoint> mInputLayoutOrigins;

    // Current measured size of this block view.
    protected final ViewPoint mBlockViewSize = new ViewPoint();
    // Position of the connection currently being updated, for temporary use during
    // layoutPatchesAndConnectors.
    protected final ViewPoint mTempConnectionPosition = new ViewPoint();
    protected final WorkspacePoint mTempWorkspacePoint = new WorkspacePoint();

    // Currently highlighted connection.
    @Nullable protected Connection mHighlightedConnection = null;

    /**
     * Creates a BlockView for the given {@link Block}.
     *
     * @param context The context for creating this view.
     * @param helper The {@link WorkspaceHelper} for this app.
     * @param factory The helper for loading workspace configs and doing calculations.
     * @param block The {@link Block} represented by this view.
     * @param inputViews The {@link InputView} children in the new {@link BlockView}.
     * @param connectionManager The {@link ConnectionManager} to update when moving connections.
     * @param touchHandler The optional handler for forwarding touch events on this block to the
     *                     {@link Dragger}.
     */
    // TODO(#137): Pass in ViewPool instead of BlockViewFactory
    protected AbstractBlockView(Context context, WorkspaceHelper helper, BlockViewFactory factory,
                                Block block, List<InputView> inputViews,
                                ConnectionManager connectionManager,
                                @Nullable BlockTouchHandler touchHandler) {
        super(context);

        mHelper = helper;
        mFactory = factory;
        mBlock = block;
        mConnectionManager = connectionManager;
        mTouchHandler = touchHandler;

        setClickable(true);
        setFocusable(true);
        setWillNotDraw(false);

        mInputCount = inputViews.size();
        mInputViews = new ArrayList<>(inputViews);
        mInputConnectorOffsets = new ArrayList<>(mInputCount);
        mInputLayoutOrigins = new ArrayList<>(mInputCount);
        for (int i = 0; i < mInputCount; ++i) {
            mInputConnectorOffsets.add(new ViewPoint());
            mInputLayoutOrigins.add(new ViewPoint());

            Input in = mInputViews.get(i).getInput();
            if (in.getType() == Input.TYPE_VALUE) {
                mHasValueInput = true;
            }
        }
        addInputViewsToViewHierarchy();
    }

    /**
     * Adds the {@link InputView}s in {@link #mInputViews} to the view hierarchy. The default
     * implementation adds the views directly to this view, in order.
     */
    protected void addInputViewsToViewHierarchy() {
        for (int i = 0; i < mInputViews.size(); ++i) {
            addView((View) mInputViews.get(i));
        }
    }

    /**
     * Sets the touch handler used on this block and all contained blocks.
     *
     * @param touchHandler The {@link BlockTouchHandler} to use.
     */
    public void setTouchHandler(BlockTouchHandler touchHandler) {
        mTouchHandler = touchHandler;
        for (int i = 0; i < mInputViews.size(); i++) {
            BlockGroup bg = mInputViews.get(i).getConnectedBlockGroup();
            if (bg != null) {
                bg.setTouchHandler(touchHandler);
            }
        }
    }

    /**
     * Test whether event hits visible parts of this block and notify {@link WorkspaceView} if it
     * does.
     *
     * @param event The {@link MotionEvent} to handle.
     *
     * @return False if the touch was on the view but not on a visible part of the block; otherwise
     * returns whether the {@link WorkspaceView} says that the event is being handled properly.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return hitTest(event) && mTouchHandler.onTouchBlock(this, event);
    }

    /**
     * Processes intercepted touch events by calling {@link BlockTouchHandler#onInterceptTouchEvent}
     * when {@link #hitTest} passes.
     *
     * @param event The touch event in progress.
     * @return The results of {@link BlockTouchHandler#onInterceptTouchEvent}, if called. Otherwise,
     *         false.
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return mTouchHandler != null && hitTest(event)
                && mTouchHandler.onInterceptTouchEvent(this, event);
    }

    /**
     * Select a connection for highlighted drawing.
     *
     * @param connection The connection whose port to highlight. This must be a connection
     * associated with the {@link Block} represented by this {@link AbstractBlockView}
     * instance.  Disables all connection highlights if connection is null.
     */
    public void setHighlightedConnection(@Nullable Connection connection) {
        mHighlightedConnection = connection;
        invalidate();
    }

    /**
     * @return The block for this view.
     */
    public Block getBlock() {
        return mBlock;
    }

    /**
     * @return The nearest {@link WorkspaceView} this view is attached to.
     */
    @Override
    public WorkspaceView getWorkspaceView() {
        return mWorkspaceView;
    }

    /**
     * @return The closest view tree ancestor that is a BlockGroup.
     */
    @Override
    @Nullable
    public BlockGroup getParentBlockGroup() {
        ViewParent viewParent = getParent();
        if (viewParent instanceof BlockGroup) {
            return (BlockGroup) viewParent;
        } else {
            return null;  // Not connected to a BlockGroup
        }
    }

    /**
     * Updates the locations of the connections based on their offsets within the {@link BlockView},
     * based upon the view's position within the  {@link WorkspaceView}.  Often used when the block
     * has moved but not changed shape, such as after a drag.
     */
    @Override
    public void updateConnectorLocations() {
        // Ensure we have the right block location before we update the connections.
        updateBlockPosition();

        // Connection location is only important when we are attached to a WorkspaceView that can
        // interact with other connections.  BlockViews outside the WorkspaceView, such as those in
        // BlockListViews, should be ignored.
        if (mWorkspaceView == null) {
            return;
        }

        final WorkspacePoint blockWorkspacePosition = mBlock.getPosition();

        final Connection previousConnection = mBlock.getPreviousConnection();
        if (previousConnection != null) {
            mHelper.virtualViewToWorkspaceDelta(mPreviousConnectorOffset, mTempWorkspacePoint);
            mConnectionManager.moveConnectionTo(
                    previousConnection, blockWorkspacePosition, mTempWorkspacePoint);
        }

        final Connection nextConnection = mBlock.getNextConnection();
        if (nextConnection != null) {
            mHelper.virtualViewToWorkspaceDelta(mNextConnectorOffset, mTempWorkspacePoint);
            mConnectionManager.moveConnectionTo(
                    nextConnection, blockWorkspacePosition, mTempWorkspacePoint);
        }

        final Connection outputConnection = mBlock.getOutputConnection();
        if (outputConnection != null) {
            mHelper.virtualViewToWorkspaceDelta(mOutputConnectorOffset, mTempWorkspacePoint);
            mConnectionManager.moveConnectionTo(
                    outputConnection, blockWorkspacePosition, mTempWorkspacePoint);
        }

        for (int i = 0; i < mInputViews.size(); i++) {
            final InputView inputView = mInputViews.get(i);
            final Connection connection = inputView.getInput().getConnection();
            if (connection != null) {
                mHelper.virtualViewToWorkspaceDelta(
                        mInputConnectorOffsets.get(i), mTempWorkspacePoint);
                mConnectionManager.moveConnectionTo(
                        connection, blockWorkspacePosition, mTempWorkspacePoint);
                if (connection.isConnected()) {
                    inputView.getConnectedBlockGroup().updateAllConnectorLocations();
                }
            }
        }
    }

    /**
     * Recursively disconnects the view from the model, and removes all views.
     */
    // TODO(#146): Move tree traversal to BlockViewFactory.unregisterView(..)
    public void unlinkModel() {
        mFactory.unregisterView(this); // TODO(#137): factory -> ViewPool

        int max = mInputViews.size();
        for (int i = 0; i < max; ++i) {
            InputView inputView = mInputViews.get(i);
            inputView.unlinkModel();
        }
        mTouchHandler = null;  // Recursive via the calls InputView calls.
        removeAllViews();
        // TODO(#45): Remove model from view. Set mBlock to null, and handle all null cases.
    }

    /**
     * @return The number of {@link InputView} instances inside this view.
     */
    public int getInputViewCount() {
        return mInputViews.size();
    }

    /**
     * @return The {@link InputView} for the {@link Input} at the given index.
     */
    @Override
    public InputView getInputView(int index) {
        return mInputViews.get(index);
    }

    @Override
    public void getTouchLocationOnScreen(MotionEvent event, @Size(2) int[] locationOut) {
        int pointerId =
                MotionEventCompat.getPointerId(event, MotionEventCompat.getActionIndex(event));
        int pointerIdx = MotionEventCompat.findPointerIndex(event, pointerId);
        float offsetX =  MotionEventCompat.getX(event, pointerIdx);
        float offsetY = MotionEventCompat.getY(event, pointerIdx);

        // Get local screen coordinates.
        getLocationOnScreen(locationOut);

        // add the scaled offset.
        if (mWorkspaceView != null) {
            float scale = mWorkspaceView.getScaleX();
            offsetX = offsetX * scale;
            offsetY = offsetY * scale;
        }
        locationOut[0] += (int) offsetX;
        locationOut[1] += (int) offsetY;
    }

    /**
     * Stores a reference to the {@link WorkspaceView} containing this BlockView.
     */
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        mWorkspaceView = null;
        ViewParent parent = getParent();
        while (parent != null && parent instanceof ViewGroup) {
            if (parent instanceof WorkspaceView) {
                mWorkspaceView = (WorkspaceView) parent;
                return;
            }
            parent = ((ViewGroup) parent).getParent();
        }
    }

    /**
     * Clears the reference to any {@link WorkspaceView}.
     */
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mWorkspaceView = null;
    }

    /**
     * Check if border highlight is rendered.
     */
    protected boolean isEntireBlockHighlighted() {
        return isPressed() || isFocused() || isSelected();
    }

    /**
     * Test whether a {@link MotionEvent} event is (approximately) hitting a visible part of this
     * view.
     * <p/>
     * This is used to determine whether the event should be handled by this view, e.g., to activate
     * dragging or to open a context menu. Since the actual block interactions are implemented at
     * the {@link WorkspaceView} level, there is no need to store the event data in this class.
     *
     * @param event The {@link MotionEvent} to check.
     *
     * @return True if the coordinate of the motion event is on the visible, non-transparent part of
     * this view; false otherwise.
     */
    protected abstract boolean hitTest(MotionEvent event);

    /**
     * This is a developer testing function subclasses can call to draw dots at the model's location
     * of all connections on this block.  Never called by default.
     *
     * @param c The canvas to draw on.
     */
    protected void drawConnectorCenters(Canvas c) {
        List<Connection> connections = mBlock.getAllConnections();
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < connections.size(); i++) {
            Connection conn = connections.get(i);
            if (conn.inDragMode()) {
                if (conn.isConnected()) {
                    paint.setColor(Color.RED);
                } else {
                    paint.setColor(Color.MAGENTA);
                }
            } else {
                if (conn.isConnected()) {
                    paint.setColor(Color.GREEN);
                } else {
                    paint.setColor(Color.CYAN);
                }
            }

            // Compute connector position relative to this view from its offset to block origin in
            // Workspace coordinates.
            mTempWorkspacePoint.set(
                    conn.getPosition().x - mBlock.getPosition().x,
                    conn.getPosition().y - mBlock.getPosition().y);
            mHelper.workspaceToVirtualViewDelta(mTempWorkspacePoint, mTempConnectionPosition);
            if (mHelper.useRtl()) {
                // In RTL mode, add block view size to x coordinate. This is counter-intuitive, but
                // equivalent to "x = size - (-x)", with the inner negation "-x" undoing the
                // side-effect of workspaceToVirtualViewDelta reversing the x coordinate. This is,
                // the addition mirrors the re-negated in-block x coordinate w.r.t. the right-hand
                // side of the block view, which is the origin of the block in RTL mode.
                mTempConnectionPosition.x += mBlockViewSize.x;
            }
            c.drawCircle(mTempConnectionPosition.x, mTempConnectionPosition.y, 10, paint);
        }
    }

    /**
     * Update the position of the block in workspace coordinates based on the view's location.
     */
    private void updateBlockPosition() {
        // Only update the block position if it isn't a top level block.
        if (mBlock.getPreviousBlock() != null
                || (mBlock.getOutputConnection() != null
                && mBlock.getOutputConnection().getTargetBlock() != null)) {
            mHelper.getWorkspaceCoordinates(this, mTempWorkspacePoint);
            mBlock.setPosition(mTempWorkspacePoint.x, mTempWorkspacePoint.y);
        }
    }
}
