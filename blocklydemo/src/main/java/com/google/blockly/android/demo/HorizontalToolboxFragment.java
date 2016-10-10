package com.google.blockly.android.demo;

import android.os.Bundle;
import android.view.LayoutInflater;

import com.google.blockly.android.ToolboxFragment;
import com.google.blockly.android.ui.ToolboxView;

/**
 * Example of styling a Toolbox that uses a horizontal scrolling flyout.
 */
public class HorizontalToolboxFragment extends ToolboxFragment {
    @Override
    protected ToolboxView onCreateToolboxView(LayoutInflater inflater, Bundle savedInstanceState) {
        mToolboxView = (ToolboxView) inflater.inflate(R.layout.horizontal_toolbox, null);
        return mToolboxView;
    }
}
