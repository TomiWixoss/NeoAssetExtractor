package com.neoassetextractor.extractor.block;

import com.neoassetextractor.core.ExtractionContext;
import com.neoassetextractor.core.ExtractionResult;
import com.neoassetextractor.extractor.base.BaseExtractor;

import java.util.Set;

/**
 * Main extractor for blocks - coordinates all block-related extraction
 */
public class BlockExtractor extends BaseExtractor {
    private final BlockstateExtractor blockstateExtractor = new BlockstateExtractor();
    private final BlockModelExtractor modelExtractor = new BlockModelExtractor();
    private final BlockTextureExtractor textureExtractor = new BlockTextureExtractor();
    
    @Override
    public boolean canExtract(ExtractionContext context) {
        return context.getBlockState() != null;
    }
    
    @Override
    protected void doExtract(ExtractionContext context, ExtractionResult result) {
        // 1. Extract blockstate JSON
        blockstateExtractor.extract(context, result);
        
        // 2. Extract all models referenced in blockstate
        Set<String> modelPaths = modelExtractor.extract(context, result);
        
        // 3. Extract all textures from models
        textureExtractor.extract(context, modelPaths, result);
    }
}
