package com.example.socialisolation.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.phys.AABB;

import java.util.List;

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
     * Returns all nearby Slimes named "Willson" (case-insensitive) within {@code radius}.
     * These count as social sources just like real players.
     */
    public static List<Slime> getNearbyWillsonSlimes(ServerPlayer player, int radius) {
        ServerLevel level = player.serverLevel();
        AABB searchBox = player.getBoundingBox().inflate(radius);
        return level.getEntitiesOfClass(Slime.class, searchBox, ProximityUtil::isWillson);
    }

    private static boolean isWillson(Slime slime) {
        if (!slime.hasCustomName()) return false;
        String name = slime.getCustomName().getString();
        return name.equalsIgnoreCase("willson");
    }

    /**
     * Returns how many "social sources" are near this player.
     * Counts real players + any Willson-named slimes within range.
     */
    public static int countNearbySocialSources(ServerPlayer player, int radius) {
        return getNearbyPlayers(player, radius).size() + getNearbyWillsonSlimes(player, radius).size();
    }

    public static boolean hasNearbyPlayer(ServerPlayer player, int radius) {
        return !getNearbyPlayers(player, radius).isEmpty();
    }
}
