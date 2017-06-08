package com.google.blockly.model;

import android.text.TextUtils;

import com.google.blockly.android.control.ProcedureManager;
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

    // Xml strings
    protected static final String ATTR_NAME = "name";
    protected static final String ATTR_STATEMENTS = "statements";
    protected static final String TAG_ARG = "arg";
    protected static final String ATTR_ARG_NAME = "name";


    final String mName;
    final List<String> mArguments;
    final boolean mDefinitionHasStatementBody;

    public ProcedureInfo(String name,
                         List<String> arguments,
                         boolean definitionHasStatements) {
        mName = name;
        mArguments = Collections.unmodifiableList(new ArrayList<>(arguments));
        mDefinitionHasStatementBody = definitionHasStatements;
    }

    public ProcedureInfo cloneWithName(String newProcedureName) {
        return new ProcedureInfo(newProcedureName, mArguments, mDefinitionHasStatementBody);
    }

    public String getProcedureName() {
        return mName;
    }

    public List<String> getArguments() {
        return mArguments;
    }
    
    public boolean getDefinitionHasStatementBody() {
        return mDefinitionHasStatementBody;
    }

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
        for (String argName : info.getArguments()) {
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
