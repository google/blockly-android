/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.blockly.model.mutator;

import android.text.TextUtils;

import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.android.control.ProcedureManager;
import com.google.blockly.model.Field;
import com.google.blockly.model.FieldLabel;
import com.google.blockly.model.Input;
import com.google.blockly.model.Mutator;
import com.google.blockly.model.ProcedureInfo;
import com.google.blockly.utils.BlockLoadingException;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
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
    public void setProcedureName(final String procName) {
        mController.groupAndFireEvents(new Runnable() {
            @Override
            public void run() {
                mutate(procName, mArguments);
            }
        });
    }

    public Input getArgumentInput(int index) {
        return mBlock.getInputs().get(index + 1); // Skip the top row (procedure name)
    }

    /**
     * Convenience method for invoking a mutation event programmatically, updating the Mutator with
     * the provided values.
     *
     * @param procedureName The name of the procedure.
     * @param argNames The names of the procedure's arguments.
     */
    public void mutate(String procedureName, List<String> argNames) {
        if (mBlock != null) {
            String mutation = writeMutationString(procedureName, argNames, null);
            try {
                mBlock.setMutation(mutation);
            } catch (BlockLoadingException e) {
                throw new IllegalStateException("Failed to update from new mutation XML.", e);
            }
        } else {
            mProcedureName = procedureName;
            mArguments.clear();
            mArguments.addAll(argNames);
        }
    }

    @Override
    public void serialize(XmlSerializer serializer) throws IOException {
        serializeImpl(serializer, mProcedureName, mArguments, null);
    }

    @Override
    protected void updateBlock() {
        super.updateBlock();

        FieldLabel nameField =
                (FieldLabel) mBlock.getFieldByName(ProcedureManager.PROCEDURE_NAME_FIELD);
        nameField.setText(mProcedureName);
    }

    @Override
    protected void validateBlockForReshape(
            String procedureName, List<String> argNames, boolean hasStatementInput)
            throws BlockLoadingException {
        if (TextUtils.isEmpty(procedureName)) {
            throw new BlockLoadingException("Procedure name must not be empty or missing.");
        }

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
        Input topRow = mBlock.getInputs().get(0);
        inputs.add(topRow);  // First row does not change shape.

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

    @Override
    protected void serializeImpl(XmlSerializer serializer, ProcedureInfo info)
            throws IOException {
        serializer.startTag(null, TAG_MUTATION);
        serializer.attribute(null, ATTR_ARG_NAME, info.getProcedureName());
        for (String argName : info.getArguments()) {
            serializer.startTag(null, TAG_ARG)
                    .attribute(null, ATTR_ARG_NAME, argName)
                    .endTag(null, TAG_ARG);
        }
        serializer.endTag(null, TAG_MUTATION);
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
