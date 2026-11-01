package net.kroia.modutilities.gui.display.client;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.ProjectionType; // mc>=1.21.2
import net.minecraft.client.renderer.FogParameters; // mc>=1.21.2 mc<=1.21.5
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.kroia.modutilities.gui.Gui;
import net.kroia.modutilities.gui.client.ClientGraphics;
import net.kroia.modutilities.gui.display.AbstractDisplayBlockEntity;
import net.kroia.modutilities.gui.display.DisplayConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class AbstractDisplayBlockEntityRenderer<T extends AbstractDisplayBlockEntity>
        implements BlockEntityRenderer<T> {

    private final ClientGraphics clientGraphics;
    private MultiBufferSource.BufferSource offscreenBufferSource;
    private GuiGraphics cachedGuiGraphics;
    //private net.kroia.modutilities.gui.client.OffscreenGuiRenderer offscreenGui; // mc>=1.21.6 (deferred GUI rendering into the display texture)

    private static class GroupRenderData {
        DynamicTexture texture;
        ResourceLocation location;
        TextureTarget framebuffer;
        boolean layoutInitialized;
        int groupWidth;
        int groupHeight;
        int texWidth;
        int texHeight;
        long lastRenderedFrame = -1;
        int lastMouseX = -1;
        int lastMouseY = -1;
    }

    // Static so multiple renderer instances (one per BE type) share one texture per
    // group. Otherwise mixed-type groups collide on the shared TextureManager location
    // and render into different textures, only one of which is actually shown.
    private static final Map<BlockPos, GroupRenderData> groupData = new HashMap<>();

    public AbstractDisplayBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.clientGraphics = new ClientGraphics();
        clientGraphics.setFont(Minecraft.getInstance().font);
        Gui.setFallbackGraphics(clientGraphics);
    }

    @Override // mc<=1.21.5
    public boolean shouldRenderOffScreen(T blockEntity) { // mc<=1.21.5
    //@Override // mc>=1.21.6
    //public boolean shouldRenderOffScreen() { // mc>=1.21.6 (no block entity parameter any more)
        return true;
    }

    private void ensureBufferSource() {
        if (offscreenBufferSource == null) {
            // Fixed buffers for glint render types so enchanted items can render
            // without the glint buffer flushing the base item buffer
            java.util.SequencedMap<RenderType, com.mojang.blaze3d.vertex.ByteBufferBuilder> fixedBuffers = new java.util.LinkedHashMap<>();
            fixedBuffers.put(RenderType.glint(), new com.mojang.blaze3d.vertex.ByteBufferBuilder(256));
            fixedBuffers.put(RenderType.entityGlint(), new com.mojang.blaze3d.vertex.ByteBufferBuilder(256));
            //fixedBuffers.put(RenderType.entityGlintDirect(), new com.mojang.blaze3d.vertex.ByteBufferBuilder(256)); // mc<=1.21.1 (removed in 1.21.2)
            fixedBuffers.put(RenderType.armorEntityGlint(), new com.mojang.blaze3d.vertex.ByteBufferBuilder(256));
            offscreenBufferSource = MultiBufferSource.immediateWithBuffers(
                    fixedBuffers, new com.mojang.blaze3d.vertex.ByteBufferBuilder(786432));
        }
    }

    private static int effectiveRenderScale(int gw, int gh, DisplayConfig config) {
        int scale = config.renderScale();
        int maxDim = config.maxTextureDim();
        while (scale > 1
                && (gw * config.virtualWidth() * scale > maxDim
                || gh * config.virtualHeight() * scale > maxDim)) {
            scale--;
        }
        return scale;
    }

    private GroupRenderData getOrCreateGroupData(BlockPos controllerPos, int gw, int gh,
                                                 DisplayConfig config) {
        GroupRenderData data = groupData.get(controllerPos);

        int scale = effectiveRenderScale(gw, gh, config);
        int texW = gw * config.virtualWidth() * scale;
        int texH = gh * config.virtualHeight() * scale;

        if (data != null && (data.groupWidth != gw || data.groupHeight != gh)) {
            Minecraft.getInstance().getTextureManager().release(data.location);
            if (data.framebuffer != null) data.framebuffer.destroyBuffers();
            data = null;
            groupData.remove(controllerPos);
        }

        if (data == null) {
            //int prevTexture = org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL11.GL_TEXTURE_BINDING_2D); // mc<=1.21.4
            //int[] prevViewport = new int[4]; // mc<=1.21.4
            //org.lwjgl.opengl.GL11.glGetIntegerv(org.lwjgl.opengl.GL11.GL_VIEWPORT, prevViewport); // mc<=1.21.4
            //com.mojang.blaze3d.pipeline.RenderTarget prevTarget = Minecraft.getInstance().getMainRenderTarget(); // mc<=1.21.4

            data = new GroupRenderData();
            data.groupWidth = gw;
            data.groupHeight = gh;
            data.texWidth = texW;
            data.texHeight = texH;
            //data.texture = new DynamicTexture(texW, texH, false); // mc<=1.21.4
            //RenderSystem.bindTexture(data.texture.getId()); // mc<=1.21.4
            //org.lwjgl.opengl.GL11.glTexImage2D(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, 0, // mc<=1.21.4
            //        org.lwjgl.opengl.GL11.GL_RGBA, texW, texH, 0, // mc<=1.21.4
            //        org.lwjgl.opengl.GL11.GL_RGBA, org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE, // mc<=1.21.4
            //        (java.nio.ByteBuffer) null); // mc<=1.21.4
            //org.lwjgl.opengl.GL11.glTexParameteri(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, // mc<=1.21.4
            //        org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER, org.lwjgl.opengl.GL11.GL_NEAREST); // mc<=1.21.4
            //org.lwjgl.opengl.GL11.glTexParameteri(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, // mc<=1.21.4
            //        org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER, org.lwjgl.opengl.GL11.GL_NEAREST); // mc<=1.21.4
            data.texture = new DynamicTexture(() -> "modutilities display " + controllerPos.toShortString(), texW, texH, false); // mc>=1.21.5
            data.texture.setFilter(false, false); // mc>=1.21.5 (nearest, no mipmaps)
            //data.framebuffer = new TextureTarget(texW, texH, true, false); // mc<=1.21.1
            //data.framebuffer = new TextureTarget(texW, texH, true); // mc>=1.21.2 mc<=1.21.4
            data.framebuffer = new TextureTarget("modutilities display framebuffer", texW, texH, true); // mc>=1.21.5
            data.location = ResourceLocation.fromNamespaceAndPath("modutilities",
                    "dynamic/display_group_" + controllerPos.getX() + "_"
                            + controllerPos.getY() + "_" + controllerPos.getZ());
            Minecraft.getInstance().getTextureManager().register(data.location, data.texture);
            data.layoutInitialized = false;
            groupData.put(controllerPos, data);

            //prevTarget.bindWrite(false); // mc<=1.21.4
            //RenderSystem.viewport(prevViewport[0], prevViewport[1], prevViewport[2], prevViewport[3]); // mc<=1.21.4
            //RenderSystem.bindTexture(prevTexture); // mc<=1.21.4
        }

        return data;
    }

    @Override
    //public void render(T blockEntity, float partialTick, PoseStack poseStack, // mc<=1.21.4
    //                   MultiBufferSource bufferSource, int packedLight, int packedOverlay) { // mc<=1.21.4
    public void render(T blockEntity, float partialTick, PoseStack poseStack, // mc>=1.21.5
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay, net.minecraft.world.phys.Vec3 cameraPos) { // mc>=1.21.5

        if (!blockEntity.isActive()) return;

        BlockPos controllerPos = blockEntity.getControllerPos();
        if (controllerPos == null) return;

        DisplayRenderProfiler.begin(controllerPos, DisplayRenderProfiler.Category.TOTAL);

        // Resolve the controller BE once. Config, group dimensions, and the gui
        // must all come from the controller — never from the block being rendered
        // — so that mixed-BE-type groups behave deterministically (controller wins).
        AbstractDisplayBlockEntity controller = null;
        if (blockEntity.isController()) {
            controller = blockEntity;
        } else if (blockEntity.getLevel() != null) {
            BlockEntity controllerBE = blockEntity.getLevel().getBlockEntity(controllerPos);
            if (controllerBE instanceof AbstractDisplayBlockEntity ctrl) {
                controller = ctrl;
            }
        }
        if (controller == null) {
            DisplayRenderProfiler.end(controllerPos, DisplayRenderProfiler.Category.TOTAL);
            return;
        }

        DisplayConfig config = controller.getDisplayConfig();
        int gw = controller.getGroupWidth();
        int gh = controller.getGroupHeight();

        GroupRenderData data = getOrCreateGroupData(controllerPos, gw, gh, config);

        long currentFrame = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.getGameTime() : 0;
        if (data.lastRenderedFrame < currentFrame) {
            Gui gui = controller.getGui();
            if (gui != null) {
                updateClientMousePos(gui, controllerPos, config);

                boolean skipRender = false;

                int maxDist = config.maxRenderDistance();
                // Only apply the distance cutoff AFTER the first render has produced
                // content in the texture. Skipping the initial render would leave the
                // GPU texture uninitialized (renders as black) until the player walks
                // within range — with this guard, beyond-distance blocks show the last
                // known frame instead.
                if (maxDist > 0 && data.layoutInitialized && Minecraft.getInstance().player != null) {
                    double distSq = Minecraft.getInstance().player.blockPosition().distSqr(controllerPos);
                    if (distSq > (long) maxDist * maxDist) {
                        skipRender = true;
                    }
                }

                if (!skipRender && config.renderInterval() > 1
                        && data.layoutInitialized
                        && currentFrame % config.renderInterval() != 0) {
                    skipRender = true;
                }

                if (!skipRender) {
                    boolean needsRender = !data.layoutInitialized
                            || gui.hasAnyDirty()
                            || gui.getMousePosX() != data.lastMouseX
                            || gui.getMousePosY() != data.lastMouseY;

                    if (needsRender) {
                        gui.clearAllDirty();
                        renderGuiToTexture(gui, partialTick, data, config, controllerPos);
                        data.lastMouseX = gui.getMousePosX();
                        data.lastMouseY = gui.getMousePosY();
                    }
                }
                data.lastRenderedFrame = currentFrame;
            }
        }

        Direction facing = blockEntity.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        int gx = blockEntity.getGridX();
        int gy = blockEntity.getGridY();

        float u0 = (float) gx / gw;
        float u1 = (float) (gx + 1) / gw;
        float v0 = 1.0f - (float) (gy + 1) / gh;
        float v1 = 1.0f - (float) gy / gh;

        DisplayRenderProfiler.begin(controllerPos, DisplayRenderProfiler.Category.QUAD_RENDER);
        renderQuadOnFace(poseStack, bufferSource, facing, data.location,
                u0, v0, u1, v1, config.faceOffset());
        DisplayRenderProfiler.end(controllerPos, DisplayRenderProfiler.Category.QUAD_RENDER);

        DisplayRenderProfiler.end(controllerPos, DisplayRenderProfiler.Category.TOTAL);
    }

    private void updateClientMousePos(Gui gui, BlockPos controllerPos, DisplayConfig config) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        if (mc.hitResult instanceof BlockHitResult blockHit
                && blockHit.getType() == HitResult.Type.BLOCK) {
            BlockEntity hitBE = mc.level.getBlockEntity(blockHit.getBlockPos());
            if (hitBE instanceof AbstractDisplayBlockEntity hitDisplay
                    && hitDisplay.isActive()
                    && controllerPos.equals(hitDisplay.getControllerPos())) {
                Direction hitFacing = mc.level.getBlockState(blockHit.getBlockPos())
                        .getValue(HorizontalDirectionalBlock.FACING);
                double[] coords = AbstractDisplayBlockEntity.computeGuiCoordsFromHit(
                        blockHit, blockHit.getBlockPos(), hitFacing, hitDisplay);
                if (coords != null) {
                    gui.storeMousePos((int) coords[0], (int) coords[1]);
                    return;
                }
            }
        }
        gui.storeMousePos(-1, -1);
    }

    private void renderGuiToTexture(Gui gui, float partialTick, GroupRenderData data,
                                    DisplayConfig config, BlockPos controllerPos) {
        Minecraft mc = Minecraft.getInstance();

        int texW = data.texWidth;
        int texH = data.texHeight;
        int guiW = data.groupWidth * config.virtualWidth();
        int guiH = data.groupHeight * config.virtualHeight();

        ensureBufferSource();

        com.mojang.blaze3d.pipeline.RenderTarget prevTarget = mc.getMainRenderTarget();
        Matrix4f prevProjection = new Matrix4f(RenderSystem.getProjectionMatrix()); // mc<=1.21.5
        //VertexSorting prevSorting = RenderSystem.getVertexSorting(); // mc<=1.21.1
        ProjectionType prevSorting = RenderSystem.getProjectionType(); // mc>=1.21.2 mc<=1.21.5
        //int[] prevViewport = new int[4]; // mc<=1.21.4
        //org.lwjgl.opengl.GL11.glGetIntegerv(org.lwjgl.opengl.GL11.GL_VIEWPORT, prevViewport); // mc<=1.21.4
        //boolean prevDepthTest = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_DEPTH_TEST); // mc<=1.21.4
        //boolean prevBlend = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_BLEND); // mc<=1.21.4
        //boolean prevCull = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_CULL_FACE); // mc<=1.21.4

        //data.framebuffer.bindWrite(false); // mc<=1.21.4
        //RenderSystem.viewport(0, 0, texW, texH); // mc<=1.21.4
        //RenderSystem.clearColor(0.15f, 0.15f, 0.15f, 1.0f); // mc<=1.21.4
        //RenderSystem.clear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT // mc<=1.21.1
        //        | org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT, false); // mc<=1.21.1
        //RenderSystem.clear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT // mc>=1.21.2 mc<=1.21.4
        //        | org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT); // mc>=1.21.2 mc<=1.21.4
        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures( // mc>=1.21.5
                data.framebuffer.getColorTexture(), 0xFF262626, data.framebuffer.getDepthTexture(), 1.0); // mc>=1.21.5 (0.15 gray)

        float[] prevShaderColor = RenderSystem.getShaderColor().clone(); // mc<=1.21.5

        //float[] prevFogColor = new float[4]; // mc<=1.21.1
        //prevFogColor[0] = RenderSystem.getShaderFogColor()[0]; // mc<=1.21.1
        //prevFogColor[1] = RenderSystem.getShaderFogColor()[1]; // mc<=1.21.1
        //prevFogColor[2] = RenderSystem.getShaderFogColor()[2]; // mc<=1.21.1
        //prevFogColor[3] = RenderSystem.getShaderFogColor()[3]; // mc<=1.21.1
        //float prevFogStart = RenderSystem.getShaderFogStart(); // mc<=1.21.1
        //float prevFogEnd = RenderSystem.getShaderFogEnd(); // mc<=1.21.1
        //RenderSystem.setShaderFogColor(0, 0, 0, 0); // mc<=1.21.1
        //RenderSystem.setShaderFogStart(Float.MAX_VALUE); // mc<=1.21.1
        //RenderSystem.setShaderFogEnd(Float.MAX_VALUE); // mc<=1.21.1
        FogParameters prevFog = RenderSystem.getShaderFog(); // mc>=1.21.2 mc<=1.21.5
        RenderSystem.setShaderFog(FogParameters.NO_FOG); // mc>=1.21.2 mc<=1.21.5

        Matrix4f ortho = new Matrix4f().setOrtho(0, guiW, guiH, 0, 1000, 21000);
        //RenderSystem.setProjectionMatrix(ortho, VertexSorting.ORTHOGRAPHIC_Z); // mc<=1.21.1
        RenderSystem.setProjectionMatrix(ortho, ProjectionType.ORTHOGRAPHIC); // mc>=1.21.2 mc<=1.21.5
        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().identity();
        //RenderSystem.applyModelViewMatrix(); // mc<=1.21.1

        if (cachedGuiGraphics == null) { // mc<=1.21.5
            cachedGuiGraphics = new GuiGraphics(mc, offscreenBufferSource); // mc<=1.21.5
        } // mc<=1.21.5
        GuiGraphics guiGraphics = cachedGuiGraphics; // mc<=1.21.5
        guiGraphics.pose().pushPose(); // mc<=1.21.5
        guiGraphics.pose().translate(0, 0, -11000); // mc<=1.21.5
        //if (offscreenGui == null) offscreenGui = new net.kroia.modutilities.gui.client.OffscreenGuiRenderer(offscreenBufferSource); // mc>=1.21.6
        //GuiGraphics guiGraphics = offscreenGui.begin(); // mc>=1.21.6

        gui.setGraphicsBackend(clientGraphics);
        gui.setFont(mc.font);
        clientGraphics.setGraphics(guiGraphics);
        clientGraphics.setFont(mc.font);
        clientGraphics.setEnableShadow(false);
        gui.setPartialTick(partialTick);

        if (!data.layoutInitialized) {
            for (int pass = 0; pass < 3; pass++) {
                gui.init();
            }
            data.layoutInitialized = true;
        }

        int scale = texW / guiW;
        gui.setOffscreenScissorMode(scale, texH); // mc<=1.21.5 (1.21.6: the GUI renderer scissors in target pixels itself)

        Lighting.setupForFlatItems(); // mc<=1.21.5

        // 1.21.2+: GUI RenderTypes bind getMainRenderTarget() on every flush, so the
        // offscreen target must be installed as the main target while drawing.
        ((net.kroia.mixin.MinecraftAccessor) mc).modutilities$setMainRenderTarget(data.framebuffer); // mc>=1.21.2 mc<=1.21.5
        try {
            DisplayRenderProfiler.begin(controllerPos, DisplayRenderProfiler.Category.GUI_RENDER);
            gui.renderBackground();
            gui.render();
            DisplayRenderProfiler.end(controllerPos, DisplayRenderProfiler.Category.GUI_RENDER);

            gui.clearOffscreenScissorMode();

            guiGraphics.pose().popPose(); // mc<=1.21.5
            offscreenBufferSource.endBatch();
        } finally {
            ((net.kroia.mixin.MinecraftAccessor) mc).modutilities$setMainRenderTarget(prevTarget); // mc>=1.21.2 mc<=1.21.5
        }
        //offscreenGui.render(guiGraphics, data.framebuffer, scale); // mc>=1.21.6 (recorded GUI -> framebuffer)
        //data.framebuffer.bindWrite(false); // mc<=1.21.4 (glCopyTexSubImage2D below reads the bound framebuffer)

        DisplayRenderProfiler.storeTextureSize(controllerPos, texW, texH);
        DisplayRenderProfiler.begin(controllerPos, DisplayRenderProfiler.Category.TEXTURE_TRANSFER);
        //data.framebuffer.bindRead(); // mc<=1.21.4
        //int prevTexture = org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL11.GL_TEXTURE_BINDING_2D); // mc<=1.21.4
        //RenderSystem.bindTexture(data.texture.getId()); // mc<=1.21.4
        //org.lwjgl.opengl.GL11.glCopyTexSubImage2D( // mc<=1.21.4
        //        org.lwjgl.opengl.GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, texW, texH); // mc<=1.21.4
        //RenderSystem.bindTexture(prevTexture); // mc<=1.21.4
        //data.framebuffer.unbindRead(); // mc<=1.21.4
        RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture( // mc>=1.21.5
                data.framebuffer.getColorTexture(), data.texture.getTexture(), 0, 0, 0, 0, 0, texW, texH); // mc>=1.21.5
        DisplayRenderProfiler.end(controllerPos, DisplayRenderProfiler.Category.TEXTURE_TRANSFER);

        //data.framebuffer.unbindWrite(); // mc<=1.21.4
        //prevTarget.bindWrite(false); // mc<=1.21.4
        RenderSystem.getModelViewStack().popMatrix();
        //RenderSystem.applyModelViewMatrix(); // mc<=1.21.1
        //RenderSystem.viewport(prevViewport[0], prevViewport[1], prevViewport[2], prevViewport[3]); // mc<=1.21.4
        RenderSystem.setProjectionMatrix(prevProjection, prevSorting); // mc<=1.21.5
        RenderSystem.setShaderColor(prevShaderColor[0], prevShaderColor[1], prevShaderColor[2], prevShaderColor[3]); // mc<=1.21.5
        //RenderSystem.setShaderFogColor(prevFogColor[0], prevFogColor[1], prevFogColor[2], prevFogColor[3]); // mc<=1.21.1
        //RenderSystem.setShaderFogStart(prevFogStart); // mc<=1.21.1
        //RenderSystem.setShaderFogEnd(prevFogEnd); // mc<=1.21.1
        RenderSystem.setShaderFog(prevFog); // mc>=1.21.2 mc<=1.21.5
        //if (prevDepthTest) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest(); // mc<=1.21.4
        //if (prevBlend) RenderSystem.enableBlend(); else RenderSystem.disableBlend(); // mc<=1.21.4
        //if (prevCull) RenderSystem.enableCull(); else RenderSystem.disableCull(); // mc<=1.21.4
        Lighting.setupLevel(); // mc<=1.21.5
    }

    private void renderQuadOnFace(PoseStack poseStack, MultiBufferSource bufferSource,
                                   Direction facing, ResourceLocation texture,
                                   float u0, float v0, float u1, float v1, float offset) {
        poseStack.pushPose();

        switch (facing) {
            case NORTH -> {
                poseStack.translate(1, 1, -offset);
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180));
            }
            case SOUTH -> poseStack.translate(0, 1, 1 + offset);
            case EAST -> {
                poseStack.translate(1 + offset, 1, 1);
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90));
            }
            case WEST -> {
                poseStack.translate(-offset, 1, 0);
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-90));
            }
        }

        poseStack.scale(1.0f, -1.0f, 1.0f);

        Matrix4f matrix = poseStack.last().pose();

        VertexConsumer consumer = bufferSource.getBuffer(
                RenderType.text(texture));

        int light = 0xF000F0;
        consumer.addVertex(matrix, 0, 0, 0).setColor(255, 255, 255, 255)
                .setUv(u0, v1).setLight(light);
        consumer.addVertex(matrix, 0, 1, 0).setColor(255, 255, 255, 255)
                .setUv(u0, v0).setLight(light);
        consumer.addVertex(matrix, 1, 1, 0).setColor(255, 255, 255, 255)
                .setUv(u1, v0).setLight(light);
        consumer.addVertex(matrix, 1, 0, 0).setColor(255, 255, 255, 255)
                .setUv(u1, v1).setLight(light);

        poseStack.popPose();
    }
}
