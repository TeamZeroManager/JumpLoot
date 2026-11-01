package com.teamzeromanager.jumploot;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class LootRouletteScreen extends Screen {
    private static final int RARITY_REVEAL_TICKS = 55;
    private final LootRarity rarity;
    private final ItemStack reward;
    private int ticks;

    public LootRouletteScreen(LootRarity rarity, ItemStack reward) {
        super(Component.literal("Jump Loot"));
        this.rarity = rarity;
        this.reward = reward.copy();
    }

    @Override
    public void tick() { ticks++; }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        graphics.fill(centerX - 150, centerY - 95, centerX + 150, centerY + 100, 0xE91A1720);
        graphics.fill(centerX - 148, centerY - 93, centerX + 148, centerY + 98, 0xE92A2533);
        graphics.drawCenteredString(this.font, Component.literal("JUMP LOOT"), centerX, centerY - 78, 0xFFFFD45A);

        if (ticks < RARITY_REVEAL_TICKS) {
            int pulse = (ticks / 5) % 2 == 0 ? 0xFFFFFFFF : rarity.getColor();
            graphics.drawCenteredString(this.font, Component.literal("REVELANDO RAREZA..."),
                    centerX, centerY - 20, 0xFFE6E0EE);
            graphics.drawCenteredString(this.font, Component.literal(rarity.name()),
                    centerX, centerY + 8, pulse);
            graphics.fill(centerX - 90, centerY + 35, centerX + 90, centerY + 38, rarity.getColor());
        } else {
            graphics.drawCenteredString(this.font, Component.literal("¡RAREZA " + rarity.name() + "!"),
                    centerX, centerY - 37, rarity.getColor());
            graphics.renderItem(reward, centerX - 8, centerY - 12);
            graphics.drawCenteredString(this.font, reward.getHoverName(),
                    centerX, centerY + 18, 0xFFFFFFFF);
            graphics.drawCenteredString(this.font, Component.literal("Recompensa guardada en tu colección"),
                    centerX, centerY + 43, 0xFFFFD45A);
            graphics.drawCenteredString(this.font, Component.literal("Pulsa C para abrir tu colección"),
                    centerX, centerY + 62, 0xFFE6E0EE);
        }

        graphics.drawCenteredString(this.font, Component.literal("Pulsa ESC para cerrar"),
                centerX, centerY + 80, 0xFFB8B0C5);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 67 && ticks >= RARITY_REVEAL_TICKS) {
            JumpNetwork.requestStorage();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
