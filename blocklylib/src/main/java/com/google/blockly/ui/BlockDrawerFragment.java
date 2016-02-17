package com.google.blockly.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.util.AttributeSet;

import com.google.blockly.R;

/**
 * Base class for ToolboxFragment and TrashFragment.  Manages the closeable and scroll direction
 * configuration arguments.
 */
public abstract class BlockDrawerFragment extends Fragment {
    public static final String ARG_CLOSEABLE = "closeable";
    public static final String ARG_SCROLL_ORIENTATION = "scrollOrientation";

    // Scroll direction argument values.  Same values as {@link LinearLayoutManager}.
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

    public boolean isCloseable() {
        return mCloseable;
    }

    public void setCloseable(boolean closeable) {
        mCloseable = closeable;
    }

    public int getScrollOrientation() {
        return mScrollOrientation;
    }

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
