package com.teamzeromanager.jumploot;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
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
            if (!editMode && minecraft.player != null && minecraft.screen == null) {
                editMode = true;
                minecraft.setScreen(new HudEditorScreen());
                minecraft.player.displayClientMessage(Component.literal(
                        "Edición del HUD: arrastra la barra. Pulsa H o ESC para terminar."), true);
            }
        }
    }

    private static void render(net.minecraftforge.client.gui.overlay.ForgeGui gui, GuiGraphics graphics,
                               float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (editMode || minecraft.player == null || minecraft.options.hideGui || !ClientHudConfig.ENABLED.get()) return;
        drawHud(minecraft, graphics, screenWidth, screenHeight, false);
    }

    private static void drawHud(Minecraft minecraft, GuiGraphics graphics, int screenWidth, int screenHeight,
                                boolean editing) {
        double scale = ClientHudConfig.SCALE.get();
        int logicalWidth = ClientHudConfig.WIDTH.get();
        int width = (int) Math.round(logicalWidth * scale);
        int height = (int) Math.round(BASE_HEIGHT * scale);
        int x = screenWidth - (int) Math.round(ClientHudConfig.RIGHT_OFFSET.get() * scale) - width;
        int y = screenHeight - (int) Math.round(ClientHudConfig.BOTTOM_OFFSET.get() * scale) - height;

        float targetProgress = Math.min(1.0F, (float) currentJumps / targetJumps);
        animatedProgress += (targetProgress - animatedProgress) * 0.22F;
        int barWidth = logicalWidth - 16;
        int fillWidth = Math.round(barWidth * animatedProgress);
        int remaining = Math.max(0, targetJumps - currentJumps);
        int textColor = currentJumps >= targetJumps - 5 ? 0xFFFFC34D : 0xFFFFFFFF;

        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale((float) scale, (float) scale, 1.0F);
        graphics.fill(0, 0, logicalWidth, BASE_HEIGHT, 0xD9101018);
        graphics.fill(0, 0, logicalWidth, 3, 0xFFE0B84F);
        graphics.drawString(minecraft.font, editing ? "JUMP LOOT - EDITANDO" : "JUMP LOOT", 8, 7, 0xFFFFD76A, false);
        String progressText = currentJumps + " / " + targetJumps + " saltos";
        if (ClientHudConfig.SHOW_PERCENT.get()) progressText += "  " + Math.round(targetProgress * 100) + "%";
        graphics.drawString(minecraft.font, progressText, 8, 19, textColor, false);
        graphics.fill(8, 31, logicalWidth - 8, 37, 0xFF39343A);
        if (fillWidth > 0) graphics.fill(8, 31, 8 + fillWidth, 37,
                currentJumps >= targetJumps - 5 ? 0xFFFFC34D : 0xFFB47AE8);
        if (remaining == 1) graphics.drawString(minecraft.font, "¡Un salto más!", 8, 39, 0xFFFFD76A, false);
        else if (editing) graphics.drawString(minecraft.font, "Arrastra para mover", 8, 39, 0xFFBEB6C8, false);
        graphics.pose().popPose();
    }

    private static final class HudEditorScreen extends Screen {
        private boolean dragging;
        private double grabX;
        private double grabY;

        private HudEditorScreen() {
            super(Component.literal("Jump Loot HUD editor"));
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            // Intentionally transparent: the world remains visible behind the editor.
            drawHud(Minecraft.getInstance(), graphics, this.width, this.height, true);
            graphics.drawCenteredString(this.font, "Arrastra la barra con clic izquierdo | Clic derecho: restablecer | H / ESC: salir",
                    this.width / 2, this.height - 24, 0xFFFFFFFF);
            super.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            int width = (int) Math.round(ClientHudConfig.WIDTH.get() * ClientHudConfig.SCALE.get());
            int height = (int) Math.round(BASE_HEIGHT * ClientHudConfig.SCALE.get());
            int x = this.width - (int) Math.round(ClientHudConfig.RIGHT_OFFSET.get() * ClientHudConfig.SCALE.get()) - width;
            int y = this.height - (int) Math.round(ClientHudConfig.BOTTOM_OFFSET.get() * ClientHudConfig.SCALE.get()) - height;

            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                ClientHudConfig.RIGHT_OFFSET.set(12);
                ClientHudConfig.BOTTOM_OFFSET.set(12);
                ClientHudConfig.WIDTH.set(142);
                ClientHudConfig.SCALE.set(1.0D);
                return true;
            }
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && mouseX >= x && mouseX <= x + width
                    && mouseY >= y && mouseY <= y + height) {
                dragging = true;
                grabX = mouseX - x;
                grabY = mouseY - y;
                return true;
            }
            // Consume all clicks while editing so they never reach the game.
            return true;
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            if (dragging && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                double scale = ClientHudConfig.SCALE.get();
                int width = (int) Math.round(ClientHudConfig.WIDTH.get() * scale);
                int height = (int) Math.round(BASE_HEIGHT * scale);
                int newX = (int) Math.round(mouseX - grabX);
                int newY = (int) Math.round(mouseY - grabY);
                int right = Math.max(0, this.width - newX - width);
                int bottom = Math.max(0, this.height - newY - height);
                ClientHudConfig.RIGHT_OFFSET.set((int) Math.round(right / scale));
                ClientHudConfig.BOTTOM_OFFSET.set((int) Math.round(bottom / scale));
                return true;
            }
            return true;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) dragging = false;
            return true;
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == GLFW.GLFW_KEY_H || keyCode == GLFW.GLFW_KEY_ESCAPE) {
                Minecraft.getInstance().setScreen(null);
                return true;
            }
            return true;
        }

        @Override
        public void onClose() {
            dragging = false;
            editMode = false;
            super.onClose();
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.literal("Edición del HUD finalizada."), true);
            }
        }
    }
}
