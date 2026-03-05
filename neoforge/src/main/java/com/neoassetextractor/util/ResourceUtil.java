package com.neoassetextractor.util;

import com.neoassetextractor.NeoAssetExtractor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Utility class for loading resources
 */
public class ResourceUtil {
    
    /**
     * Load a resource as string
     * @param resourceManager The resource manager
     * @param location The resource location
     * @return Resource content as string, or null if not found
     */
    public static String loadAsString(ResourceManager resourceManager, ResourceLocation location) {
        try {
            Optional<Resource> resourceOpt = resourceManager.getResource(location);
            if (resourceOpt.isEmpty()) {
                return null;
            }
            
            Resource resource = resourceOpt.get();
            try (InputStream is = resource.open();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                return sb.toString();
            }
            
        } catch (Exception e) {
            NeoAssetExtractor.LOGGER.error("Failed to load resource: {}", location, e);
            return null;
        }
    }
    
    /**
     * Load a resource as byte array
     * @param resourceManager The resource manager
     * @param location The resource location
     * @return Resource content as byte array, or null if not found
     */
    public static byte[] loadAsBytes(ResourceManager resourceManager, ResourceLocation location) {
        try {
            Optional<Resource> resourceOpt = resourceManager.getResource(location);
            if (resourceOpt.isEmpty()) {
                return null;
            }
            
            Resource resource = resourceOpt.get();
            try (InputStream is = resource.open()) {
                return is.readAllBytes();
            }
            
        } catch (Exception e) {
            NeoAssetExtractor.LOGGER.error("Failed to load resource: {}", location, e);
            return null;
        }
    }
    
    /**
     * Parse resource path (format: "namespace:path" or "path")
     * @param path The path to parse
     * @param defaultNamespace Default namespace if not specified
     * @return Array [namespace, path]
     */
    public static String[] parsePath(String path, String defaultNamespace) {
        if (path.contains(":")) {
            String[] parts = path.split(":", 2);
            return new String[]{parts[0], parts[1]};
        }
        return new String[]{defaultNamespace, path};
    }
    
    /**
     * Remove prefix from path if present
     * @param path The path
     * @param prefix The prefix to remove
     * @return Path without prefix
     */
    public static String removePrefix(String path, String prefix) {
        if (path.startsWith(prefix)) {
            return path.substring(prefix.length());
        }
        return path;
    }
}
