package com.google.blockly.utils;

import com.google.blockly.model.Block;

import java.util.LinkedList;
import java.util.List;

/**
 * Implementation of {@link Block.Observer} the stashes all events groups received from
 * {@link #onBlockUpdated} into {@link #mObservations}.
 */
public class TestBlockObserver implements Block.Observer {
    public final List<Observation> mObservations = new LinkedList<>();

    @Override
    public void onBlockUpdated(Block block, @Block.UpdateState int updateStateMask) {
        mObservations.add(new Observation(block, updateStateMask));
    }

    public static class Observation {
        public final Block mBlock;
        public final @Block.UpdateState int mUpdateStateMask;

        Observation(Block block, @Block.UpdateState int updateStateMask) {
            mBlock = block;
            mUpdateStateMask = updateStateMask;
        }
    }
}
