package com.google.blockly.android.testui;

import android.content.Context;
import android.support.annotation.Nullable;

import com.google.blockly.android.control.ConnectionManager;
import com.google.blockly.android.ui.BlockGroup;
import com.google.blockly.android.ui.BlockTouchHandler;
import com.google.blockly.android.ui.WorkspaceHelper;
import com.google.blockly.android.ui.WorkspaceView;
import com.google.blockly.android.ui.vertical.BlockView;
import com.google.blockly.android.ui.vertical.InputView;
import com.google.blockly.android.ui.vertical.VerticalBlockViewFactory;
import com.google.blockly.model.Block;

import java.util.List;

/**
 * Created by marshalla on 5/3/16.
 */
public class TestableBlockView extends BlockView {
    /**
     * Create a testable version of the vertical {@link BlockView}.
     */
    TestableBlockView(Context context, WorkspaceHelper helper, VerticalBlockViewFactory factory, Block block, List<InputView> inputViews, ConnectionManager connectionManager, @Nullable BlockTouchHandler touchHandler) {
        super(context, helper, factory, block, inputViews, connectionManager, touchHandler);
    }

    /**
     * This method sets the WorkspaceView that would normally occur during onAttachedToWindow().
     * It also recurses on all inputs.
     */
    public void setWorkspaceView(WorkspaceView workspaceView) {
        this.mWorkspaceView = workspaceView;

        int count = getInputViewCount();
        for (int i = 0; i < count; ++i) {
            InputView input = getInputView(i);
            BlockGroup bg = input.getConnectedBlockGroup();
            if (bg != null) {
                ((TestableBlockGroup) bg).setWorkspaceView(workspaceView);
            }
        }
    }
}
