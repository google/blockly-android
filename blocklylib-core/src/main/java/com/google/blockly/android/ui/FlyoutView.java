package com.google.blockly.android.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.google.blockly.android.R;
import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.android.control.ConnectionManager;
import com.google.blockly.model.Block;
import com.google.blockly.model.FlyoutCategory;
import com.google.blockly.model.Workspace;
import com.google.blockly.model.WorkspacePoint;
import com.google.blockly.utils.ColorUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * View for displaying a list of blocks, labels, and buttons.
 */
public class FlyoutView extends RelativeLayout {
    private static final String TAG = "FlyoutView";
    public static final int DEFAULT_BLOCKS_BACKGROUND_ALPHA = 0xBB;
    public static final int DEFAULT_BLOCKS_BACKGROUND_COLOR = Color.LTGRAY;
    protected static final float BLOCKS_BACKGROUND_LIGHTNESS = 0.75f;

    protected Button mActionButton;
    protected RecyclerView mRecyclerView;

    protected Callback mCallback;
    protected FlyoutCategory mCurrentCategory;
    protected WorkspaceHelper mHelper;
    protected ConnectionManager mConnectionManager;
    protected BlockTouchHandler mTouchHandler;

    protected int mBgAlpha = DEFAULT_BLOCKS_BACKGROUND_ALPHA;
    protected int mBgColor = DEFAULT_BLOCKS_BACKGROUND_COLOR;

    protected final Adapter mAdapter = new Adapter();
    protected final CategoryCallback mCategoryCallback = new CategoryCallback();
    private final WorkspacePoint mTempWorkspacePoint = new WorkspacePoint();
    private final ColorDrawable mBgDrawable = new ColorDrawable(mBgColor);


    public FlyoutView(Context context) {
        super(context);
    }

    public FlyoutView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FlyoutView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mActionButton = (Button) findViewById(R.id.action_button);
        mRecyclerView = (RecyclerView) findViewById(R.id.block_list_view);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addItemDecoration(new ItemSpacingDecoration(mAdapter));

