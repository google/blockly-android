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
import java.util.List;

/**
 * Base class for all procedure definition and call block mutators, providing a base implementation
 * of the mutation state variables and related I/O.
 */
class AbstractProcedureMutator extends Mutator {
    protected BlocklyController mController;
    protected ProcedureManager mProcedureManager;

    protected String mProcedureName;
    protected List<String> mArguments;

    protected AbstractProcedureMutator(Mutator.Factory factory, BlocklyController controller) {
        super(factory);
    }

    @Override
    protected void onAttached(Block block) {
        super.onAttached(block);
        this.mController = block.getController();
        this.mProcedureManager = mController.getWorkspace().getProcedureManager();
        Field nameField = block.getFieldByName("NAME");
        if (nameField instanceof FieldInput) {
            mProcedureName = ((FieldInput) nameField).getText();
        } else {
            mProcedureName = ((FieldLabel) nameField).getText();
        }
    }

    @Override
    public void serialize(XmlSerializer serializer) throws IOException {
        // TODO: write <mutation>
    }

    @Override
    public void update(XmlPullParser parser)
            throws BlockLoadingException, IOException, XmlPullParserException {
        // TODO: read <mutation> and update the block.
    }
}
