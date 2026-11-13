package com.teamzeromanager.jumploot;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Existing rarity catalogue, also used as the special-reward picker. */
public final class LootCatalogScreen extends Screen {
    private static final int COLUMNS = 8;
    private static final int ROWS = 4;
    private static final int SLOT = 24;

    private final List<LootRarity> rarities = List.of(LootRarity.values());
    private final List<ItemStack> visibleItems = new ArrayList<>();
    private LootRarity selectedRarity = LootRarity.COMMON;
    private int scrollRow;
    private int selectedItem = -1;
    private final Screen parentScreen;
    private final boolean selectionMode;
    private int remainingSpecials;
    private boolean waitingForServer;
    private Button confirmButton;

    public LootCatalogScreen(Screen parentScreen) {
        this(parentScreen, 0);
    }

    public LootCatalogScreen(Screen parentScreen, int specialRewards) {
        super(Component.literal("Jump Loot - Catálogo"));
        this.parentScreen = parentScreen;
        this.selectionMode = specialRewards > 0;
        this.remainingSpecials = Math.max(0, specialRewards);
        refreshItems();
    }

    private void refreshItems() {
        visibleItems.clear();
        LootItemCatalog catalog = LootItemCatalog.build();
        visibleItems.addAll(catalog.getItems(selectedRarity));
        scrollRow = 0;
        selectedItem = -1;
        if (confirmButton != null) confirmButton.active = false;
    }

    @Override
    protected void init() {
        int tabWidth = 58;
        int totalWidth = tabWidth * rarities.size() + 4 * (rarities.size() - 1);
        int startX = (width - totalWidth) / 2;
        int y = height / 2 - 76;
        for (int i = 0; i < rarities.size(); i++) {
            LootRarity rarity = rarities.get(i);
            int x = startX + i * (tabWidth + 4);
            addRenderableWidget(Button.builder(Component.literal(rarity.name()), button -> {
                selectedRarity = rarity;
                refreshItems();
            }).bounds(x, y, tabWidth, 20).build());
        }
        if (selectionMode) {
            confirmButton = Button.builder(Component.literal("Confirmar elección"), button -> confirmSelection())
                    .bounds(width / 2 - 65, height / 2 + 78, 130, 20).build();
            confirmButton.active = false;
            addRenderableWidget(confirmButton);
        }
    }

    private void confirmSelection() {
        if (!selectionMode || waitingForServer || selectedItem < 0 || selectedItem >= visibleItems.size()) return;
        ItemStack selected = visibleItems.get(selectedItem);
        var itemId = BuiltInRegistries.ITEM.getKey(selected.getItem());
        if (itemId == null) return;
        waitingForServer = true;
        if (confirmButton != null) confirmButton.active = false;
        JumpNetworkClient.claimSpecial(selectedRarity, itemId);
    }

    public void onSpecialChoiceResult(boolean success, int remaining) {
        waitingForServer = false;
        remainingSpecials = Math.max(0, remaining);
        if (success && remainingSpecials == 0) {
            JumpNetworkClient.requestStorage();
            return;
        }
        if (success) {
            selectedItem = -1;
            if (confirmButton != null) confirmButton.active = false;
        } else if (confirmButton != null) {
            confirmButton.active = selectedItem >= 0;
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
        graphics.drawCenteredString(font,
                selectionMode ? "✦  ELIGE TU RECOMPENSA  ✦" : "✦  POSIBLES RECOMPENSAS  ✦",
                cx, top + 7, 0xFFFFD45A);
        super.render(graphics, mouseX, mouseY, partialTick);

        int gridLeft = cx - (COLUMNS * SLOT) / 2;
        int gridTop = top + 49;
        int maxRows = Math.max(1, (visibleItems.size() + COLUMNS - 1) / COLUMNS);
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int index = (scrollRow + row) * COLUMNS + col;
                int x = gridLeft + col * SLOT;
                int y = gridTop + row * SLOT;
                boolean selected = index == selectedItem;
                graphics.fill(x, y, x + 20, y + 20, selected ? 0xFFFFD45A : 0xFF17131C);
                graphics.fill(x + 1, y + 1, x + 19, y + 19, selected ? 0xFF80633B : 0xFF403747);
                if (index < visibleItems.size()) {
                    ItemStack stack = visibleItems.get(index);
                    graphics.renderItem(stack, x + 2, y + 2);
                    if (mouseX >= x && mouseX < x + 20 && mouseY >= y && mouseY < y + 20) {
                        graphics.renderTooltip(font, stack, mouseX, mouseY);
                    }
                }
            }
        }
        graphics.drawCenteredString(font,
                selectedRarity.name() + "  ·  " + visibleItems.size() + " objetos",
                cx, top + 151, selectedRarity.getColor());
        if (selectionMode) {
            String status = waitingForServer ? "Guardando elección..." :
                    "Recompensas especiales disponibles: " + remainingSpecials;
            graphics.drawCenteredString(font, status, cx, top + 164, 0xffffd45a);
            graphics.drawCenteredString(font, "Selecciona un objeto y confirma", cx, top + 181, 0xffbdb4c9);
        } else {
            graphics.drawCenteredString(font,
                    "Rueda del ratón: explorar objetos  (" + (scrollRow + 1) + "/" + maxRows + ")",
                    cx, top + 167, 0xFFBDB4C9);
            graphics.drawCenteredString(font, "ESC: volver al inventario", cx, top + 184, 0xFFE6E0EE);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (selectionMode && button == 0) {
            int gridLeft = width / 2 - (COLUMNS * SLOT) / 2;
            int gridTop = height / 2 - 100 + 49;
            if (mouseX >= gridLeft && mouseX < gridLeft + COLUMNS * SLOT
                    && mouseY >= gridTop && mouseY < gridTop + ROWS * SLOT) {
                int col = (int) ((mouseX - gridLeft) / SLOT);
                int row = (int) ((mouseY - gridTop) / SLOT);
                int index = (scrollRow + row) * COLUMNS + col;
                if (index < visibleItems.size()) {
                    selectedItem = index;
                    if (confirmButton != null) confirmButton.active = !waitingForServer;
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int maxRows = Math.max(1, (visibleItems.size() + COLUMNS - 1) / COLUMNS);
        scrollRow = Mth.clamp(scrollRow - (int) Math.signum(delta), 0, Math.max(0, maxRows - ROWS));
        return true;
    }

    @Override
    public void onClose() {
        if (parentScreen != null) Minecraft.getInstance().setScreen(parentScreen);
        else if (selectionMode) JumpNetworkClient.requestStorage();
        else Minecraft.getInstance().setScreen(null);
    }

    @Override
    public boolean isPauseScreen() { return true; }
}
