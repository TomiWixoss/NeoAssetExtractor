package com.neoassetextractor.extractor.base;

import com.neoassetextractor.core.ExtractionContext;
import com.neoassetextractor.core.ExtractionResult;

/**
 * Base interface for all asset extractors
 */
public interface IAssetExtractor {
    /**
     * Extract assets based on the provided context
     * @param context Extraction context containing all necessary information
     * @return Result object with statistics and messages
     */
    ExtractionResult extract(ExtractionContext context);
    
    /**
     * Check if this extractor can handle the given context
     * @param context Extraction context
     * @return true if this extractor can handle the context
     */
    boolean canExtract(ExtractionContext context);
}
