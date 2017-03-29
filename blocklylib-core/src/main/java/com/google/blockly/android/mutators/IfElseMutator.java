package com.google.blockly.android.mutators;

import android.util.Log;

import com.google.blockly.model.Block;
import com.google.blockly.model.Mutator;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;


public class IfElseMutator extends Mutator {
    public static final String TAG = "IfElseMutator";

    public static class Factory implements Mutator.Factory<IfElseMutator> {

        @Override
        public IfElseMutator newMutator() {
            return new IfElseMutator();
        }
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
        return false;
    }
}
