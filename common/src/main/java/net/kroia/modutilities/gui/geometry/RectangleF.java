package net.kroia.modutilities.gui.geometry;


public class RectangleF {
    public double x;
    public double y;
    public double width;
    public double height;
    public RectangleF(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
    public RectangleF(RectangleF other) {
        this.x = other.x;
        this.y = other.y;
        this.width = other.width;
        this.height = other.height;
    }

    public boolean contains(double posX, double posY) {
        return posX >= x &&
                posX < x + width &&
                posY >= y &&
                posY < y + height;
    }
    public boolean contains(Point point) {
        return point.x >= x &&
                point.x < x + width &&
                point.y >= y &&
                point.y < y + height;
    }
    public boolean contains(PointF point) {
        return point.x >= x &&
                point.x < x + width &&
                point.y >= y &&
                point.y < y + height;
    }
    public boolean intersects(RectangleF other) {
        return x < other.x + other.width &&
                x + width > other.x &&
                y < other.y + other.height &&
                y + height > other.y;
    }
}
