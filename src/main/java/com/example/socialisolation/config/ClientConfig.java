package com.example.socialisolation.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client-side config for Social Isolation HUD.
 * Stored in socialisolation-client.toml, per-player, not synced to server.
 */
public class ClientConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue HUD_ENABLED = BUILDER
            .comment("Whether the Social Meter HUD bar is visible.")
            .define("hudEnabled", true);

    public static final ModConfigSpec.IntValue HUD_OFFSET_X = BUILDER
            .comment("Horizontal offset in pixels from the default position (right of hunger bar).",
                     "Negative = left, Positive = right.")
            .defineInRange("hudOffsetX", 0, -500, 500);

    public static final ModConfigSpec.IntValue HUD_OFFSET_Y = BUILDER
            .comment("Vertical offset in pixels from the default position (above hunger bar).",
                     "Negative = up, Positive = down.")
            .defineInRange("hudOffsetY", 0, -300, 300);

    public static final ModConfigSpec.DoubleValue HUD_SCALE = BUILDER
            .comment("Scale of the HUD bar. 1.0 = default size, 0.5 = half size, 2.0 = double.",
                     "Useful for smaller screens or UI packs.")
            .defineInRange("hudScale", 1.0, 0.25, 3.0);

    public static final ModConfigSpec SPEC = BUILDER.build();
}

