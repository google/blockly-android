package com.google.blockly.android.ui.vertical;

import android.app.DialogFragment;
import android.util.Log;

import com.google.blockly.android.mutators.IfElseMutator;
import com.google.blockly.model.Block;
import com.google.blockly.model.Mutator;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;


public class IfElseMutatorUI extends IfElseMutator {
    public static final String TAG = "IfElseMutatorUI";

    public static class Factory implements Mutator.Factory<IfElseMutatorUI> {

        @Override
        public IfElseMutatorUI newMutator() {
            return new IfElseMutatorUI();
        }
    }

    @Override
    public boolean hasUI() {
        return true;
    }

    @Override
    public void toggleUI() {
        Log.d(TAG, "Toggled the mutator UI.");
    }

    public static class MutatorDialog extends DialogFragment {

    }
}
