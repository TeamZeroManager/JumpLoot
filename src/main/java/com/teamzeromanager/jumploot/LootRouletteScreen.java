package com.teamzeromanager.jumploot;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Two-stage client presentation. The server has already selected and stored
 * the reward; this screen only animates toward that authoritative result.
 */
public final class LootRouletteScreen extends Screen {
    private enum Phase { RARITY, ITEM, RESULT }

    private static final int RARITY_DURATION = 100;
    private static final int ITEM_DURATION = 150;
    private static final int SLOT_WIDTH = 112;
    private static final int GOLD = 0xFFFFD45A;
    private static final int MUTED = 0xFFBDB4C9;

    private final LootRarity rarity;
    private final ItemStack reward;
    private final List<LootRarity> rarityReel = new ArrayList<>();
    private final List<ItemStack> itemReel = new ArrayList<>();

    private Phase phase = Phase.RARITY;
    private int phaseTick;
    private int rarityWinner;
    private int itemWinner;
    private float rarityOffset;
    private float itemOffset;

    public LootRouletteScreen(LootRarity rarity, ItemStack reward) {
        super(Component.literal("Jump Loot"));
        this.rarity = rarity;
        this.reward = reward.copy();
        buildReels();
    }

    private void buildReels() {
        RandomSource random = RandomSource.create();
        LootRarity[] tiers = LootRarity.values();
        for (int i = 0; i < 31; i++) {
            rarityReel.add(tiers[random.nextInt(tiers.length)]);
        }
        rarityWinner = 25;
        rarityReel.set(rarityWinner, rarity);

        // Use the client registry only for decorative passing items. The final
        // item is always the exact stack selected and stored by the server.
        List<ItemStack> candidates = new ArrayList<>();
        try {
            candidates.addAll(LootItemCatalog.build().getItems(rarity));
        } catch (RuntimeException ignored) {
            // A missing client-side candidate list must not prevent the reward screen.
        }
        if (candidates.isEmpty()) candidates.add(reward.copy());

        for (int i = 0; i < 41; i++) {
            itemReel.add(candidates.get(random.nextInt(candidates.size())).copy());
        }
        itemWinner = 33;
        itemReel.set(itemWinner, reward.copy());
    }

    @Override
    public void tick() {
        super.tick();
        phaseTick++;
        if (phase == Phase.RARITY) {
            float progress = Math.min(1.0F, phaseTick / (float) RARITY_DURATION);
            rarityOffset = easedTravel(progress, rarityWinner * SLOT_WIDTH);
            if (phaseTick >= RARITY_DURATION) {
                phase = Phase.ITEM;
                phaseTick = 0;
            }
        } else if (phase == Phase.ITEM) {
            float progress = Math.min(1.0F, phaseTick / (float) ITEM_DURATION);
            itemOffset = easedTravel(progress, itemWinner * SLOT_WIDTH);
            if (phaseTick >= ITEM_DURATION) {
                phase = Phase.RESULT;
            }
        }
    }

