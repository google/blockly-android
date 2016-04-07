/*
 *  Copyright 2016 Google Inc. All Rights Reserved.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.google.blockly.android.ui;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.model.Block;
import com.google.blockly.model.Workspace;
import com.google.blockly.model.WorkspacePoint;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link RecyclerView} for Block and their containing {@link BlockGroup} views.  It does not
 * define a {@link RecyclerView.LayoutManager}.
 */
public class BlockListView extends RecyclerView {
    private static final String TAG = "BlockListView";

    public interface OnDragListBlock {
        /**
         * Handles the selection of the draggable {@link BlockGroup}, including possibly adding the
         * block to the {@link Workspace} and {@link WorkspaceView}.
         *
         * @param index The list position of the touched block group.
         * @param blockInList The root block of the touched block.
         * @param touchedBlockGroup The top level touched block.
         * @param initialBlockPosition The initial workspace coordinate for
         *         {@code touchedBlockGroup}'s screen location.
         * @return The block group to drag within the workspace.
         */
        BlockGroup getDraggableBlockGroup(int index, Block blockInList,
                                          BlockGroup touchedBlockGroup,
                                          WorkspacePoint initialBlockPosition);
    }

    private final ArrayList<Block> mBlocks = new ArrayList<>();
    private final Adapter mAdapter = new Adapter();

    private final int[] tempArray = new int[2];
    private final ViewPoint tempViewPoint = new ViewPoint();
    private final WorkspacePoint mTempWorkspacePoint = new WorkspacePoint();

    private WorkspaceHelper mHelper;
    private BlockTouchHandler mTouchHandler;

    public BlockListView(Context context) {
        this(context, null, 0);
    }

    public BlockListView(Context context, AttributeSet attribs) {
        this(context, attribs, 0);
    }

    public BlockListView(Context context, AttributeSet attribs, int style) {
        super(context, attribs, style);

        setAdapter(mAdapter);
        addItemDecoration(new ItemSpacingDecoration(mAdapter));
    }

