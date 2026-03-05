package com.neoassetextractor.extractor.recipe;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.neoassetextractor.integration.jei.JEIPlugin;
import com.neoassetextractor.util.AssetWriter;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Extract recipes cho item/block
 * Ưu tiên dùng JEI (lấy được nhiều recipes hơn), fallback về RecipeManager
 */
public class RecipeExtractor {
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    /**
     * Extract tất cả recipes có output là itemId này
     */
    public static int extractRecipesForItem(ResourceLocation itemId, String assetType, Path outputDir) {
        // Thử dùng JEI trước nếu available
        if (JEIPlugin.isAvailable()) {
            try {
                int jeiCount = extractRecipesFromJEI(itemId, outputDir);
                if (jeiCount > 0) {
                    com.neoassetextractor.NeoAssetExtractor.LOGGER.info(
                        "Extracted {} recipe(s) from JEI for: {}", jeiCount, itemId);
                    return jeiCount;
                }
            } catch (Exception e) {
                com.neoassetextractor.NeoAssetExtractor.LOGGER.warn(
                    "Failed to extract from JEI, falling back to RecipeManager", e);
            }
        }
        
        // Fallback về RecipeManager
        return extractRecipesFromManager(itemId, outputDir);
    }
    
    /**
     * Extract recipes từ JEI Runtime - lấy được nhiều recipes hơn RecipeManager
     */
    private static int extractRecipesFromJEI(ResourceLocation itemId, Path outputDir) {
        var runtime = JEIPlugin.getRuntime();
        if (runtime == null) {
            return 0;
        }
        
        var item = BuiltInRegistries.ITEM.get(itemId);
        if (item == null) {
            return 0;
        }
        
        ItemStack targetStack = new ItemStack(item);
        Set<ResourceLocation> extractedRecipeIds = new HashSet<>();
        int count = 0;
        
        // Tạo focus cho output (item mà ta đang tìm recipes)
        var focusFactory = runtime.getJeiHelpers().getFocusFactory();
        var focus = focusFactory.createFocus(
            RecipeIngredientRole.OUTPUT,
            VanillaTypes.ITEM_STACK,
            targetStack
        );
        
        // Lấy tất cả recipe types
        var allRecipeTypes = runtime.getJeiHelpers().getAllRecipeTypes().toList();
        
        for (var recipeType : allRecipeTypes) {
            try {
                // Tạo recipe lookup với focus
                var recipeLookup = runtime.getRecipeManager()
                    .createRecipeLookup(recipeType)
                    .limitFocus(List.of(focus))
                    .includeHidden();
                
                // Lấy tất cả recipes matching
                var recipes = recipeLookup.get().toList();
                
                // Extract từng recipe
                for (var recipe : recipes) {
                    // Chỉ handle Minecraft recipes (RecipeHolder)
                    // Skip custom JEI recipes (brewing, composting, etc.)
                    if (recipe instanceof RecipeHolder<?> recipeHolder) {
                        ResourceLocation recipeId = recipeHolder.id();
                        
                        if (!extractedRecipeIds.contains(recipeId)) {
                            if (extractSingleRecipe(recipeHolder, itemId, outputDir)) {
                                extractedRecipeIds.add(recipeId);
                                count++;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Skip category nếu có lỗi
                com.neoassetextractor.NeoAssetExtractor.LOGGER.debug(
                    "Failed to process JEI category: {}", recipeType.getUid(), e);
            }
        }
        
        return count;
    }
    
    /**
     * Extract recipes từ RecipeManager (fallback)
     */
    private static int extractRecipesFromManager(ResourceLocation itemId, Path outputDir) {
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
            if (extractSingleRecipe(holder, itemId, outputDir)) {
                count++;
            }
        }
        
        return count;
    }
    
    private static boolean extractSingleRecipe(RecipeHolder<?> holder, ResourceLocation itemId, Path outputDir) {
        ResourceLocation recipeId = holder.id();
        
        com.neoassetextractor.NeoAssetExtractor.LOGGER.info("Extracting recipe: {}", recipeId);
        
        try {
            var recipe = holder.value();
            var serializer = recipe.getSerializer();
            var registryAccess = Minecraft.getInstance().level.registryAccess();
            
            JsonObject recipeJson = null;
            
            // Thử serialize bằng codec trước
            var ops = registryAccess.createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
            var codec = serializer.codec().codec();
            
            @SuppressWarnings("unchecked")
            var typedCodec = (com.mojang.serialization.Codec<Object>) (Object) codec;
            var encoded = typedCodec.encodeStart(ops, recipe);
            
            if (encoded.result().isPresent()) {
                // Codec serialize thành công
                recipeJson = encoded.result().get().getAsJsonObject();
            } else if (encoded.error().isPresent() && encoded.error().get().message().contains("unpacked")) {
                // Unpacked recipe - manually construct JSON
                com.neoassetextractor.NeoAssetExtractor.LOGGER.debug(
                    "Manually constructing JSON for unpacked recipe: {}", recipeId);
                recipeJson = manuallyConstructRecipeJson(recipe, registryAccess);
            } else {
                // Lỗi khác
                com.neoassetextractor.NeoAssetExtractor.LOGGER.error(
                    "Failed to encode recipe {}: {}", recipeId, encoded.error().get());
                return false;
            }
            
            if (recipeJson == null) {
                com.neoassetextractor.NeoAssetExtractor.LOGGER.warn(
                    "Could not construct JSON for recipe: {}", recipeId);
                return false;
            }
            
            // Thêm type nếu chưa có
            if (!recipeJson.has("type")) {
                ResourceLocation serializerId = BuiltInRegistries.RECIPE_SERIALIZER.getKey(serializer);
                recipeJson.addProperty("type", serializerId.toString());
            }
            
            String recipeJsonStr = GSON.toJson(recipeJson);
            
            // Output path: {outputDir}/data/{namespace}/recipe/{recipe_name}.json
            Path recipePath = outputDir
                .resolve("data")
                .resolve(recipeId.getNamespace())
                .resolve("recipe")
                .resolve(recipeId.getPath() + ".json");
            
            AssetWriter.writeFile(recipePath, recipeJsonStr);
            com.neoassetextractor.NeoAssetExtractor.LOGGER.info("Wrote recipe: {}", recipePath);
            return true;
            
        } catch (Exception e) {
            com.neoassetextractor.NeoAssetExtractor.LOGGER.error(
                "Failed to serialize recipe: {}", recipeId, e);
            return false;
        }
    }
    
    /**
     * Manually construct JSON cho unpacked recipes
     */
    private static JsonObject manuallyConstructRecipeJson(
        net.minecraft.world.item.crafting.Recipe<?> recipe,
        net.minecraft.core.HolderLookup.Provider registryAccess
    ) {
        try {
            // Check recipe type và construct tương ứng
            if (recipe instanceof net.minecraft.world.item.crafting.ShapedRecipe shapedRecipe) {
                return constructShapedRecipeJson(shapedRecipe, registryAccess);
            } else if (recipe instanceof net.minecraft.world.item.crafting.ShapelessRecipe shapelessRecipe) {
                return constructShapelessRecipeJson(shapelessRecipe, registryAccess);
            } else if (recipe instanceof net.minecraft.world.item.crafting.AbstractCookingRecipe cookingRecipe) {
                return constructCookingRecipeJson(cookingRecipe, registryAccess);
            } else if (recipe instanceof net.minecraft.world.item.crafting.SmithingTransformRecipe smithingRecipe) {
                return constructSmithingRecipeJson(smithingRecipe, registryAccess);
            }
            // Add more recipe types as needed
            
            com.neoassetextractor.NeoAssetExtractor.LOGGER.warn(
                "Unsupported recipe type for manual construction: {}", recipe.getClass().getName());
            return null;
            
        } catch (Exception e) {
            com.neoassetextractor.NeoAssetExtractor.LOGGER.error(
                "Failed to manually construct recipe JSON", e);
            return null;
        }
    }
    
    private static JsonObject constructShapedRecipeJson(
        net.minecraft.world.item.crafting.ShapedRecipe recipe,
        net.minecraft.core.HolderLookup.Provider registryAccess
    ) {
        JsonObject json = new JsonObject();
        
        // ShapedRecipe có width, height và ingredients
        int width = recipe.getWidth();
        int height = recipe.getHeight();
        var ingredients = recipe.getIngredients();
        
        // Pattern
        com.google.gson.JsonArray patternArray = new com.google.gson.JsonArray();
        for (int i = 0; i < height; i++) {
            StringBuilder row = new StringBuilder();
            for (int j = 0; j < width; j++) {
                var ingredient = ingredients.get(i * width + j);
                if (ingredient.isEmpty()) {
                    row.append(' ');
                } else {
                    row.append((char)('A' + (i * width + j)));
                }
            }
            patternArray.add(row.toString());
        }
        json.add("pattern", patternArray);
        
        // Key (ingredients)
        JsonObject keyObj = new JsonObject();
        for (int i = 0; i < ingredients.size(); i++) {
            var ingredient = ingredients.get(i);
            if (!ingredient.isEmpty()) {
                char key = (char)('A' + i);
                keyObj.add(String.valueOf(key), ingredientToJson(ingredient));
            }
        }
        json.add("key", keyObj);
        
        // Result
        json.add("result", itemStackToJson(recipe.getResultItem(registryAccess)));
        
        return json;
    }
    
    private static JsonObject constructShapelessRecipeJson(
        net.minecraft.world.item.crafting.ShapelessRecipe recipe,
        net.minecraft.core.HolderLookup.Provider registryAccess
    ) {
        JsonObject json = new JsonObject();
        
        // Ingredients
        com.google.gson.JsonArray ingredientsArray = new com.google.gson.JsonArray();
        for (var ingredient : recipe.getIngredients()) {
            ingredientsArray.add(ingredientToJson(ingredient));
        }
        json.add("ingredients", ingredientsArray);
        
        // Result
        json.add("result", itemStackToJson(recipe.getResultItem(registryAccess)));
        
        return json;
    }
    
    private static JsonObject constructCookingRecipeJson(
        net.minecraft.world.item.crafting.AbstractCookingRecipe recipe,
        net.minecraft.core.HolderLookup.Provider registryAccess
    ) {
        JsonObject json = new JsonObject();
        
        // Ingredient
        json.add("ingredient", ingredientToJson(recipe.getIngredients().get(0)));
        
        // Result
        json.add("result", itemStackToJson(recipe.getResultItem(registryAccess)));
        
        // Experience & cooking time
        json.addProperty("experience", recipe.getExperience());
        json.addProperty("cookingtime", recipe.getCookingTime());
        
        return json;
    }
    
    private static JsonObject constructSmithingRecipeJson(
        net.minecraft.world.item.crafting.SmithingTransformRecipe recipe,
        net.minecraft.core.HolderLookup.Provider registryAccess
    ) {
        JsonObject json = new JsonObject();
        
        // SmithingTransformRecipe có 3 ingredients: template, base, addition
        var ingredients = recipe.getIngredients();
        if (ingredients.size() >= 3) {
            json.add("template", ingredientToJson(ingredients.get(0)));
            json.add("base", ingredientToJson(ingredients.get(1)));
            json.add("addition", ingredientToJson(ingredients.get(2)));
        }
        
        // Result
        json.add("result", itemStackToJson(recipe.getResultItem(registryAccess)));
        
        return json;
    }
    
    private static com.google.gson.JsonElement ingredientToJson(net.minecraft.world.item.crafting.Ingredient ingredient) {
        var items = ingredient.getItems();
        if (items.length == 1) {
            // Single item
            JsonObject obj = new JsonObject();
            obj.addProperty("item", BuiltInRegistries.ITEM.getKey(items[0].getItem()).toString());
            return obj;
        } else {
            // Multiple items (tag or array)
            com.google.gson.JsonArray array = new com.google.gson.JsonArray();
            for (var stack : items) {
                JsonObject obj = new JsonObject();
                obj.addProperty("item", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
                array.add(obj);
            }
            return array;
        }
    }
    
    private static JsonObject itemStackToJson(net.minecraft.world.item.ItemStack stack) {
        JsonObject obj = new JsonObject();
        obj.addProperty("item", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        if (stack.getCount() > 1) {
            obj.addProperty("count", stack.getCount());
        }
        return obj;
    }
}
