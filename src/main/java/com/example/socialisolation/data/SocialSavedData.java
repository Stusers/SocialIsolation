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
import java.util.Map;
import java.util.UUID;

/**
 * Server-side persistent storage for all player social data.
 * Stored in the overworld's data folder as "socialisolation_data.dat".
 *
 * Call {@link #get(MinecraftServer)} to retrieve (or create) the instance.
 * Call {@link #setDirty()} after any mutation so NeoForge writes it to disk.
 */
public class SocialSavedData extends SavedData {

    private static final String DATA_KEY = "socialisolation_data";

    private final Map<UUID, PlayerSocialData> playerDataMap = new HashMap<>();

    // ── Factory / access ─────────────────────────────────────────────────────

    private static final SavedData.Factory<SocialSavedData> FACTORY =
            new SavedData.Factory<>(SocialSavedData::new, SocialSavedData::load, null);

    /**
     * Retrieve the singleton instance from the overworld's data storage.
     * Creates a fresh instance if none exists yet.
     */
    public static SocialSavedData get(MinecraftServer server) {
        DimensionDataStorage storage = server.overworld().getDataStorage();
        return storage.computeIfAbsent(FACTORY, DATA_KEY);
    }

    // ── Player data access ───────────────────────────────────────────────────

    /**
     * Returns the social data for the given player UUID,
     * creating a default entry if one does not yet exist.
     */
    public PlayerSocialData getOrCreate(UUID playerId) {
        return playerDataMap.computeIfAbsent(playerId, id -> new PlayerSocialData());
    }

    public boolean has(UUID playerId) {
        return playerDataMap.containsKey(playerId);
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
        SocialIsolation.LOGGER.info("Loaded social data for {} player(s).", data.playerDataMap.size());
        return data;
    }
}

