package com.example.socialisolation.commands;

import com.example.socialisolation.data.PlayerSocialData;
import com.example.socialisolation.data.SocialSavedData;
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
                .then(Commands.literal("get")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> executeGet(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))
                        )
                        .executes(ctx -> {
                            try {
                                ServerPlayer p = ctx.getSource().getPlayerOrException();
                                return executeGet(ctx.getSource(), p);
                            } catch (CommandSyntaxException e) {
                                ctx.getSource().sendFailure(Component.literal("You must specify a player when running from console."));
                                return 0;
                            }
                        })
                )
                .then(Commands.literal("set")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                                .executes(ctx -> {
                                    try {
                                        ServerPlayer p = ctx.getSource().getPlayerOrException();
                                        int val = IntegerArgumentType.getInteger(ctx, "value");
                                        return executeSet(ctx.getSource(), p, val);
                                    } catch (CommandSyntaxException e) {
                                        ctx.getSource().sendFailure(Component.literal("You must specify a player when running from console."));
                                        return 0;
                                    }
                                })
                        )
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                                        .executes(ctx -> {
                                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                            int val = IntegerArgumentType.getInteger(ctx, "value");
                                            return executeSet(ctx.getSource(), target, val);
                                        })
                                )
                        )
                )
        );
    }

    private static int executeGet(CommandSourceStack source, ServerPlayer target) {
        MinecraftServer server = source.getServer();
        SocialSavedData saved = SocialSavedData.get(server);
        PlayerSocialData data = saved.getOrCreate(target.getUUID());
        float meter = data.getSocialMeter();
        source.sendSuccess(() -> Component.literal("Social meter for " + target.getName().getString() + ": " + String.format("%.2f", meter)), false);
        return 1;
    }

    private static int executeSet(CommandSourceStack source, ServerPlayer target, int value) {
        MinecraftServer server = source.getServer();
        SocialSavedData saved = SocialSavedData.get(server);
        PlayerSocialData data = saved.getOrCreate(target.getUUID());
        data.setSocialMeter(value);
        saved.setDirty();
        source.sendSuccess(() -> Component.literal("Set social meter for " + target.getName().getString() + " to " + value), true);
        return 1;
    }
}
