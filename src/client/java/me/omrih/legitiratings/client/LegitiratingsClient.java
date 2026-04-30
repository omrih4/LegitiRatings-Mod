package me.omrih.legitiratings.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class LegitiratingsClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger(LegitiratingsClient.class);
    public static boolean getRateMessage = false;

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("legitirate").executes((context -> {
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
        ClientTickEvents.END_CLIENT_TICK.register((client) -> {
            LocalPlayer player = client.player;
            if (player == null) return;
            for (Slot slot : player.containerMenu.slots) {
                if (slot.hasItem()) {
                    ItemStack item = slot.getItem();
                    CompoundTag tag = null;
                    try {
                        tag = item.get(DataComponents.CUSTOM_DATA).copyTag();
                    } catch (NullPointerException ignored) {
                    }
                    if (tag != null) {
                        // return if we have already checked this world
                        if (tag.getString("legitirating").isPresent()) return;

                        CompoundTag publicBukkitValues = (CompoundTag) tag.get("PublicBukkitValues");
                        if (publicBukkitValues != null) {
                            String uuid = publicBukkitValues.getString("datapackserverpaper:uuid").orElse(null);
                            if (uuid != null) {
                                final ItemLore lore = item.get(DataComponents.LORE);
                                CompoundTag tagWithRating = tag;
                                GETManager.getInstance().get().thenAccept((worlds) -> Minecraft.getInstance().execute(() -> {
                                    for (JsonElement world : worlds) {
                                        JsonObject worldObj = world.getAsJsonObject();
                                        if (Objects.equals(worldObj.get("uuid").getAsString(), uuid)) {
                                            String avgRating = worldObj.get("rating").getAsString();
                                            String description = worldObj.get("description").getAsString();
                                            JsonArray reviews = worldObj.getAsJsonArray("ratings");

                                            tagWithRating.putString("legitirating", avgRating);
                                            item.set(DataComponents.CUSTOM_DATA, CustomData.of(tagWithRating));
                                            ItemLore newLore = lore
                                                    .withLineAdded(
                                                            Component.literal(avgRating + "/10 ★").setStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withItalic(false))
                                                    )
                                                    .withLineAdded(
                                                            Component.literal(description).setStyle(Style.EMPTY.withColor(ChatFormatting.AQUA).withItalic(false))
                                                    )
                                                    .withLineAdded(
                                                            Component.literal("➡ Reviews").setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(false))
                                                    );
                                            for (JsonElement review : reviews) {
                                                JsonObject reviewObj = review.getAsJsonObject();

                                                String reviewer = reviewObj.get("reviewer").getAsString();
                                                String rating = reviewObj.get("rating").getAsString();
                                                String userReview = reviewObj.get("review").getAsString();

                                                newLore = newLore
                                                        .withLineAdded(
                                                                Component.literal(reviewer + ' ').setStyle(Style.EMPTY.withColor(ChatFormatting.WHITE).withItalic(false))
                                                                        .append(Component.literal(rating + "/10 ★").setStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withItalic(false)))
                                                                        .append(Component.literal(" - ").setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(false)))
                                                                        .append(userReview).setStyle(Style.EMPTY.withColor(ChatFormatting.AQUA).withItalic(false))
                                                        );
                                            }
                                            item.set(DataComponents.LORE, newLore);
                                        }
                                    }
                                }));
                            }
                        }
                    }
                }
            }
        });
    }
}
