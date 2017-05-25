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
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.android.control.ProcedureManager;
import com.google.blockly.model.Block;
import com.google.blockly.model.Input;
import com.google.blockly.model.Mutator;
import com.google.blockly.model.ProcedureInfo;
import com.google.blockly.utils.BlockLoadingException;
import com.google.blockly.utils.BlocklyXmlHelper;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for all procedure definition and procedure call mutators, providing a base
 * implementation of the mutation state variables and related I/O.
 */
public abstract class AbstractProcedureMutator<Info extends ProcedureInfo, Builder extends ProcedureInfo.Builder<Info>> extends Mutator {
    private static final String TAG = "AbstractProcedureMutator";

    // Block components.
    public static final String NAME_FIELD = ProcedureManager.NAME_FIELD;

    // Xml strings
    protected static final String ATTR_NAME = "name";
    protected static final String ATTR_STATEMENTS = "statements";
    protected static final String TAG_ARG = "arg";
    protected static final String ATTR_ARG_NAME = "name";

    /**
     * Writes an XML mutation string for the provided values.
     *
     * @param procedureInfo The procedure info to write.
     * @return Serialized XML {@code <mutation>} tag, encoding the values.
     */
    protected String writeMutationString(final Info procedureInfo) {
        try {
            return BlocklyXmlHelper.writeXml(new BlocklyXmlHelper.XmlContentWriter() {
                @Override
                public void write(XmlSerializer serializer) throws IOException {
                    serializeImpl(serializer, procedureInfo);
                }
            });
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write mutation string.", e);
        }
    }

    protected final BlocklyController mController;
    protected final ProcedureManager mProcedureManager;
    protected Info mProcedureInfo = null;

    protected AbstractProcedureMutator(Mutator.Factory factory, BlocklyController controller) {
        super(factory);
        mController = controller;
        mProcedureManager = mController.getWorkspace().getProcedureManager();
    }

    /**
     * @return The procedure name associated with this mutator. May be null if not attached to a
     *         block.
     */
    @Nullable
    public String getProcedureName() {
        return mProcedureInfo.getProcedureName();
    }

    @NonNull
    public final List<String> getArgumentList() {
        return mProcedureInfo.getArguments();
    }

    @Override
    public void update(XmlPullParser parser)
            throws BlockLoadingException, IOException, XmlPullParserException {
        parseMutationXml(parser);  // Includes validation
        updateBlock();
    }

    protected void mutateImpl(Info procedureInfo) {
        mProcedureInfo = procedureInfo;
    }

    @Override
    protected void onAttached(Block block) {
        updateBlock();
    }

    protected Info parseMutationXml(XmlPullParser parser)
            throws BlockLoadingException, IOException, XmlPullParserException {
        List<String> argNames = new ArrayList<>();
        String procedureName = getProcedureName();
        boolean hasStatementInput = true;

        int tokenType = parser.next();
        if (tokenType != XmlPullParser.END_DOCUMENT) {
            parser.require(XmlPullParser.START_TAG, null, TAG_MUTATION);

            String attrValue = parser.getAttributeValue(null, ATTR_NAME);
            if (!TextUtils.isEmpty(attrValue)) {
                procedureName = attrValue;
            }

            attrValue = parser.getAttributeValue(null, ATTR_STATEMENTS);
            if (!TextUtils.isEmpty(attrValue)) {
                hasStatementInput = Boolean.getBoolean(attrValue);
            }

            tokenType = parser.next();
            while (tokenType != XmlPullParser.END_DOCUMENT) {
                if (tokenType == XmlPullParser.START_TAG) {
                    parser.require(XmlPullParser.START_TAG, null, TAG_ARG);
                    String argName = parser.getAttributeValue(null, ATTR_ARG_NAME);
                    if (argName == null) {
                        throw new BlockLoadingException(
                                "Function argument #" + argNames.size() + " missing name.");
                    }
                    argNames.add(argName);
                } else if (tokenType == XmlPullParser.END_TAG
                        && parser.getName().equals(TAG_MUTATION)) {
                    break;
                }
                tokenType = parser.next();
            }
        }

        return createValidatedProcedureInfo(
                procedureName, argNames, hasStatementInput).build();
    }

    /**
     * Creates a new ProcedureInfo object, checking that it is possible to apply the provided
     * mutation state on the current block. For example, arguments are valid variables and inputs
     * that will disappear are not connected.
     *
     * @param argNames The proposed names of the procedure's arguments.
     * @param hasStatementInput Whether the procedure definition has an input for statement blocks.
     *                          Very rarely false for anything other than a
     *                          {@code procedures_defreturn} blocks.
     * @return A new {@link ProcedureInfo} object.
     * @throws BlockLoadingException If parameters are not valid in any way.
     */
    protected abstract Builder createValidatedProcedureInfo(
            String procedureName, List<String> argNames, boolean hasStatementInput)
            throws BlockLoadingException;

    /**
     * Applies the mutation to {@code mBlock}.
     */
    protected void updateBlock() {
        mBlock.reshape(buildUpdatedInputs());
    }

    protected abstract List<Input> buildUpdatedInputs();

    protected abstract void serializeImpl(XmlSerializer serializer, Info info)
            throws IOException;
}
