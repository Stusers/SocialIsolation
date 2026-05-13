package com.example.socialisolation.commands;

import com.example.socialisolation.SocialIsolation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public class CommandRegistrar {
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        SocialCommand.register(event.getDispatcher(), event.getBuildContext());
        SocialConfigCommand.register(event.getDispatcher(), event.getBuildContext());
        WillsonCommand.register(event.getDispatcher(), event.getBuildContext());
        SocialIsolation.LOGGER.info("Social Isolation commands registered.");
    }
}
