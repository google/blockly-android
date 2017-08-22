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

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.android.control.ProcedureManager;
import com.google.blockly.model.Field;
import com.google.blockly.model.FieldLabel;
import com.google.blockly.model.Input;
import com.google.blockly.model.Mutator;
import com.google.blockly.model.ProcedureInfo;
import com.google.blockly.utils.BlockLoadingException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This mutator supports procedure call blocks for user-defined procedures
 * ({@code procedures_callreturn} and {@code procedures_callnoreturn} blocks).
 */
public class ProcedureCallMutator extends AbstractProcedureMutator<ProcedureInfo> {
    public static final String CALLNORETURN_MUTATOR_ID = "procedures_callnoreturn_mutator";
    public static final String CALLRETURN_MUTATOR_ID = "procedures_callreturn_mutator";

    public static final String NAME_FIELD_NAME = "NAME";

    public static final Mutator.Factory<ProcedureCallMutator> CALLNORETURN_FACTORY =
            new Factory(CALLNORETURN_MUTATOR_ID);
    public static final Mutator.Factory<ProcedureCallMutator> CALLRETURN_FACTORY =
            new Factory(CALLRETURN_MUTATOR_ID);

    /**
     * Constructs a new procedure call block mutator. This can be a
     * {@code procedures_callnoreturn_mutator} with previous and next connections, or a
     * {@call procedures_callreturn_mutator} with an output connection.
     * @param factory The factory that is constructing this mutator.
     * @param controller The {@link BlocklyController} for this activity.
     */
    ProcedureCallMutator(Mutator.Factory factory, BlocklyController controller) {
        super(factory, controller);
    }

    /**
     * This retrieves the block's {@link Input} that represents the nth {@code index} argument.
     * @param index The index of the argument asked for.
     * @return The {@link com.google.blockly.model.Input.InputValue} corresponding to the argument.
     */
    public Input getArgumentInput(int index) {
        return mBlock.getInputs().get(index + 1); // Skip the top row (procedure name)
    }

    /**
     * Convenience method for invoking a mutation event programmatically, updating the Mutator with
     * the provided values.
     *
     * @param procedureInfo The procedure info for the mutation.
     */
    public void mutate(ProcedureInfo procedureInfo) {
        if (mBlock != null) {
            String mutation = writeMutationString(procedureInfo);
            try {
                mBlock.setMutation(mutation);
            } catch (BlockLoadingException e) {
                throw new IllegalStateException("Failed to update from new mutation XML.", e);
            }
        } else {
            mProcedureInfo = procedureInfo;
        }
    }

    /**
     * Updates the ProcedureInfo with a new name, and updates the name field. This should never be
     * called directly. Use {@link #setProcedureName(String)} or {@link #mutate(ProcedureInfo)}.
     * @param newProcedureName The updated name. Cannot be null.
     */
    @Override
    protected void setProcedureNameImpl(final @NonNull String newProcedureName) {
        mProcedureInfo = mProcedureInfo.cloneWithName(newProcedureName);
        ((FieldLabel) mBlock.getFieldByName(NAME_FIELD_NAME)).setText(newProcedureName);
    }

    /**
     * Updates {@code mBlock} to reflect the current {@link ProcedureInfo}.
     */
    @Override
    protected void updateBlock() {
        super.updateBlock();

        if (mProcedureInfo != null) {  // May be null before <mutation> applied.
            FieldLabel nameLabel =
                    (FieldLabel) mBlock.getFieldByName(ProcedureManager.PROCEDURE_NAME_FIELD);
            if (nameLabel != null) {
                nameLabel.setText(mProcedureInfo.getProcedureName());
            }
        }
    }

    /**
     * @return A new set of {@link Input Inputs} reflecting the current ProcedureInfo state.
     */
    @Override
    protected List<Input> buildUpdatedInputs() {
        List<String> arguments = mProcedureInfo.getArgumentNames();
        final int argCount = arguments.size();
        List<Input> inputs = new ArrayList<>(argCount + 1);

        // Header (TOPROW)
        Input topRow = mBlock.getInputs().get(0);
        inputs.add(topRow);  // First row does not change shape.

        // Argument inputs
        for (int i = 0; i < argCount; ++i) {
            FieldLabel label = new FieldLabel(null, arguments.get(i));
            inputs.add(new Input.InputValue("ARG" + i,
                    Collections.<Field>singletonList(label),
                    Input.ALIGN_RIGHT,
                    null));
        }

        return inputs;
    }

    /**
     * Updates the block using the mutation in the XML.
     *
     * @param parser The parser with the {@code <mutation>} element.
     * @throws IOException If the input stream fails.
     * @throws XmlPullParserException If the input is not valid XML.
     * @throws BlockLoadingException If the input is not a valid procedure mutation, or lacks a
     *                               procedure name.
     */
    protected ProcedureInfo parseAndValidateMutationXml(XmlPullParser parser)
            throws BlockLoadingException, IOException, XmlPullParserException {
        ProcedureInfo info = ProcedureInfo.parseImpl(parser);
        if (info.getProcedureName() == null) {
            throw new BlockLoadingException(
                    "No procedure name specified in mutation for " + mBlock);
        }
        return info;
    }

    @Override
    protected void serializeInfo(XmlSerializer serializer, ProcedureInfo info)
            throws IOException {
        ProcedureInfo.serialize(serializer, info, false);
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
