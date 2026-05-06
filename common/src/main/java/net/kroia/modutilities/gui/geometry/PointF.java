package net.kroia.modutilities.gui.geometry;

/**
 * A simple mutable 2D point with double-precision floating point coordinates.
 */
public class PointF {
    /** The x coordinate. */
    public double x;
    /** The y coordinate. */
    public double y;

    /**
     * Creates a new floating point at the given coordinates.
     * @param x the x coordinate
     * @param y the y coordinate
     */
    public PointF(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Returns a new point that is this point rotated around the origin (0,0) by the given angle.
     * @param angle rotation angle in degrees
     * @return a new rotated point; this point is not modified
     */
    public PointF getRotated(float angle) {
        double rad = Math.toRadians(angle);
        double x = (this.x * Math.cos(rad) - this.y * Math.sin(rad));
        double y = (this.x * Math.sin(rad) + this.y * Math.cos(rad));
        return new PointF(x, y);
    }
}
