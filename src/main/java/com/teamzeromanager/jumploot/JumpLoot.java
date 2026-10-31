package com.teamzeromanager.jumploot;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(JumpLoot.MOD_ID)
public final class JumpLoot {
    public static final String MOD_ID = "jumploot";
    public static final Logger LOGGER = LogUtils.getLogger();

    public JumpLoot() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientHudConfig.SPEC);
        JumpNetwork.register();
        LOGGER.info("Jump Loot is loading.");
    }
}
