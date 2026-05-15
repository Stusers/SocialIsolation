package com.example.socialisolation.client;

import com.example.socialisolation.SocialIsolation;
import com.example.socialisolation.network.SocialMeterPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Client-only event handler registered on the MOD bus.
 * Handles packet reception, client command registration, and HUD layer registration.
 */
@EventBusSubscriber(modid = SocialIsolation.MODID, value = Dist.CLIENT)
public class ClientEventHandler {

    @SubscribeEvent
    public static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(
                SocialMeterPayload.TYPE,
                SocialMeterPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        ClientSocialData.setSocialMeter(payload.meter())
                )
        );
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        SocialHudCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(SocialIsolation.MODID, "social_hud"),
                new SocialHudRenderer()
        );
    }
}
