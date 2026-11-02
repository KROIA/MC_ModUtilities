package net.kroia.mixin;

// mc>=1.21.6 only (GuiRenderer item atlas was added in 1.21.6); registered in modutilities.mixins.json of the 1.21.6+ branches.

//import com.mojang.blaze3d.ProjectionType; // mc>=1.21.6 mc<=1.21.11
//import com.mojang.blaze3d.systems.CommandEncoder; // mc>=1.21.6 mc<=1.21.11
//import com.mojang.blaze3d.systems.RenderSystem; // mc>=1.21.6 mc<=1.21.11
//import com.mojang.blaze3d.textures.GpuTexture; // mc>=1.21.6 mc<=1.21.11
//import com.mojang.blaze3d.textures.GpuTextureView; // mc>=1.21.6 mc<=1.21.11
//import com.mojang.blaze3d.vertex.PoseStack; // mc>=1.21.6 mc<=1.21.11
//import net.minecraft.client.gui.render.state.GuiItemRenderState; // mc>=1.21.6 mc<=1.21.11
//import net.minecraft.client.gui.render.state.GuiRenderState; // mc>=1.21.6 mc<=1.21.11
//import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer; // mc>=1.21.6 mc<=1.21.11
//import net.minecraft.client.renderer.item.ItemStackRenderState; // mc>=1.21.6 mc<=1.21.6
//import net.minecraft.client.renderer.item.TrackingItemStackRenderState; // mc>=1.21.7 mc<=1.21.11
//import net.minecraft.client.gui.render.GuiItemAtlas; // mc>=26.1
//import net.minecraft.client.renderer.MultiBufferSource; // mc>=26.1 mc<=26.1
//import net.minecraft.client.renderer.SubmitNodeCollector; // mc>=26.1 mc<=26.1
//import net.minecraft.client.renderer.feature.FeatureRenderDispatcher; // mc>=26.1
//import net.minecraft.client.renderer.state.gui.GuiItemRenderState; // mc>=26.1
//import net.minecraft.client.renderer.state.gui.GuiRenderState; // mc>=26.1
//import net.kroia.modutilities.gui.client.ScaledGuiItemAtlas; // mc>=1.21.6 mc<=1.21.11
//import net.minecraft.client.gui.render.GuiRenderer; // mc>=1.21.6
//import org.joml.Matrix3x2fc; // mc>=1.21.6
//import org.spongepowered.asm.mixin.Final; // mc>=1.21.6
//import org.spongepowered.asm.mixin.Mixin; // mc>=1.21.6
//import org.spongepowered.asm.mixin.Shadow; // mc>=1.21.6
//import org.spongepowered.asm.mixin.Unique; // mc>=1.21.6
//import org.spongepowered.asm.mixin.injection.At; // mc>=1.21.6
//import org.spongepowered.asm.mixin.injection.Inject; // mc>=1.21.6
//import org.spongepowered.asm.mixin.injection.Redirect; // mc>=1.21.6
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfo; // mc>=1.21.6

//import java.util.HashMap; // mc>=1.21.6
//import java.util.HashSet; // mc>=26.1
//import java.util.Iterator; // mc>=1.21.6
//import java.util.LinkedHashMap; // mc>=1.21.6 mc<=1.21.11
//import java.util.Map; // mc>=1.21.6
//import java.util.Set; // mc>=26.1

/**
 * Sharp GUI items under a uniform pose scale (ModUtilities {@code Gui.setGuiScale}).
 * <p>
 * WHY: vanilla pre-renders every GUI item into one atlas at {@code 16 x guiScale} px and nearest-blits that
 * sprite through the item's pose. With a pose scale k the quad is {@code 16 x k x guiScale} px, so the sprite
 * is resampled (rows/columns dropped, e.g. a 64 px atlas slot drawn at 48 px). Items whose pose is a pure
 * uniform scale that maps one GUI unit to a whole number of pixels P != guiScale are additionally rendered
 * into a second atlas with {@code 16 x P} px slots and blitted from there, which is exactly 1:1.
 * Items at pose scale 1 (vanilla, JEI, ...) are untouched; if anything does not fit, the vanilla slot is used.
 * <p>
 * 26.x reuses vanilla's {@code GuiItemAtlas}. 1.21.6-1.21.11 keep a minimal own atlas (texture + slot grid)
 * per P and draw into it with vanilla's private {@code renderItemToAtlas} while the atlas fields are swapped.
 */
