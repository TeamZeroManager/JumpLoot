package com.teamzeromanager.jumploot;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class LootStorageScreen extends Screen {
    private static final int ROW_HEIGHT = 24;
    private final List<ItemStack> items = new ArrayList<>();
    private int scroll;
    private int selected = -1;
    private boolean confirmDiscard;

    public LootStorageScreen(List<ItemStack> contents) {
        super(Component.literal("Jump Loot - Colección"));
        for (ItemStack stack : contents) items.add(stack.copy());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        int left = this.width / 2 - 150;
        int top = this.height / 2 - 105;
        int right = left + 300;
        graphics.fill(left, top, right, top + 210, 0xE91A1720);
        graphics.fill(left + 2, top + 2, right - 2, top + 208, 0xE92A2533);
        graphics.drawCenteredString(this.font, "JUMP LOOT - COLECCIÓN", this.width / 2, top + 10, 0xFFFFD45A);
        graphics.drawString(this.font, "Objetos guardados: " + items.size(), left + 12, top + 27, 0xFFE6E0EE);

        int visible = 6;
        for (int row = 0; row < visible; row++) {
            int index = scroll + row;
            if (index >= items.size()) break;
            int y = top + 43 + row * ROW_HEIGHT;
            graphics.fill(left + 9, y, right - 9, y + 21,
                    selected == index ? 0xFF51445F : 0xFF342D3E);
            ItemStack stack = items.get(index);
            graphics.renderItem(stack, left + 14, y + 2);
            graphics.drawString(this.font, stack.getHoverName(), left + 38, y + 7, 0xFFFFFFFF);
            graphics.drawString(this.font, "x" + stack.getCount(), right - 35, y + 7, 0xFFFFD45A);
        }

        if (items.isEmpty()) {
            graphics.drawCenteredString(this.font, "Todavía no tienes recompensas.", this.width / 2, top + 105, 0xFFB8B0C5);
        }

        String actionText = selected < 0 ? "Selecciona un objeto" :
                (confirmDiscard ? "¿Descartar? Pulsa D para confirmar" : "ENTER: reclamar   |   D: descartar");
        graphics.drawCenteredString(this.font, actionText, this.width / 2, top + 190, 0xFFB8B0C5);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int left = this.width / 2 - 150;
        int top = this.height / 2 - 105;
        if (mouseX >= left + 9 && mouseX <= left + 291 && mouseY >= top + 43 && mouseY < top + 43 + 6 * ROW_HEIGHT) {
            int row = (int) ((mouseY - (top + 43)) / ROW_HEIGHT);
            int index = scroll + row;
            if (index >= 0 && index < items.size()) {
                selected = index;
                confirmDiscard = false;
                if (button == 1) confirmDiscard = true;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 265) { scroll = Math.max(0, scroll - 1); return true; }
        if (keyCode == 264) { scroll = Math.min(Math.max(0, items.size() - 6), scroll + 1); return true; }
        if (keyCode == 256) { onClose(); return true; }
        if (selected >= 0 && selected < items.size() && keyCode == 257) {
            JumpNetwork.claimStorage(selected);
            return true;
        }
        if (selected >= 0 && selected < items.size() && keyCode == 68) {
            if (confirmDiscard) JumpNetwork.discardStorage(selected);
            else confirmDiscard = true;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
