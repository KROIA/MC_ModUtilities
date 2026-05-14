package net.kroia.modutilities.sandbox;

import com.google.gson.JsonElement;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.kroia.modutilities.gui.display.client.DisplayRenderProfiler;
import net.kroia.modutilities.JsonUtilities;
import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.UtilitiesPlatform;
import net.kroia.modutilities.networking.NetworkPacketManager;
import net.kroia.modutilities.networking.client_server.streaming.StreamSystem;
import net.kroia.modutilities.setting.parser.ItemStackJsonParser;
import net.kroia.modutilities.testing.TestCommandRegistration;
import net.kroia.modutilities.testing.TestRegistry;
import net.kroia.modutilities.testing.tests.CreativeTabTests;
import net.kroia.modutilities.testing.tests.EventTests;
import net.kroia.modutilities.testing.tests.GuiLogicTests;
import net.kroia.modutilities.testing.tests.ParserTests;
import net.kroia.modutilities.testing.tests.PersistenceTests;
import net.kroia.modutilities.testing.tests.NetworkingTests;
import net.kroia.modutilities.testing.tests.SettingsTests;
import net.kroia.modutilities.testing.tests.StreamingTests;
import net.kroia.modutilities.testing.tests.UtilityTests;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class Sandbox {
    public static class SandboxNetwork extends NetworkPacketManager
    {

        public static final SineStream SINUS_STREAM = (SineStream) StreamSystem.register(new SineStream());

        public SandboxNetwork() {
            super(ModUtilitiesMod.MOD_ID);
            setupClientReceiverPackets();
            setupServerReceiverPackets();
            setupServerServerPackets();
            this.setupARRS(); // Setup the Asynchronous Request Response System (ARRS)
            this.setupStreamSystem();

            SandboxOpenGuiPacket.network = this; // Set the network instance for the SandboxOpenGuiPacket
        }

        @Override
        public void setupClientReceiverPackets() {
            registerS2C(SandboxOpenGuiPacket.TYPE, SandboxOpenGuiPacket.STREAM_CODEC, SandboxOpenGuiPacket.HANDLER);
            registerS2C(SimpleDataPacketToClient.TYPE, SimpleDataPacketToClient.STREAM_CODEC);
        }

        @Override
        public void setupServerReceiverPackets() {

        }

        @Override
        public void setupServerServerPackets()
        {

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
                                            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, itemStack);
                                            player.sendSystemMessage(Component.literal("Loaded item to hand: " + itemStack.toString()));
                                        } catch (Exception e) {
                                            ModUtilitiesMod.LOGGER.error("Failed to load item from JSON file", e);
                                            player.sendSystemMessage(Component.literal("Failed to load item from JSON file: " + e.getMessage()));
                                            return 0;
                                        }

                                        return 1;
                                    }))
                            .then(Commands.literal("openExample")
                                    .then(Commands.literal("form")
                                            .executes(ctx -> sendOpenExample(ctx, SandboxOpenGuiPacket.GuiType.EXAMPLE_FORM)))
                                    .then(Commands.literal("dialog")
                                            .executes(ctx -> sendOpenExample(ctx, SandboxOpenGuiPacket.GuiType.EXAMPLE_DIALOG)))
                                    .then(Commands.literal("settings")
                                            .executes(ctx -> sendOpenExample(ctx, SandboxOpenGuiPacket.GuiType.EXAMPLE_SETTINGS)))
                                    .then(Commands.literal("tabs")
                                            .executes(ctx -> sendOpenExample(ctx, SandboxOpenGuiPacket.GuiType.EXAMPLE_TABS)))
                                    .then(Commands.literal("dashboard")
                                            .executes(ctx -> sendOpenExample(ctx, SandboxOpenGuiPacket.GuiType.EXAMPLE_DASHBOARD)))
                                    .then(Commands.literal("itemSelection")
                                            .executes(ctx -> sendOpenExample(ctx, SandboxOpenGuiPacket.GuiType.EXAMPLE_ITEM_SELECTION)))
                                    .then(Commands.literal("playerBrowser")
                                            .executes(ctx -> sendOpenExample(ctx, SandboxOpenGuiPacket.GuiType.EXAMPLE_PLAYER_BROWSER)))
                                    .then(Commands.literal("displayShowcase")
                                            .executes(ctx -> sendOpenExample(ctx, SandboxOpenGuiPacket.GuiType.DISPLAY_SHOWCASE))))
                            .then(Commands.literal("giveDisplayBlock")
                                    .executes(context -> {
                                        ServerPlayer player = context.getSource().getPlayerOrException();
                                        ItemStack displayBlock = new ItemStack(SandboxRegistration.DISPLAY_DEMO_BLOCK.get());
                                        if (!player.getInventory().add(displayBlock)) {
                                            player.drop(displayBlock, false);
                                        }
                                        player.sendSystemMessage(Component.literal("Gave DisplayBlock demo block"));
                                        return 1;
                                    }))
                            .then(Commands.literal("giveDisplayPanel")
                                    .executes(context -> {
                                        ServerPlayer player = context.getSource().getPlayerOrException();
                                        ItemStack displayPanel = new ItemStack(SandboxRegistration.DISPLAY_DEMO_PANEL_BLOCK.get());
                                        if (!player.getInventory().add(displayPanel)) {
                                            player.drop(displayPanel, false);
                                        }
                                        player.sendSystemMessage(Component.literal("Gave DisplayPanel demo block"));
                                        return 1;
                                    }))
                            .then(Commands.literal("giveBackPanel")
                                    .executes(context -> {
                                        ServerPlayer player = context.getSource().getPlayerOrException();
                                        ItemStack backPanel = new ItemStack(SandboxRegistration.DISPLAY_DEMO_BACK_PANEL_BLOCK.get());
                                        if (!player.getInventory().add(backPanel)) {
                                            player.drop(backPanel, false);
                                        }
                                        player.sendSystemMessage(Component.literal("Gave BackPanel demo block"));
                                        return 1;
                                    }))
                            .then(Commands.literal("giveChartDemo")
                                    .executes(context -> {
                                        ServerPlayer player = context.getSource().getPlayerOrException();
                                        ItemStack chartBlock = new ItemStack(SandboxRegistration.CHART_DEMO_BLOCK.get());
                                        if (!player.getInventory().add(chartBlock)) {
                                            player.drop(chartBlock, false);
                                        }
                                        player.sendSystemMessage(Component.literal("Gave ChartDemo block"));
                                        return 1;
                                    }))
                            .then(Commands.literal("displayProfiler")
                                    .executes(context -> {
                                        boolean nowEnabled = !DisplayRenderProfiler.isEnabled();
                                        DisplayRenderProfiler.setEnabled(nowEnabled);
                                        context.getSource().sendSuccess(
                                                () -> Component.literal("Display profiler " + (nowEnabled ? "enabled" : "disabled")),
                                                false);
                                        return 1;
                                    }))
            );
        }

        private static int sendOpenExample(CommandContext<CommandSourceStack> ctx, SandboxOpenGuiPacket.GuiType type) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            SandboxOpenGuiPacket.send(player, type);
            return 1;
        }
    }

    public static SandboxNetwork network = null;

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
        // Register sandbox blocks, items, and block entities
        SandboxRegistration.register();
        net.kroia.modutilities.gui.GuiElementRegistry.register(
                "sandbox_line_chart", SandboxLineChart.class, SandboxLineChart::new);

        // Register client-side renderers and input handlers (only on physical client)
        if (UtilitiesPlatform.isClient()) {
            SandboxRegistration.registerClient();
            dev.architectury.event.events.client.ClientTickEvent.CLIENT_POST.register(mc -> {
                DisplayInputHandler.clientTick();
            });
        }

        if (TestRegistry.ENABLE_TESTS) {
            TestRegistry.register(new EventTests());
            if (UtilitiesPlatform.isClient()) {
                TestRegistry.register(new CreativeTabTests());
                TestRegistry.register(new GuiLogicTests());
            }
            TestRegistry.register(new ParserTests());
            TestRegistry.register(new PersistenceTests());
            TestRegistry.register(new SettingsTests());
            TestRegistry.register(new NetworkingTests());
            TestRegistry.register(new StreamingTests());
            TestRegistry.register(new UtilityTests());
        }

        CommandRegistrationEvent.EVENT.register((dispatcher, registryAccess, environment) -> {
            SandboxCommand.register(dispatcher);
            boolean isSlave = false; // TODO: detect from MultiServerManager if available
            TestCommandRegistration.register(dispatcher, "modutilities", "ModUtilities", isSlave);
        });
        network = new SandboxNetwork();
        //TABS.register();


    }

}
