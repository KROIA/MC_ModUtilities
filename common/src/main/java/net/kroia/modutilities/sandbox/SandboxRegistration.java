package net.kroia.modutilities.sandbox;

import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.gui.display.client.AbstractDisplayBlockEntityRenderer;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Handles block, item, and block entity registration for the sandbox
 * DisplayBlock demo. Uses Architectury's {@link DeferredRegister} for
 * cross-platform compatibility.
 */
public class SandboxRegistration {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ModUtilitiesMod.MOD_ID, Registries.BLOCK);

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ModUtilitiesMod.MOD_ID, Registries.ITEM);

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ModUtilitiesMod.MOD_ID, Registries.BLOCK_ENTITY_TYPE);

    // --- DisplayDemoBlock ---
    public static final RegistrySupplier<Block> DISPLAY_DEMO_BLOCK =
            BLOCKS.register("display_demo_block", DisplayDemoBlock::new);

    public static final RegistrySupplier<Item> DISPLAY_DEMO_BLOCK_ITEM =
            ITEMS.register("display_demo_block", () ->
                    new BlockItem(DISPLAY_DEMO_BLOCK.get(), new Item.Properties()));

    @SuppressWarnings("unchecked")
    public static final RegistrySupplier<BlockEntityType<DisplayDemoBlockEntity>> DISPLAY_DEMO_BLOCK_ENTITY =
            (RegistrySupplier<BlockEntityType<DisplayDemoBlockEntity>>) (RegistrySupplier<?>)
            BLOCK_ENTITIES.register("display_demo_block_entity", () ->
                    BlockEntityType.Builder.of(DisplayDemoBlockEntity::new, DISPLAY_DEMO_BLOCK.get())
                            .build(null));

    // --- DisplayDemoPanelBlock ---
    public static final RegistrySupplier<Block> DISPLAY_DEMO_PANEL_BLOCK =
            BLOCKS.register("display_demo_panel_block", DisplayDemoPanelBlock::new);

    public static final RegistrySupplier<Item> DISPLAY_DEMO_PANEL_BLOCK_ITEM =
            ITEMS.register("display_demo_panel_block", () ->
                    new BlockItem(DISPLAY_DEMO_PANEL_BLOCK.get(), new Item.Properties()));

    @SuppressWarnings("unchecked")
    public static final RegistrySupplier<BlockEntityType<DisplayDemoPanelBlockEntity>> DISPLAY_DEMO_PANEL_BLOCK_ENTITY =
            (RegistrySupplier<BlockEntityType<DisplayDemoPanelBlockEntity>>) (RegistrySupplier<?>)
            BLOCK_ENTITIES.register("display_demo_panel_block_entity", () ->
                    BlockEntityType.Builder.of(DisplayDemoPanelBlockEntity::new, DISPLAY_DEMO_PANEL_BLOCK.get())
                            .build(null));

    // --- ChartDemoBlock ---
    public static final RegistrySupplier<Block> CHART_DEMO_BLOCK =
            BLOCKS.register("chart_demo_block", ChartDemoBlock::new);

    public static final RegistrySupplier<Item> CHART_DEMO_BLOCK_ITEM =
            ITEMS.register("chart_demo_block", () ->
                    new BlockItem(CHART_DEMO_BLOCK.get(), new Item.Properties()));

    @SuppressWarnings("unchecked")
    public static final RegistrySupplier<BlockEntityType<ChartDemoBlockEntity>> CHART_DEMO_BLOCK_ENTITY =
            (RegistrySupplier<BlockEntityType<ChartDemoBlockEntity>>) (RegistrySupplier<?>)
            BLOCK_ENTITIES.register("chart_demo_block_entity", () ->
                    BlockEntityType.Builder.of(ChartDemoBlockEntity::new, CHART_DEMO_BLOCK.get())
                            .build(null));

    /**
     * Registers all deferred registries. Must be called during mod initialization
     * (before registry freeze).
     */
    public static void register() {
        BLOCKS.register();
        ITEMS.register();
        BLOCK_ENTITIES.register();
    }

    /**
     * Registers client-side renderers. Must be called from client-only code
     * during mod initialization.
     * <p>
     * Uses {@link RegistrySupplier#listen} to defer the actual renderer binding
     * until after the registry is frozen and the supplier is resolved.
     */
    @Environment(EnvType.CLIENT)
    public static void registerClient() {
        DISPLAY_DEMO_BLOCK_ENTITY.listen(blockEntityType ->
                BlockEntityRendererRegistry.register(blockEntityType,
                        AbstractDisplayBlockEntityRenderer::new));
        DISPLAY_DEMO_PANEL_BLOCK_ENTITY.listen(blockEntityType ->
                BlockEntityRendererRegistry.register(blockEntityType,
                        AbstractDisplayBlockEntityRenderer::new));
        CHART_DEMO_BLOCK_ENTITY.listen(blockEntityType ->
                BlockEntityRendererRegistry.register(blockEntityType,
                        AbstractDisplayBlockEntityRenderer::new));
    }
}
