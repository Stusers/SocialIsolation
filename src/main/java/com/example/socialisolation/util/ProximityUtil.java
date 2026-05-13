package com.example.socialisolation.util;

import com.example.socialisolation.data.SocialSavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Lightweight proximity helpers using vanilla's AABB entity query.
 * This is the same query used internally by vanilla — very cheap at runtime.
 */
public class ProximityUtil {

    private ProximityUtil() {}

    /**
     * Returns all ServerPlayers within {@code radius} blocks of {@code player},
     * excluding the player themselves.
     */
    public static List<ServerPlayer> getNearbyPlayers(ServerPlayer player, int radius) {
        ServerLevel level = player.serverLevel();
        AABB searchBox = player.getBoundingBox().inflate(radius);
        return level.getEntitiesOfClass(ServerPlayer.class, searchBox,
                other -> !other.getUUID().equals(player.getUUID()));
    }

    /**
     * Returns how many "social sources" are near this player.
     * Counts real players + any registered Willson slimes within range.
     * Willson slimes each count as 1 social source (same as a player).
     */
    public static int countNearbySocialSources(ServerPlayer player, int radius, SocialSavedData savedData) {
        int count = getNearbyPlayers(player, radius).size();

        Set<UUID> willsonIds = savedData.getWillsonSlimes();
        if (!willsonIds.isEmpty()) {
            ServerLevel level = player.serverLevel();
            AABB searchBox = player.getBoundingBox().inflate(radius);
            List<Slime> nearbySlimes = level.getEntitiesOfClass(Slime.class, searchBox,
                    slime -> willsonIds.contains(slime.getUUID()));
            count += nearbySlimes.size();
        }

        return count;
    }

    public static boolean hasNearbyPlayer(ServerPlayer player, int radius) {
        return !getNearbyPlayers(player, radius).isEmpty();
    }
}
