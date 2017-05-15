package com.google.blockly.model.mutator;

import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.model.Field;
import com.google.blockly.model.FieldInput;
import com.google.blockly.model.FieldLabel;
import com.google.blockly.model.Input;
import com.google.blockly.model.Mutator;
import com.google.blockly.utils.BlockLoadingException;

import java.util.Arrays;
import java.util.List;

/**
 * This mutator base class supports procedure definition blocks for user-defined procedures
 * ({@code procedures_defreturn} and {@code procedures_defnoreturn} blocks).
 */
abstract class ProcedureDefinitionMutator extends AbstractProcedureMutator {
    private static final String STATEMENT_INPUT_NAME = "STACK";

    ProcedureDefinitionMutator(Mutator.Factory factory, BlocklyController controller) {
        super(factory, controller);
    }

    @Override
    protected void validateBlockForReshape(List<String> argNames, boolean hasStatementInput)
            throws BlockLoadingException {
        if (mHasStatementInput && !hasStatementInput) {
            Input stack = mBlock.getInputByName(STATEMENT_INPUT_NAME);
            if (stack.getConnectedBlock() != null) {
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
}
