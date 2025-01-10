package net.kroia.modutilities.gui.elements.base;

import java.util.ArrayList;

public class VertexBuffer {
    ArrayList<Vertex> vertices = new ArrayList<>();

    public VertexBuffer() {
    }
    public VertexBuffer(VertexBuffer vertexBuffer) {
        for(Vertex vertex : vertexBuffer.vertices) {
            vertices.add(new Vertex(vertex));
        }
    }
    public void addVertex(Vertex vertex) {
        vertices.add(vertex);
    }
    public void addVertex(float x, float y) {
        vertices.add(new Vertex(x, y));
    }
    public void addVertex(float x, float y, int red, int green, int blue, int alpha) {
        vertices.add(new Vertex(x, y, red, green, blue, alpha));
    }

    public void addVertex(float x, float y, int color) {
        vertices.add(new Vertex(x, y, color));
    }
    public void clear() {
        vertices.clear();
    }

    public Vertex getVertex(int index) {
        return vertices.get(index);
    }
    public int size() {
        return vertices.size();
    }
    public void removeVertex(int index) {
        vertices.remove(index);
    }
    public void removeVertex(Vertex vertex) {
        vertices.remove(vertex);
    }
    public ArrayList<Vertex> getVertices() {
        return vertices;
    }
    public void setVertices(ArrayList<Vertex> vertices) {
        this.vertices = vertices;
    }
}
