package com.google.blockly.model.mutator;

import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.model.Field;
import com.google.blockly.model.FieldLabel;
import com.google.blockly.model.Input;
import com.google.blockly.model.Mutator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This mutator supports procedure call blocks for user-defined procedures
 * ({@code procedures_callreturn} and {@code procedures_callnoreturn} blocks).
 */
abstract class ProcedureCallMutator extends AbstractProcedureMutator {
    ProcedureCallMutator(Mutator.Factory factory, BlocklyController controller) {
        super(factory, controller);
    }

    @Override
    protected void updateBlock() {
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

        mBlock.reshape(inputs);
    }
}
