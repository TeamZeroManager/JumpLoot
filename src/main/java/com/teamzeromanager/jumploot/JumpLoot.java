package com.teamzeromanager.jumploot;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayerEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JumpLoot implements ModInitializer {
    public static final String MOD_ID = "jumploot";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        JumpNetwork.register();
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                JumpCounterHandler.sendCurrentProgress(handler.player));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
                JumpCounterHandler.sendCurrentProgress(newPlayer));
        LOGGER.info("Jump Loot Fabric is loading.");
    }
}
