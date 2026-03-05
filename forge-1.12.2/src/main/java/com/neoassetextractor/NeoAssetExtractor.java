package com.neoassetextractor;

import com.neoassetextractor.client.ClientEventHandler;
import com.neoassetextractor.client.KeyBindings;
import com.neoassetextractor.command.ExtractCommand;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = NeoAssetExtractor.MOD_ID, name = NeoAssetExtractor.MOD_NAME, 
     version = NeoAssetExtractor.VERSION, clientSideOnly = true)
public class NeoAssetExtractor {
    public static final String MOD_ID = "neoassetextractor";
    public static final String MOD_NAME = "NeoAssetExtractor";
    public static final String VERSION = "1.0.0";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    @Mod.EventHandler
    @SideOnly(Side.CLIENT)
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("NeoAssetExtractor pre-initializing...");
        KeyBindings.register();
    }

    @Mod.EventHandler
    @SideOnly(Side.CLIENT)
    public void init(FMLInitializationEvent event) {
        LOGGER.info("NeoAssetExtractor initializing...");
        
        // Register client command (works without op)
        ClientCommandHandler.instance.registerCommand(new ExtractCommand());
        
        // Register keybinding event handler
        MinecraftForge.EVENT_BUS.register(new ClientEventHandler());
    }
}
