package com.example.socialisolation.effects;

import com.example.socialisolation.SocialIsolation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * "In Good Company" — applied when the social meter is >= thresholdThriving.
 *
 * Attribute effects:
 *   +0.08 Block Break Speed (slight mining bonus, ~8%)
 *
 * XP boost is handled externally via XpBoostHandler (PlayerXpEvent)
 * so it applies to ALL sources: mining, smelting, mob kills, trading, etc.
 */
public class SocialThrivingEffect extends MobEffect {

    private static final ResourceLocation SPEED_MOD_ID =
            ResourceLocation.fromNamespaceAndPath(SocialIsolation.MODID, "social_thriving_break_speed");

    public SocialThrivingEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x4CAF50); // green
        this.addAttributeModifier(Attributes.BLOCK_BREAK_SPEED, SPEED_MOD_ID, 0.08, AttributeModifier.Operation.ADD_VALUE);
    }
}
