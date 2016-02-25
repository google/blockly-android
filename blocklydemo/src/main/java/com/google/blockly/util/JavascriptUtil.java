package com.google.blockly.util;

/**
 * Helper functions for dealing with Javascript.
 */
public class JavascriptUtil {
    /**
     * Creates a double quoted Javascript string, escaping backslashes, single quotes, double
     * quotes, and newlines.
     */
    public static String makeJsString(String str) {
        // TODO(#17): More complete character escaping.
        String escapedStr = str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\'", "\\\'")
                .replace("\n", "\\n");
        return "\"" + escapedStr + "\"";
    }

    private JavascriptUtil() {} // Do not instantiate.
}