//@Mixin(GuiRenderer.class) // mc>=1.21.6
//public abstract class GuiRendererScaledItemsMixin { // mc>=1.21.6
    //@Shadow @Final private GuiRenderState renderState; // mc>=1.21.6
    //@Shadow private int getGuiScaleInvalidatingItemAtlasIfChanged() { return 0; } // mc>=1.21.6

    //@Unique private int modutilities$guiScale; // mc>=1.21.6

    /** Pixels per GUI unit for a scaled item, or 0 when vanilla's atlas already matches 1:1. */
    //@Unique // mc>=1.21.6
    //private static int modutilities$pixelsPerUnit(Matrix3x2fc pose, int guiScale) { // mc>=1.21.6
        //float k = pose.m00(); // mc>=1.21.6
        //if (Math.abs(pose.m01()) > 1e-4f || Math.abs(pose.m10()) > 1e-4f || Math.abs(pose.m11() - k) > 1e-4f) // mc>=1.21.6
            //return 0; // rotated / non-uniform: leave to vanilla // mc>=1.21.6
        //float px = k * guiScale; // mc>=1.21.6
        //int p = Math.round(px); // mc>=1.21.6
        //if (p < 1 || p == guiScale || Math.abs(px - p) > 1e-3f) // mc>=1.21.6
            //return 0; // mc>=1.21.6
        //return p; // mc>=1.21.6
    //} // mc>=1.21.6

    // ===================================================================== 1.21.6 - 1.21.11
    //@Shadow private GpuTexture itemsAtlas; // mc>=1.21.6 mc<=1.21.11
    //@Shadow private GpuTextureView itemsAtlasView; // mc>=1.21.6 mc<=1.21.11
    //@Shadow @Final private CachedOrthoProjectionMatrixBuffer itemsProjectionMatrixBuffer; // mc>=1.21.6 mc<=1.21.11
    //@Shadow private int frameNumber; // mc>=1.21.6 mc<=1.21.11
    //@Shadow private void renderItemToAtlas(ItemStackRenderState state, PoseStack pose, int x, int y, int size) {} // mc>=1.21.6 mc<=1.21.6
    //@Shadow private void renderItemToAtlas(TrackingItemStackRenderState state, PoseStack pose, int x, int y, int size) {} // mc>=1.21.7 mc<=1.21.11
    //@Shadow private void submitBlitFromItemAtlas(GuiItemRenderState state, float u, float v, int slotSize, int atlasSize) {} // mc>=1.21.6 mc<=1.21.11

    //@Unique private final Map<Integer, ScaledGuiItemAtlas> modutilities$scaledAtlases = new HashMap<>(); // mc>=1.21.6 mc<=1.21.11

    //@Redirect(method = "prepareItemElements", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;getGuiScaleInvalidatingItemAtlasIfChanged()I")) // mc>=1.21.6 mc<=1.21.11
    //private int modutilities$prepareScaledAtlases(GuiRenderer self) { // mc>=1.21.6 mc<=1.21.11
        //int guiScale = getGuiScaleInvalidatingItemAtlasIfChanged(); // mc>=1.21.6 mc<=1.21.11
        //modutilities$guiScale = guiScale; // mc>=1.21.6 mc<=1.21.11
        //Map<Integer, Map<Object, ItemStackRenderState>> items = new HashMap<>(); // mc>=1.21.6 mc<=1.21.6
        //Map<Integer, Map<Object, TrackingItemStackRenderState>> items = new HashMap<>(); // mc>=1.21.7 mc<=1.21.11
        //renderState.forEachItem(state -> { // mc>=1.21.6 mc<=1.21.11
            //int p = state.oversizedItemBounds() == null ? modutilities$pixelsPerUnit(state.pose(), guiScale) : 0; // mc>=1.21.6 mc<=1.21.11
            //if (p > 0) // mc>=1.21.6 mc<=1.21.11
                //items.computeIfAbsent(p, k -> new LinkedHashMap<>()).putIfAbsent(state.itemStackRenderState().getModelIdentity(), state.itemStackRenderState()); // mc>=1.21.6 mc<=1.21.11
        //}); // mc>=1.21.6 mc<=1.21.11
        //for (Iterator<Map.Entry<Integer, ScaledGuiItemAtlas>> it = modutilities$scaledAtlases.entrySet().iterator(); it.hasNext(); ) { // mc>=1.21.6 mc<=1.21.11
            //Map.Entry<Integer, ScaledGuiItemAtlas> e = it.next(); // mc>=1.21.6 mc<=1.21.11
            //if (!items.containsKey(e.getKey())) { // mc>=1.21.6 mc<=1.21.11
                //e.getValue().close(); // mc>=1.21.6 mc<=1.21.11
                //it.remove(); // mc>=1.21.6 mc<=1.21.11
            //} // mc>=1.21.6 mc<=1.21.11
        //} // mc>=1.21.6 mc<=1.21.11
        //if (items.isEmpty()) // mc>=1.21.6 mc<=1.21.11
            //return guiScale; // mc>=1.21.6 mc<=1.21.11

        //GpuTexture vanillaAtlas = itemsAtlas; // mc>=1.21.6 mc<=1.21.11
        //GpuTextureView colorOverride = RenderSystem.outputColorTextureOverride; // mc>=1.21.6 mc<=1.21.11
        //GpuTextureView depthOverride = RenderSystem.outputDepthTextureOverride; // mc>=1.21.6 mc<=1.21.11
        //PoseStack poseStack = new PoseStack(); // mc>=1.21.6 mc<=1.21.11
        //try { // mc>=1.21.6 mc<=1.21.11
            //for (var e : items.entrySet()) { // mc>=1.21.6 mc<=1.21.11
                //int slot = 16 * e.getKey(); // mc>=1.21.6 mc<=1.21.11
                //var frameItems = e.getValue(); // mc>=1.21.6 mc<=1.21.11
                //ScaledGuiItemAtlas atlas = modutilities$scaledAtlases.get(e.getKey()); // mc>=1.21.6 mc<=1.21.11
                //int newCells = 0; // mc>=1.21.6 mc<=1.21.11
                //for (Object id : frameItems.keySet()) // mc>=1.21.6 mc<=1.21.11
                    //if (atlas == null || !atlas.cells.containsKey(id)) newCells++; // mc>=1.21.6 mc<=1.21.11
                //if (atlas == null || atlas.next + newCells > atlas.capacity()) { // mc>=1.21.6 mc<=1.21.11
                    // Full: start over with a texture that holds at least this frame's items (like vanilla's invalidation).
                    //int perRow = (int) Math.ceil(Math.sqrt(frameItems.size())); // mc>=1.21.6 mc<=1.21.11
                    //int size = Math.max(512, Integer.highestOneBit(Math.max(1, perRow * slot - 1)) << 1); // mc>=1.21.6 mc<=1.21.11
                    //if (size > RenderSystem.getDevice().getMaxTextureSize()) // mc>=1.21.6 mc<=1.21.11
                        //continue; // too many: vanilla slots are used // mc>=1.21.6 mc<=1.21.11
                    //if (atlas != null) // mc>=1.21.6 mc<=1.21.11
                        //atlas.close(); // mc>=1.21.6 mc<=1.21.11
                    //atlas = new ScaledGuiItemAtlas(slot, size); // mc>=1.21.6 mc<=1.21.11
                    //modutilities$scaledAtlases.put(e.getKey(), atlas); // mc>=1.21.6 mc<=1.21.11
                //} // mc>=1.21.6 mc<=1.21.11
                //itemsAtlas = atlas.color; // renderItemToAtlas scissors against itemsAtlas' height // mc>=1.21.6 mc<=1.21.11
                //RenderSystem.outputColorTextureOverride = atlas.colorView; // mc>=1.21.6 mc<=1.21.11
                //RenderSystem.outputDepthTextureOverride = atlas.depthView; // mc>=1.21.6 mc<=1.21.11
                //RenderSystem.setProjectionMatrix(itemsProjectionMatrixBuffer.getBuffer(atlas.size, atlas.size), ProjectionType.ORTHOGRAPHIC); // mc>=1.21.6 mc<=1.21.11
                //CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder(); // mc>=1.21.6 mc<=1.21.11
                //int perRow = atlas.size / slot; // mc>=1.21.6 mc<=1.21.11
                //for (var item : frameItems.entrySet()) { // mc>=1.21.6 mc<=1.21.11
                    //int[] cell = atlas.cells.get(item.getKey()); // mc>=1.21.6 mc<=1.21.11
                    //if (cell != null) // mc>=1.21.6 mc<=1.21.11
                        //cell[3] = frameNumber; // mc>=1.21.6 mc<=1.21.11
                    //if (cell == null) { // mc>=1.21.6 mc<=1.21.11
                        //cell = new int[]{(atlas.next % perRow) * slot, (atlas.next / perRow) * slot, -1, -1}; // mc>=1.21.6 mc<=1.21.11
                        //atlas.next++; // mc>=1.21.6 mc<=1.21.11
                        //atlas.cells.put(item.getKey(), cell); // mc>=1.21.6 mc<=1.21.11
                    //} else if (!item.getValue().isAnimated() || cell[2] == frameNumber) { // mc>=1.21.6 mc<=1.21.11
                        //continue; // cached // mc>=1.21.6 mc<=1.21.11
                    //} else { // mc>=1.21.6 mc<=1.21.11
                        //encoder.clearColorAndDepthTextures(atlas.color, 0, atlas.depth, 1.0, cell[0], atlas.size - cell[1] - slot, slot, slot); // mc>=1.21.6 mc<=1.21.11
                    //} // mc>=1.21.6 mc<=1.21.11
                    //renderItemToAtlas(item.getValue(), poseStack, cell[0], cell[1], slot); // mc>=1.21.6 mc<=1.21.11
                    //cell[2] = frameNumber; // mc>=1.21.6 mc<=1.21.11
                    //cell[3] = frameNumber; // mc>=1.21.6 mc<=1.21.11
                //} // mc>=1.21.6 mc<=1.21.11
            //} // mc>=1.21.6 mc<=1.21.11
        //} finally { // mc>=1.21.6 mc<=1.21.11
            //itemsAtlas = vanillaAtlas; // mc>=1.21.6 mc<=1.21.11
            //RenderSystem.outputColorTextureOverride = colorOverride; // mc>=1.21.6 mc<=1.21.11
            //RenderSystem.outputDepthTextureOverride = depthOverride; // mc>=1.21.6 mc<=1.21.11
        //} // mc>=1.21.6 mc<=1.21.11
        //return guiScale; // vanilla sets its own projection/overrides right after this call // mc>=1.21.6 mc<=1.21.11
    //} // mc>=1.21.6 mc<=1.21.11

    //@Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;submitBlitFromItemAtlas(Lnet/minecraft/client/gui/render/state/GuiItemRenderState;FFII)V")) // mc>=1.21.6 mc<=1.21.11
    //private void modutilities$blitScaledItem(GuiRenderer self, GuiItemRenderState state, float u, float v, int slotSize, int atlasSize) { // mc>=1.21.6 mc<=1.21.11
        //int p = modutilities$pixelsPerUnit(state.pose(), modutilities$guiScale); // mc>=1.21.6 mc<=1.21.11
        //ScaledGuiItemAtlas atlas = p > 0 ? modutilities$scaledAtlases.get(p) : null; // mc>=1.21.6 mc<=1.21.11
        //int[] cell = atlas != null ? atlas.cells.get(state.itemStackRenderState().getModelIdentity()) : null; // mc>=1.21.6 mc<=1.21.11
        //if (cell == null || cell[3] != frameNumber) { // not prepared this frame: vanilla slot // mc>=1.21.6 mc<=1.21.11
            //submitBlitFromItemAtlas(state, u, v, slotSize, atlasSize); // mc>=1.21.6 mc<=1.21.11
            //return; // mc>=1.21.6 mc<=1.21.11
        //} // mc>=1.21.6 mc<=1.21.11
        //GpuTextureView vanillaView = itemsAtlasView; // mc>=1.21.6 mc<=1.21.11
        //itemsAtlasView = atlas.colorView; // submitBlitFromItemAtlas binds itemsAtlasView // mc>=1.21.6 mc<=1.21.11
        //try { // mc>=1.21.6 mc<=1.21.11
            //submitBlitFromItemAtlas(state, (float) cell[0] / atlas.size, (float) (atlas.size - cell[1]) / atlas.size, atlas.slot, atlas.size); // mc>=1.21.6 mc<=1.21.11
        //} finally { // mc>=1.21.6 mc<=1.21.11
            //itemsAtlasView = vanillaView; // mc>=1.21.6 mc<=1.21.11
        //} // mc>=1.21.6 mc<=1.21.11
    //} // mc>=1.21.6 mc<=1.21.11

    //@Inject(method = "close", at = @At("TAIL")) // mc>=1.21.6 mc<=1.21.11
    //private void modutilities$close(CallbackInfo ci) { // mc>=1.21.6 mc<=1.21.11
        //for (ScaledGuiItemAtlas atlas : modutilities$scaledAtlases.values()) // mc>=1.21.6 mc<=1.21.11
            //atlas.close(); // mc>=1.21.6 mc<=1.21.11
        //modutilities$scaledAtlases.clear(); // mc>=1.21.6 mc<=1.21.11
    //} // mc>=1.21.6 mc<=1.21.11

    // ===================================================================== 26.x
    //@Shadow @Final private FeatureRenderDispatcher featureRenderDispatcher; // mc>=26.1
    //@Shadow @Final private SubmitNodeCollector submitNodeCollector; // mc>=26.1 mc<=26.1
    //@Shadow @Final private MultiBufferSource.BufferSource bufferSource; // mc>=26.1 mc<=26.1
    //@Shadow private void submitBlitFromItemAtlas(GuiItemRenderState state, GuiItemAtlas.SlotView slot) {} // mc>=26.1

    /** Extra atlases keyed by pixels per GUI unit. */
    //@Unique private final Map<Integer, GuiItemAtlas> modutilities$scaledAtlases = new HashMap<>(); // mc>=26.1

    //@Redirect(method = "prepareItemElements", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;getGuiScaleInvalidatingItemAtlasIfChanged()I")) // mc>=26.1
    //private int modutilities$prepareScaledAtlases(GuiRenderer self) { // mc>=26.1
        //int guiScale = getGuiScaleInvalidatingItemAtlasIfChanged(); // mc>=26.1
        //modutilities$guiScale = guiScale; // mc>=26.1
        //Map<Integer, Set<Object>> identities = new HashMap<>(); // mc>=26.1
        //renderState.forEachItem(state -> { // mc>=26.1
            //int p = state.oversizedItemBounds() == null ? modutilities$pixelsPerUnit(state.pose(), guiScale) : 0; // mc>=26.1
            //if (p > 0) // mc>=26.1
                //identities.computeIfAbsent(p, k -> new HashSet<>()).add(state.itemStackRenderState().getModelIdentity()); // mc>=26.1
        //}); // mc>=26.1
        // Drop atlases no longer used this frame (screen closed / scale changed).
        //for (Iterator<Map.Entry<Integer, GuiItemAtlas>> it = modutilities$scaledAtlases.entrySet().iterator(); it.hasNext(); ) { // mc>=26.1
            //Map.Entry<Integer, GuiItemAtlas> e = it.next(); // mc>=26.1
            //if (!identities.containsKey(e.getKey())) { // mc>=26.1
                //e.getValue().close(); // mc>=26.1
                //it.remove(); // mc>=26.1
            //} // mc>=26.1
        //} // mc>=26.1
        //for (Map.Entry<Integer, Set<Object>> e : identities.entrySet()) { // mc>=26.1
            //int slotSize = 16 * e.getKey(); // mc>=26.1
            //GuiItemAtlas atlas = modutilities$scaledAtlases.get(e.getKey()); // mc>=26.1
            //if (atlas != null && atlas.tryPrepareFor(e.getValue())) // mc>=26.1
                //continue; // mc>=26.1
            //int textureSize = GuiItemAtlas.computeTextureSizeFor(slotSize, e.getValue().size()); // mc>=26.1
            //if (atlas != null && atlas.textureSize() == textureSize) // mc>=26.1
                //continue; // at max size: getOrUpdate returns null for overflow, vanilla atlas is used // mc>=26.1
            //if (atlas != null) // mc>=26.1
                //atlas.close(); // mc>=26.1
            //atlas = new GuiItemAtlas(submitNodeCollector, featureRenderDispatcher, bufferSource, textureSize, slotSize); // mc>=26.1 mc<=26.1
            //atlas = new GuiItemAtlas(featureRenderDispatcher, textureSize, slotSize); // mc>=26.2
            //atlas.tryPrepareFor(e.getValue()); // mc>=26.1
            //modutilities$scaledAtlases.put(e.getKey(), atlas); // mc>=26.1
        //} // mc>=26.1
        //return guiScale; // mc>=26.1
    //} // mc>=26.1

    //@Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;submitBlitFromItemAtlas(Lnet/minecraft/client/renderer/state/gui/GuiItemRenderState;Lnet/minecraft/client/gui/render/GuiItemAtlas$SlotView;)V")) // mc>=26.1
    //private void modutilities$blitScaledItem(GuiRenderer self, GuiItemRenderState state, GuiItemAtlas.SlotView slot) { // mc>=26.1
        //int p = modutilities$pixelsPerUnit(state.pose(), modutilities$guiScale); // mc>=26.1
        //GuiItemAtlas atlas = p > 0 ? modutilities$scaledAtlases.get(p) : null; // mc>=26.1
        //if (atlas != null) { // mc>=26.1
            //GuiItemAtlas.SlotView scaled = atlas.getOrUpdate(state.itemStackRenderState()); // mc>=26.1
            //if (scaled != null) // mc>=26.1
                //slot = scaled; // mc>=26.1
        //} // mc>=26.1
        //submitBlitFromItemAtlas(state, slot); // mc>=26.1
    //} // mc>=26.1

    //@Inject(method = "endFrame", at = @At("TAIL")) // mc>=26.1
    //private void modutilities$endFrame(CallbackInfo ci) { // mc>=26.1
        //for (GuiItemAtlas atlas : modutilities$scaledAtlases.values()) // mc>=26.1
            //atlas.endFrame(); // mc>=26.1
    //} // mc>=26.1

    //@Inject(method = "close", at = @At("TAIL")) // mc>=26.1
    //private void modutilities$close(CallbackInfo ci) { // mc>=26.1
        //for (GuiItemAtlas atlas : modutilities$scaledAtlases.values()) // mc>=26.1
            //atlas.close(); // mc>=26.1
        //modutilities$scaledAtlases.clear(); // mc>=26.1
    //} // mc>=26.1
//} // mc>=1.21.6
