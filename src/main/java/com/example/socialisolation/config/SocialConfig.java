package com.example.socialisolation.config;

import com.example.socialisolation.SocialIsolation;
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

    // ── Meter rates (expressed as minutes for human readability) ─────────────
    public static final ModConfigSpec.IntValue MINUTES_TO_FULL_METER = BUILDER
            .comment("Minutes of continuous social contact (with a fresh/unfamiliar player) needed to go from 0 to 100.")
            .defineInRange("minutesToFullMeter", 10, 1, 600);

    public static final ModConfigSpec.IntValue MINUTES_TO_EMPTY_METER = BUILDER
            .comment("Minutes of being completely alone needed to drain the meter from 100 to 0.")
            .defineInRange("minutesToEmptyMeter", 30, 1, 600);

    // ── Familiarity (expressed as hours for human readability) ───────────────
    public static final ModConfigSpec.IntValue HOURS_TO_MAX_FAMILIARITY = BUILDER
            .comment("Hours spent near a specific player before familiarity is maxed out (meter gain from them reaches zero).",
                     "This is the anti-AFK mechanic — being with the same person for too long stops giving social credit.")
            .defineInRange("hoursToMaxFamiliarity", 5, 1, 168);

    public static final ModConfigSpec.IntValue HOURS_TO_LOSE_FAMILIARITY = BUILDER
            .comment("Hours spent apart from a player before familiarity fully decays back to zero.")
            .defineInRange("hoursToLoseFamiliarity", 24, 1, 720);

    // ── Thresholds (0–100) ───────────────────────────────────────────────────
    public static final ModConfigSpec.IntValue THRESHOLD_THRIVING = BUILDER
            .comment("Social meter value above which the player is 'Thriving' (benefits applied).")
            .defineInRange("thresholdThriving", 60, 1, 99);

    public static final ModConfigSpec.IntValue THRESHOLD_LONELY = BUILDER
            .comment("Social meter value below which mild penalties are applied.")
            .defineInRange("thresholdLonely", 40, 1, 99);

    public static final ModConfigSpec.IntValue THRESHOLD_ISOLATED = BUILDER
            .comment("Social meter value below which severe penalties are applied. Must be less than thresholdLonely.")
            .defineInRange("thresholdIsolated", 15, 1, 99);

    // ── Toggles ──────────────────────────────────────────────────────────────
    public static final ModConfigSpec.BooleanValue ENABLE_BENEFITS = BUILDER
            .comment("Whether to apply positive effects when the meter is high.")
            .define("enableBenefits", true);

    public static final ModConfigSpec.BooleanValue ENABLE_PENALTIES = BUILDER
            .comment("Whether to apply negative effects when the meter is low.")
            .define("enablePenalties", true);

    public static final ModConfigSpec.BooleanValue ENABLE_FAMILIARITY = BUILDER
            .comment("Whether to enable the familiarity system (anti-AFK). If false, players always give full meter gain.")
            .define("enableFamiliarity", true);

    public static final ModConfigSpec.BooleanValue PHANTOM_SPAWN_WHEN_ISOLATED = BUILDER
            .comment("Whether phantoms spawn on isolated players as if they haven't slept for 3 days.")
            .define("phantomSpawnWhenIsolated", true);

    // ── Open Parties and Claims integration ──────────────────────────────────
    public static final ModConfigSpec.IntValue OPAC_POINTS_PER_BONUS_CHUNK = BUILDER
            .comment("Open Parties and Claims integration: base social points needed for the first bonus claim chunk.",
                     "Cost scales exponentially — chunk N costs (pointsPerChunk × N) cumulative points.",
                     "Set to 0 to disable the integration even if the mod is present.",
                     "Default: 5000 — first chunk after ~83 min of social play, each subsequent chunk costs more.")
            .defineInRange("opacPointsPerBonusChunk", 5000, 0, 10000000);

    public static final ModConfigSpec.IntValue OPAC_MAX_BONUS_CHUNKS = BUILDER
            .comment("Open Parties and Claims integration: maximum bonus claim chunks a player can ever earn.",
                     "Default: 200 — requires ~278 hours of social play to reach at default point cost.")
            .defineInRange("opacMaxBonusChunks", 200, 0, 10000);

    public static final ModConfigSpec SPEC = BUILDER.build();

    // ── Derived rate helpers (used by game logic instead of raw config values) ─

    /** Social meter gain per second when near at least one fresh player. */
    public static float meterGainPerSecond() {
        return 100.0f / (MINUTES_TO_FULL_METER.get() * 60f);
    }

    /** Social meter drain per second when completely alone. */
    public static float meterDrainPerSecond() {
        return 100.0f / (MINUTES_TO_EMPTY_METER.get() * 60f);
    }

    /** Familiarity gained per second spent near a specific player. */
    public static float familiarityGainPerSecond() {
        return 1.0f / (HOURS_TO_MAX_FAMILIARITY.get() * 3600f);
    }

    /** Familiarity lost per second spent apart from a player. */
    public static float familiarityDecayPerSecond() {
        return 1.0f / (HOURS_TO_LOSE_FAMILIARITY.get() * 3600f);
    }

    /**
     * Validates that thresholds are ordered correctly (isolated < lonely < thriving).
     * Logs a warning if not — the game will still run but tier detection will be wrong.
     */
    public static void validateThresholds() {
        int thriving  = THRESHOLD_THRIVING.get();
        int lonely    = THRESHOLD_LONELY.get();
        int isolated  = THRESHOLD_ISOLATED.get();

        if (!(isolated < lonely && lonely < thriving)) {
            SocialIsolation.LOGGER.warn(
                    "[SocialIsolation] Threshold ordering is invalid! " +
                    "Expected: thresholdIsolated ({}) < thresholdLonely ({}) < thresholdThriving ({}). " +
                    "Tier detection will behave incorrectly until this is fixed.",
                    isolated, lonely, thriving
            );
        }
    }
}
