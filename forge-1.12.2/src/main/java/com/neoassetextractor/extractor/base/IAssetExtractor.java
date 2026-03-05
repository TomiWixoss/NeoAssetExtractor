package com.neoassetextractor.extractor.base;

import com.neoassetextractor.core.ExtractionContext;
import com.neoassetextractor.core.ExtractionResult;

/**
 * Base interface for all asset extractors
 */
public interface IAssetExtractor {
    ExtractionResult extract(ExtractionContext context);
    boolean canExtract(ExtractionContext context);
}
