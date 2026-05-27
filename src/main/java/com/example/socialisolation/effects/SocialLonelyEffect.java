package com.example.socialisolation.effects;

import com.example.socialisolation.SocialIsolation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * "Restless" — applied when the social meter is between thresholdIsolated and thresholdLonely.
 *
 * Attribute effect:
 *   -0.06 Block Break Speed (mild mining slowdown, ~6%)
 *
 * No hunger drain — that would force MORE grinding for food, defeating the mod's purpose.
 */
public class SocialLonelyEffect extends MobEffect {

    private static final ResourceLocation BREAK_SPEED_MOD_ID =
            ResourceLocation.fromNamespaceAndPath(SocialIsolation.MODID, "social_lonely_break_speed");

    public SocialLonelyEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF9800); // orange
        this.addAttributeModifier(Attributes.BLOCK_BREAK_SPEED, BREAK_SPEED_MOD_ID, -0.06, AttributeModifier.Operation.ADD_VALUE);
    }
}
