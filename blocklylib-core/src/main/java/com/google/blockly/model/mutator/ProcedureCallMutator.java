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
import com.google.blockly.model.FieldInput;
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
import java.util.Arrays;
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

    ProcedureCallMutator(Mutator.Factory factory, BlocklyController controller) {
        super(factory, controller);
    }

    public Input getArgumentInput(int index) {
        return mBlock.getInputs().get(index + 1); // Skip the top row (procedure name)
    }

    /**
     * Convenience method for invoking a mutation event programmatically, updating the Mutator with
     * the provided values.
     *
     * @param procedureInfo The procedure info
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

    @Override
    protected void setProcedureNameImpl(final String newName) {
        mProcedureInfo = mProcedureInfo.cloneWithName(newName);
        ((FieldLabel) mBlock.getFieldByName(NAME_FIELD_NAME)).setText(newName);
    }

    @Override
    protected void updateBlock() {
        super.updateBlock();

        if (mProcedureInfo != null) {  // May be null before <mutation> applied.
            FieldLabel nameLabel =
                    (FieldLabel) mBlock.getFieldByName(ProcedureManager.PROCEDURE_NAME_FIELD);
            nameLabel.setText(mProcedureInfo.getProcedureName());
        }
    }

    @Override
    protected List<Input> buildUpdatedInputs() {
        List<String> arguments = mProcedureInfo.getArguments();
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

    protected ProcedureInfo parseAndValidateMutationXml(XmlPullParser parser)
            throws BlockLoadingException, IOException, XmlPullParserException {
        return ProcedureInfo.parseImpl(parser);
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
