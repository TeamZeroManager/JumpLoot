package com.teamzeromanager.jumploot;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Particle-based two-stage reward reveal.
 * The server selects and stores the exact reward before this screen opens;
 * this class only presents that authoritative result.
 */
public final class LootRouletteScreen extends Screen {
    private enum Phase { RARITY, ITEM, RESULT }

    private static final int RARITY_DURATION = 110;
    private static final int ITEM_DURATION = 145;
    private static final int GOLD = 0xFFFFD45A;
    private static final int MUTED = 0xFFBDB4C9;
    private static final int PARTICLE_COUNT = 76;

    private final LootRarity rarity;
    private final ItemStack reward;
    private final List<Particle> particles = new ArrayList<>();

    private Phase phase = Phase.RARITY;
    private int phaseTick;

    private static final class Particle {
        final float angle;
        final float radius;
        final float speed;
        final int size;
        final float wobble;
        final float delay;

        Particle(float angle, float radius, float speed, int size, float wobble, float delay) {
            this.angle = angle;
            this.radius = radius;
            this.speed = speed;
            this.size = size;
            this.wobble = wobble;
            this.delay = delay;
        }
    }

    public LootRouletteScreen(LootRarity rarity, ItemStack reward) {
        super(Component.literal("Jump Loot"));
        this.rarity = rarity;
        this.reward = reward.copy();
        buildParticles();
    }

