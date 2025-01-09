package net.kroia.modutilities;

public class ColorUtilities {


    public static int getRed(int color)
    {
        return (color >> 16) & 0xFF;
    }
    public static int getGreen(int color)
    {
        return (color >> 8) & 0xFF;
    }
    public static int getBlue(int color)
    {
        return color & 0xFF;
    }
    public static int getAlpha(int color)
    {
        return (color >> 24) & 0xFF;
    }
    public static int getRGB(int red, int green, int blue)
    {
        return getRGB(red, green, blue, 255);
    }
    public static int getRGB(int red, int green, int blue, int alpha)
    {
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    public static int getRGB(int color, int alpha)
    {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    public static int getRGB(int color, float alpha)
    {
        return getRGB(color, (int)(alpha*255));
    }

    public static int setBrightness(int color, float brightness)
    {
        int red = getRed(color);
        int green = getGreen(color);
        int blue = getBlue(color);
        int alpha = getAlpha(color);

        red = (int)(red * brightness);
        green = (int)(green * brightness);
        blue = (int)(blue * brightness);

        return getRGB(red, green, blue, alpha);
    }
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

        int red = (int)(red1 + (red2 - red1) * ratio);
        int green = (int)(green1 + (green2 - green1) * ratio);
        int blue = (int)(blue1 + (blue2 - blue1) * ratio);
        int alpha = (int)(alpha1 + (alpha2 - alpha1) * ratio);

        return getRGB(red, green, blue, alpha);
    }
}
