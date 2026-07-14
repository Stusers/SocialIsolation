package com.example.socialisolation;

import com.example.socialisolation.commands.CommandRegistrar;
import com.example.socialisolation.config.ClientConfig;
import com.example.socialisolation.config.SocialConfig;
import com.example.socialisolation.effects.ModEffects;
import com.example.socialisolation.events.PlayerJoinLeaveHandler;
import com.example.socialisolation.events.PlayerTickHandler;
import com.example.socialisolation.events.XpBoostHandler;
import com.example.socialisolation.network.SocialConfigSyncPayload;
import com.example.socialisolation.network.SocialMeterPayload;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

@Mod(SocialIsolation.MODID)
public class SocialIsolation {

    public static final String MODID = "socialisolation";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SocialIsolation(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, SocialConfig.SPEC, "socialisolation-server.toml");
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC, "socialisolation-client.toml");

        ModEffects.register(modEventBus);
        modEventBus.addListener(this::onRegisterPayloadHandlers);
        modEventBus.addListener(this::onConfigLoad);

        NeoForge.EVENT_BUS.register(new CommandRegistrar());
        NeoForge.EVENT_BUS.register(new PlayerTickHandler());
        NeoForge.EVENT_BUS.register(new PlayerJoinLeaveHandler());
        NeoForge.EVENT_BUS.register(new XpBoostHandler());

        LOGGER.info("Social Isolation mod loaded.");
    }

    private void onConfigLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SocialConfig.SPEC) {
            SocialConfig.validateThresholds();
        }
    }

    private void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("2");
        registrar.playToClient(
                SocialMeterPayload.TYPE,
                SocialMeterPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    com.example.socialisolation.client.ClientSocialData.setSocialMeter(payload.meter());
                    com.example.socialisolation.client.ClientSocialData.setTotalPointsRegained(payload.totalPointsRegained());
                })
        );
        registrar.playToClient(
                SocialConfigSyncPayload.TYPE,
                SocialConfigSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        com.example.socialisolation.client.ClientSocialData.setOpacConfig(
                                payload.pointsPerChunk(), payload.maxChunks(),
                                payload.thresholdThriving(), payload.thresholdLonely(), payload.thresholdIsolated()
                        )
                )
        );
    }
}
