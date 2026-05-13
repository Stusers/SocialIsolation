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
 * "Isolated" — applied when the social meter is below thresholdIsolated.
 *
 * Attribute effects:
 *   -0.6 Block Break Speed (severe mining penalty, ~60% slower)
 *   -0.05 Movement Speed   (slight sluggishness)
 *
 * Per-tick effect (every 20 ticks = 1 second):
 *   stronger food exhaustion tick (hunger drains noticeably faster)
 */
public class SocialIsolatedEffect extends MobEffect {

    private static final ResourceLocation BREAK_SPEED_MOD_ID =
            ResourceLocation.fromNamespaceAndPath(SocialIsolation.MODID, "social_isolated_break_speed");
    private static final ResourceLocation MOVE_SPEED_MOD_ID =
            ResourceLocation.fromNamespaceAndPath(SocialIsolation.MODID, "social_isolated_move_speed");

    public SocialIsolatedEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF3333); // dark red
        this.addAttributeModifier(Attributes.BLOCK_BREAK_SPEED, BREAK_SPEED_MOD_ID, -0.6, AttributeModifier.Operation.ADD_VALUE);
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED,    MOVE_SPEED_MOD_ID,  -0.05, AttributeModifier.Operation.ADD_VALUE);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity instanceof Player player) {
            player.getFoodData().addExhaustion(0.015f);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        return tickCount % 20 == 0;
    }
}

