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

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewConfiguration;

import com.google.blockly.android.control.BlocklyController;

/**
 * Handles updating the viewport into the workspace and is the parent view for all blocks. This view
 * is responsible for handling drags. A drag on the workspace will move the viewport and a drag on a
 * block or stack of blocks will drag those within the workspace.
 */
public class WorkspaceView extends NonPropagatingViewGroup {
    private static final String TAG = "WorkspaceView";

    public static final String BLOCK_GROUP_CLIP_DATA_LABEL = "BlockGroupClipData";

    private final ViewPoint mTemp = new ViewPoint();
    // Distance threshold for detecting drag gestures.
    private final float mTouchSlop;
    // Viewport bounds. These define the bounding box of all blocks, in view coordinates, and
    // are used to determine ranges and offsets for scrolling.
    private final Rect mBlocksBoundingBox = new Rect();

    private BlocklyController mController = null;
    private WorkspaceHelper mHelper = null;
    private Dragger mDragger;

    public WorkspaceView(Context context) {
        this(context, null);
    }

    public WorkspaceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WorkspaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        float touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mTouchSlop = touchSlop;
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
            mHelper.workspaceToVirtualViewDelta(blockGroup.getFirstBlockPosition(), mTemp);
            if (mHelper.useRtl()) {
                mTemp.x -= blockGroup.getMeasuredWidth();
            }

            mBlocksBoundingBox.union(mTemp.x, mTemp.y,
                    mTemp.x + blockGroup.getMeasuredWidth(),
                    mTemp.y + blockGroup.getMeasuredHeight());
        }

        setMeasuredDimension(width, height);
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
                mHelper.workspaceToVirtualViewCoordinates(bg.getFirstBlockPosition(), mTemp);
                if (mHelper.useRtl()) {
                    mTemp.x -= bg.getMeasuredWidth();
                }

                child.layout(mTemp.x, mTemp.y,
                        mTemp.x + bg.getMeasuredWidth(), mTemp.y + bg.getMeasuredHeight());
            }
        }
    }

    /**
     * Sets the workspace this view should display.
     *
     * @param controller The controller for this instance.
     */
    public void setController(BlocklyController controller) {
        mController = controller;

        if (mController != null) {
            mHelper = controller.getWorkspaceHelper();
        } else {
            mHelper = null;
        }
    }

    public WorkspaceHelper getWorkspaceHelper() {
        return mHelper;
    }

    /**
     * Updates the {@link Dragger} for this workspace view and passes through the view for the trash
     * can.
     *
     * @param dragger The {@link Dragger} to use in this workspace.
     */
    public void setDragger(Dragger dragger) {
        mDragger = dragger;
        mDragger.setTouchSlop(mTouchSlop);
        setOnDragListener(mDragger.getDragEventListener());
    }

    /**
     * @return The bounding box in view coordinates of the workspace region occupied by blocks.
     */
    public Rect getBlocksBoundingBox() {
        return mBlocksBoundingBox;
    }
}
