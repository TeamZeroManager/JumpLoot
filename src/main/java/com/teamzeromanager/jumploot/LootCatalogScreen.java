package com.teamzeromanager.jumploot;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Read-only preview of the items that can appear in each rarity tier. */
public final class LootCatalogScreen extends Screen {
    private static final int COLUMNS = 8;
    private static final int ROWS = 4;
    private static final int SLOT = 24;

    private final List<LootRarity> rarities = List.of(LootRarity.values());
    private final List<ItemStack> visibleItems = new ArrayList<>();
    private LootRarity selectedRarity = LootRarity.COMMON;
    private int scrollRow;

    public LootCatalogScreen() {
        super(Component.literal("Jump Loot - Recompensas"));
        refreshItems();
    }

    private void refreshItems() {
        visibleItems.clear();
        LootItemCatalog catalog = LootItemCatalog.build();
        visibleItems.addAll(catalog.getItems(selectedRarity));
        scrollRow = 0;
    }

    @Override
    protected void init() {
        int tabWidth = 62;
        int totalWidth = tabWidth * rarities.size() + 4 * (rarities.size() - 1);
        int startX = (width - totalWidth) / 2;
        int y = height / 2 - 91;
        for (int i = 0; i < rarities.size(); i++) {
            LootRarity rarity = rarities.get(i);
            int x = startX + i * (tabWidth + 4);
            addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
                    Component.literal(rarity.name()),
                    button -> {
                        selectedRarity = rarity;
                        refreshItems();
                    }).bounds(x, y, tabWidth, 20).build());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int cx = width / 2;
        int top = height / 2 - 100;
        int left = cx - 157;
        graphics.fill(left, top, left + 314, top + 200, 0xF0100D16);
        graphics.fill(left + 2, top + 2, left + 312, top + 198, 0xF0272231);
        graphics.drawCenteredString(font, "✦  POSIBLES RECOMPENSAS  ✦", cx, top + 7, 0xFFFFD45A);
        super.render(graphics, mouseX, mouseY, partialTick);

        int gridLeft = cx - (COLUMNS * SLOT) / 2;
        int gridTop = top + 45;
        int maxRows = Math.max(1, (visibleItems.size() + COLUMNS - 1) / COLUMNS);
        int shown = 0;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int index = (scrollRow + row) * COLUMNS + col;
                int x = gridLeft + col * SLOT;
                int y = gridTop + row * SLOT;
                graphics.fill(x, y, x + 20, y + 20, 0xFF17131C);
                graphics.fill(x + 1, y + 1, x + 19, y + 19, 0xFF403747);
                if (index < visibleItems.size()) {
                    ItemStack stack = visibleItems.get(index);
                    graphics.renderItem(stack, x + 2, y + 2);
                    shown++;
                    if (mouseX >= x && mouseX < x + 20 && mouseY >= y && mouseY < y + 20) {
                        graphics.renderTooltip(font, stack, mouseX, mouseY);
                    }
                }
            }
        }
        graphics.drawCenteredString(font,
                selectedRarity.name() + "  ·  " + visibleItems.size() + " objetos",
                cx, top + 148, selectedRarity.getColor());
        graphics.drawCenteredString(font,
                "Rueda del ratón: explorar objetos  (" + (scrollRow + 1) + "/" + maxRows + ")",
                cx, top + 164, 0xFFBDB4C9);
        graphics.drawCenteredString(font, "ESC: volver al inventario", cx, top + 181, 0xFFE6E0EE);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int maxRows = Math.max(1, (visibleItems.size() + COLUMNS - 1) / COLUMNS);
        scrollRow = Mth.clamp(scrollRow - (int) Math.signum(delta), 0, Math.max(0, maxRows - ROWS));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
