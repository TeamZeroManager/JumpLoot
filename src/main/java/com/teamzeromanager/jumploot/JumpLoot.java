package com.teamzeromanager.jumploot;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(JumpLoot.MOD_ID)
public final class JumpLoot {
    public static final String MOD_ID = "jumploot";
    public static final Logger LOGGER = LogUtils.getLogger();

    public JumpLoot() {
        LOGGER.info("Jump Loot is loading.");
    }
}
