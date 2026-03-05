package com.neoassetextractor.parser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.neoassetextractor.NeoAssetExtractor;

import java.util.HashSet;
import java.util.Set;

/**
 * Parser for blockstate JSON files
 */
public class BlockstateParser {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    /**
     * Extract all model paths referenced in a blockstate JSON
     */
    public static Set<String> extractModelPaths(String blockstateJson) {
        Set<String> modelPaths = new HashSet<String>();
        
        try {
            JsonObject json = GSON.fromJson(blockstateJson, JsonObject.class);
            
            // Parse variants
            if (json.has("variants")) {
                JsonObject variants = json.getAsJsonObject("variants");
                for (String variantKey : variants.keySet()) {
                    JsonElement variantValue = variants.get(variantKey);
                    
                    if (variantValue.isJsonArray()) {
                        for (JsonElement element : variantValue.getAsJsonArray()) {
                            extractModelFromVariant(element, modelPaths);
                        }
                    } else {
                        extractModelFromVariant(variantValue, modelPaths);
                    }
                }
            }
            
            // Parse multipart
            if (json.has("multipart")) {
                JsonArray multipart = json.getAsJsonArray("multipart");
                for (JsonElement part : multipart) {
                    if (part.isJsonObject()) {
                        JsonObject partObj = part.getAsJsonObject();
                        if (partObj.has("apply")) {
                            JsonElement apply = partObj.get("apply");
                            
                            if (apply.isJsonArray()) {
                                for (JsonElement element : apply.getAsJsonArray()) {
                                    extractModelFromVariant(element, modelPaths);
                                }
                            } else {
                                extractModelFromVariant(apply, modelPaths);
                            }
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            NeoAssetExtractor.LOGGER.error("Failed to parse blockstate JSON", e);
        }
        
        return modelPaths;
    }
    
    private static void extractModelFromVariant(JsonElement variant, Set<String> modelPaths) {
        if (variant.isJsonObject() && variant.getAsJsonObject().has("model")) {
            String modelPath = variant.getAsJsonObject().get("model").getAsString();
            modelPaths.add(modelPath);
        }
    }
}
