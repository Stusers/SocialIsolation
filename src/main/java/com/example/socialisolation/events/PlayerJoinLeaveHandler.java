package com.example.socialisolation.events;

import com.example.socialisolation.SocialIsolation;
import com.example.socialisolation.compat.OpenPACCompat;
import com.example.socialisolation.data.PlayerSocialData;
import com.example.socialisolation.data.SocialSavedData;
import com.example.socialisolation.effects.EffectApplicator;
import com.example.socialisolation.config.SocialConfig;
import com.example.socialisolation.network.SocialConfigSyncPayload;
import com.example.socialisolation.network.SocialMeterPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

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

        data.markOnline();

        EffectApplicator.SocialTier tier = EffectApplicator.getTier(data.getSocialMeter());
        EffectApplicator.applyEffects(player, tier);

        PacketDistributor.sendToPlayer(player, new SocialMeterPayload(data.getSocialMeter(), data.getTotalPointsRegained()));
        PacketDistributor.sendToPlayer(player, new SocialConfigSyncPayload(
                SocialConfig.OPAC_POINTS_PER_BONUS_CHUNK.get(),
                SocialConfig.OPAC_MAX_BONUS_CHUNKS.get(),
                SocialConfig.THRESHOLD_THRIVING.get(),
                SocialConfig.THRESHOLD_LONELY.get(),
                SocialConfig.THRESHOLD_ISOLATED.get()
        ));
        // Reset so the tick handler does a fresh OPAC sync on the first proximity tick (~1 s later),
        // by which time OPAC has finished loading the player's config. Only needed when OPAC is present.
        if (OpenPACCompat.isLoaded()) {
            data.setLastSyncedOpacChunks(-1);
        }

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
        savedData.setDirty();
    }
}
