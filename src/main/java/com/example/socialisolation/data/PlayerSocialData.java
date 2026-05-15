package com.example.socialisolation.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player data model stored inside SocialSavedData.
 */
public class PlayerSocialData {

    private static final float SOCIAL_METER_DEFAULT = 70.0f; // Start with a buffer so new players don't get penalised immediately

    private float socialMeter;
    private final Map<UUID, Float> familiarityMap = new HashMap<>();
    private final Map<UUID, Long> lastSeenTogetherMs = new HashMap<>();
    private long lastOnlineMs;

    /** Wall-clock ms of the last time the meter was updated — enables accurate delta-time rates regardless of TPS. */
    private long lastMeterUpdateMs;
    /** The tier last applied to this player — used to avoid redundant effect clear/re-add cycles. */
    private int lastAppliedTierOrdinal = -1;
    /** Cumulative total of all social meter points ever gained (not lost). Useful for progression/land-claim integrations. */
    private float totalPointsRegained = 0f;

    public PlayerSocialData() {
        this.socialMeter = SOCIAL_METER_DEFAULT;
        this.lastOnlineMs = System.currentTimeMillis();
        this.lastMeterUpdateMs = System.currentTimeMillis();
    }

    // ── Meter (time-accurate) ────────────────────────────────────────────────

    public float getSocialMeter() { return socialMeter; }

    public float getTotalPointsRegained() { return totalPointsRegained; }

    public void setSocialMeter(float value) {
        this.socialMeter = Math.max(0f, Math.min(100f, value));
    }

    /** Adjust meter by {@code ratePerSecond} multiplied by real elapsed seconds since last update. */
    public void adjustMeterRate(float ratePerSecond) {
        long now = System.currentTimeMillis();
        float deltaSeconds = (now - lastMeterUpdateMs) / 1000.0f;
        if (deltaSeconds > 0) {
            float delta = ratePerSecond * deltaSeconds;
            setSocialMeter(socialMeter + delta);
            lastMeterUpdateMs = now;
            if (delta > 0) {
                totalPointsRegained += delta;
            }
        }
    }

    public void forceSetLastMeterUpdateNow() {
        lastMeterUpdateMs = System.currentTimeMillis();
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
     * Decay familiarity toward online players who are not nearby.
     * Resets last-seen timestamp after applying decay so subsequent ticks
     * only count the delta, preventing compounding.
     */
    public void decayFamiliarityToward(java.util.Set<UUID> onlineUuids, double decayPerSecond) {
        long nowMs = System.currentTimeMillis();
        for (UUID uuid : new java.util.ArrayList<>(familiarityMap.keySet())) {
            if (!onlineUuids.contains(uuid)) continue;

            long lastSeen = lastSeenTogetherMs.getOrDefault(uuid, nowMs);
            double secondsApart = (nowMs - lastSeen) / 1000.0;
            if (secondsApart <= 0) continue;

            float decay = (float) (decayPerSecond * secondsApart);
            float current = familiarityMap.get(uuid);
            float next = Math.max(0f, current - decay);
            if (next <= 0f) {
                familiarityMap.remove(uuid);
                lastSeenTogetherMs.remove(uuid);
            } else {
                familiarityMap.put(uuid, next);
                // Reset timestamp so next decay only counts new apart-time
                lastSeenTogetherMs.put(uuid, nowMs);
            }
        }
    }

    public void markSeenTogether(UUID other) {
        lastSeenTogetherMs.put(other, System.currentTimeMillis());
    }

    // ── Effective gain multiplier ────────────────────────────────────────────

    public float effectiveGainMultiplier(UUID other) {
        if (!com.example.socialisolation.config.SocialConfig.ENABLE_FAMILIARITY.get()) {
            return 1.0f;
        }
        return 1.0f - getFamiliarity(other);
    }

    // ── Online tracking ──────────────────────────────────────────────────────

    public long getLastOnlineMs() { return lastOnlineMs; }
    public void markOnline() {
        lastOnlineMs = System.currentTimeMillis();
        lastMeterUpdateMs = System.currentTimeMillis(); // Prevent offline time from being counted as "alone time"
    }

    // ── Effect tier tracking ─────────────────────���───────────────────────────

    public int getLastAppliedTierOrdinal() { return lastAppliedTierOrdinal; }
    public void setLastAppliedTierOrdinal(int ordinal) { this.lastAppliedTierOrdinal = ordinal; }

    // ── NBT serialisation ────────────────────────────────────────────────────

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("socialMeter", socialMeter);
        tag.putLong("lastOnlineMs", lastOnlineMs);
        tag.putLong("lastMeterUpdateMs", lastMeterUpdateMs);
        tag.putInt("lastAppliedTierOrdinal", lastAppliedTierOrdinal);
        tag.putFloat("totalPointsRegained", totalPointsRegained);

        ListTag familiarityList = new ListTag();
        for (Map.Entry<UUID, Float> entry : familiarityMap.entrySet()) {
            CompoundTag entry_tag = new CompoundTag();
            entry_tag.putUUID("uuid", entry.getKey());
            entry_tag.putFloat("value", entry.getValue());
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
        data.lastMeterUpdateMs = tag.getLong("lastMeterUpdateMs");
        data.lastAppliedTierOrdinal = tag.getInt("lastAppliedTierOrdinal");
        if (tag.contains("totalPointsRegained", net.minecraft.nbt.Tag.TAG_ANY_NUMERIC)) {
            data.totalPointsRegained = tag.getFloat("totalPointsRegained");
        }

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
