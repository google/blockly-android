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

import com.google.blockly.android.R;
import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.android.control.ProcedureManager;
import com.google.blockly.model.Block;
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
 * This mutator class supports procedure definition blocks for user-defined procedures
 * ({@code procedures_defreturn} and {@code procedures_defnoreturn} blocks).
 */
public class ProcedureDefinitionMutator extends AbstractProcedureMutator<ProcedureInfo> {

    public static final String DEFNORETURN_MUTATOR_ID = "procedures_defnoreturn_mutator";
    public static final String DEFRETURN_MUTATOR_ID = "procedures_defreturn_mutator";

    public static final Mutator.Factory<ProcedureDefinitionMutator> DEFNORETURN_FACTORY =
            new Factory(DEFNORETURN_MUTATOR_ID);
    public static final Mutator.Factory<ProcedureDefinitionMutator> DEFRETURN_FACTORY =
            new Factory(DEFRETURN_MUTATOR_ID);

    public static final String NAME_FIELD_NAME = ProcedureManager.NAME_FIELD;
    public static final String STATEMENT_INPUT_NAME = "STACK";
    public static final String RETURN_INPUT_NAME = "RETURN";

    private final String mBeforeParams;

    private Field.Observer mFieldObserver = null;
    private boolean mUpdatingBlock = false;

    /**
     * Constructs a new procedure definition mutator. The same mutator definition is used for both
     * {@code procedures_defnoreturn_mutator} and {@code procedures_defreturn_mutator}, the latter
     * used on blocks with an extra {@link com.google.blockly.model.Input.InputValue} for the return
     * value.
     *
     * @param factory The factory object which constructed this mutator
     * @param controller The BlocklyController for this Activity.
     */
    ProcedureDefinitionMutator(Mutator.Factory factory,
                               BlocklyController controller) {
        super(factory, controller);
        mBeforeParams = controller.getContext().getString(
                R.string.mutator_procedure_def_before_params);  // BKY_PROCEDURES_BEFORE_PARAMS
    }

    /**
     * Sets the mutator name, including setting the associated name field on the block.
     * @param newName
     */
    @Override
    protected void setProcedureNameImpl(final String newName) {
        mProcedureInfo = mProcedureInfo.cloneWithName(newName);
        mController.groupAndFireEvents(new Runnable() {
            @Override
            public void run() {
                ((FieldInput) mBlock.getFieldByName(NAME_FIELD_NAME)).setText(newName);
            }
        });
    }

    /**
     * @return Whether the function is allow to have an input.
     */
    public final boolean hasStatementInput() {
        return mProcedureInfo.getDefinitionHasStatementBody();
    }

    /**
     * Convenience method for invoking a mutation event programmatically, updating the Mutator with
     * the provided values.
     *
     * @param newProcedureInfo The new values that define this procedure.
     */
    public void mutate(ProcedureInfo newProcedureInfo) {
        if (mBlock != null) {
            try {
                String mutation = writeMutationString(newProcedureInfo);
                mBlock.setMutation(mutation);
            } catch (BlockLoadingException e) {
                throw new IllegalStateException("Failed to update from new mutation XML.", e);
            }
        } else {
            mProcedureInfo = newProcedureInfo;
        }
    }

    /**
     * Parses the provided XML into a ProcedureInfo that can parameterize this procedure.
     * @param parser The XML parser containing the &lt;mutation&gt; tag.
     * @return A new procedure info object.
     * @throws IOException If the stream backing the XML parser has a failure.
     * @throws XmlPullParserException If the stream is not valid XML.
     * @throws BlockLoadingException If the XML does not contain a proper procedure mutation.
     */
    @Override
    public ProcedureInfo parseAndValidateMutationXml(XmlPullParser parser)
            throws BlockLoadingException, IOException, XmlPullParserException {
        ProcedureInfo xmlInfo = ProcedureInfo.parseImpl(parser);
        FieldInput nameField = getNameField();
        if (TextUtils.isEmpty(xmlInfo.getProcedureName()) && nameField != null) {
            // Use the name on the field when not specified in the info.
            return new ProcedureInfo(
                    nameField.getText(),
                    xmlInfo.getArgumentNames(),
                    xmlInfo.getDefinitionHasStatementBody());
        }
        return xmlInfo;
    }

    /**
     * This outputs the mutation via the {@code serializer}.
     * @param serializer The XML serializer
     * @param info The procedure info to serialize.
     * @throws IOException If the backing output stream encounters an issue.
     */
    @Override
    public void serializeInfo(XmlSerializer serializer, ProcedureInfo info) throws IOException {
        ProcedureInfo.serialize(serializer, info, true);
    }

