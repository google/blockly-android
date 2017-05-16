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
 * This mutator supports procedure call blocks for user-defined procedures
 * ({@code procedures_callreturn} and {@code procedures_callnoreturn} blocks).
 */
public class ProcedureCallMutator extends AbstractProcedureMutator {
    public static final String CALLNORETURN_MUTATOR_ID = "procedures_callnoreturn_mutator";
    public static final String CALLRETURN_MUTATOR_ID = "procedures_callreturn_mutator";

    public static final Mutator.Factory<ProcedureCallMutator> CALLNORETURN_FACTORY =
            new Factory(CALLNORETURN_MUTATOR_ID);
    public static final Mutator.Factory<ProcedureCallMutator> CALLRETURN_FACTORY =
            new Factory(CALLRETURN_MUTATOR_ID);

    ProcedureCallMutator(Mutator.Factory factory, BlocklyController controller) {
        super(factory, controller);
    }

    @Override
    protected void validateBlockForReshape(List<String> argNames, boolean hasStatementInput)
            throws BlockLoadingException {
        List<Input> inputs = mBlock.getInputs();
        int oldCount = mArguments.size();
        int newCount = argNames.size();
        if (oldCount > newCount) {
            for (int i = newCount; i < oldCount; ++i) {
                Input input = inputs.get(i);
                if (input.getConnectedBlock() != null) {
                    throw new BlockLoadingException(
                            "Cannot remove connected argument input \"" + mArguments.get(i) +
                            "\" " + "(ARG" + i + ")");
                }
            }
        }
        int sameCount = Math.min(oldCount, newCount);
        for (int i = 0; i < sameCount; ++i) {
            Input input = inputs.get(i);
            if (input.getConnectedBlock() != null
                && !mArguments.get(i).equals(argNames.get(i))) {
                throw new BlockLoadingException(
                        "Cannot rename ARG" + i + " while connected (" + mArguments.get(i) +
                        " => " + argNames.get(i) + ")");
            }
        }
    }

    @Override
    protected List<Input> buildUpdatedInputs() {
        final int argCount = mArguments.size();
        List<Input> inputs = new ArrayList<>(argCount + 1);

        // Header (TOPROW)
        inputs.add(mBlock.getInputs().get(0));  // First row does not change shape.

        // Argument inputs
        for (int i = 0; i < argCount; ++i) {
            FieldLabel label = new FieldLabel(null, mArguments.get(i));
            inputs.add(new Input.InputValue("ARG" + i,
                    Arrays.<Field>asList(label),
                    Input.ALIGN_RIGHT,
                    null));
        }

        return inputs;
    }

    private static class Factory implements Mutator.Factory<ProcedureCallMutator> {
        final String mMutatorId;
        Factory(String mutatorId) {
            mMutatorId = mutatorId;
        }

        @Override
        public ProcedureCallMutator newMutator(BlocklyController controller) {
            return new ProcedureCallMutator(this, controller);
        }

        @Override
        public String getMutatorId() {
            return mMutatorId;
        }
    }
}
