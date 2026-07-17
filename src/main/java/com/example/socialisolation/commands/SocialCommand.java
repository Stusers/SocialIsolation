package com.example.socialisolation.commands;

import com.example.socialisolation.compat.OpenPACCompat;
import com.example.socialisolation.config.SocialConfig;
import com.example.socialisolation.data.PlayerSocialData;
import com.example.socialisolation.data.SocialSavedData;
import com.example.socialisolation.effects.EffectApplicator;
import com.example.socialisolation.util.ChunkRewardMath;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class SocialCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        dispatcher.register(Commands.literal("social")
                // /social status — check your own meter and tier (no OP required)
                .then(Commands.literal("status")
                        .executes(ctx -> {
                            try {
                                return executeStatus(ctx.getSource(), ctx.getSource().getPlayerOrException());
                            } catch (CommandSyntaxException e) {
                                ctx.getSource().sendFailure(Component.literal("Must be run as a player."));
                                return 0;
                            }
                        })
                        // /social status <player> — OP only for querying others
                        .then(Commands.argument("player", EntityArgument.player())
                                .requires(src -> src.hasPermission(2))
                                .executes(ctx -> executeStatus(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))
                        )
                )
                // /social chunks — show chunk reward progress (no OP for self)
                .then(Commands.literal("chunks")
                        .executes(ctx -> {
                            try {
                                return executeChunks(ctx.getSource(), ctx.getSource().getPlayerOrException());
                            } catch (CommandSyntaxException e) {
                                ctx.getSource().sendFailure(Component.literal("Must be run as a player."));
                                return 0;
                            }
                        })
                        .then(Commands.argument("player", EntityArgument.player())
                                .requires(src -> src.hasPermission(2))
                                .executes(ctx -> executeChunks(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))
                        )
                        // /social chunks sync — force OPAC sync for all online players (OP only)
                        .then(Commands.literal("sync")
                                .requires(src -> src.hasPermission(2))
                                .executes(ctx -> executeChunksSync(ctx.getSource(), null))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> executeChunksSync(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))
                                )
                        )
                )
                // /social set <value> / /social set <player> <value> — OP only
                .then(Commands.literal("set")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                                .executes(ctx -> {
                                    try {
                                        ServerPlayer p = ctx.getSource().getPlayerOrException();
                                        return executeSet(ctx.getSource(), p, IntegerArgumentType.getInteger(ctx, "value"));
                                    } catch (CommandSyntaxException e) {
                                        ctx.getSource().sendFailure(Component.literal("Specify a player when running from console."));
                                        return 0;
                                    }
                                })
                        )
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                                        .executes(ctx -> executeSet(
                                                ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player"),
                                                IntegerArgumentType.getInteger(ctx, "value")))
                                )
                        )
                )
        );
    }

    private static int executeStatus(CommandSourceStack source, ServerPlayer target) {
        MinecraftServer server = source.getServer();
        SocialSavedData saved = SocialSavedData.get(server);
        PlayerSocialData data = saved.getOrCreate(target.getUUID());
        float meter = data.getSocialMeter();
        EffectApplicator.SocialTier tier = EffectApplicator.getTier(meter);

        boolean isSelf = source.isPlayer() && source.getPlayer() == target;
        String name = isSelf ? "Your" : target.getName().getString() + "'s";

        float totalPoints = data.getTotalPointsRegained();
        source.sendSuccess(() -> Component.literal(
                name + " social meter: " + String.format("%.1f", meter) + "/100 (" + tierLabel(tier) + ")" +
                " | lifetime points: " + String.format("%.0f", totalPoints)
        ), false);
        return 1;
    }

    private static int executeSet(CommandSourceStack source, ServerPlayer target, int value) {
        MinecraftServer server = source.getServer();
        SocialSavedData saved = SocialSavedData.get(server);
        PlayerSocialData data = saved.getOrCreate(target.getUUID());
        data.setSocialMeter(value);
        saved.setDirty();
        source.sendSuccess(() -> Component.literal(
                "Set " + target.getName().getString() + "'s social meter to " + value), true);
        return 1;
    }

    private static int executeChunks(CommandSourceStack source, ServerPlayer target) {
        if (!OpenPACCompat.isLoaded()) {
            source.sendFailure(Component.literal("Open Parties and Claims is not installed on this server."));
            return 0;
        }

        SocialSavedData saved = SocialSavedData.get(source.getServer());
        PlayerSocialData data = saved.getOrCreate(target.getUUID());
        float totalPoints = data.getTotalPointsRegained();

        int pointsPerChunk = SocialConfig.OPAC_POINTS_PER_BONUS_CHUNK.get();
        int maxChunks = SocialConfig.OPAC_MAX_BONUS_CHUNKS.get();
        int chunks = ChunkRewardMath.chunksEarned(totalPoints, pointsPerChunk, maxChunks);
        boolean atCap = chunks >= maxChunks;

        boolean isSelf = source.isPlayer() && source.getPlayer() == target;
        String name = isSelf ? "Your" : target.getName().getString() + "'s";

        String msg;
        if (pointsPerChunk == 0) {
            msg = "OPAC chunk rewards are disabled (opacPointsPerBonusChunk = 0).";
        } else if (atCap) {
            msg = name + " bonus chunks: " + chunks + "/" + maxChunks + " (MAX)" +
                  " | lifetime points: " + String.format("%.0f", totalPoints);
        } else {
            float spent = ChunkRewardMath.pointsSpent(chunks, pointsPerChunk);
            float nextCost = ChunkRewardMath.nextChunkCost(chunks, pointsPerChunk);
            float progress = totalPoints - spent;
            msg = name + " bonus chunks: " + chunks + "/" + maxChunks +
                  " | next chunk: " + String.format("%.0f", progress) + "/" + String.format("%.0f", nextCost) + " pts" +
                  " | lifetime: " + String.format("%.0f", totalPoints) + " pts";
        }

        source.sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }

    // target == null means sync all online players
    private static int executeChunksSync(CommandSourceStack source, ServerPlayer target) {
        if (!OpenPACCompat.isLoaded()) {
            source.sendFailure(Component.literal("Open Parties and Claims is not installed on this server."));
            return 0;
        }

        MinecraftServer server = source.getServer();
        SocialSavedData saved = SocialSavedData.get(server);

        if (target != null) {
            PlayerSocialData data = saved.getOrCreate(target.getUUID());
            OpenPACCompat.updateBonusChunks(server, target.getUUID(), data.getTotalPointsRegained());
            source.sendSuccess(() -> Component.literal(
                    "Synced OPAC bonus chunks for " + target.getName().getString()), true);
            return 1;
        }

        // Sync all online players
        var players = server.getPlayerList().getPlayers();
        for (ServerPlayer p : players) {
            PlayerSocialData data = saved.getOrCreate(p.getUUID());
            OpenPACCompat.updateBonusChunks(server, p.getUUID(), data.getTotalPointsRegained());
        }
        int count = players.size();
        source.sendSuccess(() -> Component.literal(
                "Synced OPAC bonus chunks for " + count + " online player(s)."), true);
        return 1;
    }

    private static String tierLabel(EffectApplicator.SocialTier tier) {
        return switch (tier) {
            case THRIVING -> "§aThriving§r";
            case NEUTRAL  -> "§7Neutral§r";
            case LONELY   -> "§eLonely§r";
            case ISOLATED -> "§cIsolated§r";
        };
    }
}
