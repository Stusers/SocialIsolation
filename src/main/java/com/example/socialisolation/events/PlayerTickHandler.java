package com.example.socialisolation.events;

import com.example.socialisolation.compat.OpenPACCompat;
import com.example.socialisolation.config.SocialConfig;
import com.example.socialisolation.data.PlayerSocialData;
import com.example.socialisolation.data.SocialSavedData;
import com.example.socialisolation.effects.EffectApplicator;
import com.example.socialisolation.network.SocialMeterPayload;
import com.example.socialisolation.util.ProximityUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.monster.Slime;
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

    /** Hysteresis buffer — a player must drop this far below a threshold before the tier changes down. */
    private static final float TIER_HYSTERESIS = 5.0f;

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) return;

        long tick = server.getTickCount();
        SocialSavedData savedData = SocialSavedData.get(server);
        int opacPointsPerChunk = SocialConfig.OPAC_POINTS_PER_BONUS_CHUNK.get();
        int opacMaxChunks = SocialConfig.OPAC_MAX_BONUS_CHUNKS.get();

        for (int i = 0; i < players.size(); i++) {
            ServerPlayer player = players.get(i);

            // ── Proximity + meter (staggered by player index) ──────────────
            if ((tick + i) % PROXIMITY_INTERVAL == 0) {
                PlayerSocialData data = updateMeter(player, savedData);
                PacketDistributor.sendToPlayer(player, new SocialMeterPayload(data.getSocialMeter(), data.getTotalPointsRegained()));

                // Sync OPAC bonus chunks whenever earned count changes (not just on tier change)
                if (opacPointsPerChunk > 0) {
                    int earned = com.example.socialisolation.util.ChunkRewardMath.chunksEarned(
                            data.getTotalPointsRegained(), opacPointsPerChunk, opacMaxChunks);
                    if (earned != data.getLastSyncedOpacChunks()) {
                        OpenPACCompat.setBonusChunks(server, player.getUUID(), earned);
                        data.setLastSyncedOpacChunks(earned);
                        savedData.setDirty();
                    }
                }
            }

            // ── Effect application ─────────────────────────────────────────
            if ((tick + i) % EFFECT_INTERVAL == 0) {
                PlayerSocialData data = savedData.getOrCreate(player.getUUID());
                EffectApplicator.SocialTier tier = resolveTierWithHysteresis(data);
                int newOrdinal = tier.ordinal();
                int oldOrdinal = data.getLastAppliedTierOrdinal();

                if (newOrdinal != oldOrdinal) {
                    EffectApplicator.applyEffects(player, tier);
                    data.setLastAppliedTierOrdinal(newOrdinal);
                    savedData.setDirty();

                    notifyTierChange(player, tier);
                }

                // Keep TIME_SINCE_REST elevated every effect interval so vanilla phantom
                // spawning stays active for as long as the player remains ISOLATED.
                // Must be continuous — vanilla ticks this stat down and resets it on sleep.
                if (tier == EffectApplicator.SocialTier.ISOLATED
                        && SocialConfig.PHANTOM_SPAWN_WHEN_ISOLATED.get()) {
                    player.getStats().setValue(
                            player,
                            Stats.CUSTOM.get(Stats.TIME_SINCE_REST),
                            120000
                    );
                }
            }
        }

        // ── Familiarity decay + dirty mark (once per minute, not per player) ──
        if (tick % DECAY_INTERVAL == 0) {
            double decayRate = SocialConfig.familiarityDecayPerSecond();
            for (ServerPlayer player : players) {
                PlayerSocialData data = savedData.getOrCreate(player.getUUID());
                data.decayFamiliarity(decayRate);
            }
            savedData.setDirty();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    private PlayerSocialData updateMeter(ServerPlayer player, SocialSavedData savedData) {
        PlayerSocialData data = savedData.getOrCreate(player.getUUID());

        int radius = SocialConfig.PROXIMITY_RADIUS.get();
        List<ServerPlayer> nearbyPlayers = ProximityUtil.getNearbyPlayers(player, radius);
        List<Slime> nearbyWillsons = ProximityUtil.getNearbyWillsonSlimes(player, radius);
        int totalSources = nearbyPlayers.size() + nearbyWillsons.size();

        if (totalSources == 0) {
            float drainPerSecond = SocialConfig.meterDrainPerSecond();
            data.adjustMeterRate(-drainPerSecond);
        } else {
            float baseGainPerSecond = SocialConfig.meterGainPerSecond();

            float weightedSum = 0f;
            for (ServerPlayer other : nearbyPlayers) {
                weightedSum += data.effectiveGainMultiplier(other.getUUID());
                data.addFamiliarity(other.getUUID(), SocialConfig.familiarityGainPerSecond());
                data.markSeenTogether(other.getUUID());
            }

            for (Slime willson : nearbyWillsons) {
                weightedSum += data.effectiveGainMultiplier(willson.getUUID());
                data.addFamiliarity(willson.getUUID(), SocialConfig.familiarityGainPerSecond());
                data.markSeenTogether(willson.getUUID());
            }

            float groupMultiplier = Math.min(1.5f, 1.0f + (float)(Math.log(totalSources) / Math.log(2)) * 0.25f);
            float totalGainPerSecond = baseGainPerSecond * weightedSum * groupMultiplier;
            data.adjustMeterRate(totalGainPerSecond);
        }

        savedData.setDirty();
        return data;
    }

    private static void notifyTierChange(ServerPlayer player, EffectApplicator.SocialTier tier) {
        String message = switch (tier) {
            case THRIVING  -> "§a[Social] You feel great being around others!";
            case NEUTRAL   -> "§7[Social] Your social meter is balanced.";
            case LONELY    -> "§e[Social] You're starting to feel lonely...";
            case ISOLATED  -> "§c[Social] You feel completely isolated.";
        };
        player.sendSystemMessage(Component.literal(message));
    }

    private static EffectApplicator.SocialTier resolveTierWithHysteresis(PlayerSocialData data) {
        float meter = data.getSocialMeter();
        int lastOrdinal = data.getLastAppliedTierOrdinal();

        if (lastOrdinal < 0) {
            return EffectApplicator.getTier(meter);
        }

        EffectApplicator.SocialTier lastTier = EffectApplicator.SocialTier.values()[lastOrdinal];
        EffectApplicator.SocialTier rawTier = EffectApplicator.getTier(meter);

        if (rawTier == lastTier) return lastTier;

        float thriving = SocialConfig.THRESHOLD_THRIVING.get();
        float lonely   = SocialConfig.THRESHOLD_LONELY.get();
        float isolated = SocialConfig.THRESHOLD_ISOLATED.get();

        if (isBetter(rawTier, lastTier)) {
            return rawTier;
        }

        return switch (lastTier) {
            case THRIVING  -> meter < thriving - TIER_HYSTERESIS ? rawTier : lastTier;
            case NEUTRAL   -> meter < lonely   - TIER_HYSTERESIS ? rawTier : lastTier;
            case LONELY    -> meter < isolated - TIER_HYSTERESIS ? rawTier : lastTier;
            case ISOLATED  -> rawTier;
        };
    }

    private static boolean isBetter(EffectApplicator.SocialTier a, EffectApplicator.SocialTier b) {
        return a.ordinal() < b.ordinal();
    }
}
