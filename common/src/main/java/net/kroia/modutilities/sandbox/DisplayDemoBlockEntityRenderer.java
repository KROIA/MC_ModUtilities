package net.kroia.modutilities.sandbox;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.kroia.modutilities.gui.Gui;
import net.kroia.modutilities.gui.client.ClientGraphics;
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

/**
 * Renderer for {@link DisplayDemoBlockEntity}. Supports multi-block display
 * groups: the controller block renders the full GUI to a shared texture, and
 * each block in the group renders its UV subset of that texture.
 * <p>
 * Per-group render data (framebuffer, texture, image) is keyed by the
 * controller's {@link BlockPos}. Non-controller blocks look up the controller's
 * render data to obtain the shared texture.
 */
@Environment(EnvType.CLIENT)
public class DisplayDemoBlockEntityRenderer implements BlockEntityRenderer<DisplayDemoBlockEntity> {

    private static final int RENDER_SCALE = 2;

    /**
     * Maximum texture dimension (width or height) for the offscreen framebuffer.
     * Groups whose combined resolution would exceed this are rendered at a
     * reduced scale to stay within GPU texture size limits.
     */
    private static final int MAX_TEXTURE_DIM = 4096;

    private final ClientGraphics clientGraphics;
    private MultiBufferSource.BufferSource offscreenBufferSource;

    /**
     * Per-group render data, keyed by the controller block's position.
     */
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