        if (mActionButton != null) {
            // TODO (#503): Refactor the action button to be part of the FlyoutCategory.
            // See Category tabs for an example of binding click listeners to items in a recycler.
            mActionButton.setTag("createVar");
            mActionButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mCallback != null) {
                        mCallback.onActionClicked(v, (String) v.getTag(),
                                mCurrentCategory);
                    }
                }
            });
        }
    }

    /**
     * Resets the view, including removing any initialization that was done.
     * {@link #init(BlocklyController, Callback)} must be called before the view can be used again.
     */
    public void reset() {
        mHelper = null;
        mTouchHandler = null;
        mCurrentCategory = null;
        mAdapter.notifyDataSetChanged();
        mActionButton.setVisibility(View.GONE);
    }

    /**
     * Initializes the view.
     *
     * @param controller The controller should be used when building views.
     * @param callback The callback that will handle user actions.
     */
    public void init(BlocklyController controller, Callback callback) {
        mHelper = controller.getWorkspaceHelper();
        mConnectionManager = controller.getWorkspace().getConnectionManager();
        mCallback = callback;
        mTouchHandler = controller.getDragger().buildImmediateDragBlockTouchHandler(
                new DragHandler());
    }

    public void setLayoutManager(RecyclerView.LayoutManager layoutManager) {
        mRecyclerView.setLayoutManager(layoutManager);
    }

    /**
     * Sets the alpha used for the background color when the flyout is open. The color will come from the
     * category when available and fall back to the default color if the category doesn't have one.
     *
     * @param alpha The alpha to apply to the background, 0-255.
     */
    public void setBackgroundAlpha(int alpha) {
        mBgAlpha = alpha;
    }

    /**
     * Sets the color used for the background when one isn't provided by the category.
     *
     * @param color The RGB color to use as the default for the background.
     */
    public void setBackgroundColor(int color) {
        mBgColor = color;
    }

    /**
     * Sets the current category and updates the list of blocks.
     *
     * @param category The category to display in this flyout.
     */
    public void setCurrentCategory(@Nullable FlyoutCategory category) {
        if (mCurrentCategory == category) {
            return;
        }
        if (mCurrentCategory != null) {
            mCurrentCategory.setCallback(null);
        }
        mCurrentCategory = category;
        updateCategoryColors(category);
        mAdapter.notifyDataSetChanged();
        if (mCurrentCategory != null) {
            mCurrentCategory.setCallback(mCategoryCallback);
        }

        // TODO (#503): Refactor action button into category list
        if (category != null && category.isVariableCategory()) {
            mActionButton.setVisibility(View.VISIBLE);
        } else {
            mActionButton.setVisibility(View.GONE);
        }
    }

    public FlyoutCategory getCurrentCategory() {
        return mCurrentCategory;
    }

    protected void updateCategoryColors(FlyoutCategory curCategory) {
        Integer maybeColor = curCategory == null ? null : curCategory.getColor();
        int bgColor = mBgColor;
        if (maybeColor != null) {
            bgColor = getBackgroundColor(maybeColor);
        }

        mBgDrawable.setColor(bgColor);
        mBgDrawable.setAlpha(mBgAlpha);
        setBackground(mBgDrawable);
    }

    protected int getBackgroundColor(int categoryColor) {
        return ColorUtils.blendRGB(categoryColor, Color.WHITE, BLOCKS_BACKGROUND_LIGHTNESS);
    }

    /**
     * Calculates the workspace point for a {@link PendingDrag}, such that the
     * {@link MotionEvent#ACTION_DOWN} location remains in the same location on the screen
     * (i.e., under the user's finger), and calls {@link BlockListView.OnDragListBlock#getDraggableBlockGroup}
     * with the location. The workspace point accounts for the {@link WorkspaceView}'s location,
     * pan, and scale.
     *
     * @param pendingDrag The {@link PendingDrag} for the gesture.
     * @return The pair of {@link BlockGroup} and the view relative touch point returned by
     *         {@link BlockListView.OnDragListBlock#getDraggableBlockGroup}.
     */
    @NonNull
    protected Pair<BlockGroup, ViewPoint> getWorkspaceBlockGroupForTouch(PendingDrag pendingDrag) {
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
        ViewPoint touchOffset = new ViewPoint((int) Math.ceil(offsetX), (int) Math.ceil(offsetY));

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

        BlockGroup dragGroup = mCallback.getDraggableBlockGroup(
                mCurrentCategory.getBlocks().indexOf(rootBlock), rootBlock, mTempWorkspacePoint);
        return Pair.create(dragGroup, touchOffset);
    }

    /**
     *
     */
    public static abstract class Callback {
        /**
         * Called when an action button is clicked (example: when "Create variable" is clicked).
         *
         * @param v The view that was clicked.
         * @param action The action tag associated with the view.
         * @param category The category that this action was in.
         */
        public abstract void onActionClicked(View v, String action, FlyoutCategory category);

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
        public abstract BlockGroup getDraggableBlockGroup(int index, Block blockInList,
                WorkspacePoint initialBlockPosition);
    }


    protected class CategoryCallback extends FlyoutCategory.Callback {
        @Override
        public void onBlockAdded(int index, Block block) {
            mAdapter.notifyItemInserted(index);
        }

        @Override
        public void onBlockRemoved(int index, Block block) {
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
    protected class Adapter extends RecyclerView.Adapter<ViewHolder> {
        @Override
        public int getItemCount() {
            return mCurrentCategory == null ? 0 : mCurrentCategory.getBlocks().size();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(getContext());
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            List<Block> blocks = mCurrentCategory == null ? new ArrayList<Block>()
                    : mCurrentCategory.getBlocks();
            Block block = blocks.get(position);
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
        public void onViewRecycled(ViewHolder holder) {
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

    private static class ViewHolder extends RecyclerView.ViewHolder {
        final FrameLayout mContainer;
        BlockGroup bg = null;  // Root of the currently attach block views.

        ViewHolder(Context context) {
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
            post(new Runnable() {
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
