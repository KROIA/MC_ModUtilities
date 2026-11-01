package net.kroia.modutilities.gui.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
//import com.mojang.blaze3d.vertex.VertexSorting; // mc<=1.21.1
import com.mojang.blaze3d.ProjectionType; // mc>=1.21.2
//import net.minecraft.client.renderer.FogParameters; // mc>=1.21.2 mc<=1.21.5
import net.kroia.modutilities.ItemUtilities;
import net.kroia.modutilities.ModUtilitiesMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics; // mc<=1.21.11
//import net.minecraft.client.gui.GuiGraphicsExtractor; // mc>=26.1
import net.minecraft.client.renderer.MultiBufferSource; // mc<=26.1
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Renders shaped crafting recipes to PNG images using Minecraft's item renderer.
 * Produces pixel-perfect documentation images with correct 3D item models and lighting.
 * <p>
 * Must be called from the main client thread (render thread).
 * Typical usage: server command sends S2C packet, client handler calls export method.
 */
public class RecipeImageExporter {

    // Layout constants (logical GUI pixels)
    private static final int SLOT_SIZE = 18;
    private static final int ITEM_INSET = 1;
    private static final int SLOT_GAP = 2;
    private static final int PADDING = 8;
    private static final int ARROW_WIDTH = 24;
    private static final int ARROW_GAP = 8;

    // Slot colors (standard Minecraft inventory)
    private static final int SLOT_BG = 0xFF8B8B8B;
    private static final int SLOT_DARK = 0xFF373737;
    private static final int SLOT_LIGHT = 0xFFFFFFFF;
    private static final int BG_COLOR = 0xFFC6C6C6;
    private static final int ARROW_COLOR = 0xFF404040;

    private static final int DEFAULT_SCALE = 4;

