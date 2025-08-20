package net.kroia.modutilities.sandbox;

import com.mojang.brigadier.CommandDispatcher;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.networking.NetworkManager;
import net.kroia.modutilities.networking.NetworkPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

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
