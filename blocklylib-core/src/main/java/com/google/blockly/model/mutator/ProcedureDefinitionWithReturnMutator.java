package com.google.blockly.model.mutator;

import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.utils.BlockLoadingException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * This mutator supports user-defined function definitions that return a value
 * ({@code procedures_defreturn} blocks).
 */
public class ProcedureDefinitionWithReturnMutator extends AbstractProcedureMutator {
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


    public ProcedureDefinitionWithReturnMutator(Factory factory, BlocklyController controller) {
        super(factory, controller);
    }

    @Override
    public void update(XmlPullParser parser)
            throws BlockLoadingException, IOException, XmlPullParserException {
        super.update(parser);

        // TODO: Reshape mBlock
    }
}
