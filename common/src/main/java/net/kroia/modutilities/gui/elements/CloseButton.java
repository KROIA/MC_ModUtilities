package net.kroia.modutilities.gui.elements;

import net.kroia.modutilities.gui.elements.base.Vertex;
import net.kroia.modutilities.gui.elements.base.VertexBuffer;

/**
 * A pre-styled close button that renders a red square with an "X" cross.
 * Commonly used as the close affordance on dialogs and windows.
 * <p>
 * Comes with default size 20x20 and red color theme; clicking triggers the
 * runnable supplied at construction time.
 */
public class CloseButton extends Button{

    private final VertexBuffer line1;
    private final VertexBuffer line2;

    /**
     * Creates a close button with the default red color theme.
     * @param onFallingEdge runnable executed when the close button is pressed (mouse-down)
     */
    public CloseButton(Runnable onFallingEdge) {
        super(0,0,10,10,"", onFallingEdge);
        line1 = new VertexBuffer();
        line2 = new VertexBuffer();

        super.setBackgroundColor(0xFFf55a42);
        super.setHoverColor(0xFFe03d24);
        super.setPressedColor(0xFFde2b10);
        super.setOutlineColor(0xFFde2510);
        super.setSize(20,20);
    }

    @Override
    public void render(){
        // Draw cross
        drawVertexBuffer_QUADS(line1);
        drawVertexBuffer_QUADS(line2);
    }

    @Override
    protected void layoutChanged() {
        super.layoutChanged();
        updateShape();
    }

    @Override
    public void setOutlineColor(int color) {
        super.setOutlineColor(color);
        if (line1 == null || line2 == null) return;
        for(Vertex vertex : line1.getVertices())
            vertex.setColor(color);
        for(Vertex vertex : line2.getVertices())
            vertex.setColor(color);
    }

    private void updateShape()
    {

        line1.clear();
        line2.clear();
        int color = getOutlineColor();
        int outlineThickness = getOutlineThickness();

        // Corner top left
        line1.addVertex(outlineThickness, 0, color);
        line1.addVertex(0, 0, color);
        line1.addVertex(0, outlineThickness, color);
        line1.addVertex(getWidth()-outlineThickness, getHeight(), color);

        // Corner bottom right
        line1.addVertex(getWidth()-outlineThickness, getHeight(), color);
        line1.addVertex(getWidth(), getHeight(), color);
        line1.addVertex(getWidth(), getHeight()-outlineThickness, color);
        line1.addVertex(outlineThickness, 0, color);

        // Corner top right
        line2.addVertex(getWidth(), outlineThickness, color);
        line2.addVertex(getWidth(), 0, color);
        line2.addVertex(getWidth()-outlineThickness, 0, color);
        line2.addVertex(0, getHeight()-outlineThickness, color);

        // Corner bottom left
        line2.addVertex(0, getHeight()-outlineThickness, color);
        line2.addVertex(0, getHeight(), color);
        line2.addVertex(outlineThickness, getHeight(), color);
        line2.addVertex(getWidth(), outlineThickness, color);
    }
}
