package com.neoassetextractor.extractor.recipe;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.neoassetextractor.NeoAssetExtractor;
import com.neoassetextractor.util.AssetWriter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.*;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Extract recipes cho item/block (Forge 1.12.2)
 */
public class RecipeExtractor {
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    /**
     * Extract tất cả recipes có output là itemId này
     */
    public static int extractRecipesForItem(ResourceLocation itemId, String assetType, Path outputDir) {
        Set<ResourceLocation> extractedRecipeIds = new HashSet<>();
        int count = 0;
        
        // Lấy tất cả recipes từ CraftingManager
        for (IRecipe recipe : CraftingManager.REGISTRY) {
            try {
                ItemStack output = recipe.getRecipeOutput();
                if (output.isEmpty()) {
                    continue;
                }
                
                ResourceLocation outputId = output.getItem().getRegistryName();
                if (outputId != null && outputId.equals(itemId)) {
                    ResourceLocation recipeId = recipe.getRegistryName();
                    if (recipeId != null && !extractedRecipeIds.contains(recipeId)) {
                        if (extractSingleRecipe(recipe, recipeId, outputDir)) {
                            extractedRecipeIds.add(recipeId);
                            count++;
                        }
                    }
                }
            } catch (Exception e) {
                // Skip recipes có lỗi
            }
        }
        
        if (count > 0) {
            NeoAssetExtractor.LOGGER.info("Extracted {} recipe(s) for: {}", count, itemId);
        }
        
        return count;
    }
    
    private static boolean extractSingleRecipe(IRecipe recipe, ResourceLocation recipeId, Path outputDir) {
        NeoAssetExtractor.LOGGER.info("Extracting recipe: {}", recipeId);
        
        try {
            JsonObject recipeJson = null;
            
            // Manually construct JSON based on recipe type
            if (recipe instanceof ShapedRecipes) {
                recipeJson = constructShapedRecipeJson((ShapedRecipes) recipe);
            } else if (recipe instanceof ShapelessRecipes) {
                recipeJson = constructShapelessRecipeJson((ShapelessRecipes) recipe);
            } else if (recipe instanceof ShapelessOreRecipe) {
                recipeJson = constructShapelessOreRecipeJson((ShapelessOreRecipe) recipe);
            } else if (recipe instanceof ShapedOreRecipe) {
                recipeJson = constructShapedOreRecipeJson((ShapedOreRecipe) recipe);
            } else {
                NeoAssetExtractor.LOGGER.debug("Unsupported recipe type: {}", recipe.getClass().getName());
                return false;
            }
            
            if (recipeJson == null) {
                return false;
            }
            
            // Add type field
            recipeJson.addProperty("type", getRecipeType(recipe));
            
            String recipeJsonStr = GSON.toJson(recipeJson);
            
            // Output path: {outputDir}/data/{namespace}/recipes/{recipe_name}.json
            Path recipePath = outputDir
                .resolve("data")
                .resolve(recipeId.getNamespace())
                .resolve("recipes")
                .resolve(recipeId.getPath() + ".json");
            
            AssetWriter.writeFile(recipePath, recipeJsonStr);
            NeoAssetExtractor.LOGGER.info("Wrote recipe: {}", recipePath);
            return true;
            
        } catch (Exception e) {
            NeoAssetExtractor.LOGGER.error("Failed to serialize recipe: " + recipeId, e);
            return false;
        }
    }
    
    private static String getRecipeType(IRecipe recipe) {
        if (recipe instanceof ShapedRecipes || recipe instanceof ShapedOreRecipe) {
            return "minecraft:crafting_shaped";
        } else if (recipe instanceof ShapelessRecipes || recipe instanceof ShapelessOreRecipe) {
            return "minecraft:crafting_shapeless";
        }
        return "minecraft:crafting_shaped";
    }
    
