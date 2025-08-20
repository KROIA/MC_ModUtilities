package net.kroia.modutilities.sandbox;

import com.google.gson.JsonElement;
import com.mojang.brigadier.CommandDispatcher;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.kroia.modutilities.JsonUtilities;
import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.networking.NetworkManager;
import net.kroia.modutilities.networking.NetworkPacket;
import net.kroia.modutilities.setting.parser.ItemStackJsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class Sandbox {
    private static class SandboxNetwork extends NetworkManager
    {
        public SandboxNetwork() {
            super(ModUtilitiesMod.MOD_ID);
            setupClientReceiverPackets();
            setupServerReceiverPackets();
            this.setupARRS(); // Setup the Asynchronous Request Response System (ARRS)
        }

        @Override
        public void setupClientReceiverPackets() {
            register(SandboxOpenGuiPacked.class, SandboxOpenGuiPacked::encode, SandboxOpenGuiPacked::new, SandboxOpenGuiPacked::receive);

        }

        @Override
        public void setupServerReceiverPackets() {

        }
    }

    private static class SandboxOpenGuiPacked extends NetworkPacket {

        enum GuiType {
            TEST_SCREEN,
            ANOTHER_SCREEN
        }
        private GuiType guiType;
        public SandboxOpenGuiPacked(GuiType guiType) {
            this.guiType = guiType;
        }
        public SandboxOpenGuiPacked() {
            super();
        }
        public SandboxOpenGuiPacked(FriendlyByteBuf friendlyByteBuf) {
            super(friendlyByteBuf);
        }

        public static void send(ServerPlayer receiver, GuiType guiType) {
            SandboxOpenGuiPacked packet = new SandboxOpenGuiPacked(guiType);
            network.sendToClient(receiver, packet);
        }
        @Override
        public void decode(FriendlyByteBuf buf) {
            int ordinal = buf.readInt();
            this.guiType = GuiType.values()[ordinal];
        }

        @Override
        public void encode(FriendlyByteBuf buf) {
            buf.writeInt(guiType.ordinal());
        }

        @Override
        protected void handleOnClient() {
            switch(guiType) {
                case TEST_SCREEN:
                    Minecraft.getInstance().setScreen(new TestScreen());
                    break;
                case ANOTHER_SCREEN:
                    // Open another GUI
                    break;
                default:
            }
        }
    }

    private static final class SandboxCommand
    {
        public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
            dispatcher.register(
                    Commands.literal("modutilities")
                            .then(Commands.literal("openTestScreen")
                                    .executes(context -> {
                                        ServerPlayer player = context.getSource().getPlayerOrException();
                                        SandboxOpenGuiPacked.send(player, SandboxOpenGuiPacked.GuiType.TEST_SCREEN);
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
            );
        }
    }

    private static SandboxNetwork network = null;


    public static void init()
    {
        CommandRegistrationEvent.EVENT.register((dispatcher, registryAccess, environment) -> {
            SandboxCommand.register(dispatcher);
        });
        network = new SandboxNetwork();
    }
}
