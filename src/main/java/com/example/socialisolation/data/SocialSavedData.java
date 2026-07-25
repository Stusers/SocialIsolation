package com.example.socialisolation.data;

import com.example.socialisolation.SocialIsolation;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server-side persistent storage for all player social data.
 * Also persists the set of "Willson" slime UUIDs and the admin-configured
 * first-join welcome message.
 *
 * The "seen" flag for the welcome message is stored in PlayerSocialData
 * (hasSeenWelcome) so it scales naturally with the player record instead
 * of accumulating an unbounded global set here.
 */
public class SocialSavedData extends SavedData {

    private static final String DATA_KEY = "socialisolation_data";

    private final Map<UUID, PlayerSocialData> playerDataMap = new HashMap<>();
    /** UUIDs of slimes registered as Willson companions — treated like players for proximity. */
    private final Set<UUID> willsonSlimes = new HashSet<>();
    /**
     * Admin-set welcome message shown to players the first time they join after it is set.
     * Null means no message is shown. Supports § color codes. Use \n for line breaks.
     */
    private String welcomeMessage = null;

    // ── Factory / access ─────────────────────────────────────────────────────

    private static final SavedData.Factory<SocialSavedData> FACTORY =
            new SavedData.Factory<>(SocialSavedData::new, SocialSavedData::load, null);

    public static SocialSavedData get(MinecraftServer server) {
        DimensionDataStorage storage = server.overworld().getDataStorage();
        return storage.computeIfAbsent(FACTORY, DATA_KEY);
    }

    // ── Player data access ───────────────────────────────────────────────────

    public PlayerSocialData getOrCreate(UUID playerId) {
        return playerDataMap.computeIfAbsent(playerId, id -> new PlayerSocialData());
    }

    public boolean has(UUID playerId) {
        return playerDataMap.containsKey(playerId);
    }

    // ── Willson companion access ─────────────────────────────────────────────

    public Set<UUID> getWillsonSlimes() { return willsonSlimes; }

    public boolean addWillson(UUID uuid) {
        boolean added = willsonSlimes.add(uuid);
        if (added) setDirty();
        return added;
    }

    public boolean removeWillson(UUID uuid) {
        boolean removed = willsonSlimes.remove(uuid);
        if (removed) setDirty();
        return removed;
    }

    public boolean isWillson(UUID uuid) {
        return willsonSlimes.contains(uuid);
    }

    // ── Welcome message ──────────────────────────────────────────────────────

    public String getWelcomeMessage() { return welcomeMessage; }

    public boolean hasWelcomeMessage() {
        return welcomeMessage != null;
    }

    public void setWelcomeMessage(String message) {
        this.welcomeMessage = (message == null || message.isBlank()) ? null : message;
        setDirty();
    }

    /**
     * Resets the seen flag on every known player so they all see the message again on next join.
     * Called by executeWelcomeSet after updating the message text.
     */
    public void resetAllWelcomeSeen() {
        boolean any = false;
        for (PlayerSocialData data : playerDataMap.values()) {
            if (data.hasSeenWelcome()) {
                data.setHasSeenWelcome(false);
                any = true;
            }
        }
        if (any) setDirty();
    }

    /**
     * Resets the seen flag for a single player so they see the message again on next join.
     */
    public void resetWelcomeSeen(UUID playerId) {
        PlayerSocialData data = playerDataMap.get(playerId);
        if (data != null && data.hasSeenWelcome()) {
            data.setHasSeenWelcome(false);
            setDirty();
        }
    }

    // ── NBT serialisation ────────────────────────────────────────────────────

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, PlayerSocialData> entry : playerDataMap.entrySet()) {
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID("uuid", entry.getKey());
            playerTag.put("data", entry.getValue().save());
            list.add(playerTag);
        }
        tag.put("players", list);

        ListTag willsonList = new ListTag();
        for (UUID uuid : willsonSlimes) {
            CompoundTag wTag = new CompoundTag();
            wTag.putUUID("uuid", uuid);
            willsonList.add(wTag);
        }
        tag.put("willsonSlimes", willsonList);

        if (welcomeMessage != null) {
            tag.putString("welcomeMessage", welcomeMessage);
        }

        return tag;
    }

    public static SocialSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        SocialSavedData data = new SocialSavedData();
        ListTag list = tag.getList("players", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag playerTag = list.getCompound(i);
            UUID uuid = playerTag.getUUID("uuid");
            PlayerSocialData playerData = PlayerSocialData.load(playerTag.getCompound("data"));
            data.playerDataMap.put(uuid, playerData);
        }

        ListTag willsonList = tag.getList("willsonSlimes", Tag.TAG_COMPOUND);
        for (int i = 0; i < willsonList.size(); i++) {
            data.willsonSlimes.add(willsonList.getCompound(i).getUUID("uuid"));
        }

        if (tag.contains("welcomeMessage", Tag.TAG_STRING)) {
            String msg = tag.getString("welcomeMessage");
            if (!msg.isBlank()) data.welcomeMessage = msg;
        }

        SocialIsolation.LOGGER.info("Loaded social data for {} player(s), {} Willson slime(s).",
                data.playerDataMap.size(), data.willsonSlimes.size());
        return data;
    }
}
