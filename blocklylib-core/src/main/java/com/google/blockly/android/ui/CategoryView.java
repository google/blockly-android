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
import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.annotation.Nullable;
import android.support.v7.widget.OrientationHelper;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

import com.google.blockly.android.R;
import com.google.blockly.model.BlocklyCategory;
import com.google.blockly.utils.ColorUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * View wrapper for toolbox components.
 */
public class CategoryView extends RelativeLayout {
    public static final int DEFAULT_CATEGORIES_BACKGROUND_ALPHA = 0xFF;
    public static final int DEFAULT_CATEGORIES_BACKGROUND_COLOR = Color.LTGRAY;
    protected static final float BLOCKS_BACKGROUND_LIGHTNESS = 0.75f;

    protected CategoryTabs mCategoryTabs;

    protected BlocklyCategory mRootCategory;
    protected BlocklyCategory mCurrentCategory;

    // Whether we prefer having toolboxes that are closeable if there are tabs.
    private boolean mPreferCloseable = true;
    // The current state of the toolbox being closeable or not.
    private boolean mCloseable = mPreferCloseable;
    private int mScrollOrientation = OrientationHelper.VERTICAL;
    private @Rotation.Enum int mLabelRotation = Rotation.NONE;
    private CategorySelectorUI.Callback mCallback;

    public CategoryView(Context context) {
        super(context);
    }

    public CategoryView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CategoryView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.BlocklyFlyout,
                0, 0);

        try {
            mPreferCloseable = a.getBoolean(R.styleable.BlocklyFlyout_closeable, mCloseable);
            mCloseable = mPreferCloseable;
        } finally {
            a.recycle();
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mCategoryTabs = (CategoryTabs) findViewById(R.id.category_tabs);
        mCategoryTabs.setCallback(mCallback);
        mCategoryTabs.setOrientation(mScrollOrientation);
        mCategoryTabs.setLabelRotation(mLabelRotation);
    }

    public void setCallback(CategorySelectorUI.Callback callback) {
        mCallback = callback;
        if (mCategoryTabs != null) {
            mCategoryTabs.setCallback(callback);
        }
    }

    public void reset() {
        mCategoryTabs.setCategories(new ArrayList<BlocklyCategory>(0));
    }

    /**
     * Set the root category for the toolbox. This top level category must contain
     * a list of subcategories or be null. If it has subcategories, it will
     * render each subcategory with its own tab.
     *
     * @param topLevelCategory The top-level category in the toolbox.
     */
    public void setContents(@Nullable final BlocklyCategory topLevelCategory) {
        mRootCategory = topLevelCategory;
        if (topLevelCategory == null) {
            reset();
            return;
        }
        List<BlocklyCategory> subcats = topLevelCategory.getSubcategories();

        if (subcats.isEmpty()) {
            throw new IllegalArgumentException("Contents must be a set of subcategories.");
        }

        // If we have subcategories, use the closeable preference.
        mCloseable = mPreferCloseable;
        mCategoryTabs.setCategories(subcats);
        mCategoryTabs.setVisibility(View.VISIBLE);
        setCurrentCategory(mCloseable ? null : subcats.get(0));
        mCategoryTabs.setTapSelectedDeselects(mCloseable);
    }

    public void setCurrentCategory(@Nullable BlocklyCategory category) {
        if (category == mCurrentCategory) {
            return;
        }
        mCurrentCategory = category;
        mCategoryTabs.setSelectedCategory(category);
        updateCategoryColors(category);
    }

    public BlocklyCategory getCurrentCategory() {
        return mCurrentCategory;
    }

    public boolean isCloseable() {
        return mCloseable;
    }

    public void setScrollOrientation(int orientation) {
        mScrollOrientation = orientation;
        if (mCategoryTabs != null) {
            mCategoryTabs.setOrientation(orientation);
        }
    }

    public void setLabelRotation(int rotation) {
        mLabelRotation = rotation;
        if (mCategoryTabs != null) {
            mCategoryTabs.setLabelRotation(mLabelRotation);
        }
    }

    protected void updateCategoryColors(BlocklyCategory curCategory) {
        Integer maybeColor = curCategory == null ? null : curCategory.getColor();
        int bgColor = DEFAULT_CATEGORIES_BACKGROUND_COLOR;
        if (maybeColor != null) {
            bgColor = getBackgroundColor(maybeColor);
        }

        int alphaBgColor = Color.argb(
                mCloseable ? DEFAULT_CATEGORIES_BACKGROUND_ALPHA : ColorUtils.ALPHA_OPAQUE,
                Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor));
        this.setBackgroundColor(alphaBgColor);
    }

    protected int getBackgroundColor(int categoryColor) {
        return ColorUtils.blendRGB(categoryColor, Color.WHITE, BLOCKS_BACKGROUND_LIGHTNESS);
    }
}
