package com.teamzeromanager.jumploot;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

public final class InventoryButtonHandler {
    private InventoryButtonHandler() {}

    public static void attach(net.minecraft.client.gui.screens.Screen screen) {
        if (!(screen instanceof InventoryScreen)) return;
        ScreenEvents.afterInit(screen, (current, client, helper) -> {
            int left = current.width / 2 - 88;
            int top = current.height / 2 - 83;
            Button button = Button.builder(Component.literal("✦"),
                    pressed -> JumpNetwork.requestStorage())
                    .bounds(left + 78, top + 8, 16, 16)
                    .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                            Component.literal("Colección de Jump Loot")))
                    .build();
            helper.addDrawableChild(button);
        });
    }
}
