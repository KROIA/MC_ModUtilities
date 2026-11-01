package net.kroia.mixin;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Set;

/**
 * mc>=1.21.2: {@code BlockEntityType.Builder} was removed and the vanilla constructor is
 * private (NeoForge makes it public, Fabric does not), so common code creates block entity
 * types through this invoker. Not needed on mc<=1.21.1 (use {@code BlockEntityType.Builder}).
 */
@Mixin(BlockEntityType.class)
public interface BlockEntityTypeInvoker {
    @Invoker("<init>")
    static <T extends net.minecraft.world.level.block.entity.BlockEntity> BlockEntityType<T> modutilities$create(
            BlockEntityType.BlockEntitySupplier<? extends T> factory, Set<Block> validBlocks) {
        throw new AssertionError();
    }
}
