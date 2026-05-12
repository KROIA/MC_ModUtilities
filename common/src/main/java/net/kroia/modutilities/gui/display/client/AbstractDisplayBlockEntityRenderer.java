package net.kroia.modutilities.gui.display.client;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
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

    private static class GroupRenderData {
        DynamicTexture texture;
        NativeImage image;
        ResourceLocation location;
        TextureTarget framebuffer;
        boolean layoutInitialized;
        int groupWidth;
        int groupHeight;
        int texWidth;
        int texHeight;
        long lastRenderedFrame = -1;
    }

    private final Map<BlockPos, GroupRenderData> groupData = new HashMap<>();

    public AbstractDisplayBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.clientGraphics = new ClientGraphics();
        clientGraphics.setFont(Minecraft.getInstance().font);
    }

    @Override
    public boolean shouldRenderOffScreen(T blockEntity) {
        return true;
    }

    private void ensureBufferSource() {
        if (offscreenBufferSource == null) {
            offscreenBufferSource = MultiBufferSource.immediate(
                    new com.mojang.blaze3d.vertex.ByteBufferBuilder(786432));
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
            data = new GroupRenderData();
            data.groupWidth = gw;
            data.groupHeight = gh;
            data.texWidth = texW;
            data.texHeight = texH;
            data.image = new NativeImage(texW, texH, false);
            data.texture = new DynamicTexture(data.image);
            data.framebuffer = new TextureTarget(texW, texH, true, false);
            data.location = ResourceLocation.fromNamespaceAndPath("modutilities",
                    "dynamic/display_group_" + controllerPos.getX() + "_"
                            + controllerPos.getY() + "_" + controllerPos.getZ());
            Minecraft.getInstance().getTextureManager().register(data.location, data.texture);
            data.layoutInitialized = false;
            groupData.put(controllerPos, data);
        }

        return data;
    }

    @Override
    public void render(T blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        if (!blockEntity.isActive()) return;

        BlockPos controllerPos = blockEntity.getControllerPos();
        if (controllerPos == null) return;

        DisplayConfig config = blockEntity.getDisplayConfig();
        int gw = blockEntity.getGroupWidth();
        int gh = blockEntity.getGroupHeight();

        GroupRenderData data = getOrCreateGroupData(controllerPos, gw, gh, config);

        long currentFrame = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.getGameTime() : 0;
        if (data.lastRenderedFrame < currentFrame) {
            Gui gui = null;
            if (blockEntity.isController()) {
                gui = blockEntity.getGui();
            } else if (blockEntity.getLevel() != null) {
                BlockEntity controllerBE = blockEntity.getLevel().getBlockEntity(controllerPos);
                if (controllerBE instanceof AbstractDisplayBlockEntity controller) {
                    gui = controller.getGui();
                }
            }
            if (gui != null) {
                updateClientMousePos(gui, controllerPos, config);
                renderGuiToTexture(gui, partialTick, data, config);
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

        renderQuadOnFace(poseStack, bufferSource, facing, data.location,
                u0, v0, u1, v1, config.faceOffset());
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
                                    DisplayConfig config) {
        Minecraft mc = Minecraft.getInstance();

        int texW = data.texWidth;
        int texH = data.texHeight;
        int guiW = data.groupWidth * config.virtualWidth();
        int guiH = data.groupHeight * config.virtualHeight();

        ensureBufferSource();

        com.mojang.blaze3d.pipeline.RenderTarget prevTarget = mc.getMainRenderTarget();
        Matrix4f prevProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting prevSorting = RenderSystem.getVertexSorting();
        int[] prevViewport = new int[4];
        org.lwjgl.opengl.GL11.glGetIntegerv(org.lwjgl.opengl.GL11.GL_VIEWPORT, prevViewport);

        data.framebuffer.bindWrite(false);
        RenderSystem.viewport(0, 0, texW, texH);
        RenderSystem.clearColor(0.15f, 0.15f, 0.15f, 1.0f);
        RenderSystem.clear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT
                | org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT, false);

        float[] prevShaderColor = RenderSystem.getShaderColor().clone();

        float[] prevFogColor = new float[4];
        prevFogColor[0] = RenderSystem.getShaderFogColor()[0];
        prevFogColor[1] = RenderSystem.getShaderFogColor()[1];
        prevFogColor[2] = RenderSystem.getShaderFogColor()[2];
        prevFogColor[3] = RenderSystem.getShaderFogColor()[3];
        float prevFogStart = RenderSystem.getShaderFogStart();
        float prevFogEnd = RenderSystem.getShaderFogEnd();
        RenderSystem.setShaderFogColor(0, 0, 0, 0);
        RenderSystem.setShaderFogStart(Float.MAX_VALUE);
        RenderSystem.setShaderFogEnd(Float.MAX_VALUE);

        Matrix4f ortho = new Matrix4f().setOrtho(0, guiW, guiH, 0, 1000, 21000);
        RenderSystem.setProjectionMatrix(ortho, VertexSorting.ORTHOGRAPHIC_Z);
        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().identity();
        RenderSystem.applyModelViewMatrix();

        GuiGraphics guiGraphics = new GuiGraphics(mc, offscreenBufferSource);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, -11000);

        gui.setGraphicsBackend(clientGraphics);
        gui.setFont(mc.font);
        clientGraphics.setGraphics(guiGraphics);
        clientGraphics.setFont(mc.font);
        clientGraphics.setEnableShadow(false);
        gui.setPartialTick(partialTick);

        if (!data.layoutInitialized) {
            gui.init();
            data.layoutInitialized = true;
        }

        gui.renderBackground();
        gui.render();

        guiGraphics.pose().popPose();
        offscreenBufferSource.endBatch();

        int prevTexture = org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL11.GL_TEXTURE_BINDING_2D);
        data.framebuffer.bindRead();
        data.image.downloadTexture(0, false);
        data.framebuffer.unbindRead();
        RenderSystem.bindTexture(prevTexture);
        data.texture.upload();

        data.framebuffer.unbindWrite();
        prevTarget.bindWrite(false);
        RenderSystem.getModelViewStack().popMatrix();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.viewport(prevViewport[0], prevViewport[1], prevViewport[2], prevViewport[3]);
        RenderSystem.setProjectionMatrix(prevProjection, prevSorting);
        RenderSystem.setShaderColor(prevShaderColor[0], prevShaderColor[1], prevShaderColor[2], prevShaderColor[3]);
        RenderSystem.setShaderFogColor(prevFogColor[0], prevFogColor[1], prevFogColor[2], prevFogColor[3]);
        RenderSystem.setShaderFogStart(prevFogStart);
        RenderSystem.setShaderFogEnd(prevFogEnd);
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
