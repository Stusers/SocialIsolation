package com.example.socialisolation.events;

import com.example.socialisolation.effects.ModEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;

/**
 * Boosts all XP gains by 8% when the player has the SocialThrivingEffect.
 *
 * Covers ALL sources: mining, smelting, trading, breeding, mob kills, XP orbs, etc.
 * PlayerXpEvent.XpChange fires for every call to giveExperiencePoints(), so no
 * additional handlers are needed (and would cause double-dipping).
 */
public class XpBoostHandler {

    private static final float XP_BOOST_MULTIPLIER = 1.08f;

    @SubscribeEvent
    public void onXpChange(PlayerXpEvent.XpChange event) {
        Player player = event.getEntity();
        int amount = event.getAmount();
        // Only boost gains, not losses (e.g. enchanting)
        if (amount > 0 && player.hasEffect(ModEffects.SOCIAL_THRIVING)) {
            int boosted = Math.round(amount * XP_BOOST_MULTIPLIER);
            if (boosted != amount) {
                event.setAmount(boosted);
            }
        }
    }
}
