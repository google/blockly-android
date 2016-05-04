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
import android.view.View;

import com.google.blockly.model.Block;
import com.google.blockly.model.WorkspacePoint;

/**
 * This wraps a set of sequential {@link BlockView} instances.
 */
// TODO(#133): Generalize to support horizontal block groups.
public class BlockGroup extends NonPropagatingViewGroup {
    private static final String TAG = "BlockGroup";

    private final WorkspaceHelper mWorkspaceHelper;

    private int mNextBlockVerticalOffset;

    /**
     * Creates a BlockGroup to wrap one or more BlockViews. App developers should not call this
     * constructor directly.  Instead, use {@link BlockViewFactory#buildBlockGroupTree}.
     *
     * @param context The context for creating this view.
     * @param helper The helper for loading workspace configs and doing calculations.
     */
    public BlockGroup(Context context, WorkspaceHelper helper) {
        super(context);
        mWorkspaceHelper = helper;
    }

    /**
     * Recursively sets the {@link BlockTouchHandler} for this view tree.
     *
     * @param touchHandler The new touch handler.
     */
    public void setTouchHandler(BlockTouchHandler touchHandler) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; ++i) {
            ((BlockView) getChildAt(i)).setTouchHandler(touchHandler);
        }
    }

    @Override
    public void onMeasure(int widthSpec, int heightSpec) {
        boolean rtl = mWorkspaceHelper.useRtl();
        mNextBlockVerticalOffset = 0;

        int childCount = getChildCount();
        int width = 0;
        int height = 0;

        // Layout margin for Output connector.
        int margin = 0;

        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            BlockView childBlockView = (BlockView) child;

            child.measure(widthSpec, heightSpec);
            width = Math.max(margin + child.getMeasuredWidth(), width);

            // If the first child has a layout margin for an Output connector, then save the margin
            // to add it to all children that follow (but do not add to width of this child itself).
            if (!rtl && i == 0) {
                margin = childBlockView.getOutputConnectorMargin();
            }

            // Only for last child, add the entire measured height. For all other children, add
            // offset to next block. This takes into account that blocks are rendered with
            // overlaps due to extruding "Next" connectors.
            if (i == childCount - 1) {
                height += child.getMeasuredHeight();
            } else {
                height += childBlockView.getNextBlockVerticalOffset();
            }
            mNextBlockVerticalOffset += childBlockView.getNextBlockVerticalOffset();
        }
        setMeasuredDimension(width, height);
    }

    @Override
    public void addView(View child, int index, LayoutParams params) {
        if (!(child instanceof BlockView)) {
            // There's no guarantees only BlockViews can be added, but this should catch most cases
            // and serve as a strong warning not to do this.
            throw new IllegalArgumentException("BlockGroups may only contain BlockViews");
        }
        super.addView(child, index, params);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int childCount = getChildCount();
        boolean rtl = mWorkspaceHelper.useRtl();
        int x = rtl ? getMeasuredWidth() : 0;
        int y = 0;

        // Layout margin for Output connector.
        int margin = 0;

        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            BlockView childBlockView = (BlockView) child;

            int w = child.getMeasuredWidth();
            int cl = rtl ? x - margin - w : x + margin;
            child.layout(cl, y, cl + w, y + child.getMeasuredHeight());
            y += childBlockView.getNextBlockVerticalOffset();

            // If the first child has a layout margin for an Output connector, then save margin for
            // all children that follow.
            if (!rtl && i == 0) {
                margin = childBlockView.getOutputConnectorMargin();
            }
        }
        // After we finish laying out we need to update the locations of the connectors
        updateAllConnectorLocations();
    }

    /**
     * @return The first block in this group, or {@code null} if this group is empty.
     */
    public Block getFirstBlock() {
        BlockView first = getFirstBlockView();
        return first == null ? null : first.getBlock();
    }

    /**
     * @return The view for the first block in this group, or {@code null} if this group is empty.
     */
    public BlockView getFirstBlockView() {
        if (getChildCount() > 0) {
            return (BlockView) getChildAt(0);
        }
        return null;
    }

    /**
     * @return The workspace position of the top block in this group, or {@code null} if this group
     * is empty.
     */
    public WorkspacePoint getFirstBlockPosition() {
        Block topBlock = getFirstBlock();
        return topBlock == null ? null : topBlock.getPosition();
    }

    /**
     * Move block views to a new block group, starting with the given block and continuing through
     * its chain of next blocks.
     *
     * @param firstBlock The first {@link Block} to move to the new group.
     *
     * @return A new {@link BlockGroup} containing blocks from the old group.
     */
    public BlockGroup extractBlocksAsNewGroup(Block firstBlock) {
        BlockGroup newGroup = mWorkspaceHelper.getBlockViewFactory().buildBlockGroup();
        newGroup.moveBlocksFrom(this, firstBlock);
        return newGroup;
    }

    /**
     * Move block views into this group from the given group, starting with the given block and
     * continuing through its chain of next blocks.
     *
     * @param from The {@link BlockGroup} to move block views from.
     * @param firstBlock The first {@link Block} to move between groups.
     */
    public void moveBlocksFrom(BlockGroup from, Block firstBlock) {
        Block cur = firstBlock;
        while (cur != null) {
            View blockView = (View) mWorkspaceHelper.getView(cur);
            from.removeView(blockView);
            this.addView(blockView);
            cur = cur.getNextBlock();
        }
    }

    /**
     * Force every {@link BlockView} in this group to recalculate the locations of its
     * connections; used to return the views and models to a consistent state after a drag.
     */
    public void updateAllConnectorLocations() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            BlockView childBlockView = (BlockView) child;
            childBlockView.updateConnectorLocations();
            child.invalidate();
        }
    }

    /**
     * Recursively disconnects the model from view and unregisters and removes all views.
     */
    public void unlinkModel() {
        int childCount = getChildCount();
        for (int i = childCount - 1; i >= 0; --i) {
            BlockView bv = (BlockView) getChildAt(i);
            bv.unlinkModel();
        }
        removeAllViews();
    }

    /**
     * @return The vertical offset from the top of this view to the position of the next block
     * <em>below</em> this group.
     */
    int getNextBlockVerticalOffset() {
        return mNextBlockVerticalOffset;
    }
}
