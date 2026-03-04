package com.neoassetextractor.parser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.neoassetextractor.NeoAssetExtractor;

import java.util.HashSet;
import java.util.Set;

/**
 * Parser for model JSON files
 */
public class ModelParser {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    /**
     * Extract all texture paths from a model JSON
     * @param modelJson The model JSON content
     * @return Set of texture paths (format: "namespace:path" or "path")
     */
    public static Set<String> extractTexturePaths(String modelJson) {
        Set<String> texturePaths = new HashSet<>();
        
        try {
            JsonObject json = GSON.fromJson(modelJson, JsonObject.class);
            
            if (json.has("textures")) {
                JsonObject textures = json.getAsJsonObject("textures");
                for (String key : textures.keySet()) {
                    String texturePath = textures.get(key).getAsString();
                    // Skip texture variables (starting with #)
                    if (!texturePath.startsWith("#")) {
                        texturePaths.add(texturePath);
                    }
                }
            }
            
        } catch (Exception e) {
            NeoAssetExtractor.LOGGER.error("Failed to parse model JSON", e);
        }
        
        return texturePaths;
    }
    
    /**
     * Check if model has tinting
     * @param modelJson The model JSON content
     * @return true if model contains tintindex
     */
    public static boolean hasTinting(String modelJson) {
        return modelJson.contains("\"tintindex\"");
    }
    
    /**
     * Get tint index for a specific texture key
     * @param modelJson The model JSON content
     * @param textureKey The texture key to check
     * @return Tint index or -1 if not found
     */
    public static int getTintIndex(String modelJson, String textureKey) {
        try {
            String searchPattern = "\"texture\": \"#" + textureKey + "\"";
            int texturePos = modelJson.indexOf(searchPattern);
            
            if (texturePos == -1) {
                return -1;
            }
            
            String segment = modelJson.substring(texturePos, 
                Math.min(modelJson.length(), texturePos + 200));
            
            String tintPattern = "\"tintindex\": ";
            int tintPos = segment.indexOf(tintPattern);
            
            if (tintPos == -1) {
                return -1;
            }
            
            int numStart = tintPos + tintPattern.length();
            int numEnd = numStart;
            while (numEnd < segment.length() && Character.isDigit(segment.charAt(numEnd))) {
                numEnd++;
            }
            
            if (numEnd > numStart) {
                return Integer.parseInt(segment.substring(numStart, numEnd));
            }
            
        } catch (Exception e) {
            NeoAssetExtractor.LOGGER.error("Failed to parse tint index", e);
        }
        
        return -1;
    }
}
