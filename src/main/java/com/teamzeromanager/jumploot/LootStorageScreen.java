package com.teamzeromanager.jumploot;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class LootStorageScreen extends Screen {
    private static final int COLUMNS = 9;
    private static final int ROWS = 4;
    private static final int SLOT = 20;
    private static final int GRID_WIDTH = COLUMNS * SLOT;
    private final List<LootStorage.StoredReward> items = new ArrayList<>();
    private int firstRow;
    private int selected = -1;
    private boolean confirmDiscard;
    private Button revealButton;
    private int animationTicks;

    public LootStorageScreen(List<LootStorage.StoredReward> contents) {
        super(Component.literal("Jump Loot - Colección"));
        for (LootStorage.StoredReward reward : contents) {
            items.add(new LootStorage.StoredReward(reward.stack(), reward.rarity(), reward.revealed(), reward.special()));
        }
    }

    private boolean hasPendingReveal() {
        for (LootStorage.StoredReward reward : items) if (!reward.revealed() || reward.special()) return true;
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        animationTicks++;
    }

    @Override
    protected void init() {
        int left = this.width / 2 - 165;
        int top = this.height / 2 - 116;
        addRenderableWidget(Button.builder(Component.literal("✦"), button ->
                Minecraft.getInstance().setScreen(new LootCatalogScreen(this)))
                .bounds(left + 294, top + 7, 20, 18)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.literal("Ver posibles recompensas")))
                .build());
        revealButton = Button.builder(Component.literal("Reclamar recompensas"), button ->
                JumpNetworkClient.claimAllStorage())
                .bounds(left + 91, top + 190, 148, 18)
                .build();
        revealButton.active = hasPendingReveal();
        addRenderableWidget(revealButton);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        int left = this.width / 2 - 165;
        int top = this.height / 2 - 116;
        int right = left + 330;
        graphics.fill(left, top, right, top + 232, 0xF0100D16);
        graphics.fill(left + 2, top + 2, right - 2, top + 230, 0xF0272231);
        graphics.fill(left + 4, top + 4, right - 4, top + 28, 0xFF17121F);
        graphics.drawCenteredString(this.font, "✦  JUMP LOOT  ·  COLECCIÓN  ✦", this.width / 2, top + 11, 0xFFFFD45A);
        graphics.drawString(this.font, "Recompensas: " + items.size(), left + 12, top + 35, 0xFFE6E0EE);

        int gridLeft = this.width / 2 - GRID_WIDTH / 2;
        int gridTop = top + 52;
        float pulse = .5f + .5f * (float) Math.sin((animationTicks + partialTick) * .18f);
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int index = (firstRow + row) * COLUMNS + col;
                int x = gridLeft + col * SLOT;
                int y = gridTop + row * SLOT;
                boolean occupied = index < items.size();
                boolean hovered = mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18;
                int slotColor = selected == index ? 0xFF80633B : (hovered ? 0xFF62566B : 0xFF514653);
                graphics.fill(x, y, x + 18, y + 18, 0xFF17131C);
                graphics.fill(x + 1, y + 1, x + 17, y + 17, slotColor);
                graphics.fill(x + 2, y + 2, x + 16, y + 16, 0xFF302A37);
                if (occupied) {
                    LootStorage.StoredReward reward = items.get(index);
                    if (reward.special()) {
                        int glow = 150 + (int) (90 * pulse);
                        graphics.fill(x, y, x + 18, y + 18, (glow << 24) | 0x00d5a84f);
                        graphics.drawCenteredString(this.font, "✦", x + 9, y - 1, 0xFFFFD45A);
                        graphics.drawCenteredString(this.font, "?", x + 9, y + 5, 0xFFFFE58A);
                        if (hovered) graphics.renderTooltip(this.font,
                                Component.literal("✦ Recompensa especial · elige cualquier objeto"), mouseX, mouseY);
                    } else if (reward.revealed()) {
                        ItemStack stack = reward.stack();
                        graphics.renderItem(stack, x + 1, y + 1);
                        if (stack.getCount() > 1) graphics.renderItemDecorations(this.font, stack, x + 1, y + 1);
                        if (hovered) graphics.renderTooltip(this.font, stack, mouseX, mouseY);
                    } else {
                        graphics.drawCenteredString(this.font, "?", x + 9, y + 5, 0xFFFFD45A);
                        if (hovered) graphics.renderTooltip(this.font,
                                Component.literal("Recompensa sin revelar"), mouseX, mouseY);
                    }
                }
            }
        }

        int maxRows = (items.size() + COLUMNS - 1) / COLUMNS;
        if (maxRows > ROWS) {
            graphics.drawString(this.font, "Rueda del ratón: desplazarse  (" + (firstRow + 1) + "/" + maxRows + ")", left + 12, top + 137, 0xFFBDB4C9);
        }

        int detailsY = top + 158;
        graphics.fill(left + 9, detailsY, right - 9, detailsY + 34, 0xFF1A1521);
        if (selected >= 0 && selected < items.size()) {
            LootStorage.StoredReward reward = items.get(selected);
            if (reward.special()) {
                graphics.drawCenteredString(this.font, "✦ RECOMPENSA ESPECIAL ✦", this.width / 2, detailsY + 7, 0xFFFFD45A);
                graphics.drawCenteredString(this.font, "Reclámala para elegir tu objeto", this.width / 2, detailsY + 19, 0xFFE6E0EE);
            } else if (reward.revealed()) {
                ItemStack stack = reward.stack();
                graphics.renderItem(stack, left + 16, detailsY + 8);
                graphics.drawString(this.font, stack.getHoverName(), left + 40, detailsY + 7, 0xFFFFFFFF);
                graphics.drawString(this.font, "Cantidad: " + stack.getCount(), left + 40, detailsY + 19, 0xFFFFD45A);
            } else {
                graphics.drawCenteredString(this.font, "Recompensa sin revelar", this.width / 2, detailsY + 13, 0xFFFFD45A);
            }
        } else {
            graphics.drawCenteredString(this.font, "Selecciona una recompensa", this.width / 2, detailsY + 13, 0xFFBDB4C9);
        }

        String action = selected < 0 ? "ENTER: reclamar   ·   D: descartar" :
                (confirmDiscard ? "¿Descartar esta recompensa? Pulsa D otra vez" : "ENTER: reclamar   ·   D: descartar");
        graphics.drawCenteredString(this.font, action, this.width / 2, top + 212, confirmDiscard ? 0xFFFF7777 : 0xFFE6E0EE);
        graphics.drawCenteredString(this.font, "ESC: cerrar", this.width / 2, top + 223, 0xFFBDB4C9);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int gridLeft = this.width / 2 - GRID_WIDTH / 2;
        int gridTop = this.height / 2 - 116 + 52;
        if (mouseX >= gridLeft && mouseX < gridLeft + GRID_WIDTH && mouseY >= gridTop && mouseY < gridTop + ROWS * SLOT) {
            int col = (int) ((mouseX - gridLeft) / SLOT);
            int row = (int) ((mouseY - gridTop) / SLOT);
            int index = (firstRow + row) * COLUMNS + col;
            if (index < items.size()) {
                selected = index;
                confirmDiscard = button == 1;
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int maxRows = (items.size() + COLUMNS - 1) / COLUMNS;
        int maxFirstRow = Math.max(0, maxRows - ROWS);
        firstRow = Math.max(0, Math.min(maxFirstRow, firstRow - (int) Math.signum(delta)));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { onClose(); return true; }
        if (selected >= 0 && selected < items.size() && keyCode == 257) {
            if (items.get(selected).special() || items.get(selected).revealed()) JumpNetworkClient.claimAllStorage();
            return true;
        }
        if (selected >= 0 && selected < items.size() && keyCode == 68) {
            if (confirmDiscard) JumpNetworkClient.discardStorage(selected);
            else confirmDiscard = true;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() { return true; }
}
