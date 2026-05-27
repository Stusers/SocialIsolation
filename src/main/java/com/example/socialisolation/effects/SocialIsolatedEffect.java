package com.example.socialisolation.effects;

import com.example.socialisolation.SocialIsolation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * "Isolated" — applied when the social meter is below thresholdIsolated.
 *
 * Attribute effect:
 *   -0.12 Block Break Speed (mild mining slowdown, ~12%)
 *
 * No hunger drain, no movement speed penalty.
 * The slowdown is a gentle nudge toward others without
 * making solo play feel miserable.
 */
public class SocialIsolatedEffect extends MobEffect {

    private static final ResourceLocation BREAK_SPEED_MOD_ID =
            ResourceLocation.fromNamespaceAndPath(SocialIsolation.MODID, "social_isolated_break_speed");

    public SocialIsolatedEffect() {
        super(MobEffectCategory.HARMFUL, 0xF44336); // red
        this.addAttributeModifier(Attributes.BLOCK_BREAK_SPEED, BREAK_SPEED_MOD_ID, -0.12, AttributeModifier.Operation.ADD_VALUE);
    }
}
