package com.neoassetextractor;

import com.neoassetextractor.client.KeyBindings;
import com.neoassetextractor.command.ExtractCommand;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
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
        
        // Register keybindings
        modEventBus.addListener(this::onRegisterKeyMappings);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        ExtractCommand.register(event.getDispatcher());
    }
    
    private void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(KeyBindings.EXTRACT_KEY);
        event.register(KeyBindings.EXTRACT_TAB_KEY);
        LOGGER.info("Registered keybindings");
    }
}
