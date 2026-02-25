package net.kroia.modutilities.gui.elements;

import net.kroia.modutilities.gui.GuiTexture;
import net.kroia.modutilities.gui.elements.base.GuiElement;

public class TextureElement extends GuiElement {

    public enum Mode
    {
        STRETCH,
        FILL
    }
    public enum Layer
    {
        BACKGROUND,
        FOREGROUND,
        TOOLTIP,
        GIZMO
    }
    private GuiTexture texture;
    private Mode mode = Mode.STRETCH;
    private Layer layer = Layer.FOREGROUND;

    public TextureElement()
    {
        this(null);
    }
    public TextureElement(GuiTexture texture) {
        this.texture = texture;
        setEnableBackground(false);
        setEnableOutline(false);
        if(texture != null)
        {
            this.setSize(texture.getWidth(), texture.getHeight());
        }
    }
    public TextureElement(String modID, String path, int imageWidth, int imageHeight)
    {
        this(new GuiTexture(modID, path, imageWidth, imageHeight));
    }
    public void setTexture(GuiTexture texture) {
        this.texture = texture;
    }
    public GuiTexture getTexture() {
        return texture;
    }
    public void setMode(Mode mode) {
        this.mode = mode;
    }
    public Mode getMode() {
        return mode;
    }
    public void setLayer(Layer layer) {
        this.layer = layer;
    }
    public Layer getLayer() {
        return layer;
    }

    @Override
    protected void renderBackground()
    {
        super.renderBackground();
        if(layer == Layer.BACKGROUND)
            renderTextureInternal();
    }

    @Override
    protected void render() {
        if(layer == Layer.FOREGROUND)
            renderTextureInternal();
    }

    @Override
    public void renderTooltipInternal()
    {
        super.renderTooltipInternal();
        if(layer == Layer.TOOLTIP)
            renderTextureInternal();
    }

    @Override
    public void renderGizmosInternal()
    {
        if(layer == Layer.GIZMO)
            renderTextureInternal();
        super.renderGizmosInternal();
    }

    @Override
    protected void layoutChanged() {

    }

    protected void renderTextureInternal()
    {
        if(texture == null)
            return;
        switch(mode)
        {
            case STRETCH -> {
                drawTexture(texture.getResourceLocation(),
                        0,0,
                        texture.getUVOffsetX(), texture.getUVOffsetY(),
                        getWidth(), getHeight(),
                        getWidth(), getHeight());
            }
            case FILL -> {
                drawTextureFillArea(texture, 0,0, getWidth(), getHeight());
            }
        }
    }
}
