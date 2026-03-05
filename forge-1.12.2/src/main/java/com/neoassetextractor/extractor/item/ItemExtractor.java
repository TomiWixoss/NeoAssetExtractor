package com.neoassetextractor.extractor.item;

import com.neoassetextractor.core.ExtractionContext;
import com.neoassetextractor.core.ExtractionResult;
import com.neoassetextractor.extractor.base.BaseExtractor;
import com.neoassetextractor.parser.ModelParser;
import com.neoassetextractor.util.AssetWriter;
import com.neoassetextractor.util.ResourceUtil;
import com.neoassetextractor.writer.JsonWriter;
import com.neoassetextractor.writer.TextureWriter;
import net.minecraft.util.ResourceLocation;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;

/**
 * Extractor for items (Forge 1.12.2)
 */
public class ItemExtractor extends BaseExtractor {
    private final JsonWriter jsonWriter = new JsonWriter();
    private final TextureWriter textureWriter = new TextureWriter();
    
    @Override
    public boolean canExtract(ExtractionContext context) {
        return true;
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
        
        // 3. Create pack.mcmeta for Blockbench compatibility
        AssetWriter.createPackMcmeta(context.getNamespace(), "items", context.getPath());
    }
    
    private String extractModel(ExtractionContext context, ExtractionResult result) {
        ResourceLocation location = new ResourceLocation(
            context.getNamespace(),
            "models/item/" + context.getPath() + ".json"
        );
        
        String content = ResourceUtil.loadAsString(context.getResourceManager(), location);
        if (content == null) {
            result.addWarning("Item model not found: " + location);
            return null;
        }
        
        Path outputPath = AssetWriter.getResourcePackPath(
            context.getNamespace(),
            "items",
            context.getPath(),
            "models",
            "item"
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
        
        // If no textures found, follow parent model chain recursively
        if (texturePaths.isEmpty()) {
            texturePaths = followParentChainForTextures(context, modelContent, result, 0);
        }
        
        for (String texturePath : texturePaths) {
            String[] parts = ResourceUtil.parsePath(texturePath, context.getNamespace());
            String namespace = parts[0];
            String path = parts[1];
            
            // Remove prefix (item/ or block/)
            path = ResourceUtil.removePrefix(path, "item/");
            path = ResourceUtil.removePrefix(path, "block/");
            
            // Try item texture first
            ResourceLocation itemLocation = new ResourceLocation(
                namespace, "textures/item/" + path + ".png");
            byte[] content = ResourceUtil.loadAsBytes(context.getResourceManager(), itemLocation);
            
            // If not found, try block texture
            if (content == null) {
                ResourceLocation blockLocation = new ResourceLocation(
                    namespace, "textures/block/" + path + ".png");
                content = ResourceUtil.loadAsBytes(context.getResourceManager(), blockLocation);
            }
            
            // 1.12.2: also try "blocks/" instead of "block/"
            if (content == null) {
                ResourceLocation blocksLocation = new ResourceLocation(
                    namespace, "textures/blocks/" + path + ".png");
                content = ResourceUtil.loadAsBytes(context.getResourceManager(), blocksLocation);
            }
            
            // 1.12.2: also try "items/" instead of "item/"
            if (content == null) {
                ResourceLocation itemsLocation = new ResourceLocation(
                    namespace, "textures/items/" + path + ".png");
                content = ResourceUtil.loadAsBytes(context.getResourceManager(), itemsLocation);
            }
            
            if (content == null) {
                result.addWarning("Item texture not found: " + texturePath);
                continue;
            }
            
            Path outputPath = AssetWriter.getResourcePackPath(
                context.getNamespace(),
                "items",
                context.getPath(),
                "textures",
                "item"
            ).resolve(path + ".png");
            
            if (textureWriter.write(outputPath, content)) {
                result.incrementTextures();
                result.addMessage("Extracted item texture: " + path);
            }
        }
    }
    
    private Set<String> followParentChainForTextures(ExtractionContext context, String modelContent, 
                                                      ExtractionResult result, int depth) {
        if (depth > 10) {
            result.addWarning("Max parent chain depth reached (10)");
            return Collections.emptySet();
        }
        
        String parentPath = extractParentModel(modelContent);
        if (parentPath == null) {
            return Collections.emptySet();
        }
        
        result.addMessage("Following parent model [depth=" + depth + "]: " + parentPath);
        String parentContent = loadParentModel(context, parentPath);
        if (parentContent == null) {
            return Collections.emptySet();
        }
        
        Set<String> texturePaths = ModelParser.extractTexturePaths(parentContent);
        
        if (texturePaths.isEmpty()) {
            return followParentChainForTextures(context, parentContent, result, depth + 1);
        }
        
        return texturePaths;
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
        String path = parts[1];
        
        path = ResourceUtil.removePrefix(path, "item/");
        path = ResourceUtil.removePrefix(path, "block/");
        
        // Try item model first
        ResourceLocation itemLocation = new ResourceLocation(
            namespace, "models/item/" + path + ".json");
        String content = ResourceUtil.loadAsString(context.getResourceManager(), itemLocation);
        
        // If not found, try block model
        if (content == null) {
            ResourceLocation blockLocation = new ResourceLocation(
                namespace, "models/block/" + path + ".json");
            content = ResourceUtil.loadAsString(context.getResourceManager(), blockLocation);
        }
        
        return content;
    }
}
