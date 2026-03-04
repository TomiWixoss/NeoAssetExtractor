package com.neoassetextractor.extractor.item;

import com.neoassetextractor.core.ExtractionContext;
import com.neoassetextractor.core.ExtractionResult;
import com.neoassetextractor.extractor.base.BaseExtractor;
import com.neoassetextractor.parser.ModelParser;
import com.neoassetextractor.util.AssetWriter;
import com.neoassetextractor.util.ResourceUtil;
import com.neoassetextractor.writer.JsonWriter;
import com.neoassetextractor.writer.TextureWriter;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;
import java.util.Set;

/**
 * Extractor for items
 */
public class ItemExtractor extends BaseExtractor {
    private final JsonWriter jsonWriter = new JsonWriter();
    private final TextureWriter textureWriter = new TextureWriter();
    
    @Override
    public boolean canExtract(ExtractionContext context) {
        return true; // Can always try to extract items
    }
    
    @Override
    protected void doExtract(ExtractionContext context, ExtractionResult result) {
        // 1. Extract item model JSON
        String modelContent = extractModel(context, result);
        if (modelContent == null) {
            return;
        }
        
        // 2. Extract textures from model
        extractTextures(context, modelContent, result);
        
        // TODO: Handle item overrides (bow, crossbow, compass)
        // TODO: Handle armor layers
    }
    
    private String extractModel(ExtractionContext context, ExtractionResult result) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(
            context.getNamespace(),
            "models/item/" + context.getPath() + ".json"
        );
        
        String content = ResourceUtil.loadAsString(context.getResourceManager(), location);
        if (content == null) {
            result.addWarning("Item model not found: " + location);
            return null;
        }
        
        Path outputPath = AssetWriter.getAssetPath(
            context.getNamespace(),
            "items",
            context.getPath(),
            "models"
        ).resolve(context.getPath() + ".json");
        
        if (jsonWriter.write(outputPath, content)) {
            result.incrementModels();
            result.addMessage("Extracted item model: " + context.getPath());
        }
        
        return content;
    }
    
    private void extractTextures(ExtractionContext context, String modelContent, 
                                 ExtractionResult result) {
        Set<String> texturePaths = ModelParser.extractTexturePaths(modelContent);
        
        // If no textures found, try to follow parent model
        if (texturePaths.isEmpty()) {
            String parentPath = extractParentModel(modelContent);
            if (parentPath != null) {
                result.addMessage("Following parent model: " + parentPath);
                String parentContent = loadParentModel(context, parentPath);
                if (parentContent != null) {
                    texturePaths = ModelParser.extractTexturePaths(parentContent);
                }
            }
        }
        
        for (String texturePath : texturePaths) {
            String[] parts = ResourceUtil.parsePath(texturePath, context.getNamespace());
            String namespace = parts[0];
            String path = ResourceUtil.removePrefix(parts[1], "item/");
            
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(
                namespace, "textures/item/" + path + ".png");
            
            byte[] content = ResourceUtil.loadAsBytes(context.getResourceManager(), location);
            if (content == null) {
                result.addWarning("Item texture not found: " + location);
                continue;
            }
            
            Path outputPath = AssetWriter.getAssetPath(
                context.getNamespace(),
                "items",
                context.getPath(),
                "textures"
            ).resolve(path + ".png");
            
            if (textureWriter.write(outputPath, content)) {
                result.incrementTextures();
                result.addMessage("Extracted item texture: " + path);
            }
        }
    }
    
    private String extractParentModel(String modelContent) {
        try {
            com.google.gson.JsonObject json = new com.google.gson.Gson().fromJson(
                modelContent, com.google.gson.JsonObject.class);
            if (json.has("parent")) {
                return json.get("parent").getAsString();
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }
    
    private String loadParentModel(ExtractionContext context, String parentPath) {
        String[] parts = ResourceUtil.parsePath(parentPath, context.getNamespace());
        String namespace = parts[0];
        String path = ResourceUtil.removePrefix(parts[1], "item/");
        
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(
            namespace, "models/item/" + path + ".json");
        
        return ResourceUtil.loadAsString(context.getResourceManager(), location);
    }
}
