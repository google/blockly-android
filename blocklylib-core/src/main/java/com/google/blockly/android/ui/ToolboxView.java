package com.google.blockly.android.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.google.blockly.android.R;
import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.model.Block;
import com.google.blockly.model.ToolboxCategory;
import com.google.blockly.utils.ColorUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * View wrapper for toolbox components.
 */
public class ToolboxView extends RelativeLayout {
    protected BlockListView mBlockListView;
    protected CategoryTabs mCategoryTabs;
    protected Button mActionButton;
    protected View mListWrapper;

    protected OnActionClickListener mActionClickListener;
    protected ToolboxCategory mRootCategory;
    protected ToolboxCategory mCurrentCategory;

    // Whether we prefer having toolboxes that are closeable if there are tabs.
    private boolean mPreferCloseable = true;
    // The current state of the toolbox being closeable or not.
    private boolean mCloseable = mPreferCloseable;

    public ToolboxView(Context context) {
        super(context);
    }

    public ToolboxView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ToolboxView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.BlockDrawerFragment,
                0, 0);

        try {
            mPreferCloseable = a.getBoolean(R.styleable.BlockDrawerFragment_closeable, mCloseable);
            mCloseable = mPreferCloseable;
        } finally {
            a.recycle();
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mBlockListView = (BlockListView) findViewById(R.id.block_list_view);
        mActionButton = (Button) findViewById(R.id.action_button);
        mCategoryTabs = (CategoryTabs) findViewById(R.id.category_tabs);
        mListWrapper = findViewById(R.id.list_wrapper);

        if (mActionButton != null) {
            mActionButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mActionClickListener != null) {
                        mActionClickListener.onActionClicked(v, mCurrentCategory);
                    }
                }
            });
        }
    }

    public void setOnActionClickListener(OnActionClickListener listener) {
        mActionClickListener = listener;
    }

    public void setCategoryTabsCallback(CategoryTabs.Callback callback) {
        mCategoryTabs.setCallback(callback);
    }

    public BlockListView getBlockListView() {
        return mBlockListView;
    }

    public void reset() {
        mBlockListView.setContents(new ArrayList<Block>(0));
        mCategoryTabs.setCategories(new ArrayList<ToolboxCategory>(0));
        mListWrapper.setVisibility(mCloseable ? View.GONE : View.VISIBLE);
        mActionButton.setVisibility(View.GONE);
    }

    public void init(@Nullable BlocklyController controller,
            BlockListView.OnDragListBlock onDragListBlock) {
    }

    /**
     * Set the root category for the toolbox. This top level category can contain
     * either a list of blocks or a list of subcategories, but not both. If it has blocks, the
     * {@code ToolboxView} renders as a single tab/group. If it has subcategories, it will
     * render each subcategory with its own tab.
     *
     * @param topLevelCategory The top-level category in the toolbox.
     */
    public void setContents(final ToolboxCategory topLevelCategory) {
        mRootCategory = topLevelCategory;
        if (topLevelCategory == null) {
            reset();
            return;
        }
        List<Block> blocks = topLevelCategory.getBlocks();
        List<ToolboxCategory> subcats = topLevelCategory.getSubcategories();

        if (!blocks.isEmpty() && !subcats.isEmpty()) {
            throw new IllegalArgumentException(
                    "Toolbox cannot have both blocks and categories in the root level.");
        }

        if (!subcats.isEmpty()) {
            // If we have subcategories, use the closeable preference.
            mCloseable = mPreferCloseable;
            mCategoryTabs.setCategories(subcats);
            mCategoryTabs.setVisibility(View.VISIBLE);
            setCurrentCategory(mCloseable ? null : subcats.get(0));
            mCategoryTabs.setTapSelectedDeselects(mCloseable);
        } else {
            // If there's only the top level category don't allow it to be closed.
            mCloseable = false;
            mCategoryTabs.setVisibility(View.GONE);
            if (!blocks.isEmpty()) {
                setCurrentCategory(topLevelCategory);
            } else {
                reset();
            }
        }
    }

    public boolean setCurrentCategory(@Nullable ToolboxCategory category) {
        if (category == null) {
            if (mCloseable) {
                mListWrapper.setVisibility(View.GONE);
                mCategoryTabs.setSelectedCategory(null);
                return true;
            }
            return false;
        }
        mCurrentCategory = category;
        if (category != mRootCategory) {
            mCategoryTabs.setSelectedCategory(category);
        }
        updateCategoryColors(category);
        mBlockListView.setContents(category.getBlocks());
        mListWrapper.setVisibility(View.VISIBLE);
        if (category.isVariableCategory()) {
            mActionButton.setVisibility(View.VISIBLE);
        } else {
            mActionButton.setVisibility(View.GONE);
        }
        return true;
    }

    public boolean isCloseable() {
        return mCloseable;
    }

    protected static final int DEFAULT_BLOCKS_BACKGROUND_ALPHA = 0xBB;
    protected static final int DEFAULT_BLOCKS_BACKGROUND_COLOR = Color.LTGRAY;
    protected static final float BLOCKS_BACKGROUND_LIGHTNESS = 0.75f;
    protected void updateCategoryColors(ToolboxCategory curCategory) {
        Integer maybeColor = curCategory.getColor();
        int bgColor = DEFAULT_BLOCKS_BACKGROUND_COLOR;
        if (maybeColor != null) {
            bgColor = getBackgroundColor(maybeColor);
        }

        int alphaBgColor = Color.argb(
                mCloseable ? DEFAULT_BLOCKS_BACKGROUND_ALPHA : ColorUtils.ALPHA_OPAQUE,
                Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor));
        this.setBackgroundColor(alphaBgColor);
    }

    protected int getBackgroundColor(int categoryColor) {
        return ColorUtils.blendRGB(categoryColor, Color.WHITE, BLOCKS_BACKGROUND_LIGHTNESS);
    }

    public static abstract class OnActionClickListener {
        public abstract void onActionClicked(View v, ToolboxCategory category);
    }

}
