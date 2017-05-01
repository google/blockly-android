package com.google.blockly.model.mutator;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.google.blockly.android.R;
import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.android.ui.MutatorFragment;
import com.google.blockly.android.ui.mutator.IfElseMutatorFragment;
import com.google.blockly.model.Block;
import com.google.blockly.model.Field;
import com.google.blockly.model.FieldLabel;
import com.google.blockly.model.Input;
import com.google.blockly.model.Mutator;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Mutator for the if/else if/else block. This class modifies the block model, but is not
 * responsible for updating the view hierarchy or showing an editor to the user.
 * @see IfElseMutatorFragment
 */
public class IfElseMutator extends Mutator {
    public static final String TAG = "IfElseMutator";
    public static final String MUTATOR_ID = "controls_if_mutator";

    private static final String ELSE_INPUT_NAME = "ELSE";
    private static final String IF_INPUT_PREFIX = "IF";
    private static final String DO_INPUT_PREFIX = "DO";
    private static final String[] CHECKS = {"Boolean"};
    private static final int ALIGN = Input.ALIGN_LEFT;

    private final Context mContext;
    private final BlocklyController mController;

    private Block mBlock;
    private int mElseIfCount = 0;
    private boolean mElseStatement = false;

    private String mIfLabel;
    private String mThenLabel;
    private String mElseLabel;
    private String[] mChecks = {"Boolean"};

    /**
     * Create a new mutator for the given context and controller.
     *
     * @param context Used to load strings and other configuration.
     * @param controller Controller for sending events.
     */
    public IfElseMutator(Context context, BlocklyController controller) {
        mContext = context;
        mController = controller;
        mIfLabel = mContext.getString(R.string.mutator_if_else_if_label);
        mThenLabel = mContext.getString(R.string.mutator_if_else_then_label);
        mElseLabel = mContext.getString(R.string.mutator_if_else_else_label);
    }

    @Override
    public void onAttached(Block block) {
        mBlock = block;
    }

    @Override
    public void onDetached(Block block) {
        mBlock = null;
    }

    @Override
    public void serialize(XmlSerializer serializer) throws IOException {
        serializer.startTag(null, "mutation").attribute(null, "elseif",
                String.valueOf(mElseIfCount)).attribute(null, "else", mElseStatement ? "1" : "0");
        serializer.endTag(null, "mutation");
    }

    @Override
    public void update(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.next();
        String elseIfValue = parser.getAttributeValue(null, "elseif");
        int elseIfCount = 0;
        boolean hasElse = false;
        if (!TextUtils.isEmpty(elseIfValue)) {
            try {
                elseIfCount = Integer.parseInt(elseIfValue);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Error reading mutation elseif count.", e);
            }
        }
        String elseValue = parser.getAttributeValue(null, "else");
        if (TextUtils.equals("1", elseValue)) {
            hasElse = true;
        }
        update(elseIfCount, hasElse);
    }

    @Override
    public boolean hasUI() {
        return true;
    }

    @Override
    public MutatorFragment getMutatorFragment() {
        IfElseMutatorFragment dialog = new IfElseMutatorFragment();
        dialog.init(this);
        return dialog;
    }

    /**
     * @return The number of else if inputs on this block.
     */
    public int getElseIfCount() {
        return mElseIfCount;
    }

    /**
     * @return True if this block has an else statement at the end, false otherwise.
     */
    public boolean hasElse() {
        return mElseStatement;
    }

    /**
     * Updates the block's model to the given number of else if inputs and else input.
     *
     * @param elseIfCount The number of else if inputs for this block.
     * @param hasElse True if this block should have a final else statement.
     */
    public void update(final int elseIfCount, final boolean hasElse) {

        mController.groupAndFireEvents(new Runnable() {
            @Override
            public void run() {
                updateImpl(elseIfCount, hasElse);
            }
        });
    }

    /**
     * Performs the model changes for the given count. This will reuse as many inputs as possible,
     * creating new inputs if necessary. Leftover inputs will be disconnected and thrown away.
     *
     * @param elseIfCount The number of else if inputs for this block.
     * @param hasElse True if this block should have a final else statement.
     */
    private void updateImpl(int elseIfCount, boolean hasElse) {
        List<Input> oldInputs = new ArrayList<>(mBlock.getInputs());
        List<Input> newInputs = new ArrayList<>();

        // Set aside the else input for the end.
        Input elseInput = mBlock.getInputByName(ELSE_INPUT_NAME);
        if (elseInput != null) {
            oldInputs.remove(elseInput);
        }

        // Move the first if/do block into the new input list
        newInputs.add(oldInputs.remove(0)); // IF0
        newInputs.add(oldInputs.remove(0)); // DO0

        // Copy over existing inputs if we have them, make new ones if we don't.
        for (int i = 1; i <= elseIfCount; i++) {
            if (oldInputs.size() >= 2) {
                newInputs.add(oldInputs.remove(0)); // IFi
                newInputs.add(oldInputs.remove(0)); // DOi
            } else {
                // IFi value input
                List<Field> fields = new ArrayList<>();
                fields.add(new FieldLabel(null, mIfLabel));
                Input.InputValue ifInput = new Input.InputValue(IF_INPUT_PREFIX + i, fields, ALIGN,
                        CHECKS);
                newInputs.add(ifInput);

                // DOi statement input
                fields = new ArrayList<>();
                fields.add(new FieldLabel(null, mThenLabel));
                Input.InputStatement thenInput = new Input.InputStatement(DO_INPUT_PREFIX + i,
                        fields, ALIGN, null);
                newInputs.add(thenInput);
            }
        }

        // Add the else clause if we need it
        if (hasElse) {
            if (elseInput == null) {
                List<Field> fields = new ArrayList<>();
                fields.add(new FieldLabel(null, mElseLabel));
                elseInput = new Input.InputStatement(ELSE_INPUT_NAME,
                        fields, ALIGN, null);
            }
            newInputs.add(elseInput);
        } else if (elseInput != null) {
            // disconnect the else statement
            if (elseInput.getConnection() != null && elseInput.getConnectedBlock() != null) {
                Block blockToDisconnect = elseInput.getConnectedBlock();
                mController.extractBlockAsRoot(blockToDisconnect);
            }
        }

        // Clean up extra inputs
        while (oldInputs.size() > 0) {
            Input in = oldInputs.remove(0);
            if (in.getConnection() != null && in.getConnectedBlock() != null) {
                Block blockToDisconnect = in.getConnectedBlock();
                mController.extractBlockAsRoot(blockToDisconnect);
            }
        }

        mBlock.reshape(newInputs, mBlock.getOutputConnection(), mBlock.getPreviousConnection(),
                mBlock.getNextConnection());

        mElseIfCount = elseIfCount;
        mElseStatement = hasElse;
    }

    public static class Factory implements Mutator.Factory<IfElseMutator> {
        private final Context mContext;
        private final BlocklyController mController;

        public Factory(Context context, BlocklyController controller) {
            mContext = context;
            mController = controller;
        }

        @Override
        public IfElseMutator newMutator() {
            return new IfElseMutator(mContext, mController);
        }
    }
}
