package com.teamzeromanager.jumploot;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Adds a shortcut to the player's standard inventory without replacing
 * the inventory screen or interfering with its existing controls.
 */
@Mod.EventBusSubscriber(modid = JumpLoot.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class InventoryButtonHandler {
    private InventoryButtonHandler() {
    }

    @SubscribeEvent
    public static void onInventoryScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen)) {
            return;
        }

        int left = event.getScreen().width / 2 - 88;
        int top = event.getScreen().height / 2 - 83;

        Button collectionButton = Button.builder(
                Component.empty(),
                button -> JumpNetwork.requestStorage()
        ).bounds(left + 78, top + 8, 20, 20)
         .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                 Component.literal("Colección de Jump Loot")))
         .build();

        event.addListener(collectionButton);
    }
}
