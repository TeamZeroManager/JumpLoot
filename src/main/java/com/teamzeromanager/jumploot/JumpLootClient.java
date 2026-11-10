package com.teamzeromanager.jumploot;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;

public final class JumpLootClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        JumpNetworkClient.register();
        HudRenderCallback.EVENT.register((graphics, tickDelta) -> ClientJumpHud.render(graphics));
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) ->
                InventoryButtonHandler.attach(screen));
    }
}
