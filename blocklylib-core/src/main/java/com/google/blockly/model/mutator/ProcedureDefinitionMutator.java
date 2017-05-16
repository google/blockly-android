package com.google.blockly.model.mutator;

import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.model.Field;
import com.google.blockly.model.FieldLabel;
import com.google.blockly.model.Input;
import com.google.blockly.model.Mutator;
import com.google.blockly.utils.BlockLoadingException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This mutator base class supports procedure definition blocks for user-defined procedures
 * ({@code procedures_defreturn} and {@code procedures_defnoreturn} blocks).
 */
public class ProcedureDefinitionMutator extends AbstractProcedureMutator {
    public static final String DEFNORETURN_MUTATOR_ID = "procedures_defnoreturn_mutator";
    public static final String DEFRETURN_MUTATOR_ID = "procedures_defreturn_mutator";

    public static final Mutator.Factory<ProcedureDefinitionMutator> DEFNORETURN_FACTORY =
            new Factory(DEFNORETURN_MUTATOR_ID);
    public static final Mutator.Factory<ProcedureDefinitionMutator> DEFRETURN_FACTORY =
            new Factory(DEFRETURN_MUTATOR_ID);

    private static final String STATEMENT_INPUT_NAME = "STACK";
    private static final String RETURN_INPUT_NAME = "RETURN";

    ProcedureDefinitionMutator(Mutator.Factory factory, BlocklyController controller) {
        super(factory, controller);
    }

    @Override
    protected void validateBlockForReshape(List<String> argNames, boolean hasStatementInput)
            throws BlockLoadingException {
        if (mHasStatementInput && !hasStatementInput) {
            Input stack = mBlock.getInputByName(STATEMENT_INPUT_NAME);
            if (stack != null && stack.getConnectedBlock() != null) {
                throw new BlockLoadingException(
                        "Cannot remove statement input while statements are connected.");
            }
        }
    }

    protected Input newDefinitionHeader() {
        Input descriptionInput = mBlock.getInputs().get(0);
        List<Field> oldFields = descriptionInput.getFields();
        List<Field> newFields = Arrays.asList(
                oldFields.get(0),
                oldFields.get(1),
                new FieldLabel("PARAMS", getArgumentListDescription()));
        return new Input.InputDummy(null, newFields, Input.ALIGN_LEFT);
    }

    protected Input getDefintionStatementsInput() {
        Input stackInput = mBlock.getInputByName(STATEMENT_INPUT_NAME);
        if (stackInput == null) {
            stackInput = new Input.InputStatement(STATEMENT_INPUT_NAME,
                    // Placeholder for message BKY_PROCEDURES_DEF[NO]RETURN_DO
                    Arrays.<Field>asList(new FieldLabel(null, "")),
                    Input.ALIGN_LEFT, null);
        }
        return stackInput;
    }

    @Override
    protected List<Input> buildUpdatedInputs() {
        List<Input> newInputs = new ArrayList<>();
        newInputs.add(newDefinitionHeader());
        if (mHasStatementInput) {
            newInputs.add(getDefintionStatementsInput());
        }

        // For procedures_defreturn_mutator
        Input returnInput = mBlock.getInputByName(RETURN_INPUT_NAME);
        if (returnInput != null) {
            newInputs.add(returnInput);
        }
        return newInputs;
    }

    private static class Factory implements Mutator.Factory<ProcedureDefinitionMutator> {
        final String mMutatorId;
        Factory(String mutatorId) {
            mMutatorId = mutatorId;
        }

        @Override
        public ProcedureDefinitionMutator newMutator(BlocklyController controller) {
            return new ProcedureDefinitionMutator(this, controller);
        }

        @Override
        public String getMutatorId() {
            return mMutatorId;
        }
    }
}
