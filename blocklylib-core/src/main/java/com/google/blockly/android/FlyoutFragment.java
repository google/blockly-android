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

package com.google.blockly.android;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.android.control.FlyoutController;
import com.google.blockly.android.ui.BlockListHelper;
import com.google.blockly.android.ui.FlyoutCallback;
import com.google.blockly.android.ui.WorkspaceHelper;
import com.google.blockly.model.Block;
import com.google.blockly.model.FlyoutCategory;
import com.google.blockly.utils.ColorUtils;

/**
 * A drawer UI to show a set of {@link Block Blocks} one can drag into the workspace. The
 * available blocks are provided by a {@link FlyoutCategory}, with this fragment
 * displaying a single set of blocks. Set the blocks currently being shown by using
 * {@link #setCurrentCategory(FlyoutCategory)}.
 * <p/>
 * This Fragment is often used with {@link CategorySelectorFragment} which displays a list of tabs built
 * from a root category. The fragments don't interact directly, but the
 * {@link FlyoutController} can be used to handle the interaction between these components.
 * <p/>
 * The behavior of the {@code FlyoutFragment} is configurable in xml. {@code closeable} and
 * {@code scrollOrientation} attributes may be set to affect the display and behavior of this
 * fragment.
 * <p/>
 * For example:
 * <blockquote><pre>
 * &lt;fragment
 *     xmlns:android="http://schemas.android.com/apk/res/android"
 *     xmlns:blockly="http://schemas.android.com/apk/res-auto"
 *     android:name="com.google.blockly.FlyoutFragment"
 *     android:id="@+id/blockly_toolbox"
 *     android:layout_width="wrap_content"
 *     android:layout_height="match_parent"
 *     <b>blockly:closeable</b>="false"
 *     <b>blockly:scrollOrientation</b>="vertical"
 *     /&gt;
 * </pre></blockquote>
 * <p/>
 * When {@code blockly:closeable} is true, the drawer of blocks will hide in the closed state. The
 * tabs will remain visible, providing the user a way to open the drawers.
 * <p/>
 * {@code blockly:scrollOrientation} can be either {@code horizontal} or {@code vertical}, and
 * affects only the block list. The tab scroll orientation is determined by the {@code tabEdge}.
 * <p/>
 * Developers can further customize the flyout by providing their own FlyoutView in
 * {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
 *
 * @attr ref com.google.blockly.R.styleable#BlockDrawerFragment_closeable
 * @attr ref com.google.blockly.R.styleable#BlockDrawerFragment_scrollOrientation
 * @attr ref com.google.blockly.R.styleable#ToolboxFragment_tabEdge
 * @attr ref com.google.blockly.R.styleable#ToolboxFragment_rotateTabs
 */
// TODO(#9): Attribute and arguments to set the tab background.
// TODO (#510): Create an interface for the {@link BlocklyController} to interact with this fragment
public class FlyoutFragment extends Fragment {
    private static final String TAG = "FlyoutFragment";

    public static final int DEFAULT_BLOCKS_BACKGROUND_ALPHA = 0xBB;
    public static final int DEFAULT_BLOCKS_BACKGROUND_COLOR = Color.LTGRAY;
    protected static final float BLOCKS_BACKGROUND_LIGHTNESS = 0.75f;

    protected static final String ARG_CLOSEABLE = "BlockDrawerFragment_closeable";
    protected static final String ARG_SCROLL_ORIENTATION = "BlockDrawerFragment_scrollOrientation";

    protected int mBgAlpha = DEFAULT_BLOCKS_BACKGROUND_ALPHA;
    protected int mBgColor = DEFAULT_BLOCKS_BACKGROUND_COLOR;

    protected View mFlyoutView;
    protected Button mActionButton;
    protected BlocklyController mController;
    protected WorkspaceHelper mHelper;

    protected boolean mCloseable = true;
    protected int mScrollOrientation = OrientationHelper.VERTICAL;

    protected FlyoutCallback mViewCallback = null;
    protected BlockListHelper mBlockListHelper;
    protected final ColorDrawable mBgDrawable = new ColorDrawable(mBgColor);

    @Override
    public void onInflate(Context context, AttributeSet attrs, Bundle savedInstanceState) {
        super.onInflate(context, attrs, savedInstanceState);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.BlocklyFlyout,
                0, 0);

        try {
            mCloseable = a.getBoolean(R.styleable.BlocklyFlyout_closeable,
                    mCloseable);
            mScrollOrientation = a.getInt(R.styleable.BlocklyFlyout_scrollOrientation,
                    mScrollOrientation);
        } finally {
            a.recycle();
        }
        // Store values in arguments, so fragment resume works (no inflation during resume).
        Bundle args = getArguments();
        if (args == null) {
            setArguments(args = new Bundle());
        }
        args.putBoolean(ARG_CLOSEABLE, mCloseable);
        args.putInt(ARG_SCROLL_ORIENTATION, mScrollOrientation);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Read configuration, preferring the saved values over the inflated values
        readArgumentsFromBundle(getArguments());
        readArgumentsFromBundle(savedInstanceState);

        int layout = mScrollOrientation == OrientationHelper.VERTICAL
                ? R.layout.default_flyout_start : R.layout.default_flyout_bottom;
        mFlyoutView = inflater.inflate(layout, null);

