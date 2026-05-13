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

    /** Hysteresis buffer — a player must drop this far below a threshold before the tier changes down.
     * At drain rate of 0.0556/s, 5.0 points ≈ 90 seconds of solo time before losing a tier.
     * Enough to open chests, craft, or take a short break without flickering. */
    private static final float TIER_HYSTERESIS = 5.0f;

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
                EffectApplicator.SocialTier tier = resolveTierWithHysteresis(data);
                if (tier.ordinal() != data.getLastAppliedTierOrdinal()) {
                    EffectApplicator.applyEffects(player, tier);
                    data.setLastAppliedTierOrdinal(tier.ordinal());
                    savedData.setDirty();
                }
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
        int totalSources = ProximityUtil.countNearbySocialSources(player, radius, savedData);

        if (totalSources == 0) {
            // Alone — drain meter (time-accurate)
            float drainPerSecond = SocialConfig.METER_DRAIN_RATE.get().floatValue();
            data.adjustMeterRate(-drainPerSecond);
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
            float totalGainPerSecond = baseGainPerSecond * weightedSum * groupMultiplier;
            data.adjustMeterRate(totalGainPerSecond);
        }

        savedData.setDirty();
    }

    /**
     * Returns the tier with hysteresis applied.
     *
     * Rule: once you're in a tier, you must move at least {@link #TIER_HYSTERESIS}
     * points PAST the threshold in the direction of the new tier before switching.
     *
     * Going down (to a worse tier) is gated by hysteresis.
     * Going up (to a better tier) is immediate — feels more rewarding.
     */
    private static EffectApplicator.SocialTier resolveTierWithHysteresis(PlayerSocialData data) {
        float meter = data.getSocialMeter();
        int lastOrdinal = data.getLastAppliedTierOrdinal();

        if (lastOrdinal < 0) {
            return EffectApplicator.getTier(meter);
        }

        EffectApplicator.SocialTier lastTier = EffectApplicator.SocialTier.values()[lastOrdinal];
        EffectApplicator.SocialTier rawTier = EffectApplicator.getTier(meter);

        if (rawTier == lastTier) return lastTier;

        float thriving = SocialConfig.THRESHOLD_THRIVING.get().floatValue();
        float lonely   = SocialConfig.THRESHOLD_LONELY.get().floatValue();
        float isolated = SocialConfig.THRESHOLD_ISOLATED.get().floatValue();

        // Going UP to a better tier — immediate (rewarding feel)
        if (isBetter(rawTier, lastTier)) {
            return rawTier;
        }

        // Going DOWN to a worse tier — require hysteresis buffer
        return switch (lastTier) {
            case THRIVING  -> meter < thriving - TIER_HYSTERESIS ? rawTier : lastTier;
            case NEUTRAL   -> meter < lonely   - TIER_HYSTERESIS ? rawTier : lastTier;
            case LONELY    -> meter < isolated - TIER_HYSTERESIS ? rawTier : lastTier;
            case ISOLATED  -> rawTier; // can't go lower
        };
    }

    private static boolean isBetter(EffectApplicator.SocialTier a, EffectApplicator.SocialTier b) {
        return a.ordinal() < b.ordinal(); // THRIVING(0) < NEUTRAL(1) < LONELY(2) < ISOLATED(3)
    }

}
