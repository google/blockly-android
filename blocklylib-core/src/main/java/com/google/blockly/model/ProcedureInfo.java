package com.google.blockly.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.blockly.android.control.ProcedureManager;
import com.google.blockly.model.Input.InputStatement;  // For comment {@link}
import com.google.blockly.utils.BlockLoadingException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Describes a procedure for the {@link ProcedureManager} and procedure mutators.
 */
public class ProcedureInfo {
    public static final boolean HAS_STATEMENTS_DEFAULT = true;

    // XML strings
    protected static final String ATTR_NAME = "name";
    protected static final String ATTR_STATEMENTS = "statements";
    protected static final String TAG_ARG = "arg";
    protected static final String ATTR_ARG_NAME = "name";


    final String mName;
    final List<String> mArguments;
    final boolean mDefinitionHasStatementBody;

    /**
     * Constructs a new ProcedureInfo with the given arguments.
     * @param name The name of the procedure, or null if not yet defined.
     * @param argumentNames The list of parameter names, possibly empty.
     * @param definitionHasStatements Whether the procedure definition includes
     */
    public ProcedureInfo(@Nullable String name,
                         @NonNull List<String> argumentNames,
                         boolean definitionHasStatements) {
        mName = name;
        mArguments = Collections.unmodifiableList(new ArrayList<>(argumentNames));
        mDefinitionHasStatementBody = definitionHasStatements;
    }

    /**
     * Constructs a new ProcedureInfo with the same parameters, but a new name.
     * @param newProcedureName The name to use on the constructed ProcedureInfo.
     * @return A new ProcedureInfo, reflecting a renamed procedure.
     */
    public ProcedureInfo cloneWithName(String newProcedureName) {
        return new ProcedureInfo(newProcedureName, mArguments, mDefinitionHasStatementBody);
    }

    /**
     * @return The name of the procedure.
     */
    public String getProcedureName() {
        return mName;
    }

    /**
     * @return An ordered list of procedure argument names.
     */
    public List<String> getArgumentNames() {
        return mArguments;
    }

    /**
     * @return True if the procedure's definition should include a {@link Input.InputStatement} for
     *         the procedure body. Otherwise false.
     */
    public boolean getDefinitionHasStatementBody() {
        return mDefinitionHasStatementBody;
    }

    /**
     * Serializes a procedure as a XML &lt;mutation&gt; tag.
     * @param serializer The serailizer to output to.
     * @param info The ProcedureInput to serialize
     * @param asDefinition Whether the output should reflect a procedure definition's mutator, or
     *                     otherwise a calling mutator.
     * @throws IOException If the output stream backing the serializer fails.
     */
    public static void serialize(XmlSerializer serializer,
                                 ProcedureInfo info,
                                 boolean asDefinition)
            throws IOException {
        serializer.startTag("", Mutator.TAG_MUTATION);
        if (asDefinition) {
            if (!info.getDefinitionHasStatementBody()) {
                serializer.attribute("", ATTR_STATEMENTS, "false");
            }
        } else {
            String procName = info.getProcedureName();
            if (procName != null) {
                serializer.attribute("", ATTR_NAME, procName);
            }
        }
        for (String argName : info.getArgumentNames()) {
            serializer.startTag("", TAG_ARG)
                    .attribute("", ATTR_ARG_NAME, argName)
                    .endTag("", TAG_ARG);
        }
        serializer.endTag("", Mutator.TAG_MUTATION);
    }

    public static ProcedureInfo parseImpl(XmlPullParser parser)
            throws BlockLoadingException, IOException, XmlPullParserException {
        List<String> argNames = new ArrayList<>();
        String procedureName = null;
        boolean hasStatementInput = HAS_STATEMENTS_DEFAULT;

        int tokenType = parser.next();
        if (tokenType != XmlPullParser.END_DOCUMENT) {
            parser.require(XmlPullParser.START_TAG, null, Mutator.TAG_MUTATION);

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
                        && parser.getName().equals(Mutator.TAG_MUTATION)) {
                    break;
                }
                tokenType = parser.next();
            }
        }

        return new ProcedureInfo(procedureName, argNames, hasStatementInput);
    }
}
