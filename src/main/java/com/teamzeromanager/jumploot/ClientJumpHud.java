package com.teamzeromanager.jumploot;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;

@Mod.EventBusSubscriber(modid = JumpLoot.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientJumpHud {
    private static int currentJumps;
    private static int targetJumps = 30;

    private ClientJumpHud() {
    }

    public static void setProgress(int current, int target) {
        currentJumps = Math.max(0, current);
        targetJumps = Math.max(1, target);
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "jump_progress", ClientJumpHud::render);
    }

    private static void render(net.minecraftforge.client.gui.overlay.ForgeGui gui, GuiGraphics graphics,
                               float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }

        int width = 142;
        int height = 34;
        int x = screenWidth - width - 12;
        int y = screenHeight - height - 12;

        float progress = Math.min(1.0F, (float) currentJumps / (float) targetJumps);
        int barWidth = width - 12;
        int filledWidth = Math.round(barWidth * progress);

        graphics.fill(x, y, x + width, y + height, 0xB9101018);
        graphics.fill(x, y, x + width, y + 2, 0xFFE0B84F);
        graphics.drawString(minecraft.font, "JUMP LOOT", x + 6, y + 5, 0xFFFFD76A, false);
        graphics.drawString(minecraft.font, currentJumps + " / " + targetJumps + " saltos",
                x + 6, y + 16, 0xFFFFFFFF, false);
        graphics.fill(x + 6, y + 27, x + 6 + barWidth, y + 31, 0xFF39343A);
        if (filledWidth > 0) {
            int fillColor = currentJumps >= targetJumps - 5 ? 0xFFFFC34D : 0xFFB47AE8;
            graphics.fill(x + 6, y + 27, x + 6 + filledWidth, y + 31, fillColor);
        }
    }
}
