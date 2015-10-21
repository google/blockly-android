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
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.google.blockly.model.Block;
import com.google.blockly.model.Connection;
import com.google.blockly.model.Input;
import com.google.blockly.model.WorkspacePoint;

/**
 * This wraps a set of sequential {@link BlockView} instances.
 */
public class BlockGroup extends ViewGroup {
    private static final String TAG = "BlockGroup";

    private final WorkspaceHelper mWorkspaceHelper;

    private int mNextBlockVerticalOffset;

    public BlockGroup(Context context, WorkspaceHelper helper) {
        super(context);
        mWorkspaceHelper = helper;
    }

    @Override
    public void onMeasure(int widthSpec, int heightSpec) {
        mNextBlockVerticalOffset = 0;

        int childCount = getChildCount();
        int width = 0;
        int height = 0;

        // Layout margin for Output connector.
        int margin = 0;

        for (int i = 0; i < childCount; i++) {
            BlockView child = (BlockView) getChildAt(i);
            child.measure(widthSpec, heightSpec);
            width = Math.max(margin + child.getMeasuredWidth(), width);

            // If the first child has a layout margin for an Output connector, then save the margin
            // to add it to all children that follow (but do not add to width of this child itself).
            if (i == 0) {
                margin = child.getLayoutMarginLeft();
            }

            // Only for last child, add the entire measured height. For all other children, add
            // offset to next block. This takes into account that blocks are rendered with
            // overlaps due to extruding "Next" connectors.
            if (i == childCount - 1) {
                height += child.getMeasuredHeight();
            } else {
                height += child.getNextBlockVerticalOffset();
            }
            mNextBlockVerticalOffset += child.getNextBlockVerticalOffset();
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

    /**
     * @return The workspace position of the top block in this group, or {@code null} if this group
     * is empty.
     */
    public WorkspacePoint getTopBlockPosition() {
        if (getChildCount() > 0) {
            return ((BlockView) getChildAt(0)).getBlock().getPosition();
        }
        return null;
    }

    /**
     * Move block views to a new block group, starting with the given block and continuing through
     * its chain of next blocks.
     *
     * @param firstBlock The first {@link Block} to move to the new group.
     * @return A new {@link BlockGroup} containing blocks from the old group.
     */
    public BlockGroup extractBlocksAsNewGroup(Block firstBlock) {
        BlockGroup newGroup = new BlockGroup(this.getContext(), mWorkspaceHelper);
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
            from.removeView(cur.getView());
            this.addView(cur.getView());
            cur = cur.getNextBlock();
        }
    }

    /**
     * @return The {@link Block} for the last child in this group or null if there are no children.
     */
    public Block lastChildBlock() {
        if (getChildCount() == 0) {
            return null;
        }
        BlockView lastChild = (BlockView) getChildAt(getChildCount() - 1);
        return lastChild.getBlock();
    }

    /**
     * Walks the chain of blocks in this block group, at each stage checking if there are multiple
     * value inputs.  If there is only one value input at each block, follows that input to the
     * next block.
     *
     * @return the {@link Connection} on the only input on the last blockin the chain.
     */
    public Connection getLastInputConnection() {
        if (getChildCount() == 0) {
            return null;
        }
        Block block = ((BlockView) getChildAt(0)).getBlock();
        // Loop until we run out of inputs (in which case there's nothing to connect to) or we run
        // out of blocks (in which case the last
        while (true) {
            Input onlyValueInput = block.getOnlyValueInput();
            if (onlyValueInput == null) {
                return null;
            }
            Connection conn = onlyValueInput.getConnection();
            if (conn == null) {
                return null;
            }
            if (!conn.isConnected()) {
                return conn;
            }
            block = conn.getTargetBlock();
        }
    }

    /**
     * Force every {@link BlockView} in this group to recalculate the locations of its
     * connections; used to return the views and models to a consistent state after a drag.
     */
    public void updateAllConnectorLocations() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            BlockView child = (BlockView) getChildAt(i);
            child.updateConnectorLocations();
            child.invalidate();
        }
    }

    /**
     * Move the block group by the specified amount.
     *
     * @param dx How much to move in the x direction, in view coordinates.
     * @param dy How much to move in the y direction, in view coordinates.
     */
    public void moveBy(int dx, int dy) {
        setLeft(getLeft() + dx);
        setTop(getTop() + dy);
    }

    /**
     * @return The vertical offset from the top of this view to the position of the next block
     * <em>below</em> this group.
     */
    int getNextBlockVerticalOffset() {
        return mNextBlockVerticalOffset;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int childCount = getChildCount();
        boolean rtl = mWorkspaceHelper.useRtL();
        int x = rtl ? getMeasuredWidth() : 0;
        int y = 0;

        // Layout margin for Output connector.
        int margin = 0;

        for (int i = 0; i < childCount; i++) {
            BlockView child = (BlockView) getChildAt(i);

            int w = child.getMeasuredWidth();
            int cl = rtl ? x - margin - w : x + margin;
            child.layout(cl, y, cl + w, y + child.getMeasuredHeight());
            y += child.getNextBlockVerticalOffset();

            // If the first child has a layout margin for an Output connector, then save margin for
            // all children that follow.
            if (i == 0) {
                margin = child.getLayoutMarginLeft();
            }
        }
        // After we finish laying out we need to update the locations of the connectors
        updateAllConnectorLocations();
    }
}
