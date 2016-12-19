package com.google.blockly.utils;

import junit.framework.Assert;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Aseerts used by Blockly's test code.
 */
public class MoreAsserts {
    public static void assertStringNotEmpty(String mesg, String str) {
        assertNotNull(mesg + " Found null string.", str);
        assertNotEquals(mesg + " Found empty string.", str.length(), 0);
    }
}
