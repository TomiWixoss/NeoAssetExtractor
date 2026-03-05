package com.neoassetextractor.util;

import com.neoassetextractor.NeoAssetExtractor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AssetWriter {
    
    private static final Path OUTPUT_DIR = Paths.get(".minecraft", "extracted_assets");
    
    /**
     * Get output path for an asset in resource pack structure
     */
    public static Path getResourcePackPath(String modId, String assetType, String assetName, 
                                           String resourceType, String resourceSubPath) {
        Path basePath = OUTPUT_DIR
            .resolve(modId)
            .resolve(assetType)
            .resolve(assetName)
            .resolve("assets")
            .resolve(modId);
        
        basePath = basePath.resolve(resourceType);
        
        if (resourceSubPath != null && !resourceSubPath.isEmpty()) {
            basePath = basePath.resolve(resourceSubPath);
        }
        
        return basePath;
    }
    
    @Deprecated
    public static Path getAssetPath(String modId, String assetType, String assetName, String subPath) {
        Path basePath = OUTPUT_DIR.resolve(modId).resolve(assetType).resolve(assetName);
        if (subPath != null && !subPath.isEmpty()) {
            basePath = basePath.resolve(subPath);
        }
        return basePath;
    }
    
    @Deprecated
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

    /**
     * Create pack.mcmeta file for Blockbench compatibility
     * Note: pack_format 3 for 1.12.2
     */
    public static void createPackMcmeta(String modId, String assetType, String assetName) {
        Path packRoot = OUTPUT_DIR.resolve(modId).resolve(assetType).resolve(assetName);
        Path packMcmetaPath = packRoot.resolve("pack.mcmeta");

        if (Files.exists(packMcmetaPath)) {
            return;
        }

        String content = "{\n" +
            "  \"pack\": {\n" +
            "    \"pack_format\": 3,\n" +
            "    \"description\": \"Extracted assets from NeoAssetExtractor\"\n" +
            "  }\n" +
            "}";

        try {
            writeFile(packMcmetaPath, content);
        } catch (IOException e) {
            NeoAssetExtractor.LOGGER.warn("Failed to create pack.mcmeta: {}", e.getMessage());
        }
    }
}
