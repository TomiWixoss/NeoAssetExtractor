package com.neoassetextractor.extractor.block;

import com.neoassetextractor.core.ExtractionContext;
import com.neoassetextractor.core.ExtractionResult;
import com.neoassetextractor.util.AssetWriter;
import com.neoassetextractor.util.ResourceUtil;
import com.neoassetextractor.writer.JsonWriter;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;

/**
 * Extractor for blockstate JSON files
 */
public class BlockstateExtractor {
    private final JsonWriter jsonWriter = new JsonWriter();
    
    public void extract(ExtractionContext context, ExtractionResult result) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(
            context.getNamespace(),
            "blockstates/" + context.getPath() + ".json"
        );
        
        String content = ResourceUtil.loadAsString(context.getResourceManager(), location);
        if (content == null) {
            result.addWarning("Blockstate not found: " + location);
            return;
        }
        
        Path outputPath = AssetWriter.getResourcePackPath(
            context.getNamespace(), 
            "blocks", 
            context.getPath(), 
            "blockstates",
            null
        ).resolve(context.getPath() + ".json");
        
        if (jsonWriter.write(outputPath, content)) {
            result.incrementBlockstates();
            result.addMessage("Extracted blockstate: " + context.getPath());
        }
    }
}
