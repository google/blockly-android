package com.google.blockly.utils;

import com.google.blockly.model.Block;
import com.google.blockly.model.Field;

import java.util.LinkedList;
import java.util.List;

/**
 * Implementation of {@link Block.Observer} the stashes all events groups received from
 * {@link #onValueChanged} into {@link #mObservations}.
 */
public class TestFieldObserver implements Field.Observer {
    public final List<Observation> mObservations = new LinkedList<>();

    @Override
    public void onValueChanged(Field field, String oldValue, String newValue) {
        mObservations.add(new Observation(field, oldValue, newValue));
    }

    public static class Observation {
        public final Field mField;
        public final String mOldValue;
        public final String mNewValue;

        Observation(Field field, String oldValue, String newValue) {
            mField = field;
            mOldValue = oldValue;
            mNewValue = newValue;
        }
    }
}
