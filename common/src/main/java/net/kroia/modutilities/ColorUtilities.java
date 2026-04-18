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
        return ((alpha & 0xFF) << 24) | ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF);
    }
    public static int setAlpha(int color, int alpha)
    {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }
    public static int setAlpha(int color, float alpha)
    {
        return setAlpha(color, (int)(alpha*255));
    }
    public static int setRed(int color, int red)
    {
        return (color & 0xFF00FFFF) | ((red & 0xFF) << 16);
    }
    public static int setRed(int color, float red)
    {
        return setRed(color, (int)(red*255));
    }
    public static int setGreen(int color, int green)
    {
        return (color & 0xFFFF00FF) | ((green & 0xFF) << 8);
    }
    public static int setGreen(int color, float green)
    {
        return setGreen(color, (int)(green*255));
    }
    public static int setBlue(int color, int blue)
    {
        return (color & 0xFFFFFF00) | (blue & 0xFF);
    }
    public static int setBlue(int color, float blue)
    {
        return setBlue(color, (int)(blue*255));
    }


    public static int getRGB(int color, int alpha)
    {
        return ((alpha & 0xFF) << 24) | (color & 0x00FFFFFF);
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

        red = (int)Math.min(Math.max((red * brightness), 0), 255);
        green = (int)Math.min(Math.max((green * brightness), 0), 255);
        blue = (int)Math.min(Math.max((blue * brightness), 0), 255);

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

        int red = (int)Math.max(Math.min((red1 + (red2 - red1) * ratio), 255), 0);
        int green = (int)Math.max(Math.min((green1 + (green2 - green1) * ratio), 255), 0);
        int blue = (int)Math.max(Math.min((blue1 + (blue2 - blue1) * ratio), 255), 0);
        int alpha = (int)Math.max(Math.min((alpha1 + (alpha2 - alpha1) * ratio), 255), 0);

        return getRGB(red, green, blue, alpha);
    }
}
