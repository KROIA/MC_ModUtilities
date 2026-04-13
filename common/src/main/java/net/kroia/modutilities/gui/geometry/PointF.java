package net.kroia.modutilities.gui.geometry;

public class PointF {
    public double x;
    public double y;
    public PointF(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public PointF getRotated(float angle) {
        double rad = Math.toRadians(angle);
        double x = (this.x * Math.cos(rad) - this.y * Math.sin(rad));
        double y = (this.x * Math.sin(rad) + this.y * Math.cos(rad));
        return new PointF(x, y);
    }
}
