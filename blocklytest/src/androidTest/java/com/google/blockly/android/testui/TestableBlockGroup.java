package com.google.blockly.android.testui;

import android.content.Context;
import android.view.View;

import com.google.blockly.android.ui.BlockGroup;
import com.google.blockly.android.ui.WorkspaceHelper;
import com.google.blockly.android.ui.WorkspaceView;

/**
 * This {@link BlockGroup} exposes methods not usually available, specifically for testing.
 */
public class TestableBlockGroup extends BlockGroup {
    /**
     * Creates a BlockGroup with methods to facilitate testing Blockly..
     */
    public TestableBlockGroup(Context context, WorkspaceHelper helper) {
        super(context, helper);
    }

    /**
     * This method sets the WorkspaceView that would normally occur during onAttachedToWindow().
     */
    public void setWorkspaceView(WorkspaceView workspaceView) {
        int count = getChildCount();
        for (int i = 0; i < count; ++i) {
            View child = getChildAt(i);
            ((TestableBlockView) child).setWorkspaceView(workspaceView);
        }
    }
}
