package net.kroia.modutilities.gui.elements.base;

import java.util.ArrayList;

/**
 * Mutable collection of {@link Vertex} entries used for batched primitive rendering
 * (lines, quads, etc.) within the GUI framework.
 * <p>
 * Buffers are typically populated by callers and then handed to drawing utilities
 * such as {@link net.kroia.modutilities.gui.Gui#drawVertexBuffer_QUADS(VertexBuffer)}
 * to render all contained vertices in a single batch.
 *
 * @apiNote The GUI package is client-only ({@code @Environment(EnvType.CLIENT)});
 *          this buffer is intended for use only on the client.
 */
public class VertexBuffer {
    ArrayList<Vertex> vertices = new ArrayList<>();

    /**
     * Creates a new empty vertex buffer.
     */
    public VertexBuffer() {
    }

    /**
     * Creates a deep copy of the given vertex buffer.
     * Each vertex from {@code vertexBuffer} is cloned into the new buffer so that
     * later changes to the source do not affect this instance.
     *
     * @param vertexBuffer the buffer whose vertices should be copied
     */
    public VertexBuffer(VertexBuffer vertexBuffer) {
        for(Vertex vertex : vertexBuffer.vertices) {
            vertices.add(new Vertex(vertex));
        }
    }

    /**
     * Appends an existing vertex instance to the end of this buffer.
     *
     * @param vertex the vertex to add
     */
    public void addVertex(Vertex vertex) {
        vertices.add(vertex);
    }

    /**
     * Appends a new vertex with only position information (color defaults to the
     * {@link Vertex} default color).
     *
     * @param x the x coordinate
     * @param y the y coordinate
     */
    public void addVertex(float x, float y) {
        vertices.add(new Vertex(x, y));
    }

    /**
     * Appends a new vertex with explicit position and RGBA color components.
     *
     * @param x     the x coordinate
     * @param y     the y coordinate
     * @param red   the red color component (0-255)
     * @param green the green color component (0-255)
     * @param blue  the blue color component (0-255)
     * @param alpha the alpha component (0-255)
     */
    public void addVertex(float x, float y, int red, int green, int blue, int alpha) {
        vertices.add(new Vertex(x, y, red, green, blue, alpha));
    }

    /**
     * Appends a new vertex with the given position and a packed ARGB color.
     *
     * @param x     the x coordinate
     * @param y     the y coordinate
     * @param color the packed ARGB color
     */
    public void addVertex(float x, float y, int color) {
        vertices.add(new Vertex(x, y, color));
    }

    /**
     * Removes all vertices from this buffer.
     */
    public void clear() {
        vertices.clear();
    }

    /**
     * Retrieves the vertex stored at the given index.
     *
     * @param index the index of the vertex to return
     * @return the vertex at the specified position
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public Vertex getVertex(int index) {
        return vertices.get(index);
    }

    /**
     * @return the number of vertices currently stored in this buffer
     */
    public int size() {
        return vertices.size();
    }

    /**
     * Removes the vertex at the specified index, shifting any subsequent
     * vertices to the left.
     *
     * @param index the index of the vertex to remove
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public void removeVertex(int index) {
        vertices.remove(index);
    }

    /**
     * Removes the first occurrence of the given vertex instance from this buffer,
     * if present.
     *
     * @param vertex the vertex to remove
     */
    public void removeVertex(Vertex vertex) {
        vertices.remove(vertex);
    }

    /**
     * Returns the underlying live list of vertices. Modifications to the returned
     * list directly affect this buffer.
     *
     * @return the backing list of vertices
     */
    public ArrayList<Vertex> getVertices() {
        return vertices;
    }

    /**
     * Replaces the underlying vertex list with the given one. The provided list
     * becomes the new backing storage; it is not copied.
     *
     * @param vertices the new vertex list to use as backing storage
     */
    public void setVertices(ArrayList<Vertex> vertices) {
        this.vertices = vertices;
    }
}
