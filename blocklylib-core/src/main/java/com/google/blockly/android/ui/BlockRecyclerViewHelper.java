/*
 *  Copyright 2017 Google Inc. All Rights Reserved.
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
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.blockly.android.R;
import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.android.control.ConnectionManager;
import com.google.blockly.model.Block;
import com.google.blockly.model.BlocklyCategory;
import com.google.blockly.model.WorkspacePoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides the standard configuration for a {@link android.support.v7.widget.RecyclerView}
 * to display a list of blocks, labels, and buttons.
 */

public class BlockRecyclerViewHelper {
    private static final String TAG = "BlockRVHelper";

    private final Handler mHandler = new Handler();
    private final WorkspacePoint mTempWorkspacePoint = new WorkspacePoint();

    private final RecyclerView mRecyclerView;
    private final Context mContext;
    private final LayoutInflater mHelium;
    private final Adapter mAdapter;
    private final CategoryCallback mCategoryCb;
    private final LinearLayoutManager mLayoutManager;

    private WorkspaceHelper mHelper;
    private ConnectionManager mConnectionManager;
    private FlyoutCallback mCallback;
    private BlocklyCategory mCurrentCategory;
    private BlockTouchHandler mTouchHandler;

    public BlockRecyclerViewHelper(RecyclerView recyclerView, Context context) {
        mRecyclerView = recyclerView;
        mContext = context;
        mHelium = LayoutInflater.from(mContext);
        mAdapter = new Adapter();
        mCategoryCb = new CategoryCallback();
        mLayoutManager = new LinearLayoutManager(context);

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addItemDecoration(new ItemSpacingDecoration(mAdapter));
    }

    /**
     * Initialize this helper. The controller and callback allows user interactions to
     * be handled correctly.
     *
     * @param controller The controller to use for view creation and drag handling.
     * @param callback The {@link FlyoutCallback} that defines how the block list will respond to
     * user events.
     */
    public void init(BlocklyController controller, FlyoutCallback callback) {
        mCallback = callback;
        mHelper = controller.getWorkspaceHelper();
        mConnectionManager = controller.getWorkspace().getConnectionManager();

        mTouchHandler = controller.getDragger()
                .buildImmediateDragBlockTouchHandler(new DragHandler());
    }

    /**
     * Reset all the initialized components so this object may be attached to a new
     * controller or callback. The context and recycler view this was created with will
     * be retained.
     */
    public void reset() {
        mCallback = null;
        mHelper = null;
        mConnectionManager = null;
        mTouchHandler = null;
    }

    /**
     * Change the direction blocks should be laid out and scrolled in the RecyclerView.
     * @param scrollOrientation {@link android.support.v7.widget.OrientationHelper#HORIZONTAL} or
     * {@link android.support.v7.widget.OrientationHelper#VERTICAL}.
     */
    public void setScrollOrientation(int scrollOrientation) {
        mLayoutManager.setOrientation(scrollOrientation);
    }

    /**
     * Sets the category to connect to the {@link RecyclerView}.
     *
     * @param category The category to display blocks for.
     */
    public void setCurrentCategory(@Nullable BlocklyCategory category) {
        if (mCurrentCategory == category) {
            return;
        }
        if (mCurrentCategory != null) {
            mCurrentCategory.setCallback(null);
        }
        mCurrentCategory = category;
        mAdapter.notifyDataSetChanged();
        if (mCurrentCategory != null) {
            mCurrentCategory.setCallback(mCategoryCb);
        }
    }

    /**
     * @return The currently set category.
     */
    public @Nullable
    BlocklyCategory getCurrentCategory() {
        return mCurrentCategory;
    }

    /**
     * Calculates the workspace point for a {@link PendingDrag}, such that the
     * {@link MotionEvent#ACTION_DOWN} location remains in the same location on the screen
     * (i.e., under the user's finger), and calls
     * {@link FlyoutCallback#getDraggableBlockGroup} with the location. The workspace
     * point accounts for the {@link WorkspaceView}'s location, pan, and scale.
     *
     * @param pendingDrag The {@link PendingDrag} for the gesture.
     * @return The pair of {@link BlockGroup} and the view relative touch point returned by
     *         {@link FlyoutCallback#getDraggableBlockGroup}.
     */
    @NonNull
    private Pair<BlockGroup, ViewPoint> getWorkspaceBlockGroupForTouch(PendingDrag pendingDrag) {
        BlockView touchedBlockView = pendingDrag.getTouchedBlockView();
        Block rootBlock = touchedBlockView.getBlock().getRootBlock();
        BlockView rootTouchedBlockView = mHelper.getView(rootBlock);
        BlockGroup rootTouchedGroup = rootTouchedBlockView.getParentBlockGroup();

        // Calculate the offset from rootTouchedGroup to touchedBlockView in view
        // pixels. We are assuming the only transforms between BlockViews are the
        // child offsets.
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
        ViewPoint touchOffset = new ViewPoint((int) Math.ceil(offsetX),
                (int) Math.ceil(offsetY));

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

        int itemIndex = mCurrentCategory.indexOf(rootBlock); // Should never be -1
        BlockGroup dragGroup = mCallback.getDraggableBlockGroup(itemIndex, rootBlock,
                mTempWorkspacePoint);
        return Pair.create(dragGroup, touchOffset);
    }