    /**
     * Exports a shaped crafting recipe as a PNG image.
     *
     * @param grid       3x3 crafting grid (null or ItemStack.EMPTY for empty slots).
     *                   Outer array = rows, inner = columns. May be smaller than 3x3.
     * @param result     the recipe result
     * @param outputPath where to save the PNG
     * @param scale      pixel scale factor (each logical pixel = scale x scale output pixels)
     * @return true if export succeeded
     */
    public static boolean exportShapedRecipe(ItemStack[][] grid, ItemStack result,
                                             Path outputPath, int scale) {
        if (!RenderSystem.isOnRenderThread()) {
            ModUtilitiesMod.LOGGER.error("RecipeImageExporter.exportShapedRecipe must be called on the render thread");
            return false;
        }
        if (grid == null || result == null || outputPath == null) {
            ModUtilitiesMod.LOGGER.error("RecipeImageExporter: null argument");
            return false;
        }

        int guiW = computeGuiWidth();
        int guiH = computeGuiHeight();
        int texW = guiW * scale;
        int texH = guiH * scale;

        Minecraft mc = Minecraft.getInstance();

        // --- Save GL state (mirrors AbstractDisplayBlockEntityRenderer lines 253-279) ---
        RenderTarget prevTarget = ClientCompat.mainRenderTarget();
        //Matrix4f prevProjection = new Matrix4f(RenderSystem.getProjectionMatrix()); // mc<=1.21.5
        //VertexSorting prevSorting = RenderSystem.getVertexSorting(); // mc<=1.21.1
        //ProjectionType prevSorting = RenderSystem.getProjectionType(); // mc>=1.21.2 mc<=1.21.5
        //int[] prevViewport = new int[4]; // mc<=1.21.4
        //GL11.glGetIntegerv(GL11.GL_VIEWPORT, prevViewport); // mc<=1.21.4
        //boolean prevDepthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST); // mc<=1.21.4
        //boolean prevBlend = GL11.glIsEnabled(GL11.GL_BLEND); // mc<=1.21.4
        //boolean prevCull = GL11.glIsEnabled(GL11.GL_CULL_FACE); // mc<=1.21.4
        //float[] prevShaderColor = RenderSystem.getShaderColor().clone(); // mc<=1.21.5
        //float[] prevFogColor = new float[4]; // mc<=1.21.1
        //prevFogColor[0] = RenderSystem.getShaderFogColor()[0]; // mc<=1.21.1
        //prevFogColor[1] = RenderSystem.getShaderFogColor()[1]; // mc<=1.21.1
        //prevFogColor[2] = RenderSystem.getShaderFogColor()[2]; // mc<=1.21.1
        //prevFogColor[3] = RenderSystem.getShaderFogColor()[3]; // mc<=1.21.1
        //float prevFogStart = RenderSystem.getShaderFogStart(); // mc<=1.21.1
        //float prevFogEnd = RenderSystem.getShaderFogEnd(); // mc<=1.21.1
        //FogParameters prevFog = RenderSystem.getShaderFog(); // mc>=1.21.2 mc<=1.21.5

        TextureTarget framebuffer = null;
        NativeImage image = null;
        boolean success = false;

        try {
            // --- Create and bind framebuffer ---
            //framebuffer = new TextureTarget(texW, texH, true, false); // mc<=1.21.1
            //framebuffer = new TextureTarget(texW, texH, true); // mc>=1.21.2 mc<=1.21.4
            framebuffer = new TextureTarget("modutilities recipe export", texW, texH, true); // mc>=1.21.5 mc<=26.1
            //framebuffer = new TextureTarget("modutilities recipe export", texW, texH, true, com.mojang.blaze3d.GpuFormat.RGBA8_UNORM); // mc>=26.2 mc<=26.2
            //framebuffer = new TextureTarget("modutilities recipe export", texW, texH, com.mojang.renderpearl.api.GpuFormat.RGBA8_UNORM, com.mojang.renderpearl.api.GpuFormat.D32_FLOAT); // mc>=26.3
            //framebuffer.bindWrite(false); // mc<=1.21.4
            // 1.21.2+: GUI RenderTypes bind getMainRenderTarget() on every flush
            //((net.kroia.mixin.MinecraftAccessor) mc).modutilities$setMainRenderTarget(framebuffer); // mc>=1.21.2 mc<=1.21.5
            //RenderSystem.viewport(0, 0, texW, texH); // mc<=1.21.4

            float bgR = ((BG_COLOR >> 16) & 0xFF) / 255f;
            float bgG = ((BG_COLOR >> 8) & 0xFF) / 255f;
            float bgB = (BG_COLOR & 0xFF) / 255f;
            //RenderSystem.clearColor(bgR, bgG, bgB, 1.0f); // mc<=1.21.4
            //RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, false); // mc<=1.21.1
            //RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT); // mc>=1.21.2 mc<=1.21.4
            RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures( // mc>=1.21.5
                    framebuffer.getColorTexture(), 0xFF000000 | (BG_COLOR & 0xFFFFFF), framebuffer.getDepthTexture(), 1.0); // mc>=1.21.5 mc<=26.1
                    //framebuffer.getColorTexture(), net.minecraft.util.ARGB.vector4fFromARGB32(0xFF000000 | (BG_COLOR & 0xFFFFFF)), framebuffer.getDepthTexture(), 1.0); // mc>=26.2

            // --- Disable fog ---
            //RenderSystem.setShaderFogColor(0, 0, 0, 0); // mc<=1.21.1
            //RenderSystem.setShaderFogStart(Float.MAX_VALUE); // mc<=1.21.1
            //RenderSystem.setShaderFogEnd(Float.MAX_VALUE); // mc<=1.21.1
            //RenderSystem.setShaderFog(FogParameters.NO_FOG); // mc>=1.21.2 mc<=1.21.5

            // --- Set orthographic projection ---
            Matrix4f ortho = new Matrix4f().setOrtho(0, guiW, guiH, 0, 1000, 21000);
            //RenderSystem.setProjectionMatrix(ortho, VertexSorting.ORTHOGRAPHIC_Z); // mc<=1.21.1
            //RenderSystem.setProjectionMatrix(ortho, ProjectionType.ORTHOGRAPHIC); // mc>=1.21.2 mc<=1.21.5
            RenderSystem.getModelViewStack().pushMatrix();
            RenderSystem.getModelViewStack().identity();
            //RenderSystem.applyModelViewMatrix(); // mc<=1.21.1

            // --- Create GuiGraphics ---
            MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate( // mc<=26.1
                    new ByteBufferBuilder(786432)); // mc<=26.1
            //GuiGraphics guiGraphics = new GuiGraphics(mc, bufferSource); // mc<=1.21.5
            //guiGraphics.pose().pushPose(); // mc<=1.21.5
            //guiGraphics.pose().translate(0, 0, -11000); // mc<=1.21.5
            net.kroia.modutilities.gui.client.OffscreenGuiRenderer offscreen = new net.kroia.modutilities.gui.client.OffscreenGuiRenderer(bufferSource); // mc>=1.21.6 mc<=26.1
            //net.kroia.modutilities.gui.client.OffscreenGuiRenderer offscreen = new net.kroia.modutilities.gui.client.OffscreenGuiRenderer(); // mc>=26.2
            GuiGraphics guiGraphics = offscreen.begin(); // mc>=1.21.6 mc<=1.21.11
            //GuiGraphicsExtractor guiGraphics = offscreen.begin(); // mc>=26.1

            //Lighting.setupForFlatItems(); // mc<=1.21.5

            // --- Render recipe ---
            renderRecipe(guiGraphics, mc, grid, result, guiW, guiH);

            //guiGraphics.pose().popPose(); // mc<=1.21.5
            bufferSource.endBatch(); // mc<=26.1
            try { // mc>=1.21.6 (recorded GUI -> framebuffer)
                offscreen.render(guiGraphics, framebuffer, scale); // mc>=1.21.6
            } finally { // mc>=1.21.6
                offscreen.close(); // mc>=1.21.6
            } // mc>=1.21.6

            // --- Read pixels from framebuffer ---
            //image = new NativeImage(texW, texH, false); // mc<=1.21.4
            //RenderSystem.bindTexture(framebuffer.getColorTextureId()); // mc<=1.21.4
            //image.downloadTexture(0, false); // mc<=1.21.4
            //image.flipY(); // mc<=1.21.4

            // --- Save PNG ---
            //Path parent = outputPath.getParent(); // mc<=1.21.4
            //if (parent != null) { // mc<=1.21.4
            //    Files.createDirectories(parent); // mc<=1.21.4
            //} // mc<=1.21.4
            //image.writeToFile(outputPath); // mc<=1.21.4
            //ModUtilitiesMod.LOGGER.info("Recipe image exported to: {}", outputPath); // mc<=1.21.4
            // mc>=1.21.5: GPU readback is asynchronous; the PNG is written (and the target freed) in the callback.
            final TextureTarget target = framebuffer; // mc>=1.21.5
            framebuffer = null; // mc>=1.21.5
            net.minecraft.client.Screenshot.takeScreenshot(target, img -> { // mc>=1.21.5
                try (img) { // mc>=1.21.5
                    Path parent = outputPath.getParent(); // mc>=1.21.5
                    if (parent != null) Files.createDirectories(parent); // mc>=1.21.5
                    img.writeToFile(outputPath); // mc>=1.21.5
                    ModUtilitiesMod.LOGGER.info("Recipe image exported to: {}", outputPath); // mc>=1.21.5
                } catch (Exception e) { // mc>=1.21.5
                    ModUtilitiesMod.LOGGER.error("Failed to export recipe image to: {}", outputPath, e); // mc>=1.21.5
                } finally { // mc>=1.21.5
                    target.destroyBuffers(); // mc>=1.21.5
                } // mc>=1.21.5
            }); // mc>=1.21.5
            success = true;

        } catch (Exception e) {
            ModUtilitiesMod.LOGGER.error("Failed to export recipe image to: {}", outputPath, e);
        } finally {
            //((net.kroia.mixin.MinecraftAccessor) mc).modutilities$setMainRenderTarget(prevTarget); // mc>=1.21.2 mc<=1.21.5
            if (image != null) image.close();
            if (framebuffer != null) {
                //framebuffer.unbindWrite(); // mc<=1.21.4
                framebuffer.destroyBuffers();
            }

            // --- Restore GL state (mirrors AbstractDisplayBlockEntityRenderer lines 335-347) ---
            //prevTarget.bindWrite(false); // mc<=1.21.4
            RenderSystem.getModelViewStack().popMatrix();
            //RenderSystem.applyModelViewMatrix(); // mc<=1.21.1
            //RenderSystem.viewport(prevViewport[0], prevViewport[1], prevViewport[2], prevViewport[3]); // mc<=1.21.4
            //RenderSystem.setProjectionMatrix(prevProjection, prevSorting); // mc<=1.21.5
            //RenderSystem.setShaderColor(prevShaderColor[0], prevShaderColor[1], // mc<=1.21.5
                    //prevShaderColor[2], prevShaderColor[3]); // mc<=1.21.5
            //RenderSystem.setShaderFogColor(prevFogColor[0], prevFogColor[1], // mc<=1.21.1
            //        prevFogColor[2], prevFogColor[3]); // mc<=1.21.1
            //RenderSystem.setShaderFogStart(prevFogStart); // mc<=1.21.1
            //RenderSystem.setShaderFogEnd(prevFogEnd); // mc<=1.21.1
            //RenderSystem.setShaderFog(prevFog); // mc>=1.21.2 mc<=1.21.5
            //if (prevDepthTest) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest(); // mc<=1.21.4
            //if (prevBlend) RenderSystem.enableBlend(); else RenderSystem.disableBlend(); // mc<=1.21.4
            //if (prevCull) RenderSystem.enableCull(); else RenderSystem.disableCull(); // mc<=1.21.4
            //Lighting.setupLevel(); // mc<=1.21.5
        }

        return success;
    }

