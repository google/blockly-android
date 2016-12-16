package com.google.blockly.android;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.android.ui.CategoryView;
import com.google.blockly.android.ui.WorkspaceHelper;

/**
 * Created by epastern on 12/15/16.
 */

public class CategoryFragment extends Fragment {
    private static final String TAG = "CategoryFragment";

    protected CategoryView mCategoryView;
    protected BlocklyController mController;
    protected WorkspaceHelper mHelper;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return new TextView(getContext());
    }
}
