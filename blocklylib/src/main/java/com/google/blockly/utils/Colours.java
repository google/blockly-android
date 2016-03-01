package com.google.blockly.utils;

import android.graphics.Color;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.regex.Pattern;

/**
 * Constants and utility functiosn for handling colours and colour strings.
 */
public class Colours {
    private static final String TAG = "Colours";

    /** Regex pattern to match a standard six-digit color code. */
    public static final Pattern SIX_DIGIT_HEX_PATTERN = Pattern.compile("\\#([0-9A-Fa-f]{6})");
    public static final int ALPHA_OPAQUE = 255;

    public static final int DEFAULT_BLOCK_HUE = 0;
    public static final float DEFAULT_BLOCK_SATURATION = 0.7f;
    public static final float DEFAULT_BLOCK_VALUE = 0.8f;
    public static final int DEFAULT_BLOCK_COLOUR = getBlockColorForHue(DEFAULT_BLOCK_HUE, null);

    public static int parseColour(
            @Nullable String value, @Nullable float[] tempHsvArray, int defaultColour) {
        value = value.trim();
        if (value.isEmpty()) {
            return defaultColour;
        }

        char firstChar = value.charAt(0);
        if (firstChar == '#' && value.length() == 7) {
            int colour = defaultColour;
            try {
                colour = Integer.parseInt(value.substring(1,7), 16);
            } catch (NumberFormatException e) {
                Log.w(TAG, "Invalid hex color \"" + value + "\"");
            }
            return colour;
        }
        if (Character.isDigit(firstChar) && value.length() <= 3) {
            int colour = defaultColour;
            try {
                int hue = Integer.parseInt(value);
                colour = getBlockColorForHue(hue, tempHsvArray);
            } catch (NumberFormatException e) {
                Log.w(TAG, "Invalid colour hue \"" + value + "\"");
            }
            return colour;
        }
        // Maybe other color formats? 3 digit hex, CSS color functions, etc.
        return defaultColour;
    }

    @Nullable
    public static Integer maybeParseColour(
            @Nullable String value, @Nullable float[] tempHsvArray, String logColourName) {
        if (value == null) {
            return null;
        }
        value = value.trim();
        if (value.isEmpty()) {
            return null;
        }
        Integer result = null;

        char firstChar = value.charAt(0);
        if (firstChar == '#' && value.length() == 7) {
            try {
                result = Integer.parseInt(value.substring(1, 7), 16);
            } catch (NumberFormatException e) {
                StringBuilder sb = new StringBuilder("Invalid ");
                if (logColourName != null) {
                    sb.append(logColourName + " ");
                }
                sb.append(" colour \"" + value + "\"");
                Log.w(TAG, sb.toString());
            }
            return result;
        } else if (Character.isDigit(firstChar) && value.length() <= 3) {
            try {
                int hue = Integer.parseInt(value);
                result = getBlockColorForHue(hue, tempHsvArray);
            } catch (NumberFormatException e) {
                StringBuilder sb = new StringBuilder("Invalid ");
                if (logColourName != null) {
                    sb.append(logColourName + " ");
                }
                sb.append(" colour hue \"" + value + "\"");
                Log.w(TAG, sb.toString());
            }
        }
        // Maybe other color formats? 3 digit hex, CSS color functions, etc.
        return result;
    }

    public static int getBlockColorForHue(int hue, @Nullable float[] tempHsvArray) {
        hue = ((hue % 360) + 360) % 360;  // Clamp to 0-359

        if (tempHsvArray == null) {
            tempHsvArray = new float[3];
        }
        tempHsvArray[0] = hue;
        tempHsvArray[1] = DEFAULT_BLOCK_SATURATION;
        tempHsvArray[2] = DEFAULT_BLOCK_VALUE;
        return Color.HSVToColor(tempHsvArray);
    }

    /**
     * Linearly interpolate the RGB from colour {@code a} to colour {@code b}.  Alpha values are
     * ignored, and the resulting alpha is always opaque.
     *
     * @param a The start colour, or the result if the {@code ratio} is 0.0.
     * @param b The end colour, or the result if the {@code ratio} is 1.0.
     * @param ratio The ratio of {@code b}'s influence in the result, between 0.0 to 1.0.
     * @return The computed blend colour as an integer.
     */
    public static int blendRGB(int a, int b, float ratio) {
        return Color.argb(ALPHA_OPAQUE,
                clampedLerp(Color.red(a), Color.red(b), ratio),
                clampedLerp(Color.green(a), Color.green(b), ratio),
                clampedLerp(Color.blue(a), Color.blue(b), ratio));
    }

    private static int clampedLerp(int a, int b, float ratio) {
        int rawResult = a + (int)((b - a) * ratio);
        return Math.max(Math.min(rawResult, 255), 0);
    }
}
