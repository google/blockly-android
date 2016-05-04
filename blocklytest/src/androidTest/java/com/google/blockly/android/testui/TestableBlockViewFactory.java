package com.google.blockly.android.testui;

import android.content.Context;

import com.google.blockly.android.control.ConnectionManager;
import com.google.blockly.android.ui.BlockGroup;
import com.google.blockly.android.ui.BlockTouchHandler;
import com.google.blockly.android.ui.WorkspaceHelper;
import com.google.blockly.android.ui.vertical.BlockView;
import com.google.blockly.android.ui.vertical.InputView;
import com.google.blockly.android.ui.vertical.VerticalBlockViewFactory;
import com.google.blockly.model.Block;

import java.util.List;

/**
 * This factory builds "Testable"
 */
public class TestableBlockViewFactory extends VerticalBlockViewFactory {
    public TestableBlockViewFactory(Context context, WorkspaceHelper helper) {
        super(context, helper);
    }

    @Override
    public BlockGroup buildBlockGroup() {
        return new TestableBlockGroup(mContext, mHelper);
    }

    @Override
    protected BlockView buildBlockView(Block block, List<InputView> inputViews, ConnectionManager connectionManager, BlockTouchHandler touchHandler) {
        return new TestableBlockView(mContext, mHelper, this, block, inputViews,
                                     connectionManager, touchHandler);
    }
}
