package net.kroia.modutilities.gui.elements.base;

public class Vertex {
    public float x;
    public float y;

    public int red;
    public int green;
    public int blue;
    public int alpha;

    public Vertex(float x, float y) {
        setPosition(x, y);
    }

    public Vertex(float x, float y, int red, int green, int blue, int alpha) {
        setPosition(x, y);
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
    }
    public Vertex(float x, float y, int color) {
        setPosition(x, y);
        setColor(color);
    }

    public Vertex(Vertex vertex) {
        this.x = vertex.x;
        this.y = vertex.y;
        this.red = vertex.red;
        this.green = vertex.green;
        this.blue = vertex.blue;
        this.alpha = vertex.alpha;
    }

    public void setColor(int color)
    {
        this.red = (color >> 16) & 0xFF;
        this.green = (color >> 8) & 0xFF;
        this.blue = color & 0xFF;
        this.alpha = (color >> 24) & 0xFF;
    }
    public int getColor()
    {
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }
    public void setPosition(float x, float y)
    {
        this.x = x;
        this.y = y;
    }

}
