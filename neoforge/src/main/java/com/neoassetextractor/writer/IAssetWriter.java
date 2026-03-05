package com.neoassetextractor.writer;

import java.nio.file.Path;

/**
 * Interface for writing assets to disk
 */
public interface IAssetWriter {
    /**
     * Write content to file
     * @param outputPath The output path
     * @param content The content to write
     * @return true if successful
     */
    boolean write(Path outputPath, Object content);
}
