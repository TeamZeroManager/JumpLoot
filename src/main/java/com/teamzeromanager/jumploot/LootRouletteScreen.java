package com.teamzeromanager.jumploot;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Simultaneous multi-reward gacha reveal. */
public final class LootRouletteScreen extends Screen {
    private enum Phase { WAITING, RARITY, ITEM, RESULT }

    // The first seconds keep both the rarity color and item hidden.
    private static final int WAITING_TICKS = 50;
    private static final int RARITY_TICKS = 110;
    private static final int ITEM_TICKS = 145;

    private final List<LootStorage.StoredReward> rewards = new ArrayList<>();
    private final List<List<Particle>> particles = new ArrayList<>();
    private Phase phase = Phase.WAITING;
    private int ticks;

    private static final class Particle {
        final float angle;
        final float radius;
        final float speed;
        final float delay;
        final int size;

        Particle(RandomSource random) {
            angle = random.nextFloat() * (float) (Math.PI * 2);
            radius = 18 + random.nextFloat() * 34;
            speed = .5f + random.nextFloat();
            delay = random.nextFloat() * .45f;
            size = 1 + random.nextInt(2);
        }
    }

    public LootRouletteScreen(List<LootStorage.StoredReward> list) {
        super(Component.literal("Jump Loot"));
        for (LootStorage.StoredReward reward : list) {
            rewards.add(new LootStorage.StoredReward(reward.stack(), reward.rarity()));
            RandomSource random = RandomSource.create();
            List<Particle> rewardParticles = new ArrayList<>();
            for (int i = 0; i < 24; i++) rewardParticles.add(new Particle(random));
            particles.add(rewardParticles);
        }
    }

    @Override
    public void tick() {
        super.tick();
        ticks++;

        if (phase == Phase.WAITING && ticks >= WAITING_TICKS) {
            phase = Phase.RARITY;
            ticks = 0;
        } else if (phase == Phase.RARITY && ticks >= RARITY_TICKS) {
            phase = Phase.ITEM;
            ticks = 0;
        } else if (phase == Phase.ITEM && ticks >= ITEM_TICKS) {
            phase = Phase.RESULT;
            ticks = 0;
        }
    }

    private int color(int rgb, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0xffffff);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        int count = rewards.size();
        if (count == 0) {
            onClose();
            return;
        }

        int columns = count == 1 ? 1 : count <= 4 ? 2 : count <= 9 ? 3 : 4;
        int rows = (count + columns - 1) / columns;
        int cellWidth = Math.min(116, (width - 24) / columns);
        int cellHeight = Math.min(112, (height - 62) / rows);
        int panelWidth = columns * cellWidth + 16;
        int panelHeight = rows * cellHeight + 48;
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;

        graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xf0100d16);
        graphics.fill(left + 2, top + 2, left + panelWidth - 2, top + panelHeight - 2, 0xf0272231);
        graphics.drawCenteredString(font,
                "✦ JUMP LOOT · " + count + " RECOMPENSA" + (count == 1 ? "" : "S"),
                width / 2, top + 10, 0xffffd45a);

        for (int i = 0; i < count; i++) {
            LootStorage.StoredReward reward = rewards.get(i);
            ItemStack stack = reward.stack();
            int rarityColor = reward.rarity().getColor();
            int column = i % columns;
            int row = i / columns;
            int x = left + 8 + column * cellWidth;
            int y = top + 30 + row * cellHeight;
            int centerX = x + (cellWidth - 5) / 2;
            int centerY = y + cellHeight / 2 - 4;

            // Keep the card neutral until the rarity reveal phase actually begins.
            boolean rarityVisible = phase == Phase.RARITY || phase == Phase.ITEM || phase == Phase.RESULT;
            int cardColor = rarityVisible ? color(rarityColor, 55) : 0xff302a37;
            graphics.fill(x, y, x + cellWidth - 5, y + cellHeight - 5, 0xff15111c);
            graphics.fill(x + 1, y + 1, x + cellWidth - 6, y + cellHeight - 6, cardColor);

            String heading;
            int headingColor;
            if (phase == Phase.WAITING) {
                heading = "PREPARANDO...";
                headingColor = 0xffbdb4c9;
            } else if (phase == Phase.RARITY) {
                heading = "REVELANDO RAREZA";
                headingColor = 0xffffd45a;
            } else {
                heading = reward.rarity().name();
                headingColor = rarityColor;
            }
            graphics.drawCenteredString(font, heading, centerX, y + 6, headingColor);

            if (phase != Phase.RESULT) {
                float progress;
                if (phase == Phase.WAITING) {
                    progress = Math.min(1f, ticks / (float) WAITING_TICKS);
                } else if (phase == Phase.RARITY) {
                    progress = Math.min(1f, ticks / (float) RARITY_TICKS);
                } else {
                    progress = Math.min(1f, ticks / (float) ITEM_TICKS);
                }

                for (Particle particle : particles.get(i)) {
                    float particleProgress = Math.max(0,
                            Math.min(1, (progress - particle.delay) / (1 - particle.delay)));
                    float radius = particle.radius * (1 - particleProgress * .88f);
                    float angle = particle.angle + ticks * .08f * particle.speed;
                    int particleX = centerX + (int) (Math.cos(angle) * radius);
                    int particleY = centerY + (int) (Math.sin(angle) * radius * .65f);
                    int particleColor = phase == Phase.WAITING
                            ? 0xffb9c6d8
                            : (phase == Phase.RARITY ? 0xffb9c6d8 : rarityColor);
                    graphics.fill(particleX, particleY,
                            particleX + particle.size, particleY + particle.size,
                            color(particleColor, (int) (230 * particleProgress)));
                }

                if (phase == Phase.ITEM && progress > .55f) {
                    graphics.renderItem(stack, centerX - 8, centerY - 8);
                    graphics.drawCenteredString(font, stack.getHoverName(),
                            centerX, y + cellHeight - 21, 0xffffffff);
                }
            } else {
                graphics.renderItem(stack, centerX - 8, centerY - 12);
                graphics.drawCenteredString(font, stack.getHoverName(),
                        centerX, y + cellHeight - 31, 0xffffffff);
                graphics.drawCenteredString(font,
                        "x" + stack.getCount() + " · " + reward.rarity().name(),
                        centerX, y + cellHeight - 19, rarityColor);
            }
        }

        String footer;
        int footerColor;
        if (phase == Phase.WAITING) {
            footer = "Preparando la revelación...";
            footerColor = 0xffbdb4c9;
        } else if (phase == Phase.RESULT) {
            footer = "¡RECOMPENSAS OBTENIDAS!";
            footerColor = 0xffffd45a;
        } else {
            footer = "Todas las recompensas se revelan simultáneamente";
            footerColor = 0xffbdb4c9;
        }
        graphics.drawCenteredString(font, footer, width / 2, top + panelHeight - 12, footerColor);

        if (phase == Phase.RESULT) {
            graphics.drawCenteredString(font, "ENTER o ESC para continuar",
                    width / 2, Math.min(height - 12, top + panelHeight + 5), 0xffe6e0ee);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (phase == Phase.RESULT && (key == 257 || key == 256)) {
            JumpNetwork.requestStorage();
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
