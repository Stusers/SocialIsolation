package com.example.socialisolation.commands;

import com.example.socialisolation.config.SocialConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * /social config get <key>
 * /social config set <key> <value>
 * /social config list
 *
 * Requires permission level 2 (OP).
 *
 * Keys:
 *   proximityRadius         (int)
 *   minutesToFullMeter      (int)
 *   minutesToEmptyMeter     (int)
 *   hoursToMaxFamiliarity   (int)
 *   hoursToLoseFamiliarity  (int)
 *   thresholdThriving       (int)
 *   thresholdLonely         (int)
 *   thresholdIsolated       (int)
 *   enableBenefits          (bool)
 *   enablePenalties         (bool)
 *   enableFamiliarity       (bool)
 *   phantomSpawnWhenIsolated (bool)
 */
public class SocialConfigCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext ctx) {
        dispatcher.register(Commands.literal("social")
                .then(Commands.literal("config")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("get")
                                .then(Commands.argument("key", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            for (String k : KEYS) builder.suggest(k);
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> executeGet(context.getSource(),
                                                StringArgumentType.getString(context, "key")))
                                )
                        )
                        .then(Commands.literal("set")
                                .then(Commands.argument("key", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            for (String k : KEYS) builder.suggest(k);
                                            return builder.buildFuture();
                                        })
                                        .then(Commands.argument("value", StringArgumentType.greedyString())
                                                .executes(context -> executeSet(context.getSource(),
                                                        StringArgumentType.getString(context, "key"),
                                                        StringArgumentType.getString(context, "value")))
                                        )
                                )
                        )
                        .then(Commands.literal("list")
                                .executes(context -> executeList(context.getSource()))
                        )
                )
        );
    }

    private static final String[] KEYS = {
            "proximityRadius",
            "minutesToFullMeter", "minutesToEmptyMeter",
            "hoursToMaxFamiliarity", "hoursToLoseFamiliarity",
            "thresholdThriving", "thresholdLonely", "thresholdIsolated",
            "enableBenefits", "enablePenalties", "enableFamiliarity", "phantomSpawnWhenIsolated",
            "opacPointsPerBonusChunk", "opacMaxBonusChunks"
    };

    private static int executeGet(CommandSourceStack src, String key) {
        String value = switch (key) {
            case "proximityRadius"        -> String.valueOf(SocialConfig.PROXIMITY_RADIUS.get());
            case "minutesToFullMeter"     -> String.valueOf(SocialConfig.MINUTES_TO_FULL_METER.get());
            case "minutesToEmptyMeter"    -> String.valueOf(SocialConfig.MINUTES_TO_EMPTY_METER.get());
            case "hoursToMaxFamiliarity"  -> String.valueOf(SocialConfig.HOURS_TO_MAX_FAMILIARITY.get());
            case "hoursToLoseFamiliarity" -> String.valueOf(SocialConfig.HOURS_TO_LOSE_FAMILIARITY.get());
            case "thresholdThriving"      -> String.valueOf(SocialConfig.THRESHOLD_THRIVING.get());
            case "thresholdLonely"        -> String.valueOf(SocialConfig.THRESHOLD_LONELY.get());
            case "thresholdIsolated"      -> String.valueOf(SocialConfig.THRESHOLD_ISOLATED.get());
            case "enableBenefits"         -> String.valueOf(SocialConfig.ENABLE_BENEFITS.get());
            case "enablePenalties"        -> String.valueOf(SocialConfig.ENABLE_PENALTIES.get());
            case "enableFamiliarity"      -> String.valueOf(SocialConfig.ENABLE_FAMILIARITY.get());
            case "phantomSpawnWhenIsolated"  -> String.valueOf(SocialConfig.PHANTOM_SPAWN_WHEN_ISOLATED.get());
            case "opacPointsPerBonusChunk"  -> String.valueOf(SocialConfig.OPAC_POINTS_PER_BONUS_CHUNK.get());
            case "opacMaxBonusChunks"       -> String.valueOf(SocialConfig.OPAC_MAX_BONUS_CHUNKS.get());
            default -> null;
        };
        if (value == null) {
            src.sendFailure(Component.literal("Unknown config key: " + key + ". Use /social config list."));
            return 0;
        }
        src.sendSuccess(() -> Component.literal("[Social] " + key + " = " + value), false);
        return 1;
    }

    private static int executeSet(CommandSourceStack src, String key, String rawValue) {
        try {
            switch (key) {
                case "proximityRadius"        -> SocialConfig.PROXIMITY_RADIUS.set(Integer.parseInt(rawValue.trim()));
                case "minutesToFullMeter"     -> SocialConfig.MINUTES_TO_FULL_METER.set(Integer.parseInt(rawValue.trim()));
                case "minutesToEmptyMeter"    -> SocialConfig.MINUTES_TO_EMPTY_METER.set(Integer.parseInt(rawValue.trim()));
                case "hoursToMaxFamiliarity"  -> SocialConfig.HOURS_TO_MAX_FAMILIARITY.set(Integer.parseInt(rawValue.trim()));
                case "hoursToLoseFamiliarity" -> SocialConfig.HOURS_TO_LOSE_FAMILIARITY.set(Integer.parseInt(rawValue.trim()));
                case "thresholdThriving"      -> SocialConfig.THRESHOLD_THRIVING.set(Integer.parseInt(rawValue.trim()));
                case "thresholdLonely"        -> SocialConfig.THRESHOLD_LONELY.set(Integer.parseInt(rawValue.trim()));
                case "thresholdIsolated"      -> SocialConfig.THRESHOLD_ISOLATED.set(Integer.parseInt(rawValue.trim()));
                case "enableBenefits"         -> SocialConfig.ENABLE_BENEFITS.set(Boolean.parseBoolean(rawValue.trim()));
                case "enablePenalties"        -> SocialConfig.ENABLE_PENALTIES.set(Boolean.parseBoolean(rawValue.trim()));
                case "enableFamiliarity"      -> SocialConfig.ENABLE_FAMILIARITY.set(Boolean.parseBoolean(rawValue.trim()));
                case "phantomSpawnWhenIsolated" -> SocialConfig.PHANTOM_SPAWN_WHEN_ISOLATED.set(Boolean.parseBoolean(rawValue.trim()));
                case "opacPointsPerBonusChunk"  -> SocialConfig.OPAC_POINTS_PER_BONUS_CHUNK.set(Integer.parseInt(rawValue.trim()));
                case "opacMaxBonusChunks"       -> SocialConfig.OPAC_MAX_BONUS_CHUNKS.set(Integer.parseInt(rawValue.trim()));
                default -> {
                    src.sendFailure(Component.literal("Unknown config key: " + key + ". Use /social config list."));
                    return 0;
                }
            }
        } catch (NumberFormatException e) {
            src.sendFailure(Component.literal("Invalid value '" + rawValue + "' for key '" + key + "'."));
            return 0;
        }

        SocialConfig.validateThresholds();
        src.sendSuccess(() -> Component.literal("[Social] Set " + key + " = " + rawValue), true);
        return 1;
    }

    private static int executeList(CommandSourceStack src) {
        src.sendSuccess(() -> Component.literal(
                "[Social Config]\n" +
                "  proximityRadius         = " + SocialConfig.PROXIMITY_RADIUS.get() + " blocks\n" +
                "  minutesToFullMeter      = " + SocialConfig.MINUTES_TO_FULL_METER.get() + "\n" +
                "  minutesToEmptyMeter     = " + SocialConfig.MINUTES_TO_EMPTY_METER.get() + "\n" +
                "  hoursToMaxFamiliarity   = " + SocialConfig.HOURS_TO_MAX_FAMILIARITY.get() + "\n" +
                "  hoursToLoseFamiliarity  = " + SocialConfig.HOURS_TO_LOSE_FAMILIARITY.get() + "\n" +
                "  thresholdThriving       = " + SocialConfig.THRESHOLD_THRIVING.get() + "\n" +
                "  thresholdLonely         = " + SocialConfig.THRESHOLD_LONELY.get() + "\n" +
                "  thresholdIsolated       = " + SocialConfig.THRESHOLD_ISOLATED.get() + "\n" +
                "  enableBenefits          = " + SocialConfig.ENABLE_BENEFITS.get() + "\n" +
                "  enablePenalties         = " + SocialConfig.ENABLE_PENALTIES.get() + "\n" +
                "  enableFamiliarity       = " + SocialConfig.ENABLE_FAMILIARITY.get() + "\n" +
                "  phantomSpawnWhenIsolated = " + SocialConfig.PHANTOM_SPAWN_WHEN_ISOLATED.get() + "\n" +
                "  opacPointsPerBonusChunk  = " + SocialConfig.OPAC_POINTS_PER_BONUS_CHUNK.get() + "\n" +
                "  opacMaxBonusChunks       = " + SocialConfig.OPAC_MAX_BONUS_CHUNKS.get()
        ), false);
        return 1;
    }
}