        // TODO (#503): Refactor action button into category list
        mActionButton = (Button) mFlyoutView.findViewById(R.id.action_button);
        mActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mViewCallback != null) {
                    // TODO (#503): replace action name with v.getTag()
                    mViewCallback.onButtonClicked(v, null /*action name*/,
                            mBlockListHelper.getCurrentCategory());
                }
            }
        });

        RecyclerView recyclerView = (RecyclerView) mFlyoutView.findViewById(R.id.block_list_view);
        mBlockListHelper = new BlockListHelper(recyclerView, getContext());
        mBlockListHelper.setScrollOrientation(mScrollOrientation);
        return mFlyoutView;
    }

    /**
     * Saves closeable and scroll orientation state.
     *
     * @param outState {@link Bundle} to write.
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(ARG_CLOSEABLE, mCloseable);
        outState.putInt(ARG_SCROLL_ORIENTATION, mScrollOrientation);
    }

    /**
     * Connects the {@link FlyoutFragment} to the application's drag and click handling. It is
     * called by
     * {@link BlocklyController#setToolboxFragments(FlyoutFragment, CategorySelectorFragment)}
     * and should not be called by the application developer.
     *
     * @param callback The callback that will handle user actions in the flyout.
     */
    public void init(BlocklyController controller, FlyoutCallback callback) {
        if (mController != null) {
            throw new IllegalStateException("This flyout is already initialized!");
        }

        mController = controller;
        mViewCallback = callback;
        if (mController == null) {
            mBlockListHelper.reset();
        }
        mBlockListHelper.init(controller, callback);
    }

    /**
     * Sets the Flyout's current {@link FlyoutCategory}, including opening or closing the drawer.
     * In closeable toolboxes, {@code null} {@code category} is equivalent to closing the drawer.
     * Otherwise, the drawer will be rendered empty.
     *
     * @param category The {@link FlyoutCategory} with blocks to display.
     */
    public void setCurrentCategory(@Nullable FlyoutCategory category) {
        mBlockListHelper.setCurrentCategory(category);
        updateCategoryColors(category);
        // TODO (#503): Refactor action button into category list
        if (category != null && category.isVariableCategory()) {
            mActionButton.setVisibility(View.VISIBLE);
            // Note: tag not currently used, but "CREATE_VARIABLE" is the key used by web
            mActionButton.setTag("CREATE_VARIABLE");
        } else {
            mActionButton.setVisibility(View.GONE);
        }
        // TODO(#80): Add animation hooks for subclasses.
        if (category == null) {
            mFlyoutView.setVisibility(View.GONE);
        } else {
            mFlyoutView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * @return The currently set category.
     */
    public FlyoutCategory getCurrentCategory() {
        return mBlockListHelper.getCurrentCategory();
    }

    /**
     * Sets whether this flyout should be allowed to close.
     *
     * @param closeable True to allow it to close, false otherwise.
     */
    public void setCloseable(boolean closeable) {
        mCloseable = closeable;
    }

    /**
     * @return True if this flyout is allowed to close, false otherwise.
     */
    public boolean isCloseable() {
        return mCloseable;
    }

    /**
     * @return True if this fragment is currently visible, false otherwise.
     */
    public boolean isOpen() {
        return mFlyoutView.getVisibility() == View.VISIBLE;
    }

    /**
     * Attempts to close the blocks drawer.
     *
     * @return True if an action was taken (the drawer is closeable and was previously open).
     */
    // TODO(#80): Add animation hooks for subclasses.
    public boolean closeBlocksDrawer() {
        if (!isCloseable() || mFlyoutView.getVisibility() == View.GONE) {
            return false;
        }
        setCurrentCategory(null);
        return true;
    }

    /**
     * Reads {@link #ARG_CLOSEABLE} and {@link #ARG_SCROLL_ORIENTATION} from the bundle.
     *
     * @param bundle The {@link Bundle} to read from.
     */
    protected void readArgumentsFromBundle(Bundle bundle) {
        if (bundle != null) {
            mCloseable = bundle.getBoolean(ARG_CLOSEABLE, mCloseable);
            mScrollOrientation = bundle.getInt(ARG_SCROLL_ORIENTATION, mScrollOrientation);
        }
    }

    /**
     * Sets the background color for the flyout's view based on the category.
     *
     * @param curCategory The category to set the color from or null.
     */
    protected void updateCategoryColors(FlyoutCategory curCategory) {
        Integer maybeColor = curCategory == null ? null : curCategory.getColor();
        int bgColor = mBgColor;
        if (maybeColor != null) {
            bgColor = getBackgroundColor(maybeColor);
        }

        mBgDrawable.setColor(bgColor);
        mBgDrawable.setAlpha(mBgAlpha);
        mFlyoutView.setBackground(mBgDrawable);
    }

    /**
     * Adjusts a color to make it appropriate for a background behind blocks of that color.
     *
     * @param categoryColor The color to adjust for the background.
     * @return A color appropriate for the background behind blocks.
     */
    protected int getBackgroundColor(int categoryColor) {
        return ColorUtils.blendRGB(categoryColor, Color.WHITE, BLOCKS_BACKGROUND_LIGHTNESS);
    }
}
