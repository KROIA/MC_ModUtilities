package net.kroia.modutilities.gui.geometry;

public class PointF {
    public float x;
    public float y;
    public PointF(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public PointF getRotated(float angle) {
        double rad = Math.toRadians(angle);
        float x = (float) (this.x * Math.cos(rad) - this.y * Math.sin(rad));
        float y = (float) (this.x * Math.sin(rad) + this.y * Math.cos(rad));
        return new PointF(x, y);
    }
}
