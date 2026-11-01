package net.kroia.modutilities;

/**
 * Bit-twiddling helpers for packed ARGB colors.
 * <p>
 * Colors are stored in 32-bit integers with byte layout {@code 0xAARRGGBB}: alpha in the
 * high byte, followed by red, green and blue. All accessors and mutators operate on this
 * packed format.
 */
public class ColorUtilities {


    /**
     * Extracts the red channel from a packed ARGB color.
     *
     * @param color the packed ARGB color
     * @return the red component in {@code [0, 255]}
     */
    public static int getRed(int color)
    {
        return (color >> 16) & 0xFF;
    }
    /**
     * Extracts the green channel from a packed ARGB color.
     *
     * @param color the packed ARGB color
     * @return the green component in {@code [0, 255]}
     */
    public static int getGreen(int color)
    {
        return (color >> 8) & 0xFF;
    }
    /**
     * Extracts the blue channel from a packed ARGB color.
     *
     * @param color the packed ARGB color
     * @return the blue component in {@code [0, 255]}
     */
    public static int getBlue(int color)
    {
        return color & 0xFF;
    }
    /**
     * Extracts the alpha channel from a packed ARGB color.
     *
     * @param color the packed ARGB color
     * @return the alpha component in {@code [0, 255]}
     */
    public static int getAlpha(int color)
    {
        return (color >> 24) & 0xFF;
    }
    /**
     * Packs the given RGB components into an ARGB color with full opacity.
     *
     * @param red   the red component in {@code [0, 255]}
     * @param green the green component in {@code [0, 255]}
     * @param blue  the blue component in {@code [0, 255]}
     * @return the packed ARGB color with alpha set to 255
     */
    public static int getRGB(int red, int green, int blue)
    {
        return getRGB(red, green, blue, 255);
    }
    /**
     * Packs the given RGBA components into an ARGB color.
     *
     * @param red   the red component in {@code [0, 255]}
     * @param green the green component in {@code [0, 255]}
     * @param blue  the blue component in {@code [0, 255]}
     * @param alpha the alpha component in {@code [0, 255]}
     * @return the packed ARGB color
     */
    public static int getRGB(int red, int green, int blue, int alpha)
    {
        return ((alpha & 0xFF) << 24) | ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF);
    }
    /**
     * Returns the given color with its alpha channel replaced.
     *
     * @param color the packed ARGB color
     * @param alpha the new alpha value in {@code [0, 255]}
     * @return a new packed ARGB color with the requested alpha
     */
    public static int setAlpha(int color, int alpha)
    {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }
    /**
     * Returns the given color with its alpha channel replaced.
     *
     * @param color the packed ARGB color
     * @param alpha the new alpha value in {@code [0.0f, 1.0f]} (scaled to {@code [0, 255]})
     * @return a new packed ARGB color with the requested alpha
     */
    public static int setAlpha(int color, float alpha)
    {
        return setAlpha(color, (int)(alpha*255));
    }
    /**
     * Returns the given color with its red channel replaced.
     *
     * @param color the packed ARGB color
     * @param red   the new red value in {@code [0, 255]}
     * @return a new packed ARGB color with the requested red component
     */
    public static int setRed(int color, int red)
    {
        return (color & 0xFF00FFFF) | ((red & 0xFF) << 16);
    }
    /**
     * Returns the given color with its red channel replaced.
     *
     * @param color the packed ARGB color
     * @param red   the new red value in {@code [0.0f, 1.0f]} (scaled to {@code [0, 255]})
     * @return a new packed ARGB color with the requested red component
     */
    public static int setRed(int color, float red)
    {
        return setRed(color, (int)(red*255));
    }
    /**
     * Returns the given color with its green channel replaced.
     *
     * @param color the packed ARGB color
     * @param green the new green value in {@code [0, 255]}
     * @return a new packed ARGB color with the requested green component
     */
    public static int setGreen(int color, int green)
    {
        return (color & 0xFFFF00FF) | ((green & 0xFF) << 8);
    }
    /**
     * Returns the given color with its green channel replaced.
     *
     * @param color the packed ARGB color
     * @param green the new green value in {@code [0.0f, 1.0f]} (scaled to {@code [0, 255]})
     * @return a new packed ARGB color with the requested green component
     */
    public static int setGreen(int color, float green)
    {
        return setGreen(color, (int)(green*255));
    }
    /**
     * Returns the given color with its blue channel replaced.
     *
     * @param color the packed ARGB color
     * @param blue  the new blue value in {@code [0, 255]}
     * @return a new packed ARGB color with the requested blue component
     */
    public static int setBlue(int color, int blue)
    {
        return (color & 0xFFFFFF00) | (blue & 0xFF);
    }
    /**
     * Returns the given color with its blue channel replaced.
     *
     * @param color the packed ARGB color
     * @param blue  the new blue value in {@code [0.0f, 1.0f]} (scaled to {@code [0, 255]})
     * @return a new packed ARGB color with the requested blue component
     */
    public static int setBlue(int color, float blue)
    {
        return setBlue(color, (int)(blue*255));
    }


    /**
     * Combines an RGB-only color with the given alpha into a packed ARGB color.
     *
     * @param color a packed color whose alpha byte is ignored (only RGB used)
     * @param alpha the alpha component in {@code [0, 255]}
     * @return the packed ARGB color
     */
    public static int getRGB(int color, int alpha)
    {
        return ((alpha & 0xFF) << 24) | (color & 0x00FFFFFF);
    }

    /**
     * Combines an RGB-only color with the given alpha into a packed ARGB color.
     *
     * @param color a packed color whose alpha byte is ignored (only RGB used)
     * @param alpha the alpha component in {@code [0.0f, 1.0f]} (scaled to {@code [0, 255]})
     * @return the packed ARGB color
     */
    public static int getRGB(int color, float alpha)
    {
        return getRGB(color, (int)(alpha*255));
    }

    /**
     * Multiplies each RGB channel of the color by the given brightness factor.
     * <p>
     * Channel results are clamped to {@code [0, 255]}; the alpha channel is preserved.
     *
     * @param color      the packed ARGB color
     * @param brightness the multiplier applied to each RGB channel
     *                   ({@code 1.0f} returns the same color, {@code 0.0f} returns black,
     *                   values above {@code 1.0f} brighten and clip)
     * @return a new packed ARGB color with the adjusted brightness
     */
    public static int setBrightness(int color, float brightness)
    {
        int red = getRed(color);
        int green = getGreen(color);
        int blue = getBlue(color);
        int alpha = getAlpha(color);

        red = (int)Math.min(Math.max((red * brightness), 0), 255);
        green = (int)Math.min(Math.max((green * brightness), 0), 255);
        blue = (int)Math.min(Math.max((blue * brightness), 0), 255);

        return getRGB(red, green, blue, alpha);
    }
    /**
     * Linearly interpolates between two packed ARGB colors on every channel
     * (red, green, blue and alpha).
     * <p>
     * A {@code ratio} of {@code 0.0f} returns {@code color1}, {@code 1.0f} returns {@code color2}.
     * Each channel is clamped to {@code [0, 255]} after interpolation.
     *
     * @param color1 the start color (returned at ratio {@code 0.0f})
     * @param color2 the end color (returned at ratio {@code 1.0f})
     * @param ratio  the blend ratio in {@code [0.0f, 1.0f]}
     * @return the blended packed ARGB color
     */
    public static int interpolate(int color1, int color2, float ratio)
    {
        int red1 = getRed(color1);
        int green1 = getGreen(color1);
        int blue1 = getBlue(color1);
        int alpha1 = getAlpha(color1);

        int red2 = getRed(color2);
        int green2 = getGreen(color2);
        int blue2 = getBlue(color2);
        int alpha2 = getAlpha(color2);

        int red = (int)Math.max(Math.min((red1 + (red2 - red1) * ratio), 255), 0);
        int green = (int)Math.max(Math.min((green1 + (green2 - green1) * ratio), 255), 0);
        int blue = (int)Math.max(Math.min((blue1 + (blue2 - blue1) * ratio), 255), 0);
        int alpha = (int)Math.max(Math.min((alpha1 + (alpha2 - alpha1) * ratio), 255), 0);

        return getRGB(red, green, blue, alpha);
    }
}
