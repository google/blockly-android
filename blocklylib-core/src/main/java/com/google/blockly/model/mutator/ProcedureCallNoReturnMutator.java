package com.google.blockly.model.mutator;

import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.utils.BlockLoadingException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * This mutator supports user-defined function calls that do not return a value
 * ({@code procedures_callnoreturn} blocks).
 */
public class ProcedureCallNoReturnMutator extends AbstractProcedureMutator {
    public static final String MUTATOR_ID = "procedures_callnoreturn_mutator";
    public static final Factory<ProcedureCallNoReturnMutator> FACTORY =
            new Factory<ProcedureCallNoReturnMutator>() {
                @Override
                public String getMutatorId() {
                    return MUTATOR_ID;
                }

                @Override
                public ProcedureCallNoReturnMutator newMutator(BlocklyController controller) {
                    return new ProcedureCallNoReturnMutator(this, controller);
                }
            };


    public ProcedureCallNoReturnMutator(Factory factory, BlocklyController controller) {
        super(factory, controller);
    }

    @Override
    public void update(XmlPullParser parser)
            throws BlockLoadingException, IOException, XmlPullParserException {
        super.update(parser);

        // TODO: Reshape mBlock
    }
}
