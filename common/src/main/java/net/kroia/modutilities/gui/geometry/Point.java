package net.kroia.modutilities.gui.geometry;

public class Point {
    public int x;
    public int y;
    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Point getRotated(int angle) {
        double rad = Math.toRadians(angle);
        int x = (int) Math.round(this.x * Math.cos(rad) - this.y * Math.sin(rad));
        int y = (int) Math.round(this.x * Math.sin(rad) + this.y * Math.cos(rad));
        return new Point(x, y);
    }
}
