package com.neoassetextractor.extractor.recipe;

import com.neoassetextractor.util.AssetWriter;
import com.neoassetextractor.util.ResourceUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Extract recipes cho item/block
 */
public class RecipeExtractor {
    
    /**
     * Extract tất cả recipes có output là itemId này
     */
    public static int extractRecipesForItem(ResourceLocation itemId, String assetType, Path outputDir) {
        RecipeManager recipeManager = Minecraft.getInstance().level.getRecipeManager();
        List<RecipeHolder<?>> matchingRecipes = new ArrayList<>();
        
        // Tìm tất cả recipes có output là item này
        for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
            try {
                // Get result item từ recipe
                var resultItem = holder.value().getResultItem(Minecraft.getInstance().level.registryAccess());
                if (resultItem.isEmpty()) {
                    continue;
                }
                
                ResourceLocation resultId = resultItem.getItem().builtInRegistryHolder().key().location();
                if (resultId.equals(itemId)) {
                    matchingRecipes.add(holder);
                    com.neoassetextractor.NeoAssetExtractor.LOGGER.info("Found recipe: {} -> {}", 
                        holder.id(), resultId);
                }
            } catch (Exception e) {
                // Skip recipes không có result item
            }
        }
        
        if (matchingRecipes.isEmpty()) {
            com.neoassetextractor.NeoAssetExtractor.LOGGER.info("No recipes found for item: {}", itemId);
            return 0;
        }
        
        // Extract từng recipe
        int count = 0;
        for (RecipeHolder<?> holder : matchingRecipes) {
            if (extractSingleRecipe(holder, itemId, assetType, outputDir)) {
                count++;
            }
        }
        
        return count;
    }
    
    private static boolean extractSingleRecipe(RecipeHolder<?> holder, ResourceLocation itemId, 
                                               String assetType, Path outputDir) {
        ResourceLocation recipeId = holder.id();
        
        com.neoassetextractor.NeoAssetExtractor.LOGGER.info("Extracting recipe: {}", recipeId);
        
        // Load recipe JSON từ data pack
        ResourceLocation recipeLocation = ResourceLocation.fromNamespaceAndPath(
            recipeId.getNamespace(),
            "recipe/" + recipeId.getPath() + ".json"
        );
        
        String recipeContent = ResourceUtil.loadAsString(
            Minecraft.getInstance().getResourceManager(), recipeLocation);
        
        if (recipeContent == null) {
            com.neoassetextractor.NeoAssetExtractor.LOGGER.warn("Recipe file not found: {}", recipeLocation);
            return false;
        }
        
        // Output path: {outputDir}/data/{namespace}/recipe/{recipe_name}.json
        Path recipePath = outputDir
            .resolve("data")
            .resolve(recipeId.getNamespace())
            .resolve("recipe")
            .resolve(recipeId.getPath() + ".json");
        
        try {
            AssetWriter.writeFile(recipePath, recipeContent);
            com.neoassetextractor.NeoAssetExtractor.LOGGER.info("Wrote recipe: {}", recipePath);
            return true;
        } catch (Exception e) {
            com.neoassetextractor.NeoAssetExtractor.LOGGER.error("Failed to write recipe: {}", e.getMessage());
            return false;
        }
    }
}
