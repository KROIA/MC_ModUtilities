package net.kroia.modutilities.sandbox;

import com.google.gson.JsonElement;
import com.mojang.brigadier.CommandDispatcher;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.kroia.modutilities.JsonUtilities;
import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.networking.PacketManager;
import net.kroia.modutilities.networking.streaming.StreamSystem;
import net.kroia.modutilities.setting.parser.ItemStackJsonParser;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class Sandbox {
    public static class SandboxNetwork extends PacketManager
    {

        public static final SineStream SINUS_STREAM = (SineStream) StreamSystem.register(new SineStream());

        public SandboxNetwork() {
            super(ModUtilitiesMod.MOD_ID);
            setupClientReceiverPackets();
            setupServerReceiverPackets();
            this.setupARRS(); // Setup the Asynchronous Request Response System (ARRS)
            this.setupStreamSystem();

            SandboxOpenGuiPacket.network = this; // Set the network instance for the SandboxOpenGuiPacket
        }

        @Override
        public void setupClientReceiverPackets() {
            register(SandboxOpenGuiPacket.TYPE, SandboxOpenGuiPacket.STREAM_CODEC, SandboxOpenGuiPacket.HANDLER);

        }

        @Override
        public void setupServerReceiverPackets() {

        }
    }

    private static final class SandboxCommand
    {
        public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
            dispatcher.register(
                    Commands.literal("modutilities")
                            .requires(source -> source.hasPermission(2)) // Requires OP permission level
                            .then(Commands.literal("openTestScreen")
                                    .executes(context -> {
                                        ServerPlayer player = context.getSource().getPlayerOrException();
                                        SandboxOpenGuiPacket.send(player, SandboxOpenGuiPacket.GuiType.TEST_SCREEN);
                                        return 1;
                                    }))
                            .then(Commands.literal("saveItemInHand")
                                    .executes(context -> {
                                        ServerPlayer player = context.getSource().getPlayerOrException();
                                        ItemStack itemInHand = player.getMainHandItem();
                                        if(itemInHand == null || itemInHand.isEmpty()) {
                                            return 0;
                                        }
                                        ItemStackJsonParser parser = new ItemStackJsonParser();
                                        JsonElement stackJson = parser.toJson(itemInHand);
                                        ModUtilitiesMod.LOGGER.info("Saved item in hand: " + stackJson.toString());
                                        player.sendSystemMessage(Component.literal("Saved item in hand: " + stackJson.toString()));
                                        // save to file
                                        try {
                                            Files.writeString(Path.of("saved_item.json"), JsonUtilities.toPrettyString(stackJson));
                                        } catch (Exception e) {
                                            ModUtilitiesMod.LOGGER.error("Failed to save JSON to file: " + "saved_item.json", e);
                                            return 0;
                                        }
                                        return 1;
                                    }))
                            .then(Commands.literal("loadItemToHand")
                                    .executes(context -> {
                                        ServerPlayer player = context.getSource().getPlayerOrException();
                                        ItemStackJsonParser parser = new ItemStackJsonParser();

                                        // Load from file
                                        File file = new File("saved_item.json");
                                        if (!file.exists()) {
                                            player.sendSystemMessage(Component.literal("No saved item found."));
                                            return 0;
                                        }
                                        try {
                                            String jsonContent = Files.readString(file.toPath());
                                            JsonElement jsonElement = JsonUtilities.fromString(jsonContent);
                                            ItemStack itemStack = parser.fromJson(jsonElement);
                                            player.setItemInHand(player.getUsedItemHand(), itemStack);
                                            player.sendSystemMessage(Component.literal("Loaded item to hand: " + itemStack.toString()));
                                        } catch (Exception e) {
                                            ModUtilitiesMod.LOGGER.error("Failed to load item from JSON file", e);
                                            player.sendSystemMessage(Component.literal("Failed to load item from JSON file: " + e.getMessage()));
                                            return 0;
                                        }

                                        return 1;
                                    }))
                            .then(Commands.literal("loadAndSaveDataArchive")
                                    .executes(context -> {
                                        ServerPlayer player = context.getSource().getPlayerOrException();
                                        SandboxDataArchiveManager.loadAndSave();
                                        return 1;
                                    }))
            );
        }
    }

    private static SandboxNetwork network = null;

    private static SandboxDataArchiveManager dataArchiveManager;


    /*public static final Supplier<RegistrarManager> MANAGER = Suppliers.memoize(() -> RegistrarManager.get(ModUtilitiesMod.MOD_ID));
    public static final Registrar<MenuType<?>> MENUS = MANAGER.get().get(Registries.MENU);









    public static final Registrar<Item> ITEMS = MANAGER.get().get(Registries.ITEM);
    public static <T extends Item> RegistrySupplier<T> registerItem(String name, Supplier<T> item)
    {
        return ITEMS.register(new ResourceLocation(ModUtilitiesMod.MOD_ID, name), item);
    }
    public static <T extends Block> RegistrySupplier<Item> registerBlockItem(String name, RegistrySupplier<T> block)
    {
        //return registerItem(name, () -> new BlockItem(block.get(), new Item.Properties().tab(BankSystemCreativeModeTab.BANK_SYSTEM_TAB))); // 1.19.3 or below
        return registerItem(name, () -> new BlockItem(block.get(), new Item.Properties().arch$tab(MY_TAB)));
    }


    private static final Registrar<Block> BLOCKS = MANAGER.get().get(Registries.BLOCK);
    public static final RegistrySupplier<Block> MY_BLOCK = registerBlock("my_block", MyBlock::new);
    public static <T extends Block> RegistrySupplier<T> registerBlock(String name, Supplier<T> block)
    {
        RegistrySupplier<T> toReturn = BLOCKS.register(new ResourceLocation(ModUtilitiesMod.MOD_ID, name), block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }








    public static final Registrar<BlockEntityType<?>> BLOCK_ENTITIES = MANAGER.get().get(Registries.BLOCK_ENTITY_TYPE);
    public static final RegistrySupplier<BlockEntityType<?>> BANK_TERMINAL_BLOCK_ENTITY =
            registerBlockEntity("my_block_entity",
                    () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(MyBlockEntity::new, MY_BLOCK.get()).build(null));
    public static <T extends BlockEntityType<?>> RegistrySupplier<T> registerBlockEntity(String name, Supplier<T> item)
    {
        //BankSystemMod.LOGGER.info("Registering block entity: " + name);
        return BLOCK_ENTITIES.register(new ResourceLocation(ModUtilitiesMod.MOD_ID, name), item);
    }




    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(ModUtilitiesMod.MOD_ID, Registries.CREATIVE_MODE_TAB);

    public static final RegistrySupplier<CreativeModeTab> MY_TAB = TABS.register(
            "bank_system_tab", // Tab ID
            () -> {
                return CreativeTabRegistry.create(
                        Component.translatable("itemGroup."+ModUtilitiesMod.MOD_ID+".bank_system_tab"), // Tab Name
                        () -> new ItemStack(MY_BLOCK.get()) // Icon
                );}
    );

    public static void initClient()
    {
        var menu = MY_CONTAINER_MENU.get();
        MenuRegistry.registerScreenFactory(menu, TestScreen::new);
    }

    public static final RegistrySupplier<MenuType<MyContainerMenu>> MY_CONTAINER_MENU =
            MENUS.register(new ResourceLocation(ModUtilitiesMod.MOD_ID, "my_container_menu"), () -> MenuRegistry.ofExtended(MyContainerMenu::new));*/


    public static void init()
    {
        CommandRegistrationEvent.EVENT.register((dispatcher, registryAccess, environment) -> {
            SandboxCommand.register(dispatcher);
        });
        network = new SandboxNetwork();
        //TABS.register();


        //dataArchiveManager = new SandboxDataArchiveManager(Path.of("data/sandbox_data_archive"));
    }

}
