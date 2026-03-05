package com.neoassetextractor.integration.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;

/**
 * JEI Plugin để access JEI's recipe registry
 */
@JeiPlugin
public class JEIPlugin implements IModPlugin {
    
    private static IJeiRuntime jeiRuntime;
    
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath("neoassetextractor", "jei_plugin");
    }
    
    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        jeiRuntime = runtime;
        com.neoassetextractor.NeoAssetExtractor.LOGGER.info("JEI Runtime available for recipe extraction");
    }
    
    public static IJeiRuntime getRuntime() {
        return jeiRuntime;
    }
    
    public static boolean isAvailable() {
        return jeiRuntime != null;
    }
}
