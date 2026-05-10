package net.kroia.modutilities.gui.elements.base;

/**
 * A coloured 2D vertex used by {@link VertexBuffer} for low-level shape rendering.
 * Colour channels are stored as 0-255 integer components.
 */
public class Vertex {
    /** Vertex x position in element-local coordinates. */
    public float x;
    /** Vertex y position in element-local coordinates. */
    public float y;

    /** Red channel (0-255). */
    public int red;
    /** Green channel (0-255). */
    public int green;
    /** Blue channel (0-255). */
    public int blue;
    /** Alpha channel (0-255). */
    public int alpha;

    /**
     * Creates a vertex at the given position. Colour channels default to 0 (transparent black).
     * @param x x coordinate
     * @param y y coordinate
     */
    public Vertex(float x, float y) {
        setPosition(x, y);
    }

    /**
     * Creates a vertex with explicit position and RGBA components.
     * @param x x coordinate
     * @param y y coordinate
     * @param red red component (0-255)
     * @param green green component (0-255)
     * @param blue blue component (0-255)
     * @param alpha alpha component (0-255)
     */
    public Vertex(float x, float y, int red, int green, int blue, int alpha) {
        setPosition(x, y);
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
    }

    /**
     * Creates a vertex with the given position and packed ARGB colour.
     * @param x x coordinate
     * @param y y coordinate
     * @param color ARGB-packed colour (0xAARRGGBB)
     */
    public Vertex(float x, float y, int color) {
        setPosition(x, y);
        setColor(color);
    }

    /**
     * Copy constructor.
     * @param vertex the vertex to copy
     */
    public Vertex(Vertex vertex) {
        this.x = vertex.x;
        this.y = vertex.y;
        this.red = vertex.red;
        this.green = vertex.green;
        this.blue = vertex.blue;
        this.alpha = vertex.alpha;
    }

    /**
     * Sets all colour channels from a packed ARGB integer.
     * @param color ARGB-packed colour (0xAARRGGBB)
     */
    public void setColor(int color)
    {
        this.red = (color >> 16) & 0xFF;
        this.green = (color >> 8) & 0xFF;
        this.blue = color & 0xFF;
        this.alpha = (color >> 24) & 0xFF;
    }

    /**
     * @return the colour as a packed ARGB integer (0xAARRGGBB)
     */
    public int getColor()
    {
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    /**
     * Sets the position of this vertex.
     * @param x x coordinate
     * @param y y coordinate
     */
    public void setPosition(float x, float y)
    {
        this.x = x;
        this.y = y;
    }

}
