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
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.util.AttributeSet;

import com.google.blockly.android.R;

/**
 * Base class for ToolboxFragment and TrashFragment. Manages the closeable and scroll direction
 * configuration arguments through the {@code closeable} and {@code scrollOrientation} attributes.
 * <p/>
 * {@code blockly:closeable} is a simple boolean. It can also be set via the fragment argument
 * {@link #ARG_CLOSEABLE}. By default, {@code BlockDrawerFragment}s are not closeable. The precise
 * behavior of {@code blockly:closeable} is left to the subclass.
 * <p/>
 * {@code blockly:scrollOrientation} controls the block list, and can be either {@code horizontal}
 * or {@code vertical}. By default it is horizontal.  Alternatively, the app developer can configure
 * thier app's fragment instance via the {@link #ARG_SCROLL_ORIENTATION} fragment argument with
 * either {@link #SCROLL_HORIZONTAL} or {@link #SCROLL_VERTICAL}.
 *
 * @attr ref com.google.blockly.R.styleable#BlockDrawerFragment_closeable
 * @attr ref com.google.blockly.R.styleable#BlockDrawerFragment_scrollOrientation
 */
// TODO(#10): Attribute and argument to set the drawer background.
public abstract class BlockDrawerFragment extends Fragment {
    public static final String ARG_CLOSEABLE = "BlockDrawerFragment_closeable";
    public static final String ARG_SCROLL_ORIENTATION = "BlockDrawerFragment_scrollOrientation";

    // Scroll direction argument values. Same values as {@link LinearLayoutManager}.
    public static final int SCROLL_HORIZONTAL = 0;
    public static final int SCROLL_VERTICAL = 1;

    private static final boolean DEFAULT_CLOSEABLE = false;
    private static final int DEFAULT_SCROLL_ORIENTATION = SCROLL_HORIZONTAL;

    protected BlockListView mBlockListView; // Assigned by subclass.

    protected boolean mCloseable = DEFAULT_CLOSEABLE;
    protected int mScrollOrientation = DEFAULT_SCROLL_ORIENTATION;

    @Override
    public void onInflate(Context context, AttributeSet attrs, Bundle savedInstanceState) {
        super.onInflate(context, attrs, savedInstanceState);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.BlockDrawerFragment,
                0, 0);
        try {
            setCloseable(a.getBoolean(R.styleable.BlockDrawerFragment_closeable,
                    mCloseable));
            setScrollOrientation(
                    a.getInt(R.styleable.BlockDrawerFragment_scrollOrientation,
                    mScrollOrientation));
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

    /**
     * @return True if the fragment is configured to be able to close.
     */
    public boolean isCloseable() {
        return mCloseable;
    }

    /**
     * Sets whether the fragment is closeable.
     */
    public void setCloseable(boolean closeable) {
        mCloseable = closeable;
    }

    /**
     * @return The scrolling direction. Either {@link #SCROLL_HORIZONTAL} or
     *         {@link #SCROLL_VERTICAL}.
     */
    public int getScrollOrientation() {
        return mScrollOrientation;
    }

    /**
     * Sets the orientation of the scrollable blocks.
     *
     * @param scrollOrientation The scrolling direction. Either {@link #SCROLL_HORIZONTAL} or
     *         {@link #SCROLL_VERTICAL}.
     */
    public void setScrollOrientation(int scrollOrientation) {
        if (scrollOrientation != SCROLL_HORIZONTAL && scrollOrientation != SCROLL_VERTICAL) {
            throw new IllegalArgumentException("Invalid orientation: " + scrollOrientation);
        }

        mScrollOrientation = scrollOrientation;

        // Update the view if available and changed.
        if (mBlockListView != null) {
            LinearLayoutManager layout = (LinearLayoutManager) mBlockListView.getLayoutManager();
            if (layout == null) {
                mBlockListView.setLayoutManager(createLinearLayoutManager());
            } else if (layout.getOrientation() != mScrollOrientation) {
                // Preserve as much state as possible by reusing existing LayoutManager.
                layout.setOrientation(mScrollOrientation);
            }
        }
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
     * Reads {@link #ARG_CLOSEABLE} and {@link #ARG_SCROLL_ORIENTATION} from the bundle.
     *
     * @param bundle The {@link Bundle} to read from.
     */
    protected void readArgumentsFromBundle(Bundle bundle) {
        if (bundle != null) {
            setCloseable(bundle.getBoolean(ARG_CLOSEABLE, mCloseable));
            setScrollOrientation(bundle.getInt(ARG_SCROLL_ORIENTATION, mScrollOrientation));
        }
    }

    /**
     * @return {@link LinearLayoutManager} configured in the correct direction.
     */
    protected LinearLayoutManager createLinearLayoutManager() {
        // While it would be nice to see the customized LinearLayoutManager here, its use reveals
        // a double bind error in RecyclerView, that crashes when binding a BlockGroup that has an
        // existing parent.
        LinearLayoutManager layout = new LinearLayoutManager(getContext());
        layout.setOrientation(mScrollOrientation);
        return layout;
    }
}
