package com.google.blockly.utils;

import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;

/**
 * Generates ColorMatrix and ColorMatrixColorFilters that rotate color hues by a given degree.
 *
 * @see http://stackoverflow.com/a/7917978
 */
public class ColorManipulation  {
    private static final float DEGREES_PER_RADIAN = (float) Math.PI / 180;
    private static final float RED_LUMINANCE = 0.213f;
    private static final float GREEN_LUMINANCE = 0.715f;
    private static final float BLUE_LUMINANCE = 0.072f;

    /**
     * Creates a hue rotation ColorFilter.
     *
     * @see http://groups.google.com/group/android-developers/browse_thread/thread/9e215c83c3819953
     * @see http://gskinner.com/blog/archives/2007/12/colormatrix_cla.html
     * @param hueRotationDegrees degrees to shift the hue.
     * @return Newly instantiated hue rotation ColorFilter.
     */
    public static ColorFilter buildColorFilterInDegrees(float hueRotationDegrees) {
        return new ColorMatrixColorFilter(buildHueRotationMatrixInDegrees(hueRotationDegrees));
    }

    /**
     * Builds a hue rotating ColorMatrix for the given rotation angle.
     *
     * @param hueRotationDegrees degrees to shift hue.
     */
    public static ColorMatrix buildHueRotationMatrixInDegrees(float hueRotationDegrees) {
        float hueRotationRads = clamp(hueRotationDegrees, -180, 180) / DEGREES_PER_RADIAN;
        return buildHueRotationMatrixInRadians(hueRotationRads);
    }

    /**
     * Builds a hue rotating ColorMatrix for the given rotation angle.
     *
     * @see http://groups.google.com/group/android-developers/browse_thread/thread/9e215c83c3819953
     * @see http://gskinner.com/blog/archives/2007/12/colormatrix_cla.html
     *
     * @param hueRotationRads radians to shift the hue.
     */
    public static ColorMatrix buildHueRotationMatrixInRadians(float hueRotationRads) {
        float cosVal = (float) Math.cos(hueRotationRads);
        float sinVal = (float) Math.sin(hueRotationRads);
        float[] mat = new float[] {
                RED_LUMINANCE + cosVal * (1 - RED_LUMINANCE) + sinVal * (-RED_LUMINANCE),
                GREEN_LUMINANCE + cosVal * (-GREEN_LUMINANCE) + sinVal * (-GREEN_LUMINANCE),
                BLUE_LUMINANCE + cosVal * (-BLUE_LUMINANCE) + sinVal * (1 - BLUE_LUMINANCE),
                0, 0,

                RED_LUMINANCE + cosVal * (-RED_LUMINANCE) + sinVal * (0.143f),
                GREEN_LUMINANCE + cosVal * (1 - GREEN_LUMINANCE) + sinVal * (0.140f),
                BLUE_LUMINANCE + cosVal * (-BLUE_LUMINANCE) + sinVal * (-0.283f),
                0, 0,

                RED_LUMINANCE + cosVal * (-RED_LUMINANCE) + sinVal * (-(1 - RED_LUMINANCE)),
                GREEN_LUMINANCE + cosVal * (-GREEN_LUMINANCE) + sinVal * (GREEN_LUMINANCE),
                BLUE_LUMINANCE + cosVal * (1 - BLUE_LUMINANCE) + sinVal * (BLUE_LUMINANCE),
                0, 0,

                0f, 0f, 0f, 1f, 0f,
        };
        return new ColorMatrix(mat);
    }

    public static void adjustBrightness(ColorMatrix cm, float value) {
        value = clamp(value, -100, 100);
        if (value == 0) {
            return;
        }

        float[] mat = new float[] {
                1,0,0,0,value,
                0,1,0,0,value,
                0,0,1,0,value,
                0,0,0,1,0,
                0,0,0,0,1
        };
        cm.postConcat(new ColorMatrix(mat));
    }

    public static void adjustSaturation(ColorMatrix cm, float value) {
        value = clamp(value,-100, 100);
        if (value == 0) {
            return;
        }

        float x = 1+((value > 0) ? 3 * value / 100 : value / 100);
        float lumR = 0.3086f;
        float lumG = 0.6094f;
        float lumB = 0.0820f;

        float[] mat = new float[]
                {
                        lumR*(1-x)+x,lumG*(1-x),lumB*(1-x),0,0,
                        lumR*(1-x),lumG*(1-x)+x,lumB*(1-x),0,0,
                        lumR*(1-x),lumG*(1-x),lumB*(1-x)+x,0,0,
                        0,0,0,1,0,
                        0,0,0,0,1
                };
        cm.postConcat(new ColorMatrix(mat));
    }

    protected static float clamp(float value, float min, float max)
    {
        return Math.min(max, Math.max(min, value));
    }
}
