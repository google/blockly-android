package com.google.blockly.model.mutator;

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
abstract class AbstractProcedureMutator extends Mutator {
    private static final String TAG = "AbstractProcedureMutator";

    // Xml strings
    private static final String ATTR_STATEMENTS = "statements";
    private static final String TAG_ARG = "arg";
    private static final String ATTR_ARG_NAME = "name";

    /**
     * Writes an XML mutation string for the provided values.
     *
     * @param argNames The names of the procedure's arguments.
     * @param hasStatementInput Whether the procedure definition has an input for statement blocks.
     *                          Very rarely false for anything other than a
     *                          {@code procedures_defreturn} blocks.
     * @return Serialized XML {@code <mutation>} tag, encoding the values.
     */
    public static String writeMutationString(
            final List<String> argNames, final boolean hasStatementInput) {
        try {
            return BlocklyXmlHelper.writeXml(new BlocklyXmlHelper.XmlContentWriter() {
                @Override
                public void write(XmlSerializer serializer) throws IOException {
                    serializeImpl(serializer, argNames, hasStatementInput);
                }
            });
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write mutation string.", e);
        }
    }

    protected final BlocklyController mController;
    protected final ProcedureManager mProcedureManager;

    protected List<String> mArguments = Collections.emptyList();
    protected boolean mHasStatementInput = true;

    protected AbstractProcedureMutator(Mutator.Factory factory, BlocklyController controller) {
        super(factory);
        mController = controller;
        mProcedureManager = mController.getWorkspace().getProcedureManager();
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
        String mutation = writeMutationString(argNames, hasStatementInput);
        try {
            mBlock.setMutation(mutation);
        } catch (BlockLoadingException e) {
            throw new IllegalStateException("Failed to update from new mutation XML.", e);
        }
    }

    @Override
    public void serialize(XmlSerializer serializer) throws IOException {
        serializeImpl(serializer, mArguments, mHasStatementInput);
    }

    @Override
    public void update(XmlPullParser parser)
            throws BlockLoadingException, IOException, XmlPullParserException {
        parserMutationXml(parser);  // Includes validation
        updateBlock();
    }

    @Override
    protected void onAttached(Block block) {
        updateBlock();
    }

    protected void parserMutationXml(XmlPullParser parser)
            throws BlockLoadingException, IOException, XmlPullParserException {
        List<String> argNames = new ArrayList<>();
        boolean hasStatementInput = true;

        int tokenType = parser.next();
        if (tokenType != XmlPullParser.END_DOCUMENT) {
            parser.require(XmlPullParser.START_TAG, null, TAG_MUTATION);
            String statementsValue = parser.getAttributeValue(null, ATTR_STATEMENTS);
            if (statementsValue != null) {
                hasStatementInput = Boolean.getBoolean(statementsValue);
            }

            tokenType = parser.next();
            while (tokenType != XmlPullParser.END_TAG) {
                if (tokenType == XmlPullParser.TEXT) {
                    tokenType = parser.next();
                    continue;
                }
                parser.require(XmlPullParser.START_TAG, null, TAG_ARG);
                String argName = parser.getAttributeValue(null, ATTR_ARG_NAME);
                if (argName == null) {
                    throw new BlockLoadingException("Function argument missing name.");
                }
                argNames.add(argName);
            }
        }
        validateBlockForReshape(argNames, hasStatementInput);
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
    protected void validateBlockForReshape(List<String> argNames, boolean hasStatementInput)
            throws BlockLoadingException {
        // No validation, by default.
    }

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

    private static void serializeImpl(
            XmlSerializer serializer, List<String> argNames, boolean hasStatementInput)
            throws IOException {
        serializer.startTag(null, TAG_MUTATION);
        if (!hasStatementInput) {
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
