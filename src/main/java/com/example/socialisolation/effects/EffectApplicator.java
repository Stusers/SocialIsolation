package com.example.socialisolation.effects;

import com.example.socialisolation.config.SocialConfig;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/**
 * Handles applying and removing potion effects based on the social meter level.
 *
 * MODULAR DESIGN: All effect decisions pass through this single class.
 * To swap in custom MobEffects later, only change the constants at the top
 * of this file — nothing else in the codebase needs to change.
 *
 * Effect tiers:
 *   THRIVING  (meter >= thresholdThriving)  -> Saturation I + Luck I
 *   NEUTRAL   (thresholdLonely–thresholdThriving) -> no effects
 *   LONELY    (thresholdIsolated–thresholdLonely) -> Mining Fatigue I + Hunger I
 *   ISOLATED  (meter < thresholdIsolated)   -> Mining Fatigue II + Hunger II + Weakness I
 */
public class EffectApplicator {

    // ── Effect definitions ────────────────────────────────────────────────────
    // In 1.21.1, MobEffects constants are Holder<MobEffect>, not MobEffect directly.
    // To swap in custom effects: replace these with your registered DeferredHolder references.

    // Benefit effects
    private static final Holder<MobEffect> BENEFIT_SATURATION = MobEffects.SATURATION;
    private static final Holder<MobEffect> BENEFIT_LUCK       = MobEffects.LUCK;

    // Mild penalty effects
    private static final Holder<MobEffect> PENALTY_MILD_FATIGUE = MobEffects.DIG_SLOWDOWN;
    private static final Holder<MobEffect> PENALTY_MILD_HUNGER  = MobEffects.HUNGER;

    // Severe penalty effects (same types, higher amplifier)
    private static final Holder<MobEffect> PENALTY_SEVERE_FATIGUE  = MobEffects.DIG_SLOWDOWN;
    private static final Holder<MobEffect> PENALTY_SEVERE_HUNGER   = MobEffects.HUNGER;
    private static final Holder<MobEffect> PENALTY_SEVERE_WEAKNESS = MobEffects.WEAKNESS;

    // Duration in ticks — effects are re-applied every 10 seconds (200 ticks),
    // so we give them a 15 second (300 tick) duration to avoid flickering.
    private static final int EFFECT_DURATION = 300;

    // ── Public API ────────────────────────────────────────────────────────────

    public enum SocialTier { THRIVING, NEUTRAL, LONELY, ISOLATED }

    /**
     * Derives the tier from the current meter value using configured thresholds.
     */
    public static SocialTier getTier(float meter) {
        if (meter >= SocialConfig.THRESHOLD_THRIVING.get().floatValue()) return SocialTier.THRIVING;
        if (meter >= SocialConfig.THRESHOLD_LONELY.get().floatValue())   return SocialTier.NEUTRAL;
        if (meter >= SocialConfig.THRESHOLD_ISOLATED.get().floatValue()) return SocialTier.LONELY;
        return SocialTier.ISOLATED;
    }

    /**
     * Apply the appropriate effects for the given tier.
     * Removes effects from tiers that no longer apply.
     * Called every 10 seconds per player.
     */
    public static void applyEffects(Player player, SocialTier tier) {
        // Always clear all managed effects first, then re-apply what's needed.
        // This ensures clean transitions between tiers.
        clearManagedEffects(player);

        switch (tier) {
            case THRIVING -> {
                if (SocialConfig.ENABLE_BENEFITS.get()) {
                    apply(player, BENEFIT_SATURATION, 0);
                    apply(player, BENEFIT_LUCK, 0);
                }
            }
            case LONELY -> {
                if (SocialConfig.ENABLE_PENALTIES.get()) {
                    apply(player, PENALTY_MILD_FATIGUE, 0);  // Mining Fatigue I
                    apply(player, PENALTY_MILD_HUNGER, 0);   // Hunger I
                }
            }
            case ISOLATED -> {
                if (SocialConfig.ENABLE_PENALTIES.get()) {
                    apply(player, PENALTY_SEVERE_FATIGUE, 1);  // Mining Fatigue II
                    apply(player, PENALTY_SEVERE_HUNGER, 1);   // Hunger II
                    apply(player, PENALTY_SEVERE_WEAKNESS, 0); // Weakness I
                }
            }
            case NEUTRAL -> {
                // No effects — already cleared above
            }
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static void apply(Player player, Holder<MobEffect> effect, int amplifier) {
        player.addEffect(new MobEffectInstance(
                effect,
                EFFECT_DURATION,
                amplifier,
                /*ambient*/ false,
                /*showParticles*/ true,
                /*showIcon*/ true
        ));
    }

    /**
     * Remove all effects that this mod may have applied,
     * so we never leave stale effects from a previous tier.
     */
    private static void clearManagedEffects(Player player) {
        player.removeEffect(BENEFIT_SATURATION);
        player.removeEffect(BENEFIT_LUCK);
        player.removeEffect(PENALTY_MILD_FATIGUE);   // also covers SEVERE_FATIGUE (same holder)
        player.removeEffect(PENALTY_MILD_HUNGER);    // also covers SEVERE_HUNGER (same holder)
        player.removeEffect(PENALTY_SEVERE_WEAKNESS);
    }
}
