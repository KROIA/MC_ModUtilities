package net.kroia.modutilities.sandbox;

import dev.architectury.networking.NetworkManager;
import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.networking.ExtraCodecUtils;
import net.kroia.modutilities.networking.client_server.NetworkPacket;
import net.kroia.modutilities.networking.multi_server.ForwardPacketContext;
import net.kroia.modutilities.sandbox.gui.ExampleDashboardScreen;
import net.kroia.modutilities.sandbox.gui.ExampleDialogScreen;
import net.kroia.modutilities.sandbox.gui.ExampleFormScreen;
import net.kroia.modutilities.sandbox.gui.ExampleItemSelectionScreen;
import net.kroia.modutilities.sandbox.gui.ExamplePlayerBrowserScreen;
import net.kroia.modutilities.sandbox.gui.ExampleSettingsScreen;
import net.kroia.modutilities.sandbox.gui.ExampleTabsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class SandboxOpenGuiPacket extends NetworkPacket {
    public static Sandbox.SandboxNetwork network;

    public static final Type<SandboxOpenGuiPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ModUtilitiesMod.MOD_ID, "sandbox_open_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SandboxOpenGuiPacket> STREAM_CODEC = StreamCodec.composite(
            ExtraCodecUtils.enumStreamCodec(GuiType.class), p -> p.guiType,
            SandboxOpenGuiPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public enum GuiType {
        TEST_SCREEN,
        ANOTHER_SCREEN,
        EXAMPLE_FORM,
        EXAMPLE_DIALOG,
        EXAMPLE_SETTINGS,
        EXAMPLE_TABS,
        EXAMPLE_DASHBOARD,
        EXAMPLE_ITEM_SELECTION,
        EXAMPLE_PLAYER_BROWSER,
        DISPLAY_SHOWCASE
    }
    private SandboxOpenGuiPacket.GuiType guiType;
    public SandboxOpenGuiPacket(SandboxOpenGuiPacket.GuiType guiType) {
        this.guiType = guiType;
    }
    public SandboxOpenGuiPacket() {
        super();
    }

    @Override
    protected void handleOnClient(NetworkManager.PacketContext context) {
        switch(guiType) {
            case TEST_SCREEN:
                SandboxClientHooks.openTestScreen();
                break;
            case ANOTHER_SCREEN:
                // Open another GUI
                break;
            case EXAMPLE_FORM:
                Minecraft.getInstance().submit(() -> ExampleFormScreen.open());
                break;
            case EXAMPLE_DIALOG:
                Minecraft.getInstance().submit(() -> ExampleDialogScreen.open());
                break;
            case EXAMPLE_SETTINGS:
                Minecraft.getInstance().submit(() -> ExampleSettingsScreen.open());
                break;
            case EXAMPLE_TABS:
                Minecraft.getInstance().submit(() -> ExampleTabsScreen.open());
                break;
            case EXAMPLE_DASHBOARD:
                Minecraft.getInstance().submit(() -> ExampleDashboardScreen.open());
                break;
            case EXAMPLE_ITEM_SELECTION:
                Minecraft.getInstance().submit(() -> ExampleItemSelectionScreen.open());
                break;
            case EXAMPLE_PLAYER_BROWSER:
                Minecraft.getInstance().submit(() -> ExamplePlayerBrowserScreen.open());
                break;
            case DISPLAY_SHOWCASE:
                Minecraft.getInstance().submit(() -> ExampleDashboardScreen.open());
                break;
            default:
        }
    }

    @Override
    protected void handleOnServer(NetworkManager.PacketContext context) {

    }

    @Override
    protected void handleOnMaster(ForwardPacketContext context) {

    }

    @Override
    protected void handleOnSlave(ForwardPacketContext context) {

    }


    public static void send(ServerPlayer receiver, SandboxOpenGuiPacket.GuiType guiType) {
        if (network == null) {
            ModUtilitiesMod.LOGGER.warn("SandboxOpenGuiPacket.send() called but network is null — sandbox network not registered");
            return;
        }
        SandboxOpenGuiPacket packet = new SandboxOpenGuiPacket(guiType);
        network.sendToClient(receiver, packet);
    }



}