    /**
     * Exports a shaped crafting recipe as a PNG image with default scale (4x).
     */
    public static boolean exportShapedRecipe(ItemStack[][] grid, ItemStack result,
                                             Path outputPath) {
        return exportShapedRecipe(grid, result, outputPath, DEFAULT_SCALE);
    }

    /**
     * Exports a shaped crafting recipe using pattern strings and item ID maps.
     *
     * @param pattern     1-3 row strings (e.g. "ABA", " C "). Space = empty slot.
     * @param keyMap      character -> item ID (e.g. 'A' -> "minecraft:iron_ingot")
     * @param resultId    item ID of the result (e.g. "stockmarket:trading_software")
     * @param resultCount number of result items (shown as stack count decoration)
     * @param outputPath  where to save the PNG
     * @param scale       pixel scale factor
     * @return true if export succeeded
     */
    public static boolean exportShapedRecipe(String[] pattern, Map<Character, String> keyMap,
                                             String resultId, int resultCount,
                                             Path outputPath, int scale) {
        ItemStack[][] grid = patternToGrid(pattern, keyMap);
        ItemStack result = ItemUtilities.createItemStackFromId(resultId, resultCount);
        return exportShapedRecipe(grid, result, outputPath, scale);
    }

    /**
     * Exports a shaped crafting recipe using pattern strings with result count 1.
     */
    public static boolean exportShapedRecipe(String[] pattern, Map<Character, String> keyMap,
                                             String resultId, Path outputPath, int scale) {
        return exportShapedRecipe(pattern, keyMap, resultId, 1, outputPath, scale);
    }

