package com.neoassetextractor.extractor.block;

import com.neoassetextractor.parser.BlockstateParser;
import com.neoassetextractor.parser.ModelParser;
import com.neoassetextractor.util.ResourceUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

/**
 * Extract block variants thành các folder riêng biệt cho Blockbench
 */
public class BlockbenchVariantExtractor {
    
    public static int extractAllVariants(ResourceLocation blockId) {
        // Load blockstate
        ResourceLocation blockstateLocation = ResourceLocation.fromNamespaceAndPath(
            blockId.getNamespace(),
            "blockstates/" + blockId.getPath() + ".json"
        );
        
        String blockstateContent = ResourceUtil.loadAsString(
            Minecraft.getInstance().getResourceManager(), blockstateLocation);
        
        if (blockstateContent == null) {
            return 0;
        }
        
        // Parse tất cả model paths
        Set<String> modelPaths = BlockstateParser.extractModelPaths(blockstateContent);
        
        int count = 0;
        for (String modelPath : modelPaths) {
            if (extractSingleVariant(blockId, modelPath)) {
                count++;
            }
        }
        
        return count;
    }
    
    private static boolean extractSingleVariant(ResourceLocation blockId, String modelPath) {
        String[] parts = ResourceUtil.parsePath(modelPath, blockId.getNamespace());
        String namespace = parts[0];
        String path = ResourceUtil.removePrefix(parts[1], "block/");
        
        // Variant name
        String variantName = blockId.getPath() + "_" + path.replace("/", "_");
        
        // Load model
        ResourceLocation modelLocation = ResourceLocation.fromNamespaceAndPath(
            namespace, "models/block/" + path + ".json");
        
        String modelContent = ResourceUtil.loadAsString(
            Minecraft.getInstance().getResourceManager(), modelLocation);
        
        if (modelContent == null) return false;
        
        // Output dir
        Path outputDir = Paths.get(
            ".minecraft", "extracted_assets", namespace, "blocks_variants", variantName);
        
        try {
            Files.createDirectories(outputDir);
            Files.writeString(outputDir.resolve("model.json"), modelContent);
            
            // Extract textures
            Set<String> texturePaths = ModelParser.extractTexturePaths(modelContent);
            
            // LUÔN load parent nếu có
            String parentPath = extractParentPath(modelContent);
            String parentContent = null;
            if (parentPath != null) {
                parentContent = loadParentModel(namespace, parentPath);
                if (parentContent != null && texturePaths.isEmpty()) {
                    texturePaths = ModelParser.extractTexturePaths(parentContent);
                }
            }
            
            // Merge model with parent
            String completeModel = mergeModelWithParent(modelContent, parentContent);
            Files.writeString(outputDir.resolve("model.json"), completeModel);
            
            // Save textures
            Path texturesDir = outputDir.resolve("textures");
            Files.createDirectories(texturesDir);
            
            for (String texturePath : texturePaths) {
                String[] texParts = ResourceUtil.parsePath(texturePath, namespace);
                String texNamespace = texParts[0];
                String texFile = ResourceUtil.removePrefix(texParts[1], "block/");
                
                ResourceLocation texLocation = ResourceLocation.fromNamespaceAndPath(
                    texNamespace, "textures/block/" + texFile + ".png");
                
                byte[] texContent = ResourceUtil.loadAsBytes(
                    Minecraft.getInstance().getResourceManager(), texLocation);
                
                if (texContent != null) {
                    String flatName = texFile.replace("/", "_") + ".png";
                    Files.write(texturesDir.resolve(flatName), texContent);
                }
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private static String extractParentPath(String modelContent) {
        try {
            com.google.gson.JsonObject json = new com.google.gson.Gson().fromJson(
                modelContent, com.google.gson.JsonObject.class);
            if (json.has("parent")) {
                return json.get("parent").getAsString();
            }
        } catch (Exception e) {}
        return null;
    }
    
    private static String loadParentModel(String namespace, String parentPath) {
        String[] parts = ResourceUtil.parsePath(parentPath, namespace);
        String ns = parts[0];
        String path = ResourceUtil.removePrefix(parts[1], "block/");
        
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(
            ns, "models/block/" + path + ".json");
        
        return ResourceUtil.loadAsString(
            Minecraft.getInstance().getResourceManager(), location);
    }
    
    private static String mergeModelWithParent(String childContent, String parentContent) {
        if (parentContent == null) return childContent;
        
        try {
            com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
            com.google.gson.JsonObject child = gson.fromJson(childContent, com.google.gson.JsonObject.class);
            com.google.gson.JsonObject parent = gson.fromJson(parentContent, com.google.gson.JsonObject.class);
            
            // Copy elements từ parent
            if (parent.has("elements")) {
                child.add("elements", parent.get("elements"));
            }
            
            // Merge textures
            if (parent.has("textures")) {
                com.google.gson.JsonObject parentTextures = parent.getAsJsonObject("textures");
                if (!child.has("textures")) {
                    child.add("textures", parentTextures);
                } else {
                    com.google.gson.JsonObject childTextures = child.getAsJsonObject("textures");
                    for (String key : parentTextures.keySet()) {
                        if (!childTextures.has(key)) {
                            childTextures.add(key, parentTextures.get(key));
                        }
                    }
                }
            }
            
            // Rewrite texture paths thành relative flat paths
            if (child.has("textures")) {
                com.google.gson.JsonObject textures = child.getAsJsonObject("textures");
                com.google.gson.JsonObject newTextures = new com.google.gson.JsonObject();
                
                for (String key : textures.keySet()) {
                    String texPath = textures.get(key).getAsString();
                    // Remove namespace prefix (e.g., "another_furniture:block/chair/bottom/oak")
                    if (texPath.contains(":")) {
                        texPath = texPath.substring(texPath.indexOf(":") + 1);
                    }
                    // Remove "block/" prefix
                    if (texPath.startsWith("block/")) {
                        texPath = texPath.substring(6);
                    }
                    // Flatten path: chair/bottom/oak -> chair_bottom_oak
                    String flatPath = "textures/" + texPath.replace("/", "_");
                    newTextures.addProperty(key, flatPath);
                }
                
                child.add("textures", newTextures);
            }
            
            child.remove("parent");
            return gson.toJson(child);
        } catch (Exception e) {
            return childContent;
        }
    }
}
