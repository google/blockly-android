package com.google.blockly.utils;

import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Aseerts used by Blockly's test code.
 */
public class MoreAsserts {
    public static void assertStringNotEmpty(String mesg, String str) {
        assertWithMessage(mesg + " Found null string").that(str).isNotNull();
        assertWithMessage(mesg + " Found empty string").that(str).isNotEmpty();
    }
}
