package com.google.blockly.model.mutator;

import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.model.Mutator;
import com.google.blockly.utils.BlockLoadingException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * This mutator supports user-defined function definitions that do not return a value
 * ({@code procedures_defnoreturn} blocks).
 */
public class ProcedureDefinitionNoReturnMutator extends AbstractProcedureMutator {
    public static final String MUTATOR_ID = "procedures_defnoreturn_mutator";
    public static final Mutator.Factory<ProcedureDefinitionNoReturnMutator> FACTORY =
            new Mutator.Factory<ProcedureDefinitionNoReturnMutator>() {
                @Override
                public String getMutatorId() {
                    return MUTATOR_ID;
                }

                @Override
                public ProcedureDefinitionNoReturnMutator newMutator(BlocklyController controller) {
                    return new ProcedureDefinitionNoReturnMutator(this, controller);
                }
            };


    public ProcedureDefinitionNoReturnMutator(Mutator.Factory factory, BlocklyController controller) {
        super(factory, controller);
    }

    @Override
    public void update(XmlPullParser parser)
            throws BlockLoadingException, IOException, XmlPullParserException {
        super.update(parser);

        // TODO: Reshape mBlock
    }
}