    /**
     * Exports a shaped crafting recipe using pattern strings with default scale (4x).
     */
    public static boolean exportShapedRecipe(String[] pattern, Map<Character, String> keyMap,
                                             String resultId, int resultCount, Path outputPath) {
        return exportShapedRecipe(pattern, keyMap, resultId, resultCount, outputPath, DEFAULT_SCALE);
    }

    /**
     * Exports a shaped crafting recipe using pattern strings with result count 1 and default scale.
     */
    public static boolean exportShapedRecipe(String[] pattern, Map<Character, String> keyMap,
                                             String resultId, Path outputPath) {
        return exportShapedRecipe(pattern, keyMap, resultId, 1, outputPath, DEFAULT_SCALE);
    }

    /**
     * Converts pattern strings + key map into a 3x3 ItemStack grid.
     */
    public static ItemStack[][] patternToGrid(String[] pattern, Map<Character, String> keyMap) {
        ItemStack[][] grid = new ItemStack[3][3];
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                if (row < pattern.length && col < pattern[row].length()) {
                    char c = pattern[row].charAt(col);
                    if (c != ' ' && keyMap.containsKey(c)) {
                        grid[row][col] = ItemUtilities.createItemStackFromId(keyMap.get(c));
                    }
                }
            }
        }
        return grid;
    }

    // --- Private rendering ---

    private static void renderRecipe(GuiGraphics g, Minecraft mc, // mc<=1.21.11
    //private static void renderRecipe(GuiGraphicsExtractor g, Minecraft mc, // mc>=26.1
                                     ItemStack[][] grid, ItemStack result,
                                     int guiW, int guiH) {
        int gridW = 3 * SLOT_SIZE + 2 * SLOT_GAP;
        int gridH = gridW;

        // Background fill
        g.fill(0, 0, guiW, guiH, BG_COLOR);

        // 3x3 crafting grid
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int sx = PADDING + col * (SLOT_SIZE + SLOT_GAP);
                int sy = PADDING + row * (SLOT_SIZE + SLOT_GAP);
                renderSlot(g, sx, sy);

                ItemStack stack = getGridItem(grid, row, col);
                if (stack != null && !stack.isEmpty()) {
                    g.renderItem(stack, sx + ITEM_INSET, sy + ITEM_INSET); // mc<=1.21.11
                    //g.item(stack, sx + ITEM_INSET, sy + ITEM_INSET); // mc>=26.1
                }
            }
        }

        // Arrow
        int arrowX = PADDING + gridW + ARROW_GAP;
        int arrowCenterY = PADDING + gridH / 2;
        renderArrow(g, arrowX, arrowCenterY);

        // Result slot
        int resultX = arrowX + ARROW_WIDTH + ARROW_GAP;
        int resultY = PADDING + gridH / 2 - SLOT_SIZE / 2;
        renderSlot(g, resultX, resultY);
        if (!result.isEmpty()) {
            g.renderItem(result, resultX + ITEM_INSET, resultY + ITEM_INSET); // mc<=1.21.11
            //g.item(result, resultX + ITEM_INSET, resultY + ITEM_INSET); // mc>=26.1
            if (result.getCount() > 1) {
                g.renderItemDecorations(mc.font, result, resultX + ITEM_INSET, resultY + ITEM_INSET); // mc<=1.21.11
                //g.itemDecorations(mc.font, result, resultX + ITEM_INSET, resultY + ITEM_INSET); // mc>=26.1
            }
        }
    }

    private static void renderSlot(GuiGraphics g, int x, int y) { // mc<=1.21.11
    //private static void renderSlot(GuiGraphicsExtractor g, int x, int y) { // mc>=26.1
        g.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, SLOT_BG);
        // Dark border: top + left (inset effect)
        g.fill(x, y, x + SLOT_SIZE, y + 1, SLOT_DARK);
        g.fill(x, y, x + 1, y + SLOT_SIZE, SLOT_DARK);
        // Light border: bottom + right
        g.fill(x, y + SLOT_SIZE - 1, x + SLOT_SIZE, y + SLOT_SIZE, SLOT_LIGHT);
        g.fill(x + SLOT_SIZE - 1, y, x + SLOT_SIZE, y + SLOT_SIZE, SLOT_LIGHT);
    }

    private static void renderArrow(GuiGraphics g, int x, int centerY) { // mc<=1.21.11
    //private static void renderArrow(GuiGraphicsExtractor g, int x, int centerY) { // mc>=26.1
        int shaftH = 1;
        int shaftW = ARROW_WIDTH - 8;
        // Shaft
        g.fill(x, centerY - shaftH, x + shaftW, centerY + shaftH + 1, ARROW_COLOR);
        // Arrowhead (triangle via horizontal scanlines)
        int headBaseX = x + shaftW - 1;
        int headTipX = x + ARROW_WIDTH;
        int headHalfH = 5;
        for (int i = 0; i <= headTipX - headBaseX; i++) {
            float progress = (float) i / (headTipX - headBaseX);
            int h = (int) (headHalfH * (1.0f - progress));
            if (h < 0) h = 0;
            g.fill(headBaseX + i, centerY - h, headBaseX + i + 1, centerY + h + 1, ARROW_COLOR);
        }
    }

    private static ItemStack getGridItem(ItemStack[][] grid, int row, int col) {
        if (row >= grid.length) return null;
        if (grid[row] == null || col >= grid[row].length) return null;
        return grid[row][col];
    }

    private static int computeGuiWidth() {
        int gridW = 3 * SLOT_SIZE + 2 * SLOT_GAP;
        return PADDING + gridW + ARROW_GAP + ARROW_WIDTH + ARROW_GAP + SLOT_SIZE + PADDING;
    }

    private static int computeGuiHeight() {
        int gridH = 3 * SLOT_SIZE + 2 * SLOT_GAP;
        return PADDING + gridH + PADDING;
    }
}
