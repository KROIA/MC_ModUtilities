package net.kroia.modutilities;

//import net.kroia.mixin.BlockEntityTypeInvoker; // mc>=1.21.2
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation; // mc<=1.21.10
//import net.minecraft.resources.Identifier; // mc>=1.21.11
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;

/**
 * Version-agnostic registration helpers, so dependent mods need no Minecraft-version toggles
 * of their own. The same calls compile on mc 1.21.1 and mc 1.21.2+.
 */
public final class RegistryCompat {
    private RegistryCompat() {}

    /**
     * Block entity constructor reference, e.g. {@code MyBlockEntity::new}. Public replacement
     * for the package-private vanilla {@code BlockEntityType.BlockEntitySupplier}.
     */
    @FunctionalInterface
    public interface BlockEntityFactory<T extends BlockEntity> {
        T create(BlockPos pos, BlockState state);
    }

    /** Sets the block registry ID on the properties; MC 1.21.2+ requires it before the block is constructed, 1.21.1 ignores it. */
    public static BlockBehaviour.Properties blockProperties(BlockBehaviour.Properties props, ResourceLocation id) { // mc<=1.21.10
    //public static BlockBehaviour.Properties blockProperties(BlockBehaviour.Properties props, Identifier id) { // mc>=1.21.11
        return props; // mc<=1.21.1
        //return props.setId(ResourceKey.create(Registries.BLOCK, id)); // mc>=1.21.2
    }

    /** Sets the item registry ID on the properties; MC 1.21.2+ requires it before the item is constructed, 1.21.1 ignores it. */
    public static Item.Properties itemProperties(Item.Properties props, ResourceLocation id) { // mc<=1.21.10
    //public static Item.Properties itemProperties(Item.Properties props, Identifier id) { // mc>=1.21.11
        return props; // mc<=1.21.1
        //return props.setId(ResourceKey.create(Registries.ITEM, id)); // mc>=1.21.2
    }

    /** Like {@link #itemProperties}, but for a BlockItem: it keeps the block's translation key, matching the 1.21.1 BlockItem behaviour (MC 1.21.2 registry ID requirement). */
    public static Item.Properties blockItemProperties(Item.Properties props, ResourceLocation id) { // mc<=1.21.10
    //public static Item.Properties blockItemProperties(Item.Properties props, Identifier id) { // mc>=1.21.11
        return props; // mc<=1.21.1
        //return props.useBlockDescriptionPrefix().setId(ResourceKey.create(Registries.ITEM, id)); // mc>=1.21.2
    }

    /** Creates a BlockEntityType; MC 1.21.2 removed {@code BlockEntityType.Builder} and made the constructor private. */
    public static <T extends BlockEntity> BlockEntityType<T> createBlockEntityType(BlockEntityFactory<? extends T> factory, Block... blocks) {
        return BlockEntityType.Builder.<T>of(factory::create, blocks).build(null); // mc<=1.21.1
        //return BlockEntityTypeInvoker.modutilities$create(factory::create, Set.of(blocks)); // mc>=1.21.2
    }
}