    public void init(final @Nullable BlocklyController controller,
                     final OnDragListBlock blockDragListener) {
        if (controller == null) {
            mHelper = null;
            mTouchHandler = null;
        } else {
            mHelper = controller.getWorkspaceHelper();
            Dragger dragger = controller.getDragger();
            mTouchHandler = dragger.buildBlockTouchHandler(new Dragger.DragHandler() {
                @Override
                public boolean maybeStartDrag(Dragger.PendingDrag pendingDrag) {
                    BlockView touchedBlockView = pendingDrag.getTouchedBlockView();

                    // When dragging out of the BlockListView, we really care about the root block.
                    Block rootBlock = touchedBlockView.getBlock().getRootBlock();
                    BlockView rootTouchedBlockView = mHelper.getView(rootBlock);
                    BlockGroup rootTouchedGroup = rootTouchedBlockView.getParentBlockGroup();

                    // Calculate the offset from rootTouchedGroup to touchedBlockView in view
                    // pixels. We are assuming there is no scaling or translations between
                    // BlockViews.
                    View view = (View) touchedBlockView;
                    float offsetX = view.getX() + pendingDrag.getTouchDownViewOffsetX();
                    float offsetY = view.getY() + pendingDrag.getTouchDownViewOffsetY();
                    ViewGroup parent = (ViewGroup) view.getParent();
                    while (parent != rootTouchedGroup) {
                        view = parent;
                        offsetX += view.getX();
                        offsetY += view.getY();
                        parent = (ViewGroup) view.getParent();
                    }
                    Log.d(TAG, "Unscaled offset: " + offsetX + ", " + offsetY);

                    // Capture the root group's original screen location (top left corner).
                    int[] rootScreenCoord = tempArray;
                    rootTouchedGroup.getLocationOnScreen(rootScreenCoord);

                    // Adjust for RTL, where the block workspace coordinate will be in the top right
                    if (mHelper.useRtl()) {
                        offsetX = rootTouchedGroup.getWidth() - offsetX;
                    }

                    // Scale into workspace coordinates.
                    offsetX = mHelper.virtualViewToWorkspaceUnits(offsetX);
                    offsetY = mHelper.virtualViewToWorkspaceUnits(offsetY);

                    // Convert touch screen coordinate to workspace coordinate.
                    mHelper.screenToWorkspaceCoordinates(pendingDrag.getTouchDownScreenX(),
                            pendingDrag.getTouchDownScreenY(), mTempWorkspacePoint);

                    // Offset the workspace coord by the BlockGroup's touch offset.
                    mTempWorkspacePoint.offset((int) -offsetX, (int) -offsetY);

                    // Acquire the WorkspaceBlock from the OnDragListBlock
                    int blockIndex = mBlocks.indexOf(rootBlock);
                    BlockGroup dragGroup = blockDragListener.getDraggableBlockGroup(
                            blockIndex, rootBlock, rootTouchedGroup, mTempWorkspacePoint);
                    if (dragGroup != null) {
                        pendingDrag.setDragGroup(dragGroup);
                        return true;
                    } else {
                        return false;
                    }
                }
            });
        }

        // Update all currently visible BlockGroups.
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            // ViewHolder's FrameLayout
            FrameLayout container = (FrameLayout) getChildAt(i);
            if (container.getChildCount() > 0) {
                BlockGroup bg = (BlockGroup) container.getChildAt(0);
                bg.setTouchHandler(mTouchHandler);
            }
        }
    }

    protected void calculateWorkspaceLocation(Dragger.PendingDrag pendingDrag,
                                              BlockView rootTouchedBlockView,
                                              WorkspacePoint workspaceCoord) {
        ViewPoint screenCoord = tempViewPoint;
        screenCoord.x = pendingDrag.getTouchDownScreenX();
        screenCoord.y = pendingDrag.getTouchDownScreenY();
        mHelper.screenToWorkspaceCoordinates(screenCoord, workspaceCoord);

        float connectorOffsetX;
        if (mHelper.useRtl()) {
            connectorOffsetX = rootTouchedBlockView.getWidth()
                    - rootTouchedBlockView.getOutputConnectorMargin()
                    - pendingDrag.getTouchDownViewOffsetX();
        } else {
            connectorOffsetX = pendingDrag.getTouchDownViewOffsetX()
                    - rootTouchedBlockView.getOutputConnectorMargin();
        }
        float wsOffsetX = mHelper.virtualViewToWorkspaceUnits(connectorOffsetX);
        float wsOffsetY = mHelper.virtualViewToWorkspaceUnits(
                pendingDrag.getTouchDownViewOffsetY());
        workspaceCoord.x = (int) (workspaceCoord.x - wsOffsetX);
        workspaceCoord.y = (int) (workspaceCoord.y - wsOffsetY);
    }

    public void setContents(List<Block> blocks) {
        mBlocks.clear();
        mBlocks.addAll(blocks);

        mAdapter.notifyDataSetChanged();
    }

    public void addBlock(Block block) {
        addBlock(mBlocks.size(), block);
    }

    public void addBlock(int insertPostion, Block block) {
        if (insertPostion < 0 || insertPostion > mBlocks.size()) {
            throw new IllegalArgumentException("Invalid position.");
        }
        mBlocks.add(insertPostion, block);
        mAdapter.notifyItemInserted(insertPostion);
    }

    public void removeBlock(Block block) {
        int position = mBlocks.indexOf(block);
        mBlocks.remove(position);
        mAdapter.notifyItemRemoved(position);
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {
        final FrameLayout mContainer;
        BlockGroup bg = null;  // Root of the currently attach block views.

        ViewHolder(Context context) {
            super(new FrameLayout(context));
            mContainer = (FrameLayout) itemView;
        }
    }

    protected class Adapter extends RecyclerView.Adapter<BlockListView.ViewHolder> {
        @Override
        public int getItemCount() {
            return mBlocks.size();
        }

        @Override
        public BlockListView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new BlockListView.ViewHolder(getContext());
        }

        @Override
        public void onBindViewHolder(BlockListView.ViewHolder holder, int position) {
            Block block = mBlocks.get(position);
            BlockGroup bg = mHelper.getParentBlockGroup(block);
            if (bg == null) {
                bg = mHelper.getBlockViewFactory().buildBlockGroupTree(block, null, mTouchHandler);
            } else {
                bg.setTouchHandler(mTouchHandler);
            }
            holder.mContainer.addView(bg, new FrameLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            holder.bg = bg;
        }

        @Override
        public void onViewRecycled(BlockListView.ViewHolder holder) {
            // BlockGroup may be reused under a new parent.
            // Only clear if it is still a child of mContainer.
            if (holder.bg.getParent() == holder.mContainer) {
                holder.bg.unlinkModel();
                holder.bg = null;
                holder.mContainer.removeAllViews();
            }

            super.onViewRecycled(holder);
        }
    }
}
