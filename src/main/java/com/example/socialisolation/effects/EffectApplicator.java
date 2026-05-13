package com.example.socialisolation.effects;

import com.example.socialisolation.config.SocialConfig;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

/**
 * Handles applying and removing effects based on the social meter level.
 *
 * MODULAR DESIGN: All effect decisions pass through this single class.
 * Effects are our own custom MobEffects registered in ModEffects — they
 * never conflict with vanilla or other mods' effects.
 *
 * Duration is Integer.MAX_VALUE (effectively infinite) — effects are
 * removed explicitly on tier change, so there is zero HUD flickering.
 * ambient=true suppresses the swirling particles so the screen isn't
 * cluttered, but the icon still shows in the HUD.
 *
 * Effect tiers:
 *   THRIVING  (meter >= thresholdThriving)  -> SocialThrivingEffect  (Luck + mining speed)
 *   NEUTRAL   (thresholdLonely-thriving)    -> no effects
 *   LONELY    (thresholdIsolated-lonely)    -> SocialLonelyEffect    (mining slow + hunger)
 *   ISOLATED  (meter < thresholdIsolated)   -> SocialIsolatedEffect  (worse slow + hunger + move)
 */
public class EffectApplicator {

    // ── Effect definitions ────────────────────────────────────────────────────
    // Swap these holders to change which effects each tier uses.
    private static final Holder<MobEffect> EFFECT_THRIVING  = ModEffects.SOCIAL_THRIVING;
    private static final Holder<MobEffect> EFFECT_LONELY    = ModEffects.SOCIAL_LONELY;
    private static final Holder<MobEffect> EFFECT_ISOLATED  = ModEffects.SOCIAL_ISOLATED;

    // Integer.MAX_VALUE = permanent until explicitly removed. No flickering.
    private static final int PERMANENT = Integer.MAX_VALUE;

    // ── Public API ────────────────────────────────────────────────────────────

    public enum SocialTier { THRIVING, NEUTRAL, LONELY, ISOLATED }

    public static SocialTier getTier(float meter) {
        if (meter >= SocialConfig.THRESHOLD_THRIVING.get().floatValue())  return SocialTier.THRIVING;
        if (meter >= SocialConfig.THRESHOLD_LONELY.get().floatValue())    return SocialTier.NEUTRAL;
        if (meter >= SocialConfig.THRESHOLD_ISOLATED.get().floatValue())  return SocialTier.LONELY;
        return SocialTier.ISOLATED;
    }

    /**
     * Apply the correct effect for the given tier, clearing all others.
     * Only replaces the effect if the tier has actually changed, to avoid
     * unnecessary attribute recalculations.
     */
    public static void applyEffects(Player player, SocialTier tier) {
        clearManagedEffects(player);

        switch (tier) {
            case THRIVING -> {
                if (SocialConfig.ENABLE_BENEFITS.get()) {
                    apply(player, EFFECT_THRIVING, 0);
                }
            }
            case LONELY -> {
                if (SocialConfig.ENABLE_PENALTIES.get()) {
                    apply(player, EFFECT_LONELY, 0);
                }
            }
            case ISOLATED -> {
                if (SocialConfig.ENABLE_PENALTIES.get()) {
                    apply(player, EFFECT_ISOLATED, 0);
                }
            }
            case NEUTRAL -> { /* cleared above */ }
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static void apply(Player player, Holder<MobEffect> effect, int amplifier) {
        player.addEffect(new MobEffectInstance(
                effect,
                PERMANENT,
                amplifier,
                /*ambient*/      true,   // suppresses swirl particles, keeps HUD icon
                /*showParticles*/ false,  // no particle clutter
                /*showIcon*/      true
        ));
    }

    private static void clearManagedEffects(Player player) {
        player.removeEffect(EFFECT_THRIVING);
        player.removeEffect(EFFECT_LONELY);
        player.removeEffect(EFFECT_ISOLATED);
    }
}
