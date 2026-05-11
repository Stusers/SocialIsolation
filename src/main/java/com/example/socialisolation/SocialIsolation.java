package com.example.socialisolation;

import com.example.socialisolation.commands.CommandRegistrar;
import com.example.socialisolation.config.SocialConfig;
import com.example.socialisolation.events.PlayerJoinLeaveHandler;
import com.example.socialisolation.events.PlayerTickHandler;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(SocialIsolation.MODID)
public class SocialIsolation {

    public static final String MODID = "socialisolation";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SocialIsolation(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, SocialConfig.SPEC, "socialisolation-server.toml");

        NeoForge.EVENT_BUS.register(new CommandRegistrar());
        NeoForge.EVENT_BUS.register(new PlayerTickHandler());
        NeoForge.EVENT_BUS.register(new PlayerJoinLeaveHandler());

        LOGGER.info("Social Isolation mod loaded.");
    }
}
