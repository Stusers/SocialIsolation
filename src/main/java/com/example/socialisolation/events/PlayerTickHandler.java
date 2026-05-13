package com.example.socialisolation.events;

import com.example.socialisolation.config.SocialConfig;
import com.example.socialisolation.data.PlayerSocialData;
import com.example.socialisolation.data.SocialSavedData;
import com.example.socialisolation.effects.EffectApplicator;
import com.example.socialisolation.network.SocialMeterPayload;
import com.example.socialisolation.util.ProximityUtil;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Staggered tick handler — the performance heart of the mod.
 *
 * Schedule (all intervals are in server ticks, 20 ticks = 1 second):
 *   Every 20 ticks  → proximity check + meter update (per-player, staggered)
 *   Every 200 ticks → apply/remove potion effects based on current meter
 *   Every 1200 ticks → decay familiarity + mark SavedData dirty
 *
 * Players are staggered across 20-tick windows using (playerIndex % 20)
 * so not all players are processed on the same tick.
 */
public class PlayerTickHandler {

    private static final int PROXIMITY_INTERVAL  = 20;    // 1 second
    private static final int EFFECT_INTERVAL     = 200;   // 10 seconds
    private static final int DECAY_INTERVAL      = 1200;  // 1 minute

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) return;

        long tick = server.getTickCount();
        SocialSavedData savedData = SocialSavedData.get(server);

        for (int i = 0; i < players.size(); i++) {
            ServerPlayer player = players.get(i);

            // ── Proximity + meter (staggered by player index) ──────────────
            if ((tick + i) % PROXIMITY_INTERVAL == 0) {
                updateMeter(player, savedData);

                // Send updated meter to this player's client for HUD rendering
                PlayerSocialData data = savedData.getOrCreate(player.getUUID());
                PacketDistributor.sendToPlayer(player, new SocialMeterPayload(data.getSocialMeter()));
            }

            // ── Effect application ─────────────────────────────────────────
            if ((tick + i) % EFFECT_INTERVAL == 0) {
                PlayerSocialData data = savedData.getOrCreate(player.getUUID());
                EffectApplicator.SocialTier tier = EffectApplicator.getTier(data.getSocialMeter());
                EffectApplicator.applyEffects(player, tier);
            }
        }

        // ── Familiarity decay + dirty mark (once per minute, not per player) ──
        if (tick % DECAY_INTERVAL == 0) {
            double decayRate = SocialConfig.FAMILIARITY_DECAY_RATE.get();
            java.util.Set<java.util.UUID> onlineUuids = new java.util.HashSet<>();
            for (ServerPlayer p : players) onlineUuids.add(p.getUUID());

            for (ServerPlayer player : players) {
                PlayerSocialData data = savedData.getOrCreate(player.getUUID());
                data.decayFamiliarityToward(onlineUuids, decayRate);
            }
            savedData.setDirty();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void updateMeter(ServerPlayer player, SocialSavedData savedData) {
        PlayerSocialData data = savedData.getOrCreate(player.getUUID());

        int radius = SocialConfig.PROXIMITY_RADIUS.get();
        List<ServerPlayer> nearbyPlayers = ProximityUtil.getNearbyPlayers(player, radius);
        // Total social sources = real players + Willson slimes
        int totalSources = ProximityUtil.countNearbySocialSources(player, radius, savedData);

        if (totalSources == 0) {
            // Alone — drain meter
            float drainPerSecond = SocialConfig.METER_DRAIN_RATE.get().floatValue();
            data.adjustMeter(-drainPerSecond);
        } else {
            float baseGainPerSecond = SocialConfig.METER_GAIN_RATE.get().floatValue();

            // Familiarity only applies to real players (Willson never becomes familiar)
            float weightedSum = 0f;
            for (ServerPlayer other : nearbyPlayers) {
                weightedSum += data.effectiveGainMultiplier(other.getUUID());
                float familiarityGain = SocialConfig.FAMILIARITY_GAIN_RATE.get().floatValue();
                data.addFamiliarity(other.getUUID(), familiarityGain);
                data.markSeenTogether(other.getUUID());
            }

            // Willson slimes contribute a flat 1.0 multiplier each (no familiarity decay)
            int willsonCount = totalSources - nearbyPlayers.size();
            weightedSum += willsonCount;

            float groupMultiplier = Math.min(1.5f, 1.0f + (float)(Math.log(totalSources) / Math.log(2)) * 0.25f);
            float totalGain = baseGainPerSecond * weightedSum * groupMultiplier;
            data.adjustMeter(totalGain);
        }

        savedData.setDirty();
    }
}
