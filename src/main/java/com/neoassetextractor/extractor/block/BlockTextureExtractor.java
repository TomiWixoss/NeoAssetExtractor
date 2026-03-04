package com.neoassetextractor.extractor.block;

import com.neoassetextractor.core.ExtractionContext;
import com.neoassetextractor.core.ExtractionResult;
import com.neoassetextractor.parser.ModelParser;
import com.neoassetextractor.util.AssetWriter;
import com.neoassetextractor.util.ResourceUtil;
import com.neoassetextractor.util.TextureCapture;
import com.neoassetextractor.writer.TextureWriter;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Extractor for block textures
 */
public class BlockTextureExtractor {
    private final TextureWriter textureWriter = new TextureWriter();
    
    public void extract(ExtractionContext context, Set<String> modelPaths, ExtractionResult result) {
        Set<String> extractedTextures = new HashSet<>();
        
        // Check if block has color handler
        boolean hasColorHandler = false;
        if (context.getLevel() != null && context.getBlockState() != null) {
            int color = Minecraft.getInstance().getBlockColors()
                .getColor(context.getBlockState(), context.getLevel(), context.getBlockPos(), 0);
            hasColorHandler = (color != -1);
        }
        
        // Extract textures from all models
        for (String modelPath : modelPaths) {
            extractTexturesFromModel(context, modelPath, hasColorHandler, extractedTextures, result);
        }
    }
    
    private void extractTexturesFromModel(ExtractionContext context, String modelPath, 
                                         boolean hasColorHandler, Set<String> extractedTextures,
                                         ExtractionResult result) {
        // Parse model path
        String[] parts = ResourceUtil.parsePath(modelPath, context.getNamespace());
        String namespace = parts[0];
        String path = ResourceUtil.removePrefix(parts[1], "block/");
        
        // Load model JSON
        ResourceLocation modelLocation = ResourceLocation.fromNamespaceAndPath(
            namespace, "models/block/" + path + ".json");
        
        String modelContent = ResourceUtil.loadAsString(context.getResourceManager(), modelLocation);
        if (modelContent == null) {
            return;
        }
        
        // Extract texture paths from model
        Set<String> texturePaths = ModelParser.extractTexturePaths(modelContent);
        boolean hasTinting = hasColorHandler || ModelParser.hasTinting(modelContent);
        
        // Extract each texture
        for (String texturePath : texturePaths) {
            String[] texParts = ResourceUtil.parsePath(texturePath, namespace);
            String texNamespace = texParts[0];
            String texFile = ResourceUtil.removePrefix(texParts[1], "block/");
            
            String textureKey = texNamespace + ":" + texFile;
            if (extractedTextures.contains(textureKey)) {
                continue; // Already extracted
            }
            
            // Extract base texture
            if (extractTexture(context, texNamespace, texFile, result)) {
                extractedTextures.add(textureKey);
                
                // Extract tinted version if applicable
                if (hasTinting && context.getBlockState() != null) {
                    extractTintedTexture(context, modelContent, texNamespace, texFile, 
                        texturePath, result);
                }
            }
        }
    }
    
    private boolean extractTexture(ExtractionContext context, String namespace, 
                                   String texturePath, ExtractionResult result) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(
            namespace, "textures/block/" + texturePath + ".png");
        
        byte[] content = ResourceUtil.loadAsBytes(context.getResourceManager(), location);
        if (content == null) {
            result.addWarning("Texture not found: " + location);
            return false;
        }
        
        Path outputPath = AssetWriter.getOutputPath(namespace, "blocks/textures")
            .resolve(texturePath + ".png");
        
        if (textureWriter.write(outputPath, content)) {
            result.incrementTextures();
            result.addMessage("Extracted texture: " + texturePath);
            return true;
        }
        
        return false;
    }
    
    private void extractTintedTexture(ExtractionContext context, String modelContent,
                                     String namespace, String texturePath, String textureKey,
                                     ExtractionResult result) {
        // Get tint index
        int tintIndex = ModelParser.getTintIndex(modelContent, textureKey);
        if (tintIndex < 0) {
            tintIndex = 0; // Default for blocks with color handlers
        }
        
        ResourceLocation texLocation = ResourceLocation.fromNamespaceAndPath(
            namespace, "block/" + texturePath);
        
        Path tintedPath = AssetWriter.getOutputPath(namespace, "blocks/textures_tinted")
            .resolve(texturePath + "_tinted.png");
        
        if (TextureCapture.captureBlockTextureWithTint(
                texLocation, context.getBlockState(), context.getLevel(), 
                context.getBlockPos(), tintIndex, tintedPath)) {
            result.addMessage("Extracted tinted texture: " + texturePath);
        }
    }
}
