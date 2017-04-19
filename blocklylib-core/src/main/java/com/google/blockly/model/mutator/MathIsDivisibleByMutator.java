package com.google.blockly.model.mutator;

import com.google.blockly.model.Block;
import com.google.blockly.model.Mutator;
import com.google.blockly.utils.BlockLoadingException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;

/**
 * Implements <code>math_is_divisibleby_mutator</code> {@link Mutator} for the
 * <code>math_number_property</code> block. This mutator has no serialized state.
 */
public class MathIsDivisibleByMutator extends Mutator {
    private Block mBlock;

    @Override
    public void onAttached(Block block) {
        if (mBlock != null) {
            throw new IllegalStateException("Cannot reuse mutator.");
        }
        mBlock = block;
    }

    @Override
    public void serialize(XmlSerializer serializer) throws IOException {
        // Do nothing. No serialized state.
    }

    @Override
    public void update(Block block, XmlPullParser parser)
            throws BlockLoadingException, IOException, XmlPullParserException {
        // Do nothing. No serialized state.
    }
}