    private float easedTravel(float progress, float distance) {
        // Cubic ease-out: fast start, smooth deceleration, exact final position.
        float inverse = 1.0F - progress;
        return distance * (1.0F - inverse * inverse * inverse);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        graphics.fill(centerX - 226, centerY - 126, centerX + 226, centerY + 126, 0xFF08060C);
        graphics.fill(centerX - 224, centerY - 124, centerX + 224, centerY + 124, GOLD);
        graphics.fill(centerX - 221, centerY - 121, centerX + 221, centerY + 121, 0xF0181420);
        graphics.fill(centerX - 216, centerY - 116, centerX + 216, centerY + 116, 0xF0252030);
        if (phase != Phase.RARITY) {
            drawRarityAtmosphere(graphics, centerX, centerY, rarity, phase == Phase.RESULT);
        }
        graphics.drawCenteredString(this.font, "✦  JUMP LOOT  ✦", centerX, centerY - 104, GOLD);
        graphics.drawCenteredString(this.font, "Una recompensa por tus saltos", centerX, centerY - 88, MUTED);

        if (phase == Phase.RARITY) {
            graphics.drawCenteredString(this.font, "01  ·  SELECCIÓN DE RAREZA", centerX, centerY - 72, GOLD);
            drawRarityReel(graphics, centerX, centerY - 25, rarityOffset);
        } else if (phase == Phase.ITEM) {
            graphics.drawCenteredString(this.font, "02  ·  SELECCIÓN DEL OBJETO", centerX, centerY - 72, rarity.getColor());
            graphics.drawCenteredString(this.font, "Rareza: " + rarity.name(), centerX, centerY - 55, rarity.getColor());
            drawItemReel(graphics, centerX, centerY + 2, itemOffset);
        } else {
            drawRevealBurst(graphics, centerX, centerY - 18, rarity);
            graphics.drawCenteredString(this.font, "¡RECOMPENSA OBTENIDA!", centerX, centerY - 55, rarity.getColor());
            graphics.renderItem(reward, centerX - 8, centerY - 30);
            graphics.drawCenteredString(this.font, reward.getHoverName(), centerX, centerY - 5, 0xFFFFFFFF);
            graphics.drawCenteredString(this.font, "Rareza: " + rarity.name(), centerX, centerY + 13, rarity.getColor());
            graphics.drawCenteredString(this.font, "Guardada en tu colección", centerX, centerY + 36, 0xFFFFD45A);
            graphics.drawCenteredString(this.font, "Pulsa C para abrir tu colección", centerX, centerY + 58, 0xFFE6E0EE);
        }

        if (phase != Phase.RESULT) {
            graphics.drawCenteredString(this.font, "La ruleta se detendrá en tu recompensa", centerX, centerY + 88, 0xFFB8B0C5);
        }
        graphics.drawCenteredString(this.font, "Pulsa ESC para cerrar", centerX, centerY + 101, 0xFFB8B0C5);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawRarityReel(GuiGraphics graphics, int centerX, int y, float offset) {
        graphics.enableScissor(centerX - 205, y - 25, centerX + 205, y + 25);
        for (int i = 0; i < rarityReel.size(); i++) {
            int x = centerX - 56 + i * SLOT_WIDTH - Math.round(offset);
            if (x < centerX - 210 || x > centerX + 210) continue;
            LootRarity tier = rarityReel.get(i);
            graphics.fill(x, y - 21, x + SLOT_WIDTH - 6, y + 21, 0xFF09070D);
            graphics.fill(x + 1, y - 20, x + SLOT_WIDTH - 7, y + 20, i == rarityWinner ? tier.getColor() : 0xFF51465C);
            graphics.fill(x + 2, y - 19, x + SLOT_WIDTH - 8, y + 19, 0xFF30283A);
            graphics.drawCenteredString(this.font, tier.name(), x + (SLOT_WIDTH - 6) / 2, y - 4, tier.getColor());
        }
        graphics.disableScissor();
        graphics.fill(centerX - 2, y - 29, centerX + 2, y + 29, GOLD);
        graphics.fill(centerX - 9, y - 32, centerX + 9, y - 29, GOLD);
        graphics.fill(centerX - 9, y + 29, centerX + 9, y + 32, GOLD);
    }

    private void drawItemReel(GuiGraphics graphics, int centerX, int y, float offset) {
        graphics.enableScissor(centerX - 205, y - 30, centerX + 205, y + 30);
        for (int i = 0; i < itemReel.size(); i++) {
            int x = centerX - 56 + i * SLOT_WIDTH - Math.round(offset);
            if (x < centerX - 210 || x > centerX + 210) continue;
            ItemStack stack = itemReel.get(i);
            graphics.fill(x, y - 26, x + SLOT_WIDTH - 6, y + 26, 0xFF09070D);
            if (i == itemWinner) {
                int pulse = 2 + (int) (Math.sin((phaseTick + 1) * 0.22F) * 2.0F);
                graphics.fill(x - pulse, y - 26 - pulse, x + SLOT_WIDTH - 6 + pulse, y + 26 + pulse, withAlpha(rarity.getColor(), 150));
            }
            graphics.fill(x + 1, y - 25, x + SLOT_WIDTH - 7, y + 25, i == itemWinner ? rarity.getColor() : 0xFF51465C);
            graphics.fill(x + 2, y - 24, x + SLOT_WIDTH - 8, y + 24, 0xFF30283A);
            graphics.renderItem(stack, x + 5, y - 8);
            graphics.drawString(this.font, this.font.plainSubstrByWidth(stack.getHoverName().getString(), 78), x + 25, y - 3, 0xFFFFFFFF, false);
        }
        graphics.disableScissor();
        graphics.fill(centerX - 2, y - 36, centerX + 2, y + 36, GOLD);
        graphics.fill(centerX - 9, y - 39, centerX + 9, y - 36, GOLD);
        graphics.fill(centerX - 9, y + 36, centerX + 9, y + 39, GOLD);
    }

    /**
     * Tier-specific ambient glow. Higher tiers receive more motes and a stronger pulse.
     * All effects are drawn with lightweight GUI primitives (no external assets).
     */
    private void drawRarityAtmosphere(GuiGraphics graphics, int cx, int cy, LootRarity tier, boolean reveal) {
        int color = tier.getColor();
        int level = switch (tier) {
            case COMMON -> 1;
            case UNCOMMON -> 2;
            case RARE -> 3;
            case EPIC -> 4;
            case LEGENDARY -> 5;
        };
        float pulse = 0.5F + 0.5F * (float) Math.sin((phaseTick + 1) * (reveal ? 0.18F : 0.10F));
        int alpha = 35 + (int) (pulse * (level * 13));
        int inset = 4 + level * 2;
        graphics.fill(cx - 216 + inset, cy - 116 + inset, cx + 216 - inset, cy - 114 + inset, withAlpha(color, alpha));
        graphics.fill(cx - 216 + inset, cy + 114 - inset, cx + 216 - inset, cy + 116 - inset, withAlpha(color, alpha));
        graphics.fill(cx - 216 + inset, cy - 116 + inset, cx - 214 + inset, cy + 116 - inset, withAlpha(color, alpha));
        graphics.fill(cx + 214 - inset, cy - 116 + inset, cx + 216 - inset, cy + 116 - inset, withAlpha(color, alpha));

        int motes = 3 + level * 5 + (reveal ? level * 3 : 0);
        for (int i = 0; i < motes; i++) {
            double seed = i * 2.399963229728653;
            double orbit = 24 + ((i * 17 + phaseTick * (reveal ? 3 : 1)) % 155);
            int x = cx + (int) (Math.cos(seed + phaseTick * 0.035) * orbit * 1.25);
            int y = cy + (int) (Math.sin(seed + phaseTick * 0.045) * orbit * 0.48);
            int size = level >= 4 && i % 4 == 0 ? 3 : 2;
            graphics.fill(x, y, x + size, y + size, withAlpha(color, 70 + (int) (pulse * 130)));
        }
    }

    /** A stronger, short-lived burst when the final reward is revealed. */
    private void drawRevealBurst(GuiGraphics graphics, int cx, int cy, LootRarity tier) {
        int color = tier.getColor();
        int level = switch (tier) {
            case COMMON -> 1;
            case UNCOMMON -> 2;
            case RARE -> 3;
            case EPIC -> 4;
            case LEGENDARY -> 5;
        };
        float pulse = 0.5F + 0.5F * (float) Math.sin((phaseTick + 1) * 0.24F);
        int radius = 22 + level * 5 + (int) (pulse * (8 + level * 5));
        graphics.fill(cx - radius, cy - 1, cx + radius, cy + 1, withAlpha(color, 35 + level * 18));
        graphics.fill(cx - 1, cy - radius, cx + 1, cy + radius, withAlpha(color, 35 + level * 18));
        int sparks = 4 + level * 5;
        for (int i = 0; i < sparks; i++) {
            double angle = (Math.PI * 2.0 * i / sparks) + phaseTick * 0.025;
            int distance = radius + (i % 3) * 7;
            int x = cx + (int) (Math.cos(angle) * distance);
            int y = cy + (int) (Math.sin(angle) * distance * 0.55);
            graphics.fill(x - 1, y - 1, x + 2, y + 2, withAlpha(color, 100 + (int) (pulse * 155)));
        }
    }

    private int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 67 && phase == Phase.RESULT) {
            JumpNetwork.requestStorage();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() { return true; }
}
