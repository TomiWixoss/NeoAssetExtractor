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
        
        // Serialize recipe to JSON
        try {
            com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
            
            // Get serializer and encode recipe
            var serializer = holder.value().getSerializer();
            var registryAccess = Minecraft.getInstance().level.registryAccess();
            var ops = registryAccess.createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
            
            // MapCodec needs to be converted to Codec first
            var codec = serializer.codec().codec();
            var encoded = codec.encodeStart(ops, holder.value());
            
            if (encoded.error().isPresent()) {
                com.neoassetextractor.NeoAssetExtractor.LOGGER.error(
                    "Failed to encode recipe {}: {}", recipeId, encoded.error().get());
                return false;
            }
            
            String recipeJson = gson.toJson(encoded.result().get());
            
            // Output path: {outputDir}/data/{namespace}/recipes/{recipe_name}.json
            Path recipePath = outputDir
                .resolve("data")
                .resolve(recipeId.getNamespace())
                .resolve("recipes")
                .resolve(recipeId.getPath() + ".json");
            
            AssetWriter.writeFile(recipePath, recipeJson);
            com.neoassetextractor.NeoAssetExtractor.LOGGER.info("Wrote recipe: {}", recipePath);
            return true;
        } catch (Exception e) {
            com.neoassetextractor.NeoAssetExtractor.LOGGER.error(
                "Failed to serialize recipe: {}", recipeId, e);
            return false;
        }
    }
}
