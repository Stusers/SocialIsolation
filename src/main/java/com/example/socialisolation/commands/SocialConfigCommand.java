package com.example.socialisolation.commands;

import com.example.socialisolation.config.SocialConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * /social config get <key>
 * /social config set <key> <value>
 *
 * Requires permission level 2 (OP).
 *
 * Keys:
 *   proximityRadius       (int)
 *   meterGainRate         (double)
 *   meterDrainRate        (double)
 *   familiarityGainRate   (double)
 *   familiarityDecayRate  (double)
 *   thresholdThriving     (double)
 *   thresholdLonely       (double)
 *   thresholdIsolated     (double)
 *   enableBenefits        (bool)
 *   enablePenalties       (bool)
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
            "proximityRadius", "meterGainRate", "meterDrainRate",
            "familiarityGainRate", "familiarityDecayRate",
            "thresholdThriving", "thresholdLonely", "thresholdIsolated",
            "enableBenefits", "enablePenalties"
    };

    private static int executeGet(CommandSourceStack src, String key) {
        String value = switch (key) {
            case "proximityRadius"      -> String.valueOf(SocialConfig.PROXIMITY_RADIUS.get());
            case "meterGainRate"        -> String.valueOf(SocialConfig.METER_GAIN_RATE.get());
            case "meterDrainRate"       -> String.valueOf(SocialConfig.METER_DRAIN_RATE.get());
            case "familiarityGainRate"  -> String.valueOf(SocialConfig.FAMILIARITY_GAIN_RATE.get());
            case "familiarityDecayRate" -> String.valueOf(SocialConfig.FAMILIARITY_DECAY_RATE.get());
            case "thresholdThriving"    -> String.valueOf(SocialConfig.THRESHOLD_THRIVING.get());
            case "thresholdLonely"      -> String.valueOf(SocialConfig.THRESHOLD_LONELY.get());
            case "thresholdIsolated"    -> String.valueOf(SocialConfig.THRESHOLD_ISOLATED.get());
            case "enableBenefits"       -> String.valueOf(SocialConfig.ENABLE_BENEFITS.get());
            case "enablePenalties"      -> String.valueOf(SocialConfig.ENABLE_PENALTIES.get());
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
                case "proximityRadius"      -> SocialConfig.PROXIMITY_RADIUS.set(Integer.parseInt(rawValue.trim()));
                case "meterGainRate"        -> SocialConfig.METER_GAIN_RATE.set(Double.parseDouble(rawValue.trim()));
                case "meterDrainRate"       -> SocialConfig.METER_DRAIN_RATE.set(Double.parseDouble(rawValue.trim()));
                case "familiarityGainRate"  -> SocialConfig.FAMILIARITY_GAIN_RATE.set(Double.parseDouble(rawValue.trim()));
                case "familiarityDecayRate" -> SocialConfig.FAMILIARITY_DECAY_RATE.set(Double.parseDouble(rawValue.trim()));
                case "thresholdThriving"    -> SocialConfig.THRESHOLD_THRIVING.set(Double.parseDouble(rawValue.trim()));
                case "thresholdLonely"      -> SocialConfig.THRESHOLD_LONELY.set(Double.parseDouble(rawValue.trim()));
                case "thresholdIsolated"    -> SocialConfig.THRESHOLD_ISOLATED.set(Double.parseDouble(rawValue.trim()));
                case "enableBenefits"       -> SocialConfig.ENABLE_BENEFITS.set(Boolean.parseBoolean(rawValue.trim()));
                case "enablePenalties"      -> SocialConfig.ENABLE_PENALTIES.set(Boolean.parseBoolean(rawValue.trim()));
                default -> {
                    src.sendFailure(Component.literal("Unknown config key: " + key + ". Use /social config list."));
                    return 0;
                }
            }
        } catch (NumberFormatException e) {
            src.sendFailure(Component.literal("Invalid value '" + rawValue + "' for key '" + key + "'."));
            return 0;
        }
        src.sendSuccess(() -> Component.literal("[Social] Set " + key + " = " + rawValue), true);
        return 1;
    }

    private static int executeList(CommandSourceStack src) {
        src.sendSuccess(() -> Component.literal(
                "[Social Config]\n" +
                "  proximityRadius       = " + SocialConfig.PROXIMITY_RADIUS.get() + "\n" +
                "  meterGainRate         = " + SocialConfig.METER_GAIN_RATE.get() + "\n" +
                "  meterDrainRate        = " + SocialConfig.METER_DRAIN_RATE.get() + "\n" +
                "  familiarityGainRate   = " + SocialConfig.FAMILIARITY_GAIN_RATE.get() + "\n" +
                "  familiarityDecayRate  = " + SocialConfig.FAMILIARITY_DECAY_RATE.get() + "\n" +
                "  thresholdThriving     = " + SocialConfig.THRESHOLD_THRIVING.get() + "\n" +
                "  thresholdLonely       = " + SocialConfig.THRESHOLD_LONELY.get() + "\n" +
                "  thresholdIsolated     = " + SocialConfig.THRESHOLD_ISOLATED.get() + "\n" +
                "  enableBenefits        = " + SocialConfig.ENABLE_BENEFITS.get() + "\n" +
                "  enablePenalties       = " + SocialConfig.ENABLE_PENALTIES.get()
        ), false);
        return 1;
    }
}

