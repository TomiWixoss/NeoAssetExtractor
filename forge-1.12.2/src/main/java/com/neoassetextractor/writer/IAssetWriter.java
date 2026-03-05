package com.neoassetextractor.writer;

import java.nio.file.Path;

/**
 * Interface for writing assets to disk
 */
public interface IAssetWriter {
    boolean write(Path outputPath, Object content);
}
