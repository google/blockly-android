package com.google.blockly.model.mutator;

import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.model.Input;
import com.google.blockly.utils.BlockLoadingException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This mutator supports user-defined function definitions that return a value
 * ({@code procedures_defreturn} blocks).
 */
public class ProcedureDefinitionWithReturnMutator extends ProcedureDefinitionMutator {
    public static final String MUTATOR_ID = "procedures_defreturn_mutator";
    public static final Factory<ProcedureDefinitionWithReturnMutator> FACTORY =
            new Factory<ProcedureDefinitionWithReturnMutator>() {
                @Override
                public String getMutatorId() {
                    return MUTATOR_ID;
                }

                @Override
                public ProcedureDefinitionWithReturnMutator newMutator(
                        BlocklyController controller) {
                    return new ProcedureDefinitionWithReturnMutator(this, controller);
                }
            };

    private static final String RETURN = "RETURN";

    public ProcedureDefinitionWithReturnMutator(Factory factory, BlocklyController controller) {
        super(factory, controller);
    }

    @Override
    protected void updateBlock() {
        List<Input> newInputs = new ArrayList<>();
        newInputs.add(newDefinitionHeader());
        if (mHasStatementInput) {
            newInputs.add(getDefintionStatementsInput());
        }
        newInputs.add(mBlock.getInputByName(RETURN));
        mBlock.reshape(newInputs);
    }
}
