package com.google.blockly.model;

import com.google.blockly.android.BlocklyTestCase;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link Field} class.
 */

public class FieldTest extends BlocklyTestCase {
    @Before
    public void setUp() {
        configureForUIThread();
    }

    @Test
    public void testFireValueChanged() {
        Field field = new FieldImpl();

    }

    class FieldImpl extends Field {
        String value = "unset";

        public FieldImpl(String name) {
            super(name, TYPE_UNKNOWN);
        }

        @Override
        public boolean setFromString(String newValue) {
            String oldValue = value;
            value = newValue;
            fireValueChanged(oldValue, newValue);
            return true;
        }

        @Override
        public String getSerializedValue() {
            return value;
        }
    }
}
