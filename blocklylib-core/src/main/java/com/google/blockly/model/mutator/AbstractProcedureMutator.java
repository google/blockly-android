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
import com.google.blockly.utils.BlockLoadingException;
import com.google.blockly.utils.BlocklyXmlHelper;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for all procedure definition and procedure call mutators, providing a base
 * implementation of the mutation state variables and related I/O.
 */
public abstract class AbstractProcedureMutator extends Mutator {
    private static final String TAG = "AbstractProcedureMutator";

    // Block components.
    protected static final String NAME_FIELD = "name";

    // Xml strings
    protected static final String ATTR_NAME = "name";
    protected static final String ATTR_STATEMENTS = "statements";
    protected static final String TAG_ARG = "arg";
    protected static final String ATTR_ARG_NAME = "name";

    /**
     * Writes an XML mutation string for the provided values.
     *
     * @param optProcedureName The name of the procedure
     * @param argNames The names of the procedure's arguments.
     * @param optHasStatementInput Whether the procedure definition has an input for statement
     *                             blocks. Very rarely false for anything other than a
     *                             {@code procedures_defreturn} blocks.
     * @return Serialized XML {@code <mutation>} tag, encoding the values.
     */
    protected static String writeMutationString(final @Nullable String optProcedureName,
                                                final List<String> argNames,
                                                final @Nullable Boolean optHasStatementInput) {
        try {
            return BlocklyXmlHelper.writeXml(new BlocklyXmlHelper.XmlContentWriter() {
                @Override
                public void write(XmlSerializer serializer) throws IOException {
                    serializeImpl(serializer, optProcedureName, argNames, optHasStatementInput);
                }
            });
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write mutation string.", e);
        }
    }

    protected final BlocklyController mController;
    protected final ProcedureManager mProcedureManager;

    protected String mProcedureName = null;
    protected List<String> mArguments = Collections.emptyList();
    protected boolean mHasStatementInput = true;

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
        return mProcedureName;
    }

    public abstract void setProcedureName(String procName);

    @NonNull
    public List<String> getArgumentList() {
        return Collections.unmodifiableList(mArguments);
    }

    @Override
    public void update(XmlPullParser parser)
            throws BlockLoadingException, IOException, XmlPullParserException {
        parseMutationXml(parser);  // Includes validation
        updateBlock();
    }

    @Override
    protected void onAttached(Block block) {
        updateBlock();
    }

    protected void parseMutationXml(XmlPullParser parser)
            throws BlockLoadingException, IOException, XmlPullParserException {
        List<String> argNames = new ArrayList<>();
        String procedureName = mProcedureName;
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
        validateBlockForReshape(procedureName, argNames, hasStatementInput);
        mProcedureName = procedureName;
        mArguments = argNames;
        mHasStatementInput = hasStatementInput;
    }

    /**
     * Checks that it is possible to apply the provided mutation state on the current block.
     * For example, inputs that will disappear are not connected.
     *
     * @param argNames The proposed names of the procedure's arguments.
     * @param hasStatementInput Whether the procedure definition has an input for statement blocks.
     *                          Very rarely false for anything other than a
     *                          {@code procedures_defreturn} blocks.
     */
    protected abstract void validateBlockForReshape(
            String procedureName, List<String> argNames, boolean hasStatementInput)
            throws BlockLoadingException;

    /**
     * Applies the mutation to {@code mBlock}.
     */
    protected void updateBlock() {
        mBlock.reshape(buildUpdatedInputs());
    }

    protected abstract List<Input> buildUpdatedInputs();

    protected String getArgumentListDescription() {
        StringBuilder sb = new StringBuilder();
        if (!mArguments.isEmpty()) {
            sb.append("with:"); // message BKY_PROCEDURES_BEFORE_PARAMS

            int count = mArguments.size();
            for (int i = 0; i < count; ++i) {
                if (i == 0) {
                    sb.append(' ');
                } else {
                    sb.append(", ");
                }
                sb.append(mArguments.get(i));
            }
        }
        return sb.toString();
    }

    static void serializeImpl(XmlSerializer serializer,
                              @Nullable String optProcedureName,
                              List<String> argNames,
                              @Nullable Boolean optHasStatementInput)
            throws IOException {
        serializer.startTag(null, TAG_MUTATION);
        if (optProcedureName != null) {
            serializer.attribute(null, ATTR_ARG_NAME, optProcedureName);
        }
        if (optHasStatementInput != null && !optHasStatementInput) {
            serializer.attribute(null, ATTR_STATEMENTS, "false");
        }
        for (String argName : argNames) {
            serializer.startTag(null, TAG_ARG)
                    .attribute(null, ATTR_ARG_NAME, argName)
                    .endTag(null, TAG_ARG);
        }
        serializer.endTag(null, TAG_MUTATION);
    }
}
