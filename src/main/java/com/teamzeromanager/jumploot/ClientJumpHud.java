package com.teamzeromanager.jumploot;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = JumpLoot.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientJumpHud {
    private static final int BASE_HEIGHT = 43;
    private static int currentJumps;
    private static int targetJumps = 30;
    private static float animatedProgress;
    private static boolean editMode;
    private static boolean dragging;
    private static KeyMapping editKey;

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

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        editKey = new KeyMapping("key.jumploot.edit_hud", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H,
                "key.categories.jumploot");
        event.register(editKey);
        MinecraftForge.EVENT_BUS.addListener(ClientJumpHud::clientTick);
    }

    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || editKey == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        while (editKey.consumeClick()) {
            editMode = !editMode;
            dragging = false;
            if (editMode) {
                // Free the cursor so the player can move and drag the HUD.
                minecraft.mouseHandler.releaseMouse();
            } else {
                // Restore normal first-person mouse capture when editing ends.
                minecraft.mouseHandler.grabMouse();
            }
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        editMode ? "Edición del HUD: arrastra la barra. Pulsa H para terminar." : "Edición del HUD finalizada."
                ), true);
            }
        }
    }

    private static void render(net.minecraftforge.client.gui.overlay.ForgeGui gui, GuiGraphics graphics,
                               float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || !ClientHudConfig.ENABLED.get()) return;

        double scale = ClientHudConfig.SCALE.get();
        int width = (int) Math.round(ClientHudConfig.WIDTH.get() * scale);
        int height = (int) Math.round(BASE_HEIGHT * scale);
        int x = screenWidth - (int) Math.round(ClientHudConfig.RIGHT_OFFSET.get() * scale) - width;
        int y = screenHeight - (int) Math.round(ClientHudConfig.BOTTOM_OFFSET.get() * scale) - height;

        if (editMode) {
            double mouseX = minecraft.mouseHandler.xpos() * screenWidth / minecraft.getWindow().getScreenWidth();
            double mouseY = minecraft.mouseHandler.ypos() * screenHeight / minecraft.getWindow().getScreenHeight();
            boolean leftDown = GLFW.glfwGetMouseButton(minecraft.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            boolean rightDown = GLFW.glfwGetMouseButton(minecraft.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
            if (rightDown) {
                ClientHudConfig.RIGHT_OFFSET.set(12);
                ClientHudConfig.BOTTOM_OFFSET.set(12);
                ClientHudConfig.WIDTH.set(142);
                ClientHudConfig.SCALE.set(1.0D);
                width = 142;
                height = BASE_HEIGHT;
                x = screenWidth - width - 12;
                y = screenHeight - height - 12;
                dragging = false;
            }
            if (leftDown && !dragging && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
                dragging = true;
            }
            if (!leftDown) dragging = false;
            if (dragging) {
                int newRight = Math.max(0, screenWidth - (int) mouseX - width);
                int newBottom = Math.max(0, screenHeight - (int) mouseY - height);
                ClientHudConfig.RIGHT_OFFSET.set((int) Math.round(newRight / scale));
                ClientHudConfig.BOTTOM_OFFSET.set((int) Math.round(newBottom / scale));
                x = screenWidth - (int) Math.round(ClientHudConfig.RIGHT_OFFSET.get() * scale) - width;
                y = screenHeight - (int) Math.round(ClientHudConfig.BOTTOM_OFFSET.get() * scale) - height;
            }
        }

        float targetProgress = Math.min(1.0F, (float) currentJumps / targetJumps);
        animatedProgress += (targetProgress - animatedProgress) * 0.22F;
        int barWidth = ClientHudConfig.WIDTH.get() - 16;
        int fillWidth = Math.round(barWidth * animatedProgress);
        int remaining = Math.max(0, targetJumps - currentJumps);
        int textColor = currentJumps >= targetJumps - 5 ? 0xFFFFC34D : 0xFFFFFFFF;

        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale((float) scale, (float) scale, 1.0F);
        int logicalWidth = ClientHudConfig.WIDTH.get();
        graphics.fill(0, 0, logicalWidth, BASE_HEIGHT, 0xD9101018);
        graphics.fill(0, 0, logicalWidth, 3, 0xFFE0B84F);
        graphics.drawString(minecraft.font, editMode ? "JUMP LOOT - EDITANDO" : "JUMP LOOT", 8, 7, 0xFFFFD76A, false);
        String progressText = currentJumps + " / " + targetJumps + " saltos";
        if (ClientHudConfig.SHOW_PERCENT.get()) progressText += "  " + Math.round(targetProgress * 100) + "%";
        graphics.drawString(minecraft.font, progressText, 8, 19, textColor, false);
        graphics.fill(8, 31, logicalWidth - 8, 37, 0xFF39343A);
        if (fillWidth > 0) graphics.fill(8, 31, 8 + fillWidth, 37,
                currentJumps >= targetJumps - 5 ? 0xFFFFC34D : 0xFFB47AE8);
        if (remaining == 1) graphics.drawString(minecraft.font, "¡Un salto más!", 8, 39, 0xFFFFD76A, false);
        else if (editMode) graphics.drawString(minecraft.font, "Arrastra para mover", 8, 39, 0xFFBEB6C8, false);
        graphics.pose().popPose();
    }
}
