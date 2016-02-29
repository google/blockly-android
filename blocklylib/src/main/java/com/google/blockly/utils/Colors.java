package com.google.blockly.utils;

import android.graphics.Color;

import java.util.regex.Pattern;

/**
 * Utility constants for handling colors and color strings.
 */
public class Colors {
    /** Regex pattern to match a standard six-digit color code. */
    public static final Pattern SIX_DIGIT_HEX = Pattern.compile("\\#([0-9A-Fa-f]{6})");
}
