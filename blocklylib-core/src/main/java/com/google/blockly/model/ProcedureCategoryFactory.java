package com.google.blockly.model;

import com.google.blockly.android.R;
import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.android.control.ProcedureManager;
import com.google.blockly.utils.BlockLoadingException;

import java.util.Comparator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Class for building {@link BlocklyCategory categories} for procedure blocks (user-defined
 * functions).
 */
public class ProcedureCategoryFactory extends CategoryFactory {
    private static final String TAG = "ProcedureCategoryFactor";  // 23 chars max

    private static final String NAME = ProcedureManager.PROCEDURE_NAME_FIELD;

    private static final BlockTemplate DEFINE_NO_RETURN_BLOCK_TEMPLATE =
            new BlockTemplate(ProcedureManager.DEFINE_NO_RETURN_BLOCK_TYPE);
    private static final BlockTemplate DEFINE_WITH_RETURN_BLOCK_TEMPLATE =
            new BlockTemplate(ProcedureManager.DEFINE_WITH_RETURN_BLOCK_TYPE);
    private static final BlockTemplate CALL_NO_RETURN_BLOCK_TEMPLATE =
            new BlockTemplate(ProcedureManager.CALL_NO_RETURN_BLOCK_TYPE);
    private static final BlockTemplate CALL_WITH_RETURN_BLOCK_TEMPLATE =
            new BlockTemplate(ProcedureManager.CALL_WITH_RETURN_BLOCK_TYPE);
    private static final BlockTemplate IF_RETURN_TEMPLATE =
            new BlockTemplate("procedures_ifreturn");

    protected final BlocklyController mController;
    protected final Workspace mWorkspace;
    protected final ProcedureManager mProcedureManager;

    protected final String mDefaultProcedureName;

    public ProcedureCategoryFactory(BlocklyController controller) {
        mController = controller;
        mWorkspace = mController.getWorkspace();
        mProcedureManager = mWorkspace.getProcedureManager();

        mDefaultProcedureName = getDefaultProcedureName();
    }

    public String getDefaultProcedureName() {
        // TODO: Get from translation manager.
        return mController.getContext().getString(R.string.blockly_default_function_name);
    }



    @Override
    public BlocklyCategory obtainCategory(String customType) throws BlockLoadingException {
        BlocklyCategory category = new BlocklyCategory();
        rebuildItems(category);

        // TODO: Update toolbox view upon procedure changes.
        //final WeakReference<BlocklyCategory> catRef = new WeakReference<>(category);
        //mProcedureManager.registerObserver(new DataSetObserver() {
        //    @Override
        //    public void onChanged() {
        //        BlocklyCategory category = catRef.get();
        //        if (category == null) {
        //            // If the category isn't being used anymore clean up this observer.
        //            mProcedureManager.unregisterObserver(this);
        //        } else {
        //            // Otherwise, update the category's list.
        //            rebuildItems(category);
        //        }
        //    }
        //});
        return category;
    }

    private void rebuildItems(BlocklyCategory category) throws BlockLoadingException {
        BlockFactory factory = mController.getBlockFactory();

        Block block = factory.obtainBlockFrom(DEFINE_NO_RETURN_BLOCK_TEMPLATE);
        ((FieldInput)block.getFieldByName(NAME)).setText(mDefaultProcedureName);
        category.addItem(new BlocklyCategory.BlockItem(block));

        block = factory.obtainBlockFrom(DEFINE_WITH_RETURN_BLOCK_TEMPLATE);
        ((FieldInput)block.getFieldByName(NAME)).setText(mDefaultProcedureName);
        category.addItem(new BlocklyCategory.BlockItem(block));

        if (!mProcedureManager.hasReferenceWithReturn()) {
            block = factory.obtainBlockFrom(IF_RETURN_TEMPLATE);
            category.addItem(new BlocklyCategory.BlockItem(block));
        }

        // Create a call block for each definition.
        final Map<String, Block> definitions = mProcedureManager.getDefinitionBlocks();
        SortedSet<String> sortedProcNames = new TreeSet<>(new Comparator<String>() {
            @Override
            public int compare(String procName1, String procName2) {
                Block def1 = definitions.get(procName1);
                Block def2 = definitions.get(procName2);
                String type1 = def1.getType();
                String type2 = def2.getType();

                // procedures_defnoreturn < procedures_defreturn
                int typeComp = type1.compareTo(type2);
                if (typeComp != 0) {
                    return typeComp;
                }
                // Otherwise sort by procedure name, alphabetically
                int nameComp = procName1.compareToIgnoreCase(procName2);
                if (nameComp != 0) {
                    return nameComp;
                }
                return def1.getId().compareTo(def2.getId()); // Last resort, by block id
            }
        });
        sortedProcNames.addAll(definitions.keySet());
        for (String procName : sortedProcNames) {
            Block defBlock = definitions.get(procName);
            if (defBlock.getType().equals(ProcedureManager.DEFINE_NO_RETURN_BLOCK_TYPE)) {
                // New call block, without return value.
                Block callBlock = factory.obtainBlockFrom(CALL_NO_RETURN_BLOCK_TEMPLATE);
                ((FieldLabel)callBlock.getFieldByName(NAME)).setText(procName);
                category.addItem(new BlocklyCategory.BlockItem(callBlock));
            } else {
                // New call block, with return value.
                Block callBlock = factory.obtainBlockFrom(CALL_WITH_RETURN_BLOCK_TEMPLATE);
                ((FieldLabel)callBlock.getFieldByName(NAME)).setText(procName);
                category.addItem(new BlocklyCategory.BlockItem(callBlock));
            }
        }
    }
}
