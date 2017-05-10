package com.google.blockly.model;

import com.google.blockly.android.control.BlocklyController;

/**
 * Class for building {@link BlocklyCategory categories} for function blocks.
 */
public class FunctionCategoryFactory extends CategoryFactory {
    private final BlocklyController mController;

    public FunctionCategoryFactory(BlocklyController controller) {
        mController = controller;
    }

    @Override
    public BlocklyCategory obtainCategory(String customType) {
        BlockFactory factory = mController.getBlockFactory();

        BlocklyCategory category = new BlocklyCategory();
        // TODO: Add blocks.
        return category;
    }
}