    public DisplayDemoBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.clientGraphics = new ClientGraphics();
        clientGraphics.setFont(Minecraft.getInstance().font);
    }

    @Override
    public boolean shouldRenderOffScreen(DisplayDemoBlockEntity blockEntity) {
        return true;
    }

    private void ensureBufferSource() {
        if (offscreenBufferSource == null) {
            offscreenBufferSource = MultiBufferSource.immediate(
                    new com.mojang.blaze3d.vertex.ByteBufferBuilder(786432));
        }
    }

    /**
     * Computes the effective render scale for a group, reducing it if the
     * combined resolution would exceed {@link #MAX_TEXTURE_DIM}.
     */
    private static int effectiveRenderScale(int gw, int gh) {
        int scale = RENDER_SCALE;
        while (scale > 1
                && (gw * DisplayDemoBlockEntity.VIRTUAL_WIDTH * scale > MAX_TEXTURE_DIM
                || gh * DisplayDemoBlockEntity.VIRTUAL_HEIGHT * scale > MAX_TEXTURE_DIM)) {
            scale--;
        }
        return scale;
    }

    private GroupRenderData getOrCreateGroupData(BlockPos controllerPos, int gw, int gh) {
        GroupRenderData data = groupData.get(controllerPos);

        int scale = effectiveRenderScale(gw, gh);
        int texW = gw * DisplayDemoBlockEntity.VIRTUAL_WIDTH * scale;
        int texH = gh * DisplayDemoBlockEntity.VIRTUAL_HEIGHT * scale;

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
    public void render(DisplayDemoBlockEntity blockEntity, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {

        if (!blockEntity.isActive()) return;

        BlockPos controllerPos = blockEntity.getControllerPos();
        if (controllerPos == null) return;

        int gw = blockEntity.getGroupWidth();
        int gh = blockEntity.getGroupHeight();

        GroupRenderData data = getOrCreateGroupData(controllerPos, gw, gh);

        // Any visible block in the group can trigger the texture update (once per frame)
        long currentFrame = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.getGameTime() : 0;
        if (data.lastRenderedFrame < currentFrame) {
            Gui gui = null;
            if (blockEntity.isController()) {
                gui = blockEntity.getGui();
            } else if (blockEntity.getLevel() != null) {
                BlockEntity controllerBE = blockEntity.getLevel().getBlockEntity(controllerPos);
                if (controllerBE instanceof DisplayDemoBlockEntity controller) {
                    gui = controller.getGui();
                }
            }
            if (gui != null) {
                updateClientMousePos(gui, controllerPos);
                renderGuiToTexture(gui, partialTick, data);
                data.lastRenderedFrame = currentFrame;
            }
        }

        // Every block in the group renders its UV subset
        Direction facing = blockEntity.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        int gx = blockEntity.getGridX();
        int gy = blockEntity.getGridY();

        float u0 = (float) gx / gw;
        float u1 = (float) (gx + 1) / gw;
        // Flip V: framebuffer is bottom-up, gridY=0 is top row
        float v0 = 1.0f - (float) (gy + 1) / gh;
        float v1 = 1.0f - (float) gy / gh;

        renderQuadOnFace(poseStack, bufferSource, facing, data.location, u0, v0, u1, v1);
    }

    private void updateClientMousePos(Gui gui, BlockPos controllerPos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        if (mc.hitResult instanceof BlockHitResult blockHit
                && blockHit.getType() == HitResult.Type.BLOCK) {
            BlockEntity hitBE = mc.level.getBlockEntity(blockHit.getBlockPos());
            if (hitBE instanceof DisplayDemoBlockEntity hitDisplay
                    && hitDisplay.isActive()
                    && controllerPos.equals(hitDisplay.getControllerPos())) {
                Direction hitFacing = mc.level.getBlockState(blockHit.getBlockPos())
                        .getValue(HorizontalDirectionalBlock.FACING);
                double[] coords = DisplayDemoBlock.computeGuiCoords(
                        blockHit, blockHit.getBlockPos(), hitFacing, hitDisplay);
                if (coords != null) {
                    gui.storeMousePos((int) coords[0], (int) coords[1]);
                    return;
                }
            }
        }
        // Not looking at this display — move mouse off-screen
        gui.storeMousePos(-1, -1);
    }

    private void renderGuiToTexture(Gui gui, float partialTick, GroupRenderData data) {
        Minecraft mc = Minecraft.getInstance();

        int texW = data.texWidth;
        int texH = data.texHeight;
        int guiW = data.groupWidth * DisplayDemoBlockEntity.VIRTUAL_WIDTH;
        int guiH = data.groupHeight * DisplayDemoBlockEntity.VIRTUAL_HEIGHT;

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

        // Disable fog so text/GUI colors aren't tinted by sky color
        float[] prevFogColor = new float[4];
        prevFogColor[0] = RenderSystem.getShaderFogColor()[0];
        prevFogColor[1] = RenderSystem.getShaderFogColor()[1];
        prevFogColor[2] = RenderSystem.getShaderFogColor()[2];
        prevFogColor[3] = RenderSystem.getShaderFogColor()[3];
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

        // Save current texture binding, download framebuffer, restore
        int prevTexture = org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL11.GL_TEXTURE_BINDING_2D);
        data.framebuffer.bindRead();
        data.image.downloadTexture(0, false);
        data.framebuffer.unbindRead();
        RenderSystem.bindTexture(prevTexture);
        data.texture.upload();
        RenderSystem.bindTexture(prevTexture);

        data.framebuffer.unbindWrite();
        prevTarget.bindWrite(false);
        RenderSystem.getModelViewStack().popMatrix();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.viewport(prevViewport[0], prevViewport[1], prevViewport[2], prevViewport[3]);
        RenderSystem.setProjectionMatrix(prevProjection, prevSorting);
        RenderSystem.setShaderFogColor(prevFogColor[0], prevFogColor[1], prevFogColor[2], prevFogColor[3]);
    }

    /**
     * Renders a textured quad on the block face with the specified UV sub-region.
     *
     * @param u0 left U coordinate [0..1]
     * @param v0 top V coordinate [0..1]
     * @param u1 right U coordinate [0..1]
     * @param v1 bottom V coordinate [0..1]
     */
    private void renderQuadOnFace(PoseStack poseStack, MultiBufferSource bufferSource,
                                   Direction facing, ResourceLocation texture,
                                   float u0, float v0, float u1, float v1) {
        float offset = 0.005f;

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
