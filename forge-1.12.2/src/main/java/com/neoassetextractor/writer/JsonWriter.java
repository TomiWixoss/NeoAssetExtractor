package com.neoassetextractor.writer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.neoassetextractor.NeoAssetExtractor;
import com.neoassetextractor.util.AssetWriter;

import java.nio.file.Path;

/**
 * Writer for JSON files
 */
public class JsonWriter implements IAssetWriter {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    @Override
    public boolean write(Path outputPath, Object content) {
        try {
            String jsonContent;
            
            if (content instanceof String) {
                JsonObject jsonObject = GSON.fromJson((String) content, JsonObject.class);
                jsonContent = GSON.toJson(jsonObject);
            } else if (content instanceof JsonObject) {
                jsonContent = GSON.toJson(content);
            } else {
                NeoAssetExtractor.LOGGER.error("Unsupported content type for JSON writer: {}", 
                    content.getClass().getName());
                return false;
            }
            
            AssetWriter.writeFile(outputPath, jsonContent);
            return true;
            
        } catch (Exception e) {
            NeoAssetExtractor.LOGGER.error("Failed to write JSON file: {}", outputPath, e);
            return false;
        }
    }
}