    private void buildParticles() {
        RandomSource random = RandomSource.create(0x4A554D504C4F4F54L);
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            particles.add(new Particle(
                    random.nextFloat() * (float) (Math.PI * 2.0),
                    48.0F + random.nextFloat() * 112.0F,
                    0.45F + random.nextFloat() * 1.2F,
                    2 + random.nextInt(3),
                    random.nextFloat() * 6.28F,
                    random.nextFloat() * 0.58F
            ));
        }
    }

    @Override
    public void tick() {
        super.tick();
        phaseTick++;
        if (phase == Phase.RARITY && phaseTick >= RARITY_DURATION) {
            phase = Phase.ITEM;
            phaseTick = 0;
        } else if (phase == Phase.ITEM && phaseTick >= ITEM_DURATION) {
            phase = Phase.RESULT;
            phaseTick = 0;
        }
    }

    private float progress(int duration) {
        return Math.min(1.0F, phaseTick / (float) duration);
    }

    private float easeIn(float value) {
        return value * value * (3.0F - 2.0F * value);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int cx = width / 2;
        int cy = height / 2;

        graphics.fill(cx - 190, cy - 118, cx + 190, cy + 118, 0xFF08060C);
        graphics.fill(cx - 188, cy - 116, cx + 188, cy + 116, GOLD);
        graphics.fill(cx - 185, cy - 113, cx + 185, cy + 113, 0xF0181420);
        graphics.fill(cx - 180, cy - 108, cx + 180, cy + 108, 0xF0252030);

        graphics.drawCenteredString(font, "✦  JUMP LOOT  ✦", cx, cy - 91, GOLD);
        graphics.drawCenteredString(font, "Una recompensa por tus saltos", cx, cy - 75, MUTED);

        if (phase == Phase.RARITY) {
            graphics.drawCenteredString(font, "01  ·  REVELANDO RAREZA", cx, cy - 53, GOLD);
            float colorReveal = easeIn(Math.max(0.0F, Math.min(1.0F, (phaseTick - 42) / 26.0F)));
            int particleColor = blendColor(0xFFB9C6D8, rarity.getColor(), colorReveal);
            drawParticleConvergence(graphics, cx, cy + 3, particleColor, progress(RARITY_DURATION), false, 1.0F);
            float reveal = easeIn(Math.max(0.0F, (progress(RARITY_DURATION) - 0.72F) / 0.28F));
            if (reveal > 0.02F) {
                int alpha = (int) (255 * reveal);
                graphics.drawCenteredString(font, rarity.name(), cx, cy + 54,
                        (alpha << 24) | (rarity.getColor() & 0x00FFFFFF));
            }
        } else if (phase == Phase.ITEM) {
            graphics.drawCenteredString(font, "02  ·  MATERIALIZANDO RECOMPENSA", cx, cy - 53, rarity.getColor());
            drawParticleConvergence(graphics, cx, cy + 2, rarity.getColor(), progress(ITEM_DURATION), true, 1.0F);
            float reveal = easeIn(Math.max(0.0F, (progress(ITEM_DURATION) - 0.68F) / 0.32F));
            if (reveal > 0.01F) {
                int alpha = (int) (255 * reveal);
                graphics.pose().pushPose();
                float scale = 0.65F + 0.35F * reveal;
                graphics.pose().translate(cx, cy + 2, 0);
                graphics.pose().scale(scale, scale, 1.0F);
                graphics.pose().translate(-cx, -(cy + 2), 0);
                graphics.renderItem(reward, cx - 8, cy - 7);
                graphics.pose().popPose();
                graphics.drawCenteredString(font, reward.getHoverName(), cx, cy + 39,
                        (alpha << 24) | 0x00FFFFFF);
            }
        } else {
            float effectFade = 1.0F - easeIn(Math.max(0.0F, Math.min(1.0F, (phaseTick - 20) / 35.0F)));
            if (effectFade > 0.01F) {
                drawParticleConvergence(graphics, cx, cy - 17, rarity.getColor(), 1.0F, true, effectFade);
                drawRevealBurst(graphics, cx, cy - 17, rarity.getColor(), effectFade);
            }
            graphics.drawCenteredString(font, "¡RECOMPENSA OBTENIDA!", cx, cy - 58, rarity.getColor());
            graphics.pose().pushPose();
            graphics.pose().translate(cx, cy - 23, 0);
            graphics.pose().scale(1.5F, 1.5F, 1.0F);
            graphics.renderItem(reward, -8, -8);
            graphics.pose().popPose();
            graphics.drawCenteredString(font, reward.getHoverName(), cx, cy - 5, 0xFFFFFFFF);
            graphics.drawCenteredString(font, "Rareza: " + rarity.name(), cx, cy + 15, rarity.getColor());
            graphics.drawCenteredString(font, "Guardada en tu colección", cx, cy + 39, GOLD);
            graphics.drawCenteredString(font, "Pulsa C para abrir tu colección", cx, cy + 60, 0xFFE6E0EE);
        }

        graphics.drawCenteredString(font, "Pulsa ESC para cerrar", cx, cy + 96, 0xFFB8B0C5);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /**
     * Particles begin on a wide orbit, spiral inward, and converge into a
     * bright core. The second stage resolves that core into the reward icon.
     */
    private void drawParticleConvergence(GuiGraphics graphics, int cx, int cy, int color,
                                         float progress, boolean itemStage, float effectFade) {
        float gather = easeIn(progress);
        float rotation = phaseTick * (itemStage ? 0.095F : 0.075F);
        float pulse = 0.55F + 0.45F * (float) Math.sin((phaseTick + 1) * 0.19F);
        int maxRadius = itemStage ? 145 : 132;

        for (int i = 0; i < particles.size(); i++) {
            Particle particle = particles.get(i);
            float localProgress = Math.max(0.0F, Math.min(1.0F,
                    (progress - particle.delay) / (1.0F - particle.delay)));
            if (localProgress <= 0.0F) continue;
            float particleGather = easeIn(localProgress);
            float angle = particle.angle + rotation * particle.speed;
            float orbit = (particle.radius + 76.0F * (1.0F - particleGather)) * (1.0F - particleGather * 0.91F);
            float drift = (float) Math.sin(phaseTick * 0.11F + particle.wobble) * (1.0F - particleGather) * 9.0F;
            int x = cx + Math.round((float) Math.cos(angle) * orbit + drift);
            int y = cy + Math.round((float) Math.sin(angle) * orbit * 0.58F);
            int size = particle.size + (gather > 0.82F ? 1 : 0);
            int alpha = Math.min(255, (int) ((105 + pulse * 120 + particleGather * 30) * Math.min(1.0F, localProgress * 3.2F) * effectFade));
            graphics.fill(x, y, x + size, y + size, withAlpha(color, alpha));

            // Tiny white glints make the converging energy feel brighter.
            if (i % 7 == 0 && (phaseTick + i) % 3 == 0) {
                graphics.fill(x - 1, y - 1, x + 1, y + 1, withAlpha(0xFFFFFFFF, (int) (alpha * 0.8F * effectFade)));
            }
        }

        if (gather > 0.58F) {
            float core = (gather - 0.58F) / 0.42F;
            int radius = 3 + (int) (core * (itemStage ? 12 : 9) * pulse);
            graphics.fill(cx - radius, cy - radius, cx + radius, cy + radius,
                    withAlpha(color, (int) ((80 + core * 150) * effectFade)));
            graphics.fill(cx - 2, cy - 2, cx + 2, cy + 2, withAlpha(0xFFFFFFFF, (int) ((180 + core * 75) * effectFade)));
        }
    }

    private void drawRevealBurst(GuiGraphics graphics, int cx, int cy, int color, float effectFade) {
        float pulse = 0.5F + 0.5F * (float) Math.sin((phaseTick + 1) * 0.24F);
        int radius = 30 + (int) (pulse * 13);
        graphics.fill(cx - radius, cy - 1, cx + radius, cy + 1, withAlpha(color, (int) (75 * effectFade)));
        graphics.fill(cx - 1, cy - radius, cx + 1, cy + radius, withAlpha(color, 75));
        for (int i = 0; i < 20; i++) {
            double angle = Math.PI * 2.0 * i / 20.0 + phaseTick * 0.025;
            int distance = radius + (i % 3) * 7;
            int x = cx + (int) (Math.cos(angle) * distance);
            int y = cy + (int) (Math.sin(angle) * distance * 0.55);
            graphics.fill(x - 1, y - 1, x + 2, y + 2, withAlpha(color, (int) ((150 + pulse * 105) * effectFade)));
        }
    }

    private int blendColor(int from, int to, float amount) {
        int r = (int) (((from >> 16) & 255) + ((((to >> 16) & 255) - ((from >> 16) & 255)) * amount));
        int g = (int) (((from >> 8) & 255) + ((((to >> 8) & 255) - ((from >> 8) & 255)) * amount));
        int b = (int) ((from & 255) + (((to & 255) - (from & 255)) * amount));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
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
    public boolean isPauseScreen() {
        return true;
    }
}
