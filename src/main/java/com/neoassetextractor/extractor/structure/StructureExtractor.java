package com.neoassetextractor.extractor.structure;

import com.neoassetextractor.util.AssetWriter;
import com.neoassetextractor.util.ResourceUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.nio.file.Path;
import java.util.Map;

/**
 * Extract structure files (.nbt) từ mods
 */
public class StructureExtractor {
    
    /**
     * Extract tất cả structures từ 1 namespace (mod)
     * @param namespace Mod ID (e.g., "minecraft", "create")
     * @return Số lượng structures đã extract
     */
    public static int extractAllStructures(String namespace) {
        int count = 0;
        
        try {
            // List tất cả resources trong data/{namespace}/structure/
            Map<ResourceLocation, Resource> structures = Minecraft.getInstance()
                .getResourceManager()
                .listResources("structure", location -> 
                    location.getNamespace().equals(namespace) && 
                    location.getPath().endsWith(".nbt")
                );
            
            for (Map.Entry<ResourceLocation, Resource> entry : structures.entrySet()) {
                ResourceLocation location = entry.getKey();
                
                // Load structure file
                byte[] content = ResourceUtil.loadAsBytes(
                    Minecraft.getInstance().getResourceManager(), location);
                
                if (content == null) {
                    continue;
                }
                
                // Extract structure name từ path
                // Path format: "structure/{name}.nbt"
                String structurePath = location.getPath();
                String structureName = structurePath
                    .replace("structure/", "")
                    .replace(".nbt", "");
                
                // Output: extracted_assets/{namespace}/structures/{name}/data/{namespace}/structure/{name}.nbt
                Path outputDir = Path.of(".minecraft", "extracted_assets", namespace, "structures");
                Path structureFile = outputDir
                    .resolve(structureName)
                    .resolve("data")
                    .resolve(namespace)
                    .resolve("structure")
                    .resolve(structureName + ".nbt");
                
                try {
                    AssetWriter.writeFile(structureFile, content);
                    count++;
                } catch (Exception e) {
                    com.neoassetextractor.NeoAssetExtractor.LOGGER.error(
                        "Failed to write structure: {}", structureName, e);
                }
            }
            
        } catch (Exception e) {
            com.neoassetextractor.NeoAssetExtractor.LOGGER.error(
                "Failed to extract structures for namespace: {}", namespace, e);
        }
        
        return count;
    }
}
