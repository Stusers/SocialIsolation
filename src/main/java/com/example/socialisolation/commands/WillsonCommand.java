package com.example.socialisolation.commands;

import com.example.socialisolation.data.SocialSavedData;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * /social willson add    — register the nearest slime as a Willson companion
 * /social willson remove — unregister the nearest registered Willson slime
 * /social willson list   — list all registered Willson UUIDs and their alive status
 *
 * Requires permission level 2 (OP). Intended for testing.
 * A Willson slime counts as a social source for proximity checks without
 * accumulating familiarity, so it never becomes "stale" like a real player.
 */
public class WillsonCommand {

    private static final int SEARCH_RADIUS = 20;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext ctx) {
        dispatcher.register(Commands.literal("social")
                .then(Commands.literal("willson")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("add")
                                .executes(context -> executeAdd(context.getSource()))
                        )
                        .then(Commands.literal("remove")
                                .executes(context -> executeRemove(context.getSource()))
                        )
                        .then(Commands.literal("list")
                                .executes(context -> executeList(context.getSource()))
                        )
                )
        );
    }

    private static int executeAdd(CommandSourceStack src) {
        ServerPlayer player;
        try { player = src.getPlayerOrException(); }
        catch (Exception e) {
            src.sendFailure(Component.literal("Must be run by a player in-game."));
            return 0;
        }

        // Find the nearest slime within SEARCH_RADIUS blocks
        AABB box = player.getBoundingBox().inflate(SEARCH_RADIUS);
        List<Slime> slimes = player.serverLevel().getEntitiesOfClass(Slime.class, box, s -> true);

        if (slimes.isEmpty()) {
            src.sendFailure(Component.literal("No slime found within " + SEARCH_RADIUS + " blocks. Spawn one and try again."));
            return 0;
        }

        // Pick the closest
        Slime closest = slimes.stream()
                .min((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player)))
                .orElseThrow();

        SocialSavedData savedData = SocialSavedData.get(src.getServer());
        if (savedData.isWillson(closest.getUUID())) {
            src.sendFailure(Component.literal("That slime is already registered as Willson (UUID: " + closest.getUUID() + ")."));
            return 0;
        }

        // Give it a name if it doesn't have one
        if (!closest.hasCustomName()) {
            closest.setCustomName(Component.literal("Willson"));
            closest.setCustomNameVisible(true);
        }

        savedData.addWillson(closest.getUUID());
        final String uuidStr = closest.getUUID().toString();
        final String name = closest.getCustomName() != null ? closest.getCustomName().getString() : "Slime";
        src.sendSuccess(() -> Component.literal("[Social] Registered \"" + name + "\" as a Willson companion. UUID: " + uuidStr), true);
        return 1;
    }

    private static int executeRemove(CommandSourceStack src) {
        ServerPlayer player;
        try { player = src.getPlayerOrException(); }
        catch (Exception e) {
            src.sendFailure(Component.literal("Must be run by a player in-game."));
            return 0;
        }

        SocialSavedData savedData = SocialSavedData.get(src.getServer());
        Set<UUID> willsonIds = savedData.getWillsonSlimes();

        if (willsonIds.isEmpty()) {
            src.sendFailure(Component.literal("No Willson companions are registered."));
            return 0;
        }

        // Find the nearest registered Willson slime
        AABB box = player.getBoundingBox().inflate(SEARCH_RADIUS);
        List<Slime> nearbyWillsons = player.serverLevel().getEntitiesOfClass(Slime.class, box,
                s -> willsonIds.contains(s.getUUID()));

        if (nearbyWillsons.isEmpty()) {
            src.sendFailure(Component.literal("No registered Willson slime within " + SEARCH_RADIUS + " blocks."));
            return 0;
        }

        Slime closest = nearbyWillsons.stream()
                .min((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player)))
                .orElseThrow();

        savedData.removeWillson(closest.getUUID());
        final String uuidStr = closest.getUUID().toString();
        src.sendSuccess(() -> Component.literal("[Social] Removed Willson slime (UUID: " + uuidStr + ")."), true);
        return 1;
    }

    private static int executeList(CommandSourceStack src) {
        SocialSavedData savedData = SocialSavedData.get(src.getServer());
        Set<UUID> willsonIds = savedData.getWillsonSlimes();

        if (willsonIds.isEmpty()) {
            src.sendSuccess(() -> Component.literal("[Social] No Willson companions registered."), false);
            return 1;
        }

        StringBuilder sb = new StringBuilder("[Social] Willson companions (" + willsonIds.size() + "):\n");
        for (UUID uuid : willsonIds) {
            sb.append("  ").append(uuid).append("\n");
        }
        final String msg = sb.toString().trim();
        src.sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }
}

