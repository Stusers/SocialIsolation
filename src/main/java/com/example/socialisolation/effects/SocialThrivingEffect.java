package com.example.socialisolation.effects;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import com.example.socialisolation.SocialIsolation;

/**
 * "In Good Company" — applied when the social meter is >= thresholdThriving.
 *
 * Attribute effects (always active while the effect is present):
 *   +0.5 Luck           (better loot table rolls)
 *   +0.2 Block Break Speed (slight mining bonus, ~20%)
 *
 * Amplifier 0 = tier Thriving (standard values above).
 */
public class SocialThrivingEffect extends MobEffect {

    private static final ResourceLocation LUCK_MOD_ID =
            ResourceLocation.fromNamespaceAndPath(SocialIsolation.MODID, "social_thriving_luck");
    private static final ResourceLocation SPEED_MOD_ID =
            ResourceLocation.fromNamespaceAndPath(SocialIsolation.MODID, "social_thriving_break_speed");

    public SocialThrivingEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x55FF55); // green
        this.addAttributeModifier(Attributes.LUCK,            LUCK_MOD_ID,  0.5,  AttributeModifier.Operation.ADD_VALUE);
        this.addAttributeModifier(Attributes.BLOCK_BREAK_SPEED, SPEED_MOD_ID, 0.2, AttributeModifier.Operation.ADD_VALUE);
    }
}

