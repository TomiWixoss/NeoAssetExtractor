package com.neoassetextractor.util;

import com.neoassetextractor.NeoAssetExtractor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AssetWriter {
    
    private static final Path OUTPUT_DIR = Paths.get(".minecraft", "extracted_assets");
    
    /**
     * Get output path for an asset
     * @param modId The mod ID (namespace)
     * @param assetType Type of asset (items/blocks/entities)
     * @param assetName Name of the specific asset
     * @param subPath Sub-path within asset folder (e.g., "textures", "models")
     * @return Full output path
     */
    public static Path getAssetPath(String modId, String assetType, String assetName, String subPath) {
        Path basePath = OUTPUT_DIR.resolve(modId).resolve(assetType).resolve(assetName);
        if (subPath != null && !subPath.isEmpty()) {
            basePath = basePath.resolve(subPath);
        }
        return basePath;
    }
    
    /**
     * Legacy method for backward compatibility
     */
    public static Path getOutputPath(String modId, String category) {
        return OUTPUT_DIR.resolve(modId).resolve(category);
    }
    
    public static void ensureDirectoryExists(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createDirectories(path);
            NeoAssetExtractor.LOGGER.info("Created directory: {}", path);
        }
    }
    
    public static void writeFile(Path path, byte[] data) throws IOException {
        ensureDirectoryExists(path.getParent());
        Files.write(path, data);
        NeoAssetExtractor.LOGGER.info("Wrote file: {}", path);
    }
    
    public static void writeFile(Path path, String content) throws IOException {
        writeFile(path, content.getBytes());
    }
}
