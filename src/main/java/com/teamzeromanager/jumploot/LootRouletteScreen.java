package com.teamzeromanager.jumploot;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Simultaneous multi-reward gacha reveal. */
public final class LootRouletteScreen extends Screen {
    private enum Phase { RARITY, ITEM, RESULT }
    private static final int RARITY_TICKS = 50;
    private static final int ITEM_TICKS = 50;
    private static final int PANEL_EDGE = 0xffd5a84f;

    private final List<LootStorage.StoredReward> rewards = new ArrayList<>();
    private final List<List<Particle>> particles = new ArrayList<>();
    private final int specialCount;
    private Phase phase = Phase.RARITY;
    private int ticks;

    private static final class Particle {
        final float angle, radius, speed, delay;
        final int size;
        Particle(RandomSource random) {
            angle = random.nextFloat() * (float) (Math.PI * 2);
            radius = 18 + random.nextFloat() * 34;
            speed = .5f + random.nextFloat();
            delay = random.nextFloat() * .45f;
            size = 1 + random.nextInt(2);
        }
    }

    public LootRouletteScreen(List<LootStorage.StoredReward> list) { this(list, 0); }

    public LootRouletteScreen(List<LootStorage.StoredReward> list, int specialCount) {
        super(Component.literal("Jump Loot"));
        this.specialCount = Math.max(0, specialCount);
        for (LootStorage.StoredReward reward : list) {
            rewards.add(new LootStorage.StoredReward(reward.stack(), reward.rarity()));
            RandomSource random = RandomSource.create();
            List<Particle> rewardParticles = new ArrayList<>();
            for (int i = 0; i < 28; i++) rewardParticles.add(new Particle(random));
            particles.add(rewardParticles);
        }
    }

    @Override
    public void tick() {
        super.tick();
        ticks++;
        if (phase == Phase.RARITY && ticks >= RARITY_TICKS) { phase = Phase.ITEM; ticks = 0; }
        else if (phase == Phase.ITEM && ticks >= ITEM_TICKS) { phase = Phase.RESULT; ticks = 0; }
    }

