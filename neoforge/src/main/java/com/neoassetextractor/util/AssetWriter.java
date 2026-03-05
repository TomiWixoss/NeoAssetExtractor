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
     * 
     * Structure:
     * extracted_assets/
     *   minecraft/
     *     blocks/
     *       oak_button/
     *         assets/
     *           minecraft/
     *             models/block/oak_button.json
     *             textures/block/oak_planks.png
     *             blockstates/oak_button.json
     * 
     * @param modId The mod ID (namespace)
     * @param assetType Type of asset (items/blocks/entities)
     * @param assetName Name of the specific asset
     * @param resourceType Resource type (models/textures/blockstates)
     * @param resourceSubPath Sub-path within resource type (e.g., "block", "item")
     * @return Full output path in resource pack structure
     */
    public static Path getResourcePackPath(String modId, String assetType, String assetName, 
                                           String resourceType, String resourceSubPath) {
        // Base: extracted_assets/modId/assetType/assetName/assets/modId/
        Path basePath = OUTPUT_DIR
            .resolve(modId)
            .resolve(assetType)
            .resolve(assetName)
            .resolve("assets")
            .resolve(modId);
        
        // Add resource type (models/textures/blockstates)
        basePath = basePath.resolve(resourceType);
        
        // Add sub-path if provided (block/item)
        if (resourceSubPath != null && !resourceSubPath.isEmpty()) {
            basePath = basePath.resolve(resourceSubPath);
        }
        
        return basePath;
    }
    
    /**
     * Legacy method - kept for backward compatibility
     * Use getResourcePackPath() for new code
     */
    @Deprecated
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
     * @param modId The mod ID
     * @param assetType Type of asset (items/blocks/entities)
     * @param assetName Name of the specific asset
     */
    public static void createPackMcmeta(String modId, String assetType, String assetName) {
        Path packRoot = OUTPUT_DIR.resolve(modId).resolve(assetType).resolve(assetName);
        Path packMcmetaPath = packRoot.resolve("pack.mcmeta");

        // Skip if already exists
        if (Files.exists(packMcmetaPath)) {
            return;
        }

        String content = "{\n" +
            "  \"pack\": {\n" +
            "    \"pack_format\": 34,\n" +
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