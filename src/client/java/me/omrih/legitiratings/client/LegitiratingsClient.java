package me.omrih.legitiratings.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LegitiratingsClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger(LegitiratingsClient.class);
    public static boolean getRateMessage = false;

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("rate").executes((context -> {
                getRateMessage = true;
                Minecraft.getInstance().player.connection.sendCommand("uuid");
                return 1;
            })));
        });
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            Component failed = Component.literal("You are not in a legitimoose world!").withStyle(ChatFormatting.RED);
            if (message.getString().equals(failed.getString())) return true;

            if (getRateMessage && message.getStyle().getClickEvent() != null && message.getString().matches("World UUID: .* \\(click to copy\\)")) {
                String uuid = ((ClickEvent.CopyToClipboard) message.getStyle().getClickEvent()).value();
                Minecraft.getInstance().setScreen(new RateScreen(Component.empty(), uuid));
                getRateMessage = false;
                return false;
            } else if (getRateMessage) {
                Minecraft.getInstance().player.displayClientMessage(failed, false);
                getRateMessage = false;
                return true;
            }
            return true;
        });
    }
}
