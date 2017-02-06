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

package com.google.blockly.android;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.OrientationHelper;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.android.ui.CategoryTabs;
import com.google.blockly.android.ui.CategoryView;
import com.google.blockly.android.ui.Rotation;
import com.google.blockly.android.ui.WorkspaceHelper;
import com.google.blockly.model.FlyoutCategory;

/**
 * Creates a set of tabs of Blockly categories. The set of categories should be provided by
 * {@link #setContents(FlyoutCategory)}. Interaction with other fragments is not handled by this
 * class and should be done by registering a callback with
 * {@link #setCategoryCallback(CategoryTabs.Callback)}.
 *
 */
public class CategorySelectorFragment extends Fragment implements CategorySelectorUiInterface {
    private static final String TAG = "CategorySelectorFragment";

    protected CategoryView mCategoryView;
    protected WorkspaceHelper mHelper;
    protected BlocklyController mController;

    protected int mScrollOrientation = OrientationHelper.VERTICAL;
    protected @Rotation.Enum int mLabelRotation = Rotation.NONE;


    @Override
    public void onInflate(Context context, AttributeSet attrs, Bundle savedInstanceState) {
        super.onInflate(context, attrs, savedInstanceState);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.BlocklyCategory,
                0, 0);

        try {
            mScrollOrientation = a.getInt(R.styleable.BlocklyCategory_scrollOrientation,
                    mScrollOrientation);
            //noinspection ResourceType
            mLabelRotation = a.getInt(R.styleable.BlocklyCategory_labelRotation, mLabelRotation);
        } finally {
            a.recycle();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        int layout = mScrollOrientation == OrientationHelper.VERTICAL
                ? R.layout.default_category_start : R.layout.default_category_horizontal;
        mCategoryView = (CategoryView) inflater.inflate(layout, null);
        mCategoryView.setLabelRotation(mLabelRotation);
        mCategoryView.setScrollOrientation(mScrollOrientation);
        return mCategoryView;
    }

    public void setContents(FlyoutCategory rootCategory) {
        mCategoryView.setContents(rootCategory);
    }

    public void setCurrentCategory(FlyoutCategory category) {
        mCategoryView.setCurrentCategory(category);
    }

    public FlyoutCategory getCurrentCategory() {
        return mCategoryView.getCurrentCategory();
    }

    public void setCategoryCallback(CategoryTabs.Callback categoryCallback) {
        mCategoryView.setCallback(categoryCallback);
    }
}
