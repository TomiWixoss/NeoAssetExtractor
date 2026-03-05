package com.neoassetextractor.extractor.entity;

import com.neoassetextractor.core.ExtractionContext;
import com.neoassetextractor.core.ExtractionResult;
import com.neoassetextractor.extractor.base.BaseExtractor;
import com.neoassetextractor.util.AssetWriter;
import com.neoassetextractor.util.ResourceUtil;
import com.neoassetextractor.writer.TextureWriter;
import net.minecraft.util.ResourceLocation;

import java.nio.file.Path;

/**
 * Extractor for entities (Forge 1.12.2)
 */
public class EntityExtractor extends BaseExtractor {
    private final TextureWriter textureWriter = new TextureWriter();
    
    @Override
    public boolean canExtract(ExtractionContext context) {
        return true;
    }
    
    @Override
    protected void doExtract(ExtractionContext context, ExtractionResult result) {
        // Extract vanilla entity texture
        extractTexture(context, result);
        
        // Create pack.mcmeta for Blockbench compatibility
        AssetWriter.createPackMcmeta(context.getNamespace(), "entities", context.getPath());
    }
    
    private void extractTexture(ExtractionContext context, ExtractionResult result) {
        // Try "entity/" path
        ResourceLocation location = new ResourceLocation(
            context.getNamespace(),
            "textures/entity/" + context.getPath() + ".png"
        );
        
        byte[] content = ResourceUtil.loadAsBytes(context.getResourceManager(), location);
        
        // 1.12.2: some entities use nested paths (e.g., textures/entity/zombie/zombie.png)
        if (content == null) {
            ResourceLocation nestedLocation = new ResourceLocation(
                context.getNamespace(),
                "textures/entity/" + context.getPath() + "/" + context.getPath() + ".png"
            );
            content = ResourceUtil.loadAsBytes(context.getResourceManager(), nestedLocation);
        }
        
        if (content == null) {
            result.addWarning("Entity texture not found: " + location);
            result.addMessage("Entity may use Java code model (no JSON available)");
            return;
        }
        
        Path outputPath = AssetWriter.getResourcePackPath(
            context.getNamespace(),
            "entities",
            context.getPath(),
            "textures",
            "entity"
        ).resolve(context.getPath() + ".png");
        
        if (textureWriter.write(outputPath, content)) {
            result.incrementTextures();
            result.addMessage("Extracted entity texture: " + context.getPath());
            result.addWarning("Entity uses Java code model (only texture extracted)");
        }
    }
}
