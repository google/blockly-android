package com.google.blockly.model.mutator;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.blockly.android.R;
import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.android.ui.MutatorFragment;
import com.google.blockly.model.Block;
import com.google.blockly.model.Mutator;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;


public class IfElseMutator extends Mutator {
    public static final String TAG = "IfElseMutator";

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
        IfElseMutatorDialog dialog = new IfElseMutatorDialog();
        dialog.setMutator(this);
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

    public static class IfElseMutatorDialog extends MutatorFragment {
        private IfElseMutator mMutator;
        private Block mBlock;

        private void setMutator(IfElseMutator mutator) {
            mMutator = mutator;
            mBlock = mutator.mBlock;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.if_else_mutator_dialog, container, false);
            return v;
        }

        private class TouchHelperCallback extends ItemTouchHelper.Callback {

            @Override
            public int getMovementFlags(RecyclerView recyclerView,
                    RecyclerView.ViewHolder viewHolder) {
                return 0;
            }

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                    RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

            }
        }
    }
}
