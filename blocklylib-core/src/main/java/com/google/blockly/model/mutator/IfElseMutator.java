package com.google.blockly.model.mutator;

import android.content.Context;

import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.android.ui.MutatorFragment;
import com.google.blockly.android.ui.mutator.IfElseMutatorFragment;
import com.google.blockly.model.Block;
import com.google.blockly.model.Mutator;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;


public class IfElseMutator extends Mutator {
    public static final String TAG = "IfElseMutator";
    public static final String MUTATOR_ID = "controls_if_mutator";

    private final Context mContext;
    private final BlocklyController mController;

    private Block mBlock;
    private int mElseIfCount = 0;
    private boolean elseStatement = false;

    public IfElseMutator(Context context, BlocklyController controller) {
        mContext = context;
        mController = controller;
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

    }

    @Override
    public void update(Block block,
            XmlPullParser parser) throws IOException, XmlPullParserException {

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
