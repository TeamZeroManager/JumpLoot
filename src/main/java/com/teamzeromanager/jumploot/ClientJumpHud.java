package com.teamzeromanager.jumploot;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class ClientJumpHud {
    private static final int BASE_HEIGHT = 43;
    private static final int EDGE_PADDING = 12;
    private static int currentJumps;
    private static int targetJumps = 30;
    private static float animatedProgress;

    private ClientJumpHud() {}

    public static void setProgress(int current, int target) {
        currentJumps = Math.max(0, current);
        targetJumps = Math.max(1, target);
    }

    public static void render(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || !ClientHudConfig.ENABLED) return;
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        double scale = ClientHudConfig.SCALE;
        int logicalWidth = ClientHudConfig.WIDTH;
        int width = (int) Math.round(logicalWidth * scale);
        int height = (int) Math.round(BASE_HEIGHT * scale);
        int x = (int) Math.round(EDGE_PADDING * scale);
        int y = screenHeight - (int) Math.round(EDGE_PADDING * scale) - height;
        float targetProgress = Math.min(1.0F, (float) currentJumps / targetJumps);
        animatedProgress += (targetProgress - animatedProgress) * 0.22F;
        int fillWidth = Math.round((logicalWidth - 16) * animatedProgress);
        int remaining = Math.max(0, targetJumps - currentJumps);
        int textColor = currentJumps >= targetJumps - 5 ? 0xFFFFC34D : 0xFFFFFFFF;
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale((float) scale, (float) scale, 1.0F);
        graphics.fill(0, 0, logicalWidth, BASE_HEIGHT, 0xD9101018);
        graphics.fill(0, 0, logicalWidth, 3, 0xFFE0B84F);
        graphics.drawString(minecraft.font, "JUMP LOOT", 8, 7, 0xFFFFD76A, false);
        String text = currentJumps + " / " + targetJumps + " saltos";
        if (ClientHudConfig.SHOW_PERCENT) text += "  " + Math.round(targetProgress * 100) + "%";
        graphics.drawString(minecraft.font, text, 8, 19, textColor, false);
        graphics.fill(8, 31, logicalWidth - 8, 37, 0xFF39343A);
        if (fillWidth > 0) graphics.fill(8, 31, 8 + fillWidth, 37,
                currentJumps >= targetJumps - 5 ? 0xFFFFC34D : 0xFFB47AE8);
        if (remaining == 1) graphics.drawString(minecraft.font, "¡Un salto más!", 8, 39, 0xFFFFD76A, false);
        graphics.pose().popPose();
    }
}
