package com.example.socialisolation.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server-side config for Social Isolation.
 * All values are tunable by server admins via socialisolation-server.toml.
 */
public class SocialConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ── Proximity ────────────────────────────────────────────────────────────
    public static final ModConfigSpec.IntValue PROXIMITY_RADIUS = BUILDER
            .comment("Block radius within which another player counts as 'nearby'.")
            .defineInRange("proximityRadius", 24, 4, 128);

    // ── Meter rates ──────────────────────────────────────────────────────────
    public static final ModConfigSpec.DoubleValue METER_GAIN_RATE = BUILDER
            .comment("Social meter gain per second when near at least one other player (before familiarity scaling).",
                     "Default: 0.1667 — reaches 100 from 0 in ~10 minutes with a fresh (familiarity=0) player.")
            .defineInRange("meterGainRate", 0.1667, 0.0, 100.0);

    public static final ModConfigSpec.DoubleValue METER_DRAIN_RATE = BUILDER
            .comment("Social meter drain per second when completely alone.",
                     "Default: 0.02778 — drains from 100 to 0 in ~60 minutes alone.")
            .defineInRange("meterDrainRate", 0.02778, 0.0, 100.0);

    // ── Familiarity ──────────────────────────────────────────────────────────
    public static final ModConfigSpec.DoubleValue FAMILIARITY_GAIN_RATE = BUILDER
            .comment("Familiarity gained per second spent near a specific player.",
                     "Default: 0.00006 — reaches max familiarity (1.0) after ~5 hours together.",
                     "At max familiarity, that player contributes zero meter gain (anti-AFK mechanic).")
            .defineInRange("familiarityGainRate", 0.00006, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue FAMILIARITY_DECAY_RATE = BUILDER
            .comment("Familiarity lost per real-world second spent apart (applied on login/periodically).",
                     "Default: 0.00001157 — fully decays from 1.0 to 0 after ~24 hours apart.")
            .defineInRange("familiarityDecayRate", 0.00001157, 0.0, 1.0);

    // ── Thresholds ───────────────────────────────────────────────────────────
    public static final ModConfigSpec.DoubleValue THRESHOLD_THRIVING = BUILDER
            .comment("Social meter value above which the player is considered 'Thriving' (benefits applied).")
            .defineInRange("thresholdThriving", 70.0, 0.0, 100.0);

    public static final ModConfigSpec.DoubleValue THRESHOLD_LONELY = BUILDER
            .comment("Social meter value below which mild penalties are applied.")
            .defineInRange("thresholdLonely", 40.0, 0.0, 100.0);

    public static final ModConfigSpec.DoubleValue THRESHOLD_ISOLATED = BUILDER
            .comment("Social meter value below which severe penalties are applied.")
            .defineInRange("thresholdIsolated", 15.0, 0.0, 100.0);

    // ── Toggle individual effects ────────────────────────────────────────────
    public static final ModConfigSpec.BooleanValue ENABLE_BENEFITS = BUILDER
            .comment("Whether to apply positive effects when the meter is high.")
            .define("enableBenefits", true);

    public static final ModConfigSpec.BooleanValue ENABLE_PENALTIES = BUILDER
            .comment("Whether to apply negative effects when the meter is low.")
            .define("enablePenalties", true);

    public static final ModConfigSpec SPEC = BUILDER.build();
}
