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

import android.support.annotation.Nullable;

import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.android.control.ProcedureManager;
import com.google.blockly.model.Block;
import com.google.blockly.model.Field;
import com.google.blockly.model.FieldInput;
import com.google.blockly.model.FieldLabel;
import com.google.blockly.model.Input;
import com.google.blockly.model.Mutator;
import com.google.blockly.utils.BlockLoadingException;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
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

    public static final String STATEMENT_INPUT_NAME = "STACK";
    public static final String RETURN_INPUT_NAME = "RETURN";

    private Field.Observer mFieldObserver = null;

    ProcedureDefinitionMutator(Mutator.Factory factory,
                               BlocklyController controller) {
        super(factory, controller);
    }

    /**
     * @return The procedure name associated with this mutator. Will be null if not attached to a
     *         block.
     */
    @Override
    public @Nullable String getProcedureName() {
        return mBlock == null ? null : ((FieldInput) mBlock.getFieldByName(NAME_FIELD)).getText();
    }

    /**
     * Fires the block event to change which procedure this block refers to. This does not change
     * other blocks, or coordinate on naming. To rename a procedure for all blocks of the workspace,
     * use the {@link ProcedureManager}.
     * @param procName The new procedure name to reference.
     */
    @Override
    public void setProcedureName(final String procName) {
        mController.groupAndFireEvents(new Runnable() {
            @Override
            public void run() {
                ((FieldInput) mBlock.getFieldByName(NAME_FIELD)).setText(procName);
            }
        });
    }

    /**
     * @return Whether the function is allow to have an input.
     */
    public boolean hasStatementInput() {
        return mHasStatementInput;
    }

    /**
     * Convenience method for invoking a mutation event programmatically, updating the Mutator with
     * the provided values.
     *
     * @param argNames The names of the procedure's arguments.
     * @param hasStatementInput Whether the procedure definition has an input for statement blocks.
     *                          Very rarely false for anything other than a
     *                          {@code procedures_defreturn} blocks.
     */
    public void mutate(List<String> argNames, boolean hasStatementInput) {
        if (mBlock != null) {
            String mutation = writeMutationString(null, argNames, hasStatementInput);
            try {
                mBlock.setMutation(mutation);
            } catch (BlockLoadingException e) {
                throw new IllegalStateException("Failed to update from new mutation XML.", e);
            }
        } else {
            mHasStatementInput = hasStatementInput;
            mArguments.clear();
            mArguments.addAll(argNames);
        }
    }

    @Override
    public void serialize(XmlSerializer serializer) throws IOException {
        serializeImpl(serializer, null, mArguments, mHasStatementInput);
    }

    @Override
    protected void onAttached(final Block block) {
        super.onAttached(block);

        mProcedureName = getProcedureName();  // Looked up from block field.
        mFieldObserver = new Field.Observer() {
            @Override
            public void onValueChanged(Field field, String oldValue, String newValue) {
                mProcedureName = newValue;
            }
        };
        mBlock.getFieldByName(NAME_FIELD).registerObserver(mFieldObserver);
    }

    @Override
    protected void onDetached(Block block) {
        if (mFieldObserver != null) {
            FieldInput nameField = (FieldInput) block.getFieldByName(NAME_FIELD);
            nameField.unregisterObserver(mFieldObserver);
        }
        super.onDetached(block);
    }

    @Override
    protected void validateBlockForReshape(
            String procedureName, List<String> argNames, boolean hasStatementInput)
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
