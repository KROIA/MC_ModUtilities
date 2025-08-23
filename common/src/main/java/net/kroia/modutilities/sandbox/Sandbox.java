package net.kroia.modutilities.sandbox;

import com.google.gson.JsonElement;
import com.mojang.brigadier.CommandDispatcher;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.kroia.modutilities.JsonUtilities;
import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.networking.NetworkManager;
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
    public static class SandboxNetwork extends NetworkManager
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
            register(SandboxOpenGuiPacket.class, SandboxOpenGuiPacket::encode, SandboxOpenGuiPacket::new, SandboxOpenGuiPacket::receive);

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

    public static void init()
    {
        CommandRegistrationEvent.EVENT.register((dispatcher, registryAccess, environment) -> {
            SandboxCommand.register(dispatcher);
        });
        network = new SandboxNetwork();

        //dataArchiveManager = new SandboxDataArchiveManager(Path.of("data/sandbox_data_archive"));
    }
}
