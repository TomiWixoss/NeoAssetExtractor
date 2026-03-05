package com.neoassetextractor.util;

import com.neoassetextractor.NeoAssetExtractor;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for loading resources
 */
public class ResourceUtil {
    
    /**
     * Load a resource as string
     */
    public static String loadAsString(IResourceManager resourceManager, ResourceLocation location) {
        try {
            IResource resource = resourceManager.getResource(location);
            InputStream is = resource.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            
            reader.close();
            is.close();
            return sb.toString();
            
        } catch (Exception e) {
            // Resource not found is common, only log at debug level
            NeoAssetExtractor.LOGGER.debug("Resource not found: {}", location);
            return null;
        }
    }
    
    /**
     * Load a resource as byte array
     */
    public static byte[] loadAsBytes(IResourceManager resourceManager, ResourceLocation location) {
        try {
            IResource resource = resourceManager.getResource(location);
            InputStream is = resource.getInputStream();
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            
            is.close();
            return baos.toByteArray();
            
        } catch (Exception e) {
            NeoAssetExtractor.LOGGER.debug("Resource not found: {}", location);
            return null;
        }
    }
    
    /**
     * Parse resource path (format: "namespace:path" or "path")
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
     */
    public static String removePrefix(String path, String prefix) {
        if (path.startsWith(prefix)) {
            return path.substring(prefix.length());
        }
        return path;
    }
}
