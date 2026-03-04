package com.neoassetextractor.extractor.base;

import com.neoassetextractor.NeoAssetExtractor;
import com.neoassetextractor.core.ExtractionContext;
import com.neoassetextractor.core.ExtractionResult;

/**
 * Abstract base class providing common functionality for all extractors
 */
public abstract class BaseExtractor implements IAssetExtractor {
    
    @Override
    public ExtractionResult extract(ExtractionContext context) {
        ExtractionResult result = new ExtractionResult();
        
        try {
            if (!canExtract(context)) {
                result.addWarning("Cannot extract from context: " + context.getResourceId());
                return result;
            }
            
            doExtract(context, result);
            result.setSuccess(result.getTotalExtracted() > 0);
            
        } catch (Exception e) {
            NeoAssetExtractor.LOGGER.error("Extraction failed for {}", context.getResourceId(), e);
            result.addError("Extraction failed: " + e.getMessage());
            result.setSuccess(false);
        }
        
        return result;
    }
    
    /**
     * Perform the actual extraction logic
     * @param context Extraction context
     * @param result Result object to populate
     */
    protected abstract void doExtract(ExtractionContext context, ExtractionResult result);
}
