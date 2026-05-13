package com.example.socialisolation.events;

import com.example.socialisolation.effects.ModEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;

/**
 * Boosts all XP gains by 15% when the player has the SocialThrivingEffect.
 *
 * Covers:
 *   - Mining / smelting / trading / breeding (PlayerXpEvent.XpChange)
 *   - Mob kills (LivingExperienceDropEvent)
 *   - XP orb pickups (PlayerXpEvent.PickupXp)
 *
 * This is more rewarding than Luck because it directly speeds up enchanting,
 * which is a core progression system that benefits from group play.
 */
public class XpBoostHandler {

    private static final float XP_BOOST_MULTIPLIER = 1.15f;

    @SubscribeEvent
    public void onXpChange(PlayerXpEvent.XpChange event) {
        Player player = event.getEntity();
        if (player.hasEffect(ModEffects.SOCIAL_THRIVING)) {
            int boosted = Math.round(event.getAmount() * XP_BOOST_MULTIPLIER);
            if (boosted != event.getAmount()) {
                event.setAmount(boosted);
            }
        }
    }

    @SubscribeEvent
    public void onMobXpDrop(LivingExperienceDropEvent event) {
        Player player = event.getAttackingPlayer();
        if (player == null) return;
        if (player.hasEffect(ModEffects.SOCIAL_THRIVING)) {
            int boosted = Math.round(event.getDroppedExperience() * XP_BOOST_MULTIPLIER);
            event.setDroppedExperience(boosted);
        }
    }

    @SubscribeEvent
    public void onXpPickup(PlayerXpEvent.PickupXp event) {
        Player player = event.getEntity();
        if (player.hasEffect(ModEffects.SOCIAL_THRIVING)) {
            // PickupXp doesn't have a setAmount, but the orb's value is what matters.
            // We modify the orb directly before pickup.
            var orb = event.getOrb();
            int boosted = Math.round(orb.value * XP_BOOST_MULTIPLIER);
            orb.value = boosted;
        }
    }
}

