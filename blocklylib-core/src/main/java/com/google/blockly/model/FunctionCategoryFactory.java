package com.google.blockly.model;

import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.android.control.ProcedureManager;

/**
 * Class for building {@link BlocklyCategory categories} for function blocks.
 */
public class FunctionCategoryFactory extends CategoryFactory {
    protected final BlocklyController mController;
    protected final Workspace mWorkspace;
    protected final ProcedureManager mProcedureManager;

    public FunctionCategoryFactory(BlocklyController controller) {
        mController = controller;
        mWorkspace = mController.getWorkspace();
        mProcedureManager = mWorkspace.getProcedureManager();
    }

    @Override
    public BlocklyCategory obtainCategory(String customType) {
        BlockFactory factory = mController.getBlockFactory();



        BlocklyCategory category = new BlocklyCategory();
        return category;
    }
}