    /**
     * Internal implementation that listens to changes to the category and refreshes
     * the recycler view if it changes.
     */
    protected class CategoryCallback extends BlocklyCategory.Callback {

        @Override
        public void onItemAdded(int index, BlocklyCategory.CategoryItem item) {
            mAdapter.notifyItemInserted(index);
        }

        @Override
        public void onItemRemoved(int index, BlocklyCategory.CategoryItem block) {
            mAdapter.notifyItemRemoved(index);
        }

        @Override
        public void onCategoryCleared() {
            mAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Adapts {@link Block}s in list into {@link BlockGroup}s inside {@Link FrameLayout}.
     */
    public class Adapter extends RecyclerView.Adapter<BlockViewHolder> {

        @Override
        public int getItemCount() {
            return mCurrentCategory == null ? 0 : mCurrentCategory.getItems().size();
        }

        @Override
        public BlockViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new BlockViewHolder(mContext);
        }

        @Override
        public int getItemViewType(int position) {
            if (mCurrentCategory == null) {
                return -1;
            }
            return mCurrentCategory.getItems().get(position).getType();
        }

        @Override
        public void onBindViewHolder(BlockViewHolder holder, int position) {
            List<BlocklyCategory.CategoryItem> items = mCurrentCategory == null
                    ? new ArrayList<BlocklyCategory.CategoryItem>()
                    : mCurrentCategory.getItems();
            BlocklyCategory.CategoryItem item = items.get(position);
            if (item.getType() == BlocklyCategory.CategoryItem.TYPE_BLOCK) {
                Block block = ((BlocklyCategory.BlockItem) item).getBlock();
                BlockGroup bg = mHelper.getParentBlockGroup(block);
                if (bg == null) {
                    bg = mHelper.getBlockViewFactory().buildBlockGroupTree(
                            block, mConnectionManager, mTouchHandler);
                } else {
                    bg.setTouchHandler(mTouchHandler);
                }
                holder.mContainer.addView(bg, new FrameLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT));
                holder.bg = bg;
            } else if (item.getType() == BlocklyCategory.CategoryItem.TYPE_BUTTON) {
                BlocklyCategory.ButtonItem bItem = (BlocklyCategory.ButtonItem) item;
                final String action = bItem.getAction();
                Button button = (Button) mHelium.inflate(R.layout.simple_button, holder.mContainer,
                        false);
                holder.mContainer.addView(button);
                button.setText(bItem.getText());
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mCallback != null) {
                            mCallback.onButtonClicked(v, action, mCurrentCategory);
                        }
                    }
                });
            } else if (item.getType() == BlocklyCategory.CategoryItem.TYPE_LABEL) {
                BlocklyCategory.LabelItem lItem = (BlocklyCategory.LabelItem) item;
                TextView label = (TextView) mHelium.inflate(R.layout.simple_label,
                        holder.mContainer, false);
                holder.mContainer.addView(label);
                label.setText(lItem.getText());
            } else {
                Log.e(TAG, "Tried to bind unknown item type " + item.getType());
            }
        }

        @Override
        public void onViewRecycled(BlockViewHolder holder) {
            // If this was a block item BlockGroup may be reused under a new parent.
            // Only clear if it is still a child of mContainer.
            if (holder.bg != null && holder.bg.getParent() == holder.mContainer) {
                holder.bg.unlinkModel();
                holder.bg = null;
                holder.mContainer.removeAllViews();
            }

            super.onViewRecycled(holder);
        }
    }

    private class BlockViewHolder extends RecyclerView.ViewHolder {
        final FrameLayout mContainer;
        BlockGroup bg = null;  // Root of the currently attach block views.

        BlockViewHolder(Context context) {
            super(new FrameLayout(context));
            mContainer = (FrameLayout) itemView;
        }
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
                    Pair<BlockGroup, ViewPoint> dragGroupAndTouchOffset =
                            getWorkspaceBlockGroupForTouch(pendingDrag);
                    if (dragGroupAndTouchOffset != null) {
                        pendingDrag.startDrag(
                                mRecyclerView,
                                dragGroupAndTouchOffset.first,
                                dragGroupAndTouchOffset.second);
                    }

                }
            };
        }

        @Override
        public boolean onBlockClicked(final PendingDrag pendingDrag) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    // Identify and process the clicked BlockGroup.
                    getWorkspaceBlockGroupForTouch(pendingDrag);
                }
            });
            return true;
        }
    }

}
