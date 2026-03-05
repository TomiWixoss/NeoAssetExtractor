package com.neoassetextractor.writer;

import com.neoassetextractor.NeoAssetExtractor;
import com.neoassetextractor.util.AssetWriter;

import java.nio.file.Path;

/**
 * Writer for texture files (PNG, etc.)
 */
public class TextureWriter implements IAssetWriter {
    
    @Override
    public boolean write(Path outputPath, Object content) {
        try {
            if (content instanceof byte[]) {
                AssetWriter.writeFile(outputPath, (byte[]) content);
                return true;
            } else {
                NeoAssetExtractor.LOGGER.error("Unsupported content type for texture writer: {}", 
                    content.getClass().getName());
                return false;
            }
            
        } catch (Exception e) {
            NeoAssetExtractor.LOGGER.error("Failed to write texture file: {}", outputPath, e);
            return false;
        }
    }
}
