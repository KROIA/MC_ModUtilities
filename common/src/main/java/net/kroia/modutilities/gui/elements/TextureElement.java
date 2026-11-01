package net.kroia.modutilities.gui.elements;

import net.kroia.modutilities.gui.GuiTexture;
import net.kroia.modutilities.gui.elements.base.GuiElement;

/**
 * Renders a {@link GuiTexture} (sprite or image asset) into the element's bounds.
 * <p>
 * The element supports two scaling {@linkplain Mode modes} ({@link Mode#STRETCH}
 * and {@link Mode#FILL}) and four rendering {@linkplain Layer layers}
 * ({@link Layer#BACKGROUND}, {@link Layer#FOREGROUND}, {@link Layer#TOOLTIP}, and
 * {@link Layer#GIZMO}) so the texture can participate in any phase of the GUI
 * render pipeline. This is commonly used for icons, panel backgrounds, and
 * decorative overlays.
 *
 * @apiNote Background and outline of the underlying {@link GuiElement} are
 *          disabled by default so the texture renders without a frame; re-enable
 *          them through the inherited setters if needed.
 */
public class TextureElement extends GuiElement {

    /**
     * Controls how the texture is fitted into the element's bounds.
     */
    public enum Mode
    {
        /** Stretches the texture to exactly cover the element's width and height. */
        STRETCH,
        /** Tiles the texture (or fills using the texture's own draw rules) to fill the area. */
        FILL
    }

    /**
     * Selects which render phase the texture is drawn during.
     */
    public enum Layer
    {
        /** Drawn during the background phase, behind the regular foreground content. */
        BACKGROUND,
        /** Drawn during the regular foreground render phase (default). */
        FOREGROUND,
        /** Drawn on top of all elements during the tooltip phase. */
        TOOLTIP,
        /** Drawn during the debug/gizmo phase (used for editor overlays). */
        GIZMO
    }
    private GuiTexture texture;
    private Mode mode = Mode.STRETCH;
    private Layer layer = Layer.FOREGROUND;

    /**
     * Creates an empty {@code TextureElement} with no texture assigned.
     * The texture must be set later via {@link #setTexture(GuiTexture)}.
     */
    public TextureElement()
    {
        this(null);
    }

    /**
     * Creates a {@code TextureElement} for the given texture, sizing the element
     * to match the texture's native dimensions when {@code texture} is non-null.
     *
     * @param texture the texture to render, or {@code null} to leave the element empty
     */
    public TextureElement(GuiTexture texture) {
        this.texture = texture;
        setEnableBackground(false);
        setEnableOutline(false);
        if(texture != null)
        {
            this.setSize(texture.getWidth(), texture.getHeight());
        }
    }

    /**
     * Convenience constructor that creates a {@link GuiTexture} from the given
     * mod ID and resource path and uses it for this element.
     *
     * @param modID       the namespace/mod ID of the texture resource
     * @param path        the resource path to the texture image
     * @param imageWidth  the native width of the texture image in pixels
     * @param imageHeight the native height of the texture image in pixels
     */
    public TextureElement(String modID, String path, int imageWidth, int imageHeight)
    {
        this(new GuiTexture(modID, path, imageWidth, imageHeight));
    }

    /**
     * Replaces the texture currently rendered by this element.
     *
     * @param texture the new texture, or {@code null} to render nothing
     */
    public void setTexture(GuiTexture texture) {
        this.texture = texture;
    }

    /**
     * @return the texture currently assigned, or {@code null} if none is set
     */
    public GuiTexture getTexture() {
        return texture;
    }

    /**
     * Sets the scaling mode used to render the texture into the element's bounds.
     *
     * @param mode the scaling mode
     */
    public void setMode(Mode mode) {
        this.mode = mode;
    }

    /**
     * @return the current scaling mode
     */
    public Mode getMode() {
        return mode;
    }

    /**
     * Sets the render phase during which the texture is drawn.
     *
     * @param layer the render layer
     */
    public void setLayer(Layer layer) {
        this.layer = layer;
    }

    /**
     * @return the render phase during which the texture is drawn
     */
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