    private static JsonObject constructShapedRecipeJson(ShapedRecipes recipe) {
        JsonObject json = new JsonObject();
        
        int width = recipe.recipeWidth;
        int height = recipe.recipeHeight;
        NonNullList<Ingredient> ingredients = recipe.recipeItems;
        
        // Pattern
        JsonArray patternArray = new JsonArray();
        for (int i = 0; i < height; i++) {
            StringBuilder row = new StringBuilder();
            for (int j = 0; j < width; j++) {
                Ingredient ingredient = ingredients.get(i * width + j);
                if (ingredient.getMatchingStacks().length == 0) {
                    row.append(' ');
                } else {
                    row.append((char)('A' + (i * width + j)));
                }
            }
            patternArray.add(row.toString());
        }
        json.add("pattern", patternArray);
        
        // Key
        JsonObject keyObj = new JsonObject();
        for (int i = 0; i < ingredients.size(); i++) {
            Ingredient ingredient = ingredients.get(i);
            if (ingredient.getMatchingStacks().length > 0) {
                char key = (char)('A' + i);
                keyObj.add(String.valueOf(key), ingredientToJson(ingredient));
            }
        }
        json.add("key", keyObj);
        
        // Result
        json.add("result", itemStackToJson(recipe.getRecipeOutput()));
        
        return json;
    }
    
    private static JsonObject constructShapelessRecipeJson(ShapelessRecipes recipe) {
        JsonObject json = new JsonObject();
        
        // Ingredients
        JsonArray ingredientsArray = new JsonArray();
        for (Ingredient ingredient : recipe.recipeItems) {
            ingredientsArray.add(ingredientToJson(ingredient));
        }
        json.add("ingredients", ingredientsArray);
        
        // Result
        json.add("result", itemStackToJson(recipe.getRecipeOutput()));
        
        return json;
    }
    
    private static JsonObject constructShapelessOreRecipeJson(ShapelessOreRecipe recipe) {
        JsonObject json = new JsonObject();
        
        // Ingredients
        JsonArray ingredientsArray = new JsonArray();
        for (Ingredient ingredient : recipe.getIngredients()) {
            ingredientsArray.add(ingredientToJson(ingredient));
        }
        json.add("ingredients", ingredientsArray);
        
        // Result
        json.add("result", itemStackToJson(recipe.getRecipeOutput()));
        
        return json;
    }
    
    private static JsonObject constructShapedOreRecipeJson(ShapedOreRecipe recipe) {
        JsonObject json = new JsonObject();
        
        int width = recipe.getRecipeWidth();
        int height = recipe.getRecipeHeight();
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        
        // Pattern
        JsonArray patternArray = new JsonArray();
        for (int i = 0; i < height; i++) {
            StringBuilder row = new StringBuilder();
            for (int j = 0; j < width; j++) {
                Ingredient ingredient = ingredients.get(i * width + j);
                if (ingredient.getMatchingStacks().length == 0) {
                    row.append(' ');
                } else {
                    row.append((char)('A' + (i * width + j)));
                }
            }
            patternArray.add(row.toString());
        }
        json.add("pattern", patternArray);
        
        // Key
        JsonObject keyObj = new JsonObject();
        for (int i = 0; i < ingredients.size(); i++) {
            Ingredient ingredient = ingredients.get(i);
            if (ingredient.getMatchingStacks().length > 0) {
                char key = (char)('A' + i);
                keyObj.add(String.valueOf(key), ingredientToJson(ingredient));
            }
        }
        json.add("key", keyObj);
        
        // Result
        json.add("result", itemStackToJson(recipe.getRecipeOutput()));
        
        return json;
    }
    
    private static com.google.gson.JsonElement ingredientToJson(Ingredient ingredient) {
        ItemStack[] stacks = ingredient.getMatchingStacks();
        if (stacks.length == 1) {
            // Single item
            JsonObject obj = new JsonObject();
            ResourceLocation itemId = stacks[0].getItem().getRegistryName();
            if (itemId != null) {
                obj.addProperty("item", itemId.toString());
            }
            return obj;
        } else if (stacks.length > 1) {
            // Multiple items
            JsonArray array = new JsonArray();
            for (ItemStack stack : stacks) {
                JsonObject obj = new JsonObject();
                ResourceLocation itemId = stack.getItem().getRegistryName();
                if (itemId != null) {
                    obj.addProperty("item", itemId.toString());
                    array.add(obj);
                }
            }
            return array;
        }
        return new JsonObject();
    }
    
    private static JsonObject itemStackToJson(ItemStack stack) {
        JsonObject obj = new JsonObject();
        ResourceLocation itemId = stack.getItem().getRegistryName();
        if (itemId != null) {
            obj.addProperty("item", itemId.toString());
        }
        if (stack.getCount() > 1) {
            obj.addProperty("count", stack.getCount());
        }
        return obj;
    }
}