    /**
     * Called when the mutator is attached to a block. It will make sure the procedure name on the
     * block's name field is in sync with the mutator's PRocedureInfo, and register a listener on
     * the name field for future edits.
     * @param block The block the mutator is attached to.
     */
    @Override
    protected void onAttached(final Block block) {
        String procedureName = null;

        // Update the ProcedureInfo with procedure name from NAME field.
        // In the case of this class, this will not update the mutation
        // serialization, but initializes the value to synch with caller's
        // ProcedureInfo.
        Field field = mBlock.getFieldByName(NAME_FIELD_NAME);
        FieldInput nameField = (field instanceof FieldInput) ? (FieldInput)field : null;
        if (nameField != null) {
            String blockProcName = nameField.getText();
            String infoProcName =
                    (mProcedureInfo == null) ? null : mProcedureInfo.getProcedureName();

            if (!TextUtils.isEmpty(blockProcName) && !blockProcName.equals(infoProcName)) {
                if (!TextUtils.isEmpty(infoProcName)) {
                    throw new IllegalStateException(
                            "Attached to block that already has a differing procedure name.");
                }

                procedureName = blockProcName;
            } else {
                procedureName = infoProcName;
            }
        }
        if (mProcedureInfo == null) {
            mProcedureInfo = new ProcedureInfo(
                    procedureName,
                    Collections.<String>emptyList(),
                    ProcedureInfo.HAS_STATEMENTS_DEFAULT);
        } else {
            mProcedureInfo = new ProcedureInfo(
                    procedureName,
                    mProcedureInfo.getArgumentNames(),
                    mProcedureInfo.getDefinitionHasStatementBody());
        }

        super.onAttached(block);

        if (nameField != null) {
            nameField.setText(procedureName);
            mFieldObserver = new Field.Observer() {
                @Override
                public void onValueChanged(Field field, String oldValue, String newValue) {
                    if (!mUpdatingBlock) {
                        String oldProcedureName = getProcedureName();
                        ProcedureInfo newInfo = new ProcedureInfo(
                                newValue,
                                mProcedureInfo.getArgumentNames(),
                                mProcedureInfo.getDefinitionHasStatementBody());
                        if (oldProcedureName != null
                                && mProcedureManager.containsDefinition(oldProcedureName)) {
                            mProcedureManager.mutateProcedure(mBlock, newInfo);
                        } else {
                            mProcedureInfo = newInfo;
                        }
                    }
                }
            };
            nameField.registerObserver(mFieldObserver);
        }
    }

    /**
     * Unregisters the field listener when the mutator is detached from the block.
     * @param block The block the mutator was formerly attached to.
     */
    @Override
    protected void onDetached(Block block) {
        if (mFieldObserver != null) {
            FieldInput nameField = (FieldInput) block.getFieldByName(NAME_FIELD_NAME);
            nameField.unregisterObserver(mFieldObserver);
        }
        super.onDetached(block);
    }

    /**
     * Constructs the block's header {@link Input}. The new header maintains the same name field
     * instance, but updated {@code PARAMS} argument list.
     * @return Return a new header {@link Input} reflecting the latest mutator state.
     */
    protected Input newDefinitionHeader() {
        Input descriptionInput = mBlock.getInputs().get(0);
        List<Field> oldFields = descriptionInput.getFields();
        List<Field> newFields = Arrays.asList(
                oldFields.get(0),
                oldFields.get(1),
                new FieldLabel("PARAMS", getParametersListDescription()));
        return new Input.InputDummy(null, newFields, Input.ALIGN_LEFT);
    }

    /**
     * @return An {@link Input} to contain the procedure body statements.
     */
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

    /**
     * Updates {@link #mBlock} in response to a mutation, including updating the name field.
     */
    @Override
    protected void updateBlock() {
        try {
            mUpdatingBlock = true;
            super.updateBlock();

            FieldInput nameField = getNameField();
            if (mProcedureInfo != null && nameField != null) {
                nameField.setFromString(mProcedureInfo.getProcedureName());
            }
        } finally {
            mUpdatingBlock = false;
        }
    }

    /**
     * @return A list of Inputs for a block mutation.
     */
    @Override
    protected List<Input> buildUpdatedInputs() {
        List<Input> newInputs = new ArrayList<>();
        newInputs.add(newDefinitionHeader());
        if (mProcedureInfo.getDefinitionHasStatementBody()) {
            newInputs.add(getDefintionStatementsInput());
        }

        // For procedures_defreturn_mutator
        Input returnInput = mBlock.getInputByName(RETURN_INPUT_NAME);
        if (returnInput != null) {
            newInputs.add(returnInput);
        }
        return newInputs;
    }

    /**
     * @return A human-readable string describing the procedure's parameters.
     */
    protected String getParametersListDescription() {
        if (mProcedureInfo == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        List<String> arguments = mProcedureInfo.getArgumentNames();
        if (!arguments.isEmpty()) {
            sb.append(mBeforeParams);

            int count = arguments.size();
            for (int i = 0; i < count; ++i) {
                if (i == 0) {
                    sb.append(' ');
                } else {
                    sb.append(", ");
                }
                sb.append(arguments.get(i));
            }
        }
        return sb.toString();
    }

    private FieldInput getNameField() {
        Field field = (mBlock == null) ? null : mBlock.getFieldByName(NAME_FIELD_NAME);
        if (field instanceof FieldInput) {
            return (FieldInput) field;
        }
        return null;
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
