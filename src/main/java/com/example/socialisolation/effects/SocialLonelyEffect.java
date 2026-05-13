package com.example.socialisolation.effects;

import com.example.socialisolation.SocialIsolation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * "Restless" - applied when the social meter is between thresholdIsolated and thresholdLonely.
 * Attribute: -0.3 Block Break Speed
 * Per-tick: small food exhaustion
 */
public class SocialLonelyEffect extends MobEffect {

    private static final ResourceLocation BREAK_SPEED_MOD_ID =
            ResourceLocation.fromNamespaceAndPath(SocialIsolation.MODID, "social_lonely_break_speed");

    public SocialLonelyEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF9900);
        this.addAttributeModifier(Attributes.BLOCK_BREAK_SPEED, BREAK_SPEED_MOD_ID, -0.3, AttributeModifier.Operation.ADD_VALUE);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity instanceof Player player) {
            player.getFoodData().addExhaustion(0.005f);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        return tickCount % 20 == 0;
    }
}

