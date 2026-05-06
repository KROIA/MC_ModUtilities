package net.kroia.modutilities.gui.geometry;

/**
 * A simple mutable 2D point with integer coordinates.
 */
public class Point {
    /** The x coordinate. */
    public int x;
    /** The y coordinate. */
    public int y;

    /**
     * Creates a new point at the given coordinates.
     * @param x the x coordinate
     * @param y the y coordinate
     */
    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Returns a new point that is this point rotated around the origin (0,0) by the given angle.
     * Coordinates are rounded to the nearest integer.
     * @param angle rotation angle in degrees
     * @return a new rotated point; this point is not modified
     */
    public Point getRotated(int angle) {
        double rad = Math.toRadians(angle);
        int x = (int) Math.round(this.x * Math.cos(rad) - this.y * Math.sin(rad));
        int y = (int) Math.round(this.x * Math.sin(rad) + this.y * Math.cos(rad));
        return new Point(x, y);
    }
}
