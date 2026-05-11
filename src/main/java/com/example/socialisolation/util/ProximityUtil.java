package com.example.socialisolation.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Lightweight proximity helpers using vanilla's AABB entity query.
 * This is the same query used internally by vanilla — very cheap at runtime.
 */
public class ProximityUtil {

    private ProximityUtil() {}

    /**
     * Returns all ServerPlayers within {@code radius} blocks of {@code player}
     * in the same dimension, excluding the player themselves.
     */
    public static List<ServerPlayer> getNearbyPlayers(ServerPlayer player, int radius) {
        ServerLevel level = player.serverLevel();
        AABB searchBox = player.getBoundingBox().inflate(radius);
        return level.getEntitiesOfClass(ServerPlayer.class, searchBox,
                other -> !other.getUUID().equals(player.getUUID()));
    }

    /**
     * Returns true if there is at least one other player within {@code radius}
     * blocks of {@code player} in the same dimension.
     */
    public static boolean hasNearbyPlayer(ServerPlayer player, int radius) {
        return !getNearbyPlayers(player, radius).isEmpty();
    }
}

