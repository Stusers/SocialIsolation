package com.example.socialisolation.events;

import com.example.socialisolation.SocialIsolation;
import com.example.socialisolation.data.PlayerSocialData;
import com.example.socialisolation.data.SocialSavedData;
import com.example.socialisolation.effects.EffectApplicator;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Handles player login and logout events.
 *
 * On LOGIN:
 *   - Update lastOnlineMs to now (so offline time doesn't also drain meter)
 *   - Re-apply effects immediately so the player sees them right away
 *
 * On LOGOUT:
 *   - Mark the saved data dirty to ensure data is flushed before server shutdown
 *   - (Meter drain is frozen while offline — no action needed here)
 */
public class PlayerJoinLeaveHandler {

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        SocialSavedData savedData = SocialSavedData.get(player.server);
        PlayerSocialData data = savedData.getOrCreate(player.getUUID());

        // Record login time so meter drain starts from now (not from last logout)
        data.markOnline();

        // Immediately apply the correct effects so there's no delay on login
        EffectApplicator.SocialTier tier = EffectApplicator.getTier(data.getSocialMeter());
        EffectApplicator.applyEffects(player, tier);

        savedData.setDirty();

        SocialIsolation.LOGGER.debug(
                "Player {} logged in. Social meter: {}, Tier: {}",
                player.getName().getString(),
                String.format("%.1f", data.getSocialMeter()),
                tier
        );
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        SocialSavedData savedData = SocialSavedData.get(player.server);
        // Ensure data is written — NeoForge will flush on save, but explicit dirty is safe
        savedData.setDirty();
    }
}
