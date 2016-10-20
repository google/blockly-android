package com.google.blockly.utils;

import junit.framework.Assert;

/**
 * Aseerts used by Blockly's test code.
 */
public class MoreAsserts {
    public static void assertStringNotEmpty(String mesg, String str) {
        if (str == null) {
            Assert.fail(mesg + " Found null string.");
        } else if(str.length() == 0) {
            Assert.fail(mesg + " Found empty string.");
        }
    }
}
