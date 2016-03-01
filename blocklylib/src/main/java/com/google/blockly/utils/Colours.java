/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.blockly.utils;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.text.ParseException;
import java.util.regex.Pattern;

/**
 * Constants and utility functions for handling colours and colour strings.
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

    /**
     * Parses a string as an opaque colour, either as a decimal hue (example: {@code 330}) using a
     * standard set of saturation and value) or as six digit hex code (example: {@code #BB66FF}).
     * If the string is null or cannot  otherwise be parsed, any error is logged and
     * {@code defaultColour} is returned.
     *
     * @param str The input to parse.
     * @param tempHsvArray An optional previously allocated array for HSV calculations.
     * @param defaultColour The default colour to return if the colour cannot be parsed.
     * @return The parsed colour, in {@code int} form.
     */
    public static int parseColour(
            @Nullable String str, @Nullable float[] tempHsvArray, int defaultColour) {
        if (str == null) {
            return defaultColour;
        }
        str = str.trim();
        if (str.isEmpty()) {
            return defaultColour;
        }
        try {
            return parseColour(str, tempHsvArray);
        } catch (ParseException e) {
            Log.w(TAG, e.toString());
            return defaultColour;
        }
    }

    /**
     * Parses a string as an opaque colour, either as a decimal hue (example: {@code 330}) using a
     * standard set of saturation and value) or as six digit hex code (example: {@code #BB66FF}).
     * If the string cannot be parsed, a {@link ParseException} is thrown.
     *
     * @param str The input to parse.
     * @param tempHsvArray An optional previously allocated array for HSV calculations.
     * @return The parsed colour, in {@code int} form.
     * @throws ParseException
     */
    public static int parseColour(@NonNull String str, @Nullable float[] tempHsvArray)
            throws ParseException {
        Integer result = null;

        char firstChar = str.charAt(0);
        if (firstChar == '#' && str.length() == 7) {
            try {
                result = Integer.parseInt(str.substring(1, 7), 16);
            } catch (NumberFormatException e) {
                throw new ParseException("Invalid hex color: " + str, 0);
            }
            return result;
        } else if (Character.isDigit(firstChar) && str.length() <= 3) {
            try {
                int hue = Integer.parseInt(str);
                result = getBlockColorForHue(hue, tempHsvArray);
            } catch (NumberFormatException e) {
                throw new ParseException("Invalid color hue: " + str, 0);
            }
        }
        // Maybe other color formats? 3 digit hex, CSS color functions, etc.
        return result;
    }

    /**
     * Converts a hue number to a standard ARGB {@code int}, using {@link #DEFAULT_BLOCK_SATURATION}
     * and {@link #DEFAULT_BLOCK_VALUE}.  The resulting color will always be opaque.
     *
     * @param hue The hue to convert.
     * @param tempHsvArray An optional previously allocated array for HSV calculations.
     * @return The color as an ARGB {@code int}.
     */
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

    /**
     * Linearly interpolates a between two one-byte channels.  If the value is outside the range of
     * 0-255 (either because the an input is outside the range, or the ratio is outside the range of
     * 0.0-1.0), the results will be clamped to 0-255.
     *
     * @param a The start colour, or the result if the {@code ratio} is 0.0.
     * @param b The end colour, or the result if the {@code ratio} is 1.0.
     * @param ratio The ratio of {@code b}'s influence in the result, between 0.0 to 1.0.
     * @return The computed blend value.
     */
    private static int clampedLerp(int a, int b, float ratio) {
        int rawResult = a + (int)((b - a) * ratio);
        return Math.max(Math.min(rawResult, 255), 0);
    }
}
