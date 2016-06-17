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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.android.control.ConnectionManager;
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
         * @param initialBlockPosition The initial workspace coordinate for
         *         {@code touchedBlockGroup}'s screen location.
         * @return The block group to drag within the workspace.
         */
        BlockGroup getDraggableBlockGroup(int index, Block blockInList,
                                          WorkspacePoint initialBlockPosition);
    }

    private final ArrayList<Block> mBlocks = new ArrayList<>();
    private final Adapter mAdapter = new Adapter();

    private final WorkspacePoint mTempWorkspacePoint = new WorkspacePoint();

    private WorkspaceHelper mHelper;
    private ConnectionManager mConnectionManager;
    private OnDragListBlock mOnDragListBlock;
    private BlockTouchHandler mTouchHandler;

    private boolean mInstantiateOnTap = true;

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

    /**
     * Initializes this BlockListView with the {@link BlocklyController} and {@link OnDragListBlock}
     * listener.  If {@code controller} is null, this BlockListView is reset, including removing all
     * blocks from the list.
     *
     * @param controller The active {@link BlocklyController}.
     * @param onDragListBlock The listener repsonsible for instantiating blocks to the workspace.
     */
    public void init(@Nullable BlocklyController controller, OnDragListBlock onDragListBlock) {
        if (controller == null) {
            mHelper = null;
            mConnectionManager = null;
            mTouchHandler = null;

            // Do not hold any blocks if the controller is unset.
            int priorBlockCount = mBlocks.size();
            mBlocks.clear();
            mAdapter.notifyItemRangeRemoved(0, priorBlockCount-1);
        } else {
            mHelper = controller.getWorkspaceHelper();
            mConnectionManager = controller.getWorkspace().getConnectionManager();
            mOnDragListBlock = onDragListBlock;

            Dragger dragger = controller.getDragger();
            mTouchHandler = dragger.buildImmediateDragBlockTouchHandler(new DragHandler());
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

    /**
     * Calculates the workspace point for a {@link PendingDrag}, such that the
     * {@link MotionEvent#ACTION_DOWN} location remains in the same location on the screen
     * (i.e., under the user's finger), and calls {@link OnDragListBlock#getDraggableBlockGroup}
     * with the location. The workspace point accounts for the {@link WorkspaceView}'s location,
     * pan, and scale.
     *
     * @param pendingDrag The {@link PendingDrag} for the gesture.
     * @return The {@link BlockGroup} return by {@link OnDragListBlock#getDraggableBlockGroup}.
     */
    @NonNull
    protected BlockGroup getWorkspaceBlockGroupForTouch(PendingDrag pendingDrag) {
        BlockView touchedBlockView = pendingDrag.getTouchedBlockView();
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

        // Adjust for RTL, where the block workspace coordinate will be in the top right
        if (mHelper.useRtl()) {
            offsetX = rootTouchedGroup.getWidth() - offsetX;
        }

        // Scale into workspace coordinates.
        int wsOffsetX = mHelper.virtualViewToWorkspaceUnits(offsetX);
        int wsOffsetY = mHelper.virtualViewToWorkspaceUnits(offsetY);

        // Offset the workspace coord by the BlockGroup's touch offset.
        mTempWorkspacePoint.setFrom(
                pendingDrag.getTouchDownWorkspaceCoordinates());
        mTempWorkspacePoint.offset(-wsOffsetX, -wsOffsetY);

        return mOnDragListBlock.getDraggableBlockGroup(
                mBlocks.indexOf(rootBlock), rootBlock, mTempWorkspacePoint);
    }

    /** {@link Dragger.DragHandler} implementation for BlockListViews. */
    private class DragHandler implements Dragger.DragHandler {
        @Override
        public Runnable maybeGetDragGroupCreator(final PendingDrag pendingDrag) {
            return new Runnable() {
                @Override
                public void run() {
                    // Acquire the draggable BlockGroup on the Workspace from the
                    // {@link OnDragListBlock}.
                    BlockGroup dragGroup = getWorkspaceBlockGroupForTouch(pendingDrag);
                    if (dragGroup != null) {
                        pendingDrag.setDragGroup(dragGroup);
                    }

                }
            };
        }

        @Override
        public boolean onBlockClicked(final PendingDrag pendingDrag) {
            if (!mInstantiateOnTap) {
                return false;
            }

            post(new Runnable() {
                @Override
                public void run() {
                    getWorkspaceBlockGroupForTouch(pendingDrag);
                }
            });
            return true;
        }
    }

    /**
     * Adapts {@link Block}s in list into {@link BlockGroup}s inside {@Link FrameLayout}.
     */
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
                bg = mHelper.getBlockViewFactory().buildBlockGroupTree(
                        block, mConnectionManager, mTouchHandler);
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
