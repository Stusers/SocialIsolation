package com.example.socialisolation.effects;

import com.example.socialisolation.SocialIsolation;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers all custom MobEffects for Social Isolation.
 *
 * Five distinct effects — each is its own type so they never clash with
 * vanilla effects or other mods. Permanent duration (Integer.MAX_VALUE)
 * is set in EffectApplicator so there is zero flickering in the HUD.
 *
 *   SOCIAL_THRIVING      — Tier: Thriving  — benefit  (Saturation + Luck attributes)
 *   SOCIAL_IN_GOOD_COMPANY — Tier: Thriving — cosmetic companion effect (visible icon)
 *   SOCIAL_RESTLESS      — Tier: Lonely    — mild penalty (Mining Fatigue + Hunger)
 *   SOCIAL_LONELY        — Tier: Lonely    — stronger tag (same tier, distinct icon)
 *   SOCIAL_ISOLATED      — Tier: Isolated  — severe penalty (Fatigue + Hunger + Weakness)
 *
 * In practice EffectApplicator only applies one effect per tier — keeping it
 * simple with one effect per tier is cleaner and maps 1:1 with the HUD bar.
 */
public class ModEffects {

    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, SocialIsolation.MODID);

    /** Thriving — green — mild benefits */
    public static final Holder<MobEffect> SOCIAL_THRIVING =
            MOB_EFFECTS.register("social_thriving", SocialThrivingEffect::new);

    /** Lonely — orange — mild penalties */
    public static final Holder<MobEffect> SOCIAL_LONELY =
            MOB_EFFECTS.register("social_lonely", SocialLonelyEffect::new);

    /** Isolated — dark red — severe penalties */
    public static final Holder<MobEffect> SOCIAL_ISOLATED =
            MOB_EFFECTS.register("social_isolated", SocialIsolatedEffect::new);

    public static void register(IEventBus modEventBus) {
        MOB_EFFECTS.register(modEventBus);
    }
}

