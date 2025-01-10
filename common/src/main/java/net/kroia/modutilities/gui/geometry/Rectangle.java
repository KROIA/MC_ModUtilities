package net.kroia.modutilities.gui.geometry;


public class Rectangle {
    public int x;
    public int y;
    public int width;
    public int height;
    public Rectangle(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
    public Rectangle(Rectangle other) {
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
    public boolean intersects(Rectangle other) {
        return x < other.x + other.width &&
                x + width > other.x &&
                y < other.y + other.height &&
                y + height > other.y;
    }
}
