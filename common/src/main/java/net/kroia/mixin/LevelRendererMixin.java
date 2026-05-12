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
            PoseStack poseStack, VertexConsumer consumer, Entity entity,
            double camX, double camY, double camZ,
            BlockPos pos, BlockState state, CallbackInfo ci) {
        if (state.getBlock() instanceof AbstractDisplayBlock) {
            ci.cancel();
        }
    }
}
