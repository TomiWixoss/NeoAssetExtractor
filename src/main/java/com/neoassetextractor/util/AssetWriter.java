package com.neoassetextractor.util;

import com.neoassetextractor.NeoAssetExtractor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AssetWriter {
    
    private static final Path OUTPUT_DIR = Paths.get(".minecraft", "extracted_assets");
    
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
