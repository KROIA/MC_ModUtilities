package net.kroia.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.kroia.modutilities.gui.display.AbstractDisplayBlock;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Suppresses the block selection outline (wireframe hitbox) for display blocks.
 * The outline overlaps the rendered display content and is visually distracting.
 * Block interaction (right-click, breaking, placement) is unaffected because
 * the collision/interaction shape from {@code getShape()} remains intact.
 */
@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Inject(method = "renderHitOutline", at = @At("HEAD"), cancellable = true)
    private void modutilities$suppressDisplayBlockOutline(
            PoseStack poseStack, VertexConsumer consumer, Entity entity, // mc<=1.21.8
            //PoseStack poseStack, VertexConsumer consumer, // mc>=1.21.9
            double camX, double camY, double camZ,
            BlockPos pos, BlockState state, CallbackInfo ci) { // mc<=1.21.1
            //BlockPos pos, BlockState state, int color, CallbackInfo ci) { // mc>=1.21.2 mc<=1.21.8
            //net.minecraft.client.renderer.state.BlockOutlineRenderState outline, int color, CallbackInfo ci) { // mc>=1.21.9 mc<=1.21.10
            //net.minecraft.client.renderer.state.BlockOutlineRenderState outline, int color, float lineWidth, CallbackInfo ci) { // mc>=1.21.11 mc<=1.21.11
            //net.minecraft.client.renderer.state.level.BlockOutlineRenderState outline, int color, float lineWidth, CallbackInfo ci) { // mc>=26.1
        //net.minecraft.client.multiplayer.ClientLevel level = net.minecraft.client.Minecraft.getInstance().level; // mc>=1.21.9
        //if (level == null) return; // mc>=1.21.9
        //BlockState state = level.getBlockState(outline.pos()); // mc>=1.21.9
        if (state.getBlock() instanceof AbstractDisplayBlock) {
            ci.cancel();
        }
    }
}
