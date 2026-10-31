package com.teamzeromanager.jumploot;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ClientHudConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.BooleanValue SHOW_PERCENT;
    public static final ForgeConfigSpec.IntValue RIGHT_OFFSET;
    public static final ForgeConfigSpec.IntValue BOTTOM_OFFSET;
    public static final ForgeConfigSpec.IntValue WIDTH;
    public static final ForgeConfigSpec.DoubleValue SCALE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("jump_hud");
        ENABLED = builder.comment("Show the Jump Loot progress HUD").define("enabled", true);
        SHOW_PERCENT = builder.comment("Show progress percentage").define("show_percent", true);
        RIGHT_OFFSET = builder.comment("Distance from the right edge in pixels").defineInRange("right_offset", 12, 0, 2000);
        BOTTOM_OFFSET = builder.comment("Distance from the bottom edge in pixels").defineInRange("bottom_offset", 12, 0, 2000);
        WIDTH = builder.comment("HUD width in pixels").defineInRange("width", 142, 110, 400);
        SCALE = builder.comment("HUD scale").defineInRange("scale", 1.0D, 0.6D, 2.0D);
        builder.pop();
        SPEC = builder.build();
    }

    private ClientHudConfig() {
    }
}
