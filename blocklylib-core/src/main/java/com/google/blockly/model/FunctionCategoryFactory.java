package com.google.blockly.model;

import android.util.Log;

import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.android.control.ProcedureManager;
import com.google.blockly.utils.BlockLoadingException;

import java.util.Map;

/**
 * Class for building {@link BlocklyCategory categories} for function blocks.
 */
// TODO: Rename "ProceduteCategoryFactory"
public class FunctionCategoryFactory extends CategoryFactory {
    private static final String TAG = "FunctionCategoryFactory";

    private static final BlockTemplate DEFINE_NO_RETURN_BLOCK_TEMPLATE =
            new BlockTemplate(ProcedureManager.DEFINE_NO_RETURN_BLOCK_TYPE);
    private static final BlockTemplate DEFINE_WITH_RETURN_BLOCK_TEMPLATE =
            new BlockTemplate(ProcedureManager.DEFINE_WITH_RETURN_BLOCK_TEMPLATE);
    private static final BlockTemplate CALL_NO_RETURN_BLOCK_TEMPLATE =
            new BlockTemplate(ProcedureManager.CALL_NO_RETURN_BLOCK_TEMPLATE);
    private static final BlockTemplate CALL_WITH_RETURN_BLOCK_TEMPLATE =
            new BlockTemplate(ProcedureManager.CALL_WITH_RETURN_BLOCK_TEMPLATE);

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

        try {
            Block block = factory.obtainBlockFrom(DEFINE_NO_RETURN_BLOCK_TEMPLATE);
            category.addItem(new BlocklyCategory.BlockItem(block));

            block = factory.obtainBlockFrom(DEFINE_WITH_RETURN_BLOCK_TEMPLATE);
            category.addItem(new BlocklyCategory.BlockItem(block));

            Map<String, Block> definitions = mProcedureManager.getDefinitionBlocks();
            if (!mProcedureManager.hasReferenceWithReturn()) {
                block = factory.obtainBlockFrom(DEFINE_WITH_RETURN_BLOCK_TEMPLATE);
                category.addItem(new BlocklyCategory.BlockItem(block));
            }

            // TODO:

            category.addItem(new BlocklyCategory.BlockItem(block));
        } catch (BlockLoadingException e) {
            Log.e(TAG, "Failed to obtain toolbox blocks.", e);
            category.clear();
        }

        return category;
    }
}
