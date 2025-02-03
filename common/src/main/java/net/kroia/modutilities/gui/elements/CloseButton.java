package net.kroia.modutilities.gui.elements;

import net.kroia.modutilities.gui.elements.base.Vertex;
import net.kroia.modutilities.gui.elements.base.VertexBuffer;

public class CloseButton extends Button{

    // private final VertexBuffer line1;
    // private final VertexBuffer line2;
    private final Label xLabel;

    public CloseButton(Runnable onFallingEdge) {
        super(0,0,10,10,"", onFallingEdge);
        //line1 = new VertexBuffer();
        //line2 = new VertexBuffer();
        xLabel = new Label("x");
        xLabel.setAlignment(Label.Alignment.CENTER);
        addChild(xLabel);

        super.setIdleColor(0xFFf55a42);
        super.setHoverColor(0xFFe03d24);
        super.setPressedColor(0xFFde2b10);
        super.setOutlineColor(0xFFde2510);
        super.setSize(20,20);
    }

    @Override
    public void render(){
        // Draw cross
        /*drawLine(0, 0, getWidth()*2, getHeight()*2, 0xFFFFFFFF);
        drawLine(getWidth(), 0, 0, getHeight(), 0xFF000000);*/
        //drawVertexBuffer_QUADS(line1);
        //drawVertexBuffer_QUADS(line2);
    }

    @Override
    protected void layoutChanged() {
        super.layoutChanged();
        xLabel.setBounds(0, 0, getWidth(), getHeight());
        updateShape();
    }

    @Override
    public void setOutlineColor(int color) {
        super.setOutlineColor(color);
        xLabel.setTextColor(color);
        //for(Vertex vertex : line1.getVertices())
        //    vertex.setColor(color);
        //for(Vertex vertex : line2.getVertices())
        //    vertex.setColor(color);
    }

    private void updateShape()
    {
        /*
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
        line2.addVertex(getWidth(), outlineThickness, color);*/
    }
}
