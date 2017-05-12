package com.google.blockly.model.mutator;

import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.android.control.ProcedureManager;
import com.google.blockly.model.Block;
import com.google.blockly.model.Field;
import com.google.blockly.model.FieldInput;
import com.google.blockly.model.FieldLabel;
import com.google.blockly.model.Mutator;
import com.google.blockly.utils.BlockLoadingException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Base class for all procedure definition and call block mutators, providing a base implementation
 * of the mutation state variables and related I/O.
 */
class AbstractProcedureMutator extends Mutator {
    private static final String MUTATION = "mutation";
    private static final String ARG = "arg";
    private static final String NAME = "name";

    protected final BlocklyController mController;
    protected final ProcedureManager mProcedureManager;

    protected String mProcedureName;
    protected List<String> mArguments = Collections.EMPTY_LIST;
    protected boolean mHasStatements = true;

    protected AbstractProcedureMutator(Mutator.Factory factory, BlocklyController controller) {
        super(factory);
        mController = controller;
        mProcedureManager = mController.getWorkspace().getProcedureManager();
    }

    @Override
    public void serialize(XmlSerializer serializer) throws IOException {
        serializer.startTag(null, MUTATION);
        if (!mHasStatements) {
            serializer.attribute(null, "statements", "false");
        }
        for (String argName : mArguments) {
            serializer.startTag(null, ARG)
                    .attribute(null, NAME, argName)
                    .endTag(null, ARG);
        }
        serializer.endTag(null, MUTATION);
    }

    @Override
    public void update(XmlPullParser parser)
            throws BlockLoadingException, IOException, XmlPullParserException {
        // TODO: read <mutation> and update the block.
    }
}
