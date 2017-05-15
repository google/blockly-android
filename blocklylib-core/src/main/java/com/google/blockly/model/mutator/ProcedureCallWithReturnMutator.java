package com.google.blockly.model.mutator;

import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.utils.BlockLoadingException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * This mutator supports user-defined function calls that return a value
 * ({@code procedures_callreturn} blocks).
 */
public class ProcedureCallWithReturnMutator extends ProcedureCallMutator {
    public static final String MUTATOR_ID = "procedures_callreturn_mutator";
    public static final Factory<ProcedureCallWithReturnMutator> FACTORY =
            new Factory<ProcedureCallWithReturnMutator>() {
                @Override
                public String getMutatorId() {
                    return MUTATOR_ID;
                }

                @Override
                public ProcedureCallWithReturnMutator newMutator(BlocklyController controller) {
                    return new ProcedureCallWithReturnMutator(this, controller);
                }
            };


    public ProcedureCallWithReturnMutator(Factory factory, BlocklyController controller) {
        super(factory, controller);
    }

    @Override
    protected void updateBlock() {

    }
}
