package me.omrih.legitiratings.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class RateScreen extends Screen {
    double rating;
    private final String uuid;

    protected RateScreen(Component title, String uuid) {
        super(title);
        this.uuid = uuid;
    }

    @Override
    protected void init() {
        AbstractSliderButton slider = new AbstractSliderButton((this.width / 2) - 60, 40, 120, 20, Component.literal("Rating"), 0) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(String.valueOf(rating)));
            }

            @Override
            protected void applyValue() {
                rating = Math.round(Mth.clampedLerp(this.value, 0.0, 10.0) * 2.0) / 2.0;
            }
        };

        EditBox description = new EditBox(this.font, (this.width / 2) - 180, 80, 360, 20, CommonComponents.EMPTY);
        description.setMaxLength(200);

        Button submitButton = Button.builder(Component.literal("Submit"), (btn) -> {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:3000/review/" + uuid))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(String.format("{\"rating\":%s,\"description\":\"%s\",\"reviewer\":\"%s\"}", rating, description.getValue(), Minecraft.getInstance().getGameProfile().name())))
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 201) {
                    Minecraft.getInstance().player.displayClientMessage(Component.literal("Successfully submitted rating").withStyle(ChatFormatting.GREEN), false);
                } else if (response.statusCode() == 400) {
                    Minecraft.getInstance().player.displayClientMessage(Component.literal("Failed to submit rating: world already reviewed").withStyle(ChatFormatting.RED), false);
                }
                this.onClose();
            } catch (Exception e) {
                LegitiratingsClient.LOGGER.error("Request failed", e);
            }
        }).bounds((this.width / 2) - 60, this.height - 40, 120, 20).build();

        this.addRenderableWidget(slider);
        this.addRenderableWidget(description);
        this.addRenderableWidget(submitButton);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);

        graphics.drawString(this.font, "Rate World", (this.width / 2) - 30, 40 - this.font.lineHeight - 10, 0xFFFFFFFF, true);
    }
}
