package com.teamzeromanager.jumploot;

import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

public final class InventoryButtonHandler {
    private InventoryButtonHandler() {}

    public static void attach(Screen screen) {
        if (!(screen instanceof InventoryScreen)) return;
        int left = screen.width / 2 - 88;
        int top = screen.height / 2 - 83;
        Button button = Button.builder(Component.literal("✦"),
                pressed -> JumpNetworkClient.requestStorage())
                .bounds(left + 78, top + 8, 16, 16)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.literal("Colección de Jump Loot")))
                .build();
        Screens.getButtons(screen).add(button);
    }
}
