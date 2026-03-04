package com.neoassetextractor.extractor.entity;

import com.neoassetextractor.core.ExtractionContext;
import com.neoassetextractor.core.ExtractionResult;
import com.neoassetextractor.extractor.base.BaseExtractor;
import com.neoassetextractor.util.AssetWriter;
import com.neoassetextractor.util.ResourceUtil;
import com.neoassetextractor.writer.TextureWriter;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;

/**
 * Extractor for entities
 */
public class EntityExtractor extends BaseExtractor {
    private final TextureWriter textureWriter = new TextureWriter();
    
    @Override
    public boolean canExtract(ExtractionContext context) {
        return true;
    }
    
    @Override
    protected void doExtract(ExtractionContext context, ExtractionResult result) {
        // Check if entity is GeckoLib
        // TODO: Implement GeckoLib detection and extraction
        
        // For now, extract vanilla entity texture
        extractTexture(context, result);
    }
    
    private void extractTexture(ExtractionContext context, ExtractionResult result) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(
            context.getNamespace(),
            "textures/entity/" + context.getPath() + ".png"
        );
        
        byte[] content = ResourceUtil.loadAsBytes(context.getResourceManager(), location);
        if (content == null) {
            result.addWarning("Entity texture not found: " + location);
            result.addMessage("Entity may use Java code model (no JSON available)");
            return;
        }
        
        // Use resource pack structure: minecraft/entities/{entity_name}/assets/minecraft/textures/entity/
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