    private int color(int rgb, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0xffffff);
    }
    private float phaseProgress(float partialTick) {
        if (phase == Phase.RESULT) return 1.0f;
        int duration = phase == Phase.RARITY ? RARITY_TICKS : ITEM_TICKS;
        return Math.max(0.0f, Math.min(1.0f, (ticks + partialTick) / duration));
    }
    private void drawPanelFrame(GuiGraphics graphics, int left, int top, int width, int height, float partialTick) {
        float pulse = .5f + .5f * (float) Math.sin((ticks + partialTick) * .12f);
        int glow = 115 + (int) (65 * pulse);
        graphics.fill(left, top, left + width, top + height, 0xff0b0910);
        graphics.fill(left + 2, top + 2, left + width - 2, top + height - 2, 0xff211a2b);
        graphics.fill(left + 4, top + 4, left + width - 4, top + 29, 0xff15111d);
        graphics.fill(left, top, left + width, top + 2, color(PANEL_EDGE, glow));
        graphics.fill(left, top + height - 2, left + width, top + height, 0xff70502d);
        graphics.fill(left, top, left + 2, top + height, 0xff70502d);
        graphics.fill(left + width - 2, top, left + width, top + height, 0xff70502d);
    }
    private void drawParticles(GuiGraphics graphics, int index, int centerX, int centerY,
                               int particleRgb, float progress, float partialTick) {
        for (Particle particle : particles.get(index)) {
            float particleProgress = Math.max(0.0f, Math.min(1.0f,
                    (progress - particle.delay) / (1.0f - particle.delay)));
            float radius = particle.radius * (1.0f - particleProgress * .9f);
            float angle = particle.angle + (ticks + partialTick) * .08f * particle.speed;
            int particleX = centerX + (int) (Math.cos(angle) * radius);
            int particleY = centerY + (int) (Math.sin(angle) * radius * .65f);
            graphics.fill(particleX, particleY, particleX + particle.size, particleY + particle.size,
                    color(particleRgb, (int) (225 * particleProgress)));
        }
    }
    private void drawProgressBar(GuiGraphics graphics, int left, int top, int width,
                                 float progress, int progressColor) {
        int barLeft = left + 12, barRight = left + width - 12;
        int fillWidth = Math.round(Math.max(0, barRight - barLeft) * progress);
        graphics.fill(barLeft, top, barRight, top + 4, 0xff42394a);
        if (fillWidth > 0) {
            graphics.fill(barLeft, top, barLeft + fillWidth, top + 4, progressColor);
            if (fillWidth > 2) graphics.fill(barLeft, top, barLeft + fillWidth, top + 1, 0x88ffffff);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int count = rewards.size();
        if (count == 0) { finishReveal(); return; }
        int columns = count == 1 ? 1 : count <= 4 ? 2 : count <= 9 ? 3 : 4;
        int rows = (count + columns - 1) / columns;
        int panelWidth = Math.min(width - 24, Math.max(300, columns * 132 + 16));
        int cellWidth = Math.max(1, (panelWidth - 16) / columns);
        int cellHeight = Math.min(112, Math.max(76, (height - 104) / rows));
        int panelHeight = rows * cellHeight + 76;
        int left = (width - panelWidth) / 2;
        int top = Math.max(8, (height - panelHeight) / 2);
        if (top + panelHeight > height - 8) {
            cellHeight = Math.max(54, (height - 92) / rows);
            panelHeight = rows * cellHeight + 76;
            top = Math.max(4, (height - panelHeight) / 2);
        }
        drawPanelFrame(graphics, left, top, panelWidth, panelHeight, partialTick);
        String title = phase == Phase.RARITY ? "✦  REVELANDO RAREZAS  ✦" :
                phase == Phase.ITEM ? "✦  REVELANDO RECOMPENSAS  ✦" : "✦  ¡RECOMPENSAS OBTENIDAS!  ✦";
        graphics.drawCenteredString(font, title, width / 2, top + 10, 0xffffd45a);
        graphics.drawCenteredString(font, count + (count == 1 ? " recompensa" : " recompensas"),
                width / 2, top + 21, 0xffbdb4c9);
        float progress = phaseProgress(partialTick);
        for (int i = 0; i < count; i++) {
            LootStorage.StoredReward reward = rewards.get(i);
            ItemStack stack = reward.stack();
            int rarityColor = reward.rarity().getColor();
            int column = i % columns, row = i / columns;
            int x = left + 8 + column * cellWidth, y = top + 34 + row * cellHeight;
            int cardRight = x + cellWidth - 5, cardBottom = y + cellHeight - 5;
            int centerX = x + (cellWidth - 5) / 2, centerY = y + cellHeight / 2 - 4;
            boolean rarityVisible = phase != Phase.RARITY;
            graphics.fill(x, y, cardRight, cardBottom, rarityVisible ? color(rarityColor, 190) : 0xff51475a);
            graphics.fill(x + 1, y + 1, cardRight - 1, cardBottom - 1, 0xff15111c);
            graphics.fill(x + 3, y + 3, cardRight - 3, cardBottom - 3,
                    rarityVisible ? color(rarityColor, 34) : 0xff302a37);
            String heading = phase == Phase.RARITY ? "ANALIZANDO..." : reward.rarity().name();
            graphics.drawCenteredString(font, fitText(heading, cellWidth - 16), centerX, y + 7,
                    phase == Phase.RARITY ? 0xffd0c7d8 : rarityColor);
            if (phase == Phase.RARITY) drawParticles(graphics, i, centerX, centerY, 0xffc8d5e8, progress, partialTick);
            else if (phase == Phase.ITEM) drawParticles(graphics, i, centerX, centerY, rarityColor, progress, partialTick);
            else {
                float pop = 1.0f + .08f * (float) Math.sin((ticks + partialTick) * .16f);
                graphics.pose().pushPose();
                graphics.pose().translate(centerX, centerY - 3, 0);
                graphics.pose().scale(pop, pop, 1.0f);
                graphics.renderItem(stack, -8, -8);
                graphics.pose().popPose();
                graphics.drawCenteredString(font, fitText(stack.getHoverName().getString(), cellWidth - 16),
                        centerX, y + cellHeight - 31, 0xffffffff);
                graphics.drawCenteredString(font, "x" + stack.getCount() + " · " + reward.rarity().name(),
                        centerX, y + cellHeight - 19, rarityColor);
            }
        }
        String footer = phase == Phase.RARITY ? "La rareza se revelará en unos instantes" :
                phase == Phase.ITEM ? "La recompensa está a punto de aparecer" :
                        (specialCount > 0 ? "Pulsa ENTER para elegir tu recompensa especial" : "Pulsa ENTER o ESC para continuar");
        int footerColor = phase == Phase.RESULT ? 0xffffdf8a : 0xffc8c0d2;
        int progressColor = phase == Phase.RARITY ? 0xffb9c6d8 :
                phase == Phase.ITEM ? rewards.get(0).rarity().getColor() : 0xffffd45a;
        graphics.drawCenteredString(font, fitText(footer, panelWidth - 28), width / 2,
                top + panelHeight - 27, footerColor);
        drawProgressBar(graphics, left, top + panelHeight - 17, panelWidth, progress, progressColor);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void finishReveal() {
        if (specialCount > 0) Minecraft.getInstance().setScreen(new LootCatalogScreen(null, specialCount));
        else JumpNetworkClient.requestStorage();
    }
    private String fitText(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        String ellipsis = "...";
        return font.plainSubstrByWidth(text, Math.max(0, maxWidth - font.width(ellipsis))) + ellipsis;
    }
    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (phase == Phase.RESULT && (key == 257 || key == 256)) { finishReveal(); return true; }
        return super.keyPressed(key, scanCode, modifiers);
    }
    @Override
    public boolean isPauseScreen() { return true; }
}
