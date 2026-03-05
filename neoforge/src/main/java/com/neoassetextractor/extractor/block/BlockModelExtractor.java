package com.neoassetextractor.extractor.block;

import com.neoassetextractor.core.ExtractionContext;
import com.neoassetextractor.core.ExtractionResult;
import com.neoassetextractor.parser.BlockstateParser;
import com.neoassetextractor.util.AssetWriter;
import com.neoassetextractor.util.ResourceUtil;
import com.neoassetextractor.writer.JsonWriter;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Extractor for block model JSON files
 */
public class BlockModelExtractor {
    private final JsonWriter jsonWriter = new JsonWriter();
    
    public Set<String> extract(ExtractionContext context, ExtractionResult result) {
        Set<String> extractedModels = new HashSet<>();
        
        // Get all model paths from blockstate
        Set<String> modelPaths = getModelPathsFromBlockstate(context);
        
        // If no models found, try direct model
        if (modelPaths.isEmpty()) {
            modelPaths.add(context.getNamespace() + ":block/" + context.getPath());
        }
        
        // Extract each model (and their parents recursively)
        for (String modelPath : modelPaths) {
            extractModelWithParents(context, modelPath, extractedModels, result, 0);
        }
        
        return extractedModels;
    }
    
    /**
     * Extract model and all its parents recursively
     */
    private void extractModelWithParents(ExtractionContext context, String modelPath,
                                        Set<String> extractedModels, ExtractionResult result, int depth) {
        // Prevent infinite recursion
        if (depth > 10) {
            result.addWarning("Max model depth reached (10)");
            return;
        }
        
        // Skip if already extracted
        if (extractedModels.contains(modelPath)) {
            return;
        }
        
        String[] parts = ResourceUtil.parsePath(modelPath, context.getNamespace());
        String namespace = parts[0];
        String path = ResourceUtil.removePrefix(parts[1], "block/");
        
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(
            namespace,
            "models/block/" + path + ".json"
        );
        
        String content = ResourceUtil.loadAsString(context.getResourceManager(), location);
        if (content == null) {
            result.addWarning("Model not found: " + location);
            return;
        }
        
        Path outputPath = AssetWriter.getResourcePackPath(
            context.getNamespace(),
            "blocks",
            context.getPath(),
            "models",
            "block"
        ).resolve(path + ".json");
        
        if (jsonWriter.write(outputPath, content)) {
            result.incrementModels();
            result.addMessage("Extracted model: " + path);
            extractedModels.add(modelPath);
            
            // Extract parent model recursively
            String parentPath = extractParentPath(content);
            if (parentPath != null && !parentPath.startsWith("minecraft:block/") && !parentPath.startsWith("block/")) {
                result.addMessage("Following parent model [depth=" + depth + "]: " + parentPath);
                extractModelWithParents(context, parentPath, extractedModels, result, depth + 1);
            }
        }
    }
    
    private Set<String> getModelPathsFromBlockstate(ExtractionContext context) {
        ResourceLocation blockstateLocation = ResourceLocation.fromNamespaceAndPath(
            context.getNamespace(),
            "blockstates/" + context.getPath() + ".json"
        );
        
        String blockstateContent = ResourceUtil.loadAsString(
            context.getResourceManager(), blockstateLocation);
        
        if (blockstateContent == null) {
            return new HashSet<>();
        }
        
        return BlockstateParser.extractModelPaths(blockstateContent);
    }
    
    /**
     * Extract parent path from model JSON
     */
    private String extractParentPath(String modelContent) {
        try {
            com.google.gson.JsonObject json = new com.google.gson.Gson().fromJson(
                modelContent, com.google.gson.JsonObject.class);
            if (json.has("parent")) {
                return json.get("parent").getAsString();
            }
        } catch (Exception e) {
            // Ignore parse errors
        }
        return null;
    }
}
