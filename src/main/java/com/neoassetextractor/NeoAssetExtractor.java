package com.neoassetextractor;

import com.neoassetextractor.command.ExtractCommand;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(NeoAssetExtractor.MOD_ID)
public class NeoAssetExtractor {
    public static final String MOD_ID = "neoassetextractor";
    public static final Logger LOGGER = LoggerFactory.getLogger(NeoAssetExtractor.class);

    public NeoAssetExtractor(IEventBus modEventBus) {
        LOGGER.info("NeoAssetExtractor initializing...");
        
        // Register command handler
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        ExtractCommand.register(event.getDispatcher());
    }
}
