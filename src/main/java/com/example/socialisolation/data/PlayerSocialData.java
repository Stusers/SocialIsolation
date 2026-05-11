package com.example.socialisolation.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player data model stored inside SocialSavedData.
 *
 * socialMeter          – 0-100 float; source of truth for all effect decisions.
 * familiarityMap       – UUID → 0.0-1.0 float; how "used to" this player is to each neighbour.
 * lastSeenTogetherMs   – UUID → wall-clock ms; timestamp of last proximity tick with that player.
 *                        Familiarity only decays toward players who are currently online and
 *                        not nearby — offline time is never counted against familiarity.
 * lastOnlineMs         – wall-clock ms when the player last logged in (used to freeze meter
 *                        drain while offline).
 */
public class PlayerSocialData {

    private static final float SOCIAL_METER_DEFAULT = 50.0f;

    private float socialMeter;
    private final Map<UUID, Float> familiarityMap = new HashMap<>();
    /** Wall-clock System.currentTimeMillis() of last proximity tick with each UUID. */
    private final Map<UUID, Long> lastSeenTogetherMs = new HashMap<>();
    /** Wall-clock time of the player's last login — used to compute offline decay. */
    private long lastOnlineMs;

    public PlayerSocialData() {
        this.socialMeter = SOCIAL_METER_DEFAULT;
        this.lastOnlineMs = System.currentTimeMillis();
    }

    // ── Meter ────────────────────────────────────────────────────────────────

    public float getSocialMeter() { return socialMeter; }

    public void setSocialMeter(float value) {
        this.socialMeter = Math.max(0f, Math.min(100f, value));
    }

    public void adjustMeter(float delta) {
        setSocialMeter(socialMeter + delta);
    }

    // ── Familiarity ──────────────────────────────────────────────────────────

    public float getFamiliarity(UUID other) {
        return familiarityMap.getOrDefault(other, 0.0f);
    }

    public void setFamiliarity(UUID other, float value) {
        familiarityMap.put(other, Math.max(0f, Math.min(1f, value)));
    }

    public void addFamiliarity(UUID other, float delta) {
        setFamiliarity(other, getFamiliarity(other) + delta);
    }

    /**
     * Decay familiarity only toward players in {@code onlineUuids} who haven't been
     * seen together recently. Players who are offline are completely ignored —
     * familiarity is preserved until they are both online and apart.
     *
     * @param onlineUuids  UUIDs of all players currently on the server
     * @param decayPerSecond rate from config
     */
    public void decayFamiliarityToward(java.util.Set<UUID> onlineUuids, double decayPerSecond) {
        long nowMs = System.currentTimeMillis();
        for (UUID uuid : familiarityMap.keySet()) {
            // Skip players who are offline — their familiarity is frozen
            if (!onlineUuids.contains(uuid)) continue;

            long lastSeen = lastSeenTogetherMs.getOrDefault(uuid, nowMs);
            double secondsApart = (nowMs - lastSeen) / 1000.0;
            float decay = (float) (decayPerSecond * secondsApart);
            float current = familiarityMap.get(uuid);
            familiarityMap.put(uuid, Math.max(0f, current - decay));
        }
        // Remove fully-decayed entries to save memory
        familiarityMap.entrySet().removeIf(e -> e.getValue() <= 0f);
    }

    public void markSeenTogether(UUID other) {
        lastSeenTogetherMs.put(other, System.currentTimeMillis());
    }

    // ── Effective gain multiplier ────────────────────────────────────────────

    /**
     * Returns the effective meter-gain multiplier when near {@code other}.
     * Familiarity 0.0 → full gain; familiarity 1.0 → zero gain.
     */
    public float effectiveGainMultiplier(UUID other) {
        return 1.0f - getFamiliarity(other);
    }

    // ── Online tracking ──────────────────────────────────────────────────────

    public long getLastOnlineMs() { return lastOnlineMs; }
    public void markOnline() { lastOnlineMs = System.currentTimeMillis(); }

    // ── NBT serialisation ────────────────────────────────────────────────────

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("socialMeter", socialMeter);
        tag.putLong("lastOnlineMs", lastOnlineMs);

        ListTag familiarityList = new ListTag();
        for (Map.Entry<UUID, Float> entry : familiarityMap.entrySet()) {
            CompoundTag entry_tag = new CompoundTag();
            entry_tag.putUUID("uuid", entry.getKey());
            entry_tag.putFloat("value", entry.getValue());
            // Store last-seen timestamp alongside familiarity so decay survives restarts
            entry_tag.putLong("lastSeenMs", lastSeenTogetherMs.getOrDefault(entry.getKey(), lastOnlineMs));
            familiarityList.add(entry_tag);
        }
        tag.put("familiarity", familiarityList);
        return tag;
    }

    public static PlayerSocialData load(CompoundTag tag) {
        PlayerSocialData data = new PlayerSocialData();
        data.socialMeter = tag.getFloat("socialMeter");
        data.lastOnlineMs = tag.getLong("lastOnlineMs");

        ListTag familiarityList = tag.getList("familiarity", Tag.TAG_COMPOUND);
        for (int i = 0; i < familiarityList.size(); i++) {
            CompoundTag entry_tag = familiarityList.getCompound(i);
            UUID uuid = entry_tag.getUUID("uuid");
            float value = entry_tag.getFloat("value");
            long lastSeenMs = entry_tag.getLong("lastSeenMs");
            data.familiarityMap.put(uuid, value);
            data.lastSeenTogetherMs.put(uuid, lastSeenMs);
        }
        return data;
    }
}
