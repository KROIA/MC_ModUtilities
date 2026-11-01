package net.kroia.modutilities.gui.geometry;


/**
 * A simple mutable axis-aligned rectangle with double-precision floating point coordinates.
 */
public class RectangleF {
    /** The x coordinate of the top-left corner. */
    public double x;
    /** The y coordinate of the top-left corner. */
    public double y;
    /** The width. */
    public double width;
    /** The height. */
    public double height;

    /**
     * Creates a new floating-point rectangle from the given top-left corner and size.
     * @param x left edge
     * @param y top edge
     * @param width width
     * @param height height
     */
    public RectangleF(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * Copy constructor.
     * @param other the rectangle to copy
     */
    public RectangleF(RectangleF other) {
        this.x = other.x;
        this.y = other.y;
        this.width = other.width;
        this.height = other.height;
    }

    /**
     * Tests whether the given point lies within this rectangle (right/bottom edges exclusive).
     * @param posX x to test
     * @param posY y to test
     * @return true if inside this rectangle
     */
    public boolean contains(double posX, double posY) {
        return posX >= x &&
                posX < x + width &&
                posY >= y &&
                posY < y + height;
    }

    /**
     * Tests whether the given integer point lies within this rectangle.
     * @param point the point to test
     * @return true if inside this rectangle
     */
    public boolean contains(Point point) {
        return point.x >= x &&
                point.x < x + width &&
                point.y >= y &&
                point.y < y + height;
    }

    /**
     * Tests whether the given floating-point point lies within this rectangle.
     * @param point the point to test
     * @return true if inside this rectangle
     */
    public boolean contains(PointF point) {
        return point.x >= x &&
                point.x < x + width &&
                point.y >= y &&
                point.y < y + height;
    }

    /**
     * Tests whether this rectangle overlaps another.
     * @param other the rectangle to test against
     * @return true if the two rectangles intersect
     */
    public boolean intersects(RectangleF other) {
        return x < other.x + other.width &&
                x + width > other.x &&
                y < other.y + other.height &&
                y + height > other.y;
    }
}
