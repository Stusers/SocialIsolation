package com.example.socialisolation.client;

import com.example.socialisolation.SocialIsolation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * Client-only event handler registered on the MOD bus.
 * Handles packet reception, client command registration, and HUD layer registration.
 */
@EventBusSubscriber(modid = SocialIsolation.MODID, value = Dist.CLIENT)
public class ClientEventHandler {

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(SocialIsolation.MODID, "social_hud"),
                new SocialHudRenderer()
        );
    }
}
